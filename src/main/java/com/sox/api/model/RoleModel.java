package com.sox.api.model;

import com.sox.api.service.Com;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RoleModel {
    @Autowired
    public Com com;

    public Db db;

    @Autowired
    public void UserServiceImpl(Db db) {
        this.db = db.clone();
        this.db.table = "sys_role";
    }

    public String auth_table = "sys_role_auth";

    public String field = "*";

    public Map<String, Object> for_return(Map<String, String> data) {
        Map<String, Object> item = new LinkedHashMap<>();

        for (String key : data.keySet()) {
            item.put(key, data.get(key));
        }

        return item;
    }

    public List<Map<String, Object>> list(Map<String, Object> map, int... count) {
        List<Map<String, Object>> list = new ArrayList<>();

        map.putIfAbsent("#field", field);
        map.putIfAbsent("#order", "id,asc");

        if (count.length == 1 && count[0] == 1) {
            return db.total(map);
        }

        for (Map<String, String> item: db.read(map)) {
            list.add(this.for_return(item));
        }

        return list;
    }

    public Long list_count(Map<String, Object> map) {
        return Long.parseLong(this.list(map, 1).get(0).get("count").toString());
    }

    public Map<String, Object> item(String id) {
        Map<String, String> data = db.find(field, id);

        return this.for_return(data);
    }

    public Long add(Map<String, String> data) {
        String time = com.time() + "";

        data.put("create_time", time);
        data.put("update_time", time);

        return db.create(data);
    }

    public Long mod(String role_id, Map<String, String> data) {
        return db.update(role_id, data);
    }

    public Long del(String role_id) {
        return db.delete(role_id);
    }

    public Map<String, String> get_auth(String role_id) {
        Map<String, String> auth = new LinkedHashMap<>();

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("#field", "auth_id,value");
        map.put("role_id", role_id);

        List<Map<String, String>> auth_list = db.table(auth_table).read(map);

        if (auth_list != null) {
            for (Map<String, String> auth_item : auth_list) {
                auth.put(auth_item.get("auth_id"), auth_item.get("value"));
            }
        }

        return auth;
    }

    public void set_auth(String role_id, String auth_id, String value)
    {
        Map<String, String> role_data = db.table("sys_role_auth").find("id,value", "role_id", role_id, "auth_id", auth_id);

        if (role_data.size() == 0) {
            db.table("sys_role_auth").create("role_id", role_id, "auth_id", auth_id, "value", value);
        } else if(!role_data.get("value").equals(value)) {
            db.table("sys_role_auth").update(role_data.get("id"), "role_id", role_id, "auth_id", auth_id, "value", value);
        }
    }

    public void bind(String user_id, String role_id)
    {
        String user_role_key = db.table("sys_user_role").field("id", "user_id", user_id, "role_id", role_id);

        if (user_role_key.equals("")) {
            db.table("sys_user_role").create("user_id", user_id, "role_id", role_id);
        }
    }

    public void unbind(String user_id, String role_id)
    {
        Long result = db.table("sys_user_role").delete("user_id", user_id, "role_id", role_id);

        if (result > 0) {
            System.out.println("unbind role");
        }
    }
}
