package com.sox.api.model;

import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TableModel {
    @Autowired
    public CodeModel code_m;

    @Autowired
    public Db db;

    public Map<String, Object> for_return(Map<String, String> data, List<Map<String, String>> field_list) {
        Map<String, Object> item = new LinkedHashMap<>();

        Map<String, Map<String, String>> field_check = new LinkedHashMap<>();

        for (Map<String, String> field_item : field_list) {
            field_check.put(field_item.get("name"), field_item);
        }

        for (String key : data.keySet()) {
            item.put(key, data.get(key));

            if (field_check.get(key) != null && (field_check.get(key).get("show").equals("2") || field_check.get(key).get("show").equals("3"))) {
                item.put(key + "_state", code_m.state(field_check.get(key).get("code_index"), data.get(key), Integer.parseInt(field_check.get(key).get("code_level"))));
            }
        }

        return item;
    }

    public List<Map<String, Object>> list(Map<String, Object> map, int... count) {
        List<Map<String, Object>> list = new ArrayList<>();

        if (map.get("#table") == null) return list;

        map.putIfAbsent("#field", "*");
        map.putIfAbsent("#order", "id,asc");

        if (count.length == 1 && count[0] == 1) {
            return db.table(map.get("#table").toString()).total(map);
        }

        List<Map<String, String>> field_list = db.table("set_base_field").read("*", "sort,asc;id,asc", "table", map.get("#table").toString());

        for (Map<String, String> item: db.table(map.get("#table").toString()).read(map)) {
            list.add(this.for_return(item, field_list));
        }

        return list;
    }

    public Long list_count(Map<String, Object> map) {
        return Long.parseLong(this.list(map, 1).get(0).get("count").toString());
    }

    public Map<String, Object> item(String table, String id) {
        Map<String, String> data = db.table(table).find("*", id);

        List<Map<String, String>> field_list = db.table("set_base_field").read("*", "sort,asc;id,asc", "table", table);

        return this.for_return(data, field_list);
    }

    public Long add(String table, Map<String, String> data) {
        return db.table(table).create(data);
    }

    public Long mod(String table, String id, Map<String, String> data) {
        return db.table(table).update(id, data);
    }

    public Long del(String table, String id) {
        return db.table(table).delete(id);
    }
}
