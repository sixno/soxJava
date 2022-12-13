package com.sox.api.service;

import com.alibaba.fastjson.JSONObject;
import com.sox.api.utils.CallbackUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class Api {
    @Autowired
    private Com com;

    public static class Res implements Cloneable {
        public String out = "";
        public String msg = "";

        public Map<String, String> err = new LinkedHashMap<>();

        public String code = "";
        public Object data = "";
        public String line = "";

        public Map<String, Object> dict = new LinkedHashMap<>();

        public Res(Object... obj) {
            for (int i = 0;i < obj.length - 1;i += 2) {
                try {
                    this.getClass().getDeclaredField(obj[i].toString()).set(this, obj[i + 1]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public Res clone() {
            Res res;

            try {
                res = (Res) super.clone();
            } catch (Exception e) {
                e.printStackTrace();

                res = new Res();
            }

            return res;
        }
    }

    public final ThreadLocal<Res> res = ThreadLocal.withInitial(Res::new);

    public String arg(String key, String... def) {
        String def_0 = def.length > 0 ? def[0] : "";

        String str = com.http_json(key, def_0);

        return str.equals("") ? def_0 : str;
    }

    @SafeVarargs
    public final Map<String, String> arg(CallbackUtils<Map<String, String>>... callback) {
        if (callback.length > 0) {
            Map<String, String> data = new LinkedHashMap<>();

            for (String key : com.http_json().keySet()) {
                Map<String, String> json = new LinkedHashMap<>();

                json.put("key", key);
                json.put("val", this.arg(key));

                if (callback[0] != null) {
                    try {
                        callback[0].deal(json);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (!json.get("key").equals("")) data.put(json.get("key"), json.get("val"));
            }

            return data;
        } else {
            return com.http_json();
        }
    }

    public static class Line {
        public Long page = 0L;
        public Long size = 0L;
        public Long rows = 0L;
    }

    public Line line(Object... size) {
        String line_str = size.length > 1 ? size[1].toString() : com.http_get("line");

        Line line = new Line();

        if(line_str.equals("")) {
            line.page = 1L;
            line.size = size.length == 0 ? 10 : Long.parseLong(size[0].toString());
        } else if(line_str.equals("$")) {
            line.page = 0L;
            line.size = size.length == 0 ? 10 : Long.parseLong(size[0].toString());
        }
        else {
            String[] line_arr = line_str.split(",");

            line.page = Long.parseLong(line_arr[0]);
            line.size = Long.parseLong(line_arr[1]);
            line.rows = Long.parseLong(line_arr[2]);
        }

        return line;
    }

    public long page(long rows, long size) {
        return (long) Math.ceil((double) rows / (double) size);
    }

    public long page(Line line) {
        return this.page(line.rows, line.size);
    }

    public void output() {
        HttpServletResponse response = com.http_response.get();

        if (response == null) return;

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

    public Res res() {
        return res.get();
    }

    public void set(String key, Object val) {
        try {
            res.get().getClass().getDeclaredField(key).set(res.get(), val);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void set_err(Map<String, String> err) {
        res.get().err = err;
    }

    public void set_line(Line line) {
        String line_str = "";

        line_str += "" + (line.page + 1);

        line_str += "," + line.size;

        line_str += "," + line.rows;

        this.res().line = line_str;
    }

    public void set_dict(String key, Object val) {
        this.res().dict.put(key, val);
    }

    public Res out() {
        // 避免在接口上做聚合时避免输出污染
        Res out = this.res().clone();

        res.set(new Res());

        return out;
    }

    public Res msg(String msg) {
        this.res().out = "1";
        this.res().msg = msg;

        return this.out();
    }

    public Res msg(String msg, Object data) {
        this.res().out = "1";
        this.res().msg = msg;

        this.res().data = data;

        return this.out();
    }

    public Res put(Object data) {
        this.res().out = "1";
        this.res().msg = "";

        this.res().data = data;

        return this.out();
    }

    public Res put(String code, Object data) {
        this.res().out = "1";
        this.res().msg = "";

        this.res().code = code;
        this.res().data = data;

        return this.out();
    }

    public Res put(Object data, String msg) {
        this.res().out = "1";
        this.res().msg = msg;

        this.res().data = data;

        return this.out();
    }

    public Res put(Object data, String msg, String code) {
        this.res().out = "1";
        this.res().msg = msg;

        this.res().code = code;
        this.res().data = data;

        return this.out();
    }

    public Res err(String err) {
        this.res().out = "0";
        this.res().msg = err;

        return this.out();
    }

    public Res err(String err, Object data) {
        this.res().out = "0";
        this.res().msg = err;

        this.res().data = data;

        return this.out();
    }
}
