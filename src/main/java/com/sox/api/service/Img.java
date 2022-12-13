package com.sox.api.service;

import com.idrsolutions.image.png.PngCompressor;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@Service
public class Img {
    public void resize(String file, int dst_width, int dst_height, float... quality) {
        // dst_width 为 0 时，以 dst_height 为硬边
        // dst_height 为 0 时，以 dst_width 为硬边
        // dst_width 和 dst_height 都不为 0 时，为长宽限定
        float quality_0 = quality.length > 0 ? quality[0] : 0.9f;

        BufferedImage bufImage;

        try {
            bufImage = ImageIO.read(new File(file));
        } catch (IOException e) {
            e.printStackTrace();

            return;
        }

        boolean transparent = file.substring(file.lastIndexOf(".") + 1).toLowerCase().equals("png");

        int src_width  = bufImage.getWidth();
        int src_height = bufImage.getHeight();

        float src_ratio = (float)src_width / src_height;

        if (dst_width != 0 && dst_height != 0) {
            float dst_ratio = (float)dst_width / dst_height;

            if (src_ratio > dst_ratio) {
                dst_height = 0;
            } else {
                dst_width = 0;
            }
        }

        int new_width;
        int new_height;

        if (dst_width == 0) {
            if (dst_height == src_height) return;

            new_width  = Math.round(dst_height * src_ratio);
            new_height = dst_height;

            bufImage = this.toBufferedImage(bufImage.getScaledInstance(new_width, new_height, BufferedImage.SCALE_SMOOTH), transparent);
        }

        if (dst_height == 0) {
            if (dst_width == src_width) return;

            new_width  = dst_width;
            new_height = Math.round(dst_width / src_ratio);

            bufImage = this.toBufferedImage(bufImage.getScaledInstance(new_width, new_height, BufferedImage.SCALE_SMOOTH), transparent);
        }

        boolean del_ok = new File(file).delete();

        if(del_ok) this.save(file, bufImage, file.substring(file.lastIndexOf(".") + 1), quality_0);
    }

    public void crop(String file, int dst_width, int dst_height, float... quality) {
        float quality_0 = quality.length > 0 ? quality[0] : 0.9f;

        BufferedImage bufImage;

        try {
            bufImage = ImageIO.read(new File(file));
        } catch (IOException e) {
            e.printStackTrace();

            return;
        }

        boolean transparent = file.substring(file.lastIndexOf(".") + 1).toLowerCase().equals("png");

        int src_width  = bufImage.getWidth();
        int src_height = bufImage.getHeight();

        float src_ratio = (float)src_width / src_height;
        float dst_ratio = (float)dst_width / dst_height;

        int new_width;
        int new_height;

        if (src_ratio > dst_ratio && src_height != dst_height) {
            new_width  = Math.round(dst_height * src_ratio);
            new_height = dst_height;

            bufImage = this.toBufferedImage(bufImage.getScaledInstance(new_width, new_height, BufferedImage.SCALE_SMOOTH), transparent);

            bufImage = bufImage.getSubimage((int) Math.floor(Math.abs(dst_width - new_width) / 2.0), 0, dst_width, dst_height);
        }

        if (src_ratio < dst_ratio && src_width != dst_width) {
            new_width  = dst_width;
            new_height = Math.round(dst_width / src_ratio);

            bufImage = this.toBufferedImage(bufImage.getScaledInstance(new_width, new_height, BufferedImage.SCALE_SMOOTH), transparent);

            bufImage = bufImage.getSubimage(0, (int) Math.floor(Math.abs(dst_height - new_height) / 2.0), dst_width, dst_height);
        }

        if (src_ratio == dst_ratio && src_width != dst_width) {
            bufImage = this.toBufferedImage(bufImage.getScaledInstance(dst_width, dst_height, BufferedImage.SCALE_SMOOTH), transparent);
        }

        boolean del_ok = new File(file).delete();

        if(del_ok) this.save(file, bufImage, file.substring(file.lastIndexOf(".") + 1), quality_0);
    }

    public void crop(String file, int x, int y, int width, int height, float... quality) {
        float quality_0 = quality.length > 0 ? quality[0] : 0.9f;

        BufferedImage bufImage;

        try {
            bufImage = ImageIO.read(new File(file));
        } catch (IOException e) {
            e.printStackTrace();

            return;
        }

        boolean transparent = file.substring(file.lastIndexOf(".") + 1).toLowerCase().equals("png");

        bufImage = this.toBufferedImage(bufImage, transparent);

        bufImage = bufImage.getSubimage(x, y, width, height);

        boolean del_ok = new File(file).delete();

        if(del_ok) this.save(file, bufImage, file.substring(file.lastIndexOf(".") + 1), quality_0);
    }

    public String text_wm(String file, String text, int x, int y, int... color) {
        return "";
    }

    public String img_wm(String file, String wm_file, int wm_size, String position, int wm_opacity) {
        // position: top right bottom left center left-top right-top left-bottom right-bottom
        // wm_size: %
        // wm_opacity: %

        return "";
    }

    private BufferedImage toBufferedImage(Image img, boolean transparent) {
        int width  = img.getWidth(null);
        int height = img.getHeight(null);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();

        if(transparent) {
            image = graphics.getDeviceConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);

            graphics = image.createGraphics();
        }

        graphics.drawImage(img, 0, 0,null);

        graphics.dispose();

        return image;
    }

    // 保存图片 支持 jpg 格式指定压缩质量（默认0.9），png 格式直接压缩，其他格式直接保存
    public void save(String filePath, BufferedImage bufferedImage, String ext, float... quality) {
        File file = new File(filePath);

        if (!ext.toLowerCase().equals("jpg") && !ext.toLowerCase().equals("jpeg")) {
            try {
                if (!file.isFile()) ImageIO.write(bufferedImage, filePath.substring(filePath.lastIndexOf(".") + 1).toUpperCase(), file);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!ext.toLowerCase().equals("png")) return;
        }

        if (!ext.toLowerCase().equals("png")) {
            boolean del_ok = !file.isFile() || file.delete();

            if (!del_ok) return;

            float quality_0 = quality.length > 0 ? quality[0] : 0.9f;

            Iterator<ImageWriter> imageWriterIter = ImageIO.getImageWritersByFormatName(ext);

            if (imageWriterIter.hasNext()) {
                ImageWriter writer = imageWriterIter.next();
                ImageWriteParam param = writer.getDefaultWriteParam();

                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

                param.setCompressionQuality(quality_0);

                try {
                    FileImageOutputStream out = new FileImageOutputStream(file);

                    writer.setOutput(out);

                    writer.write(null, new IIOImage(bufferedImage, null, null), param);

                    out.close();

                    writer.dispose();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } else {
            try {
                PngCompressor.compress(file, file); // 第一个参数为源文件路径，第二个参数为目标文件路径，相同则覆盖
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
