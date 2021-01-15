package com.sox.api.controller;

import com.sox.api.interceptor.CheckLogin;
import com.sox.api.model.UserModel;
import com.sox.api.service.Api;
import com.sox.api.service.Img;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@EnableAutoConfiguration
@RequestMapping("/common")
public class CommonController {
    @Autowired
    private Api api;

    @Autowired
    private UserModel user_m;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${sox.upload_dir}")
    private String upload_dir;

    @RequestMapping("/time")
    public Map<String, Object> time() {
        Map<String, Object> data = new HashMap<>();

        data.put("time",Long.toString(System.currentTimeMillis() / 1000L));

        return api.put(data);
    }

    @CheckLogin
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String user_id = user_m.get_session("id");

        if (file.isEmpty()) return api.err("未检测到文件流");

        String step = api.get("step");
        String path = upload_dir;

        String raw_file_name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();

        if (!raw_file_name.contains(".")) return api.err("文件类型未知");

        String file_type = raw_file_name.substring(raw_file_name.lastIndexOf(".") + 1).toLowerCase();
        String file_name = user_id + "-" + System.currentTimeMillis() + "." + file_type;

        if (step.equals("cover")) {
            if (!Arrays.asList(("jpg,jpeg,png,gif").split(",")).contains(file_type)) return api.err("不是预期的文件类型");
        }

        File upload_dir = new File(path);

        if (!upload_dir.isDirectory()) {
            if (!upload_dir.mkdirs()) {
                return api.err("上传目录创建失败");
            }
        }

        if (!upload_dir.canWrite()) {
            return api.err("上传目录不可用");
        }

        File save_file = new File(path, file_name);

        try {
            file.transferTo(save_file);
        } catch(Exception e) {
            e.printStackTrace();

            return api.err("上传失败");
        }

        if (step.equals("cover")) {
            Img img = new Img();

            img.crop(path + file_name, 320, 320);
        }

        Map<String, String> result = new HashMap<>();

        String base_url = request.getScheme() + "://" + request.getServerName() + (request.getServerPort() != 80 && request.getServerPort() != 443 ? ":" + request.getServerPort() : "");

        result.put("file", base_url + "/common/file/" + file_name);
        result.put("client_name", raw_file_name);

        return api.put(result);
    }

    @RequestMapping("/file/{file_name}")
    public ResponseEntity file(@PathVariable("file_name") final String file_name) {
        String path = upload_dir;

        try {
            return ResponseEntity.ok(resourceLoader.getResource("file:" + path + file_name));
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.notFound().build();
        }
    }
}
