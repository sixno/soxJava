package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.CodeModel;
import com.sox.api.model.ConfModel;
import com.sox.api.model.IndexModel;
import com.sox.api.model.UserModel;
import com.sox.api.service.Api;
import com.sox.api.service.Check;
import com.sox.api.service.Com;
import com.sox.api.service.Img;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/common")
public class CommonController {
    @Value("${sox.upload_dir}")
    private String upload_dir;

    @Autowired
    private Com com;

    @Autowired
    private Api api;

    @Autowired
    private Img img;

    @Autowired
    private Check check;

    @Autowired
    private CodeModel code_m;

    @Autowired
    private ConfModel conf_m;

    @Autowired
    private UserModel user_m;

    @Autowired
    private UserController user_c;

    @Autowired
    private IndexModel index_m;

    @Autowired
    private IndexController index_c;

    @Autowired
    private ResourceLoader resourceLoader;

    @RequestMapping("/time")
    public Api.Res time() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("time",Long.toString(System.currentTimeMillis() / 1000L));

        return api.put(data);
    }

    @RequestMapping("/date_list")
    public Api.Res date_list() {
        List<Map<String, String>> list = user_m.db.table("data_date").read("id,date", "date,desc");

        return api.put(list);
    }

    @CheckLogin
    @RequestMapping("/init")
    public Api.Res init() {
        // 本接口返回用户登录后界面初始化数据
        // 本接口为数据聚合接口，提供强一致性接口数据
        Map<String, Object> user_auth = user_c.get_auth();
        Map<String, Object> date_list = this.date_list();

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("user_auth", user_auth.get("data"));
        data.put("date_list", date_list.get("data"));

        return api.put(data);
    }

    @CheckLogin
    @RequestMapping("/home")
    public Api.Res home() {
        // 本接口返回用户登录后首页初始化数据
        // 本接口为数据聚合接口，提供强一致性接口数据
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("z_100001", index_m.item_pack("100001"));
        data.put("z_100002", index_m.item_pack("100002"));
        data.put("z_100004", index_m.item_pack("100004"));
        data.put("z_100007", index_m.item_pack("100007"));
        data.put("z_100008", index_m.item_pack("100008"));
        data.put("z_100009", index_m.item_pack("100009"));
        data.put("z_100010", index_m.item_pack("100010"));
        data.put("z_100011", index_m.item_pack("100011"));

        data.put("index_bat_01", index_c.chart_bat("100001,100002", ""));

        Map<String, String> home_conf = new LinkedHashMap<>();

        home_conf.put("home_1", conf_m.get("home_1"));
        home_conf.put("home_2", conf_m.get("home_2"));
        home_conf.put("home_3", conf_m.get("home_3"));
        home_conf.put("home_4", conf_m.get("home_4"));

        home_conf.put("home_5", conf_m.get("home_5"));

        home_conf.put("home_6", conf_m.get("home_6"));

        data.put("home_conf", home_conf);

        return api.put(data);
    }

    @CheckLogin
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public Api.Res upload(@RequestParam("file") MultipartFile file) {
        int tag = 0;

        String user_id = user_m.get_session("id");

        if (file.isEmpty()) return api.err("未检测到文件流");

        String step = com.http_get("step");
        String path = com.path(upload_dir + File.separator);

        String sub_dir = "";

        switch (step) {
            case "temp":
                sub_dir = "temp" + File.separator;
                break;
            case "cover":
                sub_dir = "cover" + File.separator;
                break;
            case "kettle":
                tag = 1;

                sub_dir = "kettle" + File.separator;
                break;
        }

        String raw_file_name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();

        if (!raw_file_name.contains(".")) return api.err("文件类型未知");

        String file_type = raw_file_name.substring(raw_file_name.lastIndexOf(".") + 1).toLowerCase();
        String file_name = user_id + "-" + System.currentTimeMillis() + "." + file_type;

        if (step.equals("cover")) {
            if (!Arrays.asList(("jpg,jpeg,png,gif").split(",")).contains(file_type)) return api.err("不是预期的文件类型");
        }

        if (step.equals("kettle")) {
            if (!Arrays.asList(("ktr,kjb").split(",")).contains(file_type)) return api.err("不是预期的文件类型");

            if (!raw_file_name.matches("[0-9A-Za-z_\\.\\-]*")) return api.err("文件名只能由字母、数字、下划线组成");

            file_name = raw_file_name;
        }

        File dir_obj = new File(path + sub_dir);

        if (!dir_obj.isDirectory()) {
            if (!dir_obj.mkdirs()) {
                return api.err("上传目录创建失败");
            }
        }

        if (!dir_obj.canWrite()) {
            return api.err("上传目录不可用");
        }

        File save_file = new File(path + sub_dir, file_name);

        try {
            file.transferTo(save_file);
        } catch(Exception e) {
            e.printStackTrace();

            return api.err("上传失败");
        }

        if (step.equals("cover")) {
            img.crop(path + sub_dir + file_name, 320, 320);
        }

        Map<String, String> result = new LinkedHashMap<>();

        switch (step) {
            case "kettle":
                result.put("file", file_name);
                break;
            case "temp":
                result.put("file", path + sub_dir + file_name);
                break;
            default:
                result.put("file", com.base_url("/common/file/" + sub_dir.replace(File.separator, "/") + file_name));
                break;
        }

        result.put("client_name", raw_file_name);

        // 如果不是临时文件则记录
        if(!step.equals("temp")) com.add_upload_record(tag, result.get("file"), file_name, upload_dir + File.separator + sub_dir + file_name, user_id);

        return api.put(result);
    }

    @CheckLogin
    @RequestMapping("/upload_list")
    public Api.Res upload_list() {
        Map<String, Object> map = com.map(api.arg());

        Api.Line line = api.line(20);

        if (line.rows == 0) line.rows = conf_m.db.table("file_upload").count(map);
        if (line.rows == 0) line.page = api.page(line);

        api.set_line(line);

        map.put("#limit", line);

        map.put("#order", "id,desc");

        List<Map<String, String>> list = conf_m.db.table("file_upload").read(map);

        if (list.size() > 0) {
            return api.put(list);
        } else {
            return api.err("没有数据");
        }
    }

    @RequestMapping("/file/**")
    public ResponseEntity<Resource> file(HttpServletRequest request) {
        String file_name = request.getServletPath();

        if (file_name.startsWith("/common/file/kettle/")) return ResponseEntity.notFound().build();

        String path = com.path(upload_dir + File.separator);

        String type = file_name.substring(file_name.lastIndexOf(".") + 1).toLowerCase();

        String download_name = com.http_get("download_name");

        try {
            ResponseEntity.BodyBuilder responseEntity = ResponseEntity.ok().header("Content-Type", code_m.state("mime", type, 0, "application/octet-stream"));

            if (!download_name.equals("")) responseEntity.header("Content-Disposition", "attachment;filename*=utf-8'zh_cn'" + URLEncoder.encode(download_name, "UTF-8"));

            return responseEntity.body(resourceLoader.getResource("file:" + path + file_name.substring(13).replace("/", File.separator)));
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping("/sort")
    public Api.Res sort() {
        Map<String, String> data = api.arg();

        check.reset(data);

        check.validate("table", "表名", "required");
        check.validate("scope", "范围", "required", "@@NULL"); // 为 null 时忽略必填
        check.validate("major", "数据主键", "required");

        if (check.result.get()) {
            String prev = data.getOrDefault("prev", "");
            String next = data.getOrDefault("next", "");
            String sort = data.getOrDefault("sort", "");
            // String desc = data.getOrDefault("desc", "0");

            if (prev.equals("") && next.equals("")) return api.err("操作失败");

            if (sort.equals("")) sort = "sort";

            Map<String, String> curr_data = user_m.db.table(data.get("table")).find("*", "id", data.get("major"));

            Map<String, String> prev_data = user_m.db.table(data.get("table")).find("*", "id", prev);
            Map<String, String> next_data = user_m.db.table(data.get("table")).find("*", "id", next);

            Map<String, Object> scope = new LinkedHashMap<>();

            if (data.get("scope") != null) {
                String[] scope_arr = data.get("scope").split(",");

                for (int i = 0;i < scope_arr.length;i += 2) {
                    if (i > scope_arr.length - 2) break;

                    scope.put(scope_arr[i], scope_arr[i + 1]);
                }
            }

            if (prev_data.size() == 0) {
                Map<String, Object> prev_map = new LinkedHashMap<>(scope);

                prev_map.put("not_in#id", next_data.get("id") + "," + curr_data.get("id"));
                prev_map.put(sort + " <=", next_data.get(sort));
                prev_map.put("#order", sort + ",desc;id,desc");

                prev_data = user_m.db.table(data.get("table")).find(prev_map);
            }

            if (next_data.size() == 0) {
                Map<String, Object> next_map = new LinkedHashMap<>(scope);

                next_map.put("not_in#id", prev_data.get("id") + "," + curr_data.get("id"));
                next_map.put(sort + " >=", prev_data.get(sort));
                next_map.put("#order", sort + ",asc;id,asc");

                next_data = user_m.db.table(data.get("table")).find(next_map);
            }

            if (prev_data.size() == 0 && next_data.size() == 0) return api.err("操作失败");

            if (prev_data.size() == 0) {
                Map<String, Object> update_map = new LinkedHashMap<>(scope);

                int next_sort = Integer.parseInt(next_data.get("sort"));

                if (next_sort <= 1) {
                    user_m.db.table(data.get("table")).update(1, "id", curr_data.get("id"), sort, "1");

                    update_map.put("id !=", curr_data.get("id"));

                    String finalSort = sort;
                    user_m.db.table(data.get("table")).increase(update_map, new LinkedHashMap<String, String>(){{
                        put(finalSort, (2 - next_sort) + "");
                    }});
                } else {
                    user_m.db.table(data.get("table")).update(1, "id", curr_data.get("id"), sort, (next_sort - 1) + "");
                }
            } else if (next_data.size() == 0) {
                int prev_sort = Integer.parseInt(prev_data.get("sort"));

                user_m.db.table(data.get("table")).update(1, "id", curr_data.get("id"), sort, (prev_sort + 1) + "");
            } else {
                Map<String, Object> update_map = new LinkedHashMap<>(scope);

                int prev_sort = Integer.parseInt(prev_data.get("sort"));
                int next_sort = Integer.parseInt(next_data.get("sort"));

                if (next_sort - prev_sort <= 1) {
                    user_m.db.table(data.get("table")).update(1, "id", curr_data.get("id"), sort, (prev_sort + 1) + "");

                    update_map.put(sort + " >", next_sort);
                    update_map.put("^1", "or (");
                    update_map.put(sort, next_sort);
                    update_map.put("id >=", next_data.get("id"));
                    update_map.put("^2", ")");
                    update_map.put("id !=", curr_data.get("id"));

                    String finalSort = sort;
                    user_m.db.table(data.get("table")).increase(update_map, new LinkedHashMap<String, String>(){{
                        put(finalSort, (prev_sort + 2 - next_sort) + "");
                    }});
                } else {
                    int delta = next_sort - prev_sort;

                    user_m.db.table(data.get("table")).update(1, "id", curr_data.get("id"), sort, (prev_sort + (delta - delta % 2) / 2) + "");
                }
            }

            return api.msg("操作成功");
        } else {
            api.res().err = check.errors.get();

            return api.err(check.error.get());
        }
    }
}
