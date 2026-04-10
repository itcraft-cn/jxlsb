package cn.itcraft.jxlsb.memory;

import cn.itcraft.jxlsb.spi.AllocatorProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AllocatorFactory Test")
class AllocatorFactoryTest {
    
    @Test
    @DisplayName("Should create default allocator")
    void shouldCreateDefaultAllocator() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        
        assertNotNull(allocator);
        assertTrue(allocator.getStrategyName().contains("ByteBuffer") || 
                   allocator.getStrategyName().contains("MemorySegment"));
    }
    
    @Test
    @DisplayName("Should have default provider")
    void shouldHaveDefaultProvider() {
        AllocatorProvider provider = AllocatorFactory.getDefaultProvider();
        
        assertNotNull(provider);
        assertTrue(provider.getPriority() >= 0);
    }
    
    @Test
    @DisplayName("Default allocator should allocate blocks")
    void defaultAllocatorShouldAllocateBlocks() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        
        MemoryBlock block = allocator.allocate(1024);
        
        assertNotNull(block);
        assertEquals(1024, block.size());
        
        block.close();
    }
}