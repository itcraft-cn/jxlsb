package cn.itcraft.jxlsb.perf;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import cn.itcraft.jxlsb.memory.MemoryPool;
import cn.itcraft.jxlsb.memory.MemoryBlock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 完整内存泄漏测试
 * 
 * <p>测试循环写入、堆外内存释放、内存池回收。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@DisplayName("Memory Leak Complete Test")
class MemoryLeakCompleteTest {
    
    @Test
    @DisplayName("Test no leak on repeated write (100 iterations)")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testNoLeakOnRepeatedWrite() throws IOException, InterruptedException {
        Path path = Files.createTempFile("write-leak", ".xlsb");
        
        System.out.println("\n════════════════════════════════════════");
        System.out.println("Repeated Write Memory Leak Test");
        System.out.println("════════════════════════════════════════");
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = getUsedMemoryMB();
        
        for (int i = 0; i < 100; i++) {
            Files.deleteIfExists(path);
            path = Files.createTempFile("write-leak-" + i, ".xlsb");
            
            try (XlsbWriter writer = XlsbWriter.builder().path(path).build()) {
                writer.writeBatch("Sheet1",
                    (row, col) -> CellData.number(row * 100.0 + col),
                    10_000, 10);
            }
        }
        
        System.gc();
        Thread.sleep(100);
        
        long finalMemory = getUsedMemoryMB();
        long delta = finalMemory - initialMemory;
        
        System.out.println("  Iterations: 100");
        System.out.println("  Initial memory: " + initialMemory + " MB");
        System.out.println("  Final memory: " + finalMemory + " MB");
        System.out.println("  Memory delta: " + delta + " MB");
        
        assertTrue(delta < 10, 
            "Memory leak detected: delta = " + delta + " MB (should be < 10MB)");
        
        Files.deleteIfExists(path);
    }
    
    @Test
    @DisplayName("Test off-heap memory release")
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void testOffHeapMemoryRelease() throws IOException, InterruptedException {
        System.out.println("\n════════════════════════════════════════");
        System.out.println("Off-Heap Memory Release Test");
        System.out.println("════════════════════════════════════════");
        
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryPool pool = new MemoryPool(allocator);
        
        long initialPooled = pool.getTotalPooledSize();
        
        System.out.println("  Initial pooled size: " + initialPooled + " bytes");
        
        for (int i = 0; i < 50; i++) {
            MemoryBlock block = pool.acquire(4 * 1024);
            block.putInt(0, i);
            block.close();
        }
        
        long midPooled = pool.getTotalPooledSize();
        System.out.println("  After 50 allocations: " + midPooled + " bytes");
        
        assertTrue(midPooled > initialPooled, 
            "Memory pool should accumulate blocks");
        
        pool.close();
        
        long finalPooled = pool.getTotalPooledSize();
        System.out.println("  After close: " + finalPooled + " bytes");
        
        assertEquals(0, finalPooled, 
            "Memory pool should be empty after close");
    }
    
    @Test
    @DisplayName("Test memory pool reuse")
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void testMemoryPoolReuse() throws IOException {
        System.out.println("\n════════════════════════════════════════");
        System.out.println("Memory Pool Reuse Test");
        System.out.println("════════════════════════════════════════");
        
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryPool pool = new MemoryPool(allocator);
        
        long initialPooled = pool.getTotalPooledSize();
        
        for (int i = 0; i < 100; i++) {
            MemoryBlock block = pool.acquire(4 * 1024);
            block.putInt(0, i);
            block.close();
        }
        
        long afterFirst100 = pool.getTotalPooledSize();
        System.out.println("  After 100 allocations: " + afterFirst100 + " bytes");
        
        for (int i = 0; i < 50; i++) {
            MemoryBlock block = pool.acquire(4 * 1024);
            block.putInt(0, i);
            block.close();
        }
        
        long afterSecond50 = pool.getTotalPooledSize();
        System.out.println("  After reuse (50): " + afterSecond50 + " bytes");
        
        assertTrue(afterFirst100 > initialPooled,
            "Pool should accumulate memory blocks");
        
        pool.close();
    }
    
    @Test
    @DisplayName("Test allocator stress")
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void testAllocatorStress() throws IOException {
        System.out.println("\n════════════════════════════════════════");
        System.out.println("Allocator Stress Test");
        System.out.println("════════════════════════════════════════");
        
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        
        long initialMemory = getUsedMemoryMB();
        
        for (int iteration = 0; iteration < 10; iteration++) {
            for (int i = 0; i < 1000; i++) {
                MemoryBlock block = allocator.allocate(1024);
                block.putInt(0, i);
                block.close();
            }
        }
        
        long finalMemory = getUsedMemoryMB();
        long delta = finalMemory - initialMemory;
        
        System.out.println("  Total allocations: 10,000");
        System.out.println("  Memory delta: " + delta + " MB");
        
        assertTrue(delta < 20,
            "Allocator should not leak memory: delta = " + delta + " MB");
    }
    
    private long getUsedMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }
}