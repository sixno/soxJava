package com.sox.api.model;

import com.sox.api.quartz.utils.QuartzManager;
import com.sox.api.service.Com;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@EnableAutoConfiguration
public class TaskModel {
    public Db db;
    public Com com;
    public QuartzManager quartzManager;

    @Autowired
    public void UserServiceImpl(Db db, Com com, QuartzManager quartzManager) {
        this.db = db.clone();
        this.db.table = "sys_task";

        this.com = com;

        this.quartzManager = quartzManager;
    }

    public Map<String, String> status_state = new HashMap<String, String>(){
        {
            put("0", "停止");
            put("1", "运行");
        }
    };

    public String state(String index, Map<String, String> state, String... def) {
        return state.getOrDefault(index, def.length == 0 ? "" : def[0]);
    }

    public Map<String, Object> for_return(Map<String, String> data) {
        Map<String, Object> item = new HashMap<>();

        for (String key : data.keySet()){
            item.put(key, data.get(key));

            if (key.equals("status")) item.put("status_state", this.state(data.get(key), status_state, "未知"));
        }

        return item;
    }

    public List<Map<String, Object>> list(Map<String, Object> map, int... count) {
        List<Map<String, Object>> list = new ArrayList<>();

        map.putIfAbsent("#field", "*");
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
        Map<String, String> data = db.find("*", id);

        return this.for_return(data);
    }

    public int add(Map<String, String> data) {
        String time = com.time() + "";

        data.put("create_time", time);
        data.put("update_time", time);

        int id = db.create(data);

        if (id > 0) {
            if (data.get("manual").equals("0")) {
                quartzManager.add_job(data.get("name"), data.get("group"), data.get("path"), data.get("cron_exp"), "__id:" + data.get("id") + ";" + data.get("arg"));
            }
        }

        return id;
    }

    public int mod(String task_id, Map<String, String> data) {
        data.put("update_time", com.time() + "");

        int num = db.update(task_id, data);

        if (num > 0) {
            data = db.find("*", task_id);

            if (data.get("manual").equals("0")) {
                quartzManager.add_job(data.get("name"), data.get("group"), data.get("path"), data.get("cron_exp"), "__id:" + data.get("id") + ";" + data.get("arg"));
            }
        }

        return num;
    }

    public int del(String task_id) {
        Map<String,String> data = db.find("*", task_id);

        if (data.size() > 0) {
            quartzManager.del_job(data.get("name"), data.get("group"));
        }

        return db.delete(task_id);
    }

    public void run(String task_id) {
        Map<String,String> data = db.find("*", task_id);

        quartzManager.add_job(data.get("name"), data.get("group"), data.get("path"), data.get("cron_exp"), "__id:" + data.get("id") + ";" + data.get("arg"));
    }
}
