package com.sox.api.controller;

import com.sox.api.interceptor.CheckAuth;
import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.RoleModel;
import com.sox.api.model.UserModel;
import com.sox.api.service.Api;
import com.sox.api.service.Check;
import com.sox.api.service.Com;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@CheckLogin(except = "login")
@RequestMapping("/user")
public class UserController {
    @Autowired
    private Api api;

    @Autowired
    private Com com;

    @Autowired
    private UserModel user_m;

    @Autowired
    private RoleModel role_m;

    @Autowired
    private Check check;

    @RequestMapping("/login")
    public Map<String, Object> login() {
        String user = api.json("user");
        String pass = com.rsa_at_time(api.json("pass"), 30);
        String reme = api.json("reme", "0");

        check.reset();

        check.validate("", "账号", "required", user);
        check.validate("", "密码", "required|min_length,6", pass);

        if (check.result) {
            Map<String, String> db_user = user_m.db.find("id,password,disabled", "name", user);

            if (db_user.size() == 0) return api.err("用户不存在");
            if (db_user.get("disabled").equals("1")) return api.err("账号被禁用");

            String password = com.salt_hash(pass, db_user.get("password"));

            if (!password.equals(db_user.get("password"))) return api.err("密码错误");

            return api.put(user_m.login(db_user.get("id"), reme));
        } else {
            api.set("err", check.errors);

            return api.err(check.error);
        }
    }

    @RequestMapping("/logout")
    public Map<String, Object> logout() {
        user_m.del_session();

        return api.msg("退出成功");
    }

    @RequestMapping("/current")
    public Map<String, Object> current() {
        String user_id = user_m.get_session("id");

        Map<String, Object> item = user_m.item(user_id);

        if (item.size() > 0) {
            return api.put("0001", item);
        } else {
            return api.err("没有数据");
        }
    }

    @RequestMapping("/get_auth")
    public Map<String, Object> get_auth() {
        Map<String, String> data = user_m.get_auth(user_m.get_session("id"));

        data.put("0", "1");

        return api.put(data);
    }

    @CheckAuth(index = "3", value = "2")
    @RequestMapping("/add")
    public Map<String, Object> add() {
        Map<String, String> data = api.json(input -> {
            if (input.get("key").equals("password")) {
                input.put("val", com.rsa_decrypt(input.get("val")));
            }
        });

        check.reset(data);

        check.validate("name", "账号", "required|is_unique,sys_user,name");
        check.validate("cname", "姓名", "required");
        check.validate("password", "密码", "required|min_length,6");

        if (check.result) {
            data.put("creator", user_m.get_session("id"));

            long user_id = user_m.add(data);

            if (user_id > 0) {
                return api.msg("用户创建成功", user_m.item(Long.toString(user_id)));
            } else {
                return api.err("用户创建失败");
            }
        } else {
            api.set("err", check.errors);

            return api.err(check.error);
        }
    }

    @RequestMapping("/mod")
    public Map<String, Object> mod() {
        String user_id = api.json("user_id", user_m.get_session("id"));

        if (!user_id.equals(user_m.get_session("id")) && !com.check_auth("3", "2")) return api.err("您没有权限");

        Map<String, String> data = api.json(input -> {
            if (input.get("key").equals("user_id")) input.put("key", "");
        });

        check.reset(data);

        check.validate("name", "账号", "required|is_unique,sys_user,name#" + user_id, "@@NULL");
        check.validate("cname", "姓名", "required", "@@NULL");

        if (check.result) {
            if (user_m.mod(user_id, data) > 0) {
                if (user_id.equals(user_m.get_session("id"))) api.set("code", "0001");

                return api.put(user_m.item(user_id), "用户信息修改成功");
            } else {
                return api.err("没有信息被修改");
            }
        } else {
            api.set("err", check.errors);

            return api.err(check.error);
        }
    }

    @RequestMapping("/set_password")
    public Map<String, Object> set_password() {
        String user_id = api.json("user_id");

        String newpword = com.rsa_decrypt(api.json("newpword"));

        if(user_id.equals("")) {
            user_id = user_m.get_session("id");

            String oldpword = com.rsa_at_time(api.json("oldpword"), 30);
            String password = user_m.db.field("password", user_id);

            if(!password.equals(com.salt_hash(oldpword, password))) return api.err("密码错误");

            if(newpword.equals(oldpword)) return api.err("新密码不能和旧密码一样");
        } else {
            if (!com.check_auth("3", "2")) return api.err("您没有权限");
        }

        check.reset();

        if(!check.required(newpword)) return api.err("新密码不能为空");
        if(!check.min_length(newpword, 6)) return api.err("新密码至少6位");

        user_m.db.update(user_id, "password", com.salt_hash(newpword));

        return api.msg("密码修改成功");
    }

    @RequestMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> map = com.map(api.json(), "role_id");

        if (!api.json("__search").equals("")) {
            map.put("like#name,cname", api.json("__search"));
        }

        if (!api.json("role_id").equals("")) {
            List<Map<String, String>> user_role_list = user_m.db.table("sys_user_role").read("*", "in#role_id", api.json("role_id").replace("|", ","));

            if (user_role_list.size() > 0) {
                map.put("in#id", com.join(user_role_list, "user_id", ","));
            } else {
                return api.err("没有数据");
            }
        }

        Map<String, Long> line = api.line(20);

        if (line.get("rows") == 0) line.put("rows", user_m.list_count(map));
        if (line.get("page") == 0) line.put("page", (long)Math.ceil((double)line.get("rows") / (double)line.get("size")));

        map.put("#limit", line.get("size") + "," + ((line.get("page") - 1) * line.get("size")));

        List<Map<String, Object>> list = user_m.list(map);

        api.set_line(line);

        if (list.size() > 0) {
            return api.put(list);
        } else {
            return api.err("没有数据");
        }
    }

    @RequestMapping("/item")
    public Map<String, Object> item() {
        String user_id = api.json("user_id");

        return api.put(user_m.item(user_id));
    }

    @RequestMapping("/role")
    public Map<String, Object> role() {
        String user_id = api.json("user_id");

        List<Map<String, String>> role = user_m.db.table("sys_user_role").read("role_id", "user_id", user_id);

        List<Map<String, Object>> list = role_m.list(new LinkedHashMap<>());

        if (list.size() > 0) {
            for (Map<String, Object> item : list) {
                Map<String, String> role_map = new LinkedHashMap<>();

                role_map.put("role_id", item.get("id").toString());

                item.put("checked", role.contains(role_map) ? "1" : "0");
            }

            return api.put(list);
        } else {
            return api.err("没有数据");
        }
    }
}
