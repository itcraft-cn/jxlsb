package cn.itcraft.jxlsb.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 样式注册表
 * 管理单元格样式格式(CellStyleFormat)
 */
public final class CellStyleRegistry {
    
    private final List<CellStyleFormat> styles;
    private final NumberFormatRegistry formatRegistry;
    
    public CellStyleRegistry() {
        this.styles = new ArrayList<>();
        this.formatRegistry = new NumberFormatRegistry();
        
        initializeDefaultStyle();
    }
    
    private void initializeDefaultStyle() {
        styles.add(CellStyleFormat.builder()
            .numFmtId(0)
            .fontId(0)
            .fillId(0)
            .borderId(0)
            .build());
    }
    
    /**
     * 添加样式
     * @param numFmtId 数字格式ID
     * @param fontId 字体ID
     * @param fillId 填充ID
     * @param borderId 边框ID
     * @return 样式ID
     */
    public int addStyle(int numFmtId, int fontId, int fillId, int borderId) {
        CellStyleFormat style = CellStyleFormat.builder()
            .numFmtId(numFmtId)
            .fontId(fontId)
            .fillId(fillId)
            .borderId(borderId)
            .build();
        
        styles.add(style);
        return styles.size() - 1;
    }
    
    /**
     * 获取默认样式ID
     */
    public int getDefaultStyleId() {
        return 0;
    }
    
    /**
     * 获取日期样式ID
     * @param dateFormat 日期格式字符串
     * @return 样式ID
     */
    public int getDateStyleId(String dateFormat) {
        Objects.requireNonNull(dateFormat, "dateFormat不能为null");
        
        int formatId = formatRegistry.getFormatId(dateFormat);
        if (formatId == -1) {
            formatId = formatRegistry.addFormat(dateFormat);
        }
        
        for (int i = 0; i < styles.size(); i++) {
            CellStyleFormat style = styles.get(i);
            if (style.getNumFmtId() == formatId) {
                return i;
            }
        }
        
        return addStyle(formatId, 0, 0, 0);
    }
    
    /**
     * 获取所有样式
     */
    public List<CellStyleFormat> getStyles() {
        return new ArrayList<>(styles);
    }
    
    /**
     * 获取格式注册表
     */
    public NumberFormatRegistry getFormatRegistry() {
        return formatRegistry;
    }
}