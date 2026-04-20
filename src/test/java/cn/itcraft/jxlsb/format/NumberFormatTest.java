package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class NumberFormatTest {
    
    @Test
    void writeAllFormats() throws Exception {
        Path output = Paths.get("/tmp/all_formats.xlsb");
        
        System.out.println("=== 所有数字格式测试 ===\n");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(output).build()) {
            writer.writeBatch("Formats", (row, col) -> {
                double value = row * 123.456 - 500;
                switch (col) {
                    case 0: return CellData.text("Row-" + row);
                    case 1: return CellData.number(value);
                    case 2: return CellData.percentage(value / 100);
                    case 3: return CellData.numberWithComma(value);
                    case 4: return CellData.numberNegativeRed(value);
                    case 5: return CellData.currency(value);
                    case 6: return CellData.date(System.currentTimeMillis());
                    case 7: return CellData.time(System.currentTimeMillis());
                    default: return CellData.blank();
                }
            }, 20, 8);
        }
        
        System.out.println("文件: " + output + " (" + Files.size(output) + " bytes)");
        System.out.println("\n格式说明:");
        System.out.println("  Col 1: 普通数字");
        System.out.println("  Col 2: 百分比 (0.00%)");
        System.out.println("  Col 3: 千分位 (#,##0.00)");
        System.out.println("  Col 4: 负红 (#,##0.00;[Red]-#,##0.00)");
        System.out.println("  Col 5: 货币 (￥#,##0.00)");
        System.out.println("  Col 6: 日期 (m/d/yy h:mm)");
        System.out.println("  Col 7: 时间 (h:mm:ss)");
        System.out.println("\n请用WPS打开验证各列格式是否正确！");
    }
    
    @Test
    void percentageFormats() throws Exception {
        Path output = Paths.get("/tmp/percentage.xlsb");
        
        System.out.println("=== 百分比格式测试 ===\n");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(output).build()) {
            writer.writeBatch("Percent", (row, col) -> {
                double value = row * 0.0123;
                switch (col) {
                    case 0: return CellData.text("Value-" + row);
                    case 1: return CellData.percentage(value);
                    case 2: return CellData.percentage(value, 0);
                    case 3: return CellData.percentage(value, 4);
                    default: return CellData.blank();
                }
            }, 10, 4);
        }
        
        System.out.println("文件: " + output + " (" + Files.size(output) + " bytes)");
        System.out.println("\n列说明:");
        System.out.println("  Col 1: 0.00% (2位小数)");
        System.out.println("  Col 2: 0% (无小数)");
        System.out.println("  Col 3: 0.0000% (4位小数)");
    }
    
    @Test
    void timeFormats() throws Exception {
        Path output = Paths.get("/tmp/time_format.xlsb");
        
        System.out.println("=== 时间格式测试 ===\n");
        
        long baseTime = System.currentTimeMillis();
        
        try (XlsbWriter writer = XlsbWriter.builder().path(output).build()) {
            writer.writeBatch("Time", (row, col) -> {
                long time = baseTime + row * 3600000L;
                switch (col) {
                    case 0: return CellData.text("T-" + row);
                    case 1: return CellData.time(time);
                    case 2: return CellData.time(time, "h:mm");
                    case 3: return CellData.time(time, "h:mm:ss AM/PM");
                    default: return CellData.blank();
                }
            }, 10, 4);
        }
        
        System.out.println("文件: " + output + " (" + Files.size(output) + " bytes)");
        System.out.println("\n列说明:");
        System.out.println("  Col 1: h:mm:ss");
        System.out.println("  Col 2: h:mm");
        System.out.println("  Col 3: h:mm:ss AM/PM");
    }
    
    @Test
    void negativeRedFormat() throws Exception {
        Path output = Paths.get("/tmp/negative_red.xlsb");
        
        System.out.println("=== 负红格式测试 ===\n");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(output).build()) {
            writer.writeBatch("NegRed", (row, col) -> {
                double value = row * 100 - 500;
                switch (col) {
                    case 0: return CellData.text("V-" + row);
                    case 1: return CellData.number(value);
                    case 2: return CellData.numberNegativeRed(value);
                    default: return CellData.blank();
                }
            }, 15, 3);
        }
        
        System.out.println("文件: " + output);
        System.out.println("\nCol 1: 普通数字");
        System.out.println("Col 2: 负红格式 - 负数应显示为红色");
    }
    
    @Test
    void commaFormats() throws Exception {
        Path output = Paths.get("/tmp/comma_format.xlsb");
        
        System.out.println("=== 千分位格式测试 ===\n");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(output).build()) {
            writer.writeBatch("Comma", (row, col) -> {
                long value = row * 100000L;
                switch (col) {
                    case 0: return CellData.text("N-" + row);
                    case 1: return CellData.number(value);
                    case 2: return CellData.numberWithComma(value);
                    case 3: return CellData.numberWithComma(value, 0);
                    case 4: return CellData.numberWithComma(value, 4);
                    default: return CellData.blank();
                }
            }, 10, 5);
        }
        
        System.out.println("文件: " + output);
        System.out.println("\nCol 1: 普通数字");
        System.out.println("Col 2: #,##0.00");
        System.out.println("Col 3: #,##0 (无小数)");
        System.out.println("Col 4: #,##0.0000 (4位小数)");
    }
}