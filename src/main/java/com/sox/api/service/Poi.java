package com.sox.api.service;

import com.sox.api.utils.CallbackUtils;
import com.sox.api.utils.CastUtils;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class Poi {
    @Autowired
    public Check check;

    public static class XSSFLine {
        public int row_no;
        public int max_no;
        public Map<Integer, String> data;
        public XSSFWorkbook workbook;

        public XSSFLine(Object... obj) {
            if (obj.length > 0) this.row_no = CastUtils.cast(obj[0]);
            if (obj.length > 1) this.max_no = CastUtils.cast(obj[1]);
            if (obj.length > 2) this.data = CastUtils.cast(obj[2]);
            if (obj.length > 3) this.workbook = CastUtils.cast(obj[3]);
        }
    }

    public String cell_value(Cell cell, String... def) {
        String def_0 = def.length > 0 ? def[0] : "";

        if (cell == null) return def_0;

        String value = def_0;

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

    public String cell_value(XSSFSheet sheet, int row_id, int col_id, String... def) {
        return this.cell_value(sheet.getRow(row_id).getCell(col_id), def);
    }

    public String cell_value(XSSFWorkbook workbook, int sheet_id, int row_id, int col_id, String... def) {
        return this.cell_value(workbook.getSheetAt(sheet_id), row_id, col_id, def);
    }

    public int col_int(String col) {
        col = col.toUpperCase();

        int len = col.length();

        int num = 0;

        for (int i = 0;i < len;i++) {
            int tmp = (int) col.charAt(len - i - 1) - (int) 'A' + 1;

            tmp *= Math.pow(26, i);

            num += tmp;
        }

        return num - 1;
    }

    public int row_int(String row) {
        return Integer.parseInt(row) - 1;
    }

    public String col_str(int col_int) {
        String col = "";

        do {
            if (col.length() > 0) col_int--;

            col = ((char) (col_int % 26 + (int) 'A')) + col;

            col_int = (col_int - col_int % 26) / 26;
        } while (col_int > 0);

        return col;
    }

    public String row_str(int row_int) {
        return (row_int + 1) + "";
    }

    public Map<String, Integer> grid(String str) {
        Map<String, Integer> grid = new LinkedHashMap<String, Integer>(){{
            put("col", 0);
            put("row", 0);
        }};

        StringBuilder col = new StringBuilder();
        StringBuilder row = new StringBuilder();

        for (String s : str.split("")) {
            if (check.numeric(s)) {
                row.append(s);
            } else {
                col.append(s);
            }
        }

        if (!col.toString().equals("")) {
            grid.put("col", this.col_int(col.toString()));
        }

        if (!row.toString().equals("")) {
            grid.put("row", this.row_int(row.toString()));
        }

        return grid;
    }

    public void open_xlsx(String file_path, CallbackUtils<XSSFWorkbook> callback) {
        if (!file_path.endsWith(".xlsx")) return;

        try {
            InputStream inputStream = new FileInputStream(file_path);

            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

            callback.deal(workbook);

            workbook.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void read_xlsx(String file_path, int sheet_id, CallbackUtils<XSSFLine> callback) {
        this.open_xlsx(file_path, (XSSFWorkbook workbook) -> {
            Map<Integer, String> data = new LinkedHashMap<>();

            XSSFSheet sheet = workbook.getSheetAt(sheet_id);

            int max_no = sheet.getLastRowNum();

            data.put(0, sheet.getSheetName());

            callback.deal(new XSSFLine(-1, max_no, data, workbook));

            for (Row row : sheet) {
                data = new LinkedHashMap<>();

                for (int i = 0;i < row.getLastCellNum();i++) {
                    Cell cell = row.getCell(i);

                    if (cell == null) continue;

                    data.put(cell.getColumnIndex(), this.cell_value(cell));
                }

                callback.deal(new XSSFLine(row.getRowNum(), max_no, data, workbook));
            }
        });
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

    public int generate_docx(String tpl_file, Map<String, String> params, String dst_file) {
        if (!tpl_file.endsWith(".docx") || !dst_file.endsWith(".docx")) return 0;

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

            FileOutputStream fos = new FileOutputStream(new File(dst_file));

            document.write(fos);

            fos.close();

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }
}
