package com.sox.api.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class RunListener implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private InitializeListener initializeListener;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("System is ready...");

        if (!initializeListener.runnable) System.exit(0);
    }
}
