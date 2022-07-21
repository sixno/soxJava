package com.sox.api.listener;

import com.sox.api.service.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class RunListener implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private InitializeListener initializeListener;

    @Autowired
    private Log log;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (initializeListener.runnable) {
            log.msg("System is ready...", 0);
        } else {
            log.msg("System initialization failed... it will stop running...", 0);

            System.exit(0);
        }
    }
}
