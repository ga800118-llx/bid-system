package com.bid.system.service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelImportService {

    public byte[] template(String sheetName, List<String> headers, List<List<String>> sampleRows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet(sheetName == null || sheetName.isBlank() ? "模板" : sheetName.trim());
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int rowIndex = 0; rowIndex < sampleRows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<String> values = sampleRows.get(rowIndex);
                for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    cell.setCellValue(colIndex < values.size() ? nullSafe(values.get(colIndex)) : "");
                    cell.setCellStyle(bodyStyle);
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(width + 1024, 14000));
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("生成导入模板失败");
        }
    }

    public List<List<String>> readRows(MultipartFile file, int expectedColumns) {
        if (file == null || file.isEmpty()) throw new RuntimeException("请选择导入文件");
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim().toLowerCase();
        if (!fileName.endsWith(".xlsx")) throw new RuntimeException("仅支持导入 .xlsx 文件");
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) return List.of();
            XSSFSheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<List<String>> rows = new ArrayList<>();
            int fromRow = Math.max(sheet.getFirstRowNum() + 1, 1);
            for (int rowIndex = fromRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                List<String> values = new ArrayList<>();
                boolean blank = true;
                for (int colIndex = 0; colIndex < expectedColumns; colIndex++) {
                    Cell cell = row.getCell(colIndex);
                    String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
                    if (!value.isEmpty()) blank = false;
                    values.add(value);
                }
                if (!blank) {
                    rows.add(values);
                }
            }
            return rows;
        } catch (IOException e) {
            throw new RuntimeException("读取导入文件失败");
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);

        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createBodyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
