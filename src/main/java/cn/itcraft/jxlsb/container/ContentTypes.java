package cn.itcraft.jxlsb.container;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ContentTypes {
    
    private final List<Override> overrides = new ArrayList<>();
    
    public void addOverride(String partName, String contentType) {
        overrides.add(new Override(partName, contentType));
    }
    
    public byte[] toXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n");
        
        sb.append("  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n");
        sb.append("  <Default Extension=\"bin\" ContentType=\"application/vnd.ms-excel.sheet.binary.macroEnabled.main\"/>\n");
        
        for (Override o : overrides) {
            sb.append("  <Override PartName=\"").append(o.partName)
              .append("\" ContentType=\"").append(o.contentType).append("\"/>\n");
        }
        
        sb.append("</Types>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    private static final class Override {
        final String partName;
        final String contentType;
        
        Override(String partName, String contentType) {
            this.partName = partName;
            this.contentType = contentType;
        }
    }
}