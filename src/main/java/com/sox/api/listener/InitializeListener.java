package com.sox.api.listener;

import com.sox.api.service.Com;
import com.sox.api.service.Db;
import com.sox.api.service.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class InitializeListener implements ApplicationListener<ApplicationStartedEvent> {
    public boolean runnable = true;

    @Value("${sox.host_id}")
    public String host_id;

    @Value("${sox.host_ip}")
    public String host_ip;

    @Value("${server.port}")
    public String port;

    @Value("${sox.log_dir}")
    private String log_dir;

    @Autowired
    public Com com;

    @Autowired
    public Db db;

    @Autowired
    public Log log;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        // 检测是否将日志输出到文件
        if (!log_dir.equals("")) {
            try {
                System.setOut(log.out());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 应用初始化，可以在这里实现诸如软件授权验证、模块注册、数据库初始化等动作
        log.msg("Number of initialization beans in container: " + event.getApplicationContext().getBeanDefinitionCount(), 0);

        log.msg("System initialization...", 0);

        File dir_1 = new File(com.path("upload/export"));

        if (!dir_1.isDirectory()) {
            if (!dir_1.mkdirs()) {
                System.out.println("File export directory creation failed...");
            }
        }

        if (!host_id.equals("0")) {
            if (host_ip.equals("")) {
                host_ip = com.net_addr();
            }

            System.out.println("It is currently distributed deployment mode...");
            System.out.println("host_id: " + host_id);
            System.out.println("host_ip: " + host_ip);
            System.out.println("port: " + port);

            // 在主机列表中注册自身

            if (db.table("sys_host").field("host_id", "host_id", host_id).equals("")) {
                db.table("sys_host").create("host_id", host_id, "ip", host_ip, "port", port);
            } else {
                db.table("sys_host").update(1, "host_id", host_id, "ip", host_ip, "port", port);
            }
        }

        // runnable = false;
    }
}
