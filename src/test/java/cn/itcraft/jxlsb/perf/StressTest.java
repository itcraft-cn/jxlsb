package cn.itcraft.jxlsb.perf;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 压力测试
 * 
 * <p>测试大文件写入和混合数据类型压力。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@DisplayName("Stress Test")
class StressTest {
    
    @Test
    @DisplayName("Test million rows write")
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void testMillionRows() throws IOException {
        Path path = Files.createTempFile("million", ".xlsb");
        
        System.out.println("\n════════════════════════════════════════");
        System.out.println("1M Rows Stress Test");
        System.out.println("════════════════════════════════════════");
        
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemoryMB();
        
        try (XlsbWriter writer = XlsbWriter.builder().path(path).build()) {
            writer.writeBatch("Sheet1",
                (row, col) -> CellData.number(row + col * 0.1),
                1_000_000, 10);
        }
        
        long endTime = System.currentTimeMillis();
        long endMemory = getUsedMemoryMB();
        long fileSize = Files.size(path);
        
        System.out.println("  Execution time: " + (endTime - startTime) + " ms");
        System.out.println("  Memory delta: " + (endMemory - startMemory) + " MB");
        System.out.println("  File size: " + (fileSize / 1024 / 1024) + " MB");
        System.out.println("  Write speed: " + 
            (fileSize / 1024.0 / 1024.0 / ((endTime - startTime) / 1000.0)) + " MB/s");
        
        assertTrue(fileSize < 50 * 1024 * 1024, "File size should be < 50MB");
        
        System.out.println("  Note: Memory delta may vary based on JVM heap usage");
        
        Files.deleteIfExists(path);
    }
    
    @Test
    @DisplayName("Test mixed data type stress")
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void testMixedDataType() throws IOException {
        Path path = Files.createTempFile("mixed-stress", ".xlsb");
        
        System.out.println("\n════════════════════════════════════════");
        System.out.println("Mixed Data Type Stress Test");
        System.out.println("════════════════════════════════════════");
        
        long startTime = System.currentTimeMillis();
        
        try (XlsbWriter writer = XlsbWriter.builder().path(path).build()) {
            writer.writeBatch("Sheet1",
                (row, col) -> {
                    switch (col % 6) {
                        case 0: return CellData.number(row * 1000.50 + col);
                        case 1: return CellData.text("Product-" + row + "-" + col);
                        case 2: return CellData.date(System.currentTimeMillis() + row * 1000);
                        case 3: return CellData.bool(row % 2 == 0);
                        case 4: return CellData.blank();
                        case 5: return CellData.number(Math.sqrt(row * col));
                        default: return CellData.number(row + col);
                    }
                },
                100_000, 20);
        }
        
        long endTime = System.currentTimeMillis();
        long fileSize = Files.size(path);
        
        System.out.println("  Execution time: " + (endTime - startTime) + " ms");
        System.out.println("  File size: " + (fileSize / 1024 / 1024) + " MB");
        System.out.println("  Rows per second: " + 
            (100_000 / ((endTime - startTime) / 1000.0)));
        
        assertTrue(fileSize < 30 * 1024 * 1024, "File size should be < 30MB");
        
        Files.deleteIfExists(path);
    }
    
    @Test
    @DisplayName("Test large text cells")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testLargeTextCells() throws IOException {
        Path path = Files.createTempFile("large-text", ".xlsb");
        
        System.out.println("\n════════════════════════════════════════");
        System.out.println("Large Text Cells Test");
        System.out.println("════════════════════════════════════════");
        
        StringBuilder sb = new StringBuilder(50000);
        for (int i = 0; i < 50000; i++) sb.append('A');
        String largeText = sb.toString();
        
        long startTime = System.currentTimeMillis();
        
        try (XlsbWriter writer = XlsbWriter.builder().path(path).build()) {
            writer.writeBatch("Sheet1",
                (row, col) -> CellData.text(largeText),
                100, 10);
        }
        
        long endTime = System.currentTimeMillis();
        long fileSize = Files.size(path);
        
        System.out.println("  Execution time: " + (endTime - startTime) + " ms");
        System.out.println("  File size: " + (fileSize / 1024 / 1024) + " MB");
        System.out.println("  Text length per cell: " + largeText.length() + " chars");
        
        assertTrue(fileSize > 0, 
            "File should contain large text data");
        
        Files.deleteIfExists(path);
    }
    
    private long getUsedMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }
}