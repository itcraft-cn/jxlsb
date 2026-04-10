package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.data.CellType;

/**
 * Cell数据结构
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class CellData {
    
    private final CellType type;
    private final Object value;
    
    private CellData(CellType type, Object value) {
        this.type = type;
        this.value = value;
    }
    
    public static CellData text(String text) {
        return new CellData(CellType.TEXT, text);
    }
    
    public static CellData number(double number) {
        return new CellData(CellType.NUMBER, number);
    }
    
    public static CellData date(long timestamp) {
        return new CellData(CellType.DATE, timestamp);
    }
    
    public static CellData bool(boolean bool) {
        return new CellData(CellType.BOOLEAN, bool);
    }
    
    public static CellData blank() {
        return new CellData(CellType.BLANK, null);
    }
    
    public CellType getType() {
        return type;
    }
    
    public Object getValue() {
        return value;
    }
}