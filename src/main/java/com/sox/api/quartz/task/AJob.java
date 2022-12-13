package com.sox.api.quartz.task;

import com.sox.api.model.TaskModel;
import com.sox.api.quartz.utils.JobHelper;
import com.sox.api.service.Com;
import com.sox.api.service.Log;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@DisallowConcurrentExecution // 作业不并发
@Component
public class AJob implements Job {
    @Autowired
    private Com com;

    @Autowired
    private Log log;

    @Autowired
    private TaskModel task_m;

    @Autowired
    private JobHelper job_h;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Map<String, String> arg = job_h.arg(jobExecutionContext);

        String task_id = arg.get("__id");

        task_m.set_status(task_id, 0,  "1", "任务启动", com.time().toString());

        log.msg("Ii is a job, and received: " + arg.getOrDefault("message", "nothing"), 5);

        task_m.set_status(task_id, 0,  "0", "任务完成");
    }
}
