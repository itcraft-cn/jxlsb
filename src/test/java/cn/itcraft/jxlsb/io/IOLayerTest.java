package cn.itcraft.jxlsb.io;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IO Layer Test")
class IOLayerTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("Should write and read memory block")
    void shouldWriteAndReadBlock() throws IOException {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        Path testFile = tempDir.resolve("test.bin");
        
        MemoryBlock writeBlock = allocator.allocate(64);
        writeBlock.putInt(0, 0x12345678);
        writeBlock.putDouble(4, 3.14159);
        
        try (OffHeapOutputStream output = new OffHeapOutputStream(testFile)) {
            output.writeBlock(writeBlock);
        }
        
        try (OffHeapInputStream input = new OffHeapInputStream(testFile)) {
            assertEquals(64, input.getFileSize());
            
            MemoryBlock readBlock = input.readBlock();
            assertNotNull(readBlock);
            assertEquals(0x12345678, readBlock.getInt(0));
            assertEquals(3.14159, readBlock.getDouble(4), 0.00001);
            
            readBlock.close();
        }
        
        writeBlock.close();
    }
    
    @Test
    @DisplayName("Should stream process blocks")
    void shouldStreamProcessBlocks() throws IOException {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        Path testFile = tempDir.resolve("stream.bin");
        
        try (OffHeapOutputStream output = new OffHeapOutputStream(testFile)) {
            for (int i = 0; i < 5; i++) {
                MemoryBlock block = allocator.allocate(64);
                block.putInt(0, i * 100);
                output.writeBlock(block);
                block.close();
            }
        }
        
        int[] count = {0};
        try (OffHeapInputStream input = new OffHeapInputStream(testFile)) {
            input.streamProcess(block -> {
                count[0]++;
            });
        }
        
        assertTrue(count[0] > 0);
    }
}