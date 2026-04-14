package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.format.VarIntReader;
import cn.itcraft.jxlsb.data.CellType;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SheetParser {
    
    private final InputStream inputStream;
    private final SharedStringsTable sst;
    private final List<CellInfo> cells = new ArrayList<>();
    private int maxRow = -1;
    private int maxCol = -1;
    
    public static final class CellInfo {
        public final int row;
        public final int col;
        public final CellData data;
        public final int styleIndex;
        
        CellInfo(int row, int col, CellData data, int styleIndex) {
            this.row = row;
            this.col = col;
            this.data = data;
            this.styleIndex = styleIndex;
        }
    }
    
    public SheetParser(InputStream inputStream, SharedStringsTable sst) {
        this.inputStream = inputStream;
        this.sst = sst;
    }
    
    public List<CellInfo> parse() throws IOException {
        byte[] buffer = new byte[16384];
        int bytesRead;
        int offset = 0;
        int currentRow = -1;
        
        while ((bytesRead = inputStream.read(buffer, offset, buffer.length - offset)) != -1) {
            offset += bytesRead;
            int processed = processBuffer(buffer, offset, currentRow);
            
            if (processed > 0 && processed < offset) {
                byte[] remaining = new byte[offset - processed];
                System.arraycopy(buffer, processed, remaining, 0, remaining.length);
                System.arraycopy(remaining, 0, buffer, 0, remaining.length);
                offset = remaining.length;
            } else {
                offset = 0;
            }
        }
        
        inputStream.close();
        return cells;
    }
    
    public int getMaxRow() { return maxRow; }
    public int getMaxCol() { return maxCol; }
    
    private int processBuffer(byte[] buffer, int length, int currentRow) {
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
                        currentRow = readIntLE(buffer, pos);
                        maxRow = Math.max(maxRow, currentRow);
                        break;
                    
                    case Biff12RecordType.BrtCellIsst:
                        handleCellIsst(buffer, pos, recordSize, currentRow);
                        break;
                    
                    case Biff12RecordType.BrtCellSt:
                        handleCellSt(buffer, pos, recordSize, currentRow);
                        break;
                    
                    case Biff12RecordType.BrtCellReal:
                        handleCellReal(buffer, pos, recordSize, currentRow);
                        break;
                    
                    case Biff12RecordType.BrtCellRk:
                        handleCellRk(buffer, pos, recordSize, currentRow);
                        break;
                    
                    case Biff12RecordType.BrtCellBool:
                        handleCellBool(buffer, pos, recordSize, currentRow);
                        break;
                    
                    case Biff12RecordType.BrtCellBlank:
                        int col = readIntLE(buffer, pos);
                        maxCol = Math.max(maxCol, col);
                        break;
                }
                
                pos += recordSize;
            } catch (Exception e) {
                pos += recordSize;
            }
        }
        
        return pos;
    }
    
    private void handleCellIsst(byte[] buffer, int offset, int size, int row) {
        int col = readIntLE(buffer, offset);
        int styleIndex = readStyleIndex(buffer, offset);
        int sstIdx = readIntLE(buffer, offset + 8);
        String text = sst.getString(sstIdx);
        if (text != null) {
            cells.add(new CellInfo(row, col, CellData.text(text), styleIndex));
            maxCol = Math.max(maxCol, col);
        }
    }
    
    private void handleCellSt(byte[] buffer, int offset, int size, int row) {
        int col = readIntLE(buffer, offset);
        int styleIndex = readStyleIndex(buffer, offset);
        int sstIdx = readIntLE(buffer, offset + 8);
        String text = sst.getString(sstIdx);
        if (text != null) {
            cells.add(new CellInfo(row, col, CellData.text(text), styleIndex));
            maxCol = Math.max(maxCol, col);
        }
    }
    
    private void handleCellReal(byte[] buffer, int offset, int size, int row) {
        int col = readIntLE(buffer, offset);
        int styleIndex = readStyleIndex(buffer, offset);
        double value = readDoubleLE(buffer, offset + 8);
        cells.add(new CellInfo(row, col, CellData.number(value), styleIndex));
        maxCol = Math.max(maxCol, col);
    }
    
    private void handleCellRk(byte[] buffer, int offset, int size, int row) {
        int col = readIntLE(buffer, offset);
        int styleIndex = readStyleIndex(buffer, offset);
        int rkValue = readIntLE(buffer, offset + 8);
        double value = decodeRk(rkValue);
        cells.add(new CellInfo(row, col, CellData.number(value), styleIndex));
        maxCol = Math.max(maxCol, col);
    }
    
    private void handleCellBool(byte[] buffer, int offset, int size, int row) {
        int col = readIntLE(buffer, offset);
        int styleIndex = readStyleIndex(buffer, offset);
        boolean value = buffer[offset + 8] != 0;
        cells.add(new CellInfo(row, col, CellData.bool(value), styleIndex));
        maxCol = Math.max(maxCol, col);
    }
    
    private int readStyleIndex(byte[] buffer, int offset) {
        if (offset + 7 > buffer.length) return 0;
        return (buffer[offset + 4] & 0xFF) |
               ((buffer[offset + 5] & 0xFF) << 8) |
               ((buffer[offset + 6] & 0xFF) << 16);
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
    
    private int readIntLE(byte[] buffer, int offset) {
        if (offset + 4 > buffer.length) return 0;
        return (buffer[offset] & 0xFF) | 
               ((buffer[offset + 1] & 0xFF) << 8) |
               ((buffer[offset + 2] & 0xFF) << 16) |
               ((buffer[offset + 3] & 0xFF) << 24);
    }
    
    private double readDoubleLE(byte[] buffer, int offset) {
        if (offset + 8 > buffer.length) return 0.0;
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
}