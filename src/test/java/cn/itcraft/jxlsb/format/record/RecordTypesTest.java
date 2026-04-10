package cn.itcraft.jxlsb.format.record;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import cn.itcraft.jxlsb.data.CellType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Record Types Test")
class RecordTypesTest {
    
    @Test
    @DisplayName("Should create and read CellRecord")
    void shouldCreateAndReadCellRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(1024);
        
        CellRecord record = CellRecord.create(10, 5, CellType.NUMBER, 123.456, block);
        
        assertEquals(CellRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(10, record.getRowIndex());
        assertEquals(5, record.getColIndex());
        assertEquals(CellType.NUMBER, record.getCellType());
        assertEquals(123.456, record.getNumberValue(), 0.001);
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create and read CellRecord with text")
    void shouldCreateAndReadCellRecordWithText() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(2048);
        
        CellRecord record = CellRecord.create(0, 0, CellType.TEXT, "Hello World", block);
        
        assertEquals(CellType.TEXT, record.getCellType());
        assertEquals("Hello World", record.getTextValue());
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create BeginSheetRecord")
    void shouldCreateBeginSheetRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        BeginSheetRecord record = BeginSheetRecord.create(3, block);
        
        assertEquals(BeginSheetRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(3, record.getSheetIndex());
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create BeginRowRecord")
    void shouldCreateBeginRowRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        BeginRowRecord record = BeginRowRecord.create(100, 50, block);
        
        assertEquals(BeginRowRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(100, record.getRowIndex());
        assertEquals(50, record.getColumnCount());
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create StringRecord")
    void shouldCreateStringRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(1024);
        
        StringRecord record = StringRecord.create("Test String Data", block);
        
        assertEquals(StringRecord.RECORD_TYPE, record.getRecordType());
        assertEquals("Test String Data", record.getString());
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create VersionRecord")
    void shouldCreateVersionRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        VersionRecord record = VersionRecord.create(VersionRecord.VERSION_2016, block);
        
        assertEquals(VersionRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(VersionRecord.VERSION_2016, record.getVersion());
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create EndSheetRecord")
    void shouldCreateEndSheetRecord() {
        EndSheetRecord record = new EndSheetRecord();
        
        assertEquals(EndSheetRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(0, record.getRecordSize());
    }
    
    @Test
    @DisplayName("Should create EndRowRecord")
    void shouldCreateEndRowRecord() {
        EndRowRecord record = new EndRowRecord();
        
        assertEquals(EndRowRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(0, record.getRecordSize());
    }
}