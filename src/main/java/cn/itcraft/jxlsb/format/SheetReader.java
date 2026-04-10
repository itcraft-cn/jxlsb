package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.api.RowHandler;
import cn.itcraft.jxlsb.format.record.reader.*;
import cn.itcraft.jxlsb.memory.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Sheet读取器
 * 
 * <p>流式解析worksheet.bin，通过RowHandler回调处理行数据。
 * 
 * <p>支持的记录类型：
 * <ul>
 *   <li>BrtRowHdr - 行头</li>
 *   <li>BrtCellRk - RK编码数字</li>
 *   <li>BrtCellReal - Double数字</li>
 *   <li>BrtCellSt - SST字符串引用</li>
 *   <li>BrtCellBool - 布尔值</li>
 *   <li>BrtCellBlank - 空白单元格</li>
 *   <li>BrtCellIsst - 内联字符串</li>
 * </ul>
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class SheetReader implements AutoCloseable {
    
    private final InputStream inputStream;
    private final SharedStringsTable sst;
    private final OffHeapAllocator allocator;
    
    public SheetReader(InputStream inputStream, SharedStringsTable sst) {
        this.inputStream = inputStream;
        this.sst = sst;
        this.allocator = AllocatorFactory.createDefaultAllocator();
    }
    
    public void readRows(RowHandler handler) throws IOException {
        byte[] buffer = new byte[16384];
        int bytesRead;
        int offset = 0;
        int totalRead = 0;
        
        while ((bytesRead = inputStream.read(buffer, offset, buffer.length - offset)) != -1) {
            offset += bytesRead;
            totalRead += bytesRead;
            
            int processed = processSheetBuffer(buffer, offset, handler);
            
            if (processed > 0 && processed < offset) {
                byte[] remaining = new byte[offset - processed];
                System.arraycopy(buffer, processed, remaining, 0, remaining.length);
                System.arraycopy(remaining, 0, buffer, 0, remaining.length);
                offset = remaining.length;
            } else {
                offset = 0;
            }
        }
        
        if (offset > 0) {
            processSheetBuffer(buffer, offset, handler);
        }
    }
    
    private int processSheetBuffer(byte[] buffer, int length, RowHandler handler) {
        int pos = 0;
        
        while (pos + 8 <= length) {
            int recordType = readIntLE(buffer, pos);
            int recordSize = readIntLE(buffer, pos + 4);
            pos += 8;
            
            if (recordSize > 0 && pos + recordSize > length) {
                return pos - 8;
            }
            
            try {
                switch (recordType) {
                    case Biff12RecordType.BrtRowHdr:
                        handleBrtRowHdr(buffer, pos, recordSize, handler);
                        break;
                        
                    case Biff12RecordType.BrtCellRk:
                        handleBrtCellRk(buffer, pos, recordSize, handler);
                        break;
                        
                    case Biff12RecordType.BrtCellReal:
                        handleBrtCellReal(buffer, pos, recordSize, handler);
                        break;
                        
                    case Biff12RecordType.BrtCellSt:
                        handleBrtCellSt(buffer, pos, recordSize, handler);
                        break;
                        
                    case Biff12RecordType.BrtCellBool:
                        handleBrtCellBool(buffer, pos, recordSize, handler);
                        break;
                        
                    case Biff12RecordType.BrtCellBlank:
                        handleBrtCellBlank(buffer, pos, recordSize, handler);
                        break;
                        
                    case Biff12RecordType.BrtCellIsst:
                        handleBrtCellIsst(buffer, pos, recordSize, handler);
                        break;
                }
                
                pos += recordSize;
            } catch (Exception e) {
                pos += recordSize;
            }
        }
        
        return pos;
    }
    
    private void handleBrtRowHdr(byte[] buffer, int offset, int size, RowHandler handler) {
        int row = readIntLE(buffer, offset);
        int firstCol = readIntLE(buffer, offset + 4);
        int lastCol = readIntLE(buffer, offset + 8);
        
        handler.onRowStart(row, lastCol - firstCol + 1);
    }
    
    private void handleBrtCellRk(byte[] buffer, int offset, int size, RowHandler handler) {
        int row = readIntLE(buffer, offset);
        int col = readIntLE(buffer, offset + 4);
        int rkValue = readIntLE(buffer, offset + 12);
        
        double value = decodeRk(rkValue);
        handler.onCellNumber(row, col, value);
    }
    
    private void handleBrtCellReal(byte[] buffer, int offset, int size, RowHandler handler) {
        int row = readIntLE(buffer, offset);
        int col = readIntLE(buffer, offset + 4);
        double value = readDoubleLE(buffer, offset + 12);
        
        handler.onCellNumber(row, col, value);
    }
    
    private void handleBrtCellSt(byte[] buffer, int offset, int size, RowHandler handler) {
        int row = readIntLE(buffer, offset);
        int col = readIntLE(buffer, offset + 4);
        int sstIndex = readIntLE(buffer, offset + 12);
        
        String value = sst.getString(sstIndex);
        handler.onCellText(row, col, value);
    }
    
    private void handleBrtCellBool(byte[] buffer, int offset, int size, RowHandler handler) {
        int row = readIntLE(buffer, offset);
        int col = readIntLE(buffer, offset + 4);
        boolean value = buffer[offset + 12] != 0;
        
        handler.onCellBoolean(row, col, value);
    }
    
    private void handleBrtCellBlank(byte[] buffer, int offset, int size, RowHandler handler) {
        int row = readIntLE(buffer, offset);
        int col = readIntLE(buffer, offset + 4);
        
        handler.onCellBlank(row, col);
    }
    
    private void handleBrtCellIsst(byte[] buffer, int offset, int size, RowHandler handler) {
        int row = readIntLE(buffer, offset);
        int col = readIntLE(buffer, offset + 4);
        
        String value = readXLWideString(buffer, offset + 12);
        handler.onCellText(row, col, value);
    }
    
    private double decodeRk(int rkValue) {
        boolean isInt = (rkValue & 0x01) != 0;
        boolean div100 = (rkValue & 0x02) != 0;
        
        int valueBits = rkValue & 0xFFFFFFFC;
        
        double value;
        if (isInt) {
            value = valueBits >> 2;
        } else {
            long doubleBits = ((long)valueBits << 32);
            value = Double.longBitsToDouble(doubleBits);
        }
        
        if (div100) {
            value /= 100.0;
        }
        
        return value;
    }
    
    private String readXLWideString(byte[] buffer, int offset) {
        if (offset + 4 > buffer.length) {
            return "";
        }
        
        int length = readIntLE(buffer, offset);
        if (length == 0) {
            return "";
        }
        
        int strBytesLength = length * 2;
        if (offset + 4 + strBytesLength > buffer.length) {
            return "";
        }
        
        byte[] strBytes = new byte[strBytesLength];
        System.arraycopy(buffer, offset + 4, strBytes, 0, strBytesLength);
        
        return new String(strBytes, StandardCharsets.UTF_16LE);
    }
    
    private int readIntLE(byte[] buffer, int offset) {
        if (offset + 4 > buffer.length) {
            return 0;
        }
        
        return (buffer[offset] & 0xFF) | 
               ((buffer[offset + 1] & 0xFF) << 8) |
               ((buffer[offset + 2] & 0xFF) << 16) |
               ((buffer[offset + 3] & 0xFF) << 24);
    }
    
    private double readDoubleLE(byte[] buffer, int offset) {
        if (offset + 8 > buffer.length) {
            return 0.0;
        }
        
        long bits = ((long)buffer[offset] & 0xFF) |
                    (((long)buffer[offset + 1] & 0xFF) << 8) |
                    (((long)buffer[offset + 2] & 0xFF) << 16) |
                    (((long)buffer[offset + 3] & 0xFF) << 24) |
                    (((long)buffer[offset + 4] & 0xFF) << 32) |
                    (((long)buffer[offset + 5] & 0xFF) << 40) |
                    (((long)buffer[offset + 6] & 0xFF) << 48) |
                    (((long)buffer[offset + 7] & 0xFF) << 56);
        
        return Double.longBitsToDouble(bits);
    }
    
    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}