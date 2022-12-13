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
    public Api.Res statistics() {
        Map<String, Object> data = new LinkedHashMap<>();
        
        data.put("task_running", task_m.db.count("status", "1"));
        data.put("task_stopped", task_m.db.count("status", "0"));

        return api.put(data);
    }

    @RequestMapping("/list")
    public Api.Res list() {
        Map<String, Object> map = com.map(api.arg());

        if (!api.arg("__search").equals("")) {
            map.put("like#name,group,descr", api.arg("__search"));
        }

        Api.Line line = api.line(20);

        if (line.rows == 0) line.rows = task_m.list_count(map);
        if (line.page == 0) line.page = api.page(line);

        api.set_line(line);

        map.put("#limit", line);

        List<Map<String, Object>> list = task_m.list(map);

        api.set_dict("is_dcs", host_id.equals("0") ? "0" : "1");

        Long cur_num = task_m.db.count("status", "1");

        api.set_dict("cur_num", cur_num.toString());

        if (list.size() > 0) {
            return api.put(list);
        } else {
            return api.err("没有数据");
        }
    }

    @RequestMapping("/item")
    public Api.Res item() {
        String task_id = api.arg("task_id");

        api.set_dict("is_dcs", host_id.equals("0") ? "0" : "1");

        return api.put(task_m.item(task_id));
    }

    @RequestMapping("/add")
    public Api.Res add() {
        Map<String, String> data = api.arg();

        if (data.get("auto").equals("1") && task_m.next_scheduled_time(data.getOrDefault("cron_exp", "")).equals("")) return api.err("定时设置的cron表达式有误");

        data.put("user_id", user_m.get_session("id"));

        check.reset(data);

        check.validate("group", "任务组", "required");
        check.validate("name", "任务名", "required");

        if (check.result.get()) {
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
            api.set("err", check.errors.get());

            return api.err(check.error.get());
        }
    }

    @RequestMapping("/mod")
    public Api.Res mod() {
        String task_id = api.arg("task_id");

        Map<String, String> task_data = task_m.db.find("*", "id", task_id);

        if (task_data.get("status").equals("1")) return api.err("当前任务正在运行中，请稍后再试");

        if (!task_data.get("host_id").equals("0") && !task_data.get("host_id").equals(host_id)) {
            return com.request_hand_out(task_data.get("host_id"));
        }

        Map<String, String> data = new LinkedHashMap<>();

        for (String key : api.arg().keySet()) {
            if (key.equals("task_id")) continue;

            data.put(key, api.arg(key));
        }

        if (task_data.get("host_id").equals("0")) data.put("host_id", host_id);

        if (data.get("auto").equals("1") && task_m.next_scheduled_time(data.getOrDefault("cron_exp", "")).equals("")) return api.err("定时设置的cron表达式有误");

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
    public Api.Res del() {
        String task_id = api.arg("task_id");

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
    public Api.Res run(String task_id, String... manual_arg) {
        // 此接口只能在有http请求的地方调用（即不能在涉及后台任务的进程中调用，会由于获取不到请求参数报错 - 20211201）
        if (task_id == null) task_id = api.arg("task_id");

        String manual = "";

        if (manual_arg != null && manual_arg.length > 0) manual = manual_arg[0];

        if (manual.equals("") && !api.arg("manual").equals("")) {
            manual = api.arg("manual");
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
