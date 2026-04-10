package cn.itcraft.jxlsb.container;

import java.nio.charset.StandardCharsets;

public final class RelsGenerator {
    
    public static byte[] generateRootRels() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n");
        sb.append("  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.bin\"/>\n");
        sb.append("</Relationships>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    public static byte[] generateWorkbookRels(int sheetCount, boolean hasSharedStrings) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        
        int rId = 1;
        
        // Worksheets
        for (int i = 1; i <= sheetCount; i++) {
            sb.append("<Relationship Id=\"rId").append(rId++)
              .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\"")
              .append(" Target=\"worksheets/sheet").append(i).append(".bin\"/>");
        }
        
        // Theme
        sb.append("<Relationship Id=\"rId").append(rId++)
          .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme\"")
          .append(" Target=\"theme/theme1.xml\"/>");
        
        // SharedStrings (optional)
        if (hasSharedStrings) {
            sb.append("<Relationship Id=\"rId").append(rId++)
              .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\"")
              .append(" Target=\"sharedStrings.bin\"/>");
        }
        
        // Styles
        sb.append("<Relationship Id=\"rId").append(rId)
          .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\"")
          .append(" Target=\"styles.bin\"/>");
        
        sb.append("</Relationships>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    public static byte[] generateWorkbookRels(int sheetCount) {
        return generateWorkbookRels(sheetCount, true);
    }
    
    private RelsGenerator() {}
}