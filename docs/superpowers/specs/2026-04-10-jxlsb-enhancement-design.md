# jxlsb 功能增强设计文档

## 一、设计目标

基于已完成的核心写入功能，增强以下4个模块：

1. **XlsbReader读取功能** - 完整实现XLSB文件流式读取
2. **日期格式样式支持** - 实现完整的样式系统
3. **性能优化和压力测试** - 完善测试体系
4. **文档和示例** - 提供完整的使用指南

---

## 二、模块1: XlsbReader读取功能

### 2.1 现状分析

**已有代码：**
- `XlsbFormatReader` - 部分格式读取框架
- `RecordParser` - BIFF12记录解析框架
- `CellRecord` 等记录类型

**缺失功能：**
- ZIP容器解析（提取内部文件）
- 完整BIFF12记录类型支持
- SST（Shared Strings Table）解析
- 流式API完整实现

### 2.2 架构设计

```
┌─────────────────────────────────────────────────┐
│                    XlsbReader                    │
│              (Builder模式构建)                    │
├─────────────────────────────────────────────────┤
│   XlsbContainerReader                           │
│   ├── ZipFile/ZipInputStream 解析                │
│   ├── 提取 xl/workbook.bin                      │
│   ├── 提取 xl/worksheets/sheetN.bin             │
│   └── 提取 xl/sharedStrings.bin                 │
├─────────────────────────────────────────────────┤
│   WorkbookReader                                │
│   ├── 解析 BrtBundleSh 获取Sheet列表             │
│   ├── 解析 BrtWbProp 获取Workbook属性            │
│   └── SheetInfo 包含名称、索引、路径              │
├─────────────────────────────────────────────────┤
│   SheetReader                                   │
│   ├── 解析 BrtWsDim 获取Sheet维度                │
│   ├── 解析 BrtRowHdr 获取行信息                  │
│   ├── 解析单元格记录：                           │
│   │   ├── BrtCellRk (RK编码数字)                │
│   │   ├── BrtCellReal (IEEE 754 double)         │
│   │   ├── BrtCellSt (SST字符串引用)              │
│   │   ├── BrtCellBool (布尔值)                   │
│   │   ├── BrtCellBlank (空白单元格)              │
│   │   └── BrtCellIsst (内联字符串)               │
│   └── 流式处理避免全量加载                        │
├─────────────────────────────────────────────────┤
│   SharedStringsReader                           │
│   ├── 解析 BrtSSTBegin                          │
│   ├── 解析 BrtSSTItem 存储字符串                 │
│   ├── SST索引 → 字符串映射表                     │
│   └── 内存优化：大文本单独堆外存储                │
└─────────────────────────────────────────────────┘
```

### 2.3 核心类设计

#### 2.3.1 XlsbContainerReader

```java
package cn.itcraft.jxlsb.container;

public final class XlsbContainerReader implements AutoCloseable {
    private final ZipFile zipFile;
    private final Map<String, ZipEntry> entries;
    
    public XlsbContainerReader(Path path) throws IOException {
        this.zipFile = new ZipFile(path.toFile());
        this.entries = buildEntryMap();
    }
    
    public InputStream getWorkbookStream() throws IOException {
        ZipEntry entry = entries.get("xl/workbook.bin");
        return zipFile.getInputStream(entry);
    }
    
    public InputStream getSheetStream(int sheetIndex) throws IOException {
        ZipEntry entry = entries.get("xl/worksheets/sheet" + sheetIndex + ".bin");
        return zipFile.getInputStream(entry);
    }
    
    public InputStream getSharedStringsStream() throws IOException {
        ZipEntry entry = entries.get("xl/sharedStrings.bin");
        return entry != null ? zipFile.getInputStream(entry) : null;
    }
    
    public List<SheetInfo> getSheetInfos() throws IOException {
        WorkbookReader reader = new WorkbookReader(getWorkbookStream());
        return reader.parseSheetList();
    }
}
```

#### 2.3.2 WorkbookReader

```java
package cn.itcraft.jxlsb.format;

public final class WorkbookReader {
    private final InputStream inputStream;
    private final OffHeapAllocator allocator;
    
    public WorkbookReader(InputStream inputStream) {
        this.inputStream = inputStream;
        this.allocator = AllocatorFactory.createDefaultAllocator();
    }
    
    public List<SheetInfo> parseSheetList() throws IOException {
        List<SheetInfo> sheets = new ArrayList<>();
        RecordParser parser = new RecordParser(allocator);
        
        parser.parse(inputStream, record -> {
            if (record.getRecordType() == Biff12RecordType.BRT_BUNDLE_SH) {
                SheetInfo info = parseBrtBundleSh(record);
                sheets.add(info);
            }
        });
        
        return sheets;
    }
    
    private SheetInfo parseBrtBundleSh(BiffRecord record) {
        MemoryBlock block = record.getDataBlock();
        int hsState = block.getInt(0);
        int iTabId = block.getInt(4);
        String relId = readXLWideString(block, 8);
        String name = readXLWideString(block, relIdOffset);
        
        return new SheetInfo(name, iTabId, relId);
    }
}
```

#### 2.3.3 SheetReader

```java
package cn.itcraft.jxlsb.format;

public final class SheetReader implements AutoCloseable {
    private final InputStream inputStream;
    private final SharedStringsTable sst;
    private final OffHeapAllocator allocator;
    private final int sheetIndex;
    
    public void readRows(RowHandler handler) throws IOException {
        RecordParser parser = new RecordParser(allocator);
        
        parser.parse(inputStream, record -> {
            switch (record.getRecordType()) {
                case Biff12RecordType.BRT_ROW_HDR:
                    BrtRowHdr rowHdr = new BrtRowHdr(record);
                    handler.onRowStart(rowHdr.getRowIndex(), rowHdr.getColumnCount());
                    break;
                    
                case Biff12RecordType.BRT_CELL_RK:
                    BrtCellRk cellRk = new BrtCellRk(record);
                    handler.onCellNumber(cellRk.getRow(), cellRk.getCol(), cellRk.getValue());
                    break;
                    
                case Biff12RecordType.BRT_CELL_REAL:
                    BrtCellReal cellReal = new BrtCellReal(record);
                    handler.onCellNumber(cellReal.getRow(), cellReal.getCol(), cellReal.getValue());
                    break;
                    
                case Biff12RecordType.BRT_CELL_ST:
                    BrtCellSt cellSt = new BrtCellSt(record, sst);
                    handler.onCellText(cellSt.getRow(), cellSt.getCol(), cellSt.getValue());
                    break;
                    
                case Biff12RecordType.BRT_CELL_BOOL:
                    BrtCellBool cellBool = new BrtCellBool(record);
                    handler.onCellBoolean(cellBool.getRow(), cellBool.getCol(), cellBool.getValue());
                    break;
                    
                case Biff12RecordType.BRT_CELL_BLANK:
                    BrtCellBlank cellBlank = new BrtCellBlank(record);
                    handler.onCellBlank(cellBlank.getRow(), cellBlank.getCol());
                    break;
            }
        });
    }
}
```

#### 2.3.4 SharedStringsTable

```java
package cn.itcraft.jxlsb.format;

public final class SharedStringsTable implements AutoCloseable {
    private final List<String> strings;
    private final Map<Integer, MemoryBlock> largeTextBlocks;
    
    public void load(InputStream inputStream) throws IOException {
        RecordParser parser = new RecordParser(allocator);
        
        parser.parse(inputStream, record -> {
            if (record.getRecordType() == Biff12RecordType.BRT_SST_ITEM) {
                String text = parseSSTItem(record);
                strings.add(text);
            }
        });
    }
    
    public String getString(int index) {
        if (index < 0 || index >= strings.size()) {
            throw new IndexOutOfBoundsException("SST index out of bounds");
        }
        return strings.get(index);
    }
    
    public int size() {
        return strings.size();
    }
}
```

### 2.4 流式API设计

```java
package cn.itcraft.jxlsb.api;

public final class XlsbReader implements AutoCloseable {
    private final XlsbContainerReader containerReader;
    private final SharedStringsTable sst;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public void forEachSheet(SheetConsumer consumer) throws IOException {
        List<SheetInfo> sheets = containerReader.getSheetInfos();
        for (SheetInfo info : sheets) {
            SheetReader reader = new SheetReader(
                containerReader.getSheetStream(info.getIndex()),
                sst, info.getIndex()
            );
            consumer.accept(info, reader);
            reader.close();
        }
    }
    
    public void forEachRow(int sheetIndex, RowConsumer consumer) throws IOException {
        SheetReader reader = getSheetReader(sheetIndex);
        reader.readRows(new RowHandler() {
            @Override
            public void onRowStart(int rowIndex, int columnCount) {
                consumer.onRowStart(rowIndex);
            }
            
            @Override
            public void onCellNumber(int row, int col, double value) {
                consumer.onCell(row, col, CellData.number(value));
            }
            
            @Override
            public void onCellText(int row, int col, String value) {
                consumer.onCell(row, col, CellData.text(value));
            }
        });
    }
    
    public static final class Builder {
        private Path path;
        
        public Builder path(Path path) {
            this.path = path;
            return this;
        }
        
        public XlsbReader build() throws IOException {
            return new XlsbReader(path);
        }
    }
}
```

### 2.5 BIFF12记录类型补充

需要实现的记录类型：

| Record Type | Hex | Description |
|-------------|-----|-------------|
| BrtCellRk | 0x0002 | RK编码数字 |
| BrtCellReal | 0x000E | IEEE 754 double |
| BrtCellSt | 0x000A | SST字符串引用 |
| BrtCellBool | 0x0009 | 布尔值 |
| BrtCellBlank | 0x0001 | 空白单元格 |
| BrtCellIsst | 0x000B | 内联字符串 |
| BrtRowHdr | 0x0080 | 行头记录 |
| BrtWsDim | 0x0094 | Sheet维度 |
| BrtSSTItem | 0x00FA | SST字符串项 |
| BrtBundleSh | 0x008F | Sheet信息 |

---

## 三、模块2: 日期格式样式支持

### 3.1 现状分析

**已有实现：**
- 7638字节styles.bin模板（硬编码）
- 日期类型写入（但无格式样式）
- 缺少样式索引应用

### 3.2 设计方案

#### 3.2.1 样式系统架构

```
┌───────────────────────────────────────────────┐
│             StylesWriter                      │
│          (完整样式表生成)                       │
├───────────────────────────────────────────────┤
│   CellStyleFormat                             │
│   ├── numFmtId (数字格式ID)                    │
│   ├── fontId (字体ID)                         │
│   ├── fillId (填充ID)                         │
│   ├── borderId (边框ID)                        │
│   └── xfId (样式格式ID)                        │
├───────────────────────────────────────────────┤
│   NumberFormatRegistry                        │
│   ├── 内置格式：0-163                          │
│   │   ├── 通用格式 (0)                         │
│   │   ├── 数字格式 (1-11)                      │
│   │   ├── 日期格式 (14-22)                     │
│   │   ├── 时间格式 (18-21)                     │
│   │   └── 百分比/科学计数法 (9-11)              │
│   ├── 自定义格式：164+                          │
│   └── 格式字符串映射                            │
├───────────────────────────────────────────────┤
│   CellStyleRegistry                           │
│   ├── 默认样式                                 │
│   ├── 日期样式集合                             │
│   ├── 数字样式集合                             │
│   └── 样式索引 → CellStyleFormat               │
└───────────────────────────────────────────────┘
```

#### 3.2.2 内置日期格式

Excel内置日期格式（ID 14-22）：

```java
public final class BuiltInFormats {
    public static final Map<Integer, String> DATE_FORMATS = Map.of(
        14, "mm-dd-yy",           // 日期短格式
        15, "d-mmm-yy",           // 日期带月份名
        16, "d-mmm",              // 仅日期
        17, "mmm-yy",             // 月份-年份
        18, "h:mm AM/PM",         // 时间12小时制
        19, "h:mm:ss AM/PM",      // 时间带秒
        20, "h:mm",               // 时间24小时制
        21, "h:mm:ss",            // 时间带秒24小时制
        22, "m/d/yy h:mm"         // 日期时间组合
    );
}
```

#### 3.2.3 StylesWriter完整实现

```java
package cn.itcraft.jxlsb.format;

public final class StylesWriter {
    private final List<NumberFormat> numberFormats;
    private final List<Font> fonts;
    private final List<Fill> fills;
    private final List<Border> borders;
    private final List<CellStyleFormat> cellStyleFormats;
    
    public StylesWriter() {
        this.numberFormats = new ArrayList<>();
        this.fonts = new ArrayList<>();
        this.fills = new ArrayList<>();
        this.borders = new ArrayList<>();
        this.cellStyleFormats = new ArrayList<>();
        
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // 添加默认字体
        fonts.add(Font.builder()
            .name("Calibri")
            .size(11)
            .color("000000")
            .build());
        
        // 添加默认填充
        fills.add(Fill.empty());
        fills.add(Fill.gray125());
        
        // 添加默认边框
        borders.add(Border.empty());
        
        // 添加默认样式格式
        cellStyleFormats.add(CellStyleFormat.builder()
            .numFmtId(0)
            .fontId(0)
            .fillId(0)
            .borderId(0)
            .build());
    }
    
    public int addDateFormat(String formatString) {
        // 查找内置格式
        for (Map.Entry<Integer, String> entry : BuiltInFormats.DATE_FORMATS.entrySet()) {
            if (entry.getValue().equals(formatString)) {
                return entry.getKey();
            }
        }
        
        // 添加自定义格式
        int customId = 164 + numberFormats.size();
        numberFormats.add(new NumberFormat(customId, formatString));
        
        // 创建样式格式
        CellStyleFormat styleFormat = CellStyleFormat.builder()
            .numFmtId(customId)
            .fontId(0)
            .fillId(0)
            .borderId(0)
            .build();
        
        cellStyleFormats.add(styleFormat);
        return cellStyleFormats.size() - 1;
    }
    
    public void writeTo(ZipOutputStream zipOut) throws IOException {
        ZipEntry entry = new ZipEntry("xl/styles.bin");
        zipOut.putNextEntry(entry);
        
        // 写入BIFF12样式记录
        writeStyleRecords(zipOut);
        
        zipOut.closeEntry();
    }
}
```

#### 3.2.4 SheetWriter样式应用

```java
public final class SheetWriter {
    private final StylesWriter stylesWriter;
    private int currentStyleId = 0;
    
    public void writeCell(int row, int col, double value, int styleId) throws IOException {
        // BrtCellReal记录
        writer.writeRecordHeader(Biff12RecordType.BRT_CELL_REAL);
        writer.writeInt(row);           // row
        writer.writeInt(col);           // col
        writer.writeInt(styleId);       // styleId (新增)
        writer.writeDouble(value);      // value
    }
    
    public void writeDateCell(int row, int col, long excelDate, 
                              String dateFormat) throws IOException {
        int styleId = stylesWriter.addDateFormat(dateFormat);
        writeCell(row, col, excelDateToDouble(excelDate), styleId);
    }
}
```

### 3.3 BIFF12样式记录

需要实现的样式相关记录：

| Record Type | Hex | Description |
|-------------|-----|-------------|
| BrtFmt | 0x002C | 数字格式 |
| BrtFont | 0x002B | 字体定义 |
| BrtFill | 0x002D | 填充定义 |
| BrtBorder | 0x002E | 边框定义 |
| BrtXF | 0x002F | 样式格式 |
| BrtStyle | 0x0030 | 样式名称 |
| BrtBeginStyleSheet | 0x0F72 | 样式表开始 |
| BrtEndStyleSheet | 0x0F73 | 样式表结束 |

---

## 四、模块3: 性能优化和压力测试

### 4.1 现状分析

**已有测试：**
- JMH基准测试（写入性能）
- 100K行性能对比
- FileSizeComparisonTest

**缺失测试：**
- 大文件压力测试（>100MB）
- 读取性能测试
- 内存泄漏验证
- GC监控

### 4.2 测试体系设计

#### 4.2.1 JMH基准测试完善

```java
package cn.itcraft.jxlsb.perf;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(3)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class XlsbBenchmark {
    
    @Param({"10000", "100000", "1000000"})
    int rowCount;
    
    @Param({"10", "50", "100"})
    int columnCount;
    
    @Param({"number", "text", "mixed"})
    String dataType;
    
    private OffHeapAllocator allocator;
    private Path outputPath;
    
    @Setup(Level.Trial)
    void setup() throws IOException {
        allocator = AllocatorFactory.createDefaultAllocator();
        outputPath = Files.createTempFile("benchmark", ".xlsb");
    }
    
    @Benchmark
    public void writeNumbers(Blackhole bh) throws IOException {
        XlsbWriter writer = XlsbWriter.builder()
            .path(outputPath)
            .allocator(allocator)
            .build();
        
        writer.writeBatch("Sheet1", (row, col) -> 
            CellData.number(row * col + Math.random()),
            rowCount, columnCount
        );
        
        bh.consume(writer);
        writer.close();
    }
    
    @Benchmark
    public void writeText(Blackhole bh) throws IOException {
        XlsbWriter writer = XlsbWriter.builder()
            .path(outputPath)
            .allocator(allocator)
            .build();
        
        writer.writeBatch("Sheet1", (row, col) -> 
            CellData.text("Cell-" + row + "-" + col),
            rowCount, columnCount
        );
        
        bh.consume(writer);
        writer.close();
    }
    
    @Benchmark
    public void writeMixed(Blackhole bh) throws IOException {
        XlsbWriter writer = XlsbWriter.builder()
            .path(outputPath)
            .allocator(allocator)
            .build();
        
        writer.writeBatch("Sheet1", (row, col) -> {
            if (col % 3 == 0) {
                return CellData.number(row * col);
            } else if (col % 3 == 1) {
                return CellData.text("Text-" + row);
            } else {
                return CellData.date(System.currentTimeMillis());
            }
        }, rowCount, columnCount);
        
        bh.consume(writer);
        writer.close();
    }
    
    @Benchmark
    public void readNumbers(Blackhole bh) throws IOException {
        XlsbReader reader = XlsbReader.builder()
            .path(outputPath)
            .build();
        
        int count = 0;
        reader.forEachRow(0, (row, col, data) -> {
            count++;
        });
        
        bh.consume(count);
        reader.close();
    }
}
```

#### 4.2.2 压力测试

```java
package cn.itcraft.jxlsb.perf;

public class StressTest {
    
    @Test
    void testMillionRows() throws IOException {
        Path path = Files.createTempFile("million", ".xlsb");
        
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        
        XlsbWriter writer = XlsbWriter.builder()
            .path(path)
            .build();
        
        writer.writeBatch("Sheet1", (row, col) -> 
            CellData.number(row + col * 0.1),
            1000000, 10
        );
        
        writer.close();
        
        long endTime = System.currentTimeMillis();
        long endMemory = getUsedMemory();
        long fileSize = Files.size(path);
        
        System.out.println("1M rows test:");
        System.out.println("  Time: " + (endTime - startTime) + " ms");
        System.out.println("  Memory delta: " + (endMemory - startMemory) + " MB");
        System.out.println("  File size: " + (fileSize / 1024 / 1024) + " MB");
        
        assertTrue(fileSize < 50 * 1024 * 1024, "File too large");
        assertTrue(endMemory - startMemory < 100, "Memory leak detected");
    }
    
    @Test
    void testMemoryLeak() throws IOException {
        Path path = Files.createTempFile("leak-test", ".xlsb");
        
        // 创建测试文件
        XlsbWriter writer = XlsbWriter.builder().path(path).build();
        writer.writeBatch("Sheet1", (r, c) -> CellData.number(r), 10000, 10);
        writer.close();
        
        long initialMemory = getUsedMemory();
        
        // 循环读写100次
        for (int i = 0; i < 100; i++) {
            XlsbReader reader = XlsbReader.builder().path(path).build();
            reader.forEachRow(0, (row, col, data) -> {});
            reader.close();
        }
        
        long finalMemory = getUsedMemory();
        long delta = finalMemory - initialMemory;
        
        System.out.println("Memory leak test:");
        System.out.println("  Initial: " + initialMemory + " MB");
        System.out.println("  Final: " + finalMemory + " MB");
        System.out.println("  Delta: " + delta + " MB");
        
        assertTrue(delta < 10, "Memory leak: " + delta + " MB");
    }
    
    @Test
    void testLargeText() throws IOException {
        Path path = Files.createTempFile("large-text", ".xlsb");
        String largeText = "A".repeat(50000);
        
        XlsbWriter writer = XlsbWriter.builder().path(path).build();
        writer.writeBatch("Sheet1", (r, c) -> 
            CellData.text(largeText),
            100, 10
        );
        writer.close();
        
        XlsbReader reader = XlsbReader.builder().path(path).build();
        reader.forEachRow(0, (row, col, data) -> {
            assertEquals(largeText, data.asText());
        });
        reader.close();
    }
    
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }
}
```

#### 4.2.3 GC监控测试

```java
package cn.itcraft.jxlsb.perf;

public class GCMonitorTest {
    
    @Test
    void testGCPressure() throws IOException {
        // 启用GC日志
        // -Xlog:gc*:file=gc.log
        
        List<GarbageCollectorMXBean> gcBeans = 
            ManagementFactory.getGarbageCollectorMXBeans();
        
        long initialGcCount = gcBeans.stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionCount)
            .sum();
        
        Path path = Files.createTempFile("gc-test", ".xlsb");
        
        XlsbWriter writer = XlsbWriter.builder().path(path).build();
        writer.writeBatch("Sheet1", (r, c) -> CellData.number(r), 100000, 10);
        writer.close();
        
        long finalGcCount = gcBeans.stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionCount)
            .sum();
        
        long gcDelta = finalGcCount - initialGcCount;
        
        System.out.println("GC pressure test:");
        System.out.println("  GC count delta: " + gcDelta);
        
        assertTrue(gcDelta < 5, "Too many GC: " + gcDelta);
    }
}
```

### 4.3 性能优化策略

#### 4.3.1 内存池优化

```java
public final class MemoryPool {
    private static final long[] SIZE_CLASSES = {
        64L,        // 64B - 小单元格
        4 * 1024L,  // 4KB - 中单元格
        64 * 1024L, // 64KB - 大文本
        1024 * 1024L, // 1MB - 批量写入
        16 * 1024 * 1024L // 16MB - 大块数据
    };
    
    private final AtomicInteger poolHits = new AtomicInteger(0);
    private final AtomicInteger poolMisses = new AtomicInteger(0);
    
    public MemoryBlock acquire(long size) {
        long classSize = findSizeClass(size);
        Queue<MemoryBlock> queue = pool.get(classSize);
        
        if (queue != null) {
            MemoryBlock block = queue.poll();
            if (block != null) {
                poolHits.incrementAndGet();
                return block;
            }
        }
        
        poolMisses.incrementAndGet();
        return allocator.allocate(classSize);
    }
    
    public double getHitRate() {
        int hits = poolHits.get();
        int total = hits + poolMisses.get();
        return total > 0 ? (double) hits / total : 0.0;
    }
}
```

#### 4.3.2 SST优化

```java
public final class SharedStringsTable {
    private final Object2IntOpenHashMap<String> stringToIndex;
    private final List<String> strings;
    private final Int2ObjectOpenHashMap<MemoryBlock> largeTextBlocks;
    
    private static final int LARGE_TEXT_THRESHOLD = 8192;
    
    public int addString(String text) {
        int existingIndex = stringToIndex.getOrDefault(text, -1);
        if (existingIndex >= 0) {
            return existingIndex;
        }
        
        int newIndex = strings.size();
        stringToIndex.put(text, newIndex);
        
        if (text.length() > LARGE_TEXT_THRESHOLD) {
            MemoryBlock block = allocator.allocate(text.length() * 2);
            block.putString(0, text);
            largeTextBlocks.put(newIndex, block);
        } else {
            strings.add(text);
        }
        
        return newIndex;
    }
}
```

---

## 五、模块4: 文档和示例

### 5.1 文档结构

```
docs/
├── README.md (完善)
│   ├── 项目介绍
│   ├── 快速开始
│   ├── API概览
│   ├── 性能对比
│   └── 限制说明
│
├── api-guide.md
│   ├── XlsbWriter详细示例
│   ├── XlsbReader详细示例
│   ├── 流式处理
│   ├── 样式支持
│   └── 错误处理
│
├── performance.md
│   ├── 性能优化建议
│   ├── 内存配置
│   ├── 大文件处理
│   └── GC优化
│
├── best-practices.md
│   ├── 批量写入
│   ├── 内存池使用
│   ├── 多线程处理
│   └── 常见陷阱
│
└── examples/
    ├── BasicWriteExample.java
    ├── StreamWriteExample.java
    ├── ReadExample.java
    ├── StyleExample.java
    └── PerformanceExample.java
```

### 5.2 API使用示例

#### 5.2.1 基础写入示例

```java
package cn.itcraft.jxlsb.examples;

public class BasicWriteExample {
    public static void main(String[] args) throws IOException {
        Path path = Paths.get("output.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(path)
                .build()) {
            
            // 创建Sheet
            OffHeapSheet sheet = writer.createSheet("数据表", 1000, 10);
            
            // 写入数据
            for (int row = 0; row < 1000; row++) {
                OffHeapRow currentRow = sheet.createRow(row);
                for (int col = 0; col < 10; col++) {
                    OffHeapCell cell = currentRow.getCell(col);
                    if (col == 0) {
                        cell.setText("行-" + row);
                    } else {
                        cell.setNumber(row * col + Math.random());
                    }
                }
            }
            
            writer.writeSheet(sheet);
        }
        
        System.out.println("文件写入完成: " + path);
    }
}
```

#### 5.2.2 流式写入示例

```java
package cn.itcraft.jxlsb.examples;

public class StreamWriteExample {
    public static void main(String[] args) throws IOException {
        Path path = Paths.get("large-output.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(path)
                .build()) {
            
            // 批量写入（适用于大数据）
            writer.writeBatch("Sheet1", (row, col) -> {
                // 数据供应函数
                switch (col % 4) {
                    case 0:
                        return CellData.number(row * col);
                    case 1:
                        return CellData.text("数据-" + row);
                    case 2:
                        return CellData.date(System.currentTimeMillis());
                    case 3:
                        return CellData.bool(row % 2 == 0);
                    default:
                        return CellData.blank();
                }
            }, 100000, 50);
        }
        
        System.out.println("大文件写入完成: " + path);
    }
}
```

#### 5.2.3 读取示例

```java
package cn.itcraft.jxlsb.examples;

public class ReadExample {
    public static void main(String[] args) throws IOException {
        Path path = Paths.get("input.xlsb");
        
        try (XlsbReader reader = XlsbReader.builder()
                .path(path)
                .build()) {
            
            // 遍历所有Sheet
            reader.forEachSheet((sheetInfo, sheetReader) -> {
                System.out.println("Sheet: " + sheetInfo.getName());
                
                // 流式读取行
                sheetReader.readRows(new RowHandler() {
                    @Override
                    public void onRowStart(int rowIndex, int columnCount) {
                        System.out.println("行 " + rowIndex + " (" + columnCount + " 列)");
                    }
                    
                    @Override
                    public void onCellNumber(int row, int col, double value) {
                        System.out.println("  [" + col + "] 数字: " + value);
                    }
                    
                    @Override
                    public void onCellText(int row, int col, String value) {
                        System.out.println("  [" + col + "] 文本: " + value);
                    }
                    
                    @Override
                    public void onCellBoolean(int row, int col, boolean value) {
                        System.out.println("  [" + col + "] 布尔: " + value);
                    }
                });
            });
        }
    }
}
```

#### 5.2.4 样式示例

```java
package cn.itcraft.jxlsb.examples;

public class StyleExample {
    public static void main(String[] args) throws IOException {
        Path path = Paths.get("styled-output.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(path)
                .stylesWriter(new StylesWriter())  // 启用样式
                .build()) {
            
            // 添加日期格式
            int dateStyleId = writer.addDateFormat("yyyy-mm-dd");
            
            // 添加数字格式
            int numberStyleId = writer.addNumberFormat("#,##0.00");
            
            OffHeapSheet sheet = writer.createSheet("样式示例", 100, 5);
            
            for (int row = 0; row < 100; row++) {
                OffHeapRow currentRow = sheet.createRow(row);
                
                // 应用样式
                currentRow.getCell(0).setNumber(row, numberStyleId);
                currentRow.getCell(1).setDate(System.currentTimeMillis(), dateStyleId);
                currentRow.getCell(2).setText("文本");
            }
            
            writer.writeSheet(sheet);
        }
        
        System.out.println("带样式文件写入完成: " + path);
    }
}
```

### 5.3 最佳实践文档

#### 5.3.1 内存优化建议

```markdown
## 内存优化最佳实践

### 1. 使用内存池

默认情况下，jxlsb使用内存池管理堆外内存。建议：

- 对于频繁读写场景，使用独立的allocator避免内存池竞争
- 监控内存池命中率，调整SIZE_CLASSES配置

### 2. 大文件处理

处理GB级文件时：

- 使用流式API避免全量加载
- 调整JVM参数：`-Xmx1g -XX:MaxDirectMemorySize=2g`
- 使用try-with-resources确保内存释放

### 3. SST优化

对于大量重复文本：

- 优先使用SST（自动）
- 对于超长文本（>32KB），考虑拆分

### 4. GC调优

推荐JVM参数：

```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UnlockExperimentalVMOptions
-XX:G1NewSizePercent=20
-XX:G1MaxNewSizePercent=30
-XX:MaxDirectMemorySize=4g
```
```

---

## 六、并行开发策略

### 6.1 任务分配

| 模块 | 子代理 | 主要任务 | 依赖关系 |
|------|--------|----------|----------|
| XlsbReader | subagent-reader | ZIP解析、记录解析、流式API | 无依赖 |
| 样式支持 | subagent-styles | 样式系统、格式注册、写入集成 | 无依赖 |
| 性能测试 | subagent-perf | JMH完善、压力测试、GC监控 | 无依赖 |
| 文档示例 | subagent-docs | README、API指南、示例代码 | 无依赖 |

### 6.2 并行执行流程

```
┌─────────────────────────────────────────────────┐
│                  Main Session                    │
│                                                  │
│  ┌────────────────┐ ┌────────────────┐          │
│  │ subagent-reader│ │ subagent-styles│          │
│  │                │ │                │          │
│  │ - ZIP解析      │ │ - 样式系统     │          │
│  │ - BIFF12记录   │ │ - 格式注册     │          │
│  │ - 流式API      │ │ - 写入集成     │          │
│  └────────────────┘ └────────────────┘          │
│                                                  │
│  ┌────────────────┐ ┌────────────────┐          │
│  │ subagent-perf  │ │ subagent-docs  │          │
│  │                │ │                │          │
│  │ - JMH基准      │ │ - README       │          │
│  │ - 压力测试     │ │ - API指南      │          │
│  │ - GC监控       │ │ - 示例代码     │          │
│  └────────────────┘ └────────────────┘          │
│                                                  │
│  ┌─────────────────────────────────────┐        │
│  │         合并测试验证                 │        │
│  │  - 整合测试                          │        │
│  │  - 性能对比                          │        │
│  │  - 文档一致性                        │        │
│  └─────────────────────────────────────┘        │
└─────────────────────────────────────────────────┘
```

### 6.3 验收标准

**Reader模块：**
- 能正确读取Excel生成的XLSB文件
- 支持数字、文本、布尔、日期类型
- 流式API内存占用稳定

**Styles模块：**
- 日期格式能正确显示
- 自定义格式支持
- styles.bin符合规范

**Performance模块：**
- JMH基准测试覆盖所有场景
- 1M行压力测试通过
- 内存泄漏测试通过
- GC次数<5次/GB文件

**Documentation模块：**
- README完整清晰
- API示例可运行
- 最佳实践文档完整

---

## 七、风险评估

### 7.1 技术风险

| 风险项 | 级别 | 缓解措施 |
|--------|------|----------|
| BIFF12规范复杂 | 高 | 严格遵循MS-XLSB文档，单元测试覆盖 |
| ZIP解析性能 | 中 | 使用FileChannel零拷贝，内存池优化 |
| 样式兼容性 | 中 | 使用Excel验证生成文件 |
| 内存泄漏 | 高 | 堆外内存严格管理，压力测试验证 |
| 并行开发冲突 | 中 | 独立模块，最后合并测试 |

### 7.2 时间估算

| 模块 | 估算时间 | 说明 |
|------|----------|------|
| XlsbReader | 4-6小时 | ZIP解析+记录类型+API |
| 样式支持 | 2-4小时 | 样式系统+格式注册 |
| 性能测试 | 2-3小时 | JMH完善+压力测试 |
| 文档示例 | 1-2小时 | 文档编写+示例代码 |
| 合并验证 | 1-2小时 | 整合测试+性能对比 |
| **总计** | **10-17小时** | 并行执行 |

---

## 八、成功标准

### 8.1 功能完整性

- ✅ XlsbReader能读取Excel/WPS生成的XLSB
- ✅ 样式支持日期格式显示
- ✅ 性能测试覆盖完整
- ✅ 文档和示例完整

### 8.2 性能目标

| 指标 | 目标值 | 验证方法 |
|------|--------|----------|
| 读取速度 | >30MB/s | JMH基准测试 |
| 写入速度 | >30MB/s | JMH基准测试 |
| 内存占用 | <50MB/1M行 | 堆外内存监控 |
| GC次数 | <5次/GB | GC监控测试 |

### 8.3 质量标准

- 测试覆盖率 > 80%
- 所有单元测试通过
- 生成的XLSB文件Excel/WPS兼容
- 内存泄漏测试通过
- 文档完整清晰

---

## 九、后续规划

完成本设计后，将进入实施阶段：

1. **调用writing-plans skill** - 创建详细实施计划
2. **启动4个子代理** - 并行开发各模块
3. **合并验证** - 整合测试和性能对比
4. **代码审查** - 确保质量
5. **发布准备** - 文档完善和版本发布

---

**设计文档版本：** 1.0
**设计日期：** 2026-04-10
**设计者：** AI架构师