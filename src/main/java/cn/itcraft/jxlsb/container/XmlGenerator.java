package cn.itcraft.jxlsb.container;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public final class XmlGenerator {
    
    public static byte[] generateAppXml(int sheetCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" ");
        sb.append("xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">");
        sb.append("<Application>jxlsb</Application>");
        sb.append("<HeadingPairs><vt:vector size=\"2\" baseType=\"variant\">");
        sb.append("<vt:variant><vt:lpstr>Worksheets</vt:lpstr></vt:variant>");
        sb.append("<vt:variant><vt:i4>").append(sheetCount).append("</vt:i4></vt:variant>");
        sb.append("</vt:vector></HeadingPairs>");
        sb.append("<TitlesOfParts><vt:vector size=\"").append(sheetCount).append("\" baseType=\"lpstr\">");
        for (int i = 0; i < sheetCount; i++) {
            sb.append("<vt:lpstr>Sheet").append(i + 1).append("</vt:lpstr>");
        }
        sb.append("</vt:vector></TitlesOfParts></Properties>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    public static byte[] generateCoreXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" ");
        sb.append("xmlns:dc=\"http://purl.org/dc/elements/1.1/\" ");
        sb.append("xmlns:dcterms=\"http://purl.org/dc/terms/\" ");
        sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<dc:creator>jxlsb</dc:creator>");
        sb.append("<cp:lastModifiedBy>jxlsb</cp:lastModifiedBy>");
        sb.append("<dc:description>created by jxlsb</dc:description>");
        String now = Instant.now().toString();
        sb.append("<dcterms:created xsi:type=\"dcterms:W3CDTF\">").append(now).append("</dcterms:created>");
        sb.append("<dcterms:modified xsi:type=\"dcterms:W3CDTF\">").append(now).append("</dcterms:modified>");
        sb.append("</cp:coreProperties>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    public static byte[] generateThemeXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<a:theme xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" name=\"Office Theme\">");
        sb.append("<a:themeElements>");
        sb.append("<a:clrScheme name=\"Office\">");
        sb.append("<a:dk1><a:sysClr val=\"windowText\" lastClr=\"000000\"/></a:dk1>");
        sb.append("<a:lt1><a:sysClr val=\"window\" lastClr=\"FFFFFF\"/></a:lt1>");
        sb.append("<a:dk2><a:srgbClr val=\"1F497D\"/></a:dk2>");
        sb.append("<a:lt2><a:srgbClr val=\"EEECE1\"/></a:lt2>");
        sb.append("<a:accent1><a:srgbClr val=\"4F81BD\"/></a:accent1>");
        sb.append("<a:accent2><a:srgbClr val=\"C0504D\"/></a:accent2>");
        sb.append("<a:accent3><a:srgbClr val=\"9BBB59\"/></a:accent3>");
        sb.append("<a:accent4><a:srgbClr val=\"8064A2\"/></a:accent4>");
        sb.append("<a:accent5><a:srgbClr val=\"4BACC6\"/></a:accent5>");
        sb.append("<a:accent6><a:srgbClr val=\"F79646\"/></a:accent6>");
        sb.append("<a:hlink><a:srgbClr val=\"0000FF\"/></a:hlink>");
        sb.append("<a:folHlink><a:srgbClr val=\"800080\"/></a:folHlink>");
        sb.append("</a:clrScheme>");
        sb.append("<a:fontScheme name=\"Office\">");
        sb.append("<a:majorFont><a:latin typeface=\"Calibri\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:majorFont>");
        sb.append("<a:minorFont><a:latin typeface=\"Calibri\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:minorFont>");
        sb.append("</a:fontScheme>");
        sb.append("<a:fmtScheme name=\"Office\">");
        sb.append("<a:fillStyleLst><a:noFill/><a:solidFill><a:srgbClr val=\"FFFFFF\"/></a:solidFill></a:fillStyleLst>");
        sb.append("<a:lnStyleLst><a:ln><a:noFill/></a:ln></a:lnStyleLst>");
        sb.append("<a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst>");
        sb.append("<a:bgFillStyleLst><a:noFill/></a:bgFillStyleLst>");
        sb.append("</a:fmtScheme>");
        sb.append("</a:themeElements></a:theme>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    private XmlGenerator() {}
}