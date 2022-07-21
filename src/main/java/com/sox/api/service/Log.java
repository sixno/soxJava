package com.sox.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.PrintStream;

@Service
public class Log {
    @Value("${sox.log_dir}")
    public String dir;

    @Value("${sox.log_out}")
    public String out;

    @Autowired
    private Com com;

    private final PrintStream[] storage_out = {null};
    private final PrintStream console_out = System.out;

    public PrintStream out() {
        if (dir.equals("")) return console_out;

        if (storage_out[0] != null) {
            storage_out[0].flush();
            storage_out[0].close();
        }

        try {
            storage_out[0] = new PrintStream(com.path(dir + "/" + com.date("yyyy-MM-dd") + ".log"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return storage_out[0] == null ? console_out : storage_out[0];
    }

    public void msg(Object obj, int... log_type) {
        if (!out.equals("") && log_type.length > 0) {
            boolean no_out = true;

            for (int type : log_type) {
                if (("," + out + ",").contains("," + type + ",")) {
                    no_out = false;

                    break;
                }
            }

            if (no_out) return;
        }

        System.out.println(com.date("yyyy-MM-dd HH:mm:ss.SSS") + "  " + obj.toString());
    }
}
