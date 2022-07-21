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
public class CodeSubjectModel {
    @Autowired
    public Com com;

    public Db db;

    @Autowired
    public void UserServiceImpl(Db db) {
        this.db = db.clone();
        this.db.table = "code_subject";
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

    public Long mod(String code_subject_id, Map<String, String> data) {
        String time = com.time() + "";

        data.put("update_time", time);

        return db.update(code_subject_id, data);
    }

    public Long del(String code_subject_id) {
        return db.delete(code_subject_id);
    }
}
