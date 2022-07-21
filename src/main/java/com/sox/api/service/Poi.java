package com.sox.api.service;

import com.sox.api.utils.CallbackUtils_2;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class Poi {
    public String cell_value(Cell cell) {
        if (cell == null) return "";

        String value = "";

        CellType cell_type = cell.getCellType();

        if (cell_type.equals(CellType.FORMULA)) {
            cell_type = cell.getCachedFormulaResultType();
        }

        switch (cell_type) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                DecimalFormat df = new DecimalFormat("0.0000");

                value = df.format(cell.getNumericCellValue());

                if (value.endsWith("0")) value = value.replaceAll("(0)+$", "");

                if (value.endsWith(".")) value = value.replaceAll("(\\.)+$", "");
                break;
            case BOOLEAN:
                value = cell.getBooleanCellValue() ? "1" : "0";
                break;
        }

        return value;
    }

    public String cell_value(String file_path, int sheet_id, int row_id, int col_id, String... def) {
        String def_value = def.length > 0 ? def[0] : "";

        if (!file_path.endsWith(".xls") && !file_path.endsWith(".xlsx")) return def_value;

        try {
            Row row;

            InputStream inputStream = new FileInputStream(file_path);

            if (file_path.endsWith(".xls")) {
                HSSFWorkbook workbook = new HSSFWorkbook(inputStream);

                HSSFSheet sheet = workbook.getSheetAt(sheet_id);

                row = sheet.getRow(row_id);
            } else {
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

                XSSFSheet sheet = workbook.getSheetAt(sheet_id);

                row = sheet.getRow(row_id);
            }

            String cell_value = this.cell_value(row.getCell(col_id));

            inputStream.close();

            return cell_value;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return def_value;
    }

    public int ord(String str) {
        return str.length() > 0 ? (str.getBytes(StandardCharsets.UTF_8)[0] & 0xff) : 0; //& 0xff 针对utf-8编码
    }

    public int col_int(String col) {
        int num = 0;
        int len = col.length();

        double bit = Math.pow(26, len - 1);

        int pos = 0;

        String chr = col;

        while(pos <= len - 1)
        {
            chr = chr.substring(pos, len);
            num += bit * (this.ord(chr.toUpperCase()) - 64);
            bit /= 26;

            pos++;
        }

        return num - 1;
    }

    public String cell_value(String file_path, String sheet_name, String row_name, String col_name, String... def) {
        int sheet_id = Integer.parseInt(sheet_name);

        int row_id = Integer.parseInt(row_name) - 1;
        int col_id = this.col_int(col_name);

        return this.cell_value(file_path, sheet_id, row_id, col_id, def);
    }

    public void read_xls(String file_path, int sheet_id, CallbackUtils_2<Map<Integer, String>, Integer> callback) {
        if (!file_path.endsWith(".xls") && !file_path.endsWith(".xlsx")) return;

        try {
            Map<Integer, String> line = new LinkedHashMap<>();

            // 1、获取文件输入流
            InputStream inputStream = new FileInputStream(file_path);

            if (file_path.endsWith(".xls")) {
                // 2、获取Excel工作簿对象
                HSSFWorkbook workbook = new HSSFWorkbook(inputStream);

                // 3、得到Excel工作表对象
                HSSFSheet sheet = workbook.getSheetAt(sheet_id);

                line.put(0, sheet.getSheetName());

                callback.deal(line, -1);

                // 4、循环读取表格数据
                for (Row row : sheet) {
                    line = new LinkedHashMap<>();

                    for (int i = 0;i < row.getLastCellNum();i++) {
                        Cell cell = row.getCell(i);

                        if (cell == null) continue;

                        line.put(cell.getColumnIndex(), this.cell_value(cell));
                    }

                    callback.deal(line, row.getRowNum());
                }

                // 5、关闭流
                workbook.close();
            } else {
                // 2、获取Excel工作簿对象
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

                // 3、得到Excel工作表对象
                XSSFSheet sheet = workbook.getSheetAt(sheet_id);

                line.put(0, sheet.getSheetName());

                callback.deal(line, -1);

                // 4、循环读取表格数据
                for (Row row : sheet) {
                    line = new LinkedHashMap<>();

                    for (int i = 0;i < row.getLastCellNum();i++) {
                        Cell cell = row.getCell(i);

                        if (cell == null) continue;

                        line.put(cell.getColumnIndex(), this.cell_value(cell));
                    }

                    callback.deal(line, row.getRowNum());
                }

                // 5、关闭流
                workbook.close();
            }

            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void make_paragraph(XWPFParagraph paragraph, Map<String, String> params) {
        if (paragraph.getText().contains("{%")) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();

                if (paragraph.getText().contains(key)) {
                    List<XWPFRun> runs = paragraph.getRuns();

                    int i_start = -1;

                    String i_key = "";

                    for (int i =0;i < runs.size();i++) {
                        XWPFRun run = runs.get(i);

                        String text = run.getText(0);

                        if (text != null && key.startsWith(text)) {
                            if (key.startsWith(text)) {
                                i_start = i;
                                i_key = "";
                            }
                        }

                        if (text != null && i_start > -1) {
                            i_key += text;
                        }

                        if (i_key.length() == key.length()) {
                            if (i_key.equals(key)) {
                                runs.get(i_start).setText(entry.getValue(), 0);

                                for (int j = i_start + 1;j <= i;j++) {
                                    runs.get(j).setText("", 0);
                                }
                            }

                            i_start = -1;
                            i_key = "";
                        }
                    }
                }
            }

            List<XWPFRun> runs = paragraph.getRuns();

            for (XWPFRun run : runs) {
                String text = run.getText(0);

                if (text != null) {
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        String key = entry.getKey();
                        if(text.contains(key)){
                            String value = entry.getValue();
                            if (value != null) {
                                text = text.replace(key, value);
                                run.setText(text,0);
                            }
                        }
                    }
                }
            }
        }
    }

    public int generate_doc(String tpl_file, Map<String, String> params, String doc_file) {
        if (!tpl_file.endsWith(".doc") && !tpl_file.endsWith(".docx")) return 0;
        if (!doc_file.endsWith(".doc") && !doc_file.endsWith(".docx")) return 0;

        if (tpl_file.endsWith(".doc") && doc_file.endsWith(".docx")) return 0;
        if (tpl_file.endsWith(".docx") && doc_file.endsWith(".doc")) return 0;

        if (tpl_file.endsWith(".doc")) {
            try {
                // 读取模板
                FileInputStream is = new FileInputStream(new File(tpl_file));
                HWPFDocument document = new HWPFDocument(is);
                // 读取文本内容
                Range bodyRange = document.getRange();
                // 替换内容
                for (Map.Entry<String, String> param : params.entrySet()) {
                    bodyRange.replaceText(param.getKey(), param.getValue());
                }

                FileOutputStream fos = new FileOutputStream(new File(doc_file));

                document.write(fos);

                fos.close();

                return 1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                // 获取docx解析对象
                XWPFDocument document = new XWPFDocument(POIXMLDocument.openPackage(tpl_file));

                // 当前循环方式效率较低

                // 获取段落集合
                List<XWPFParagraph> paragraphs = document.getParagraphs();

                for (XWPFParagraph paragraph : paragraphs) {
                    this.make_paragraph(paragraph, params);
                }

                // 获取表格对象集合
                List<XWPFTable> tables = document.getTables();

                for (XWPFTable table : tables) {
                    List<XWPFTableRow> rows = table.getRows();

                    for (XWPFTableRow row : rows) {
                        List<XWPFTableCell> cells = row.getTableCells();

                        for (XWPFTableCell cell : cells) {
                            if (cell.getText().contains("{%")) {
                                List<XWPFParagraph> cell_paragraphs = cell.getParagraphs();

                                for (XWPFParagraph paragraph : cell_paragraphs) {
                                    this.make_paragraph(paragraph, params);
                                }
                            }
                        }
                    }
                }

                FileOutputStream fos = new FileOutputStream(new File(doc_file));

                document.write(fos);

                fos.close();

                return 1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return -1;
    }
}
