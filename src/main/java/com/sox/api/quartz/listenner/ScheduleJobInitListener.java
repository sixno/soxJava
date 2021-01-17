package com.sox.api.quartz.listenner;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.Date;

@Component
@Order(value = 1)
public class ScheduleJobInitListener implements CommandLineRunner {
    @Autowired
    private Scheduler scheduler;

    @Override
    public void run(String... arg) {
        try {
            System.out.println("schedule init");

            try {
                // 创建jobDetail实例，绑定Job实现类
                // 指明job的名称，所在组的名称，以及绑定job类

                Class<? extends Job> jobClass = (Class<? extends Job>) (Class.forName("com.sox.api.quartz.task.HelloWorldJob").newInstance().getClass());
                JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity("test", "test")// 任务名称和组构成任务key
                        .build();

                jobDetail.getJobDataMap().put("test", "aaaabbbb");
                // 定义调度触发规则
                // 使用cornTrigger规则
//                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("test", "test")// 触发器key
//                        .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.SECOND))
//                        .withSchedule(CronScheduleBuilder.cronSchedule("0/2 * * * * ?")).startNow().build();

                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("test", "test")// 触发器key
                        .startAt(new Date())
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(3)
                                .withRepeatCount(0)).startNow().build();
                // 把作业和触发器注册到任务调度中
                scheduler.scheduleJob(jobDetail, trigger);
                // 启动
                if (scheduler.isShutdown()) {
                    System.out.println("aaaaa");
//                    scheduler.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
