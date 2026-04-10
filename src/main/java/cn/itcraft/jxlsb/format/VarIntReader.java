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
 * @author AI架构师
 * @since 1.0.0
 */
public final class VarIntReader {
    
    /**
     * 读取变长整数（记录类型）
     */
    public static int readVarInt(byte[] buffer, int offset) {
        int b0 = buffer[offset] & 0xFF;
        
        if ((b0 & 0x80) == 0) {
            return b0;
        }
        
        int b1 = buffer[offset + 1] & 0x7F;
        return (b0 & 0x7F) | (b1 << 7);
    }
    
    /**
     * 读取变长大小（记录大小）
     */
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
    
    /**
     * 计算变长整数的字节数
     */
    public static int varIntSize(int value) {
        return value >= 128 ? 2 : 1;
    }
    
    /**
     * 计算变长大小的字节数
     */
    public static int varSizeSize(int value) {
        if (value < 128) return 1;
        if (value < 16384) return 2;
        if (value < 2097152) return 3;
        return 4;
    }
    
    private VarIntReader() {}
}