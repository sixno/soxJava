package com.sox.api.model;

import com.sox.api.service.Com;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Component
@EnableAutoConfiguration
public class UserModel {
    public Db db;
    public Com com;

    @Autowired
    public void UserServiceImpl(Db db, Com com) {
        this.db = db.clone();
        this.db.table = "sys_user";

        this.com = com;
    }

    public String sess_seed = "fDS48V5G";

    public String auth_table = "sys_user_auth";

    public String field = "id,lv,sex,name,cname,avatar,company,dept,post,login_time,create_time,update_time,inviter,disabled,manual";

    public Map<String, Object> for_return(Map<String, String> data) {
        if (data == null) return null;

        Map<String, Object> item = new HashMap<>();

        Set<String> keys = data.keySet();

        for (String key :keys){
            item.put(key, data.get(key));

            if (key.equals("sex")) {
                switch (data.get("sex")) {
                    case "1":
                        item.put("sex_state", "男");
                        break;
                    case "2":
                        item.put("sex_state", "女");
                        break;
                    default:
                        item.put("sex_state", "未知");
                        break;
                }
            }
        }

        return item;
    }

    public List<Map<String, String>> list() {
        Map<String, Object> map = new HashMap<>();

        map.put("#field", "id,name");
        map.put("#order", "id,asc");
        map.put("#limit", "5,0");

        map.put("id >=", "10000005");

        List<Map<String, String>> list = db.read(map);

        return list;
    }

    public Map<String, Object> item(String id) {
        Map<String, String> data = this.db.find(this.field, id);

        return this.for_return(data);
    }

    public Map<String, Object> login(String id,String remember) {
        Map<String, Object> user = this.item(id);

        if (user != null) {
            if (user.get("disabled").toString().equals("1")) return null;

            String time = com.time() + "";

            this.set_session(id, time, remember);

            this.db.update(id, new HashMap<String, String>(){ { put("login_time", time); } });

            user.put("login_time", time);

            return user;
        }

        return null;
    }

    public void set_session(String id, String line, String remember) {
        HttpServletResponse response = Objects.requireNonNull(((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse());

        response.setHeader("Access-Control-Expose-Headers", "Content-Type, Token");
        response.setHeader("Token", (remember.equals("") ? "0" : remember.substring(0, 1)) + com.str_encrypt(id + "," + line, this.sess_seed));
    }

    public String get_session(String key) {
        Map<String, String> sess_data = new HashMap<>();

        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        String token = request.getHeader("token");

        if (token != null && !token.equals("")) {
            String[] token_arr = com.str_decrypt(token.substring(1), this.sess_seed).split(",");

            if (token_arr.length == 2){
                sess_data.put("id", token_arr[0]);
                sess_data.put("line", token_arr[1]);
                sess_data.put("remember", token.substring(0,1));
            }
        }

        return sess_data.getOrDefault(key, "");
    }

    public void del_session() {
        HttpServletResponse response = Objects.requireNonNull(((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse());

        response.setHeader("Access-Control-Expose-Headers", "Content-Type, Token");
        response.setHeader("Token", "");
    }

    public Map<String, String> get_auth(String user_id) {
        Map<String, String> auth = new HashMap<>();

        Map<String, Object> map = new HashMap<>();

        map.put("#field", "auth_id,value");
        map.put("user_id", user_id);

        List<Map<String, String>> auth_list = this.db.table(this.auth_table).read(map);

        if (auth_list != null) {
            for (Map<String, String> auth_item : auth_list) {
                auth.put(auth_item.get("auth_id"), auth_item.get("value"));
            }
        }

        return auth;
    }
}
