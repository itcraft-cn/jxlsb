package cn.itcraft.jxlsb.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * BIFF12记录写入器
 * 严格按照MS-XLSB规范2.1.4节实现
 */
public final class Biff12Writer {
    
    private final ByteArrayOutputStream baos;
    
    public Biff12Writer() {
        this(64 * 1024);
    }
    
    public Biff12Writer(int estimatedSize) {
        this.baos = new ByteArrayOutputStream(estimatedSize);
    }
    
    /**
     * 写入记录头（记录类型 + 记录大小）
     * 使用变长编码
     */
    public void writeRecordHeader(int recordType, int recordSize) throws IOException {
        writeVarInt(recordType);
        writeVarSize(recordSize);
    }
    
    /**
     * 写入变长整数（用于记录类型）
     * 规则：高位置1表示需要下一字节，实际值用低7位
     */
    private void writeVarInt(int value) throws IOException {
        if (value >= 128) {
            // 需要2字节
            baos.write((value & 0x7F) | 0x80);  // 低7位 + 高位置1
            baos.write((value >> 7) & 0x7F);     // 高7位
        } else {
            // 只需1字节
            baos.write(value & 0x7F);
        }
    }
    
    /**
     * 写入变长大小（用于记录大小）
     * 规则：高位置1表示需要下一字节，最多4字节
     */
    private void writeVarSize(int value) throws IOException {
        if (value < 128) {
            baos.write(value);
        } else if (value < 16384) {
            baos.write((value & 0x7F) | 0x80);
            baos.write((value >> 7) & 0x7F);
        } else if (value < 2097152) {
            baos.write((value & 0x7F) | 0x80);
            baos.write(((value >> 7) & 0x7F) | 0x80);
            baos.write((value >> 14) & 0x7F);
        } else {
            baos.write((value & 0x7F) | 0x80);
            baos.write(((value >> 7) & 0x7F) | 0x80);
            baos.write(((value >> 14) & 0x7F) | 0x80);
            baos.write((value >> 21) & 0x7F);
        }
    }
    
    /**
     * 写入小端序int
     */
    public void writeIntLE(int value) throws IOException {
        baos.write(value & 0xFF);
        baos.write((value >> 8) & 0xFF);
        baos.write((value >> 16) & 0xFF);
        baos.write((value >> 24) & 0xFF);
    }
    
    /**
     * 写入小端序long
     */
    public void writeLongLE(long value) throws IOException {
        writeIntLE((int)(value & 0xFFFFFFFFL));
        writeIntLE((int)((value >> 32) & 0xFFFFFFFFL));
    }
    
    /**
     * 写入小端序double
     */
    public void writeDoubleLE(double value) throws IOException {
        writeLongLE(Double.doubleToLongBits(value));
    }
    
    /**
     * 写入字节数组
     */
    public void writeBytes(byte[] data) throws IOException {
        baos.write(data);
    }
    
    /**
     * 写入XLWideString（UTF-16LE字符串）
     * 格式：长度(4字节) + 字符数据(UTF-16LE)
     */
    public void writeXLWideString(String str) throws IOException {
        if (str == null || str.isEmpty()) {
            writeIntLE(0);
            return;
        }
        byte[] utf16le = str.getBytes(StandardCharsets.UTF_16LE);
        writeIntLE(str.length());  // 字符数，不是字节数
        baos.write(utf16le);
    }
    
    /**
     * 写入Cell结构（8字节）
     * 格式：column(4) + iStyleRef(3字节,24位) + flags(1字节)
     */
    public void writeCell(int column, int styleIndex) throws IOException {
        writeIntLE(column);
        // iStyleRef (24 bits) + fPhShow (1 bit) + reserved (7 bits) = 4 bytes
        int styleAndFlags = (styleIndex & 0xFFFFFF) << 8;  // style在高位24位
        writeIntLE(styleAndFlags);
    }
    
    /**
     * 写入空记录（只有头，没有数据）
     */
    public void writeEmptyRecord(int recordType) throws IOException {
        writeRecordHeader(recordType, 0);
    }
    
    public byte[] toByteArray() {
        return baos.toByteArray();
    }
    
    public int size() {
        return baos.size();
    }
    
    public void reset() {
        baos.reset();
    }
}