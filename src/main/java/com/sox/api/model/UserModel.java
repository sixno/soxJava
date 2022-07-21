package com.sox.api.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sox.api.service.Com;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Component
public class UserModel {
    @Value("${sox.super_user}")
    private String super_user;

    @Autowired
    public Com com;

    @Autowired
    public CodeModel code_m;

    @Autowired
    public AuthModel auth_m;

    @Autowired
    public RoleModel role_m;

    public Db db;

    @Autowired
    public void UserServiceImpl(Db db) {
        this.db = db.clone();
        this.db.table = "sys_user";
    }

    public String sess_seed = "fDS48V5G";

    public String auth_table = "sys_user_auth";

    public String field = "sys_user.id,lv,sex,name,cname,avatar,company,dept,post,phone,email,inviter,creator,sys_user.create_time,active_time,disabled,auto_syn,sys_user.update_time";

    Map<String, String> disabled_state = new LinkedHashMap<String, String>(){{
        put("0", "启用");
        put("1", "禁用");
    }};

    public Map<String, Object> for_return(Map<String, String> data) {
        Map<String, Object> item = new LinkedHashMap<>();

        if (data.size() == 0) return item;

        for (String key : data.keySet()){
            item.put(key, data.get(key));

            if (key.equals("disabled")) item.put("disabled_state", disabled_state.getOrDefault(data.get(key), ""));

            if (key.equals("sex")) item.put("sex_state", code_m.state("sex", data.get(key), 0));

            if (key.equals("post")) item.put("post_state", code_m.state("post", data.get(key), 0));

            if (key.equals("dept")) {
                ArrayList<String> d_no = new ArrayList<>();
                ArrayList<String> dept = new ArrayList<>();
                String is_through = "0";
                String dept_no_last = "";
                String dept_state_last = "";

                if (!data.get(key).equals("")) {
                    for (String dept_no : data.get(key).split("/")) {
                        dept_no_last = dept_no;
                        dept_state_last = code_m.state("dept", dept_no, 0);

                        d_no.add(dept_no_last);
                        dept.add(dept_state_last);
                    }

                    Map<String, String> dept_state = code_m.state.get("dept_0").get(dept_no_last);

                    if (dept_state != null && dept_state.get("extra").equals("1")) is_through = "1";
                }

                item.put("dept_no", d_no);
                item.put("dept_state", dept);
                item.put("is_through", is_through);
                item.put("dept_no_last", dept_no_last);
                item.put("dept_state_last", dept_state_last);
            }

            if (key.equals("creator")) {
                Map<String, String> creator_data = new LinkedHashMap<>();

                if (!data.get(key).equals("0")) {
                    creator_data = db.find(field, "id", data.get(key));
                }

                item.put("creator_data", creator_data);
            }

            item.put("super", data.get("id").equals(super_user) ? "1" : "0");
        }

        String role_state = "";

        List<Map<String, String>> role_ids = db.table("sys_user_role").read("role_id", "user_id", data.get("id"));

        if (role_ids.size() > 0) {
            for (Map<String, String> role_item : db.table("sys_role").read("*", "in#id", com.join(role_ids, "role_id", ","))) {
                role_state += (role_state.equals("") ? "" : "、") + role_item.get("title");
            }
        }

        item.put("role_state", role_state);

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

        if (data.get("password") != null) {
            data.put("password", com.salt_hash(data.get("password")));
        }

        return db.create(data);
    }

    public Long mod(String user_id, Map<String, String> data) {
        return db.update(user_id, data);
    }

    public Map<String, Object> login(String id, String remember, String... line) {
        Map<String, Object> user = this.item(id);

        if (user.size() > 0) {
            if (user.get("disabled").toString().equals("1")) return null;

            String time = line.length == 0 ? com.time() + "" : line[0];

            this.set_session(id, time, remember);

            db.update(id, "active_time", time);

            user.put("active_time", time);

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
        Map<String, String> sess_data = new LinkedHashMap<>();

        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        String token = request.getHeader("token");

        if (token == null || token.equals("")) {
            token = request.getParameter("token");

            if (token != null) {
                token = com.rsa_at_time(token, 60);
            }
        }

        if (token != null && !token.equals("")) {
            String[] token_arr = com.str_decrypt(token.substring(1), this.sess_seed).split(",");

            if (token_arr.length == 2) {
                sess_data.put("id", token_arr[0]);
                sess_data.put("line", token_arr[1]);
                sess_data.put("remember", token.substring(0, 1));

                if (com.time() - Integer.parseInt(token_arr[1]) > 3600) {
                    sess_data.put("line", com.time() + "");

                    this.login(token_arr[0], token.substring(0, 1), sess_data.get("line"));
                }
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
        Map<String, String> auth = new LinkedHashMap<>();

        Map<String, Object> map = new LinkedHashMap<>();

        if (user_id.equals(super_user)) {
            Map<String, Object> auth_map = new LinkedHashMap<>();

            auth_map.put("#order", "sort,asc;id,asc");

            List<Map<String, Object>> auth_list = auth_m.list(auth_map);

            for (Map<String, Object> auth_item : auth_list) {
                String auth_value = "";

                if (auth_item.get("option").equals("")) {
                    auth_value = "1";
                } else {
                    if (auth_item.get("option") instanceof JSONArray) {
                        for (Object auth_item_obj : (JSONArray) auth_item.get("option")) {
                            if (auth_item_obj instanceof JSONObject) {
                                JSONObject auth_item_map = (JSONObject) auth_item_obj;

                                String auth_item_value = auth_item_map.getOrDefault("value", "").toString();

                                auth_value += auth_item_value.equals("") ? "" : (auth_value.equals("") ? "" : ",") + auth_item_value;
                            }
                        }
                    }
                }

                auth.put(auth_item.get("id").toString(), auth_value);
            }
        } else {
            map.put("#field", "auth_id,value");
            map.put("user_id", user_id);

            List<Map<String, String>> auth_list = db.table(auth_table).read(map);

            if (auth_list != null) {
                for (Map<String, String> auth_item : auth_list) {
                    auth.put(auth_item.get("auth_id"), auth_item.get("value"));
                }
            }
        }

        return auth;
    }

    public void act_role(String user_id) {
        Map<String, String> user_auth = this.get_auth(user_id);

        List<Map<String, String>> role_list = db.table("sys_user_role").read("role_id", "user_id", user_id);

        Map<String, String> auth = new LinkedHashMap<>();

        for (Map<String, String> role_item : role_list) {
            Map<String, String> role_auth = role_m.get_auth(role_item.get("role_id"));

            for (String key : role_auth.keySet()) {
                if (auth.get(key) == null || auth.get(key).equals("")) {
                    auth.put(key, role_auth.get(key));
                } else {
                    List<String> auth_list = new ArrayList<>(Arrays.asList(auth.get(key).split(",")));

                    for (String auth_item : role_auth.get(key).split(",")) {
                        if (!auth_item.equals("") && !auth_list.contains(auth_item)) {
                            auth_list.add(auth_item);
                        }
                    }

                    auth_list.sort(Comparator.comparing(Integer::parseInt));

                    auth.put(key, com.join(auth_list, ","));
                }
            }
        }

        for (String key : user_auth.keySet()) {
            if (auth.get(key) == null) db.table("sys_user_auth").delete("user_id", user_id, "auth_id", key);
        }

        for (String key : auth.keySet()) {
            if (user_auth.get(key) == null) {
                db.table("sys_user_auth").create("user_id", user_id, "auth_id", key, "value", auth.get(key));
            } else if(!user_auth.get(key).equals(auth.get(key))) {
                db.table("sys_user_auth").update(2,"user_id", user_id, "auth_id", key, "user_id", user_id, "auth_id", key, "value", auth.get(key));
            }
        }
    }
}
