package com.sox.api.model;

import com.sox.api.quartz.utils.QuartzManager;
import com.sox.api.service.Com;
import com.sox.api.service.Db;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TaskModel {
    @Value("${sox.host_id}")
    private String host_id;

    @Autowired
    public Com com;

    @Autowired
    public QuartzManager qm;

    public Db db;

    @Autowired
    public void UserServiceImpl(Db db) {
        this.db = db.clone();
        this.db.table = "sys_task";
    }

    public static Map<String, String> status = new LinkedHashMap<>();
    public static Map<String, String> last_result = new LinkedHashMap<>();
    public static Map<String, String> last_result_time = new LinkedHashMap<>();
    public static Map<String, String> last_result_update_time = new LinkedHashMap<>();

    public void set_status(String id, int interval, String... set) {
        Long time = com.time();

        String set_status = set.length > 0 ? set[0] : "0";
        String set_last_result = set.length > 1 ? set[1] : "";
        String set_last_result_time = time.toString();

        String set_last_start_time = set.length > 2 ? set[2] : "";

        if (!status.getOrDefault(id, "0").equals(set_status) || interval == 0 || time - Long.parseLong(last_result_update_time.getOrDefault(id, "0")) > interval) {
            last_result_update_time.put(id, set_last_result_time);

            Map<String, String> update_data = new LinkedHashMap<>();

            update_data.put("status", set_status);
            update_data.put("last_result", set_last_result);
            update_data.put("last_result_time", set_last_result_time);

            if (!set_last_start_time.equals("")) update_data.put("last_start_time", set_last_start_time);

            db.update(id, update_data);
        }

        status.put(id, set_status);
        last_result.put(id, set_last_result);
        last_result_time.put(id, set_last_result_time);
    }

    public void set_status(String id, String... set) {
        this.set_status(id, 60, set);
    }

    public Map<String, String> status_state = new LinkedHashMap<String, String>(){{
        put("0", "停止");
        put("1", "运行");
        put("2", "失败");
    }};

    public String state(String index, Map<String, String> state, String... def) {
        return state.getOrDefault(index, def.length == 0 ? "" : def[0]);
    }

    public Map<String, Object> for_return(Map<String, String> data) {
        Map<String, Object> item = new LinkedHashMap<>();

        if (data.size() > 0) {
            status.putIfAbsent(data.get("id"), "0");

            data.put("status", status.get(data.get("id")));

            if (last_result.get(data.get("id")) == null) {
                last_result.put(data.get("id"), data.get("last_result"));
            } else {
                data.put("last_result", last_result.get(data.get("id")));
            }

            if (last_result_time.get(data.get("id")) == null) {
                last_result_time.put(data.get("id"), data.get("last_result_time"));
            } else {
                data.put("last_result_time", last_result_time.get(data.get("id")));
            }

            if (last_result_update_time.get(data.get("id")) == null) {
                last_result_update_time.put(data.get("id"), data.get("last_result_time"));
            }
        }

        for (String key : data.keySet()) {
            item.put(key, data.get(key));

            if (key.equals("status")) item.put("status_state", this.state(data.get(key), status_state, "未知"));

            if (key.equals("cron_exp")) {
                item.put("next_scheduled_time", "0");

                if (!data.get(key).equals("")) {
                    try {
                        CronExpression cronExpression = new CronExpression(data.get(key));

                        Date date = cronExpression.getNextValidTimeAfter(new Date());

                        item.put("next_scheduled_time", (date.getTime() / 1000L) + "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return item;
    }

    public List<Map<String, Object>> list(Map<String, Object> map, int... count) {
        List<Map<String, Object>> list = new ArrayList<>();

        map.putIfAbsent("#field", "*");
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
        Map<String, String> data = db.find("*", id);

        return this.for_return(data);
    }

    public Long add(Map<String, String> data) {
        String time = com.time().toString();

        data.put("host_id", host_id);

        data.put("update_time", time);
        data.put("create_time", time);

        data.putIfAbsent("last_result", "");

        return db.create(data);
    }

    public Long mod(String task_id, Map<String, String> data) {
        data.put("update_time", com.time().toString());

        return db.update(task_id, data);
    }

    public Long del(String task_id) {
        Map<String,String> data = db.find("*", task_id);

        if (data.size() > 0) {
            qm.del_job(data.get("name"), data.get("group"));
        }

        return db.delete(task_id);
    }

    public void run(String task_id) {
        Map<String,String> data = db.find("*", task_id);

        if (data.get("type").equals("0")) {
            qm.add_job(data.get("name"), data.get("group"), data.get("path"), data.get("cron_exp"), "__id", data.get("id"), "__user_id", data.get("user_id"), data.get("arg"));
        } else {
            qm.add_job(data.get("name"), data.get("group"), "com.sox.api.quartz.task.KettleJob", data.get("cron_exp"), "__id", data.get("id"), "__user_id", data.get("user_id"), "__file", data.get("path"), data.get("arg"));
        }
    }

    public void run(String task_id, String manual) {
        Map<String,String> data = db.find("*", task_id);

        if (data.get("type").equals("0")) {
            qm.add_job("__" + manual + "::" + data.get("name"), "system::" + data.get("group"), data.get("path"), "", "__id", data.get("id"), "__user_id", data.get("user_id"), data.get("arg"));
        } else {
            qm.add_job("__" + manual + "::" + data.get("name"), "system::" + data.get("group"), data.get("path"), "", "__id", data.get("id"), "__user_id", data.get("user_id"), "__file", data.get("path"), data.get("arg"));
        }
    }

    public void end(String task_id) {
        Map<String,String> data = db.find("*", task_id);

        qm.del_job(data.get("name"), data.get("group"));
    }

    public void pause(String task_id) {
        Map<String,String> data = db.find("*", task_id);

        qm.pause_job(data.get("name"), data.get("group"));

        db.update(task_id, "status", "2");
    }

    public void start(String task_id) {
        Map<String,String> data = db.find("*", task_id);

        qm.start_job(data.get("name"), data.get("group"));

        db.update(task_id, "status", "1");
    }
}
