package com.sox.api.service;

import java.util.HashMap;
import java.util.Map;

public class Check {
    public boolean result = true;

    public String error = "";
    public Map<String, String> errors = new HashMap<>();

    public Map<String, String> err_tp = new HashMap<>();

    public Check() {
        err_tp.put("required", "%s为必填项");
        err_tp.put("min_length", "%s至少%s位");
        err_tp.put("max_length", "%s最多%s位");
        err_tp.put("exact_length", "%s必须%s位");
    }

    public boolean required(String value, String... other) {
        boolean current = !value.equals("");

        if (!current && this.error.equals("")) {
            if (other.length == 1) {
                if (other[0].startsWith("#")) {
                    this.error = String.format(this.err_tp.get("required"), other[0].substring(1));
                } else {
                    this.error = other[0];
                }
            } else {
                this.error = "必填项未填写";
            }
        }

        this.result = this.result && current;

        return this.result;
    }
}
