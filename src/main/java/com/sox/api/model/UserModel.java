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
    public CodeModel code;

    @Autowired
    public void UserServiceImpl(Db db, Com com, CodeModel code) {
        this.db = db.clone();
        this.db.table = "sys_user";

        this.com = com;

        this.code = code;
    }

    public String sess_seed = "fDS48V5G";

    public String auth_table = "sys_user_auth";

    public String field = "id,lv,sex,name,cname,avatar,company,dept,post,login_time,create_time,update_time,inviter,disabled,manual";

    public Map<String, Object> for_return(Map<String, String> data) {
        Map<String, Object> item = new HashMap<>();

        for (String key : data.keySet()){
            item.put(key, data.get(key));

            if (key.equals("sex")) item.put("sex_state", code.state("sex", data.get(key), 0, "未知"));
        }

        return item;
    }

    public List<Map<String, Object>> list(Map<String, Object> map, int... count) {
        List<Map<String, Object>> list = new ArrayList<>();

        map.putIfAbsent("#field", field);
        map.putIfAbsent("#order", "id,asc");

        if (count.length == 1 && count[0] == 1) {
            return db.list_count(map);
        }

        for (Map<String, String> item: db.read(map)) {
            list.add(this.for_return(item));
        }

        return list;
    }

    public int list_count(Map<String, Object> map) {
        return Integer.parseInt(this.list(map, 1).get(0).get("count").toString());
    }

    public Map<String, Object> item(String id) {
        Map<String, String> data = db.find(field, id);

        return this.for_return(data);
    }

    public int mod(String user_id, Map<String, String> data) {
        return db.update(user_id, data);
    }

    public Map<String, Object> login(String id,String remember) {
        Map<String, Object> user = this.item(id);

        if (user != null) {
            if (user.get("disabled").toString().equals("1")) return null;

            String time = com.time() + "";

            this.set_session(id, time, remember);

            db.update(id, "login_time", time);

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

        List<Map<String, String>> auth_list = db.table(auth_table).read(map);

        if (auth_list != null) {
            for (Map<String, String> auth_item : auth_list) {
                auth.put(auth_item.get("auth_id"), auth_item.get("value"));
            }
        }

        return auth;
    }
}
