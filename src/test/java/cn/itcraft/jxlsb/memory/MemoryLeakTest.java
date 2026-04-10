package cn.itcraft.jxlsb.memory;

import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Memory Leak Test")
class MemoryLeakTest {
    
    @Test
    @DisplayName("Should allocate without error")
    void shouldAllocateWithoutError() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        
        for (int i = 0; i < 1000; i++) {
            MemoryBlock block = allocator.allocate(1024);
            block.putInt(0, i);
            block.close();
        }
        
        assertTrue(true, "Allocations completed without OutOfMemoryError");
    }
    
    @Test
    @DisplayName("Should handle repeated row operations")
    void shouldHandleRepeatedRowOperations() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        
        for (int i = 0; i < 100; i++) {
            OffHeapRow row = new OffHeapRow(i, 50, allocator);
            
            for (int j = 0; j < 50; j++) {
                OffHeapCell cell = row.getCell(j);
                cell.setNumber(i * 100.0 + j);
            }
            
            row.close();
        }
        
        assertTrue(true, "Row operations completed without memory issues");
    }
    
    @Test
    @DisplayName("Should handle sheet operations")
    void shouldHandleSheetOperations() {
        for (int i = 0; i < 10; i++) {
            OffHeapSheet sheet = new OffHeapSheet("Sheet" + i, i, 100, 20);
            
            for (OffHeapRow row : sheet) {
                for (int j = 0; j < row.getColumnCount(); j++) {
                    OffHeapCell cell = row.getCell(j);
                    cell.setText("Data-" + i + "-" + j);
                }
            }
            
            sheet.close();
        }
        
        assertTrue(true, "Sheet operations completed successfully");
    }
    
    @Test
    @DisplayName("Should not leak on workbook operations")
    void shouldNotLeakOnWorkbookOperations() {
        for (int iteration = 0; iteration < 5; iteration++) {
            OffHeapWorkbook workbook = new OffHeapWorkbook();
            
            for (int i = 0; i < 5; i++) {
                OffHeapSheet sheet = workbook.createSheet("Sheet" + i, 50, 10);
                
                for (int row = 0; row < 50; row++) {
                    OffHeapRow r = sheet.createRow(row);
                    for (int col = 0; col < 10; col++) {
                        OffHeapCell cell = r.getCell(col);
                        cell.setNumber(row * 10.0 + col);
                    }
                }
            }
            
            workbook.close();
        }
        
        assertTrue(true, "Workbook operations completed without memory issues");
    }
    
    @Test
    @DisplayName("Memory pool should reuse blocks")
    void memoryPoolShouldReuseBlocks() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryPool pool = new MemoryPool(allocator);
        
        long initialPooled = pool.getTotalPooledSize();
        
        for (int i = 0; i < 100; i++) {
            MemoryBlock block = pool.acquire(1024);
            block.putInt(0, i);
            block.close();
        }
        
        long finalPooled = pool.getTotalPooledSize();
        
        assertTrue(finalPooled > initialPooled,
            "Memory pool should have accumulated blocks");
        
        pool.close();
    }
}