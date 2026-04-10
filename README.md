# jxlsb - Java XLSB Library

纯Java实现的XLSB（Excel Binary Workbook）格式读写库，具有以下特性：

- **零依赖**：仅依赖SLF4J，无需POI等重型库
- **堆外内存**：全量堆外内存架构，追求零GC压力
- **流式API**：支持大规模数据流式写入
- **企业级质量**：支持Java 8+，完善的测试覆盖

## 核心功能

- ✅ XLSB文件写入（数字、文本、布尔、日期、空白）
- ✅ Excel/WPS兼容性验证通过
- ✅ 性能优异：比POI快2-3倍，文件小30-50%
- 🚧 XLSB文件读取（开发中）
- 🚧 样式和格式支持（开发中）

## 性能数据

**100K行 × 10列测试结果：**

| 库 | 文件大小 | 写入时间 | 格式 |
|----|---------|---------|------|
| jxlsb | 2.61 MB | 590 ms | XLSB |
| FastExcel | 5.42 MB | 591 ms | XLSX |
| EasyExcel | 4.18 MB | 1173 ms | XLSX |
| POI | 4.16 MB | 1826 ms | XLSX |

**优势总结：**
- 文件大小：比POI小37%，比EasyExcel小38%
- 写入速度：比POI快3倍，比EasyExcel快2倍
- 内存占用：堆外内存架构，GC压力极低

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jxlsb</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 5分钟教程

#### 1. 写入XLSB文件

```java
import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import java.nio.file.Path;
import java.nio.file.Paths;

Path file = Paths.get("output.xlsb");

try (XlsbWriter writer = XlsbWriter.builder()
        .path(file)
        .build()) {
    
    writer.writeBatch("Sheet1", 
        (row, col) -> CellData.number(row * 100.0 + col),
        1000, 10);
}
```

#### 2. 读取XLSB文件

```java
import cn.itcraft.jxlsb.api.XlsbReader;
import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;

try (XlsbReader reader = XlsbReader.builder()
        .path(Paths.get("data.xlsb"))
        .build()) {
    
    reader.readSheets(sheet -> {
        System.out.println("Sheet: " + sheet.getSheetName());
        
        for (OffHeapRow row : sheet) {
            for (int i = 0; i < row.getColumnCount(); i++) {
                OffHeapCell cell = row.getCell(i);
                System.out.print(formatCell(cell) + " | ");
            }
            System.out.println();
        }
    });
}
```

#### 3. 支持的单元格类型

```java
// 文本
CellData.text("Hello World")

// 数字
CellData.number(3.14159)

// 日期（Unix毫秒时间戳）
CellData.date(System.currentTimeMillis())

// 布尔
CellData.bool(true)

// 空白
CellData.blank()
```

## API概览

### XlsbWriter

**Builder模式构建：**
```java
XlsbWriter writer = XlsbWriter.builder()
    .path(Paths.get("output.xlsb"))
    .build();
```

**批量写入API：**
```java
writer.writeBatch("SheetName", 
    (row, col) -> {
        // 返回CellData
        return CellData.text("数据-" + row);
    },
    rowCount,    // 行数
    columnCount  // 列数
);
```

**流式写入API：**
```java
writer.writeBatch("Sheet1",
    (row, col) -> {
        if (col % 3 == 0) return CellData.number(row * col);
        if (col % 3 == 1) return CellData.text("文本");
        return CellData.date(System.currentTimeMillis());
    },
    100000, 50);
```

### XlsbReader

**Builder模式构建：**
```java
XlsbReader reader = XlsbReader.builder()
    .path(Paths.get("input.xlsb"))
    .build();
```

**流式读取API：**
```java
reader.readSheets(sheet -> {
    // 处理每个Sheet
    for (OffHeapRow row : sheet) {
        for (OffHeapCell cell : row) {
            // 处理单元格
        }
    }
});
```

**指定Sheet读取：**
```java
OffHeapSheet sheet = reader.readSheet(0); // 读取第一个Sheet
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      API Layer                               │
│  XlsbReader / XlsbWriter + Builder pattern                   │
├─────────────────────────────────────────────────────────────┤
│                    Data Structure Layer                      │
│  OffHeapCell / OffHeapRow / OffHeapSheet / OffHeapWorkbook   │
├─────────────────────────────────────────────────────────────┤
│                    Memory Management Layer                   │
│  MemoryBlock / OffHeapAllocator / MemoryPool                 │
│  ByteBufferAllocator (Java 8+)                               │
├─────────────────────────────────────────────────────────────┤
│                   XLSB Binary Format Layer                   │
│  RecordParser / RecordWriter / BIFF12 Records                │
│  CellRecord / BeginSheetRecord / BeginRowRecord / ...        │
├─────────────────────────────────────────────────────────────┤
│                      IO Layer                                │
│  OffHeapInputStream / OffHeapOutputStream                    │
│  FileChannel zero-copy read/write                            │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jxlsb</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Writing XLSB

```java
Path file = Paths.get("output.xlsb");

try (XlsbWriter writer = XlsbWriter.builder()
        .path(file)
        .build()) {
    
    writer.writeBatch("Sheet1", 
        (row, col) -> CellData.number(row * 100.0 + col),
        1000, 10);
}
```

### Reading XLSB

```java
Path file = Paths.get("data.xlsb");

try (XlsbReader reader = XlsbReader.builder()
        .path(file)
        .build()) {
    
    reader.readSheets(sheet -> {
        System.out.println("Sheet: " + sheet.getSheetName());
        
        for (OffHeapRow row : sheet) {
            for (int i = 0; i < row.getColumnCount(); i++) {
                OffHeapCell cell = row.getCell(i);
                System.out.print(formatCell(cell) + " | ");
            }
            System.out.println();
        }
    });
}
```

### Cell Types

```java
// Text
CellData.text("Hello World")

// Number
CellData.number(3.14159)

// Date (Unix timestamp in milliseconds)
CellData.date(System.currentTimeMillis())

// Boolean
CellData.bool(true)

// Blank
CellData.blank()
```

## Performance

| Metric | Target | Notes |
|--------|--------|-------|
| Memory Usage | Off-heap only | All data structures use off-heap memory |
| GC Pressure | Minimal | No large objects on Java heap |
| Streaming | Supported | Process GB-scale files |

## BIFF12 Records Supported

| Record | Type Code | Description |
|--------|-----------|-------------|
| BEGIN_BOOK | 0x0083 | Workbook start |
| END_BOOK | 0x0084 | Workbook end |
| BEGIN_SHEET | 0x0085 | Sheet start |
| END_SHEET | 0x0086 | Sheet end |
| BEGIN_ROW | 0x0087 | Row start |
| END_ROW | 0x0088 | Row end |
| VERSION | 0x0080 | Version info |
| CELL | 0x0143 | Cell data |
| STRING | 0x00F9 | String data |
| **Extended Records** | | |
| INDEX | 0x0089 | Row index optimization |
| FORMAT | 0x0041 | Cell format strings |
| XF | 0x0043 | Extended format (font/align/fill/border) |
| FORMULA | 0x0108 | Formula support |
| MERGE_CELL | 0x00B7 | Merged cells |
| CONDITIONAL_FORMAT | 0x01CD | Conditional formatting |
| DATA_VALIDATION | 0x01B2 | Data validation |

## Advanced Features

### Java 17+ MemorySegment Support

The library automatically uses `MemorySegment` (Foreign Memory API) on Java 17+ for better performance:

```java
// Automatic selection via ServiceLoader
// Java 8: ByteBufferAllocator (priority 10)
// Java 17+: MemorySegmentAllocator (priority 20) 
OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
```

### Formula Support

```java
FormulaRecord record = FormulaRecord.create(
    10, 5,                              // row, col
    CellType.NUMBER, 42.0,              // result type and value
    "SUM(A1:A10)", dataBlock);          // formula string
```

### Cell Formatting

```java
// Create format
FormatRecord format = FormatRecord.create(
    FormatRecord.FORMAT_DATE, "yyyy-mm-dd", dataBlock);

// Create extended format
XFRecord xf = XFRecord.create(
    0, FormatRecord.FORMAT_NUMBER,      // font, format
    XFRecord.ALIGN_RIGHT, XFRecord.VERTICAL_CENTER,
    0, 0, dataBlock);                   // fill, border
```

### Merged Cells

```java
MergeCellRecord merge = MergeCellRecord.create(
    0, 2, 0, 3, dataBlock);  // merge rows 0-2, cols 0-3
```

### Data Validation

```java
DataValidationRecord validation = DataValidationRecord.create(
    DataValidationRecord.TYPE_WHOLE,    // validation type
    DataValidationRecord.ERROR_STYLE_STOP,
    false,                              // allow blank
    "BETWEEN 1 AND 100", dataBlock);    // validation rule
```

### Conditional Formatting

```java
ConditionalFormatRecord cf = ConditionalFormatRecord.create(
    ConditionalFormatRecord.TYPE_CELL_IS,
    ConditionalFormatRecord.OPERATOR_GREATER_THAN,
    "100", dataBlock);                  // condition formula
```

## Build

```bash
# Compile
mvn clean compile

# Run tests
mvn clean test

# Package
mvn clean package -DskipTests
```

## Project Statistics

- Java Source Files: 68
- Test Cases: 61 (100% pass)
- Test Coverage: Memory, Format, Data, IO, API layers
- BIFF12 Record Types: 15+ supported
- Git Commits: 18

## Dependencies

**Runtime:**
- Java 8+ (Java 17+ recommended for MemorySegment support)
- SLF4J API (users must provide implementation)

**Build:**
- Maven 3.6+
- For Java 17+ features: JDK 17+ with `--enable-preview` or JDK 21+

**Test:**
- JUnit 5
- Mockito
- JMH (benchmarks)

## License

Apache License 2.0

## References

- [MS-XLSB]: Excel Binary Workbook (.xlsb) File Format - Microsoft Open Specifications