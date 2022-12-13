package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.ConfModel;
import com.sox.api.service.Api;
import com.sox.api.service.Com;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
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
    private ConfModel conf_m;

    @RequestMapping("/all")
    public Api.Res all() {
        conf_m.ini();

        Map<String, String> set = api.arg();

        if (set.size() == 0) {
            return api.put(conf_m.list);
        } else {
            Map<String, Object> list = new LinkedHashMap<>();

            for (String key : set.keySet()) {
                list.put(key, this.get(key, set.get(key)));
            }

            return api.put(list);
        }
    }

    @RequestMapping("/get")
    public Api.Res get(String conf_id, String is_json) {
        if (conf_id == null) conf_id = api.arg("conf_id");
        if (is_json == null) is_json = api.arg("is_json", "0");

        switch (is_json) {
            case "1": return api.put(conf_m.get_obj(conf_id));
            case "2": return api.put(conf_m.get_arr(conf_id));
            default:  return api.put(conf_m.get(conf_id));
        }
    }

    @RequestMapping("/set")
    public Api.Res set() {
        Map<String, String> set = api.arg();

        int num = 0;

        for (String key : set.keySet()) {
            num += conf_m.set(key, set.get(key));
        }

        if (num > 0) {
            com.request_hand_out("/conf/clean_cache");

            return api.msg("设置成功");
        } else {
            return api.err("设置失败");
        }
    }

    @RequestMapping("/clean_cache")
    public Api.Res clean_cache() {
        conf_m.clean();

        return api.msg("配置缓存已清理");
    }

    @RequestMapping("/list")
    public Api.Res list() {
        Map<String, Object> map = com.map(api.arg());

        Api.Line line = api.line(20);

        if (line.rows == 0) line.rows = conf_m.list_count(map);
        if (line.page == 0) line.page = api.page(line);

        api.set_line(line);

        map.put("#limit", line);

        map.put("#order", "conf_id,asc");

        List<Map<String, Object>> list = conf_m.list(map);

        if (list.size() > 0) {
            return api.put(list);
        } else {
            return api.err("没有数据");
        }
    }

    @RequestMapping("/item")
    public Api.Res item() {
        String conf_id = api.arg("conf_id");

        return api.put(conf_m.item(conf_id));
    }

    @RequestMapping("/add")
    public Api.Res add() {
        String conf_id = api.arg("conf_id");
        String content = api.arg("content");

        if (conf_m.add(conf_id, content) > 0) {
            com.request_hand_out("/conf/clean_cache");

            return api.msg("添加成功");
        } else {
            return api.err("添加失败");
        }
    }

    @RequestMapping("/mod")
    public Api.Res mod() {
        String conf_id = api.arg("conf_id");
        String content = api.arg("content");

        if (conf_m.mod(conf_id, content) > 0) {
            com.request_hand_out("/conf/clean_cache");

            return api.msg("修改成功");
        } else {
            return api.err("修改失败");
        }
    }

    @RequestMapping("/del")
    public Api.Res del() {
        String conf_id = api.arg("conf_id");

        if (conf_m.del(conf_id) > 0) {
            com.request_hand_out("/conf/clean_cache");

            return api.msg("删除成功");
        } else {
            return api.err("删除失败");
        }
    }
}
