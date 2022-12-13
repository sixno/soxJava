package com.sox.api.quartz.task;

import com.sox.api.quartz.utils.JobHelper;
import com.sox.api.service.Com;
import com.sox.api.service.Db;
import com.sox.api.service.Log;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@DisallowConcurrentExecution // 作业不并发
@Component
public class CleanJob implements Job {
    @Value("${sox.upload_dir}")
    private String upload_dir;

    @Value("${sox.dat_dir}")
    private String dat_dir;

    @Value("${sox.dat_cls}")
    private String dat_cls;

    @Autowired
    private Com com;

    @Autowired
    private Log log;

    @Autowired
    private Db db;

    @Autowired
    private JobHelper job_h;

    public void execute(JobExecutionContext jobExecutionContext) {
        Map<String, String> arg = job_h.arg(jobExecutionContext);

        String tag = arg.get("tag");

        if (tag.contains("1")) this.close_idle_connection();

        if (tag.contains("2")) this.clean_upload_temp();

        if (tag.contains("3")) this.clean_data_file();
    }

    // 关闭打开超过1个小时的空闲数据库连接
    private void close_idle_connection() {
        for (String p_id : db.pool_use.keySet()) {
            if (db.pool_use.get(p_id) != null && !db.pool_use.get(p_id)) {
                Db.Conn connection = db.pool.get(p_id);

                if (com.time() - connection.time > 3600) {
                    String conn_p_id = connection.p_id + "";

                    db.pool_use.put(conn_p_id, true); // 设置为正在使用，阻止其他地方调用

                    try {
                        connection.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    connection.conn = null;

                    db.pool_use.put(conn_p_id, null);
                }
            }
        }
    }

    // 删除上传目录下临时文件中1天前的文件
    private void clean_upload_temp() {
        File dir = new File(com.path(upload_dir + File.separator + "temp"));

        if (!dir.isDirectory()) return;

        File[] files = dir.listFiles();

        if (files == null) return;

        for(File f : files) {
            File f_info = f.getAbsoluteFile();

            long curr_time = System.currentTimeMillis();

            long last_time = f_info.lastModified();

            long time = curr_time - last_time;

            if (time > 24 * 60 * 60 * 1000) {
                if (!f_info.delete()) log.msg("failed to delete upload temp file: " + f_info.getAbsolutePath(), 4);
            }
        }
    }

    // 删除超过n天的数据文件
    private void  clean_data_file() {
        if (dat_dir.equals("")) return;

        File dir = new File(com.path(dat_dir));

        File[] files = dir.listFiles();

        if (files == null) return;

        for(File f : files) {
            File f_info = f.getAbsoluteFile();

            long curr_time = System.currentTimeMillis();

            long last_time = f_info.lastModified();

            long time = curr_time - last_time;

            // 60 * 1000
            // Long.parseLong(log_cls) * 24 * 60 * 60 * 1000
            if (time > Long.parseLong(dat_cls) * 24 * 60 * 60 * 1000) {
                if (!f_info.delete()) log.msg("failed to delete data file: " + f_info.getAbsolutePath(), 4);
            }
        }
    }
}
