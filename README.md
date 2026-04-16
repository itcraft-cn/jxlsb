**English** | [中文](README_cn.md)

# jxlsb - Java XLSB Library

A pure Java XLSB (Excel Binary Workbook) format reader/writer library.

## Features

- **Zero Dependencies**: Only SLF4J, no heavy libraries like POI
- **Off-Heap Memory**: Full off-heap architecture, zero GC pressure
- **High Performance**: 3x faster than POI, 2.5x faster than EasyExcel, 35-50% smaller files
- **Enterprise Ready**: Java 8+ support, Multi-Release JAR (Java 23+ auto-switches to Foreign Memory API)

> **Enterprise features** (encryption, watermark) available in [Commercial Edition](docs/opensource-vs-commercial.md)

## Performance Data

**100K rows × 10 columns:**

| Library | File Size | Write Time | Format |
|---|---|---|---|
| **jxlsb** | **2.72 MB** | **453 ms** | XLSB |
| FastExcel | 5.42 MB | 521 ms | XLSX |
| EasyExcel | 4.21 MB | 1121 ms | XLSX |
| POI | 4.16 MB | 1528 ms | XLSX |

**1M rows × 10 columns:**

| Library | File Size | Write Time | Format |
|---|---|---|---|
| **jxlsb** | **26.71 MB** | **4647 ms** | XLSB |
| FastExcel | 55.00 MB | 4621 ms | XLSX |
| EasyExcel | 42.54 MB | 9405 ms | XLSX |
| POI | 42.25 MB | 8334 ms | XLSX |

## API Scenarios

### Write API

| API | Use Case | Data Source | Memory Pressure | Example |
|---|---|---|---|---|
| **writeBatch** | Reports, memory data export | In-memory / computed | None | One-shot functional write |
| **startSheet + writeRows + endSheet** | DB pagination, large file streaming | DB pagination / file stream | Low | Batch append write |
| **template + fillBatch/fillAtMarker** | Template filling, report generation | Template + data | None | Preserve template styles |

### Template Fill API

Fill data based on XLSB template, preserving all template content (styles, merged cells, etc.):

```java
// Create template fill Writer
XlsbWriter writer = XlsbWriter.builder()
    .template(Paths.get("template.xlsb"))  // Template path
    .path(Paths.get("output.xlsb"))        // Output path
    .build();

// Method 1: Fixed position fill
List<List<Object>> data = Arrays.asList(
    Arrays.asList("John", "NYC", 25, "M"),
    Arrays.asList("Jane", "LA", 30, "F")
);
writer.fillBatch(0, data, 4, 2);  // sheetIndex, dataList, startRow, startCol

// Method 2: Marker lookup fill
writer.fillAtMarker("${data}", data);  // Find ${data} marker position and fill

// Method 3: Streaming fill
writer.startFill(0, 12, 8);
writer.fillRows(batch1);
writer.fillRows(batch2);
writer.endFill();

writer.close();
```

**Template Support Range**:
- ✅ Preserve all template content: styles.bin, theme, static text, etc.
- ✅ Preserve cell styles: font, border, fill, alignment
- ✅ Preserve merged cells
- ✅ Support marker lookup fill (e.g., `${data}`)
- ⚠️ **Header template only**: Data fills downward from specified position
- ❌ No footer template support: Cannot preserve bottom static content after fill

### Read API

| API | Use Case | Data Size | Example |
|---|---|---|---|
| **forEachRow** | Streaming process, data cleansing | Any | Callback per row |
| **readRows** | Paginated read, batch processing | Large files | List/Array batch return |

### Scenario Guide

**Write scenarios**:

```java
// Scenario 1: Memory data export (recommended: writeBatch)
List<Product> products = cache.getAll(); // Already in memory
writer.writeBatch("Products", (row, col) -> toCell(products.get(row), col), products.size(), 5);

// Scenario 2: DB pagination export (recommended: writeRows streaming)
writer.startSheet("Orders", 5);
int offset = 0;
while (true) {
    List<Order> batch = db.query(offset, 1000); // Paginated query, avoid OOM
    if (batch.isEmpty()) break;
    writer.writeRows(batch, offset, (order, col) -> toCell(order, col));
    offset += batch.size();
}
writer.endSheet();
```

**Read scenarios**:

```java
// Scenario 1: Streaming process (recommended: forEachRow)
reader.forEachRow(0, new RowConsumer() {
    void onCell(int row, int col, CellData data) {
        // Process directly, no storage needed
        processCell(data);
    }
});

// Scenario 2: Paginated batch process (recommended: readRows)
int offset = 0;
while (true) {
    List<CellData[]> batch = reader.readRows(0, offset, 1000);
    if (batch.isEmpty()) break;
    batchProcess(batch); // Batch process 1000 rows
    offset += 1000;
}
```

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jxlsb</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Write Example

```java
import cn.itcraft.jxlsb.api.*;
import java.nio.file.Paths;

// One-shot write (memory data)
try (XlsbWriter writer = XlsbWriter.builder().path(Paths.get("output.xlsb")).build()) {
    writer.writeBatch("Sheet1", (row, col) -> CellData.number(row * col), 1000, 10);
}

// Streaming append write (DB query)
try (XlsbWriter writer = XlsbWriter.builder().path(Paths.get("output.xlsb")).build()) {
    writer.startSheet("Orders", 4);
    int offset = 0;
    while (true) {
        List<Order> batch = db.query(offset, 1000);
        if (batch.isEmpty()) break;
        writer.writeRows(batch, offset, (order, col) -> {
            switch (col) {
                case 0: return CellData.number(order.getId());
                case 1: return CellData.text(order.getName());
                case 2: return CellData.number(order.getAmount());
                case 3: return CellData.date(order.getTime());
                default: return CellData.blank();
            }
        });
        offset += batch.size();
    }
    writer.endSheet();
}
```

### Read Example

```java
import cn.itcraft.jxlsb.api.*;

try (XlsbReader reader = XlsbReader.builder().path(Paths.get("data.xlsb")).build()) {
    // Streaming process
    reader.forEachRow(0, new RowConsumer() {
        void onCell(int row, int col, CellData data) {
            System.out.println(row + "," + col + ": " + data.getValue());
        }
    });
    
    // Paginated batch read
    int offset = 0;
    while (true) {
        List<CellData[]> batch = reader.readRows(0, offset, 1000);
        if (batch.isEmpty()) break;
        // Process batch
        offset += batch.size();
    }
}
```

### Cell Types

```java
CellData.text("Hello")       // Text
CellData.number(3.14159)     // Number
CellData.date(timestamp)     // Date (millisecond timestamp)
CellData.bool(true)          // Boolean
CellData.blank()             // Blank

// Number formats (percentage, comma, negative red, currency, etc.)
CellData.percentage(0.1234)           // 0.00%
CellData.numberWithComma(1234567.89)  // #,##0.00
CellData.numberNegativeRed(-1234.56)  // #,##0.00;[Red]-#,##0.00
CellData.currency(1234.56)            // $#,##0.00
CellData.time(timestamp)              // h:mm:ss
```

## Feature Status

| Feature | Status | Notes |
|---|---|---|
| Number cell | ✅ Complete | Integer, floating-point |
| Text cell | ✅ Complete | SST optimized, large text support |
| Boolean cell | ✅ Complete | |
| Date cell | ✅ Complete | Excel date serial number |
| Blank cell | ✅ Complete | |
| Style system | ✅ Complete | Font, border, fill, alignment |
| Number format | ✅ Complete | Custom format strings |
| Streaming write | ✅ Complete | startSheet/writeRows/endSheet |
| Streaming read | ✅ Complete | forEachRow callback |
| Paginated read | ✅ Complete | readRows batch return |
| Template fill | ✅ Complete | fillBatch/fillAtMarker/startFill |
| Merged cells | ✅ Complete | Preserved from template |
| Formula | ❌ Not supported | |
| Chart | ❌ Not supported | |
| Conditional format | ❌ Not supported | |
| Macro/VBA | ❌ Not supported | |

## Production Readiness

**Recommended scenarios**:
- ✅ Large Excel export (100K-1M rows)
- ✅ Database pagination export
- ✅ Storage cost sensitive (50% smaller files)
- ✅ Memory constrained environments (off-heap memory)
- ✅ Template report generation (preserve styles, merged cells)

**Not recommended scenarios**:
- ❌ Requires formulas, charts
- ❌ Requires conditional formatting
- ❌ Reports with both header and footer templates (header template only)

## Architecture

```
┌─────────────────────────────────────────┐
│           API Layer                      │
│  XlsbWriter / XlsbReader                 │
│  writeBatch / writeRows / readRows       │
├─────────────────────────────────────────┤
│       Data Structure Layer               │
│  CellData / RowDataSupplier              │
├─────────────────────────────────────────┤
│       Memory Management Layer            │
│  ByteBufferAllocator (Java 8)            │
│  MemorySegmentAllocator (Java 23+)       │
├─────────────────────────────────────────┤
│       BIFF12 Format Layer                │
│  BrtCellRk / BrtCellReal / BrtCellIsst   │
├─────────────────────────────────────────┤
│           IO Layer                       │
│  ZipContainer / FileChannel              │
└─────────────────────────────────────────┘
```

## Test Coverage

- **130 tests all passing**
- Memory layer: allocation, read/write, close, leak detection
- Format layer: BIFF12 records, VarInt encoding
- API layer: write, read, streaming append
- Performance tests: 100K/1M row comparison

## License

Apache License 2.0