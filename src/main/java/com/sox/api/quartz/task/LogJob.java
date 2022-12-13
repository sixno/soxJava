package com.sox.api.quartz.task;

import com.sox.api.service.Com;
import com.sox.api.service.Log;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@DisallowConcurrentExecution // 作业不并发
@Component
public class LogJob implements Job {
    @Autowired
    private Com com;

    @Autowired
    private Log log;

    @Value("${sox.log_dir}")
    private String log_dir;

    @Value("${sox.log_cls}")
    private String log_cls;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        try {
            System.setOut(log.out());
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.clean_old_log();
    }

    private void clean_old_log () {
        File dir = new File(com.path(log_dir));

        File[] files = dir.listFiles();

        if (files == null) return;

        for(File f : files) {
            File f_info = f.getAbsoluteFile();

            long curr_time = System.currentTimeMillis();

            long last_time = f_info.lastModified();

            long time = curr_time - last_time;

            // 60 * 1000
            // Long.parseLong(log_cls) * 24 * 60 * 60 * 1000
            if (time > Long.parseLong(log_cls) * 24 * 60 * 60 * 1000) {
                if (!f_info.delete()) log.msg("failed to delete old log file: " + f_info.getAbsolutePath(), 4);
            }
        }
    }
}
