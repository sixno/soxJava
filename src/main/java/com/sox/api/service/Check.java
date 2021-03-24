package com.sox.api.service;

import java.util.HashMap;
import java.util.Map;

public class Check {
    public boolean result = true;
    public boolean queue  = true;
    public String  error  = "";

    public Map<String, String> errors = new HashMap<>();
    public Map<String, String> err_tp = new HashMap<>();

    public Check(boolean... set_queue) {
        err_tp.put("required", "%s为必填项");
        err_tp.put("min_length", "%s至少%s位");
        err_tp.put("max_length", "%s最多%s位");
        err_tp.put("exact_length", "%s必须%s位");

        if (set_queue.length == 1) {
            queue = set_queue[0];
        }
    }

    public boolean required(String value, String... other) {
        boolean current = !value.equals("");

        if (!current && queue) this.set_error("required", other, "必填项未填");

        result = queue ? result && current : current;

        return result;
    }

    public boolean min_length(String value, int num, String... other) {
        boolean current = value.length() >= num;

        if (!current && queue) this.set_error("min_length", other, "字符串长度不符");

        result = queue ? result && current : current;

        return result;
    }

    public boolean max_length(String value, int num, String... other) {
        boolean current = value.length() <= num;

        if (!current && queue) this.set_error("max_length", other, "字符串长度不符");

        result = queue ? result && current : current;

        return result;
    }

    public boolean exact_length(String value, int num, String... other) {
        boolean current = value.length() == num;

        if (!current && queue) this.set_error("exact_length", other, "字符串长度不符");

        result = queue ? result && current : current;

        return result;
    }

    private void set_error(String rule_name, String[] other, String... def) {
        if (other.length == 1) {
            if (other[0].startsWith("#")) {
                String[] field = other[0].substring(1).split(",");
                String   value = "";

                int i = 0;

                for (String tp : err_tp.get(rule_name).split("%s")) {
                    if (i < field.length) {
                        value += String.format(tp + "%s", field[i]);
                    } else {
                        value += tp;
                    }

                    i++;
                }

                if (error.equals("")) error = value;

                errors.putIfAbsent(field[0], value);
            } else {
                error = other[0];
            }
        } else if (other.length == 2) {
            if (error.equals("")) error = other[1];

            errors.put(other[0], other[1]);
        } else {
            error = def.length == 1 ? def[0] : "未知错误";
        }
    }
}
