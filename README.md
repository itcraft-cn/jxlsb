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

- Java Source Files: 43
- Test Cases: 54 (100% pass)
- Test Coverage: Memory, Format, Data, IO, API layers

## Dependencies

**Runtime:**
- Java 8+ (Java 17+ recommended)
- SLF4J API (users must provide implementation)

**Test:**
- JUnit 5
- Mockito
- JMH (benchmarks)

## License

Apache License 2.0

## References

- [MS-XLSB]: Excel Binary Workbook (.xlsb) File Format - Microsoft Open Specifications