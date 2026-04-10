package cn.itcraft.jxlsb.perf;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import com.alibaba.excel.EasyExcel;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.time.LocalDateTime;

/**
 * 性能和文件大小对比测试
 */
class PerformanceComparisonTest {
    
    @Test
    void test1000Rows() throws IOException {
        runComparison(1_000, 10);
    }
    
    @Test
    void test10000Rows() throws IOException {
        runComparison(10_000, 10);
    }
    
    @Test  
    void test100000Rows() throws IOException {
        runComparison(100_000, 10);
    }
    
    private void runComparison(int rows, int cols) throws IOException {
        Path tempDir = Files.createTempDirectory("excel-perf-");
        
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("测试规模: " + formatNumber(rows) + " 行 × " + cols + " 列");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        Result poi = testPOI(tempDir, rows, cols);
        Result easyexcel = testEasyExcel(tempDir, rows, cols);
        Result fastexcel = testFastExcel(tempDir, rows, cols);
        Result jxlsb = testJxlsb(tempDir, rows, cols);
        
        System.out.println("\n┌────────────┬──────────────┬──────────────┬──────────────┐");
        System.out.println("│ 库         │ 文件大小     │ 写入时间     │ 格式         │");
        System.out.println("├────────────┼──────────────┼──────────────┼──────────────┤");
        printResult("POI", poi, "XLSX");
        printResult("EasyExcel", easyexcel, "XLSX");
        printResult("FastExcel", fastexcel, "XLSX");
        printResult("jxlsb", jxlsb, "XLSB");
        System.out.println("└────────────┴──────────────┴──────────────┴──────────────┘");
        
        System.out.println("\n性能对比:");
        System.out.printf("  jxlsb vs POI:       %.1fx 更快, 文件小 %.1f%%\n",
            (double)poi.timeMs / jxlsb.timeMs,
            (1 - (double)jxlsb.size / poi.size) * 100);
        System.out.printf("  jxlsb vs EasyExcel: %.1fx 更快, 文件小 %.1f%%\n",
            (double)easyexcel.timeMs / jxlsb.timeMs,
            (1 - (double)jxlsb.size / easyexcel.size) * 100);
        System.out.printf("  jxlsb vs FastExcel: %.1fx 更快, 文件小 %.1f%%\n",
            (double)fastexcel.timeMs / jxlsb.timeMs,
            (1 - (double)jxlsb.size / fastexcel.size) * 100);
        
        // 清理
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
    }
    
    private Result testPOI(Path tempDir, int rows, int cols) throws IOException {
        Path file = tempDir.resolve("poi.xlsx");
        long start = System.currentTimeMillis();
        
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
            Sheet sheet = workbook.createSheet("Sheet1");
            
            for (int row = 0; row < rows; row++) {
                Row excelRow = sheet.createRow(row);
                for (int col = 0; col < cols; col++) {
                    Cell cell = excelRow.createCell(col);
                    switch (col % 4) {
                        case 0: cell.setCellValue("P-" + row + "-" + col); break;
                        case 1: cell.setCellValue(row * 100.5 + col); break;
                        case 2: cell.setCellValue(new Date()); break;
                        case 3: cell.setCellValue(row % 2 == 0); break;
                    }
                }
            }
            
            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
        
        long time = System.currentTimeMillis() - start;
        long size = Files.size(file);
        Files.deleteIfExists(file);
        return new Result(size, time);
    }
    
    private Result testEasyExcel(Path tempDir, int rows, int cols) throws IOException {
        Path file = tempDir.resolve("easyexcel.xlsx");
        long start = System.currentTimeMillis();
        
        List<List<Object>> data = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            List<Object> rowData = new ArrayList<>(cols);
            for (int col = 0; col < cols; col++) {
                switch (col % 4) {
                    case 0: rowData.add("P-" + row + "-" + col); break;
                    case 1: rowData.add(row * 100.5 + col); break;
                    case 2: rowData.add(new Date()); break;
                    case 3: rowData.add(row % 2 == 0); break;
                    default: rowData.add(null);
                }
            }
            data.add(rowData);
        }
        
        EasyExcel.write(file.toFile()).sheet("Sheet1").doWrite(data);
        
        long time = System.currentTimeMillis() - start;
        long size = Files.size(file);
        Files.deleteIfExists(file);
        return new Result(size, time);
    }
    
    private Result testFastExcel(Path tempDir, int rows, int cols) throws IOException {
        Path file = tempDir.resolve("fastexcel.xlsx");
        long start = System.currentTimeMillis();
        
        try (OutputStream out = Files.newOutputStream(file);
             Workbook workbook = new Workbook(out, "Test", "1.0")) {
            
            Worksheet sheet = workbook.newWorksheet("Sheet1");
            
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    switch (col % 4) {
                        case 0: sheet.value(row, col, "P-" + row + "-" + col); break;
                        case 1: sheet.value(row, col, row * 100.5 + col); break;
                        case 2: sheet.value(row, col, LocalDateTime.now()); break;
                        case 3: sheet.value(row, col, row % 2 == 0); break;
                    }
                }
            }
        }
        
        long time = System.currentTimeMillis() - start;
        long size = Files.size(file);
        Files.deleteIfExists(file);
        return new Result(size, time);
    }
    
    private Result testJxlsb(Path tempDir, int rows, int cols) throws IOException {
        Path file = tempDir.resolve("jxlsb.xlsb");
        long start = System.currentTimeMillis();
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Sheet1",
                (row, col) -> {
                    switch (col % 4) {
                        case 0: return CellData.text("P-" + row + "-" + col);
                        case 1: return CellData.number(row * 100.5 + col);
                        case 2: return CellData.date(System.currentTimeMillis());
                        case 3: return CellData.bool(row % 2 == 0);
                        default: return CellData.blank();
                    }
                },
                rows, cols);
        }
        
        long time = System.currentTimeMillis() - start;
        long size = Files.size(file);
        Files.deleteIfExists(file);
        return new Result(size, time);
    }
    
    private void printResult(String name, Result result, String format) {
        System.out.printf("│ %-10s │ %8.2f MB │ %6d ms   │ %-12s │\n",
            name, result.size / 1024.0 / 1024.0, result.timeMs, format);
    }
    
    private String formatNumber(int num) {
        if (num >= 1_000_000) return (num / 1_000_000) + "M";
        if (num >= 1_000) return (num / 1_000) + "K";
        return String.valueOf(num);
    }
    
    private static final class Result {
        final long size;
        final long timeMs;
        Result(long size, long timeMs) {
            this.size = size;
            this.timeMs = timeMs;
        }
    }
}