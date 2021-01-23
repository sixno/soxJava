package com.sox.api.quartz.listenner;

import com.sox.api.quartz.utils.QuartzManager;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(value = 1)
public class ScheduleJobInitListener implements CommandLineRunner {
    @Autowired
    Db db;

    @Autowired
    QuartzManager quartzManager;

    @Override
    public void run(String... arg) {
        Map<String, Object> map = new HashMap<>();

        map.put("manual", "0");

        List<Map<String, String>> list = db.table("sys_task").read(map);

        for (Map<String, String> item : list) {
            quartzManager.add_job(item.get("name"), item.get("group"), item.get("path"), item.get("cron_exp"), "__id:"+item.get("id")+";" + item.get("arg"));
        }
    }

}
