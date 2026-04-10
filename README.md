# jxlsb

Pure Java XLSB (Excel Binary Workbook) reader/writer library with off-heap memory.

## Features

- **Zero Third-party Dependencies** - Only Java SDK 8+ and SLF4J API
- **Off-heap Memory Architecture** - All data structures use off-heap memory for minimal GC pressure
- **Streaming API** - Process large files without loading everything into memory
- **Memory Pool** - Reusable memory blocks with 5-tier size classification (64B/4KB/64KB/1MB/16MB)
- **Builder Pattern** - Fluent API for easy configuration
- **Multi-JDK Support** - Java 8+ compatible, Java 17+ recommended

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