package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import cn.itcraft.jxlsb.format.record.UnknownRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

/**
 * XLSB记录解析器
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class RecordParser {
    
    private static final Logger log = LoggerFactory.getLogger(RecordParser.class);
    private static final int RECORD_HEADER_SIZE = 8;
    
    private final OffHeapAllocator allocator;
    
    public RecordParser() {
        this.allocator = AllocatorFactory.createDefaultAllocator();
    }
    
    public BiffRecord parse(MemoryBlock block, long offset) {
        int recordType = block.getInt(offset);
        int recordSize = block.getInt(offset + 4);
        
        if (recordSize < 0) {
            throw new IllegalArgumentException("Invalid record size: " + recordSize);
        }
        
        MemoryBlock recordData = null;
        if (recordSize > 0) {
            recordData = allocator.allocateFromPool(recordSize);
            byte[] data = new byte[recordSize];
            block.getBytes(offset + RECORD_HEADER_SIZE, data, 0, recordSize);
            recordData.putBytes(0, data, 0, recordSize);
        }
        
        return createRecord(recordType, recordSize, recordData);
    }
    
    public void parseStream(MemoryBlock block, RecordHandler handler) {
        long offset = 0;
        long blockSize = block.size();
        
        while (offset + RECORD_HEADER_SIZE <= blockSize) {
            try {
                BiffRecord record = parse(block, offset);
                handler.handle(record);
                offset += RECORD_HEADER_SIZE + record.getRecordSize();
            } catch (Exception e) {
                log.warn("Failed at offset {}", offset, e);
                break;
            }
        }
    }
    
    private BiffRecord createRecord(int type, int size, MemoryBlock data) {
        return new UnknownRecord(type, size, data);
    }
}