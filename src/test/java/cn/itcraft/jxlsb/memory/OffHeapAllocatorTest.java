package cn.itcraft.jxlsb.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OffHeapAllocator Test")
class OffHeapAllocatorTest {
    
    @Test
    @DisplayName("Should allocate MemoryBlock")
    void shouldAllocateBlock() {
        OffHeapAllocator allocator = new MockAllocator();
        
        MemoryBlock block = allocator.allocate(1024);
        
        assertNotNull(block);
        assertEquals(1024, block.size());
        
        block.close();
    }
    
    @Test
    @DisplayName("Should track total allocated memory")
    void shouldTrackTotalAllocated() {
        OffHeapAllocator allocator = new MockAllocator();
        
        long initial = allocator.getTotalAllocated();
        MemoryBlock block = allocator.allocate(1024);
        
        assertTrue(allocator.getTotalAllocated() > initial);
        
        block.close();
    }
    
    @Test
    @DisplayName("Should allocate from pool")
    void shouldAllocateFromPool() {
        OffHeapAllocator allocator = new MockAllocator();
        
        MemoryBlock block = allocator.allocateFromPool(1024);
        
        assertNotNull(block);
        
        block.close();
    }
    
    @Test
    @DisplayName("Should deallocate memory block")
    void shouldDeallocateBlock() {
        OffHeapAllocator allocator = new MockAllocator();
        
        MemoryBlock block = allocator.allocate(1024);
        long afterAllocate = allocator.getTotalAllocated();
        
        allocator.deallocate(block);
        
        assertTrue(allocator.getTotalAllocated() < afterAllocate);
    }
    
    @Test
    @DisplayName("Should return strategy name")
    void shouldReturnStrategyName() {
        OffHeapAllocator allocator = new MockAllocator();
        
        assertEquals("MockAllocator", allocator.getStrategyName());
    }
}