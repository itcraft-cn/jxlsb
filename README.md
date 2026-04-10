# jxlsb

Pure Java XLSB (Excel Binary Workbook) reader/writer library with off-heap memory.

## Features

- **Zero Third-party Dependencies** - Only Java SDK and SLF4J
- **Off-heap Memory** - All data structures use off-heap memory for zero GC pressure
- **Multi-JDK Support** - Java 8+ (Java 17 recommended)
- **Streaming API** - Process large files without loading everything into memory
- **Memory Pool** - Reusable memory blocks for better performance

## Quick Start

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
                System.out.println(cell.getText());
            }
        }
    });
}
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

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      API Layer (Streaming)                   │
│  XlsbReader / XlsbWriter + Builder pattern                   │
├─────────────────────────────────────────────────────────────┤
│                    Data Structure Layer                      │
│  OffHeapCell / OffHeapRow / OffHeapSheet                     │
├─────────────────────────────────────────────────────────────┤
│                    Memory Management Layer                   │
│  MemoryBlock / OffHeapAllocator / MemoryPool                 │
├─────────────────────────────────────────────────────────────┤
│                   XLSB Binary Format Layer                   │
│  RecordParser / RecordWriter / BIFF12 Records                │
├─────────────────────────────────────────────────────────────┤
│                      IO Layer                                │
│  OffHeapInputStream / OffHeapOutputStream                    │
└─────────────────────────────────────────────────────────────┘
```

## Build

```bash
mvn clean test
mvn package
```

## Tech Stack

- Java 8+ (Java 17 recommended)
- SLF4J (API only)
- JUnit 5 (testing)
- Maven

## License

Apache License 2.0