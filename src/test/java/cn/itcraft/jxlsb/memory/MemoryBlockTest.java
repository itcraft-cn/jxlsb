package cn.itcraft.jxlsb.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MemoryBlock Interface Test")
class MemoryBlockTest {
    
    @Test
    @DisplayName("Should write and read byte")
    void shouldReadWriteByte() {
        MemoryBlock block = new MockMemoryBlock(64);
        
        block.putByte(0, (byte) 0x42);
        assertEquals((byte) 0x42, block.getByte(0));
        
        block.close();
    }
    
    @Test
    @DisplayName("Should write and read int in little-endian")
    void shouldReadWriteIntLittleEndian() {
        MemoryBlock block = new MockMemoryBlock(64);
        
        int testValue = 0x12345678;
        block.putInt(0, testValue);
        assertEquals(testValue, block.getInt(0));
        
        byte firstByte = block.getByte(0);
        assertEquals(0x78, firstByte);
        
        block.close();
    }
    
    @Test
    @DisplayName("Should write and read long")
    void shouldReadWriteLong() {
        MemoryBlock block = new MockMemoryBlock(64);
        
        long testValue = 0x123456789ABCDEF0L;
        block.putLong(0, testValue);
        assertEquals(testValue, block.getLong(0));
        
        block.close();
    }
    
    @Test
    @DisplayName("Should write and read double")
    void shouldReadWriteDouble() {
        MemoryBlock block = new MockMemoryBlock(64);
        
        double testValue = 3.14159265358979;
        block.putDouble(0, testValue);
        assertEquals(testValue, block.getDouble(0), 0.0000001);
        
        block.close();
    }
    
    @Test
    @DisplayName("Should write and read bytes array")
    void shouldReadWriteBytesArray() {
        MemoryBlock block = new MockMemoryBlock(64);
        
        byte[] src = {1, 2, 3, 4, 5};
        block.putBytes(0, src, 0, 5);
        
        byte[] dst = new byte[5];
        block.getBytes(0, dst, 0, 5);
        
        assertArrayEquals(src, dst);
        
        block.close();
    }
    
    @Test
    @DisplayName("Should return correct size")
    void shouldReturnCorrectSize() {
        MemoryBlock block = new MockMemoryBlock(1024);
        
        assertEquals(1024, block.size());
        
        block.close();
    }
    
    @Test
    @DisplayName("Should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
        MemoryBlock block = new MockMemoryBlock(64);
        
        assertInstanceOf(AutoCloseable.class, block);
        
        block.close();
    }
    
    @Test
    @DisplayName("Should throw after closed")
    void shouldThrowAfterClosed() {
        MemoryBlock block = new MockMemoryBlock(64);
        block.close();
        
        assertThrows(IllegalStateException.class, 
            () -> block.getByte(0));
    }
}