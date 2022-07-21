package com.sox.api.quartz.utils;

import com.sox.api.service.Com;
import com.sox.api.utils.CallbackUtils;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.plugins.PluginFolder;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobEntryResult;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KettleService {
    @Value("${sox.upload_dir}")
    private String upload_dir;

    @Autowired
    private Com com;

    public String path(String file_name) {
        return com.path(upload_dir + File.separator + "kettle" + File.separator + file_name);
    }

    public void run_ktr(String file_name, CallbackUtils<Trans> callback, Map<String, String> params) {
        File plugins_path = new File(this.path("plugins") + File.separator);

        String ktr_file = this.path(file_name);

        try {
            // 如果有插件则加载插件
            if (plugins_path.isDirectory()) {
                File[] plugins = plugins_path.listFiles();

                if (plugins != null) {
                    for (File plugin : plugins){
                        StepPluginType.getInstance().getPluginFolders().add(new PluginFolder(plugin.getPath(),false,true));
                    }
                }
            }

            KettleEnvironment.init();

            TransMeta tm = new TransMeta(ktr_file);

            Trans trans = new Trans(tm);

            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    trans.setParameterValue(entry.getKey(), entry.getValue());
                }
            }

            callback.deal(trans);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run_ktr(String file_name, Map<String, String> params) {
        this.run_ktr(file_name, trans -> {
            trans.execute(null);
            trans.waitUntilFinished();
        }, params);
    }

    public void run_ktr(String file_name, Map<String, String> params, CallbackUtils<List<Map<String, String>>> callback) {
        this.run_ktr(file_name, trans -> {
            trans.execute(null);
            trans.waitUntilFinished();

            List<StepMetaDataCombi> steps = trans.getSteps();

            List<Map<String, String>> step_list = new ArrayList<>();

            for (StepMetaDataCombi step : steps) {
                Map<String, String> step_item = new LinkedHashMap<>();

                step_item.put("step_name", step.stepname);
                step_item.put("read_lines", Long.toString(step.step.getLinesRead()));
                step_item.put("write_lines", Long.toString(step.step.getLinesWritten()));
                step_item.put("input_lines", Long.toString(step.step.getLinesInput()));
                step_item.put("output_lines", Long.toString(step.step.getLinesOutput()));
                step_item.put("update_lines", Long.toString(step.step.getLinesUpdated()));
                step_item.put("reject_lines", Long.toString(step.step.getLinesRejected()));
                step_item.put("error_lines", Long.toString(step.step.getErrors()));
                step_item.put("status", step.step.getStatus().toString());

                step_list.add(step_item);
            }

            callback.deal(step_list);
        }, params);
    }

    public void run_kjb(String file_name, CallbackUtils<Job> callback, Map<String, String> params) {
        File plugins_path = new File(this.path("plugins") + File.separator);

        String kjb_file = this.path(file_name);

        try {
            // 如果有插件则加载插件
            if (plugins_path.isDirectory()) {
                File[] plugins = plugins_path.listFiles();

                if (plugins != null) {
                    for (File plugin : plugins){
                        StepPluginType.getInstance().getPluginFolders().add(new PluginFolder(plugin.getPath(),false,true));
                    }
                }
            }

            KettleEnvironment.init();

            JobMeta jm = new JobMeta(kjb_file, null);

            Job job = new Job(null, jm);

            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    job.setVariable(entry.getKey(), entry.getValue());
                }
            }

            callback.deal(job);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run_kjb(String file_name, Map<String, String> params) {
        this.run_kjb(file_name, job -> {
            job.start();
            job.waitUntilFinished();
        }, params);
    }

    public void run_kjb(String file_name, Map<String, String> params, CallbackUtils<List<Map<String, String>>> callback) {
        this.run_kjb(file_name, job -> {
            job.start();
            job.waitUntilFinished();

            List<JobEntryResult> results = job.getJobEntryResults();

            List<Map<String, String>> result_list = new ArrayList<>();

            for (JobEntryResult result : results) {
                Map<String, String> result_item = new LinkedHashMap<>();

                result_item.put("task_name", result.getJobEntryName());
                result_item.put("task_file", result.getJobEntryFilename() == null ? "" : result.getJobEntryFilename());
                result_item.put("errors", Long.toString(result.getResult().getNrErrors()));
                result_item.put("status", result.getComment());

                result_list.add(result_item);
            }

            callback.deal(result_list);
        }, params);
    }
}
