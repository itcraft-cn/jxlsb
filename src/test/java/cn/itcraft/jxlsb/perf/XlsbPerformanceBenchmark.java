package cn.itcraft.jxlsb.perf;

import cn.itcraft.jxlsb.api.*;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class XlsbPerformanceBenchmark {
    
    private static final String TEMP_DIR = "/tmp";
    private static final int TOTAL_ROWS = 1_000_000;
    private static final int COLUMN_COUNT = 10;
    private static final int PAGE_SIZE = 1000;
    
    @Test
    void performanceBenchmark() throws Exception {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║         jxlsb 性能基准测试 (100万行 × 10字段)                 ║");
        System.out.println("║         字段类型: 3字符串 + 4整数 + 3浮点数                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
        
        Path batchFile = Paths.get(TEMP_DIR, "jxlsb_batch_write.xlsb");
        Path streamFile = Paths.get(TEMP_DIR, "jxlsb_stream_write.xlsb");
        
        System.out.println("═════════════════════════════════════════════════════════════");
        System.out.println("1. 普通分页写 (startSheet + writeRows + endSheet)");
        System.out.println("═════════════════════════════════════════════════════════════\n");
        
        long batchWriteTime, batchWriteSize;
        {
            long start = System.nanoTime();
            try (XlsbWriter writer = XlsbWriter.builder().path(batchFile).build()) {
                writer.startSheet("Data", COLUMN_COUNT);
                int offset = 0;
                while (offset < TOTAL_ROWS) {
                    int batchSize = Math.min(PAGE_SIZE, TOTAL_ROWS - offset);
                    List<Integer> batch = createBatch(offset, batchSize);
                    writer.writeRows(batch, offset, (rowIdx, col) -> createCell(rowIdx, col));
                    offset += batchSize;
                }
                writer.endSheet();
            }
            batchWriteTime = (System.nanoTime() - start) / 1_000_000;
            batchWriteSize = Files.size(batchFile);
        }
        
        System.out.printf("写入耗时: %d ms\n", batchWriteTime);
        System.out.printf("文件大小: %.2f MB\n", batchWriteSize / 1_000_000.0);
        System.out.printf("写入速度: %.2f 行/秒\n", TOTAL_ROWS / (batchWriteTime / 1000.0));
        System.out.printf("文件路径: %s\n", batchFile);
        
        System.out.println("\n═════════════════════════════════════════════════════════════");
        System.out.println("2. 普通分页读 (readRows, 每次1000行)");
        System.out.println("═════════════════════════════════════════════════════════════\n");
        
        if (!Files.exists(batchFile)) {
            System.err.println("ERROR: 文件不存在: " + batchFile);
            return;
        }
        
        long batchReadTime;
        {
            long start = System.nanoTime();
            try (XlsbReader reader = XlsbReader.builder().path(batchFile).build()) {
                int offset = 0;
                while (offset < TOTAL_ROWS) {
                    List<CellData[]> batch = reader.readRows(0, offset, PAGE_SIZE);
                    if (batch.isEmpty()) break;
                    offset += batch.size();
                }
            }
            batchReadTime = (System.nanoTime() - start) / 1_000_000;
        }
        
        System.out.printf("读取耗时: %d ms\n", batchReadTime);
        System.out.printf("读取速度: %.2f 行/秒\n", TOTAL_ROWS / (batchReadTime / 1000.0));
        
        System.out.println("\n═════════════════════════════════════════════════════════════");
        System.out.println("3. 普通流式写 (writeBatch)");
        System.out.println("═════════════════════════════════════════════════════════════\n");
        
        long streamWriteTime, streamWriteSize;
        {
            long start = System.nanoTime();
            try (XlsbWriter writer = XlsbWriter.builder().path(streamFile).build()) {
                writer.writeBatch("Data", (row, col) -> createCell(row, col), TOTAL_ROWS, COLUMN_COUNT);
            }
            streamWriteTime = (System.nanoTime() - start) / 1_000_000;
            streamWriteSize = Files.size(streamFile);
        }
        
        System.out.printf("写入耗时: %d ms\n", streamWriteTime);
        System.out.printf("文件大小: %.2f MB\n", streamWriteSize / 1_000_000.0);
        System.out.printf("写入速度: %.2f 行/秒\n", TOTAL_ROWS / (streamWriteTime / 1000.0));
        
        System.out.println("\n═════════════════════════════════════════════════════════════");
        System.out.println("4. 普通流式读 (forEachRow)");
        System.out.println("═════════════════════════════════════════════════════════════\n");
        
        long streamReadTime;
        {
            long start = System.nanoTime();
            AtomicInteger count = new AtomicInteger(0);
            try (XlsbReader reader = XlsbReader.builder().path(streamFile).build()) {
                reader.forEachRow(0, new RowConsumer() {
                    @Override public void onRowStart(int rowIndex) {}
                    @Override public void onRowEnd(int rowIndex) {}
                    @Override public void onCell(int row, int col, CellData cell) {
                        if (col == 0) count.incrementAndGet();
                    }
                });
            }
            streamReadTime = (System.nanoTime() - start) / 1_000_000;
        }
        
        System.out.printf("读取耗时: %d ms\n", streamReadTime);
        System.out.printf("读取速度: %.2f 行/秒\n", TOTAL_ROWS / (streamReadTime / 1000.0));
        
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    性能测试结果汇总                           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
        
        System.out.printf("┌────────────────────┬──────────────┬──────────────┬──────────────┐%n");
        System.out.printf("│ 操作类型           │ 耗时 (ms)    │ 文件大小(MB) │ 速度(行/秒) │%n");
        System.out.printf("├────────────────────┼──────────────┼──────────────┼──────────────┤%n");
        System.out.printf("│ %-18s │ %12d │ %12.2f │ %12.0f │%n",
            "分页写", batchWriteTime, batchWriteSize / 1_000_000.0, TOTAL_ROWS / (batchWriteTime / 1000.0));
        System.out.printf("│ %-18s │ %12d │ %12s │ %12.0f │%n",
            "分页读", batchReadTime, "-", TOTAL_ROWS / (batchReadTime / 1000.0));
        System.out.printf("│ %-18s │ %12d │ %12.2f │ %12.0f │%n",
            "流式写", streamWriteTime, streamWriteSize / 1_000_000.0, TOTAL_ROWS / (streamWriteTime / 1000.0));
        System.out.printf("│ %-18s │ %12d │ %12s │ %12.0f │%n",
            "流式读", streamReadTime, "-", TOTAL_ROWS / (streamReadTime / 1000.0));
        System.out.printf("└────────────────────┴──────────────┴──────────────┴──────────────┘%n%n");
    }
    
    private CellData createCell(int row, int col) {
        switch (col) {
            case 0: return CellData.text("Name-" + row);
            case 1: return CellData.text("Cat-" + (row % 100));
            case 2: return CellData.text("R-" + (row % 10));
            case 3: return CellData.number(row);
            case 4: return CellData.number(row * 10L);
            case 5: return CellData.number(row * 100L);
            case 6: return CellData.number(row % 1000);
            case 7: return CellData.number(row * 1.5);
            case 8: return CellData.number(row * 2.345);
            case 9: return CellData.number(row / 7.0);
            default: return CellData.blank();
        }
    }
    
    private List<Integer> createBatch(int start, int count) {
        List<Integer> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            batch.add(start + i);
        }
        return batch;
    }
}