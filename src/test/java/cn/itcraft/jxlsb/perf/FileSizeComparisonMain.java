package cn.itcraft.jxlsb.perf;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.util.TempFile;
import org.apache.poi.util.DefaultTempFileCreationStrategy;
import com.alibaba.excel.EasyExcel;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public class FileSizeComparisonMain {
    
    public static void main(String[] args) throws IOException {
        Path tempDir = Files.createTempDirectory("excel-compare-");
        Path poiTempDir = Files.createTempDirectory("poi-temp-");
        
        TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy(poiTempDir.toFile()));
        
        System.out.println("\n========================================");
        System.out.println("   File Size Comparison Benchmark");
        System.out.println("========================================\n");
        
        compareFileSizes(tempDir, 100_000, 10);
        compareFileSizes(tempDir, 1_000_000, 10);
        
        cleanupDir(tempDir);
        cleanupDir(poiTempDir);
    }
    
    private static void cleanupDir(Path dir) throws IOException {
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) {} });
    }
    
    private static void compareFileSizes(Path tempDir, int rows, int cols) throws IOException {
        System.out.println("\n=== Test: " + formatNumber(rows) + " rows x " + cols + " columns ===\n");
        
        Path poiFile = tempDir.resolve("poi-" + rows + ".xlsx");
        long poiStart = System.currentTimeMillis();
        generatePoiFile(poiFile, rows, cols);
        long poiTime = System.currentTimeMillis() - poiStart;
        long poiSize = Files.size(poiFile);
        
        Path easyExcelFile = tempDir.resolve("easyexcel-" + rows + ".xlsx");
        long easyStart = System.currentTimeMillis();
        generateEasyExcelFile(easyExcelFile, rows, cols);
        long easyTime = System.currentTimeMillis() - easyStart;
        long easySize = Files.size(easyExcelFile);
        
        Path jxlsbFile = tempDir.resolve("jxlsb-" + rows + ".xlsb");
        long jxlsbStart = System.currentTimeMillis();
        generateJxlsbFile(jxlsbFile, rows, cols);
        long jxlsbTime = System.currentTimeMillis() - jxlsbStart;
        long jxlsbSize = Files.size(jxlsbFile);
        
        System.out.println("+----------------+------------+------------+------------+");
        System.out.println("| Library        | File Size  | Write Time | Format     |");
        System.out.println("+----------------+------------+------------+------------+");
        System.out.printf("| %-14s | %8.2fMB | %6dms   | %-10s |\n", "POI", poiSize / 1024.0 / 1024.0, poiTime, "XLSX");
        System.out.printf("| %-14s | %8.2fMB | %6dms   | %-10s |\n", "EasyExcel", easySize / 1024.0 / 1024.0, easyTime, "XLSX");
        System.out.printf("| %-14s | %8.2fMB | %6dms   | %-10s |\n", "jxlsb", jxlsbSize / 1024.0 / 1024.0, jxlsbTime, "XLSB");
        System.out.println("+----------------+------------+------------+------------+");
        
        double poiRatio = (double) poiSize / jxlsbSize;
        double easyRatio = (double) easySize / jxlsbSize;
        
        System.out.println("\nSize Reduction Analysis:");
        System.out.printf("  POI XLSX is       %.2fx larger than jxlsb XLSB\n", poiRatio);
        System.out.printf("  EasyExcel XLSX is %.2fx larger than jxlsb XLSB\n", easyRatio);
        System.out.printf("  XLSB saves %.1f%% space compared to POI XLSX\n", (1 - 1.0/poiRatio) * 100);
        System.out.printf("  XLSB saves %.1f%% space compared to EasyExcel XLSX\n", (1 - 1.0/easyRatio) * 100);
        
        Files.deleteIfExists(poiFile);
        Files.deleteIfExists(easyExcelFile);
        Files.deleteIfExists(jxlsbFile);
    }
    
    private static void generatePoiFile(Path file, int rows, int cols) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
            Sheet sheet = workbook.createSheet("Sheet1");
            
            for (int row = 0; row < rows; row++) {
                Row excelRow = sheet.createRow(row);
                
                for (int col = 0; col < cols; col++) {
                    Cell cell = excelRow.createCell(col);
                    switch (col % 4) {
                        case 0: cell.setCellValue("Product-" + row + "-" + col); break;
                        case 1: cell.setCellValue(row * 100.50 + col); break;
                        case 2: cell.setCellValue(new Date(System.currentTimeMillis())); break;
                        case 3: cell.setCellValue(row % 2 == 0); break;
                    }
                }
            }
            
            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
    }
    
    private static void generateEasyExcelFile(Path file, int rows, int cols) throws IOException {
        List<List<Object>> dataList = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            List<Object> rowData = new ArrayList<>(cols);
            for (int col = 0; col < cols; col++) {
                switch (col % 4) {
                    case 0: rowData.add("Product-" + row + "-" + col); break;
                    case 1: rowData.add(row * 100.50 + col); break;
                    case 2: rowData.add(new Date(System.currentTimeMillis())); break;
                    case 3: rowData.add(row % 2 == 0); break;
                    default: rowData.add(null);
                }
            }
            dataList.add(rowData);
        }
        
        EasyExcel.write(file.toFile()).sheet("Sheet1").doWrite(dataList);
    }
    
    private static void generateJxlsbFile(Path file, int rows, int cols) throws IOException {
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
    }
    
    private static String formatNumber(int num) {
        if (num >= 1_000_000) return (num / 1_000_000) + "M";
        if (num >= 1_000) return (num / 1_000) + "K";
        return String.valueOf(num);
    }
}