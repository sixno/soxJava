package com.sox.api.service;

import com.sox.api.model.UserModel;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Cipher;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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
    @Autowired
    Api api;

    @Autowired
    UserModel user_m;

    @Value("#{${sox.rsa.pri_key}}")
    Map<String, String> pri_keys;

    public boolean check_login() {
        return !user_m.get_session("id").equals("");
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
        StringBuilder ds = new StringBuilder(time.length > 0 ? time[0] : api.get("time"));

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

    public String join(List<String> list, String separator) {
        StringBuilder sb = new StringBuilder();

        for (String item : list) {
            sb.append(item).append(separator);
        }

        return sb.toString().substring(0, sb.toString().length() - 1);
    }

    public Long time() {
        return System.currentTimeMillis() / 1000L;
    }

    public String date(String pattern, Long... time) {
        // pattern example: yyyy-MM-dd HH:mm:ss z
        SimpleDateFormat format = new SimpleDateFormat(pattern);

        Date date = new Date(time.length == 0 ? System.currentTimeMillis() : time[0] * 1000L);

        return format.format(date);
    }

    public String path(String path) {
        return System.getProperty("user.dir") + java.io.File.separator + path.replace("/", java.io.File.separator);
    }

    public String resource(String path, String... delimiters) {
        BufferedReader reader = null;
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
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

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

    public boolean is_numeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");

        Matcher isNum = pattern.matcher(str);

        if (!isNum.matches()) {
            return false;
        }

        return true;
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
        String[] arr = this.rsa_decrypt(str).split("@");

        if (arr.length < 2) return "";

        if (Math.abs(Integer.parseInt(arr[1]) - this.time()) > allow_delay) return "";

        return arr[0];
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
}
