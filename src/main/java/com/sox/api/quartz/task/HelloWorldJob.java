package com.sox.api.quartz.task;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Date;

@DisallowConcurrentExecution // 作业不并发
@Component
public class HelloWorldJob implements Job{
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

        System.out.println("Hello, " + jobDataMap.getOrDefault("to_who", "World") + (new Date()).getTime());
    }
}
