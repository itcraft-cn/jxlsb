package cn.itcraft.jxlsb.format;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class WorkbookWriter {
    
    private final List<SheetInfo> sheets = new ArrayList<>();
    
    public void addSheet(String name) {
        sheets.add(new SheetInfo(name, sheets.size() + 1));
    }
    
    public int getSheetCount() {
        return sheets.size();
    }
    
    public byte[] toBiff12Bytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // BEGIN_BOOK
        writeRecordHeader(dos, Biff12Constants.BEGIN_BOOK, 0);
        
        // FILE_VERSION
        writeRecordHeader(dos, Biff12Constants.FILE_VERSION, 12);
        writeIntLE(dos, 0x0006);
        writeIntLE(dos, 0);
        writeIntLE(dos, 0);
        
        // BEGIN_BUNDLE_SHS
        writeRecordHeader(dos, Biff12Constants.BEGIN_BUNDLE_SHS, 0);
        
        // Sheet records
        for (SheetInfo sheet : sheets) {
            byte[] nameBytes = sheet.name.getBytes(StandardCharsets.UTF_16LE);
            writeRecordHeader(dos, Biff12Constants.SHEET, 13 + nameBytes.length);
            writeIntLE(dos, sheet.sheetId);
            dos.writeByte(0); // state (visible)
            writeIntLE(dos, nameBytes.length / 2);
            dos.write(nameBytes);
        }
        
        // END_BUNDLE_SHS
        writeRecordHeader(dos, Biff12Constants.END_BUNDLE_SHS, 0);
        
        // END_BOOK
        writeRecordHeader(dos, Biff12Constants.END_BOOK, 0);
        
        dos.flush();
        return baos.toByteArray();
    }
    
    private void writeRecordHeader(DataOutputStream dos, int type, int size) throws IOException {
        dos.write(type & 0xFF);
        dos.write((type >> 8) & 0xFF);
        dos.write(size & 0xFF);
        dos.write((size >> 8) & 0xFF);
    }
    
    private void writeIntLE(DataOutputStream dos, int value) throws IOException {
        dos.write(value & 0xFF);
        dos.write((value >> 8) & 0xFF);
        dos.write((value >> 16) & 0xFF);
        dos.write((value >> 24) & 0xFF);
    }
    
    private static final class SheetInfo {
        final String name;
        final int sheetId;
        
        SheetInfo(String name, int sheetId) {
            this.name = name;
            this.sheetId = sheetId;
        }
    }
}