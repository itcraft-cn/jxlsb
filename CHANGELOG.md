# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-04-16

### Added

**Core Write Features**
- Number cell support (integer, floating-point)
- Text cell support with SST optimization
- Boolean cell support
- Date cell support (Excel date serial number)
- Blank cell support
- Cell style system (font, border, fill, alignment)
- Number format support (percentage, comma, negative red, currency, date, time)

**Write APIs**
- `writeBatch(sheetName, supplier, rowCount, columnCount)` - One-shot batch write
- `startSheet(sheetName, columnCount)` + `writeRows()` + `endSheet()` - Streaming append write
- Template-based write with `XlsbWriter.builder().template(path).path(output).build()`

**Template Fill APIs**
- `fillBatch(sheetIndex, dataList, startRow, startCol)` - Fixed position fill
- `fillAtMarker("${marker}", dataList)` - Marker lookup fill
- `startFill()` + `fillRows()` + `endFill()` - Streaming fill
- Preserve template styles, merged cells, and all content

**Read Features**
- Streaming read via `forEachRow(sheetIndex, rowConsumer)` callback
- Paginated read via `readRows(sheetIndex, offset, limit)` batch return
- Shared Strings Table (SST) support
- BIFF12 format parsing (BrtCellRk, BrtCellReal, BrtCellSt, BrtCellBool, BrtCellBlank, BrtCellIsst)

**Memory Management**
- Full off-heap memory architecture (zero GC pressure)
- ByteBufferAllocator for Java 8-22
- MemorySegmentAllocator for Java 23+ (Foreign Memory API)
- SPI-based allocator provider mechanism
- Memory pool with size-based buckets (64B/4KB/64KB/1MB/16MB)

**Multi-Release JAR**
- Three artifact variants:
  - `jxlsb-1.0.0.jar` (default, JDK 8)
  - `jxlsb-1.0.0-jdk8.jar` (classifier: jdk8, JDK 8)
  - `jxlsb-1.0.0-jdk23.jar` (classifier: jdk23, JDK 23+)
- Automatic allocator selection based on JVM version

**Performance**
- 2-3x faster than Apache POI
- 2-2.5x faster than EasyExcel
- Comparable to FastExcel in speed
- 35-50% smaller file size than XLSX
- 256KB zip buffer optimization
- SST concurrent optimization

### Performance Benchmarks

**100K rows × 10 columns:**
| Library | File Size | Write Time |
|---------|-----------|------------|
| jxlsb | 2.72 MB | 453 ms |
| FastExcel | 5.42 MB | 521 ms |
| EasyExcel | 4.21 MB | 1121 ms |
| POI | 4.16 MB | 1528 ms |

**1M rows × 10 columns:**
| Library | File Size | Write Time |
|---------|-----------|------------|
| jxlsb | 26.71 MB | 4647 ms |
| FastExcel | 55.00 MB | 4621 ms |
| EasyExcel | 42.54 MB | 9405 ms |
| POI | 42.25 MB | 8334 ms |

### Dependencies

- SLF4J API (only dependency)
- No Apache POI, no EasyExcel, no other heavy libraries

### Test Coverage

- 130 unit tests all passing
- Memory leak detection tests
- Performance comparison tests
- API integration tests
- Template fill tests

### Not Supported

- Formula cells
- Chart objects
- Conditional formatting
- Macro/VBA
- Footer template preservation (only header template supported)

### Code Quality

- P0-P2 issues resolved from code review
- Pattern pre-compiled for XML template replacement
- AutoCloseable implementation for resource management
- serialVersionUID added for exception classes
- EncryptionUtils extracted to eliminate code duplication

### Documentation

- README.md (English)
- README_cn.md (Chinese)
- CHANGELOG.md
- AGENTS.md (project guide)
- MEMORY.md (local development notes, not tracked)