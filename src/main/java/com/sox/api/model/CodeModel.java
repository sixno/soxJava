package com.sox.api.model;

import com.sox.api.service.Com;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CodeModel {
    @Autowired
    public Com com;

    public Db db;

    @Autowired
    public void UserServiceImpl(Db db) {
        this.db = db.clone();
        this.db.table = "code_value";
    }

    public String field = "*";

    public Map<String, Map<String, Map<String, String>>> state = new LinkedHashMap<>();

    public String state_ini(String index, int level) {
        String key = index + "_" + level;

        if (state.get(key) == null) {
            Map<String, Map<String, String>> state_item = new LinkedHashMap<>();

            Map<String, Object> map = new LinkedHashMap<>();

            map.put("#field", "id,value,state,extra,prev,sort");
            map.put("#order", "id,asc");

            map.put("index", index);

            if (level > 0) {
                map.put("level", level + "");
            }

            List<Map<String, String>> state_list = db.read(map);

            for (Map<String, String> item : state_list) {
                Map<String, String> state_item_map = new LinkedHashMap<>();

                state_item_map.put("state", item.get("state"));
                state_item_map.put("extra", item.get("extra"));
                state_item_map.put("prev", item.get("prev"));
                state_item_map.put("sort", item.get("sort") + "." + item.get("id"));

                state_item.put(item.get("value"), state_item_map);
            }

            state.put(key, state_item);
        }

        return key;
    }

    public String state(String index, String value, int level, String... def) {
        if (value.equals("")) return def.length == 0 ? "" : def[0];

        Map<String, String> def_map = new LinkedHashMap<>();

        def_map.put("state", def.length == 0 ? "" : def[0]);
        def_map.put("prev", "");

        String key = this.state_ini(index, level);

        return state.get(key).getOrDefault(value, def_map).get("state");
    }

    public List<Map<String, String>> state_list(String index, int level, String... prev) {
        List<Map<String, String>> screen_result = new ArrayList<>();

        if (index.equals("")) return screen_result;

        String key = this.state_ini(index, level);

        Map<String, Map<String, String>> result = state.get(key);

        List<Map.Entry<String, Map<String, String>>> list = new ArrayList<>(result.entrySet());

        list.sort(Comparator.comparing(o -> Float.parseFloat(o.getValue().get("sort")))); // 升序排列

        for (Map.Entry<String,  Map<String, String>> mapping : list) {
            Map<String, String> code_state = mapping.getValue();

            code_state.put("value", mapping.getKey());

            boolean prev_0 = true;
            boolean prev_1 = true;

            if (prev.length > 0 && !prev[0].equals("")) {
                for (String code_prev : code_state.get("prev").split("\\|")) {
                    prev_0 = ("|" + prev[0] + "|").contains("|" + code_prev + "|") || code_prev.equals("");

                    if (prev_0) break;
                }
            }

            if (prev.length > 1 && !prev[1].equals("")) {
                for (String code_extra : code_state.get("extra").split("\\|")) {
                    prev_1 = ("|" + prev[1] + "|").contains("|" + code_extra + "|");

                    if (prev_1) break;
                }
            }

            if (prev_0 && prev_1) {
                screen_result.add(code_state);
            }
        }

        return screen_result;
    }

    public void clean(String... index) {
        if (index.length == 0) {
            state = new LinkedHashMap<>();
        } else {
            for (String key : index) {
                if(state.get(key) != null) state.put(key, null);
            }
        }
    }

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
        String time = com.time().toString();

        data.put("create_time", time);
        data.put("update_time", time);

        if (data.get("prev") != null) {
            Map<String, String> prev = db.find("id,index,value,prev,next,level", "index", data.getOrDefault("index", ""), "value", data.getOrDefault("prev", ""));

            if (prev.size() > 0) {
                // 码值层级请必须按照层级规范构造，如一级为A，则二级为A1，三级为A11，即下级码值必须以父级码值打头，否则将造成前端表单构造器发生不可控情形。

                if(prev.get("next").equals("0")) db.update(prev.get("id"), "next", "1");

                data.put("level", (Integer.parseInt(prev.get("level")) + 1) + "");
            } else {
                // 当码值层级为1时可以使用其他索引的码值作为父级
                // 此举为了使前端使表单构造器更加灵活
                // 若有父级，此时码值必须以父级码值打头

                data.put("level", "1");
            }
        }

        if (data.getOrDefault("sort", "0").equals("0") || data.getOrDefault("sort", "").equals("")) {
            String sort = db.field("sort", "sort,desc;id,desc", "index", data.getOrDefault("index", ""));

            int sort_int = sort.equals("") ? 0 : Integer.parseInt(sort);

            data.put("sort", (sort_int + 10) + "");
        }

        Long result = db.create(data);

        this.clean(data.get("index") + "_0", data.get("index") + "_" + data.getOrDefault("level", "1"));

        return result;
    }

    public Long mod(String code_id, Map<String, String> data) {
        String time = com.time() + "";

        data.put("update_time", time);

        Map<String, String> db_code = db.find("id,index,value,extra", "id", code_id);

        if (data.get("prev") != null) {
            Map<String, String> prev = db.find("id,index,value,prev,next,level", "index", data.getOrDefault("index", ""), "value", data.getOrDefault("prev", ""));

            if (prev.size() > 0) {
                if(prev.get("next").equals("0")) db.update(prev.get("id"), "next", "1");

                data.put("level", (Integer.parseInt(prev.get("level")) + 1) + "");
            } else {
                data.put("level", "1");
            }
        }

        if (db_code.get("index").equals("rule_alias") && data.getOrDefault("index", db_code.get("index")).equals(db_code.get("index")) && !data.getOrDefault("extra", "").equals("")) {
            db.table("app_warn_rule").update(1, "alias", db_code.get("value"), "work", data.get("extra"));
        }

        Long result = db.update(code_id, data);

        if (result > 0) {
            Map<String, String> code = db.find("index,level", "id", code_id);

            if (code != null) {
                this.clean(code.get("index") + "_0", code.get("index") + "_" + code.getOrDefault("level", "1"));
            }
        }

        return result;
    }

    public Long del(String code_id) {
        Map<String, String> code = db.find("index,level", "id", code_id);

        Long result = db.delete(code_id);

        if (result > 0 && code != null) {
            this.clean(code.get("index") + "_0", code.get("index") + "_" + code.getOrDefault("level", "1"));
        }

        return result;
    }
}
