package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.data.CellType;

public final class CellData {
    
    private final CellType type;
    private final Object value;
    private final String formatCode;
    private final int styleIndex;
    
    private CellData(CellType type, Object value, String formatCode, int styleIndex) {
        this.type = type;
        this.value = value;
        this.formatCode = formatCode;
        this.styleIndex = styleIndex;
    }
    
    private CellData(CellType type, Object value) {
        this(type, value, null, 0);
    }
    
    public static CellData text(String text) {
        return new CellData(CellType.TEXT, text);
    }
    
    public static CellData number(double number) {
        return new CellData(CellType.NUMBER, number);
    }
    
    public static CellData number(double number, String formatCode) {
        return new CellData(CellType.NUMBER, number, formatCode, -1);
    }
    
    public static CellData date(long timestamp) {
        return new CellData(CellType.DATE, timestamp);
    }
    
    public static CellData date(long timestamp, String formatCode) {
        return new CellData(CellType.DATE, timestamp, formatCode, -1);
    }
    
    public static CellData bool(boolean bool) {
        return new CellData(CellType.BOOLEAN, bool);
    }
    
    public static CellData blank() {
        return new CellData(CellType.BLANK, null);
    }
    
    public static CellData percentage(double value) {
        return percentage(value, 2);
    }
    
    public static CellData percentage(double value, int decimals) {
        String formatCode = decimals == 0 ? "0%" : "0." + repeat("0", decimals) + "%";
        return new CellData(CellType.NUMBER, value, formatCode, -1);
    }
    
    public static CellData time(long timestamp) {
        return new CellData(CellType.DATE, timestamp, "h:mm:ss", -1);
    }
    
    public static CellData time(long timestamp, String formatCode) {
        return new CellData(CellType.DATE, timestamp, formatCode, -1);
    }
    
    public static CellData numberNegativeRed(double value) {
        return new CellData(CellType.NUMBER, value, "#,##0.00;[Red]-#,##0.00", -1);
    }
    
    public static CellData numberWithComma(double value) {
        return new CellData(CellType.NUMBER, value, "#,##0.00", -1);
    }
    
    public static CellData numberWithComma(double value, int decimals) {
        String formatCode = decimals == 0 ? "#,##0" : "#,##0." + repeat("0", decimals);
        return new CellData(CellType.NUMBER, value, formatCode, -1);
    }
    
    public static CellData currency(double value) {
        return new CellData(CellType.NUMBER, value, "￥#,##0.00", -1);
    }
    
    public static CellData currency(double value, String symbol) {
        return new CellData(CellType.NUMBER, value, symbol + "#,##0.00", -1);
    }
    
    public CellType getType() { return type; }
    public Object getValue() { return value; }
    public String getFormatCode() { return formatCode; }
    public int getStyleIndex() { return styleIndex; }
    public boolean hasFormatCode() { return formatCode != null; }
    
    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }
}