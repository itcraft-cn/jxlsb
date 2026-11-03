[中文](MANUAL.md) | **English**

# jxlsb Manual

> This manual is for developers using jxlsb. It covers environment requirements, dependency setup, build, API usage and common scenarios.
> For a feature overview see [README_en.md](README_en.md); for version history see [CHANGELOG_en.md](CHANGELOG_en.md).

## 1. Introduction

jxlsb is a pure Java XLSB (Excel Binary Workbook) reader/writer library. Its fixed starting point is **high-performance data output (writing)**; reading was initially meant only for consuming templates, and high-performance reading followed naturally once reading was complete.

## 2. Requirements

| Item | Requirement |
|------|-------------|
| JDK | 8+ (Java 23+ automatically switches to the Foreign Memory API) |
| Build tool | Maven |
| Runtime dependency | SLF4J (logging only) |

## 3. Installation & Build

### 3.1 Maven Dependency

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jxlsb</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3.2 Build from Source

```bash
mvn clean package          # package
mvn test                   # run tests
./run_benchmarks.sh        # run benchmarks
```

## 4. API Usage

### 4.1 Writing

**One-shot batch write (data already in memory)**

```java
try (XlsbWriter writer = XlsbWriter.builder().path(Paths.get("output.xlsb")).build()) {
    writer.writeBatch("Sheet1", (row, col) -> CellData.number(row * col), 1000, 10);
}
```

**Streaming append write (DB pagination, avoids OOM)**

```java
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

### 4.2 Reading

**Streaming row-by-row processing**

```java
try (XlsbReader reader = XlsbReader.builder().path(Paths.get("data.xlsb")).build()) {
    reader.forEachRow(0, new RowConsumer() {
        public void onCell(int row, int col, CellData data) {
            processCell(data);
        }
    });
}
```

**Paginated batch read**

```java
try (XlsbReader reader = XlsbReader.builder().path(Paths.get("data.xlsb")).build()) {
    int offset = 0;
    while (true) {
        List<CellData[]> batch = reader.readRows(0, offset, 1000);
        if (batch.isEmpty()) break;
        batchProcess(batch);
        offset += batch.size();
    }
}
```

### 4.3 Template Fill

```java
XlsbWriter writer = XlsbWriter.builder()
    .template(Paths.get("template.xlsb"))
    .path(Paths.get("output.xlsb"))
    .build();

List<List<Object>> data = Arrays.asList(
    Arrays.asList("John", "NYC", 25, "M"),
    Arrays.asList("Jane", "LA", 30, "F")
);

// Option 1: fixed-position fill
writer.fillBatch(0, data, 4, 2);   // sheetIndex, dataList, startRow, startCol

// Option 2: marker lookup fill
writer.fillAtMarker("${data}", data);

// Option 3: streaming fill
writer.startFill(0, 12, 8);
writer.fillRows(batch1);
writer.fillRows(batch2);
writer.endFill();

writer.close();
```

### 4.4 Row/Column Limits and Compression

```java
import cn.itcraft.jxlsb.format.Limits;

Limits.setOverflow(Limits.OverflowBehavior.CUT, Limits.OverflowBehavior.ERROR);

try (XlsbWriter writer = XlsbWriter.builder()
        .path(Paths.get("output.xlsb"))
        .compressionLevel(6)   // 0=store, 1-9=deflate; default 1
        .build()) {
    writer.writeBatch("Sheet1", supplier, rows, cols);
}
```

- Limits: `Limits.MAX_COLUMNS = 16384`, `Limits.MAX_ROWS = 1048576`
- `ERROR` throws `LimitExceededException`; `CUT` truncates to the limit
- Configuration is global and snapshotted when each write operation starts
- Batch writes, streaming writes and template filling share the same policy

## 5. CellData Types

```java
CellData.text("Hello")       // Text
CellData.number(3.14159)     // Number
CellData.date(timestamp)     // Date (millisecond timestamp)
CellData.bool(true)          // Boolean
CellData.blank()             // Blank
```

## 6. Number Formats

```java
CellData.percentage(0.1234)           // 0.00%
CellData.numberWithComma(1234567.89)  // #,##0.00
CellData.numberNegativeRed(-1234.56)  // #,##0.00;[Red]-#,##0.00
CellData.currency(1234.56)            // $#,##0.00
CellData.time(timestamp)              // h:mm:ss
```

## 7. Scenario Guide

| Scenario | Recommended API | Data Source | Memory Pressure |
|----------|-----------------|-------------|-----------------|
| Reports, in-memory data export | `writeBatch` | In-memory / computed | None |
| DB pagination, large file streaming | `startSheet` + `writeRows` + `endSheet` | DB pagination / file stream | Low |
| Template fill, report generation | `template` + `fillBatch`/`fillAtMarker` | Template + data | None |
| Streaming cleansing | `forEachRow` | Any | None |
| Paginated batch processing | `readRows` | Large files | Low |

## 8. Performance

100K rows × 10 columns write comparison:

| Library | File Size | Write Time | Format |
|---------|-----------|------------|--------|
| **jxlsb** | **2.72 MB** | **453 ms** | XLSB |
| FastExcel | 5.42 MB | 521 ms | XLSX |
| EasyExcel | 4.21 MB | 1121 ms | XLSX |
| POI | 4.16 MB | 1528 ms | XLSX |

## 9. Limitations

- Only **header templates** are supported; footer templates are not
- No formula, chart, conditional formatting or macro/VBA support

## 10. Further Docs

- [README_en.md](README_en.md) — feature overview and architecture
- [CHANGELOG_en.md](CHANGELOG_en.md) — version history
- `docs/api-guide.md` — in-depth API guide
- `docs/best-practices.md` — best practices
- `docs/performance.md` — performance report
