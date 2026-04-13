package cn.itcraft.jxlsb.perf;

import cn.itcraft.jxlsb.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class ApiStylePerformanceTest {
    
    @TempDir
    Path tempDir;
    
    private static final int[] ROW_COUNTS = {1_000, 100_000};
    private static final int COLUMN_COUNT = 10;
    private static final int PAGE_SIZE = 1000;
    private static final int WARMUP = 2;
    private static final int ITERATIONS = 5;
    
    static class TestData {
        long id;
        String name;
        double value;
        String category;
        long timestamp;
        
        TestData(long id, String name, double value, String category, long timestamp) {
            this.id = id;
            this.name = name;
            this.value = value;
            this.category = category;
            this.timestamp = timestamp;
        }
    }
    
    @Test
    void compareWriteApiStyles() throws IOException {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║              写入 API 性能对比                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        for (int rowCount : ROW_COUNTS) {
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("测试规模: " + formatCount(rowCount) + " 行 × " + COLUMN_COUNT + " 列");
            System.out.println("═══════════════════════════════════════════════════════════\n");
            
            long batchTime = testWriteBatch(rowCount);
            long streamingTime = testWriteStreaming(rowCount);
            
            System.out.println();
            System.out.printf("┌────────────────┬──────────────┬──────────────────┐%n");
            System.out.printf("│ API风格         │ 耗时         │ 对比             │%n");
            System.out.printf("├────────────────┼──────────────┼──────────────────┤%n");
            System.out.printf("│ writeBatch     │ %8d ms  │ 基准             │%n", batchTime);
            System.out.printf("│ streaming      │ %8d ms  │ %+.1f%%           │%n", 
                streamingTime, 
                (double)(streamingTime - batchTime) / batchTime * 100);
            System.out.printf("└────────────────┴──────────────┴──────────────────┘%n%n");
        }
    }
    
    @Test
    void compareReadApiStyles() throws IOException {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║              读取 API 性能对比                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        for (int rowCount : ROW_COUNTS) {
            Path testFile = createTestFile(rowCount);
            
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("测试规模: " + formatCount(rowCount) + " 行 × " + COLUMN_COUNT + " 列");
            System.out.println("═══════════════════════════════════════════════════════════\n");
            
            long streamingTime = testReadStreaming(testFile, rowCount);
            long paginationTime = testReadPagination(testFile, rowCount);
            
            System.out.println();
            System.out.printf("┌────────────────┬──────────────┬──────────────────┐%n");
            System.out.printf("│ API风格         │ 耗时         │ 对比             │%n");
            System.out.printf("├────────────────┼──────────────┼──────────────────┤%n");
            System.out.printf("│ forEachRow     │ %8d ms  │ 基准             │%n", streamingTime);
            System.out.printf("│ readRows       │ %8d ms  │ %+.1f%%           │%n", 
                paginationTime,
                (double)(paginationTime - streamingTime) / streamingTime * 100);
            System.out.printf("└────────────────┴──────────────┴──────────────────┘%n%n");
        }
    }
    
    private long testWriteBatch(int rowCount) throws IOException {
        for (int i = 0; i < WARMUP; i++) {
            Path warmup = tempDir.resolve("warmup_batch_" + i + ".xlsb");
            runWriteBatch(warmup, rowCount);
        }
        
        long total = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            Path file = tempDir.resolve("perf_batch_" + i + ".xlsb");
            long start = System.nanoTime();
            runWriteBatch(file, rowCount);
            total += (System.nanoTime() - start) / 1_000_000;
        }
        return total / ITERATIONS;
    }
    
    private void runWriteBatch(Path file, int rowCount) throws IOException {
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Data", (row, col) -> createCellData(row, col), rowCount, COLUMN_COUNT);
        }
    }
    
    private long testWriteStreaming(int rowCount) throws IOException {
        for (int i = 0; i < WARMUP; i++) {
            Path warmup = tempDir.resolve("warmup_stream_" + i + ".xlsb");
            runWriteStreaming(warmup, rowCount);
        }
        
        long total = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            Path file = tempDir.resolve("perf_stream_" + i + ".xlsb");
            long start = System.nanoTime();
            runWriteStreaming(file, rowCount);
            total += (System.nanoTime() - start) / 1_000_000;
        }
        return total / ITERATIONS;
    }
    
    private void runWriteStreaming(Path file, int rowCount) throws IOException {
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.startSheet("Data", COLUMN_COUNT);
            
            int offset = 0;
            while (offset < rowCount) {
                int batchSize = Math.min(PAGE_SIZE, rowCount - offset);
                List<TestData> batch = createDataBatch(offset, batchSize);
                writer.writeRows(batch, offset, (data, col) -> {
                    switch (col) {
                        case 0: return CellData.number(data.id);
                        case 1: return CellData.text(data.name);
                        case 2: return CellData.number(data.value);
                        case 3: return CellData.text(data.category);
                        case 4: return CellData.date(data.timestamp);
                        default: return CellData.number(col);
                    }
                });
                offset += batchSize;
            }
            
            writer.endSheet();
        }
    }
    
    private long testReadStreaming(Path file, int rowCount) throws IOException {
        for (int i = 0; i < WARMUP; i++) {
            runReadStreaming(file, rowCount);
        }
        
        long total = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            int readRows = runReadStreaming(file, rowCount);
            total += (System.nanoTime() - start) / 1_000_000;
        }
        return total / ITERATIONS;
    }
    
    private int runReadStreaming(Path file, int expectedRows) throws IOException {
        AtomicInteger count = new AtomicInteger(0);
        try (XlsbReader reader = XlsbReader.builder().path(file).build()) {
            reader.forEachRow(0, new RowConsumer() {
                @Override
                public void onRowStart(int rowIndex) {
                }
                
                @Override
                public void onCell(int row, int col, CellData cell) {
                    if (col == 0) {
                        count.incrementAndGet();
                    }
                }
                
                @Override
                public void onRowEnd(int rowIndex) {
                }
            });
        }
        return count.get();
    }
    
    private long testReadPagination(Path file, int rowCount) throws IOException {
        for (int i = 0; i < WARMUP; i++) {
            runReadPagination(file, rowCount);
        }
        
        long total = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            int readRows = runReadPagination(file, rowCount);
            total += (System.nanoTime() - start) / 1_000_000;
        }
        return total / ITERATIONS;
    }
    
    private int runReadPagination(Path file, int expectedRows) throws IOException {
        int totalRows = 0;
        try (XlsbReader reader = XlsbReader.builder().path(file).build()) {
            int offset = 0;
            while (offset < expectedRows) {
                List<CellData[]> batch = reader.readRows(0, offset, PAGE_SIZE);
                totalRows += batch.size();
                offset += PAGE_SIZE;
                if (batch.isEmpty()) break;
            }
        }
        return totalRows;
    }
    
    private Path createTestFile(int rowCount) throws IOException {
        Path file = tempDir.resolve("read_test_" + rowCount + ".xlsb");
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Data", (row, col) -> createCellData(row, col), rowCount, COLUMN_COUNT);
        }
        return file;
    }
    
    private CellData createCellData(int row, int col) {
        switch (col % 5) {
            case 0: return CellData.number(row * 1000L + col);
            case 1: return CellData.text("Text-" + row + "-" + col);
            case 2: return CellData.number(row * 1.5 + col);
            case 3: return CellData.text("Cat-" + (row % 10));
            case 4: return CellData.date(System.currentTimeMillis() - row * 1000L);
            default: return CellData.number(col);
        }
    }
    
    private List<TestData> createDataBatch(int startId, int count) {
        List<TestData> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long id = startId + i;
            batch.add(new TestData(id, "Name-" + id, id * 1.5, "Cat-" + (id % 10), 
                System.currentTimeMillis() - id * 1000));
        }
        return batch;
    }
    
    private String formatCount(int count) {
        if (count >= 1_000_000) return (count / 1_000_000) + "M";
        if (count >= 1_000) return (count / 1_000) + "K";
        return String.valueOf(count);
    }
}