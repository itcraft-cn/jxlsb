package cn.itcraft.jxlsb.data;

/**
 * 单元格类型枚举
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public enum CellType {
    TEXT(0),
    NUMBER(1),
    DATE(2),
    BOOLEAN(3),
    ERROR(4),
    BLANK(5);
    
    private final int code;
    
    CellType(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    public static CellType fromCode(int code) {
        for (CellType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return BLANK;
    }
}