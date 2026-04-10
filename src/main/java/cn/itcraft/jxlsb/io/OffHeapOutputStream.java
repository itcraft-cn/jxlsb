package cn.itcraft.jxlsb.io;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 堆外内存输出流
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class OffHeapOutputStream implements AutoCloseable {
    
    private final FileChannel fileChannel;
    private long position = 0;
    
    public OffHeapOutputStream(Path path) throws IOException {
        this.fileChannel = FileChannel.open(path, 
            StandardOpenOption.CREATE, 
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public void writeBlock(MemoryBlock block) throws IOException {
        long size = block.size();
        byte[] data = new byte[(int) size];
        block.getBytes(0, data, 0, (int) size);
        
        ByteBuffer buffer = ByteBuffer.wrap(data);
        fileChannel.write(buffer, position);
        position += size;
    }
    
    public void writeBlock(MemoryBlock block, long size) throws IOException {
        byte[] data = new byte[(int) size];
        block.getBytes(0, data, 0, (int) size);
        
        ByteBuffer buffer = ByteBuffer.wrap(data);
        fileChannel.write(buffer, position);
        position += size;
    }
    
    public void writeBlocks(MemoryBlock[] blocks) throws IOException {
        for (MemoryBlock block : blocks) {
            writeBlock(block);
        }
    }
    
    public long getPosition() {
        return position;
    }
    
    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}