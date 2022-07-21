package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.TaskModel;
import com.sox.api.model.UserModel;
import com.sox.api.service.Api;
import com.sox.api.service.Check;
import com.sox.api.service.Com;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@CheckLogin
@RequestMapping("/task")
public class TaskController {
    @Value("${sox.host_id}")
    private String host_id;

    @Autowired
    private Com com;

    @Autowired
    private Api api;

    @Autowired
    private TaskModel task_m;

    @Autowired
    private UserModel user_m;

    @Autowired
    private Check check;

    @RequestMapping("/statistics")
    public Map<String, Object> statistics() {
        Map<String, Object> data = new LinkedHashMap<>();
        
        data.put("task_running", task_m.db.count("status", "1"));
        data.put("task_stopped", task_m.db.count("status", "0"));

        return api.put(data);
    }

    @RequestMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> dict = new LinkedHashMap<>();

        Map<String, Long> line = api.line(20);

        Map<String, Object> map = new LinkedHashMap<>();

        if (line.get("rows") == 0) line.put("rows", task_m.list_count(map));

        if (line.get("page") == 0) {
            line.put("page", (long)Math.ceil((double)line.get("rows") / (double)line.get("size")));
        }

        map.put("#limit", line.get("size") + "," + ((line.get("page") - 1) * line.get("size")));

        List<Map<String, Object>> list = task_m.list(map);

        api.set_line(line);

        api.set_dict("is_dcs", host_id.equals("0") ? "0" : "1");

        if (list.size() > 0) {
            return api.put(list);
        } else {
            return api.err("没有数据");
        }
    }

    @RequestMapping("/item")
    public Map<String, Object> item() {
        String task_id = api.json("task_id");

        api.set_dict("is_dcs", host_id.equals("0") ? "0" : "1");

        return api.put(task_m.item(task_id));
    }

    @RequestMapping("/add")
    public Map<String, Object> add() {
        Map<String, String> data = api.json(null);

        data.put("user_id", user_m.get_session("id"));

        check.reset(data);

        check.validate("group", "任务组", "required");
        check.validate("name", "任务名", "required");

        if (check.result) {
            if (data.get("group").equals("system")) return api.err("“system”任务组不能手动添加");

            data.put("host_id", host_id);

            Long task_id = task_m.add(data);

            if (task_id > 0) {
                Map<String, Object> item = task_m.item(task_id.toString());

                if (item.get("auto").equals("1")) task_m.run(task_id.toString());

                return api.msg("任务添加成功");
            } else {
                return api.err("任务添加失败，请检查“任务名-任务组”组合是否唯一");
            }
        } else {
            api.set("err", check.errors);

            return api.err(check.error);
        }
    }

    @RequestMapping("/mod")
    public Map<String, Object> mod() {
        String task_id = api.json("task_id");

        Map<String, String> task_data = task_m.db.find("*", "id", task_id);

        if (task_data.get("status").equals("1")) return api.err("当前任务正在运行中，请稍后再试");

        if (!task_data.get("host_id").equals("0") && !task_data.get("host_id").equals(host_id)) {
            return com.request_hand_out(task_data.get("host_id"));
        }

        Map<String, String> data = new LinkedHashMap<>();

        for (String key : api.json().keySet()) {
            if (key.equals("task_id")) continue;

            data.put(key, api.json(key));
        }

        if (task_data.get("host_id").equals("0")) data.put("host_id", host_id);

        if (task_m.mod(task_id, data) > 0) {
            Map<String, Object> item = task_m.item(task_id);

            task_m.end(task_id);

            if (item.get("auto").equals("1")) task_m.run(task_id);

            api.set_dict("is_dcs", host_id.equals("0") ? "0" : "1");

            return api.put(task_m.item(task_id), "修改成功");
        } else {
            return api.err("修改失败");
        }
    }

    @RequestMapping("/del")
    public Map<String, Object> del() {
        String task_id = api.json("task_id");

        Map<String, String> task_data = task_m.db.find("*", "id", task_id);

        if (task_data.get("status").equals("1")) return api.err("当前任务正在运行中，删除失败");

        if (task_data.get("host_id").equals(host_id)) {
            task_m.del(task_id);
        } else {
            com.request_hand_out(task_data.get("host_id"));
        }

        return api.msg("任务已删除");
    }

    @RequestMapping("/run")
    public Map<String, Object> run(String task_id, String... manual_arg) {
        // 此接口只能在有http请求的地方调用（即不能在涉及后台任务的进程中调用，会由于获取不到请求参数报错 - 20211201）
        if (task_id == null) task_id = api.json("task_id");

        String manual = "";

        if (manual_arg != null && manual_arg.length > 0) manual = manual_arg[0];

        if (manual.equals("") && !api.json("manual").equals("")) {
            manual = api.json("manual");
        }

        Map<String, String> task_data = task_m.db.find("*", "id", task_id);

        if (task_data.get("status").equals("1")) return api.err("当前任务正在运行中...");

        if (task_data.get("host_id").equals(host_id)) {
            if (manual.equals("")) {
                task_m.run(task_id);
            } else {
                task_m.run(task_id, manual);
            }

        } else {
            com.request_hand_out(task_data.get("host_id"));
        }

        return api.msg("任务开始执行");
    }
}
