package com.sox.api.model;

import com.sox.api.service.Com;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;

@Component
@EnableAutoConfiguration
public class TaskModel {
    public Db db;
    public Com com;

    @Autowired
    public void UserServiceImpl(Db db, Com com) {
        this.db = db.clone();
        this.db.table = "sys_task";

        this.com = com;
    }
}
