package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.EsModel;
import com.sox.api.service.Api;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@EnableAutoConfiguration
@CheckLogin
@RequestMapping("/conf")
public class ConfController {
    @Autowired
    private Api api;

    @Autowired
    private Db db;

    @Autowired
    private EsModel es;

    String table = "sys_config";

    @RequestMapping("/list")
    public Map<String, Object> list() {
        Map<String, String> conf = new HashMap<>();

        Map<String, Object> map = new HashMap<>();

        map.put("#field", "id,content");

        for (Map<String, String> item : db.table(table).read(map)) {
            conf.put(item.get("id"), item.get("content"));
        }

        return api.put(conf);
    }

    @RequestMapping("/set")
    public Map<String, Object> set() {
        Map<String, Object> json = api.json();

        Map<String, String> data;

        int num = 0;

        for (String key : json.keySet()) {
            data = new HashMap<>();

            data.put("content", json.get(key).toString());

            num += db.table(table).update(key, data);
        }

        return num > 0 ? api.msg("修改成功") : api.msg("修改失败");
    }

    @RequestMapping("/set_es_index")
    public Map<String, Object> set_es_index() {
        String index = api.json("index");
        String value = api.json("value");

        if (es.set_index(index, value)) {
            return api.msg("新建成功");
        } else {
            return api.err("新建失败");
        }
    }

    @RequestMapping("/del_es_index")
    public Map<String, Object> del_es_index() {
        String index = api.json("index");

        if (es.del_index(index)) {
            return api.msg("删除成功");
        } else {
            return api.err("删除失败");
        }
    }
}
