package cn.itcraft.jxlsb.memory.impl;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MemorySegmentAllocator Test (Java 23+)")
class MemorySegmentAllocatorTest {
    
    private OffHeapAllocator allocator;
    
    @AfterEach
    void cleanup() {
        allocator = null;
    }
    
    @Test
    @DisplayName("Should allocate MemorySegment-based MemoryBlock")
    void shouldAllocateMemorySegmentBlock() {
        allocator = new MemorySegmentAllocator();
        
        MemoryBlock block = allocator.allocate(1024);
        
        assertNotNull(block);
        assertEquals(1024, block.size());
        assertEquals("MemorySegment-ForeignAPI", allocator.getStrategyName());
        
        block.close();
    }
    
    @Test
    @DisplayName("Should write and read byte correctly")
    void shouldReadWriteByte() {
        allocator = new MemorySegmentAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        block.putByte(0, (byte) 0x42);
        byte value = block.getByte(0);
        
        assertEquals((byte) 0x42, value);
        
        block.close();
    }
    
    @Test
    @DisplayName("Should write and read int in little-endian")
    void shouldReadWriteIntLittleEndian() {
        allocator = new MemorySegmentAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        int testValue = 0x12345678;
        block.putInt(0, testValue);
        int value = block.getInt(0);
        
        assertEquals(testValue, value);
        
        byte firstByte = block.getByte(0);
        assertEquals(0x78, firstByte);
        
        block.close();
    }
    
    @Test
    @DisplayName("Should write and read long")
    void shouldReadWriteLong() {
        allocator = new MemorySegmentAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        long testValue = 0x123456789ABCDEF0L;
        block.putLong(0, testValue);
        assertEquals(testValue, block.getLong(0));
        
        block.close();
    }
    
    @Test
    @DisplayName("Should write and read double")
    void shouldReadWriteDouble() {
        allocator = new MemorySegmentAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        double testValue = 3.14159265358979;
        block.putDouble(0, testValue);
        assertEquals(testValue, block.getDouble(0), 0.0000001);
        
        block.close();
    }
    
    @Test
    @DisplayName("Should throw IndexOutOfBoundsException on out-of-bounds access")
    void shouldThrowOutOfBounds() {
        allocator = new MemorySegmentAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        assertThrows(IndexOutOfBoundsException.class, 
            () -> block.getInt(100));
        
        block.close();
    }
    
    @Test
    @DisplayName("Should throw after closed")
    void shouldThrowAfterClosed() {
        allocator = new MemorySegmentAllocator();
        MemoryBlock block = allocator.allocate(64);
        block.close();
        
        assertThrows(IllegalStateException.class, 
            () -> block.getByte(0));
    }
}