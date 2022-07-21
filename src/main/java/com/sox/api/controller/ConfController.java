package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.ConfModel;
import com.sox.api.model.EsModel;
import com.sox.api.service.Api;
import com.sox.api.service.Com;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CheckLogin
@RequestMapping("/conf")
public class ConfController {
    @Autowired
    private Com com;

    @Autowired
    private Api api;

    @Autowired
    private EsModel es_m;

    @Autowired
    private ConfModel conf_m;

    @RequestMapping("/list")
    public Map<String, Object> list() {
        return api.put(conf_m.list);
    }

    @RequestMapping("/set")
    public Map<String, Object> set() {
        Map<String, Object> json = api.json();

        int num = 0;

        for (String key : json.keySet()) {
            num += conf_m.set(key, json.get(key).toString());
        }

        if (num > 0) {
            com.request_hand_out("/conf/clean_cache");

            return api.msg("修改成功");
        } else {
            return api.err("修改失败");
        }
    }

    @RequestMapping("/clean_cache")
    public Map<String, Object> clean_cache() {
        conf_m.clean();

        return api.msg("配置缓存已清理");
    }

    @RequestMapping("/set_es_index")
    public Map<String, Object> set_es_index() {
        String index = api.json("index");
        String value = api.json("value");

        if (es_m.set_index(index, value)) {
            return api.msg("新建成功");
        } else {
            return api.err("新建失败");
        }
    }

    @RequestMapping("/del_es_index")
    public Map<String, Object> del_es_index() {
        String index = api.json("index");

        if (es_m.del_index(index)) {
            return api.msg("删除成功");
        } else {
            return api.err("删除失败");
        }
    }
}
