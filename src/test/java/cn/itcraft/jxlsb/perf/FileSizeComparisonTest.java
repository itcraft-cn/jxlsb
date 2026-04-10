package cn.itcraft.jxlsb.perf;

import cn.itcraft.jxlsb.api.XlsbReader;
import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.util.TempFile;
import org.apache.poi.util.DefaultTempFileCreationStrategy;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.util.*;

@DisplayName("File Size Comparison Test")
public class FileSizeComparisonTest {
    
    @TempDir
    Path tempDir;
    
    private Path poiTempDir;
    
    @BeforeEach
    void setUp() throws IOException {
        poiTempDir = Files.createTempDirectory("poi-temp-");
        TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy(poiTempDir.toFile()));
    }
    
    @AfterEach
    void tearDown() throws IOException {
        if (poiTempDir != null) {
            Files.walk(poiTempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) {} });
        }
    }
    
    @Test
    @DisplayName("Compare file sizes: 100K rows x 10 columns")
    void compareFileSizes100K() throws IOException {
        compareFileSizes(100_000, 10);
    }
    
    @Test
    @DisplayName("Compare file sizes: 1M rows x 10 columns")
    void compareFileSizes1M() throws IOException {
        compareFileSizes(1_000_000, 10);
    }
    
    private void compareFileSizes(int rows, int cols) throws IOException {
        System.out.println("\n=== File Size Comparison: " + rows + " rows x " + cols + " columns ===\n");
        
        List<TestData> testDatas = new ArrayList<>(Math.min(rows, 10000));
        for (int i = 0; i < Math.min(rows, 10000); i++) {
            testDatas.add(new TestData("Product-" + i, i * 100.50, System.currentTimeMillis(), i % 2 == 0));
        }
        
        Path poiFile = tempDir.resolve("poi-size.xlsx");
        long poiStart = System.currentTimeMillis();
        generatePoiFile(poiFile, rows, cols, testDatas);
        long poiTime = System.currentTimeMillis() - poiStart;
        long poiSize = Files.size(poiFile);
        
        Path easyExcelFile = tempDir.resolve("easyexcel-size.xlsx");
        long easyStart = System.currentTimeMillis();
        generateEasyExcelFile(easyExcelFile, rows, cols, testDatas);
        long easyTime = System.currentTimeMillis() - easyStart;
        long easySize = Files.size(easyExcelFile);
        
        Path jxlsbFile = tempDir.resolve("jxlsb-size.xlsb");
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
        
        System.out.println("\nSize Reduction (XLSB vs XLSX):");
        System.out.printf("  POI XLSX is %.2fx larger than jxlsb XLSB\n", poiRatio);
        System.out.printf("  EasyExcel XLSX is %.2fx larger than jxlsb XLSB\n", easyRatio);
        System.out.printf("  XLSB saves %.1f%% space compared to POI XLSX\n", (1 - 1.0/poiRatio) * 100);
        System.out.printf("  XLSB saves %.1f%% space compared to EasyExcel XLSX\n", (1 - 1.0/easyRatio) * 100);
        
        Files.deleteIfExists(poiFile);
        Files.deleteIfExists(easyExcelFile);
        Files.deleteIfExists(jxlsbFile);
    }
    
    private void generatePoiFile(Path file, int rows, int cols, List<TestData> testDatas) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
            Sheet sheet = workbook.createSheet("Sheet1");
            
            for (int row = 0; row < rows; row++) {
                Row excelRow = sheet.createRow(row);
                TestData data = testDatas.get(row % testDatas.size());
                
                for (int col = 0; col < cols; col++) {
                    Cell cell = excelRow.createCell(col);
                    switch (col % 4) {
                        case 0: cell.setCellValue(data.product + "-" + col); break;
                        case 1: cell.setCellValue(data.price + col); break;
                        case 2: cell.setCellValue(new Date(data.timestamp)); break;
                        case 3: cell.setCellValue(data.active); break;
                    }
                }
            }
            
            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
    }
    
    private void generateEasyExcelFile(Path file, int rows, int cols, List<TestData> testDatas) throws IOException {
        List<List<Object>> dataList = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            TestData data = testDatas.get(row % testDatas.size());
            List<Object> rowData = new ArrayList<>(cols);
            for (int col = 0; col < cols; col++) {
                switch (col % 4) {
                    case 0: rowData.add(data.product + "-" + col); break;
                    case 1: rowData.add(data.price + col); break;
                    case 2: rowData.add(new Date(data.timestamp)); break;
                    case 3: rowData.add(data.active); break;
                    default: rowData.add(null);
                }
            }
            dataList.add(rowData);
        }
        
        EasyExcel.write(file.toFile()).sheet("Sheet1").doWrite(dataList);
    }
    
    private void generateJxlsbFile(Path file, int rows, int cols) throws IOException {
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
    
    public static class TestData {
        private String product;
        private double price;
        private long timestamp;
        private boolean active;
        
        public TestData() {}
        
        public TestData(String product, double price, long timestamp, boolean active) {
            this.product = product;
            this.price = price;
            this.timestamp = timestamp;
            this.active = active;
        }
        
        public String getProduct() { return product; }
        public void setProduct(String product) { this.product = product; }
        
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}