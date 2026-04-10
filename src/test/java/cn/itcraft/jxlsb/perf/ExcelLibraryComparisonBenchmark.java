package cn.itcraft.jxlsb.perf;

import cn.itcraft.jxlsb.api.XlsbReader;
import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Excel库性能对比基准测试
 * 
 * <p>对比POI、EasyExcel、jxlsb的读写性能。
 * 
 * <p>测试参数：
 * - 预热10次
 * - 测试5次
 * - 每次预热100ms
 * - 每次测试20ms
 * - 4线程
 * - fork 3次
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(3)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Threads(4)
public class ExcelLibraryComparisonBenchmark {
    
    @Param({"1000", "10000", "100000"})
    int rowCount;
    
    @Param({"10"})
    int columnCount;
    
    private Path tempDir;
    private List<TestData> testDatas;
    
    // 固定文件名用于读取测试
    private Path poiReadFile;
    private Path easyexcelReadFile;
    private Path jxlsbReadFile;
    
    @Setup(Level.Trial)
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("excel-bench-");
        
        // 生成测试数据
        testDatas = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            testDatas.add(new TestData(
                "Product-" + i,
                i * 100.50,
                System.currentTimeMillis(),
                i % 2 == 0
            ));
        }
        
        // 创建预埋的读取测试文件
        poiReadFile = tempDir.resolve("poi-read.xlsx");
        easyexcelReadFile = tempDir.resolve("easyexcel-read.xlsx");
        jxlsbReadFile = tempDir.resolve("jxlsb-read.xlsb");
        
        // 预生成文件
        generatePoiFile(poiReadFile);
        generateEasyExcelFile(easyexcelReadFile);
        generateJxlsbFile(jxlsbReadFile);
    }
    
    @TearDown(Level.Trial)
    public void teardown() throws IOException {
        // 清理临时文件
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
    }
    
    // ==================== 写入测试 ====================
    
    @Benchmark
    public void writeWithPOI(Blackhole bh) throws IOException {
        Path file = tempDir.resolve("poi-write-" + UUID.randomUUID() + ".xlsx");
        
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Sheet1");
            
            for (int row = 0; row < rowCount; row++) {
                Row excelRow = sheet.createRow(row);
                TestData data = testDatas.get(row);
                
                excelRow.createCell(0).setCellValue(data.product);
                excelRow.createCell(1).setCellValue(data.price);
                excelRow.createCell(2).setCellValue(new Date(data.timestamp));
                excelRow.createCell(3).setCellValue(data.active);
            }
            
            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
        
        bh.consume(file);
        Files.deleteIfExists(file);
    }
    
    @Benchmark
    public void writeWithEasyExcel(Blackhole bh) throws IOException {
        Path file = tempDir.resolve("easyexcel-write-" + UUID.randomUUID() + ".xlsx");
        
        EasyExcel.write(file.toFile(), TestData.class)
            .sheet("Sheet1")
            .doWrite(testDatas);
        
        bh.consume(file);
        Files.deleteIfExists(file);
    }
    
    @Benchmark
    public void writeWithJxlsb(Blackhole bh) throws IOException {
        Path file = tempDir.resolve("jxlsb-write-" + UUID.randomUUID() + ".xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(file)
                .build()) {
            
            writer.writeBatch("Sheet1",
                (row, col) -> {
                    TestData data = testDatas.get(row);
                    switch (col) {
                        case 0: return CellData.text(data.product);
                        case 1: return CellData.number(data.price);
                        case 2: return CellData.date(data.timestamp);
                        case 3: return CellData.bool(data.active);
                        default: return CellData.blank();
                    }
                },
                rowCount, columnCount);
        }
        
        bh.consume(file);
        Files.deleteIfExists(file);
    }
    
    // ==================== 读取测试 ====================
    
    @Benchmark
    public void readWithPOI(Blackhole bh) throws IOException {
        try (InputStream in = Files.newInputStream(poiReadFile);
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            for (Row row : sheet) {
                for (int i = 0; i < 4; i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null) {
                        bh.consume(getCellValue(cell));
                    }
                }
            }
        }
    }
    
    @Benchmark
    public void readWithEasyExcel(Blackhole bh) throws IOException {
        List<Object> results = new ArrayList<>();
        
        EasyExcel.read(easyexcelReadFile.toFile(), TestData.class, 
            new AnalysisEventListener<TestData>() {
                @Override
                public void invoke(TestData data, AnalysisContext context) {
                    results.add(data);
                }
                
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().doRead();
        
        bh.consume(results);
    }
    
    @Benchmark
    public void readWithJxlsb(Blackhole bh) throws IOException {
        try (XlsbReader reader = XlsbReader.builder()
                .path(jxlsbReadFile)
                .build()) {
            
            reader.readSheets(sheet -> {
                for (OffHeapRow row : sheet) {
                    for (int i = 0; i < row.getColumnCount(); i++) {
                        OffHeapCell cell = row.getCell(i);
                        bh.consume(readCell(cell));
                    }
                }
            });
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private void generatePoiFile(Path file) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Sheet1");
            
            for (int row = 0; row < rowCount; row++) {
                Row excelRow = sheet.createRow(row);
                TestData data = testDatas.get(row);
                
                excelRow.createCell(0).setCellValue(data.product);
                excelRow.createCell(1).setCellValue(data.price);
                excelRow.createCell(2).setCellValue(new Date(data.timestamp));
                excelRow.createCell(3).setCellValue(data.active);
            }
            
            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
    }
    
    private void generateEasyExcelFile(Path file) throws IOException {
        EasyExcel.write(file.toFile(), TestData.class)
            .sheet("Sheet1")
            .doWrite(testDatas);
    }
    
    private void generateJxlsbFile(Path file) throws IOException {
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(file)
                .build()) {
            
            writer.writeBatch("Sheet1",
                (row, col) -> {
                    TestData data = testDatas.get(row);
                    switch (col) {
                        case 0: return CellData.text(data.product);
                        case 1: return CellData.number(data.price);
                        case 2: return CellData.date(data.timestamp);
                        case 3: return CellData.bool(data.active);
                        default: return CellData.blank();
                    }
                },
                rowCount, columnCount);
        }
    }
    
    private Object getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            default:
                return null;
        }
    }
    
    private Object readCell(OffHeapCell cell) {
        switch (cell.getType()) {
            case TEXT:
                return cell.getText();
            case NUMBER:
                return cell.getNumber();
            case DATE:
                return cell.getDate();
            case BOOLEAN:
                return cell.getBoolean();
            default:
                return null;
        }
    }
    
    // 测试数据类
    public static class TestData {
        private String product;
        private double price;
        private long timestamp;
        private boolean active;
        
        public TestData() {
        }
        
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