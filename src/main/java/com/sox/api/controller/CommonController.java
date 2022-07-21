package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.CodeModel;
import com.sox.api.model.ConfModel;
import com.sox.api.model.IndexModel;
import com.sox.api.model.UserModel;
import com.sox.api.service.Api;
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
    public Map<String, Object> time() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("time",Long.toString(System.currentTimeMillis() / 1000L));

        return api.put(data);
    }

    @RequestMapping("/date_list")
    public Map<String, Object> date_list() {
        List<Map<String, String>> list = user_m.db.table("data_date").read("id,date", "date,desc");

        return api.put(list);
    }

    @CheckLogin
    @RequestMapping("/init")
    public Map<String, Object> init() {
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
    public Map<String, Object> home() {
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
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
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

        return api.put(result);
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
}
