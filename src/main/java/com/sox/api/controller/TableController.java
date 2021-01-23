package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.service.Api;
import com.sox.api.service.Db;
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
@RequestMapping("/table")
public class TableController {
    @Autowired
    private Api api;

    @Autowired
    private Db db;

    private final String allow_tables = "tyc_company,tyc_company_fl,tyc_company_sh";

    @RequestMapping("/list")
    public Map<String, Object> list() {
        String table = api.json("table");

        if(!("," + allow_tables + ",").contains("," + table + ",")) return api.err("当前数据表禁止查看");

        Map<String, Integer> line = api.line(20);

        Map<String, Object> map = new HashMap<>();

        map.put("#limit", line.get("size") + "," + ((line.get("page") - 1) * line.get("size")));

        if (line.get("rows") == 0) line.put("rows", db.table(table).count());

        List<Map<String, String>> list = db.table(table).read(map);

        api.set_line(line);

        if (list.size() > 0) {
            return api.put(list);
        } else {
            return api.err("没有数据");
        }
    }

    @RequestMapping("/cols")
    public Map<String, Object> cols() {
        String table = api.json("table");

        if(!("," + allow_tables + ",").contains("," + table + ",")) return api.err("当前数据表禁止查看");

        List<Object[]> list = db.table(table).cols();

        if (list.size() > 0) {
            return api.put(list);
        } else {
            return api.err("没有数据");
        }
    }
}
