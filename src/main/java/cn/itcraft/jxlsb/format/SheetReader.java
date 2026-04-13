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
    
    public static final class BatchCompleteException extends RuntimeException {
    }
    
    private final InputStream inputStream;
    private final SharedStringsTable sst;
    private final OffHeapAllocator allocator;
    private int currentRow = -1;
    private RowHandler currentHandler = null;
    
    public SheetReader(InputStream inputStream, SharedStringsTable sst) {
        this.inputStream = inputStream;
        this.sst = sst;
        this.allocator = AllocatorFactory.createDefaultAllocator();
    }
    
    private void handleBrtRowHdr(byte[] buffer, int offset, int size, RowHandler handler) {
        if (currentRow >= 0 && currentHandler != null) {
            currentHandler.onRowEnd(currentRow);
        }
        
        if (offset + 4 > buffer.length) return;
        currentRow = readIntLE(buffer, offset);
        currentHandler = handler;
        
        int numSpans = 0;
        if (offset + 17 <= buffer.length) {
            numSpans = readIntLE(buffer, offset + 13);
        }
        
        int lastCol = 0;
        if (numSpans > 0 && offset + 17 + numSpans * 8 <= buffer.length) {
            int lastSpanOffset = offset + 17 + (numSpans - 1) * 8;
            lastCol = readIntLE(buffer, lastSpanOffset + 4);
        }
        
        int columnCount = Math.max(1, lastCol + 1);
        handler.onRowStart(currentRow, columnCount);
    }
    
    private void handleBrtCellRk(byte[] buffer, int offset, int size, RowHandler handler) {
        int col = readIntLE(buffer, offset);
        int rkValue = readIntLE(buffer, offset + 8);
        
        double value = decodeRk(rkValue);
        handler.onCellNumber(currentRow, col, value);
    }
    
    private void handleBrtCellReal(byte[] buffer, int offset, int size, RowHandler handler) {
        int col = readIntLE(buffer, offset);
        double value = readDoubleLE(buffer, offset + 8);
        
        handler.onCellNumber(currentRow, col, value);
    }
    
    private void handleBrtCellSt(byte[] buffer, int offset, int size, RowHandler handler) {
        int col = readIntLE(buffer, offset);
        int sstIndex = readIntLE(buffer, offset + 8);
        
        String value = sst.getString(sstIndex);
        handler.onCellText(currentRow, col, value);
    }
    
    private void handleBrtCellBool(byte[] buffer, int offset, int size, RowHandler handler) {
        int col = readIntLE(buffer, offset);
        boolean value = buffer[offset + 8] != 0;
        
        handler.onCellBoolean(currentRow, col, value);
    }
    
    private void handleBrtCellBlank(byte[] buffer, int offset, int size, RowHandler handler) {
        int col = readIntLE(buffer, offset);
        
        handler.onCellBlank(currentRow, col);
    }
    
    private void handleBrtCellIsst(byte[] buffer, int offset, int size, RowHandler handler) {
        if (offset + 12 > buffer.length) return;
        
        int col = readIntLE(buffer, offset);
        int sstIndex = readIntLE(buffer, offset + 8);
        
        String value = sst.getString(sstIndex);
        handler.onCellText(currentRow, col, value != null ? value : "");
    }
    
    public void readRows(RowHandler handler) throws IOException {
        byte[] buffer = new byte[16384];
        int bytesRead;
        int offset = 0;
        int totalRead = 0;
        
        try {
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
            
            // 结束最后一行
            if (currentRow >= 0 && currentHandler != null) {
                currentHandler.onRowEnd(currentRow);
            }
        } catch (BatchCompleteException e) {
            // 正常结束，读取到目标行数
            // 结束最后一行
            if (currentRow >= 0 && currentHandler != null) {
                currentHandler.onRowEnd(currentRow);
            }
        }
    }
    
    private int processSheetBuffer(byte[] buffer, int length, RowHandler handler) {
        int pos = 0;
        
        while (pos + 2 <= length) {
            int recordType = VarIntReader.readVarInt(buffer, pos);
            int typeSize = VarIntReader.varIntSize(recordType);
            pos += typeSize;
            
            if (pos >= length) break;
            
            int recordSize = VarIntReader.readVarSize(buffer, pos);
            int sizeBytes = VarIntReader.varSizeSize(recordSize);
            pos += sizeBytes;
            
            if (recordSize < 0 || recordSize > length) {
                pos += Math.max(0, recordSize);
                continue;
            }
            
            if (recordSize > 0 && pos + recordSize > length) {
                return pos - typeSize - sizeBytes;
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
            } catch (BatchCompleteException e) {
                throw e;
            } catch (Exception e) {
                pos += recordSize;
            }
        }
        
        return pos;
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