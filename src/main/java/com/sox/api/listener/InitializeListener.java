package com.sox.api.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class InitializeListener implements ApplicationListener<ContextRefreshedEvent> {
    public boolean runnable = true;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 应用初始化，可以在这里实现诸如软件授权验证、模块注册、数据库初始化等动作
        System.out.println("Number of initialization beans in container: " + event.getApplicationContext().getBeanDefinitionCount());

        System.out.println("System initialization...");

        // runnable = false;
    }
}
