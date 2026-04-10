package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import cn.itcraft.jxlsb.io.OffHeapInputStream;
import cn.itcraft.jxlsb.format.RecordParser;
import cn.itcraft.jxlsb.format.XlsbFormatReader;
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
    private final XlsbFormatReader formatReader;
    
    private XlsbReader(Builder builder) throws IOException {
        Objects.requireNonNull(builder.path, "Path must not be null");
        this.inputStream = new OffHeapInputStream(builder.path);
        this.recordParser = new RecordParser();
        this.formatReader = new XlsbFormatReader();
    }
    
    public void readSheets(SheetHandler handler) throws IOException {
        inputStream.streamProcess(block -> {
            formatReader.readSheet(block, handler);
        });
    }
    
    public OffHeapSheet readSheet(int sheetIndex) throws IOException {
        SheetCollector collector = new SheetCollector(sheetIndex);
        readSheets(collector);
        return collector.getSheet();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public void close() throws IOException {
        inputStream.close();
    }
    
    private static final class SheetCollector implements SheetHandler {
        private final int targetIndex;
        private OffHeapSheet collected;
        
        SheetCollector(int targetIndex) {
            this.targetIndex = targetIndex;
        }
        
        @Override
        public void handle(OffHeapSheet sheet) throws Exception {
            if (sheet.getSheetIndex() == targetIndex) {
                this.collected = sheet;
            } else {
                sheet.close();
            }
        }
        
        OffHeapSheet getSheet() {
            return collected;
        }
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