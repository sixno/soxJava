package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.IndexModel;
import com.sox.api.model.TableModel;
import com.sox.api.service.Api;
import com.sox.api.service.Check;
import com.sox.api.service.Com;
import com.sox.api.service.Poi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@CheckLogin
@RequestMapping("/table")
public class TableController {
    @Autowired
    private Com com;

    @Autowired
    private Api api;

    @Autowired
    private Poi poi;

    @Autowired
    private Check check;

    @Autowired
    private TableModel table_m;

    @Autowired
    private IndexModel index_m;

    @Autowired
    private SqlController sql_c;

    private boolean allow(String table) {
        String allow_tables = "base_ledger,base_credit,base_fund";

        return ("," + allow_tables + ",").contains("," + table + ",");
    }

    @RequestMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> dict = new LinkedHashMap<>();

        dict.put("data_date", "");

        String query = api.json("query");
        String table = api.json("table");

        if (!query.equals("")) {
            String data_date = api.json("data_date");
            String dept = api.json("dept_no", index_m.summary_dept_no);

            String[] forbidden = {"insert ", " update ", "delete ", "drop "};

            for (String fb_str : forbidden) {
                if(query.toLowerCase().contains(fb_str)) return api.err("SQL语句不支持非SELECT查询");
            }

            int from_pos = query.toLowerCase().indexOf(" from ");

            String from_str = query.substring(from_pos);

            int table_pos = from_str.substring(6).trim().indexOf(" ");

            table = (from_str.substring(6, table_pos == -1 ? from_str.length() : table_pos + 6).trim().replace("`", "").replace("\"", "")).toLowerCase();

            query = query.replace("@data_date", data_date).replace("@which_dept", dept.equals(index_m.summary_dept_no) ? "" : " AND " + index_m.db.key_esc("dept_no") + "='" + index_m.db.escape(dept) + "' ");
        }

        if(query.equals("") && !this.allow(table)) return api.err("当前数据表禁止查看");

        Map<String, Object> map = com.map(api.json(), "table");

        map.put("#table", table);

        Map<String, Long> line = api.line(20);

        if (query.equals("") && map.getOrDefault("data_date", "").equals("latest")) {
            String latest_date = table_m.db.table("data_date").field("date", "date,desc");

            map.put("data_date", latest_date);

            dict.put("data_date", latest_date);
        }

        if (query.equals("")) {
            if (line.get("rows") == 0) line.put("rows", table_m.list_count(map));
        } else {
            if (line.get("rows") == 0) {
                String rows = table_m.db.single("SELECT COUNT(*) FROM (" + query + ") " + table_m.db.key_esc("__T"));

                if (rows.equals("")) return api.err("查询语句有错误，请在”指标管理 - 指标维护“中检查");

                line.put("rows", Long.parseLong(rows));
            }
        }

        if (line.get("page") == 0) line.put("page", (long)Math.ceil((double)line.get("rows") / (double)line.get("size")));

        map.put("#limit", line.get("size") + "," + ((line.get("page") - 1) * line.get("size")));

        map.put("#order", "id,asc");

        map.put("#field", "id");

        List<Map<String, String>> list;

        if (query.equals("")) {
            list = table_m.db.table(table).read(map);
        } else {
            list = table_m.db.result(query, line.get("size"), (line.get("page") - 1) * line.get("size"));
        }

        api.set_line(line);

        if (list.size() > 0) {
            Map<String, Object> list_map = new LinkedHashMap<>();

            String in_str = com.join(list, "id", ",");

            list_map.put("#table", table);
            list_map.put("in#id", in_str);
            list_map.put("#order", "field(id," + in_str + ")");

            List<Map<String, String>> field_list = table_m.db.table("set_base_field").read("*", "sort,asc;id,asc", "table", table);

            dict.put("field_list", field_list);

            api.set("dict", dict);

            return api.put(table_m.list(list_map));
        } else {
            api.set("dict", dict);

            return api.err("没有数据");
        }
    }

    @RequestMapping("/field_list")
    public Map<String, Object> field_list() {
        this.get_field_list();

        Map<String, Object> map = com.map(api.json());

        map.put("#table", "set_base_field");

        Map<String, Long> line = api.line(20);

        if (line.get("rows") == 0) line.put("rows", table_m.list_count(map));
        if (line.get("page") == 0) line.put("page", (long)Math.ceil((double)line.get("rows") / (double)line.get("size")));

        api.set_line(line);

        map.put("#limit", line.get("size") + "," + ((line.get("page") - 1) * line.get("size")));

        map.put("#order", "sort,asc;id,asc");

        List<Map<String, String>> list = table_m.db.table("set_base_field").read(map);

        if (list.size() > 0) {
            return api.put(list);
        } else {
            return api.err("没有数据");
        }
    }

    @RequestMapping("/get_field_list")
    public Map<String, Object> get_field_list() {
        String table = api.json("table");

        if(!this.allow(table)) return api.err("当前数据表禁止查看");

        List<Map<String, String>> field_list_1 = table_m.db.table("set_base_field").read("*", "sort,asc;id,asc", "table", table);

        List<Map<String, String>> field_list_2 = table_m.db.cols_map(table);

         for (Map<String, String> field_item_1 : field_list_1) {
             boolean del = true;

             for (Map<String, String> field_item_2 : field_list_2) {
                 if (field_item_1.get("name").equals(field_item_2.get("column_name"))) {
                     del = false;

                     break;
                 }
             }

             if (del) table_m.db.table("set_base_field").delete(field_item_1.get("id"));
         }

        for (Map<String, String> field_item_2 : field_list_2) {
            boolean add = true;

            for (Map<String, String> field_item_1 : field_list_1) {
                if (field_item_2.get("column_name").equals(field_item_1.get("name"))) {
                    add = false;

                    break;
                }
            }

            if (add) {
                Map<String, String> data = new LinkedHashMap<>();

                data.put("table", table);

                data.put("name", field_item_2.get("column_name"));

                String[] comment = field_item_2.get("column_comment").split("\\|");

                data.put("cname", comment[0]);

                String sort = table_m.db.table("set_base_field").field("sort", "sort,desc;id,desc", "table", table);

                int sort_int = sort.equals("") ? 0 : Integer.parseInt(sort);

                data.put("sort", (sort_int + 10) + "");

                table_m.db.table("set_base_field").create(data);
            }
        }

        return api.msg("字段列表已更新");
    }

    @RequestMapping("/set_base_field")
    public Map<String, Object> set_base_field() {
        String base_field_id = api.json("base_field_id");

        Map<String, String> data = new LinkedHashMap<>();

        for (String key : api.json().keySet()) {
            if (key.equals("base_field_id")) continue;

            data.put(key, api.json(key));
        }

        if (table_m.db.table("set_base_field").update(base_field_id, data) > 0) {
            return api.put(table_m.db.table("set_base_field").find("*", "id", base_field_id), "修改成功");
        } else {
            return api.err("修改失败");
        }
    }

    @RequestMapping("/report_form_list")
    public Map<String, Object> report_form_list() {
        return sql_c.query("select * from report_form order by id desc");
    }

    @RequestMapping("/report_form_import")
    public Map<String, Object> report_form_import(String date, String file) {
        if (date == null) date = api.json("date");
        if (file == null) file = api.json("file");

        if (!check.required(file) || !(new File(file)).isFile()) return api.err("数据文件不存在");

        if (!check.required(date)) return api.err("数据日期不能为空");

        table_m.db.table("report_form").delete("datadate", date);

        final Map<String, String> info = new LinkedHashMap<String, String>(){{
            put("en_name", "");
            put("zh_name", "");
        }};

        final Map<Integer, String> cols = new LinkedHashMap<>();
        final Map<Integer, String> unit = new LinkedHashMap<>();

        poi.read_xls(file, 0, (line, line_no) -> {
            if (line_no == -1) {
                info.put("en_name", line.get(0));
            }

            System.out.println("======");

            for (int cell_no : line.keySet()) {
                System.out.println(line.get(cell_no));
            }

            System.out.println("======");
        });

        if (!(new File(file)).delete()) {
            System.out.println("Failed to delete temp file: " + file);
        }

        return api.msg("导入成功");
    }
}
