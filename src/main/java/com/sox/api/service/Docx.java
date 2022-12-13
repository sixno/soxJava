package com.sox.api.service;

import com.deepoove.poi.data.*;
import org.springframework.stereotype.Service;

/**
 * 报告生成服务
 * 目前使用 poi-tl 作为基底
 */
@Service
public class Docx {
    public CellRenderData cell_render(String text, String... style) {
        if (style.length == 0) return Cells.of(text).create();

        Texts.TextBuilder text_builder = Texts.of(text);

        if ((":" + style[0] + ":").contains(":*b:")) {
            text_builder.bold();
        }

        if (style[0].contains(":#")) {
            int pos = style[0].indexOf(":#");

            text_builder.color(style[0].substring(pos + 2, pos + 8));
        }

        Cells.CellBuilder cell_builder = Cells.of(text_builder.create());

        cell_builder.verticalCenter();

        if ((":" + style[0] + ":").contains(":*l:")) {
            cell_builder.horizontalLeft();
        }

        if ((":" + style[0] + ":").contains(":*r:")) {
            cell_builder.horizontalRight();
        }

        if ((":" + style[0] + ":").contains(":*c:")) {
            cell_builder.horizontalCenter();
        }

        if ((":" + style[0] + ":").contains(":*cc:")) {
            cell_builder.center();
        }

        if ((":" + style[0] + ":").contains(":*vc:")) {
            cell_builder.verticalCenter();
        }

        return cell_builder.create();
    }
}
