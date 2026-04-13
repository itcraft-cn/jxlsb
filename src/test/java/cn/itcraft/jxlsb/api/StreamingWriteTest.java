package cn.itcraft.jxlsb.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StreamingWriteTest {
    
    @TempDir
    Path tempDir;
    
    static class Order {
        long id;
        String customer;
        double amount;
        long timestamp;
        
        Order(long id, String customer, double amount, long timestamp) {
            this.id = id;
            this.customer = customer;
            this.amount = amount;
            this.timestamp = timestamp;
        }
    }
    
    @Test
    void testStreamingWriteWithList() throws IOException {
        Path file = tempDir.resolve("streaming-list.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.startSheet("Orders", 4);
            
            int offset = 0;
            int pageSize = 100;
            int totalPages = 10;
            
            for (int page = 0; page < totalPages; page++) {
                List<Order> batch = createBatch(offset, pageSize);
                writer.writeRows(batch, offset, (order, col) -> {
                    switch (col) {
                        case 0: return CellData.number(order.id);
                        case 1: return CellData.text(order.customer);
                        case 2: return CellData.number(order.amount);
                        case 3: return CellData.date(order.timestamp);
                        default: return CellData.blank();
                    }
                });
                offset += pageSize;
            }
            
            writer.endSheet();
        }
        
        assertTrue(file.toFile().exists());
        assertTrue(file.toFile().length() > 0);
        
        System.out.println("Streaming write test passed, file size: " + file.toFile().length());
    }
    
    @Test
    void testStreamingWriteWithArray() throws IOException {
        Path file = tempDir.resolve("streaming-array.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.startSheet("Products", 3);
            
            int offset = 0;
            while (offset < 500) {
                Order[] batch = createArrayBatch(offset, 50);
                writer.writeRows(batch, offset, (order, col) -> {
                    switch (col) {
                        case 0: return CellData.number(order.id);
                        case 1: return CellData.text(order.customer);
                        case 2: return CellData.number(order.amount);
                        default: return CellData.blank();
                    }
                });
                offset += 50;
            }
            
            writer.endSheet();
        }
        
        assertTrue(file.toFile().exists());
        assertTrue(file.toFile().length() > 0);
    }
    
    @Test
    void testMixedMode() throws IOException {
        Path file = tempDir.resolve("mixed.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            // Sheet1: 使用 writeBatch（一次性）
            writer.writeBatch("Summary", (row, col) -> CellData.number(row * col), 100, 5);
            
            // Sheet2: 使用流式追加
            writer.startSheet("Details", 3);
            List<Order> batch1 = createBatch(0, 200);
            writer.writeRows(batch1, 0, (order, col) -> {
                switch (col) {
                    case 0: return CellData.number(order.id);
                    case 1: return CellData.text(order.customer);
                    case 2: return CellData.number(order.amount);
                    default: return CellData.blank();
                }
            });
            writer.endSheet();
            
            // Sheet3: 再使用 writeBatch
            writer.writeBatch("Footer", (row, col) -> CellData.text("End-" + row), 10, 2);
        }
        
        assertTrue(file.toFile().exists());
        assertTrue(file.toFile().length() > 0);
    }
    
    @Test
    void testStateExceptionWithoutStartSheet() throws IOException {
        Path file = tempDir.resolve("error.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            List<Order> batch = createBatch(0, 10);
            
            assertThrows(IllegalStateException.class, () -> {
                writer.writeRows(batch, 0, (order, col) -> CellData.number(order.id));
            });
        }
    }
    
    @Test
    void testStateExceptionWithoutEndSheet() throws IOException {
        Path file = tempDir.resolve("error2.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.startSheet("Test", 3);
            
            assertThrows(IllegalStateException.class, () -> {
                writer.startSheet("Test2", 3);
            });
            
            writer.endSheet();
        }
    }
    
    private List<Order> createBatch(int startId, int count) {
        List<Order> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            batch.add(new Order(startId + i, "Customer-" + (startId + i), 
                (startId + i) * 100.5, System.currentTimeMillis()));
        }
        return batch;
    }
    
    private Order[] createArrayBatch(int startId, int count) {
        Order[] batch = new Order[count];
        for (int i = 0; i < count; i++) {
            batch[i] = new Order(startId + i, "Product-" + (startId + i), 
                (startId + i) * 50.0, System.currentTimeMillis());
        }
        return batch;
    }
}