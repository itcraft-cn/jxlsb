**中文** | [English](MANUAL_en.md)

# jxlsb 使用手册

> 本手册面向使用 jxlsb 的开发者，覆盖环境要求、依赖引入、构建、API 用法与常见场景。
> 特性概览见 [README.md](README.md)，版本历史见 [CHANGELOG.md](CHANGELOG.md)。

## 1. 简介

jxlsb 是纯 Java 实现的 XLSB（Excel Binary Workbook）读写库。设计出发点固定为**高性能数据输出（写入）**；读取最初只用于读取模板，读能力完备后自然延伸出高性能读。

## 2. 环境要求

| 项 | 要求 |
|----|------|
| JDK | 8+（Java 23+ 自动切换到 Foreign Memory API） |
| 构建工具 | Maven |
| 运行时依赖 | SLF4J（仅日志） |

## 3. 安装与构建

### 3.1 Maven 依赖

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jxlsb</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3.2 源码构建

```bash
mvn clean package          # 打包
mvn test                   # 运行测试
./run_benchmarks.sh        # 运行性能基准
```

## 4. API 用法

### 4.1 写入

**一次性批量写入（内存中已有数据）**

```java
try (XlsbWriter writer = XlsbWriter.builder().path(Paths.get("output.xlsb")).build()) {
    writer.writeBatch("Sheet1", (row, col) -> CellData.number(row * col), 1000, 10);
}
```

**流式追加写入（数据库分页，避免 OOM）**

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

### 4.2 读取

**流式逐行处理**

```java
try (XlsbReader reader = XlsbReader.builder().path(Paths.get("data.xlsb")).build()) {
    reader.forEachRow(0, new RowConsumer() {
        public void onCell(int row, int col, CellData data) {
            processCell(data);
        }
    });
}
```

**分页批量读取**

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

### 4.3 模板填充

```java
XlsbWriter writer = XlsbWriter.builder()
    .template(Paths.get("template.xlsb"))
    .path(Paths.get("output.xlsb"))
    .build();

List<List<Object>> data = Arrays.asList(
    Arrays.asList("John", "NYC", 25, "M"),
    Arrays.asList("Jane", "LA", 30, "F")
);

// 方式一：固定位置填充
writer.fillBatch(0, data, 4, 2);   // sheetIndex, dataList, startRow, startCol

// 方式二：标记查找填充
writer.fillAtMarker("${data}", data);

// 方式三：流式填充
writer.startFill(0, 12, 8);
writer.fillRows(batch1);
writer.fillRows(batch2);
writer.endFill();

writer.close();
```

## 5. CellData 类型

```java
CellData.text("Hello")       // 文本
CellData.number(3.14159)     // 数字
CellData.date(timestamp)     // 日期（毫秒时间戳）
CellData.bool(true)          // 布尔
CellData.blank()             // 空白
```

## 6. 数字格式

```java
CellData.percentage(0.1234)           // 0.00%
CellData.numberWithComma(1234567.89)  // #,##0.00
CellData.numberNegativeRed(-1234.56)  // #,##0.00;[Red]-#,##0.00
CellData.currency(1234.56)            // $#,##0.00
CellData.time(timestamp)              // h:mm:ss
```

## 7. 场景选型

| 场景 | 推荐 API | 数据来源 | 内存压力 |
|------|----------|----------|----------|
| 报表、内存数据导出 | `writeBatch` | 内存/计算得出 | 无 |
| 数据库分页、大文件流式 | `startSheet` + `writeRows` + `endSheet` | 分页查询/文件流 | 低 |
| 模板填充、报表生成 | `template` + `fillBatch`/`fillAtMarker` | 模板 + 数据 | 无 |
| 流式清洗处理 | `forEachRow` | 任意 | 无 |
| 分页批处理 | `readRows` | 大文件 | 低 |

## 8. 性能

100K 行 × 10 列写入对比：

| 库 | 文件大小 | 写入时间 | 格式 |
|----|----------|----------|------|
| **jxlsb** | **2.72 MB** | **453 ms** | XLSB |
| FastExcel | 5.42 MB | 521 ms | XLSX |
| EasyExcel | 4.21 MB | 1121 ms | XLSX |
| POI | 4.16 MB | 1528 ms | XLSX |

## 9. 限制与不支持项

- 仅支持**表头模板**，不支持尾部模板
- 不支持公式、图表、条件格式、宏/VBA

## 10. 更多文档

- [README.md](README.md) — 特性概览与架构
- [CHANGELOG.md](CHANGELOG.md) — 版本历史
- `docs/api-guide.md` — API 深入指南
- `docs/best-practices.md` — 最佳实践
- `docs/performance.md` — 性能报告
