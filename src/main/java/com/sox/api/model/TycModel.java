package com.sox.api.model;

import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@EnableAutoConfiguration
public class TycModel {
    public Map<String, Object> parse(Map<String, Object> json, Map<String, Object> more) {
        Map<String, Object> data = new HashMap<>();

        data.put("email", "");

        if (json.get("emailList") != null) {
            for (Object email : JSONObject.parseArray(JSONObject.toJSONString(json.get("emailList")))) {
                String email_str = email.toString();

                if (data.get("email").equals("") && !email_str.equals("")) {
                    data.put("email", email_str);

                    break;
                }
            }
        }

        data.put("tel", "");
        data.put("mobile", "");

        if (json.get("phoneList") != null) {
            for (Object phone : JSONObject.parseArray(JSONObject.toJSONString(json.get("phoneList")))) {
                String phone_str = phone.toString();

                if ((phone_str.contains("-") || phone_str.length() != 11) && !phone_str.equals("")) {
                    data.put("tel", phone_str);
                }

                if (phone_str.length() == 11) {
                    data.put("mobile", phone_str);
                }

                if (!data.get("tel").equals("") && !data.get("mobile").equals("")) {
                    break;
                }
            }
        }

        data.put("avatar", json.getOrDefault("logo", ""));
        data.put("address", json.getOrDefault("regLocation", ""));
        data.put("name", json.getOrDefault("name", ""));
        data.put("legal_person", json.getOrDefault("legalPersonName", ""));
        data.put("alias", json.getOrDefault("alias", ""));
        data.put("status", json.getOrDefault("regStatus", ""));
        data.put("province", json.getOrDefault("base", ""));
        data.put("city", json.getOrDefault("city", ""));
        data.put("district", json.getOrDefault("district", ""));
        data.put("rounds", "");

        if (json.get("labelJsonList") != null) {
            for (Object label : JSONObject.parseArray(JSONObject.toJSONString(json.get("labelJsonList")))) {
                String label_str = label.toString();

                if (!label_str.equals("")) {
                    Map<String, Object> label_map = JSONObject.parseObject(label_str);

                    if (label_map.get("type").toString().equals("1")) {
                        data.put("rounds", label_map.get("value"));
                    }
                }
            }
        }

        data.put("bond_type", json.getOrDefault("bondType", ""));
        data.put("bond_code", json.getOrDefault("bondNum", ""));

        String found_date = json.getOrDefault("estiblishTime", "").toString();

        if(found_date.contains(" ")) data.put("found_date", found_date.substring(0, found_date.indexOf(" ")));

        String reg_cap_unit = json.getOrDefault("regCapital", "").toString();

        reg_cap_unit = reg_cap_unit.replaceAll("\\d+","");
        reg_cap_unit = reg_cap_unit.replaceAll(",","");
        reg_cap_unit = reg_cap_unit.replaceAll(".","");

        data.put("reg_cap_unit", reg_cap_unit);

        String reg_cap = json.getOrDefault("regCapital", "").toString().trim();

        reg_cap = reg_cap.replaceAll(",","");
        reg_cap = reg_cap.replaceAll(reg_cap_unit,"");

        data.put("reg_cap", reg_cap);

        data.put("reg_cap_type", reg_cap_unit.contains("人民币") ? "人民币" : (reg_cap_unit.contains("美元") ? "美元" : "其它"));

        data.put("industry", json.getOrDefault("categoryStr", ""));
        data.put("staff_num", json.getOrDefault("socialSecurityStaff_num", ""));
        data.put("type", json.getOrDefault("companyOrgType", ""));
        data.put("reg_no", json.getOrDefault("regNumber", ""));
        data.put("credit_code", json.getOrDefault("creditCode", ""));
        data.put("range", json.getOrDefault("businessScope", ""));
        data.put("out_id", json.getOrDefault("id", "").toString());
        data.put("out_type", "1");

        data.put("reg_authority", more.getOrDefault("regInstitute", ""));

        return data;
    }

    public Map<String, Object> parse(String json_str, String more) {
        return this.parse(JSONObject.parseObject(json_str), JSONObject.parseObject(more));
    }

    public List<Map<String, Object>> parse_holder(String json_str) {
        List<Map<String, Object>> list = new ArrayList<>();

        List<Object> list_obj = JSONObject.parseArray(json_str);

        if(list_obj == null) return list;

        for (Object item_obj : list_obj) {
            Map<String, Object> item = new HashMap<>();

            Map<String, Object> item_map = JSONObject.parseObject(JSONObject.toJSONString(item_obj));

            item.put("name", item_map.getOrDefault("name", ""));

            for (Object capital : JSONObject.parseArray(JSONObject.toJSONString(item_map.getOrDefault("capital", "")))) {
                Map<String, Object> capital_map = JSONObject.parseObject(JSONObject.toJSONString(capital));

                item.put("rate", capital_map.getOrDefault("percent", ""));
                item.put("fund", capital_map.getOrDefault("amomon", ""));

                break;
            }

            item.put("toco", item_map.getOrDefault("toco", ""));

            list.add(item);
        }

        return list;
    }

    public Object parse_staff(String json_str) {
        List<Map<String, Object>> list = new ArrayList<>();

        List<Object> list_obj = JSONObject.parseArray(json_str);

        if(list_obj == null) return list;

        for (Object item_obj : list_obj) {
            Map<String, Object> item = new HashMap<>();

            Map<String, Object> item_map = JSONObject.parseObject(JSONObject.toJSONString(item_obj));

            item.put("name", item_map.getOrDefault("name", ""));

            item.put("post", item_map.getOrDefault("typeSore", ""));

            item.put("toco", item_map.getOrDefault("toco", ""));

            list.add(item);
        }

        return list;
    }

    public Map<String, Object> for_return(Map<String, String> data) {
        Map<String, Object> item = new HashMap<>();

        if (data.size() > 0) {
            item = this.parse(data.get("tyc_data"), data.get("tyc_data2"));

            item.put("holder", this.parse_holder(data.get("holder_data")));
            item.put("staff", this.parse_staff(data.get("staff_data")));

            item.put("id", data.getOrDefault("id", ""));
        }

        return item;
    }
}
