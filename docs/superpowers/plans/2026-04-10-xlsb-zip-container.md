# XLSB ZIP Container Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement complete XLSB file format with ZIP container, correct BIFF12 records, and shared string table to produce valid XLSB files that Excel can open.

**Architecture:** 
- XLSB file is a ZIP container containing binary BIFF12 records
- Main components: workbook.bin, sheet.bin, sharedStrings.bin, and XML metadata
- Use Java ZipOutputStream for container, refine BIFF12 record writing

**Tech Stack:** Java SDK, SLF4J, JUnit 5

---

## File Structure

**New files to create:**
```
src/main/java/cn/itcraft/jxlsb/
├── container/
│   ├── XlsbContainer.java          # ZIP container manager
│   ├── ContentTypes.java           # [Content_Types].xml generator
│   └── RelsGenerator.java          # _rels files generator
├── format/
│   ├── Biff12Constants.java        # BIFF12 record type constants
│   ├── SharedStringsTable.java     # Shared strings table
│   └── WorkbookWriter.java         # xl/workbook.bin writer
```

**Files to modify:**
- `XlsbWriter.java` - Use XlsbContainer instead of direct file output
- `XlsbFormatWriter.java` - Correct BIFF12 record format
- `RecordWriter.java` - Proper BIFF12 record header

---

## Phase 1: ZIP Container Foundation

### Task 1.1: Create XlsbContainer

**Files:**
- Create: `src/main/java/cn/itcraft/jxlsb/container/XlsbContainer.java`
- Test: `src/test/java/cn/itcraft/jxlsb/container/XlsbContainerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jxlsb.container;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class XlsbContainerTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void createContainerProducesValidZip() throws Exception {
        Path xlsbFile = tempDir.resolve("test.xlsb");
        
        try (XlsbContainer container = XlsbContainer.create(xlsbFile)) {
            container.addEntry("test.txt", "Hello".getBytes());
        }
        
        assertTrue(Files.exists(xlsbFile));
        assertTrue(Files.size(xlsbFile) > 0);
        
        try (ZipFile zf = new ZipFile(xlsbFile.toFile())) {
            assertNotNull(zf.getEntry("test.txt"));
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvnd test -Dtest=XlsbContainerTest -q`
Expected: FAIL - class not found

- [ ] **Step 3: Implement XlsbContainer**

```java
package cn.itcraft.jxlsb.container;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class XlsbContainer implements AutoCloseable {
    
    private final ZipOutputStream zipOut;
    private final ByteArrayOutputStream buffer;
    
    private XlsbContainer(ZipOutputStream zipOut, ByteArrayOutputStream buffer) {
        this.zipOut = zipOut;
        this.buffer = buffer;
    }
    
    public static XlsbContainer create(Path path) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(64 * 1024);
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(path.toFile()));
        return new XlsbContainer(zipOut, buffer);
    }
    
    public void addEntry(String name, byte[] data) throws IOException {
        zipOut.putNextEntry(new ZipEntry(name));
        zipOut.write(data);
        zipOut.closeEntry();
    }
    
    public void addEntry(String name, byte[] data, int offset, int length) throws IOException {
        zipOut.putNextEntry(new ZipEntry(name));
        zipOut.write(data, offset, length);
        zipOut.closeEntry();
    }
    
    @Override
    public void close() throws IOException {
        zipOut.finish();
        zipOut.close();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvnd test -Dtest=XlsbContainerTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jxlsb/container/XlsbContainer.java src/test/java/cn/itcraft/jxlsb/container/XlsbContainerTest.java
git commit -m "feat(container): add XlsbContainer for ZIP file management"
```

---

### Task 1.2: Create ContentTypes Generator

**Files:**
- Create: `src/main/java/cn/itcraft/jxlsb/container/ContentTypes.java`
- Test: `src/test/java/cn/itcraft/jxlsb/container/ContentTypesTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jxlsb.container;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContentTypesTest {
    
    @Test
    void generatesValidContentTypesXml() {
        ContentTypes ct = new ContentTypes();
        ct.addOverride("/xl/workbook.bin", "application/vnd.ms-excel.workbook");
        ct.addOverride("/xl/worksheets/sheet1.bin", "application/vnd.ms-excel.worksheet");
        
        byte[] xml = ct.toXml();
        String content = new String(xml);
        
        assertTrue(content.contains("<?xml version"));
        assertTrue(content.contains("Types"));
        assertTrue(content.contains("Override"));
        assertTrue(content.contains("/xl/workbook.bin"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvnd test -Dtest=ContentTypesTest -q`
Expected: FAIL - class not found

- [ ] **Step 3: Implement ContentTypes**

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvnd test -Dtest=ContentTypesTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jxlsb/container/ContentTypes.java src/test/java/cn/itcraft/jxlsb/container/ContentTypesTest.java
git commit -m "feat(container): add ContentTypes XML generator"
```

---

### Task 1.3: Create RelsGenerator

**Files:**
- Create: `src/main/java/cn/itcraft/jxlsb/container/RelsGenerator.java`
- Test: `src/test/java/cn/itcraft/jxlsb/container/RelsGeneratorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jxlsb.container;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelsGeneratorTest {
    
    @Test
    void generatesRootRels() {
        byte[] xml = RelsGenerator.generateRootRels();
        String content = new String(xml);
        
        assertTrue(content.contains("<?xml version"));
        assertTrue(content.contains("Relationships"));
        assertTrue(content.contains("rId1"));
        assertTrue(content.contains("workbook.bin"));
    }
    
    @Test
    void generatesWorkbookRels() {
        byte[] xml = RelsGenerator.generateWorkbookRels(1);
        String content = new String(xml);
        
        assertTrue(content.contains("sheet1.bin"));
        assertTrue(content.contains("rId1"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvnd test -Dtest=RelsGeneratorTest -q`
Expected: FAIL - class not found

- [ ] **Step 3: Implement RelsGenerator**

```java
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
    
    public static byte[] generateWorkbookRels(int sheetCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n");
        
        for (int i = 1; i <= sheetCount; i++) {
            sb.append("  <Relationship Id=\"rId").append(i)
              .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\"")
              .append(" Target=\"worksheets/sheet").append(i).append(".bin\"/>\n");
        }
        
        sb.append("  <Relationship Id=\"rId").append(sheetCount + 1)
          .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\"")
          .append(" Target=\"sharedStrings.bin\"/>\n");
        
        sb.append("</Relationships>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    private RelsGenerator() {}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvnd test -Dtest=RelsGeneratorTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jxlsb/container/RelsGenerator.java src/test/java/cn/itcraft/jxlsb/container/RelsGeneratorTest.java
git commit -m "feat(container): add RelsGenerator for relationship files"
```

---

## Phase 2: BIFF12 Record Format

### Task 2.1: Define BIFF12 Constants

**Files:**
- Create: `src/main/java/cn/itcraft/jxlsb/format/Biff12Constants.java`

- [ ] **Step 1: Create BIFF12 record type constants**

```java
package cn.itcraft.jxlsb.format;

public final class Biff12Constants {
    
    // File structure records
    public static final int FILE_VERSION = 0x0808;
    public static final int END_FILE_VERSION = 0x0809;
    
    // Workbook records
    public static final int BEGIN_BOOK = 0x0083;
    public static final int END_BOOK = 0x0084;
    public static final int BEGIN_BUNDLE_SHS = 0x0085;
    public static final int END_BUNDLE_SHS = 0x0086;
    public static final int BEGIN_SHEET = 0x0089;
    public static final int END_SHEET = 0x008A;
    
    // Worksheet records
    public static final int WS_DIMENSION = 0x0094;
    public static final int BEGIN_WS_VIEWS = 0x009D;
    public static final int END_WS_VIEWS = 0x009E;
    public static final int BEGIN_COL_INFOS = 0x00A0;
    public static final int END_COL_INFOS = 0x00A1;
    
    // Row/Cell records
    public static final int BEGIN_ROW = 0x0001;
    public static final int END_ROW = 0x0002;
    public static final int CELL_BLANK = 0x0003;
    public static final int CELL_BOOL = 0x0004;
    public static final int CELL_ERROR = 0x0005;
    public static final int CELL_FLOAT = 0x0006;
    public static final int CELL_STRING = 0x0007;
    public static final int CELL_RICH_STRING = 0x0008;
    
    // Shared strings
    public static final int BEGIN_SST = 0x009F;
    public static final int END_SST = 0x00A0;
    public static final int SST_ITEM = 0x0013;
    
    // Book views
    public static final int BEGIN_BOOK_VIEWS = 0x0087;
    public static final int END_BOOK_VIEWS = 0x0088;
    public static final int BOOK_VIEW = 0x0089;
    
    // Sheets
    public static final int SHEET = 0x009C;
    
    private Biff12Constants() {}
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/cn/itcraft/jxlsb/format/Biff12Constants.java
git commit -m "feat(format): add BIFF12 record type constants"
```

---

### Task 2.2: Implement SharedStringsTable

**Files:**
- Create: `src/main/java/cn/itcraft/jxlsb/format/SharedStringsTable.java`
- Test: `src/test/java/cn/itcraft/jxlsb/format/SharedStringsTableTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jxlsb.format;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedStringsTableTest {
    
    @Test
    void deduplicatesStrings() {
        SharedStringsTable sst = new SharedStringsTable();
        
        int idx1 = sst.addString("Hello");
        int idx2 = sst.addString("World");
        int idx3 = sst.addString("Hello"); // duplicate
        
        assertEquals(0, idx1);
        assertEquals(1, idx2);
        assertEquals(0, idx3); // same as first
        assertEquals(2, sst.getCount());
        assertEquals(3, sst.getTotalCount());
    }
    
    @Test
    void writesToBytes() {
        SharedStringsTable sst = new SharedStringsTable();
        sst.addString("A");
        sst.addString("B");
        
        byte[] data = sst.toBiff12Bytes();
        assertTrue(data.length > 0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvnd test -Dtest=SharedStringsTableTest -q`
Expected: FAIL - class not found

- [ ] **Step 3: Implement SharedStringsTable**

```java
package cn.itcraft.jxlsb.format;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public final class SharedStringsTable {
    
    private final List<String> strings = new ArrayList<>();
    private final Map<String, Integer> indexMap = new HashMap<>();
    private int totalCount = 0;
    
    public synchronized int addString(String str) {
        totalCount++;
        Integer idx = indexMap.get(str);
        if (idx != null) {
            return idx;
        }
        int newIndex = strings.size();
        strings.add(str);
        indexMap.put(str, newIndex);
        return newIndex;
    }
    
    public int getCount() {
        return strings.size();
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public String getString(int index) {
        return strings.get(index);
    }
    
    public byte[] toBiff12Bytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // BEGIN_SST record
        writeRecordHeader(dos, Biff12Constants.BEGIN_SST, 8);
        writeInt(dos, strings.size());   // count
        writeInt(dos, totalCount);       // totalCount
        
        // SST items
        for (String str : strings) {
            byte[] strBytes = str.getBytes(StandardCharsets.UTF_16LE);
            writeRecordHeader(dos, Biff12Constants.SST_ITEM, 4 + strBytes.length);
            writeInt(dos, strBytes.length);
            dos.write(strBytes);
        }
        
        // END_SST record
        writeRecordHeader(dos, Biff12Constants.END_SST, 0);
        
        dos.flush();
        return baos.toByteArray();
    }
    
    private void writeRecordHeader(DataOutputStream dos, int type, int size) throws IOException {
        dos.writeShort(type & 0xFFFF);
        dos.writeShort(size & 0xFFFF);
    }
    
    private void writeInt(DataOutputStream dos, int value) throws IOException {
        dos.writeInt(Integer.reverseBytes(value));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvnd test -Dtest=SharedStringsTableTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jxlsb/format/SharedStringsTable.java src/test/java/cn/itcraft/jxlsb/format/SharedStringsTableTest.java
git commit -m "feat(format): add SharedStringsTable for string deduplication"
```

---

## Phase 3: Complete XlsbWriter Integration

### Task 3.1: Create WorkbookWriter

**Files:**
- Create: `src/main/java/cn/itcraft/jxlsb/format/WorkbookWriter.java`
- Test: `src/test/java/cn/itcraft/jxlsb/format/WorkbookWriterTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jxlsb.format;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkbookWriterTest {
    
    @Test
    void writesWorkbookWithOneSheet() throws Exception {
        WorkbookWriter writer = new WorkbookWriter();
        writer.addSheet("Sheet1");
        
        byte[] data = writer.toBiff12Bytes();
        assertTrue(data.length > 0);
        
        // Check for BEGIN_BOOK
        int type = ((data[0] & 0xFF) | ((data[1] & 0xFF) << 8));
        assertEquals(Biff12Constants.BEGIN_BOOK, type);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvnd test -Dtest=WorkbookWriterTest -q`
Expected: FAIL - class not found

- [ ] **Step 3: Implement WorkbookWriter**

```java
package cn.itcraft.jxlsb.format;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class WorkbookWriter {
    
    private final List<SheetInfo> sheets = new ArrayList<>();
    
    public void addSheet(String name) {
        sheets.add(new SheetInfo(name, sheets.size()));
    }
    
    public byte[] toBiff12Bytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // BEGIN_BOOK
        writeRecordHeader(dos, Biff12Constants.BEGIN_BOOK, 0);
        
        // FILE_VERSION
        writeRecordHeader(dos, Biff12Constants.FILE_VERSION, 12);
        writeInt(dos, 0x0006); // version
        writeInt(dos, 0); // reserved
        
        // BEGIN_SHEETS
        writeRecordHeader(dos, Biff12Constants.BEGIN_BUNDLE_SHS, 0);
        
        // Sheet records
        for (SheetInfo sheet : sheets) {
            byte[] nameBytes = sheet.name.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
            writeRecordHeader(dos, Biff12Constants.SHEET, 8 + nameBytes.length);
            writeInt(dos, sheet.index); // sheetId
            writeInt(dos, 0); // state (visible)
            // name string
        }
        
        // END_SHEETS
        writeRecordHeader(dos, Biff12Constants.END_BUNDLE_SHS, 0);
        
        // END_BOOK
        writeRecordHeader(dos, Biff12Constants.END_BOOK, 0);
        
        dos.flush();
        return baos.toByteArray();
    }
    
    private void writeRecordHeader(DataOutputStream dos, int type, int size) throws IOException {
        dos.writeShort(type & 0xFFFF);
        dos.writeShort(size & 0xFFFF);
    }
    
    private void writeInt(DataOutputStream dos, int value) throws IOException {
        dos.writeInt(Integer.reverseBytes(value));
    }
    
    private static final class SheetInfo {
        final String name;
        final int index;
        
        SheetInfo(String name, int index) {
            this.name = name;
            this.index = index;
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvnd test -Dtest=WorkbookWriterTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jxlsb/format/WorkbookWriter.java src/test/java/cn/itcraft/jxlsb/format/WorkbookWriterTest.java
git commit -m "feat(format): add WorkbookWriter for workbook.bin"
```

---

### Task 3.2: Integrate XlsbWriter with Container

**Files:**
- Modify: `src/main/java/cn/itcraft/jxlsb/api/XlsbWriter.java`
- Test: `src/test/java/cn/itcraft/jxlsb/api/XlsbWriterIntegrationTest.java`

- [ ] **Step 1: Write integration test**

```java
package cn.itcraft.jxlsb.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class XlsbWriterIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void writesValidXlsbStructure() throws Exception {
        Path file = tempDir.resolve("test.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Sheet1", (row, col) -> {
                if (row == 0 && col == 0) return CellData.text("Hello");
                if (row == 0 && col == 1) return CellData.number(123.45);
                return CellData.blank();
            }, 10, 2);
        }
        
        // Verify ZIP structure
        assertTrue(Files.exists(file));
        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertNotNull(zf.getEntry("[Content_Types].xml"), "Missing Content_Types");
            assertNotNull(zf.getEntry("_rels/.rels"), "Missing root rels");
            assertNotNull(zf.getEntry("xl/workbook.bin"), "Missing workbook.bin");
            assertNotNull(zf.getEntry("xl/worksheets/sheet1.bin"), "Missing sheet1.bin");
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvnd test -Dtest=XlsbWriterIntegrationTest -q`
Expected: FAIL - missing entries in ZIP

- [ ] **Step 3: Refactor XlsbWriter to use container**

Read current `XlsbWriter.java` first, then modify:

```java
// Add import
import cn.itcraft.jxlsb.container.XlsbContainer;
import cn.itcraft.jxlsb.container.ContentTypes;
import cn.itcraft.jxlsb.container.RelsGenerator;
import cn.itcraft.jxlsb.format.SharedStringsTable;
import cn.itcraft.jxlsb.format.WorkbookWriter;

// Modify class to use container
public final class XlsbWriter implements AutoCloseable {
    
    private final XlsbContainer container;
    private final SharedStringsTable sharedStrings;
    private final WorkbookWriter workbookWriter;
    private int sheetCount = 0;
    
    private XlsbWriter(Builder builder) throws IOException {
        this.container = XlsbContainer.create(builder.path);
        this.sharedStrings = new SharedStringsTable();
        this.workbookWriter = new WorkbookWriter();
    }
    
    public void writeBatch(String sheetName, CellDataSupplier supplier,
                           int rowCount, int columnCount) throws IOException {
        // Collect strings and write sheet data
        byte[] sheetData = writeSheetData(sheetName, supplier, rowCount, columnCount);
        
        workbookWriter.addSheet(sheetName);
        
        // Add to container
        container.addEntry("xl/worksheets/sheet" + (sheetCount + 1) + ".bin", sheetData);
        sheetCount++;
    }
    
    @Override
    public void close() throws IOException {
        // Write all container entries
        writeContainerStructure();
        container.close();
    }
    
    private void writeContainerStructure() throws IOException {
        // [Content_Types].xml
        ContentTypes ct = new ContentTypes();
        ct.addOverride("/xl/workbook.bin", "application/vnd.ms-excel.workbook");
        for (int i = 1; i <= sheetCount; i++) {
            ct.addOverride("/xl/worksheets/sheet" + i + ".bin", "application/vnd.ms-excel.worksheet");
        }
        container.addEntry("[Content_Types].xml", ct.toXml());
        
        // _rels/.rels
        container.addEntry("_rels/.rels", RelsGenerator.generateRootRels());
        
        // xl/workbook.bin
        container.addEntry("xl/workbook.bin", workbookWriter.toBiff12Bytes());
        
        // xl/_rels/workbook.bin.rels
        container.addEntry("xl/_rels/workbook.bin.rels", RelsGenerator.generateWorkbookRels(sheetCount));
        
        // xl/sharedStrings.bin
        if (sharedStrings.getCount() > 0) {
            container.addEntry("xl/sharedStrings.bin", sharedStrings.toBiff12Bytes());
        }
    }
    
    private byte[] writeSheetData(String sheetName, CellDataSupplier supplier,
                                  int rowCount, int columnCount) throws IOException {
        // Implement proper BIFF12 sheet writing
        // ...
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvnd test -Dtest=XlsbWriterIntegrationTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jxlsb/api/XlsbWriter.java src/test/java/cn/itcraft/jxlsb/api/XlsbWriterIntegrationTest.java
git commit -m "feat(writer): integrate XlsbWriter with ZIP container"
```

---

## Phase 4: Proper BIFF12 Cell Records

### Task 4.1: Fix Cell Record Format

**Files:**
- Modify: `src/main/java/cn/itcraft/jxlsb/format/record/CellRecord.java`

- [ ] **Step 1: Update CellRecord to use correct BIFF12 format**

According to MS-XLSB, cell records have specific formats:
- CELL_BLANK: row(4) + col(4)
- CELL_BOOL: row(4) + col(4) + value(1)
- CELL_FLOAT: row(4) + col(4) + value(8)
- CELL_STRING: row(4) + col(4) + sstIndex(4)

Read current file, then modify:

```java
public static CellRecord createNumber(int rowIndex, int colIndex, double value, MemoryBlock dataBlock) {
    // Use CELL_FLOAT (0x0006) record type
    // Format: row(4) + col(4) + value(8)
    dataBlock.putInt(0, rowIndex);
    dataBlock.putInt(4, colIndex);
    dataBlock.putDouble(8, value);
    return new CellRecord(Biff12Constants.CELL_FLOAT, 16, dataBlock);
}

public static CellRecord createText(int rowIndex, int colIndex, int sstIndex, MemoryBlock dataBlock) {
    // Use CELL_STRING (0x0007) record type
    // Format: row(4) + col(4) + sstIndex(4)
    dataBlock.putInt(0, rowIndex);
    dataBlock.putInt(4, colIndex);
    dataBlock.putInt(8, sstIndex);
    return new CellRecord(Biff12Constants.CELL_STRING, 12, dataBlock);
}
```

- [ ] **Step 2: Run all tests**

Run: `mvnd test -q`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add src/main/java/cn/itcraft/jxlsb/format/record/CellRecord.java
git commit -m "fix(record): use correct BIFF12 cell record types"
```

---

## Phase 5: Final Integration

### Task 5.1: Complete XlsbWriter Implementation

**Files:**
- Modify: `src/main/java/cn/itcraft/jxlsb/api/XlsbWriter.java`
- Modify: `src/main/java/cn/itcraft/jxlsb/format/XlsbFormatWriter.java`

- [ ] **Step 1: Implement complete sheet writing**

```java
private byte[] writeSheetData(String sheetName, CellDataSupplier supplier,
                              int rowCount, int columnCount) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    
    // BEGIN_SHEET
    writeRecordHeader(dos, Biff12Constants.BEGIN_SHEET, 0);
    
    // Dimension
    writeRecordHeader(dos, Biff12Constants.WS_DIMENSION, 16);
    writeLong(dos, 0); // minRow
    writeLong(dos, rowCount - 1); // maxRow
    writeLong(dos, 0); // minCol
    writeLong(dos, columnCount - 1); // maxCol
    
    // Sheet data
    for (int row = 0; row < rowCount; row++) {
        // BEGIN_ROW
        writeRecordHeader(dos, Biff12Constants.BEGIN_ROW, 8);
        writeInt(dos, row);
        writeInt(dos, columnCount);
        
        for (int col = 0; col < columnCount; col++) {
            CellData data = supplier.get(row, col);
            if (data != null && data.getType() != null) {
                writeCellRecord(dos, row, col, data);
            }
        }
        
        // END_ROW
        writeRecordHeader(dos, Biff12Constants.END_ROW, 0);
    }
    
    // END_SHEET
    writeRecordHeader(dos, Biff12Constants.END_SHEET, 0);
    
    dos.flush();
    return baos.toByteArray();
}

private void writeCellRecord(DataOutputStream dos, int row, int col, CellData data) throws IOException {
    switch (data.getType()) {
        case NUMBER:
            writeRecordHeader(dos, Biff12Constants.CELL_FLOAT, 16);
            writeInt(dos, row);
            writeInt(dos, col);
            dos.writeDouble(Double.longBitsToDouble(Double.doubleToLongBits((Double)data.getValue()) ^ 0x8000000000000000L));
            break;
        case TEXT:
            int sstIdx = sharedStrings.addString((String) data.getValue());
            writeRecordHeader(dos, Biff12Constants.CELL_STRING, 12);
            writeInt(dos, row);
            writeInt(dos, col);
            writeInt(dos, sstIdx);
            break;
        case BOOLEAN:
            writeRecordHeader(dos, Biff12Constants.CELL_BOOL, 9);
            writeInt(dos, row);
            writeInt(dos, col);
            dos.writeByte((Boolean) data.getValue() ? 1 : 0);
            break;
        default:
            break;
    }
}
```

- [ ] **Step 2: Run all tests**

Run: `mvnd test -q`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add src/main/java/cn/itcraft/jxlsb/api/XlsbWriter.java
git commit -m "feat(writer): complete BIFF12 sheet data writing"
```

---

### Task 5.2: Verify with File Size Test

**Files:**
- Run: `FileSizeComparisonTest`

- [ ] **Step 1: Run file size comparison**

Run: `mvnd test -Dtest=FileSizeComparisonTest -q`

Expected: jxlsb XLSB file should be smaller than XLSX

- [ ] **Step 2: Analyze and fix if needed**

If file is still large, check:
1. String deduplication is working
2. ZIP compression is applied
3. BIFF12 records are minimal

---

## Verification Checklist

- [ ] All tests pass: `mvnd test`
- [ ] File size comparison: jxlsb < POI XLSX
- [ ] ZIP structure valid with all required entries
- [ ] Shared strings table deduplicates strings
- [ ] BIFF12 records use correct types

---

**Plan complete.**