package com.sox.api.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class Api {
    private final ThreadLocal<Map<String, Object>> req = new ThreadLocal<>();
    private final ThreadLocal<Map<String, Object>> res = new ThreadLocal<>();

    public String get(String key) {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        return request.getParameter(key) == null ? "" : request.getParameter(key);
    }

    public Map<String, Object> json() {
        Map<String, Object> json = req.get();

        if (json == null) {
            json = new HashMap<>();

            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

            StringBuilder json_str = new StringBuilder();

            try {
                BufferedReader bufferReader = new BufferedReader(request.getReader());

                String line;

                while ((line = bufferReader.readLine()) != null) {
                    json_str.append(line);
                }

                json = JSONObject.parseObject(json_str.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

            req.set(json);
        }

        return json;
    }

    public String json(String key, Object... def) {
        return def.length == 0 ? this.json().getOrDefault(key, "").toString() : this.json().getOrDefault(key, def[0]).toString();
    }

    public void output() {
        HttpServletResponse response = Objects.requireNonNull(((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse());

        response.setStatus(200);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            response.getWriter().write(JSONObject.toJSONString(this.out()));
            response.getWriter().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> res() {
        if (res.get() == null) {
            res.set(new HashMap<>());

            res.get().put("out", "1");
            res.get().put("msg", "");

            res.get().put("data", "");
            res.get().put("code", "");
            res.get().put("line", "");
            res.get().put("rows", "");
        }

        return res.get();
    }

    public void set(String key, Object val) {
        this.res().put(key, val);
    }

    public Map<String, Object> out() {
        // 这种方式可以方便在接口上做聚合时避免输出污染，其他倒没有什么别的想法
        Map<String, Object> res_copy = new HashMap<>(this.res());

        res.remove();

        return res_copy;
    }

    public Map<String, Object> msg(String str) {
        this.set("out", "1");
        this.set("msg", str);

        return this.out();
    }

    public Map<String, Object> msg(String str,Object obj) {
        this.set("out", "1");
        this.set("msg", str);

        this.set("data", obj);

        return this.out();
    }

    public Map<String, Object> put(Object obj) {
        this.set("out", "1");
        this.set("msg", "");

        this.set("data", obj);

        return this.out();
    }

    public Map<String, Object> put(Object obj,String str) {
        this.set("out", "1");
        this.set("msg", "");

        this.set("data", obj);
        this.set("code", str);

        return this.out();
    }

    public Map<String, Object> err(String str) {
        this.set("out", "0");
        this.set("msg", str);

        return this.out();
    }

    public Map<String, Object> err(String str,Object obj) {
        this.set("out", "0");
        this.set("msg", str);

        this.set("data", obj);

        return this.out();
    }
}
