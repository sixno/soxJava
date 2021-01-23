package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.TaskModel;
import com.sox.api.model.UserModel;
import com.sox.api.service.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@EnableAutoConfiguration
@CheckLogin
@RequestMapping("/task")
public class TaskController {
    @Autowired
    private Api api;

    @Autowired
    private TaskModel task_m;

    @Autowired
    private UserModel user_m;

    @RequestMapping("/statistics")
    public Map<String, Object> statistics() {
        Map<String, Object> data = new HashMap<>();

        data.put("task_running", task_m.db.count("status", "1"));
        data.put("task_stopped", task_m.db.count("status", "0"));

        data.put("tyc_company_num", task_m.db.table("tyc_company").count());
        data.put("tyc_company_fl_num", task_m.db.table("tyc_company_fl").count());

        return api.put(data);
    }

    @RequestMapping("/list")
    public Map<String, Object> list() {
        Map<String, Integer> line = api.line(20);

        Map<String, Object> map = new HashMap<>();

        if (line.get("rows") == 0) line.put("rows", task_m.list_count(map));

        if (line.get("page") == 0) {
            line.put("page", (int) Math.ceil((double) line.get("rows") / (double) line.get("size")));
        }

        map.put("#limit", line.get("size") + "," + ((line.get("page") - 1) * line.get("size")));

        List list = task_m.list(map);

        api.set_line(line);

        if (list.size() > 0) {
            return api.put(list);
        } else {
            return api.err("没有数据");
        }
    }

    @RequestMapping("/item")
    public Map<String, Object> item() {
        String task_id = api.json("task_id");

        return api.put(task_m.item(task_id));
    }

    @RequestMapping("/add")
    public Map<String, Object> add() {
        Map<String, String> data = new HashMap<>();

        for (String key : api.json().keySet()) {
            data.put(key, api.json(key));
        }

        data.put("user_id", user_m.get_session("id"));

        if (task_m.add(data) > 0) {
            return api.msg("任务添加成功");
        } else {
            return api.err("任务添加失败，请检查“任务名-任务组”组合是否唯一");
        }
    }

    @RequestMapping("/del")
    public Map<String, Object> del() {
        String task_id = api.json("task_id");

        task_m.del(task_id);

        return api.msg("任务已删除");
    }

    @RequestMapping("/run")
    public Map<String, Object> run() {
        String task_id = api.json("task_id");

        task_m.run(task_id);

        return api.msg("任务开始执行");
    }
}
