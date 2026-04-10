package cn.itcraft.jxlsb.data;

import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Data Structure Test")
class DataStructureTest {
    
    @Test
    @DisplayName("Should create and read text cell")
    void shouldCreateAndReadTextCell() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        
        OffHeapRow row = new OffHeapRow(0, 5, allocator);
        OffHeapCell cell = row.getCell(0);
        
        cell.setText("Hello World");
        assertEquals("Hello World", cell.getText());
        
        row.close();
    }
    
    @Test
    @DisplayName("Should create and read number cell")
    void shouldCreateAndReadNumberCell() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        
        OffHeapRow row = new OffHeapRow(0, 5, allocator);
        OffHeapCell cell = row.getCell(0);
        
        cell.setNumber(3.14159);
        assertEquals(3.14159, cell.getNumber(), 0.00001);
        
        row.close();
    }
    
    @Test
    @DisplayName("Should create and read boolean cell")
    void shouldCreateAndReadBooleanCell() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        
        OffHeapRow row = new OffHeapRow(0, 5, allocator);
        OffHeapCell cell = row.getCell(0);
        
        cell.setBoolean(true);
        assertTrue(cell.getBoolean());
        
        row.close();
    }
    
    @Test
    @DisplayName("Should create sheet with rows")
    void shouldCreateSheetWithRows() {
        OffHeapSheet sheet = new OffHeapSheet("TestSheet", 0, 100, 10);
        
        assertEquals("TestSheet", sheet.getSheetName());
        assertEquals(100, sheet.getRowCount());
        assertEquals(10, sheet.getColumnCount());
        
        sheet.close();
    }
    
    @Test
    @DisplayName("Should iterate rows")
    void shouldIterateRows() {
        OffHeapSheet sheet = new OffHeapSheet("TestSheet", 0, 10, 5);
        
        int count = 0;
        for (OffHeapRow row : sheet) {
            count++;
        }
        
        assertEquals(10, count);
        
        sheet.close();
    }
    
    @Test
    @DisplayName("Should create workbook with sheets")
    void shouldCreateWorkbookWithSheets() {
        OffHeapWorkbook workbook = new OffHeapWorkbook();
        
        workbook.createSheet("Sheet1", 100, 10);
        workbook.createSheet("Sheet2", 200, 20);
        
        assertEquals(2, workbook.getSheetCount());
        assertNotNull(workbook.getSheet(0));
        
        workbook.close();
    }
}