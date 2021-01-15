package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.UserModel;
import com.sox.api.service.Api;
import com.sox.api.service.Check;
import com.sox.api.service.Com;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

@RestController
@EnableAutoConfiguration
@CheckLogin(except = "login,test")
@RequestMapping("/user")
public class UserController {
    @Autowired
    private Api api;

    @Autowired
    private Com com;

    @Autowired
    private UserModel user_m;

    @Transactional
    @Modifying
    @RequestMapping("/login")
    public Map<String, Object> login() {
        Check check = new Check();

        String user = api.json("user");
        String pass = api.json("pass");
        String reme = api.json("reme", "0");

        check.required(user, "#账号");
        check.required(pass, "#密码");

        if (check.result) {
            Map<String, String> db_user = user_m.db.find("id,password,disabled", "name", user);

            if (db_user == null) return api.err("用户不存在");

            pass = com.rsa_decrypt(pass);

            String[] pass_a = pass.split("@");

            if (pass_a.length < 2) return api.err("密码错误");

            if (Math.abs(Integer.parseInt(pass_a[1]) - com.time()) > 30) return api.err("系统时间校验失败");

            String password = com.salt_hash(pass_a[0], db_user.get("password"), 8);

            if (!password.equals(db_user.get("password"))) return api.err("密码错误");

            return api.put(user_m.login(db_user.get("id"), reme));
        } else {
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

        Object user = user_m.item(user_id);

        return api.put(user);
    }

    @RequestMapping("/get_auth")
    public Map<String, Object> get_auth() {
        Map<String, String> data = user_m.get_auth(user_m.get_session("id"));

        data.put("0", "1");

        return api.put(data);
    }

    @RequestMapping("/list")
    public Map<String, Object> list() {
        Object list = user_m.list();

        return api.put(list);
    }

    @RequestMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> data = new HashMap<>();

        data.put("current_user", this.current().get("data"));
        data.put("user_list", this.list().get("data"));

        return api.put(data);
    }
}
