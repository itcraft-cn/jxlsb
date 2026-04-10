package cn.itcraft.jxlsb.perf;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.TempFile;
import org.apache.poi.util.DefaultTempFileCreationStrategy;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import com.alibaba.excel.EasyExcel;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.time.LocalDateTime;

public class PerformanceComparison {
    
    public static void main(String[] args) throws IOException {
        Path tempDir = Files.createTempDirectory("excel-perf-");
        Path poiTempDir = Files.createTempDirectory("poi-temp-");
        
        TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy(poiTempDir.toFile()));
        
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║      Excel库性能和文件大小对比测试                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        int[] testSizes = {1_000, 10_000, 100_000};
        
        for (int rows : testSizes) {
            System.out.println(repeat("═", 60));
            System.out.println("测试规模: " + formatNumber(rows) + " 行 × 10 列");
            System.out.println(repeat("═", 60));
            
            compareLibraries(tempDir, rows, 10);
            System.out.println();
        }
        
        cleanupDir(tempDir);
        cleanupDir(poiTempDir);
        
        System.out.println("测试完成！");
    }
    
    private static void cleanupDir(Path dir) throws IOException {
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) {} });
    }
    
    private static void compareLibraries(Path tempDir, int rows, int cols) throws IOException {
        System.out.println();
        
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
        
        System.out.println("\n性能对比分析:");
        System.out.printf("  jxlsb vs POI:       %.1fx 更快, 文件小 %.1f%%\n",
            (double)poi.timeMs / jxlsb.timeMs, (1 - (double)jxlsb.size / poi.size) * 100);
        System.out.printf("  jxlsb vs EasyExcel: %.1fx 更快, 文件小 %.1f%%\n",
            (double)easyexcel.timeMs / jxlsb.timeMs, (1 - (double)jxlsb.size / easyexcel.size) * 100);
        System.out.printf("  jxlsb vs FastExcel: %.1fx 更快, 文件小 %.1f%%\n",
            (double)fastexcel.timeMs / jxlsb.timeMs, (1 - (double)jxlsb.size / fastexcel.size) * 100);
    }
    
    private static Result testPOI(Path tempDir, int rows, int cols) throws IOException {
        Path file = tempDir.resolve("poi-" + rows + ".xlsx");
        long start = System.currentTimeMillis();
        
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
            Sheet sheet = workbook.createSheet("Sheet1");
            
            for (int row = 0; row < rows; row++) {
                Row excelRow = sheet.createRow(row);
                
                for (int col = 0; col < cols; col++) {
                    Cell cell = excelRow.createCell(col);
                    switch (col % 4) {
                        case 0: cell.setCellValue("Product-" + row + "-" + col); break;
                        case 1: cell.setCellValue(row * 100.50 + col); break;
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
    
    private static Result testEasyExcel(Path tempDir, int rows, int cols) throws IOException {
        Path file = tempDir.resolve("easyexcel-" + rows + ".xlsx");
        long start = System.currentTimeMillis();
        
        List<List<Object>> dataList = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            List<Object> rowData = new ArrayList<>(cols);
            for (int col = 0; col < cols; col++) {
                switch (col % 4) {
                    case 0: rowData.add("Product-" + row + "-" + col); break;
                    case 1: rowData.add(row * 100.50 + col); break;
                    case 2: rowData.add(new Date()); break;
                    case 3: rowData.add(row % 2 == 0); break;
                    default: rowData.add(null);
                }
            }
            dataList.add(rowData);
        }
        
        EasyExcel.write(file.toFile()).sheet("Sheet1").doWrite(dataList);
        
        long time = System.currentTimeMillis() - start;
        long size = Files.size(file);
        Files.deleteIfExists(file);
        
        return new Result(size, time);
    }
    
    private static Result testFastExcel(Path tempDir, int rows, int cols) throws IOException {
        Path file = tempDir.resolve("fastexcel-" + rows + ".xlsx");
        long start = System.currentTimeMillis();
        
        try (OutputStream out = Files.newOutputStream(file);
             Workbook workbook = new Workbook(out, "TestApp", "1.0")) {
            
            Worksheet sheet = workbook.newWorksheet("Sheet1");
            
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    switch (col % 4) {
                        case 0: sheet.value(row, col, "Product-" + row + "-" + col); break;
                        case 1: sheet.value(row, col, row * 100.50 + col); break;
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
    
    private static Result testJxlsb(Path tempDir, int rows, int cols) throws IOException {
        Path file = tempDir.resolve("jxlsb-" + rows + ".xlsb");
        long start = System.currentTimeMillis();
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Sheet1",
                (row, col) -> {
                    switch (col % 4) {
                        case 0: return CellData.text("Product-" + row + "-" + col);
                        case 1: return CellData.number(row * 100.50 + col);
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
    
    private static void printResult(String name, Result result, String format) {
        System.out.printf("│ %-10s │ %8.2f MB │ %6d ms   │ %-12s │\n",
            name, result.size / 1024.0 / 1024.0, result.timeMs, format);
    }
    
    private static String formatNumber(int num) {
        if (num >= 1_000_000) return (num / 1_000_000) + "M";
        if (num >= 1_000) return (num / 1_000) + "K";
        return String.valueOf(num);
    }
    
    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
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