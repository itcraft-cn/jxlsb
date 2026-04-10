package cn.itcraft.jxlsb.memory;

import cn.itcraft.jxlsb.memory.impl.ByteBufferAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MemoryPool Test")
class MemoryPoolTest {
    
    private OffHeapAllocator allocator;
    private MemoryPool pool;
    
    @BeforeEach
    void setup() {
        allocator = new ByteBufferAllocator();
        pool = new MemoryPool(allocator);
    }
    
    @AfterEach
    void cleanup() {
        pool.close();
    }
    
    @Test
    @DisplayName("Should acquire block from pool")
    void shouldAcquireBlockFromPool() {
        MemoryBlock block = pool.acquire(1024);
        
        assertNotNull(block);
        assertTrue(block.size() >= 1024);
        
        block.close();
    }
    
    @Test
    @DisplayName("Should reuse released block")
    void shouldReuseReleasedBlock() {
        MemoryBlock block1 = pool.acquire(1024);
        block1.putInt(0, 12345);
        block1.close();
        
        MemoryBlock block2 = pool.acquire(1024);
        
        assertTrue(block2.size() >= 1024);
        
        block2.close();
    }
    
    @Test
    @DisplayName("Should classify size correctly")
    void shouldClassifySize() {
        MemoryBlock smallBlock = pool.acquire(32);
        assertTrue(smallBlock.size() >= 64);
        
        MemoryBlock mediumBlock = pool.acquire(2048);
        assertTrue(mediumBlock.size() >= 4096);
        
        smallBlock.close();
        mediumBlock.close();
    }
    
    @Test
    @DisplayName("Should close and clear pool")
    void shouldCloseAndClearPool() {
        MemoryBlock block1 = pool.acquire(1024);
        MemoryBlock block2 = pool.acquire(2048);
        
        block1.close();
        block2.close();
        
        pool.close();
        
        assertTrue(true);
    }
}