package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.container.SheetInfo;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Workbook读取器
 * 读取xl/workbook.bin获取Sheet列表
 */
public final class WorkbookReader implements AutoCloseable {
    
    private final InputStream inputStream;
    
    public WorkbookReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }
    
    public List<SheetInfo> parseSheetList() {
        return new ArrayList<>();
    }
    
    @Override
    public void close() throws Exception {
        inputStream.close();
    }
}