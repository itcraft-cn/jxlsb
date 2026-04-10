package cn.itcraft.jxlsb.memory.impl;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ByteBufferAllocator Test")
class ByteBufferAllocatorTest {
    
    private OffHeapAllocator allocator;
    
    @AfterEach
    void cleanup() {
        allocator = null;
    }
    
    @Test
    @DisplayName("Should allocate ByteBuffer-based MemoryBlock")
    void shouldAllocateByteBufferBlock() {
        allocator = new ByteBufferAllocator();
        
        MemoryBlock block = allocator.allocate(1024);
        
        assertNotNull(block);
        assertEquals(1024, block.size());
        assertEquals("ByteBuffer-Direct", allocator.getStrategyName());
        
        block.close();
    }
    
    @Test
    @DisplayName("Should write and read byte correctly")
    void shouldReadWriteByte() {
        allocator = new ByteBufferAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        block.putByte(0, (byte) 0x42);
        byte value = block.getByte(0);
        
        assertEquals((byte) 0x42, value);
        
        block.close();
    }
    
    @Test
    @DisplayName("Should write and read int in little-endian")
    void shouldReadWriteIntLittleEndian() {
        allocator = new ByteBufferAllocator();
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
        allocator = new ByteBufferAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        long testValue = 0x123456789ABCDEF0L;
        block.putLong(0, testValue);
        assertEquals(testValue, block.getLong(0));
        
        block.close();
    }
    
    @Test
    @DisplayName("Should throw IndexOutOfBoundsException on out-of-bounds access")
    void shouldThrowOutOfBounds() {
        allocator = new ByteBufferAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        assertThrows(IndexOutOfBoundsException.class, 
            () -> block.getInt(100));
        
        block.close();
    }
    
    @Test
    @DisplayName("Should track total allocated memory")
    void shouldTrackTotalAllocated() {
        allocator = new ByteBufferAllocator();
        
        long initial = allocator.getTotalAllocated();
        MemoryBlock block1 = allocator.allocate(1024);
        MemoryBlock block2 = allocator.allocate(2048);
        
        assertTrue(allocator.getTotalAllocated() >= initial + 3072);
        
        block1.close();
        block2.close();
    }
    
    @Test
    @DisplayName("Should throw after closed")
    void shouldThrowAfterClosed() {
        allocator = new ByteBufferAllocator();
        MemoryBlock block = allocator.allocate(64);
        block.close();
        
        assertThrows(IllegalStateException.class, 
            () -> block.getByte(0));
    }
}