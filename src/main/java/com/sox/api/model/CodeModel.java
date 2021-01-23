package com.sox.api.model;

import com.sox.api.service.Com;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@EnableAutoConfiguration
public class CodeModel {
    public Db db;
    public Com com;

    @Autowired
    public void UserServiceImpl(Db db, Com com) {
        this.db = db.clone();
        this.db.table = "code_value";

        this.com = com;
    }

    public Map<String, Map<String, String>> state = new HashMap<>();

    public String state(String index, String value, int level, String... def) {
        if (value.equals("")) return def.length == 0 ? "" : def[0];

        String key = index + "_" + level;

        if (state.get(key) == null) {
            Map<String, String> state_item = new HashMap<>();

            Map<String, Object> map = new HashMap<>();

            map.put("#field", "value,state");

            map.put("index", index);

            if (level > 0) {
                map.put("level", level + "");
            }

            List<Map<String, String>> state_list = db.read(map);

            for (Map<String, String> item : state_list) {
                state_item.put(item.get("value"), item.get("state"));
            }

            state.put(key, state_item);
        }

        return state.get(key).getOrDefault(value, def.length == 0 ? "" : def[0]);
    }

    public void clean(String... index) {
        for (String key : state.keySet()) {
            if (index.length == 0 || ("," + index[0] + ",").contains("," + key + ",")) state.remove(key);
        }
    }
}
