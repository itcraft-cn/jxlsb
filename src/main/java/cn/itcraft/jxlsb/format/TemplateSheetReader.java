package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.format.VarIntReader;
import java.io.InputStream;
import java.io.IOException;

public final class TemplateSheetReader {
    
    private final InputStream inputStream;
    private final SharedStringsTable sst;
    private int maxColumn = -1;
    private int maxRow = -1;
    private boolean analyzed = false;
    
    public TemplateSheetReader(InputStream inputStream, SharedStringsTable sst) {
        this.inputStream = inputStream;
        this.sst = sst;
    }
    
    public int getColumnCount() throws IOException {
        if (!analyzed) {
            analyzeSheet();
        }
        return Math.max(1, maxColumn + 1);
    }
    
    public int getRowCount() throws IOException {
        if (!analyzed) {
            analyzeSheet();
        }
        return maxRow + 1;
    }
    
    private void analyzeSheet() throws IOException {
        byte[] buffer = new byte[16384];
        int bytesRead;
        int offset = 0;
        
        while ((bytesRead = inputStream.read(buffer, offset, buffer.length - offset)) != -1) {
            offset += bytesRead;
            int processed = processBuffer(buffer, offset);
            
            if (processed > 0 && processed < offset) {
                byte[] remaining = new byte[offset - processed];
                System.arraycopy(buffer, processed, remaining, 0, remaining.length);
                System.arraycopy(remaining, 0, buffer, 0, remaining.length);
                offset = remaining.length;
            } else {
                offset = 0;
            }
        }
        
        analyzed = true;
        inputStream.close();
    }
    
    private int processBuffer(byte[] buffer, int length) {
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
                        handleBrtRowHdr(buffer, pos, recordSize);
                        break;
                    
                    case Biff12RecordType.BrtCellRk:
                    case Biff12RecordType.BrtCellReal:
                    case Biff12RecordType.BrtCellSt:
                    case Biff12RecordType.BrtCellBool:
                    case Biff12RecordType.BrtCellBlank:
                    case Biff12RecordType.BrtCellIsst:
                        handleCell(buffer, pos, recordSize);
                        break;
                }
                
                pos += recordSize;
            } catch (Exception e) {
                pos += recordSize;
            }
        }
        
        return pos;
    }
    
    private void handleBrtRowHdr(byte[] buffer, int offset, int size) {
        if (offset + 4 > buffer.length) return;
        int row = VarIntReader.readIntLE(buffer, offset);
        maxRow = Math.max(maxRow, row);
    }
    
    private void handleCell(byte[] buffer, int offset, int size) {
        if (offset + 4 > buffer.length) return;
        int col = VarIntReader.readIntLE(buffer, offset);
        maxColumn = Math.max(maxColumn, col);
    }
}