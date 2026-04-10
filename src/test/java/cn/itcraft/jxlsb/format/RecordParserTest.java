package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecordParser Test")
class RecordParserTest {
    
    @Test
    @DisplayName("Should parse record from memory block")
    void shouldParseRecordFromBlock() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        RecordParser parser = new RecordParser();
        
        MemoryBlock block = allocator.allocate(64);
        block.putInt(0, 0x0085);
        block.putInt(4, 8);
        byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        block.putBytes(8, data, 0, 8);
        
        BiffRecord record = parser.parse(block, 0);
        
        assertNotNull(record);
        assertEquals(0x0085, record.getRecordType());
        assertEquals(8, record.getRecordSize());
        
        record.close();
        block.close();
    }
    
    @Test
    @DisplayName("Should parse stream of records")
    void shouldParseStreamOfRecords() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        RecordParser parser = new RecordParser();
        
        MemoryBlock block = allocator.allocate(20);
        block.putInt(0, 0x0085);
        block.putInt(4, 4);
        block.putInt(8, 12345);
        
        block.putInt(12, 0x0086);
        block.putInt(16, 0);
        
        int[] count = {0};
        parser.parseStream(block, record -> {
            count[0]++;
            record.close();
        });
        
        assertEquals(2, count[0]);
        
        block.close();
    }
}