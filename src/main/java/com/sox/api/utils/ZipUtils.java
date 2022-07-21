package com.sox.api.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

/**
 * 压缩工具类
 */
public class ZipUtils {
    private static final String CHINESE_CHARSET = "GBK"; // 使用GBK编码可以避免压缩中文文件名乱码

    private static final int CACHE_SIZE = 1024; // 文件读取缓冲区大小

    private static void zip(File parentFile, String basePath, ZipOutputStream zos) {
        try {
            File[] files;

            if (parentFile.isDirectory()) {
                files = parentFile.listFiles();

                if (files == null) return;
            } else {
                files = new File[1];

                files[0] = parentFile;
            }

            String pathName;
            InputStream fis;
            BufferedInputStream bis;

            byte[] cache = new byte[CACHE_SIZE];

            for (File file : files) {
                if (file.isDirectory()) {
                    pathName = file.getPath().substring(basePath.length() + 1) + "/";

                    zos.putNextEntry(new ZipEntry(pathName));

                    zip(file, basePath, zos);
                } else {
                    pathName = file.getPath().substring(basePath.length() + 1);

                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);

                    zos.putNextEntry(new ZipEntry(pathName));

                    int nRead;

                    while ((nRead = bis.read(cache, 0, CACHE_SIZE)) != -1) {
                        zos.write(cache, 0, nRead);
                    }

                    bis.close();
                    fis.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void zip(String sourceFolder, String zipFilePath) {
        try {
            OutputStream out = new FileOutputStream(zipFilePath);
            BufferedOutputStream bos = new BufferedOutputStream(out);
            ZipOutputStream zos = new ZipOutputStream(bos);

            // 解决中文文件名乱码
            zos.setEncoding(CHINESE_CHARSET);
            File file = new File(sourceFolder);

            String basePath;

            if (file.isDirectory()) {
                basePath = file.getPath();
            } else {
                basePath = file.getParent();
            }

            zip(file, basePath, zos);

            zos.closeEntry();

            zos.close();
            bos.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unzip(String zipFilePath, String destDir) {
        try {
            ZipFile zipFile = new ZipFile(zipFilePath, CHINESE_CHARSET);
            Enumeration<?> emu = zipFile.getEntries();
            BufferedInputStream bis;
            FileOutputStream fos;
            BufferedOutputStream bos;
            File file, parentFile;
            ZipEntry entry;

            byte[] cache = new byte[CACHE_SIZE];

            while (emu.hasMoreElements()) {
                entry = (ZipEntry) emu.nextElement();
                if (entry.isDirectory()) {
                    new File(destDir + entry.getName()).mkdirs();
                    continue;
                }

                bis = new BufferedInputStream(zipFile.getInputStream(entry));
                file = new File(destDir + entry.getName());
                parentFile = file.getParentFile();
                if (parentFile != null && (!parentFile.exists())) {
                    parentFile.mkdirs();
                }
                fos = new FileOutputStream(file);
                bos = new BufferedOutputStream(fos, CACHE_SIZE);
                int nRead = 0;
                while ((nRead = bis.read(cache, 0, CACHE_SIZE)) != -1) {
                    fos.write(cache, 0, nRead);
                }

                bos.flush();
                bos.close();
                fos.close();
                bis.close();
            }

            zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
