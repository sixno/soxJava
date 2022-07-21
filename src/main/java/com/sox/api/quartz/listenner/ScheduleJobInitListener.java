package com.sox.api.quartz.listenner;

import com.sox.api.listener.InitializeListener;
import com.sox.api.quartz.utils.QuartzManager;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(value = 1)
public class ScheduleJobInitListener implements CommandLineRunner {
    @Value("${sox.host_id}")
    private String host_id;

    @Value("${sox.log_dir}")
    private String log_dir;

    @Value("${sox.log_chk}")
    private String log_chk;

    @Value("${sox.dat_chk}")
    private String dat_chk;

    @Autowired
    private InitializeListener initializeListener;

    @Autowired
    Db db;

    @Autowired
    QuartzManager quartzManager;

    @Override
    public void run(String... arg) {
        if (!initializeListener.runnable) return;

        // 添加日志切片任务
        if (!log_dir.equals("")) {
            // "0 * * * * ?"
            // "0 0 " + log_chk + " * * ?"
            quartzManager.add_job("log", "system", "com.sox.api.quartz.task.LogJob", "0 0 " + log_chk + " * * ?", "__id:0");
        }

        // 添加系统清理任务：关闭空闲数据库连接，每小时执行一次
        quartzManager.add_job("clean_1", "system", "com.sox.api.quartz.task.CleanJob", "0 0 * * * ?", "__id:0;tag:1");

        // 添加系统清理任务：清理上传的临时文件，每日凌晨两点执行一次
        quartzManager.add_job("clean_2", "system", "com.sox.api.quartz.task.CleanJob", "0 0 2 * * ?", "__id:0;tag:2");

        // 添加数据文件清理任务
        quartzManager.add_job("clean_3", "system", "com.sox.api.quartz.task.CleanJob", "0 0 " + dat_chk + " * * ?", "__id:0;tag:3");

        // 任务初始化
        if (host_id.equals("0")) {
            db.table("sys_task").update(1, "host_id!=", "0", "host_id", "0");
            db.table("sys_task").update(1, "status", "1", "status", "0");
        } else {
            db.table("sys_task").update(1, "host_id", "0", "host_id", host_id);
            db.table("sys_task").update(2, "host_id", host_id, "status", "1", "status", "0");
        }

        Map<String, Object> map = new HashMap<>();

        map.put("host_id", host_id);

        map.put("auto", "1");

        List<Map<String, String>> list = db.table("sys_task").read(map);

        for (Map<String, String> item : list) {
            quartzManager.add_job(item.get("name"), item.get("group"), item.get("path"), item.get("cron_exp"), "__id:"+item.get("id")+";" + item.get("arg"));
        }
    }
}
