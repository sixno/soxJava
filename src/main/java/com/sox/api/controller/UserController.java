package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.UserModel;
import com.sox.api.service.Api;
import com.sox.api.service.Check;
import com.sox.api.service.Com;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@EnableAutoConfiguration
@CheckLogin(except = "login")
@RequestMapping("/user")
public class UserController {
    @Autowired
    private Api api;

    @Autowired
    private Com com;

    @Autowired
    private UserModel user_m;

    @RequestMapping("/login")
    public Map<String, Object> login() {
        Check check = new Check();

        String user = api.json("user");
        String pass = com.rsa_by_time(api.json("pass"), 30);
        String reme = api.json("reme", "0");

        check.required(user, "#账号");

        check.required(pass, "#密码");
        check.min_length(pass, 6, "#密码,6");

        if (check.result) {
            Map<String, String> db_user = user_m.db.find("id,password,disabled", "name", user);

            if (db_user == null) return api.err("用户不存在");

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
            return api.put(item);
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

    @RequestMapping("/mod")
    public Map<String, Object> mod() {
        String user_id = api.json("user_id", user_m.get_session("id"));

        if (!user_id.equals(user_m.get_session("id")) && !com.check_auth("3", "2")) return api.err("您没有权限");

        Map<String, String> data = new HashMap<>();

        for (String key : api.json().keySet()) {
            if (!key.equals("user_id")) data.put(key, api.json(key));
        }

        if (user_m.mod(user_id, data) > 0) {
            return api.put(user_m.item(user_id), "用户信息修改成功");
        } else {
            return api.err("没有信息被修改");
        }
    }

    @RequestMapping("/set_password")
    public Map<String, Object> set_password() {
        String user_id = api.json("user_id");

        String newpword = com.rsa_decrypt(api.json("newpword"));

        if(user_id.equals("")) {
            user_id = user_m.get_session("id");

            String oldpword = com.rsa_by_time(api.json("oldpword"), 30);
            String password = user_m.db.find("password#" + user_id);

            if(!password.equals(com.salt_hash(oldpword, password))) return api.err("密码错误");

            if(newpword.equals(oldpword)) return api.err("新密码不能和旧密码一样");
        } else {
            if (!com.check_auth("3", "2")) return api.err("您没有权限");
        }

        Check check = new Check(false);

        if(!check.required(newpword)) return api.err("新密码不能为空");
        if(!check.min_length(newpword, 6)) return api.err("新密码至少6位");

        user_m.db.update(user_id, "password", com.salt_hash(newpword));

        return api.msg("密码修改成功");
    }

    @RequestMapping("/list")
    public Map<String, Object> list() {
        Map<String, Integer> line = api.line(20);

        Map<String, Object> map = new HashMap<>();

        map.put("#limit", line.get("size") + "," + ((line.get("page") - 1) * line.get("size")));

        if (line.get("rows") == 0) line.put("rows", user_m.list_count(map));

        Object list = user_m.list(map);

        api.set_line(line);

        return api.put(list);
    }

    @RequestMapping("/item")
    public Map<String, Object> item() {
        String user_id = api.json("user_id");

        return api.put(user_m.item(user_id));
    }
}
