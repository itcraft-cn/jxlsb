package cn.itcraft.jxlsb.container;

/**
 * Sheet信息结构
 * 
 * <p>存储Workbook中Sheet的元信息。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class SheetInfo {
    
    private final String name;
    private final int index;
    private final String relId;
    
    public SheetInfo(String name, int index, String relId) {
        this.name = name;
        this.index = index;
        this.relId = relId;
    }
    
    public String getName() {
        return name;
    }
    
    public int getIndex() {
        return index;
    }
    
    public String getRelId() {
        return relId;
    }
    
    @Override
    public String toString() {
        return "SheetInfo{name='" + name + "', index=" + index + ", relId='" + relId + "'}";
    }
}