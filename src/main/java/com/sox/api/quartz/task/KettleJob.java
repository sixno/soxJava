package com.sox.api.quartz.task;

import com.sox.api.model.TaskModel;
import com.sox.api.quartz.utils.JobHelper;
import com.sox.api.quartz.utils.KettleService;
import com.sox.api.service.Com;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@DisallowConcurrentExecution // 作业不并发
@Component
public class KettleJob implements Job {
    @Autowired
    private Com com;

    @Autowired
    private TaskModel task_m;

    @Autowired
    private JobHelper job_h;

    @Autowired
    private KettleService kettle_s;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Map<String, String> arg = job_h.arg(jobExecutionContext);

        String task_id = arg.get("__id");
        String file = arg.get("__file");

        task_m.set_status(task_id, 0,  "1", "任务启动", com.time().toString());

        File file_o = new File(kettle_s.path(file));

        if (!file_o.exists() || !file_o.isFile()) {
            task_m.set_status(task_id, 0,  "2", "kettle 转换或作业文件不存在");

            return;
        }

        String[] msg = {"任务完成，步骤清单:\r\n\r\n"};

        switch (file.substring(file.length() - 3)) {
            case "ktr":
                kettle_s.run_ktr(file, arg, list -> {
                    for (Map<String, String> item : list) {
                        msg[0] += "步骤名称: " + item.get("step_name")
                                + " 读: " + item.get("read_lines")
                                + " 写: " + item.get("write_lines")
                                + " 输入: " + item.get("input_lines")
                                + " 输出: " + item.get("output_lines")
                                + " 更新: " + item.get("update_lines")
                                + " 拒绝: " + item.get("reject_lines")
                                + " 错误: " + item.get("error_lines")
                                + " 状态: " + item.get("status")
                                + "\r\n";
                    }
                });
                break;
            case "kjb":
                kettle_s.run_kjb(file, arg, list -> {
                    for (Map<String, String> item : list) {
                        msg[0] += "任务名称: " + item.get("task_name")
                                + " 错误数量: " + item.get("errors")
                                + " 任务状态: " + item.get("status")
                                + " 任务文件: " + item.get("task_file")
                                + "\r\n";
                    }
                });
                break;
            default:
                task_m.set_status(task_id, 0,  "2", "不是预期的 kettle 转换或作业文件");
                return;
        }

        task_m.set_status(task_id, 0,  "0", msg[0]);
    }
}
