package com.sox.api.quartz.utils;

import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
            job_class = (Class<? extends Job>) (Class.forName(path).newInstance().getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JobDetail jobDetail = JobBuilder.newJob(job_class).withIdentity(job_key).build();

        if (arg.length > 0) {
            for (String str : arg[0].split(";")) {
                if(str.trim().equals("")) continue;

                String[] arr = str.trim().split(":");

                jobDetail.getJobDataMap().put(arr[0], arr[1]);
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
    public void off_job(String job_name, String job_group) {
        JobKey job_key = job_key(job_name, job_group);

        try {
            scheduler.pauseJob(job_key);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    // 恢复作业
    public void rub_job(String job_name, String job_group) {
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            CronTriggerImpl cronTriggerImpl = new CronTriggerImpl();

            try {
                cronTriggerImpl.setCronExpression(cron_exp);

                List<Date> dates = TriggerUtils.computeFireTimesBetween(cronTriggerImpl, null, new Date(), new Date((new Date()).getTime() + 63072000000L));

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                String nextTime = dateFormat.format(dates.get(0));

                Date date = dateFormat.parse(nextTime);

                trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey)
                        .startAt(date)
                        .withSchedule(CronScheduleBuilder.cronSchedule(cron_exp)).build();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).build();
        }

        return trigger;
    }
}
