package com.sox.api.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sox.api.service.Com;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConfModel {
    @Autowired
    public Com com;

    public Db db;

    @Autowired
    public void UserServiceImpl(Db db) {
        this.db = db.clone();
        this.db.table = "sys_conf";
    }

    public Map<String, String> list = null; // new LinkedHashMap<>();

    public void ini() {
        // 应用初始化步骤（com.sox.api.listener.InitializeListener）需要优先业务代码执行
        // 由于容器化技术，这一步是为了防止数据库中相关数据表尚未创建引发查询错误

        if (list != null) return;

        list = new LinkedHashMap<>();

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("#field", "conf_id,content");
        map.put("#order", "conf_id,asc");

        for (Map<String, String> item : this.db.read(map)) {
            list.put(item.get("conf_id"), item.get("content"));
        }
    }

    public String get(String key, String... def) {
        this.ini();

        return list.getOrDefault(key, def.length > 0 ? def[0] : "");
    }

    public JSONObject get_obj(String key) {
        String val = this.get(key);

        return val.equals("") ? new JSONObject() : JSONObject.parseObject(val);
    }

    public JSONArray get_arr(String key) {
        String val = this.get(key);

        return val.equals("") ? new JSONArray() : JSONArray.parseArray(val);
    }

    public Long set(String key, String val) {
        this.ini();

        String conf_id = db.field("conf_id", "conf_id", key);

        Long num;

        if (!conf_id.equals("")) {
            num = db.update(1, "conf_id", key, "content", val);
        } else {
            num = db.create("conf_id", key, "content", val);
        }

        if (num > 0) {
            list.put(key, val);
        }

        return num;
    }

    public void clean() {
        list = null;
    }

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

    public Map<String, Object> item(String conf_id) {
        Map<String, String> data = db.find(field, "conf_id", conf_id);

        return this.for_return(data);
    }

    public Long add(String key, String val) {
        return db.create("conf_id", key, "content", val);
    }

    public Long mod(String key, String val) {
        return db.update(1, "conf_id", key, "content", val);
    }

    public Long del(String key) {
        return db.delete("conf_id", key);
    }
}
