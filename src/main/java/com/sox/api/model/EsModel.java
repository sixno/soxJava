package com.sox.api.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sox.api.service.Com;
import com.sox.api.service.Curl;
import com.sox.api.service.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@EnableAutoConfiguration
public class EsModel {
    public String es_addr = "";

    public Db db;
    public Com com;

    @Autowired
    public void UserServiceImpl(Db db, Com com) {
        this.db = db.clone();
        this.db.table = "sys_config";

        this.com = com;

        this.es_addr = this.db.find("content#es_addr");
    }

    private final Curl.Resolver<Map<String, Object>> jsonResolver = (httpCode, responseBody) -> {
        String json_str = new String(responseBody, StandardCharsets.UTF_8);

        System.out.println(json_str);

        return JSONObject.parseObject(json_str);
    };

    public boolean set_index(String index, String value) {
        Curl curl = new Curl();

        if (!value.equals("")) curl.opt("-H", "Content-Type: application/json");

        curl.opt("-X", "PUT", es_addr + index);

        if (!value.equals("")) curl.opt("-d", value);

        Map<String, Object> json = curl.exec(jsonResolver,null);

        return json != null && Boolean.parseBoolean(json.get("acknowledged").toString());
    }

    public boolean del_index(String index) {
        Curl curl = new Curl();

        curl.opt("-X", "DELETE", es_addr + index);

        Map<String, Object> json = curl.exec(jsonResolver,null);

        return json != null && Boolean.parseBoolean(json.get("acknowledged").toString());
    }

    public boolean add_data(String index, String id, Object data) {
        Curl curl = new Curl();

        curl.opt("-H", "Content-Type: application/json", "-X", "PUT", es_addr + index + "/_doc/" + id, "-d", JSONObject.toJSONString(data));

        Map<String, Object> json = curl.exec(jsonResolver,null);

        return json != null && (json.getOrDefault("result", "").toString().equals("created") || json.getOrDefault("result", "").toString().equals("updated"));
    }

    public Map<String, Object> search(String index,int size, int from, String... match) {
        Map<String, Object> result = new HashMap<>();

        result.put("num", 0);

        result.put("ids", "");

        Curl curl = new Curl();

        String url = es_addr + index + "/_doc/_search?size=" + size + "&from=" + from;

        if (match.length > 0) {
            url += "&q=";

            for (int i = 0;i < match.length;i += 2) {
                if (match[i + 1].equals("")) continue;

                String match_str = "";
                try {
                    match_str = URLEncoder.encode(match[i] + ":" + match[i + 1], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (!match_str.equals("")) url += match_str + (i < match.length - 2 ? "+" : "");
            }
        }

        curl.opt("-X", "GET", url);

        Map<String, Object> json = curl.exec(jsonResolver,null);

        Object hits = json.get("hits");

        if (hits == null) return result;

        Map<String, Object> hits_map = JSONObject.parseObject(JSONObject.toJSONString(hits));

        Map<String, Object> hits_total = JSONObject.parseObject(JSONObject.toJSONString(hits_map.get("total")));

        JSONArray hits_hits = JSONObject.parseArray(JSONObject.toJSONString(hits_map.get("hits")));

        result.put("num", hits_total.get("value"));

        if (hits_hits.size() > 0) {
            String ids = "";

            for (Object info_obj : hits_hits) {
                Map<String, Object> info = JSONObject.parseObject(JSONObject.toJSONString(info_obj));

                ids += info.get("_id").toString() + ",";
            }

            result.put("ids", ids.substring(0, ids.length() - 1));
        }

        return result;
    }
}
