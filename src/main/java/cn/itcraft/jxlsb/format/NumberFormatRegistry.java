package cn.itcraft.jxlsb.format;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 数字格式注册表
 * 管理Excel内置格式(ID 0-163)和自定义格式(ID 164+)
 */
public final class NumberFormatRegistry {
    
    private static final Map<Integer, String> DATE_FORMATS;
    private static final int CUSTOM_FORMAT_START_ID = 164;
    
    private final Map<String, Integer> formatStringToId;
    private final Map<Integer, String> customFormats;
    private int nextCustomId;
    
    static {
        DATE_FORMATS = new HashMap<>();
        DATE_FORMATS.put(14, "mm-dd-yy");
        DATE_FORMATS.put(15, "d-mmm-yy");
        DATE_FORMATS.put(16, "d-mmm");
        DATE_FORMATS.put(17, "mmm-yy");
        DATE_FORMATS.put(18, "h:mm AM/PM");
        DATE_FORMATS.put(19, "h:mm:ss AM/PM");
        DATE_FORMATS.put(20, "h:mm");
        DATE_FORMATS.put(21, "h:mm:ss");
        DATE_FORMATS.put(22, "m/d/yy h:mm");
    }
    
    public NumberFormatRegistry() {
        this.formatStringToId = new HashMap<>();
        this.customFormats = new HashMap<>();
        this.nextCustomId = CUSTOM_FORMAT_START_ID;
        
        initializeBuiltInFormats();
    }
    
    private void initializeBuiltInFormats() {
        DATE_FORMATS.forEach((id, format) -> formatStringToId.put(format, id));
    }
    
    /**
     * 添加自定义数字格式
     * @param formatString 格式字符串
     * @return 格式ID
     */
    public int addFormat(String formatString) {
        Objects.requireNonNull(formatString, "formatString不能为null");
        
        Integer existingId = formatStringToId.get(formatString);
        if (existingId != null) {
            return existingId;
        }
        
        int newId = nextCustomId++;
        formatStringToId.put(formatString, newId);
        customFormats.put(newId, formatString);
        
        return newId;
    }
    
    /**
     * 获取格式ID
     * @param formatString 格式字符串
     * @return 格式ID,不存在返回-1
     */
    public int getFormatId(String formatString) {
        Objects.requireNonNull(formatString, "formatString不能为null");
        
        Integer id = formatStringToId.get(formatString);
        return id != null ? id : -1;
    }
    
    /**
     * 获取内置日期格式映射
     */
    public static Map<Integer, String> getDateFormats() {
        return DATE_FORMATS;
    }
    
    /**
     * 获取所有自定义格式
     */
    public Map<Integer, String> getCustomFormats() {
        return new HashMap<>(customFormats);
    }
}