package com.sox.api.quartz.utils;

import com.sox.api.service.Com;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JobHelper {
    @Autowired
    private Com com;

    public Map<String, String> arg(JobExecutionContext jobExecutionContext) {
        Map<String, String> map = new LinkedHashMap<>();

        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

        for (String key : jobDataMap.getKeys()) {
            map.put(key, com.at_var(jobDataMap.getString(key)));
        }

        return map;
    }
}
