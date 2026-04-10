package cn.itcraft.jxlsb.io;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 堆外内存输入流
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class OffHeapInputStream implements AutoCloseable {
    
    private static final int DEFAULT_BLOCK_SIZE = 16 * 1024 * 1024;
    
    private final FileChannel fileChannel;
    private final OffHeapAllocator allocator;
    private final long fileSize;
    private long position = 0;
    
    public OffHeapInputStream(Path path) throws IOException {
        this.fileChannel = FileChannel.open(path, StandardOpenOption.READ);
        this.allocator = AllocatorFactory.createDefaultAllocator();
        this.fileSize = fileChannel.size();
    }
    
    public MemoryBlock readBlock() throws IOException {
        long remaining = fileSize - position;
        if (remaining <= 0) {
            return null;
        }
        
        int blockSize = (int) Math.min(DEFAULT_BLOCK_SIZE, remaining);
        MemoryBlock block = allocator.allocate(blockSize);
        
        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        int bytesRead = fileChannel.read(buffer, position);
        
        if (bytesRead > 0) {
            position += bytesRead;
            byte[] data = new byte[bytesRead];
            buffer.flip();
            buffer.get(data);
            block.putBytes(0, data, 0, bytesRead);
            return block;
        }
        
        block.close();
        return null;
    }
    
    public void streamProcess(BlockHandler handler) throws IOException {
        MemoryBlock block;
        while ((block = readBlock()) != null) {
            try {
                handler.handle(block);
            } catch (Exception e) {
                throw new IOException("Block handler exception", e);
            } finally {
                block.close();
            }
        }
    }
    
    public long getPosition() {
        return position;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}