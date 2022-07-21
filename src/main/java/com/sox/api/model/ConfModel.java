package com.sox.api.model;

import com.sox.api.service.Com;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ConfModel {
    @Autowired
    public Com com;

    public Db db;

    @Autowired
    public void UserServiceImpl(Db db) {
        this.db = db.clone();
        this.db.table = "sys_conf";
    }

    public Map<String, String> list = null; // new LinkedHashMap<>();

    public void ini() {
        // 应用初始化步骤（com.sox.api.listener.InitializeListener）需要优先业务代码执行
        // 由于容器化技术，这一步是为了防止数据库中相关数据表尚未创建引发查询错误

        if (list != null) return;

        list = new LinkedHashMap<>();

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("#field", "conf_id,content");
        map.put("#order", "conf_id,asc");

        for (Map<String, String> item : this.db.read(map)) {
            list.put(item.get("conf_id"), item.get("content"));
        }
    }

    public String get(String key) {
        this.ini();

        return list.getOrDefault(key, "");
    }

    public Long set(String key, String val) {
        this.ini();

        String conf_id = db.field("conf_id", "conf_id", key);

        Long num;

        if (!conf_id.equals("")) {
            num = db.update(1, "conf_id", key, "content", val);
        } else {
            num = db.create("conf_id", key, "content", val);
        }

        if (num > 0) {
            list.put(key, val);
        }

        return num;
    }

    public void clean() {
        list = null;
    }
}
