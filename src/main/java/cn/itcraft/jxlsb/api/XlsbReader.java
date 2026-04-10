package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import cn.itcraft.jxlsb.io.OffHeapInputStream;
import cn.itcraft.jxlsb.format.RecordParser;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Objects;

/**
 * XLSB文件读取器
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class XlsbReader implements AutoCloseable {
    
    private final OffHeapInputStream inputStream;
    private final RecordParser recordParser;
    
    private XlsbReader(Builder builder) throws IOException {
        Objects.requireNonNull(builder.path, "Path must not be null");
        this.inputStream = new OffHeapInputStream(builder.path);
        this.recordParser = new RecordParser();
    }
    
    public void readSheets(SheetHandler handler) throws IOException {
        inputStream.streamProcess(block -> {
            recordParser.parseStream(block, record -> {
            });
        });
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public void close() throws IOException {
        inputStream.close();
    }
    
    public static final class Builder {
        private Path path;
        
        public Builder path(Path path) {
            this.path = path;
            return this;
        }
        
        public XlsbReader build() throws IOException {
            return new XlsbReader(this);
        }
    }
}