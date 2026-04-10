package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import cn.itcraft.jxlsb.format.record.UnknownRecord;
import cn.itcraft.jxlsb.format.record.CellRecord;
import cn.itcraft.jxlsb.format.record.BeginSheetRecord;
import cn.itcraft.jxlsb.format.record.BeginRowRecord;
import cn.itcraft.jxlsb.format.record.StringRecord;
import cn.itcraft.jxlsb.format.record.VersionRecord;
import cn.itcraft.jxlsb.format.record.IndexRecord;
import cn.itcraft.jxlsb.format.record.FormatRecord;
import cn.itcraft.jxlsb.format.record.XFRecord;
import cn.itcraft.jxlsb.format.record.FormulaRecord;
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
        switch (type) {
            case CellRecord.RECORD_TYPE:
                return new CellRecord(size, data);
            case BeginSheetRecord.RECORD_TYPE:
                return new BeginSheetRecord(size, data);
            case BeginRowRecord.RECORD_TYPE:
                return new BeginRowRecord(size, data);
            case StringRecord.RECORD_TYPE:
                return new StringRecord(size, data);
            case VersionRecord.RECORD_TYPE:
                return new VersionRecord(size, data);
            case IndexRecord.RECORD_TYPE:
                return new IndexRecord(size, data);
            case FormatRecord.RECORD_TYPE:
                return new FormatRecord(size, data);
            case XFRecord.RECORD_TYPE:
                return new XFRecord(size, data);
            case FormulaRecord.RECORD_TYPE:
                return new FormulaRecord(size, data);
            default:
                return new UnknownRecord(type, size, data);
        }
    }
}