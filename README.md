# jxlsb - Java XLSB Library

纯Java实现的XLSB（Excel Binary Workbook）格式读写库。

## 特性

- **零依赖**：仅依赖SLF4J，无需POI等重型库
- **堆外内存**：全量堆外内存架构，零GC压力
- **高性能**：比POI快3x，比EasyExcel快2.5x，文件小35-50%
- **企业级**：Java 8+支持，Multi-Release JAR（Java 23+自动使用Foreign Memory API）

## 性能数据

**100K行 × 10列：**

| 库 | 文件大小 | 写入时间 | 格式 |
|---|---|---|---|
| **jxlsb** | **2.72 MB** | **453 ms** | XLSB |
| FastExcel | 5.42 MB | 521 ms | XLSX |
| EasyExcel | 4.21 MB | 1121 ms | XLSX |
| POI | 4.16 MB | 1528 ms | XLSX |

**1M行 × 10列：**

| 库 | 文件大小 | 写入时间 | 格式 |
|---|---|---|---|
| **jxlsb** | **26.71 MB** | **4647 ms** | XLSB |
| FastExcel | 55.00 MB | 4621 ms | XLSX |
| EasyExcel | 42.54 MB | 9405 ms | XLSX |
| POI | 42.25 MB | 8334 ms | XLSX |

## API 场景适配

### 写入 API

| API | 适用场景 | 数据来源 | 内存压力 | 示例 |
|---|---|---|---|---|
| **writeBatch** | 计算报表、内存数据导出 | 内存已有 / 实时计算 | 无 | 函数式一次性写入 |
| **startSheet + writeRows + endSheet** | 数据库分页查询、大文件流式处理 | DB分页 / 文件流 | 低 | 分批追加写入 |
| **template + fillBatch/fillAtMarker** | 模板填充、报表生成 | 模板 + 数据 | 无 | 保留模板样式 |

### 模板填充 API

基于XLSB模板填充数据，保留模板所有内容（样式、合并单元格等）：

```java
// 创建模板填充Writer
XlsbWriter writer = XlsbWriter.builder()
    .template(Paths.get("template.xlsb"))  // 模板路径
    .path(Paths.get("output.xlsb"))        // 输出路径
    .build();

// 方式1: 固定位置填充
List<List<Object>> data = Arrays.asList(
    Arrays.asList("张三", "北京", 25, "男"),
    Arrays.asList("李四", "上海", 30, "女")
);
writer.fillBatch(0, data, 4, 2);  // sheetIndex, dataList, startRow, startCol

// 方式2: 标记查找填充
writer.fillAtMarker("${data}", data);  // 查找${data}标记位置填充

// 方式3: 流式填充
writer.startFill(0, 12, 8);
writer.fillRows(batch1);
writer.fillRows(batch2);
writer.endFill();

writer.close();
```

**模板支持范围**：
- ✅ 保留模板所有内容：styles.bin、theme、静态文本等
- ✅ 保留单元格样式：字体、边框、填充、对齐
- ✅ 保留合并单元格
- ✅ 支持标记查找填充（如`${data}`）
- ⚠️ **仅支持表头模板**：数据从指定位置向下填充，不支持头尾都有模板
- ❌ 不支持尾部模板：填充数据后无法保留底部静态内容

### 读取 API

| API | 适用场景 | 数据量 | 示例 |
|---|---|---|---|
| **forEachRow** | 流式处理、数据清洗 | 任意 | 回调处理每行 |
| **readRows** | 分页读取、批量处理 | 大文件 | List/Array批量返回 |

### 场景选择指南

**写入场景**：

```java
// 场景1: 内存数据导出（推荐 writeBatch）
List<Product> products = cache.getAll(); // 已在内存
writer.writeBatch("Products", (row, col) -> toCell(products.get(row), col), products.size(), 5);

// 场景2: 数据库分页导出（推荐 writeRows 流式追加）
writer.startSheet("Orders", 5);
int offset = 0;
while (true) {
    List<Order> batch = db.query(offset, 1000); // 分页查询，避免OOM
    if (batch.isEmpty()) break;
    writer.writeRows(batch, offset, (order, col) -> toCell(order, col));
    offset += batch.size();
}
writer.endSheet();
```

**读取场景**：

```java
// 场景1: 流式处理（推荐 forEachRow）
reader.forEachRow(0, new RowConsumer() {
    void onCell(int row, int col, CellData data) {
        // 直接处理，无需存储
        processCell(data);
    }
});

// 场景2: 分页批量处理（推荐 readRows）
int offset = 0;
while (true) {
    List<CellData[]> batch = reader.readRows(0, offset, 1000);
    if (batch.isEmpty()) break;
    batchProcess(batch); // 批量处理1000行
    offset += 1000;
}
```

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jxlsb</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 写入示例

```java
import cn.itcraft.jxlsb.api.*;
import java.nio.file.Paths;

// 一次性写入（内存数据）
try (XlsbWriter writer = XlsbWriter.builder().path(Paths.get("output.xlsb")).build()) {
    writer.writeBatch("Sheet1", (row, col) -> CellData.number(row * col), 1000, 10);
}

// 分页追加写入（数据库查询）
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

### 读取示例

```java
import cn.itcraft.jxlsb.api.*;

try (XlsbReader reader = XlsbReader.builder().path(Paths.get("data.xlsb")).build()) {
    // 流式处理
    reader.forEachRow(0, new RowConsumer() {
        void onCell(int row, int col, CellData data) {
            System.out.println(row + "," + col + ": " + data.getValue());
        }
    });
    
    // 分页批量读取
    int offset = 0;
    while (true) {
        List<CellData[]> batch = reader.readRows(0, offset, 1000);
        if (batch.isEmpty()) break;
        // 处理batch
        offset += batch.size();
    }
}
```

### 单元格类型

```java
CellData.text("Hello")       // 文本
CellData.number(3.14159)     // 数字
CellData.date(timestamp)     // 日期（毫秒时间戳）
CellData.bool(true)          // 布尔
CellData.blank()             // 空白

// 数字格式（支持百分比、千分位、负红、货币等）
CellData.percentage(0.1234)           // 0.00%
CellData.numberWithComma(1234567.89)  // #,##0.00
CellData.numberNegativeRed(-1234.56)  // #,##0.00;[Red]-#,##0.00
CellData.currency(1234.56)            // ￥#,##0.00
CellData.time(timestamp)              // h:mm:ss
```

## 功能状态

| 功能 | 状态 | 说明 |
|---|---|---|
| 数字单元格 | ✅ 完整 | 支持整数、浮点数 |
| 文本单元格 | ✅ 完整 | SST优化，大文本支持 |
| 布尔单元格 | ✅ 完整 | |
| 日期单元格 | ✅ 完整 | Excel日期序列号 |
| 空白单元格 | ✅ 完整 | |
| 样式系统 | ✅ 完整 | 字体、边框、填充、对齐 |
| 数字格式 | ✅ 完整 | 自定义格式字符串 |
| 流式写入 | ✅ 完整 | startSheet/writeRows/endSheet |
| 流式读取 | ✅ 完整 | forEachRow回调 |
| 分页读取 | ✅ 完整 | readRows批量返回 |
| 模板填充 | ✅ 完整 | fillBatch/fillAtMarker/startFill |
| 合并单元格 | ✅ 完整 | 模板中合并单元格保留 |
| 公式 | ❌ 不支持 | |
| 图表 | ❌ 不支持 | |
| 条件格式 | ❌ 不支持 | |
| 宏/VBA | ❌ 不支持 | |

## 生产就绪评估

**推荐场景**：
- ✅ 大数据量Excel导出（100K-1M行）
- ✅ 数据库分页查询导出
- ✅ 存储成本敏感（文件小50%）
- ✅ 内存受限环境（堆外内存）
- ✅ 模板报表生成（保留样式、合并单元格）

**不推荐场景**：
- ❌ 需要公式、图表
- ❌ 需要条件格式
- ❌ 需要头尾都有模板的报表（仅支持表头模板）

## 架构

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

## 测试覆盖

- **110个测试全部通过**
- 内存层：分配、读写、关闭、泄漏检测
- 格式层：BIFF12记录、VarInt编码
- API层：写入、读取、流式追加
- 性能测试：100K/1M行对比

## License

Apache License 2.0