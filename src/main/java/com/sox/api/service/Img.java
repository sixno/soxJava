package com.sox.api.service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Img {
    public void crop(String file, int dst_width, int dst_height) {
        BufferedImage bufImage;

        try {
            bufImage = ImageIO.read(new File(file));
        } catch (IOException e) {
            e.printStackTrace();

            return;
        }

        boolean save = false;

        int src_width  = bufImage.getWidth();
        int src_height = bufImage.getHeight();

        float src_ratio = (float)src_width / src_height;
        float dst_ratio = (float)dst_width / dst_height;

        int new_width  = 0;
        int new_height = 0;

        if (src_ratio > dst_ratio && src_height != dst_height) {
            save = true;

            new_width  = Math.round(dst_height * src_ratio);
            new_height = dst_height;

            bufImage = toBufferedImage(bufImage.getScaledInstance(new_width, new_height, BufferedImage.SCALE_SMOOTH));

            bufImage = bufImage.getSubimage((int) Math.floor(Math.abs(dst_width - new_width) / 2.0), 0, dst_width, dst_height);
        }

        if (src_ratio < dst_ratio && src_width != dst_width) {
            save = true;

            new_width  = dst_width;
            new_height = Math.round(dst_width / src_ratio);

            bufImage = toBufferedImage(bufImage.getScaledInstance(new_width, new_height, BufferedImage.SCALE_SMOOTH));

            bufImage = bufImage.getSubimage(0, (int) Math.floor(Math.abs(dst_height - new_height) / 2.0), dst_width, dst_height);
        }

        if (src_ratio == dst_ratio && src_width != dst_width) {
            save = true;

            bufImage = toBufferedImage(bufImage.getScaledInstance(dst_width, dst_height, BufferedImage.SCALE_SMOOTH));
        }

        if (save) {
            (new File(file)).delete();

            try {
                ImageIO.write(bufImage, file.substring(file.lastIndexOf(".") + 1).toUpperCase(), new File(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void crop(String file, int x, int y, int width, int height) {
        BufferedImage bufImage;

        try {
            bufImage = ImageIO.read(new File(file));
        } catch (IOException e) {
            e.printStackTrace();

            return;
        }

        (new File(file)).delete();

        bufImage = bufImage.getSubimage(x, y, width, height);

        try {
            ImageIO.write(bufImage, file.substring(file.lastIndexOf(".") + 1).toUpperCase(), new File(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private static BufferedImage toBufferedImage(Image img) {
        int width  = img.getWidth(null);
        int height = img.getHeight(null);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();

        graphics.drawImage(img, 0, 0,null);

        graphics.dispose();

        return image;
    }
}
