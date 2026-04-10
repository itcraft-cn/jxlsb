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
import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * GC监控测试
 * 
 * <p>监控GC次数、GC时间和内存压力。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@DisplayName("GC Monitor Test")
class GCMonitorTest {
    
    @Test
    @DisplayName("Test GC pressure on write")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testGCPressureOnWrite() throws IOException {
        Path path = Files.createTempFile("gc-write", ".xlsb");
        
        System.out.println("\n════════════════════════════════════════");
        System.out.println("GC Pressure Test - Write");
        System.out.println("════════════════════════════════════════");
        
        List<GarbageCollectorMXBean> gcBeans = 
            ManagementFactory.getGarbageCollectorMXBeans();
        
        long initialGcCount = getTotalGcCount(gcBeans);
        long initialGcTime = getTotalGcTime(gcBeans);
        
        try (XlsbWriter writer = XlsbWriter.builder().path(path).build()) {
            writer.writeBatch("Sheet1",
                (row, col) -> CellData.number(row + col),
                100_000, 10);
        }
        
        long fileSize = Files.size(path);
        
        long finalGcCount = getTotalGcCount(gcBeans);
        long finalGcTime = getTotalGcTime(gcBeans);
        
        long gcCountDelta = finalGcCount - initialGcCount;
        long gcTimeDelta = finalGcTime - initialGcTime;
        long fileSizeMB = fileSize / 1024 / 1024;
        
        System.out.println("  File size: " + fileSizeMB + " MB");
        System.out.println("  GC count delta: " + gcCountDelta);
        System.out.println("  GC time delta: " + gcTimeDelta + " ms");
        System.out.println("  GC per MB: " + (fileSizeMB > 0 ? gcCountDelta / fileSizeMB : 0));
        
        int expectedMaxGc = Math.max(5, (int) (fileSizeMB * 5));
        
        assertTrue(gcCountDelta < expectedMaxGc, 
            "Too many GC: " + gcCountDelta + " (expected < " + expectedMaxGc + ")");
        
        Files.deleteIfExists(path);
    }
    
    @Test
    @DisplayName("Test GC frequency on million rows")
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void testGCFrequencyOnMillionRows() throws IOException {
        Path path = Files.createTempFile("gc-million", ".xlsb");
        
        System.out.println("\n════════════════════════════════════════");
        System.out.println("GC Frequency Test - Million Rows");
        System.out.println("════════════════════════════════════════");
        
        List<GarbageCollectorMXBean> gcBeans = 
            ManagementFactory.getGarbageCollectorMXBeans();
        
        long initialGcCount = getTotalGcCount(gcBeans);
        
        long startTime = System.currentTimeMillis();
        
        try (XlsbWriter writer = XlsbWriter.builder().path(path).build()) {
            writer.writeBatch("Sheet1",
                (row, col) -> CellData.number(row + col),
                1_000_000, 10);
        }
        
        long endTime = System.currentTimeMillis();
        
        long finalGcCount = getTotalGcCount(gcBeans);
        long gcCountDelta = finalGcCount - initialGcCount;
        long fileSize = Files.size(path);
        
        System.out.println("  Execution time: " + (endTime - startTime) + " ms");
        System.out.println("  File size: " + (fileSize / 1024 / 1024) + " MB");
        System.out.println("  GC count: " + gcCountDelta);
        System.out.println("  GC frequency: " + 
            (gcCountDelta / ((endTime - startTime) / 1000.0)) + " GC/s");
        
        assertTrue(gcCountDelta < 20, 
            "Too frequent GC: " + gcCountDelta + " (expected < 20 for 1M rows)");
        
        Files.deleteIfExists(path);
    }
    
    @Test
    @DisplayName("Test memory allocation pattern")
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void testMemoryAllocationPattern() throws IOException {
        Path path = Files.createTempFile("gc-pattern", ".xlsb");
        
        System.out.println("\n════════════════════════════════════════");
        System.out.println("Memory Allocation Pattern Test");
        System.out.println("════════════════════════════════════════");
        
        Runtime runtime = Runtime.getRuntime();
        
        long[] memorySnapshots = new long[10];
        long[] gcSnapshots = new long[10];
        
        List<GarbageCollectorMXBean> gcBeans = 
            ManagementFactory.getGarbageCollectorMXBeans();
        
        for (int i = 0; i < 10; i++) {
            Files.deleteIfExists(path);
            path = Files.createTempFile("gc-pattern-" + i, ".xlsb");
            
            memorySnapshots[i] = getUsedMemoryMB(runtime);
            gcSnapshots[i] = getTotalGcCount(gcBeans);
            
            try (XlsbWriter writer = XlsbWriter.builder().path(path).build()) {
                writer.writeBatch("Sheet" + i,
                    (row, col) -> CellData.number(row * 100.0 + col),
                    10_000, 10);
            }
        }
        
        System.out.println("  Memory pattern:");
        for (int i = 0; i < 10; i++) {
            System.out.println("    Iteration " + i + ": " + 
                memorySnapshots[i] + " MB, GC: " + gcSnapshots[i]);
        }
        
        Files.deleteIfExists(path);
    }
    
    private long getTotalGcCount(List<GarbageCollectorMXBean> gcBeans) {
        return gcBeans.stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionCount)
            .sum();
    }
    
    private long getTotalGcTime(List<GarbageCollectorMXBean> gcBeans) {
        return gcBeans.stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionTime)
            .sum();
    }
    
    private long getUsedMemoryMB(Runtime runtime) {
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }
}