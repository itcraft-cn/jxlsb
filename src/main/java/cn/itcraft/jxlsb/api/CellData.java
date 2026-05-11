package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.data.CellType;

/**
 * Cell数据类
 *
 * <p>表示XLSB单元格的数据值。支持数字、文本、布尔、日期等类型。
 *
 * <p><b>格式与样式说明：</b>
 * <ul>
 *   <li>{@code formatCode} — 写入时使用，表示期望的数字格式字符串（如"#,##0.00"）。
 *       SheetWriter写入时负责将formatCode解析为最终的XF样式索引。</li>
 *   <li>{@code styleIndex} — 读取时使用，表示模板解析得到的原始XF样式索引。
 *       仅在模板填充场景有意义。</li>
 *   <li>两者使用场景互斥：通过工厂方法构造时formatCode有值（styleIndex=-1），
 *       通过模板解析构造时styleIndex有值（formatCode=null）。</li>
 * </ul>
 *
 * @author AI架构师
 * @since 1.0.0
 */
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
    
    public boolean isText() { return type == CellType.TEXT; }
    public boolean isNumber() { return type == CellType.NUMBER; }
    public boolean isBlank() { return type == CellType.BLANK; }
    public boolean isBoolean() { return type == CellType.BOOLEAN; }
    public boolean isDate() { return type == CellType.DATE; }
    
    public String getTextValue() { 
        return type == CellType.TEXT ? (String) value : null; 
    }
    
    public Double getNumberValue() {
        return type == CellType.NUMBER ? (Double) value : null;
    }
    
    /**
     * 获取数字值（原始类型，避免装箱开销）。
     *
     * @return 数字值（非NUMBER类型时返回0.0）
     */
    public double getNumberPrimitive() {
        return type == CellType.NUMBER ? (Double) value : 0.0;
    }
    
    public Boolean getBooleanValue() {
        return type == CellType.BOOLEAN ? (Boolean) value : null;
    }
    
    public Long getDateValue() {
        return type == CellType.DATE ? (Long) value : null;
    }
    
    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }
}