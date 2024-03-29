package com.sox.api.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.sox.api.model.CodeModel;
import com.sox.api.model.UserModel;
import org.apache.tomcat.util.buf.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class Com {
    @Value("${sox.host_id}")
    public String host_id;

    @Value("#{${sox.rsa.pri_key}}")
    public Map<String, String> pri_keys;

    @Value("${sox.super_user}")
    public String super_user;

    @Autowired
    public Log log;

    @Autowired
    public Api api;

    @Autowired
    public Db db;

    @Autowired
    public Check check;

    @Autowired
    public Disk disk;

    @Autowired
    public CodeModel code_m;

    @Autowired
    public UserModel user_m;

    public final ThreadLocal<HttpServletRequest> http_request = new ThreadLocal<>();
    public final ThreadLocal<HttpServletResponse> http_response = new ThreadLocal<>();

    public final ThreadLocal<String> http_g_str = ThreadLocal.withInitial(() -> "");
    public final ThreadLocal<Map<String, String>> http_arg_g = new ThreadLocal<>();
    public Map<String, String> http_get() {
        if (http_arg_g.get() == null) {
            http_arg_g.set(new LinkedHashMap<>());

            if (!http_g_str.get().equals("")) {
                try {
                    String[] qs = http_g_str.get().split("&");

                    for (String q : qs) {
                        q = URLDecoder.decode(q, "UTF-8");

                        int pos = q.indexOf("=");

                        if(pos > 0) http_arg_g.get().put(q.substring(0, pos), q.substring(pos + 1));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return http_arg_g.get();
    }
    public String http_get(String key, String... def) {
        String def_0 = def.length > 0 ? def[0] : "";

        return this.http_get().get(key) != null ? this.http_get().get(key) : def_0;
    }

    public final ThreadLocal<String> http_p_str = ThreadLocal.withInitial(() -> "");
    public final ThreadLocal<Map<String, String>> http_arg_p = new ThreadLocal<>();
    public Map<String, String> http_post() {
        if (http_arg_p.get() == null) {
            http_arg_p.set(new LinkedHashMap<>());

            if (!http_p_str.get().equals("")) {
                try {
                    String[] qs = http_p_str.get().split("&");

                    for (String q : qs) {
                        q = URLDecoder.decode(q, "UTF-8");

                        int pos = q.indexOf("=");

                        if (pos > 0) http_arg_p.get().put(q.substring(0, pos), q.substring(pos + 1));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return http_arg_p.get();
    }
    public String http_post(String key, String... def) {
        String def_0 = def.length > 0 ? def[0] : "";

        return this.http_post().get(key) != null ? this.http_post().get(key) : def_0;
    }

    public final ThreadLocal<String> http_j_str = ThreadLocal.withInitial(() -> "");
    public final ThreadLocal<Map<String, String>> http_arg_j = new ThreadLocal<>();
    public Map<String, String> http_json() {
        if (http_arg_j.get() == null) {
            http_arg_j.set(new LinkedHashMap<>());

            if (!http_j_str.get().equals("")) {
                try {
                    JSONObject json_obj = JSONObject.parseObject(http_j_str.get(), Feature.OrderedField);

                    if (json_obj.size() > 0) {
                        for (String json_key : json_obj.keySet()) {
                            http_arg_j.get().put(json_key, json_obj.get(json_key).toString());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return http_arg_j.get();
    }
    public String http_json(String key, String... def) {
        String def_0 = def.length > 0 ? def[0] : "";

        return this.http_json().get(key) != null ? this.http_json().get(key) : def_0;
    }

    public String net_addr = null;
    public String net_addr() {
        if (net_addr == null) {
            net_addr = "";

            try {
                InetAddress addr = InetAddress.getLocalHost();

                net_addr = addr.getHostAddress();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return net_addr;
    }

    public String mac_addr = null;
    public String mac_addr(boolean... no_cat) {
        if (mac_addr == null) {
            mac_addr = "";

            try {
                // 获取网卡，获取地址
                InetAddress addr = InetAddress.getLocalHost();

                byte[] mac = NetworkInterface.getByInetAddress(addr).getHardwareAddress();

                StringBuilder sb = new StringBuilder();

                for(int i = 0;i < mac.length;i++) {
                    if(i > 0) {
                        sb.append("-");
                    }

                    // 字节转换为整数
                    int temp = mac[i] & 0xff;

                    String str = Integer.toHexString(temp);

                    if(str.length() == 1) {
                        sb.append("0").append(str);
                    }else {
                        sb.append(str);
                    }
                }

                mac_addr = sb.toString().toUpperCase();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return no_cat.length > 0 && no_cat[0] ? mac_addr.replace("-", "") : mac_addr;
    }

    public boolean check_login() {
        return !user_m.get_session("id").equals("");
    }

    public boolean check_super() {
        return user_m.get_session("id").equals(super_user);
    }

    public boolean check_auth(String index, String value) {
        String user_id = user_m.get_session("id");

        if (user_id.equals("")) return false;

        Map<String, String> user_auth = user_m.get_auth(user_id);

        return ("," + user_auth.getOrDefault(index, "") + ",").contains("," + value + ",");
    }

    public boolean check_sign() {
        return true;
    }

    public String sign(Map<String, String> data, String ak, String sk, String... time) {
        StringBuilder ds = new StringBuilder(time.length > 0 ? time[0] : this.http_get("time"));

        if (ds.toString().equals("")) return "";

        if (data != null && data.size() > 0) {
            Map<String, String> map = new TreeMap<>();

            for (String key : data.keySet()) {
                map.put(key, data.get(key));
            }

            List<Map.Entry<String, String>> list = new ArrayList<>(map.entrySet());

            list.sort(Map.Entry.comparingByKey());

            for (Map.Entry<String, String> stringStringEntry : list) {
                ds.append("&").append(stringStringEntry.getKey()).append("=").append(stringStringEntry.getValue());
            }
        }

        return this.md5(sk + this.md5(ak + ds));
    }

    public String at_var(String var) {
        if (var.startsWith("@")) {
            String key = var.substring(1);
            String val = "";

            int pos = var.indexOf("#");

            if (pos > -1) {
                key = var.substring(1, pos);
                val = var.substring(pos + 1);
            }

            String[] arg = val.equals("") ? new String[0] : val.split("\\|");

            switch (key) {
                case "month_no": // 当年第 n 个月
                    if (arg.length == 0) {
                        var = Integer.parseInt(this.date("MM")) + "";
                    } else {
                        var = Integer.parseInt(this.date("MM", arg[0])) + "";
                    }
                    break;
                case "date": // 获取当前日期
                    var = this.date("yyyy-MM-dd");
                    break;
                case "time": // 获取当前时间
                    var = this.date("yyyy-MM-dd HH:mm:ss");
                    break;
                case "day":
                    int delta_days = arg.length > 0 ? Integer.parseInt(arg[0]) : 0;

                    Long time = this.time();

                    time = time + delta_days * 86400;

                    var = this.date("yyyy-MM-dd", time);
                    break;
                case "last_month_last_day":
                    String this_month = this.date("yyyy-MM");
                    Long this_month_first_day = this.time(this_month + "-01 12:00:00");

                    var = this.date("yyyy-MM-dd", this_month_first_day - 86400);
                    break;
                case "first_day_of_year":
                    String f_year = arg.length > 0 ? arg[0].trim() : this.date("yyyy"); // 默认当年

                    var = f_year + "-01-01";
                    break;
                case "last_day_of_year":
                    String l_year = arg.length > 0 ? arg[0].trim() : this.date("yyyy"); // 默认当年

                    var = l_year + "-12-31";
                    break;
                case "latest_data_date":
                    var = db.table("data_date").field("date", "date,desc");
                    break;

                default:
                    if (key.contains(":")) {
                        String[] code = key.split(":");

                        var = code_m.state(code[0], code[1], code.length > 2 ? Integer.parseInt(code[2]) : 0, code.length > 3 ? code[3] : "");
                    }
                    break;
            }
        }

        return var;
    }

    public String join(List<String> list, String separator) {
        StringBuilder sb = new StringBuilder();

        for (String item : list) {
            if(!item.equals("")) sb.append(item).append(separator);
        }

        return sb.toString().equals("") ? "" : sb.toString().substring(0, sb.toString().length() - separator.length());
    }

    public String join(List<Map<String, String>> list, String field, String separator) {
        StringBuilder sb = new StringBuilder();

        for (Map<String, String> item : list) {
            sb.append(item.get(field)).append(separator);
        }

        return sb.toString().substring(0, sb.toString().length() - separator.length());
    }

    public boolean str_search(String stack, String needle, String... separator) {
        String sep = separator.length > 0 ? separator[0] : ",";

        return (sep + stack + sep).contains(sep + needle + sep);
    }

    public Long time(String... arg) {
        if (arg.length == 0) {
            return System.currentTimeMillis() / 1000L;
        } else {
            String date = arg[0];
            String pattern = arg.length > 1 ? arg[1] : "yyyy-MM-dd HH:mm:ss";

            try {
                Date date_obj = new SimpleDateFormat(pattern).parse(date);

                return date_obj.getTime() / 1000L;
            } catch (Exception e) {
                e.printStackTrace();

                return null;
            }
        }
    }

    public String date(String pattern, Long... time) {
        // pattern example: yyyy-MM-dd HH:mm:ss z
        SimpleDateFormat format = new SimpleDateFormat(pattern);

        Date date = new Date(time.length == 0 ? System.currentTimeMillis() : time[0] * 1000L);

        return format.format(date);
    }

    public String date(String pattern, String date, String... def) {
        Date date_obj;

        String date_def = def.length > 0 ? def[0] : "";

        SimpleDateFormat format = new SimpleDateFormat(pattern);

        int date_len = date.length();

        switch (date_len) {
            case 8:
                try {
                    date_obj = new SimpleDateFormat("yyyyMMdd").parse(date);
                } catch (Exception e) {
                    return date_def;
                }
                break;
            case 10:
                try {
                    date_obj = new SimpleDateFormat("yyyy-MM-dd").parse(date);
                } catch (Exception e) {
                    return date_def;
                }
                break;
            default:
                return date_def;
        }

        return format.format(date_obj);
    }

    public Map<String, Object> str_obj_map(Map<String, String> map) {
        Map<String, Object> str_obj = new LinkedHashMap<>();

        if (map == null) return str_obj;

        for (String key : map.keySet()) {
            str_obj.put(key, map.get(key));
        }

        return str_obj;
    }

    public Map<String, String> str_str_map(Map<String, Object> map) {
        Map<String, String> str_str = new LinkedHashMap<>();

        if (map == null) return str_str;

        for (String key : map.keySet()) {
            str_str.put(key, map.get(key).toString());
        }

        return str_str;
    }

    public Map<String, String> dict_map(List<Map<String, String>> list, String key, String val) {
        Map<String, String> dict = new LinkedHashMap<>();

        for (Map<String, String> item : list) {
            dict.put(item.get(key), item.get(val));
        }

        return dict;
    }

    public String map_md5(Object map, Object... ext) {
        String str = JSONObject.toJSONString(map);

        for (int i = 0;i < ext.length;i++) {
            str += JSONObject.toJSONString(ext[i]);
        }

        return this.md5(str);
    }

    public Map<String, Object> map(Map<String, String> input, String... except) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (input == null) return map;

        for (String key : input.keySet()) {
            if (key.startsWith("__")) continue;
            if (input.get(key).equals("")) continue;

            if (except.length > 0 && ("," + StringUtils.join(Arrays.asList(except), ',') + ",").contains("," + key + ",")) continue;

            // __like %_like _%like __gt __lt __ge __le

            if (key.endsWith("##")) {
                map.put("#" + key.substring(0, key.length() - 2), input.get(key));
            } else if (key.endsWith("__like")) {
                String field = key.substring(0, key.length() - 6);

                if (field.equals("")) continue;

                map.put("like#" + field, input.get(key));
            } else if(key.endsWith("_%like")) {
                String field = key.substring(0, key.length() - 6);

                if (field.equals("")) continue;

                map.put("like#" + field + "%", input.get(key));
            } else if(key.endsWith("%_like")) {
                String field = key.substring(0, key.length() - 6);

                if (field.equals("")) continue;

                map.put("like#%" + field, input.get(key));
            } else if(key.endsWith("__gt")) {
                String field = key.substring(0, key.length() - 4);

                if (field.equals("")) continue;

                map.put(field + " >=", input.get(key));
            } else if(key.endsWith("__ge")) {
                String field = key.substring(0, key.length() - 4);

                if (field.equals("")) continue;

                map.put(field + " >=", input.get(key));
            } else if(key.endsWith("__lt")) {
                String field = key.substring(0, key.length() - 4);

                if (field.equals("")) continue;

                map.put(field + " <", input.get(key));
            } else if(key.endsWith("__le")) {
                String field = key.substring(0, key.length() - 4);

                if (field.equals("")) continue;

                map.put(field + " <=", input.get(key));
            } else {
                if (!input.get(key).contains(",")) {
                    if (input.get(key).contains("|")) {
                        map.put("in#" + key, input.get(key).split("\\|"));
                    } else {
                        map.put(key, input.get(key));
                    }
                } else {
                    String cluster = "";
                    int insert = 0;

                    if (input.get(key).contains("|")) {
                        String[] sections = input.get(key).split("\\|");

                        map.put("^" + insert + "-" + key, "and (");

                        for (String section : sections) {
                            String[] condition = section.split(",");

                            insert++;

                            map.put("^" + insert + "-" + key, "or (");

                            if (condition[0] != null && !condition[0].endsWith("-")) {
                                cluster += "#";

                                if (condition[0].startsWith("(")) {
                                    map.put("and" + cluster + key + " >", condition[0].substring(1));
                                } else {
                                    map.put("and" + cluster + key + " >=", condition[0].substring(1));
                                }
                            }

                            if (condition[1] != null && !condition[1].startsWith("+")) {
                                cluster += "#";

                                if (condition[1].endsWith(")")) {
                                    map.put("and" + cluster + key + " <", condition[1].substring(0, condition[1].length() - 1));
                                } else {
                                    map.put("and" + cluster + key + " <=", condition[1].substring(0, condition[1].length() - 1));
                                }
                            }

                            insert++;

                            map.put("^" + insert + "-" + key, ")");
                        }

                        insert++;

                        map.put("^" + insert + "-" + key, ")");
                    } else {
                        String[] condition = input.get(key).split(",");

                        if (condition[0] != null && !condition[0].endsWith("-")) {
                            cluster += "#";

                            if (condition[0].startsWith("(")) {
                                map.put("and" + cluster + key + " >", condition[0].substring(1));
                            } else {
                                map.put("and" + cluster + key + " >=", condition[0].substring(1));
                            }
                        }

                        if (condition[1] != null && !condition[1].startsWith("+")) {
                            cluster += "#";

                            if (condition[1].endsWith(")")) {
                                map.put("and" + cluster + key + " <", condition[1].substring(0, condition[1].length() - 1));
                            } else {
                                map.put("and" + cluster + key + " <=", condition[1].substring(0, condition[1].length() - 1));
                            }
                        }
                    }
                }
            }
        }

        return map;
    }

    public Map<String, String> dim_map(String dim) {
        Map<String, String> map = new LinkedHashMap<>();

        if (dim.contains(":")) {
            // 格式 {field}:{index},{level},{extra},{prev}
            String[] arr = dim.split(":");

            map.put("field", arr[0]);

            String[] arr_1 = arr[1].split(",");

            map.put("index", arr_1[0]);
            map.put("level", arr_1.length > 1 ? arr_1[1] : "0");

            map.put("extra", arr_1.length > 2 ? arr_1[2] : "");

            map.put("prev", arr_1.length > 3 ? arr_1[3] : "");
        } else {
            String[] arr = dim.split("!");

            map.put("field", arr[0]);
            map.put("index", arr[0]);
            map.put("level", "0");

            if (arr.length > 1) {
                // 注意这里是先配置 extra，再配置 prev，以“;”分割

                String[] arr_1 = arr[1].split(";");

                map.put("extra", arr_1[0]);

                map.put("prev", arr_1.length > 1 ? arr_1[1] : "");
            } else {
                map.put("extra", "");

                map.put("prev", "");
            }

            Pattern pattern = Pattern.compile("_\\d+$");
            Matcher matcher = pattern.matcher(arr[0]);

            if (matcher.find()) {
                map.put("index", arr[0].substring(0, arr[0].length() - matcher.group().length()));
                map.put("level", matcher.group().substring(1));
            }
        }

        return map;
    }

    public String path(String path) {
        return System.getProperty("user.dir") + java.io.File.separator + path.replace("/", java.io.File.separator);
    }

    public String base_url(String... uri) {
        String base_url = "";

        HttpServletRequest request = http_request.get();

        if (request != null) {
            base_url = (request.getHeader("X-Forwarded-Scheme") == null ? request.getScheme() : request.getHeader("X-Forwarded-Scheme")) + "://" + request.getServerName() + (request.getServerPort() != 80 && request.getServerPort() != 443 ? ":" + request.getServerPort() : "");
        }

        return base_url + (uri.length > 0 ? uri[0] : "");
    }

    public String resource(String path, String... delimiters) {
        BufferedReader reader;
        String content = "";

        String delimiter = delimiters.length > 0 ? delimiters[0] : "\n";

        try {
            ClassPathResource resource = new ClassPathResource(path);

            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));

            content = reader.lines().collect(Collectors.joining(delimiter));

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return content;
    }

    public String remote_ip() {
        HttpServletRequest request = http_request.get();

        if (request == null) return "";

        String ip = request.getHeader("x-forwarded-for");

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "X-Real-IP".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    public String num_rand(int min, int max, int... ext) {
        int ext_0 = ext.length > 0 ? ext[0] : 1; // 乘数
        int ext_1 = ext.length > 1 ? ext[1] : 1; // 除数
        int ext_2 = ext.length > 2 ? ext[2] : 0; // 是否有符号随机

        Random random = new Random();

        int num = random.nextInt(max - min + 1) + min;

        String result = (ext_2 == 1 && Math.random() < 0.5 ? "-" : "") + ((double) num * (double) ext_0 / (double) ext_1);

        return result.endsWith(".0") ? result.substring(0, result.length() - 2) : result;
    }

    public String hash(String source, String hashType) {
        StringBuilder sb = new StringBuilder();
        MessageDigest md5;

        try {
            md5 = MessageDigest.getInstance(hashType);

            md5.update(source.getBytes());

            for (byte b : md5.digest()) {
                // 10进制转16进制，X 表示以十六进制形式输出，02 表示不足两位前面补0输出
                sb.append(String.format("%02X", b));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return sb.toString().toLowerCase();
    }

    public String md5(String source) {
        return this.hash(source, "MD5");
    }

    public String sha1(String source) {
        return this.hash(source, "SHA");
    }

    public String salt_hash(String str, String salt, int length) {
        if (salt.equals("")) {
            salt = this.md5(System.currentTimeMillis() + "").substring(0, length);
        } else {
            salt = salt.substring(0, length);
        }

        return salt + this.sha1(salt + str);
    }

    public String salt_hash(String str, int length) {
        return this.salt_hash(str, "", length);
    }

    public String salt_hash(String str, String salt) {
        return this.salt_hash(str, salt, 8);
    }

    public String salt_hash(String str) {
        return salt_hash(str, 8);
    }

    public String base64_encode(String str) {
        byte[] bytes = str.getBytes();

        return new String(Base64.encodeBase64(bytes));
    }

    public String base64_decode(String str) {
        byte[] bytes = str.getBytes();

        return new String(Base64.decodeBase64(bytes));
    }

    public String rsa_encrypt(String str, String publicKey) {
        String outStr = "";

        //base64编码的公钥
        byte[] decoded = Base64.decodeBase64(publicKey);

        try {
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));

            //RSA加密
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);

            outStr = Base64.encodeBase64String(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return outStr;
    }

    public String rsa_decrypt(String str, String privateKey){
        if (str.equals("")) return "";

        String outStr = "";

        byte[] inputByte = Base64.decodeBase64(str.getBytes(StandardCharsets.UTF_8));

        //base64编码的私钥
        byte[] decoded = Base64.decodeBase64(privateKey);

        try {
            RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));

            //RSA解密
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, priKey);

            outStr = new String(cipher.doFinal(inputByte));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return outStr;
    }

    public String rsa_encrypt(String str) {
        String[] rsa = str.split(":");

        if (rsa.length < 2) return "";

        return rsa_encrypt(str.substring(str.indexOf(":") + 1), pri_keys.getOrDefault(rsa[0], ""));
    }

    public String rsa_decrypt(String str) {
        String[] rsa = str.split(":");

        if (rsa.length < 2) return "";

        return rsa_decrypt(rsa[1], pri_keys.getOrDefault(rsa[0], ""));
    }

    public String rsa_at_time(String str) {
        return rsa_encrypt(str + "@" + this.time());
    }

    public String rsa_at_time(String str, String time) {
        return rsa_encrypt(str + "@" + time);
    }

    public String rsa_at_time(String str, int allow_delay) {
        str = this.rsa_decrypt(str);

        if (!str.contains("@")) return "";

        int pos = str.lastIndexOf("@");

        if (Math.abs(Integer.parseInt(str.substring(pos + 1)) - this.time()) > allow_delay) return "";

        return str.substring(0, pos);
    }

    public String str_rand(String str,int len) {
        int str_len = str.length();

        Random random = new Random();

        StringBuffer sb = new StringBuffer();

        for (int i = 0;i < len;i++) {
            int pos = random.nextInt(str_len);

            sb.append(str.charAt(pos));
        }

        return sb.toString();

    }

    public String str_rand(int len) {
        return this.str_rand("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", len);
    }

    public byte[] str_key(String txt, String key) {
        key = this.md5(key);

        byte[] key_bytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] txt_bytes = txt.getBytes(StandardCharsets.UTF_8);

        int ctr = 0;

        byte[] tmp = new byte[txt_bytes.length];

        for (int i = 0;i < txt_bytes.length;i++) {
            ctr = ctr == key_bytes.length ? 0 : ctr;
            tmp[i] = (byte) (txt_bytes[i] ^ key_bytes[ctr++]);
        }

        return tmp;
    }

    public String str_encrypt(String txt, String key) {
        String mix = this.str_rand(4);

        byte[] mix_bytes = mix.getBytes(StandardCharsets.UTF_8);
        byte[] txt_bytes = txt.getBytes(StandardCharsets.UTF_8);

        int ctr = 0;

        byte[] tmp = new byte[txt_bytes.length];

        for (int i = 0;i < txt_bytes.length;i++) {
            ctr = ctr == mix_bytes.length ? 0 : ctr;
            tmp[i] = (byte) (txt_bytes[i] ^ mix_bytes[ctr++]);
        }

        String txt_new = mix + new String(tmp);

        byte[] tmp_new = str_key(txt_new, key);

        return  (new String(Base64.encodeBase64(tmp_new))).replace("+", "-").replace("/", "_").replace("=", "");
    }

    public String str_decrypt(String txt, String key) {
        try {
            txt = new String(Base64.decodeBase64(txt.replace("-", "+").replace("_", "/").getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "";
        }

        String txt_new = new String(this.str_key(txt, key));

        String mix = txt_new.substring(0, 4);

        byte[] mix_bytes = mix.getBytes(StandardCharsets.UTF_8);
        byte[] txt_bytes;

        try {
            txt_bytes = txt_new.substring(4).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }

        int ctr = 0;

        byte[] tmp = new byte[txt_bytes.length];

        for (int i = 0;i < txt_bytes.length;i++) {
            ctr = ctr == mix_bytes.length ? 0 : ctr;
            tmp[i] = (byte) (txt_bytes[i] ^ mix_bytes[ctr++]);
        }

        return new String(tmp);
    }

    public String month_end(Object... obj) {
        if (obj.length == 0) return "";

        int year = 0;
        int month = 1;
        int at_day;
        int up_day = 0;

        if (obj.length == 1) {
            if (obj[0] instanceof String) {
                String[] fmt = ((String) obj[0]).split("-");

                if (fmt.length < 2) return Integer.parseInt(fmt[0]) + "-12-31";

                year = Integer.parseInt(fmt[0]);
                month = Integer.parseInt(fmt[1]);

                if (fmt.length > 2) up_day = Integer.parseInt(fmt[2]);
            } else {
                return obj[0].toString() + "-12-31";
            }
        }

        if (obj.length > 1) {
            if (obj[0] instanceof Integer) {
                year = (int) obj[0];
            } else if (obj[0] instanceof String) {
                year = Integer.parseInt((String) obj[0]);
            } else {
                return "";
            }

            if (obj[1] instanceof Integer) {
                month = (int) obj[1];
            } else if (obj[1] instanceof String) {
                month = Integer.parseInt((String) obj[1]);
            } else {
                return "";
            }
        }

        if (obj.length > 2) {
            if (obj[2] instanceof Integer) {
                up_day = (int) obj[2];
            } else if (obj[2] instanceof String) {
                up_day = Integer.parseInt((String) obj[2]);
            }
        }

        if (month < 1) month = 1;
        if (month > 12) month = 12;

        if (month == 2) {
            if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
                at_day = 29;
            } else {
                at_day = 28;
            }
        } else if (month <= 7) {
            if (month % 2 == 1) {
                at_day = 31;
            } else {
                at_day = 30;
            }
        } else {
            if (month % 2 == 1) {
                at_day = 30;
            } else {
                at_day = 31;
            }
        }

        if (up_day > 0 && up_day < at_day) at_day = up_day;

        return year + "-" + (month < 10 ? "0" : "") + month + "-" + (at_day < 10 ? "0" : "") + at_day;
    }

    public String last_month(Object... months) {
        String month = months.length > 0 ? months[0].toString() : this.date("yyyy-MM");

        String[] arr = month.split("-");

        if (arr.length < 2) return "";

        int y = Integer.parseInt(arr[0]);
        int m = Integer.parseInt(arr[1]);

        int new_y = y;

        int new_m = m - (months.length > 1 ? Integer.parseInt(months[1].toString()) : 1);

        if (new_m < 1) {
            new_y = y + (int) Math.floor((double) new_m / 12.0) - 1;

            new_m = 12 + (new_m % 12);
        }

        if (new_m > 12) {
            new_y = y + (int) Math.floor((double) new_m / 12.0);

            new_m = new_m % 12;
        }

        return new_y + "-" + String.format("%02d", new_m);
    }

    // 记录用户上传文件
    public void add_upload_record(int tag, String url, String file_name, String file_path, String user_id) {
        String file_id = db.table("file_upload").field("id", "path", file_path);

        if (file_id.equals("")) {
            Map<String, String> data = new LinkedHashMap<>();

            data.put("tag", tag + "");
            data.put("url", url);
            data.put("name", file_name);
            data.put("path", file_path);
            data.put("creator", user_id);
            data.put("create_time", this.time().toString());

            db.table("file_upload").create(data);
        }
    }

    // 删除用户上传文件
    public void del_upload_record(String file_id) {
        Map<String, String> data = db.table("file_upload").find("*", file_id);

        if (data.size() == 0) return;

        disk.del_file(this.path(data.get("path")));

        db.table("file_upload").delete(file_id);
    }

    public void del_upload_file(String path, int... is_abs) {
        int is_abs_0 = is_abs.length > 0 ? is_abs[0] : 0;

        disk.del_file(is_abs_0 == 0 ? this.path(path) : path);

        String file_id = db.table("file_upload").field("id", "path", path);

        if (!file_id.equals("")) {
            db.table("file_upload").delete(file_id);
        }
    }

    // 分布式部署请求分发
    // 若指定host（主机号，即配置文件中host_id）则只转发当前请求到指定主机，不分发
    // 分发模式下，将请求转发给不包含自身在内的集群内所有主机，返回true
    // 转发模式下，若指定主机为自身，则不转发，返回false，否则返回true
    public Api.Res request_hand_out(Object... host) {
        if (host_id.equals("0")) return new Api.Res("out", "0", "msg", "非分布式部署，请求不需要转发或分发");
        if (this.http_get("no_hand").equals("1")) return new Api.Res("out", "0", "msg", "请求包含禁止转发或分发标记");

        String target_host = "0";

        String uri = "";
        String get = "";
        String arg = "";

        if (host.length > 0) {
            if (check.is_natural_no_zero(host[0].toString())) {
                target_host = host[0].toString();

                if (host.length > 1) uri = host[1].toString();
                if (host.length > 2) get = host[2].toString();
                if (host.length > 3) arg = JSONObject.toJSONString(host[3]);
            } else {
                uri = host[0].toString();

                if (host.length > 1) get = host[1].toString();
                if (host.length > 2) arg = JSONObject.toJSONString(host[2]);
            }
        }

        List<Map<String, String>> host_list;

        if (!target_host.equals("0")) {
            if (host_id.equals(target_host)) return new Api.Res("out", "0", "msg", "目标主机为本机，不需要转发或分发请求");

            host_list = db.table("sys_host").read("host_id,ip,port", "host_id", target_host);
        } else {
            host_list = db.table("sys_host").read("host_id,ip,port", "host_id!=", host_id);
        }

        HttpServletRequest request = http_request.get();

        String token = request.getHeader("token");

        Map<String, String> header = new LinkedHashMap<>();

        header.put("Content-Type", "application/json");
        header.put("Token", token);

        String queryString = request.getQueryString();

        if (queryString != null) {
            try {
                queryString = URLDecoder.decode(queryString, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (uri.equals("")) {
            uri = request.getRequestURI();
            get = (queryString == null ? "" : queryString);

            if (api.arg().size() > 0) {
                arg = JSONObject.toJSONString(api.arg());
            }
        }

        int failed_count = 0;

        List<Map<String, String>> failed_host_list = new ArrayList<>();

        for (Map<String, String> host_item : host_list) {
            Curl curl = new Curl("http://" + host_item.get("ip") + ":" + host_item.get("port") + uri + "?no_hand=1" + (get.equals("") ? "" : "&" + get));

            curl.headers(header);

            if (!arg.equals("")) {
                curl.opt("-d", arg);
            }

            Map<String, Object> json = curl.exec((httpCode, responseBody) -> {
                String json_str = new String(responseBody, StandardCharsets.UTF_8);

                return JSONObject.parseObject(json_str);
            }, null);

            if (!json.getOrDefault("out", "0").toString().equals("1")) {
                failed_count++;

                Map<String, String> failed_host = new LinkedHashMap<String, String>(){{
                    put("host_id", host_item.get("host_id"));
                    put("host_err", json.get("msg").toString());
                }};

                failed_host_list.add(failed_host);
            }
        }

        return new Api.Res("out", failed_count == 0 ? "1" : "1",
                "msg", failed_count == 0 ? "" : "请求分发后，有" + failed_count + "台主机返回执行错误",
                "data", failed_count == 0 ? "" : failed_host_list);
    }

    public String num_hans(String str) {
        str = str.replace(",", "").replace(" ", "").trim();

        if (!check.numeric(str)) return str.toUpperCase();

        boolean isPositive = true;

        //判断是不是负数
        if(str.charAt(0)=='-') isPositive = false;

        StringBuilder sb = new StringBuilder();

        if(!isPositive) sb.append("负");

        //整数部分和小数部分（如果有）隔开
        String[] input = str.split("\\.");

        String IntegerPart = isPositive ? input[0] : input[0].substring(1);

        String[] s1 = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
        String[] s2 = {"十", "百", "千", "万", "十", "百", "千", "亿", "十", "百", "千", "兆", "十", "百", "千"};

        int n = IntegerPart.length();

        for (int i = 0; i < n;i++) {
            int num = IntegerPart.charAt(i) - '0';

            if (i != n - 1 && num != 0) {
                sb.append(s1[num]).append(s2[n - 2 - i]);
            } else {
                // 如果有连续的两个零，只加一个零;如果num不是0，也加上去
                if ((sb.length() > 1 && sb.charAt(sb.length() - 1) != '零') || num != 0) {
                    sb.append(s1[num]);
                }
            }
            // 处理特殊情况，如果在每个亿、万的部分末尾有零
            // 例如14001400,不应该是一千四百零一千四百零，而是一千四百万一千四百
            if (sb.charAt(sb.length() - 1) == '零' && (n - 2 - i == -1 || s2[n - 2 - i].equals("万") || s2[n - 2 - i].equals("亿"))){
                sb.deleteCharAt(sb.length()-1);

                //如果不是遍历到字符串最后，就加上对应的万或者亿
                if (n-2-i != -1) {
                    sb.append(s2[n - 2 - i]);
                }
            }
        }
        //如果还有小数部分
        if (input.length>1) {
            sb.append("点");

            for (int i = 0;i < input[1].length();i++) {
                int num = input[1].charAt(i) - '0';

                sb.append(s1[num]);
            }
        }
        return sb.toString();
    }
}
