package com.sox.api.quartz.utils;

import com.sox.api.utils.CastUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class QuartzManager {
    @Autowired
    private Scheduler scheduler;

    public JobKey job_key(String name, String group) {
        return JobKey.jobKey(name, group);
    }

    public TriggerKey trigger_key(String name, String group) {
        return TriggerKey.triggerKey(name, group);
    }

    public void add_job(String job_name, String job_group, String path, String cron_exp, String... arg) {
        JobDetail  job         = null;
        JobKey     job_key     = job_key(job_name, job_group);
        TriggerKey trigger_key = trigger_key(job_name, job_group);

        Class<? extends Job> job_class = null;

        try {
            job = scheduler.getJobDetail(job_key);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }

        try {
            job_class = CastUtils.cast(Class.forName(path).newInstance().getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JobDetail jobDetail = JobBuilder.newJob(job_class).withIdentity(job_key).build();

        if (arg.length > 0) {
            String pre_key = "";

            for (String ars : arg) {
                if (pre_key.equals("")) {
                    if (ars.contains(":")) {
                        for (String str : ars.split(";")) {
                            if(str.trim().equals("")) continue;

                            str = str.trim();

                            int pos = str.indexOf(":");

                            jobDetail.getJobDataMap().put(str.substring(0, pos), str.substring(pos + 1));
                        }
                    } else {
                        pre_key = ars;
                    }
                } else {
                    jobDetail.getJobDataMap().put(pre_key, ars);

                    pre_key = "";
                }
            }

            if (!pre_key.equals("")) {
                jobDetail.getJobDataMap().put(pre_key, "");
            }

        }

        Trigger trigger = this.build_trigger(trigger_key, cron_exp);

        if(trigger == null) return;

        try {
            if (job == null) {
                scheduler.scheduleJob(jobDetail, trigger);
            } else {
                if (!job.getJobClass().equals(job_class) || !job.getJobDataMap().equals(jobDetail.getJobDataMap())) {
                    try {
                        scheduler.pauseJob(job_key);

                        scheduler.deleteJob(job_key);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    scheduler.scheduleJob(jobDetail, trigger);
                } else {
                    scheduler.rescheduleJob(trigger_key, trigger);
                }
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }

        try {
            if (scheduler.isShutdown()) {
                scheduler.start();
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    // 暂停作业
    public void pause_job(String job_name, String job_group) {
        JobKey job_key = job_key(job_name, job_group);

        try {
            scheduler.pauseJob(job_key);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    // 恢复作业
    public void start_job(String job_name, String job_group) {
        JobKey job_key = job_key(job_name, job_group);

        try {
            scheduler.resumeJob(job_key);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    // 删除作业
    public void del_job(String job_name, String job_group) {
        JobKey job_key = job_key(job_name, job_group);

        try {
            scheduler.pauseJob(job_key);

            scheduler.deleteJob(job_key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Trigger build_trigger(TriggerKey triggerKey, String cron_exp) {
        Trigger trigger = null;

        if (!cron_exp.equals("")) {
            try {
                CronExpression cronExpression = new CronExpression(cron_exp);

                // 通过 cron 表达式获取下一次任务执行时间
                Date date = cronExpression.getNextValidTimeAfter(new Date());

                trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey)
                        .startAt(date)
                        .withSchedule(CronScheduleBuilder.cronSchedule(cron_exp)).build();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).build();
        }

        return trigger;
    }
}
