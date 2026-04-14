package cn.itcraft.jxlsb.container;

import cn.itcraft.jxlsb.format.WorkbookReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.*;

public final class XlsbContainerReader implements AutoCloseable {
    
    private static final Logger log = LoggerFactory.getLogger(XlsbContainerReader.class);
    
    private final ZipFile zipFile;
    private final Map<String, ZipEntry> entries;
    
    public XlsbContainerReader(Path path) throws IOException {
        this.zipFile = new ZipFile(path.toFile());
        this.entries = buildEntryMap();
    }
    
    private Map<String, ZipEntry> buildEntryMap() {
        Map<String, ZipEntry> map = new HashMap<>();
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement();
            map.put(entry.getName(), entry);
        }
        
        return map;
    }
    
    public InputStream getWorkbookStream() throws IOException {
        ZipEntry entry = entries.get("xl/workbook.bin");
        if (entry == null) {
            throw new IOException("workbook.bin not found in XLSB container");
        }
        return zipFile.getInputStream(entry);
    }
    
    public InputStream getSheetStream(int sheetIndex) throws IOException {
        String path = "xl/worksheets/sheet" + (sheetIndex + 1) + ".bin";
        ZipEntry entry = entries.get(path);
        if (entry == null) {
            throw new IOException("Sheet " + sheetIndex + " not found: " + path);
        }
        return zipFile.getInputStream(entry);
    }
    
    public InputStream getSharedStringsStream() throws IOException {
        ZipEntry entry = entries.get("xl/sharedStrings.bin");
        return entry != null ? zipFile.getInputStream(entry) : null;
    }
    
    public List<SheetInfo> getSheetInfos() throws IOException {
        WorkbookReader reader = new WorkbookReader(getWorkbookStream());
        try {
            return reader.parseSheetList();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                log.warn("Failed to close workbook reader: {}", e.getMessage());
            }
        }
    }
    
    public boolean hasSharedStrings() {
        return entries.containsKey("xl/sharedStrings.bin");
    }
    
    @Override
    public void close() throws IOException {
        zipFile.close();
    }
    
    public Set<String> getAllEntryNames() {
        return entries.keySet();
    }
    
    public InputStream getEntryStream(String name) throws IOException {
        ZipEntry entry = entries.get(name);
        if (entry == null) {
            return null;
        }
        return zipFile.getInputStream(entry);
    }
    
    public byte[] getEntryBytes(String name) throws IOException {
        ZipEntry entry = entries.get(name);
        if (entry == null) {
            return null;
        }
        try (InputStream in = zipFile.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }
    
    public InputStream getStylesStream() throws IOException {
        return getEntryStream("xl/styles.bin");
    }
    
    public InputStream getThemeStream() throws IOException {
        return getEntryStream("xl/theme/theme1.xml");
    }
    
    public InputStream getDrawingStream(int drawingIndex) throws IOException {
        return getEntryStream("xl/drawings/drawing" + (drawingIndex + 1) + ".xml");
    }
    
    public boolean hasStyles() {
        return entries.containsKey("xl/styles.bin");
    }
    
    public boolean hasTheme() {
        return entries.containsKey("xl/theme/theme1.xml");
    }
}