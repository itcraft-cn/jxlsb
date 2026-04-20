package cn.itcraft.jxlsb.format;

/**
 * BIFF12变长编码工具类
 * 
 * <p>根据MS-XLSB规范2.1.4节，记录类型和大小使用变长编码：
 * <ul>
 *   <li>高位置1表示需要下一字节</li>
 *   <li>实际值存储在低7位</li>
 * </ul>
 * 
 * <p>提供公共方法用于读取BIFF12格式数据：
 * <ul>
 *   <li>readVarInt - 读取变长整数（记录类型）</li>
 *   <li>readVarSize - 读取变长大小（记录大小）</li>
 *   <li>readIntLE - 读取4字节小端整数</li>
 *   <li>readDoubleLE - 读取8字节小端双精度浮点数</li>
 * </ul>
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class VarIntReader {
    
    public static int readVarInt(byte[] buffer, int offset) {
        int b0 = buffer[offset] & 0xFF;
        
        if ((b0 & 0x80) == 0) {
            return b0;
        }
        
        int b1 = buffer[offset + 1] & 0x7F;
        return (b0 & 0x7F) | (b1 << 7);
    }
    
    public static int readVarSize(byte[] buffer, int offset) {
        int b0 = buffer[offset] & 0xFF;
        
        if ((b0 & 0x80) == 0) {
            return b0;
        }
        
        int b1 = buffer[offset + 1] & 0xFF;
        if ((b1 & 0x80) == 0) {
            return (b0 & 0x7F) | (b1 << 7);
        }
        
        int b2 = buffer[offset + 2] & 0xFF;
        if ((b2 & 0x80) == 0) {
            return (b0 & 0x7F) | ((b1 & 0x7F) << 7) | (b2 << 14);
        }
        
        int b3 = buffer[offset + 3] & 0x7F;
        return (b0 & 0x7F) | ((b1 & 0x7F) << 7) | ((b2 & 0x7F) << 14) | (b3 << 21);
    }
    
    public static int varIntSize(int value) {
        return value >= 128 ? 2 : 1;
    }
    
    public static int varSizeSize(int value) {
        if (value < 128) return 1;
        if (value < 16384) return 2;
        if (value < 2097152) return 3;
        return 4;
    }
    
    public static int readIntLE(byte[] buffer, int offset) {
        return (buffer[offset] & 0xFF) |
               ((buffer[offset + 1] & 0xFF) << 8) |
               ((buffer[offset + 2] & 0xFF) << 16) |
               ((buffer[offset + 3] & 0xFF) << 24);
    }
    
    public static double readDoubleLE(byte[] buffer, int offset) {
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
    
    private VarIntReader() {}
}