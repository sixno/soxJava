package com.sox.api.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.sox.api.utils.CallbackUtils;
import com.sox.api.utils.CastUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class Api {
    @Autowired
    private Com com;

    @Autowired
    private Log log;

    public final ThreadLocal<Map<String, Object>> req = new ThreadLocal<>();
    public final ThreadLocal<Map<String, Object>> res = new ThreadLocal<>();

    public Map<String, Object> json() {
        if (req.get() == null) {
            req.set(new LinkedHashMap<>());

            if (RequestContextHolder.getRequestAttributes() != null) {
                HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

                StringBuilder json_str = new StringBuilder();

                try {
                    BufferedReader bufferReader = new BufferedReader(request.getReader());

                    String line;

                    while ((line = bufferReader.readLine()) != null) {
                        json_str.append(line);
                    }

                    log.msg("input json: " + json_str, 1);

                    req.set(json_str.toString().startsWith("{") ? JSONObject.parseObject(json_str.toString(), Feature.OrderedField) : new LinkedHashMap<>());

                    if (req.get() == null) req.set(new LinkedHashMap<>());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return req.get();
    }

    public String json(String key, Object... def) {
        String value = def.length == 0 ? this.json().getOrDefault(key, "").toString() : this.json().getOrDefault(key, def[0]).toString();

        return value.equals("") && def.length > 0 ? def[0].toString() : value;
    }

    public Map<String, String> json(CallbackUtils<Map<String, String>> callback) {
        Map<String, String> data = new LinkedHashMap<>();

        for (String key : this.json().keySet()) {
            Map<String, String> json = new LinkedHashMap<>();

            json.put("key", key);
            json.put("val", this.json(key));

            if (callback != null) {
                try {
                    callback.deal(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!json.get("key").equals("")) data.put(json.get("key"), json.get("val"));
        }

        return data;
    }

    public Map<String, Long> line(Object... size) {
        String line_str = size.length > 1 ? size[1].toString() : com.http_get("line");

        Map<String, Long> line = new LinkedHashMap<>();

        if(line_str.equals("")) {
            line.put("page", 1L);
            line.put("size", size.length == 0 ? 10 : Long.parseLong(size[0].toString()));
            line.put("rows", 0L);
        } else if(line_str.equals("$")) {
            line.put("page", 0L);
            line.put("size", size.length == 0 ? 10 : Long.parseLong(size[0].toString()));
            line.put("rows", 0L);
        }
        else {
            String[] line_arr = line_str.split(",");

            line.put("page", Long.parseLong(line_arr[0]));
            line.put("size", Long.parseLong(line_arr[1]));
            line.put("rows", Long.parseLong(line_arr[2]));
        }

        return line;
    }

    public long page(long rows, long size) {
        return (long) Math.ceil((double) rows / (double) size);
    }

    public void set_line(Map<String, Long> line) {
        String line_str = "";

        line_str += "" + (line.get("page") + 1);

        line_str += "," + line.get("size");

        line_str += "," + line.get("rows");

        this.set("line", line_str);
    }

    public void set_dict(String key, Object val) {
        Map<String, Object> dict = this.res().get("dict") == null ? new LinkedHashMap<>() : CastUtils.cast(this.res().get("dict"));

        dict.put(key, val);

        this.set("dict", dict);
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
            res.set(new LinkedHashMap<>());

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
        Map<String, Object> res_copy = new LinkedHashMap<>(this.res());

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
