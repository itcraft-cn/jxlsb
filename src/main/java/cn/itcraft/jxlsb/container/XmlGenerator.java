package cn.itcraft.jxlsb.container;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XmlGenerator {
    
    private static final String THEME_TEMPLATE;
    private static final String CORE_TEMPLATE;
    private static final String APP_TEMPLATE;
    
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\$\\{timestamp\\}");
    private static final Pattern SHEET_COUNT_PATTERN = Pattern.compile("\\$\\{sheetCount\\}");
    private static final Pattern SHEET_NAMES_PATTERN = Pattern.compile("\\$\\{sheetNames\\}");
    
    static {
        THEME_TEMPLATE = loadTemplate("jxlsb/theme.xml.template");
        CORE_TEMPLATE = loadTemplate("jxlsb/core.xml.template");
        APP_TEMPLATE = loadTemplate("jxlsb/app.xml.template");
    }
    
    private static String loadTemplate(String name) {
        try (InputStream is = XmlGenerator.class.getClassLoader().getResourceAsStream(name)) {
            Objects.requireNonNull(is, "Template not found: " + name);
            byte[] bytes = new byte[is.available()];
            int read = is.read(bytes);
            return new String(bytes, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load template: " + name, e);
        }
    }
    
    public static byte[] generateThemeXml() {
        return THEME_TEMPLATE.getBytes(StandardCharsets.UTF_8);
    }
    
    public static byte[] generateCoreXml() {
        String timestamp = Instant.now().toString();
        Matcher m = TIMESTAMP_PATTERN.matcher(CORE_TEMPLATE);
        return m.replaceAll(timestamp).getBytes(StandardCharsets.UTF_8);
    }
    
    public static byte[] generateAppXml(int sheetCount) {
        StringBuilder sheetNames = new StringBuilder();
        for (int i = 0; i < sheetCount; i++) {
            sheetNames.append("<vt:lpstr>Sheet").append(i + 1).append("</vt:lpstr>");
        }
        String result = SHEET_NAMES_PATTERN.matcher(APP_TEMPLATE).replaceAll(sheetNames.toString());
        result = SHEET_COUNT_PATTERN.matcher(result).replaceAll(String.valueOf(sheetCount));
        return result.getBytes(StandardCharsets.UTF_8);
    }
    
    private XmlGenerator() {}
}