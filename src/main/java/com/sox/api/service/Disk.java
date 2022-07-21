package com.sox.api.service;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Service
public class Disk {
    @Autowired
    private Log log;

    public String get_contents(String path) {
        if (!path.startsWith("http://") && !path.startsWith("https://")) {
            File file = new File(path);

            try {
                return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                URL url = new URL(path);
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();

                conn.setConnectTimeout(3000);

                conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

                InputStream inputStream = conn.getInputStream();

                byte[] buffer = new byte[1024];

                int len = 0;

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                while((len = inputStream.read(buffer)) != -1) {
                    bytes.write(buffer, 0, len);
                }

                bytes.close();

                return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "";
    }

    public byte[] get_byte_contents(String path) {
        if (!path.startsWith("http://") && !path.startsWith("https://")) {
            File file = new File(path);

            try {
                return FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                URL url = new URL(path);
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();

                conn.setConnectTimeout(3000);

                conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

                InputStream inputStream = conn.getInputStream();

                byte[] buffer = new byte[1024];

                int len = 0;

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                while((len = inputStream.read(buffer)) != -1) {
                    bytes.write(buffer, 0, len);
                }

                bytes.close();

                return bytes.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new byte[0];
    }

    public void put_contents(String path, String contents, boolean... append) {
        try {
            if(append.length > 0 && append[0]) {
                FileUtils.writeStringToFile(new File(path), contents, StandardCharsets.UTF_8, true);
            } else {
                FileUtils.writeStringToFile(new File(path), contents, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put_byte_contents(String path, byte[] contents, boolean... append) {
        try {
            if(append.length > 0 && append[0]) {
                FileUtils.writeByteArrayToFile(new File(path), contents, true);
            } else {
                FileUtils.writeByteArrayToFile(new File(path), contents);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean del(String path) {
        File file = new File(path);
        if (!file.exists()) {
            log.msg("删除失败:" + path + "不存在！", 4);
            return false;
        } else {
            if (file.isFile())
                return del_file(path);
            else
                return del_dir(path);
        }
    }

    public boolean del_file(String path) {
        File file = new File(path);

        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                log.msg("删除文件" + path + "成功！", 4);
                return true;
            } else {
                log.msg("删除文件" + path + "失败！", 4);
                return false;
            }
        } else {
            log.msg("删除文件失败：" + path + "不存在！", 4);
            return false;
        }
    }

    public boolean del_dir(String path) {
        if (!path.endsWith(File.separator)) path = path + File.separator;

        File dirFile = new File(path);

        if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
            log.msg("删除目录失败：" + path + "不存在！", 4);
            return false;
        }

        boolean flag = true;

        // 删除文件夹中的所有文件包括子目录
        File[] files = dirFile.listFiles();
        for (int i = 0; i < Objects.requireNonNull(files).length; i++) {
            // 删除子文件
            if (files[i].isFile()) {
                flag = this.del_file(files[i].getAbsolutePath());

                if (!flag) break;
            }

            // 删除子目录
            else if (files[i].isDirectory()) {
                flag = this.del_dir(files[i].getAbsolutePath());

                if (!flag) break;
            }
        }
        if (!flag) {
            log.msg("删除目录失败！", 4);
            return false;
        }

        // 删除当前目录
        if (dirFile.delete()) {
            log.msg("删除目录" + path + "成功！", 4);
            return true;
        } else {
            return false;
        }
    }

    public void copy_file(String src, String dist) {
        try {
            // 复制文件
            // FileInputStream fis = new FileInputStream(src);
            // FileOutputStream fos = new FileOutputStream(dist);

            // byte[] data = new byte[1024 * 8];

            // int len = fis.read(data);

            // while (len != -1) {
            //    fos.write(data, 0, len);

            //     len = fis.read(data);
            // }

            // fis.close();
            // fos.close();

            FileUtils.copyFile(new File(src), new File(dist));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
