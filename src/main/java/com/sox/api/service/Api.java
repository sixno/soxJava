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
    public final ThreadLocal<Map<String, Object>> req = new ThreadLocal<>();
    public final ThreadLocal<Map<String, Object>> res = new ThreadLocal<>();

    public String get(String key) {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        return request.getParameter(key) == null ? "" : request.getParameter(key);
    }

    public Map<String, Object> json() {
        if (req.get() == null) {
            req.set(new HashMap<>());

            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

            StringBuilder json_str = new StringBuilder();

            try {
                BufferedReader bufferReader = new BufferedReader(request.getReader());

                String line;

                while ((line = bufferReader.readLine()) != null) {
                    json_str.append(line);
                }

                req.set(JSONObject.parseObject(json_str.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return req.get();
    }

    public String json(String key, Object... def) {
        return def.length == 0 ? this.json().getOrDefault(key, "").toString() : this.json().getOrDefault(key, def[0]).toString();
    }

    public Map<String, Integer> line(int... size) {
        Map<String, Integer> line = new HashMap<>();

        String line_str = this.get("line");

        if(line_str.equals("")) {
            line.put("page", 1);
            line.put("size", size.length == 0 ? 10 : size[0]);
            line.put("rows", 0);
        } else if(line_str.equals("$")) {
            line.put("page", 0);
            line.put("size", size.length == 0 ? 10 : size[0]);
            line.put("rows", 0);
        }
        else {
            String[] line_arr = line_str.split(",");

            line.put("page", Integer.parseInt(line_arr[0]));
            line.put("size", Integer.parseInt(line_arr[1]));
            line.put("rows", Integer.parseInt(line_arr[2]));
        }

        return line;
    }

    public void set_line(Map<String, Integer> line) {
        String line_str = "";

        line_str += (line.get("page") + 1) + "";

        line_str += "," + line.get("size");

        line_str += "," + line.get("rows");

        this.set("line", line_str);
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

            res.get().put("out", "");
            res.get().put("msg", "");
            res.get().put("err", "");

            res.get().put("data", "");
            res.get().put("code", "");
            res.get().put("line", "");
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

    public Map<String, Object> msg(String msg) {
        this.set("out", "1");
        this.set("msg", msg);

        return this.out();
    }

    public Map<String, Object> msg(String msg, Object data) {
        this.set("out", "1");
        this.set("msg", msg);

        this.set("data", data);

        return this.out();
    }

    public Map<String, Object> put(Object data) {
        this.set("out", "1");
        this.set("msg", "");

        this.set("data", data);

        return this.out();
    }

    public Map<String, Object> put(String code, Object data) {
        this.set("out", "1");
        this.set("msg", "");

        this.set("data", data);
        this.set("code", code);

        return this.out();
    }

    public Map<String, Object> put(Object data, String msg) {
        this.set("out", "1");
        this.set("msg", msg);

        this.set("data", data);

        return this.out();
    }

    public Map<String, Object> put( Object data, String msg, String code) {
        this.set("out", "1");
        this.set("msg", msg);

        this.set("data", data);
        this.set("code", code);

        return this.out();
    }

    public Map<String, Object> err(String err) {
        this.set("out", "0");
        this.set("msg", err);

        return this.out();
    }

    public Map<String, Object> err(String err, Object data) {
        this.set("out", "0");
        this.set("msg", err);

        this.set("data", data);

        return this.out();
    }
}
