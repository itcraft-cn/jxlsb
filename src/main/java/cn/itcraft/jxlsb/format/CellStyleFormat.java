package cn.itcraft.jxlsb.format;

/**
 * 样式格式
 * 包含数字格式、字体、填充、边框索引
 */
public final class CellStyleFormat {
    
    private final int numFmtId;
    private final int fontId;
    private final int fillId;
    private final int borderId;
    
    private CellStyleFormat(int numFmtId, int fontId, int fillId, int borderId) {
        this.numFmtId = numFmtId;
        this.fontId = fontId;
        this.fillId = fillId;
        this.borderId = borderId;
    }
    
    public int getNumFmtId() {
        return numFmtId;
    }
    
    public int getFontId() {
        return fontId;
    }
    
    public int getFillId() {
        return fillId;
    }
    
    public int getBorderId() {
        return borderId;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private int numFmtId = 0;
        private int fontId = 0;
        private int fillId = 0;
        private int borderId = 0;
        
        public Builder numFmtId(int numFmtId) {
            this.numFmtId = numFmtId;
            return this;
        }
        
        public Builder fontId(int fontId) {
            this.fontId = fontId;
            return this;
        }
        
        public Builder fillId(int fillId) {
            this.fillId = fillId;
            return this;
        }
        
        public Builder borderId(int borderId) {
            this.borderId = borderId;
            return this;
        }
        
        public CellStyleFormat build() {
            return new CellStyleFormat(numFmtId, fontId, fillId, borderId);
        }
    }
}