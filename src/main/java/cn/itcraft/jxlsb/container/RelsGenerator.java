package cn.itcraft.jxlsb.container;

import java.nio.charset.StandardCharsets;

public final class RelsGenerator {
    
    public static byte[] generateRootRels() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        sb.append("<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.bin\"/>");
        sb.append("<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>");
        sb.append("<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/>");
        sb.append("</Relationships>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    public static byte[] generateWorkbookRels(int sheetCount, boolean hasSharedStrings) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        
        int rId = 1;
        
        for (int i = 1; i <= sheetCount; i++) {
            sb.append("<Relationship Id=\"rId").append(rId++)
              .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\"")
              .append(" Target=\"worksheets/sheet").append(i).append(".bin\"/>");
        }
        
        sb.append("<Relationship Id=\"rId").append(rId++)
          .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme\"")
          .append(" Target=\"theme/theme1.xml\"/>");
        
        if (hasSharedStrings) {
            sb.append("<Relationship Id=\"rId").append(rId++)
              .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\"")
              .append(" Target=\"sharedStrings.bin\"/>");
        }
        
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