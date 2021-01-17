package com.sox.api.quartz.task;

import com.sox.api.model.UserModel;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@DisallowConcurrentExecution //作业不并发
@Component
public class HelloWorldJob implements Job{
    @Autowired
    UserModel user_m;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Map<String, String> user = user_m.db.find("id,name,cname", "name", "sixno");

        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

        System.out.println("这是一个定时任务 " + jobDataMap.get("test") + user.get("cname") + (new Date()).getTime());

    }
}
