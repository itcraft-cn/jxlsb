**中文** | [English](CHANGELOG_en.md)

# 变更日志

本项目所有值得注意的变更都记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [未发布]

### 新增

- **行列上限溢出策略**（与 rxlsb 对齐）：`Limits.MAX_COLUMNS = 16384`、`MAX_ROWS = 1048576`；
  `Limits.setColumnOverflow/setRowOverflow/setOverflow` 全局配置，`OverflowBehavior.{ERROR, CUT}`；
  每次写入通过 `SheetLimits` 快照，超限抛 `LimitExceededException` 或截断
- 写入器 ZIP 压缩级别可配置：`XlsbWriter.Builder.compressionLevel(int)`，默认 **1（速度优先）**

### 修复

- 代码审查问题修复：复用 `decodeRk`、简化加锁、截断告警、魔数常量化、资源管理
- 空白单元格不再写入 `BrtCellBlank`，避免文件膨胀与 WPS 解析异常
- 流式写入的 `BrtWsDim.last_row` 改为前导占位、结束时按偏移回填，不再因维度为 0 导致只显示部分行
- RK 编码改为标准整数形式 `(n << 2) | 0x02`，修复大整数（如 536870911）被误判为 fInt/fX100 的读写错误；
  `decodeRk` 标志位对齐 MS-XLSB（bit1=fInt，bit0=fX100）
- 模板填充统一受行列上限约束（维度裁剪、超限单元格/填充列跳过）

### 新增

- 各模块需求文档（API / format / container / data / 基础设施，含全局汇总）
- 代码审查报告（2026-05-12）
- 1M 行性能基准测试

### 变更

- 文档：新增「设计定位」章节
- 构建：GPG 升级至 3.2.7
- 版本：进入 1.2.0-dev

## [1.1.0] - 2026-04-20

### 修复

- **readRows 缓冲区边界缺陷**：靠近批次边界时文本单元格（BrtCellIsst）被错误跳过
  - 根因：`handleBrtCellIsst` 使用 `buffer.length`（固定 16384）而非 `recordSize` 做边界判断
  - 当偏移接近缓冲区末尾时，文本单元格（第 0/1 列）丢失，导致测试出现 NullPointerException
  - 修复：使用 `recordSize` 参数，因上层 `processSheetBuffer` 已保证数据完整性
- **SheetReader.java**：`handleBrtRowHdr` 与 `handleBrtCellIsst` 改用 `size` 而非 `buffer.length`
- **VarIntReader.java**：移除返回默认值的错误边界判断
- **SharedStringsTable.java**：`processBuffer` 现在返回已处理位置，`load` 正确处理剩余数据

### 新增

- **Java 8 兼容**：
  - `IoUtils.java` 工具类，替代 `readAllBytes()`
  - 测试文件改用 `Paths.get()` 替代 `Path.of()`（Java 9+）
  - pom.xml profile 机制用于 JDK23 编译（JDK8 环境跳过）

## [1.0.0] - 2026-04-16

### 新增

**核心写入功能**
- 数字单元格（整数、浮点）
- 文本单元格，SST 优化
- 布尔单元格
- 日期单元格（Excel 日期序列号）
- 空白单元格
- 单元格样式系统（字体、边框、填充、对齐）
- 数字格式支持（百分比、千分位、负红、货币、日期、时间）

**写入 API**
- `writeBatch(sheetName, supplier, rowCount, columnCount)` — 一次性批量写入
- `startSheet(sheetName, columnCount)` + `writeRows()` + `endSheet()` — 流式追加写入
- 基于模板的写入 `XlsbWriter.builder().template(path).path(output).build()`

**模板填充 API**
- `fillBatch(sheetIndex, dataList, startRow, startCol)` — 固定位置填充
- `fillAtMarker("${marker}", dataList)` — 标记查找填充
- `startFill()` + `fillRows()` + `endFill()` — 流式填充
- 保留模板样式、合并单元格及全部内容

**读取功能**
- 流式读取 `forEachRow(sheetIndex, rowConsumer)` 回调
- 分页读取 `readRows(sheetIndex, offset, limit)` 批量返回
- 共享字符串表（SST）支持
- BIFF12 格式解析（BrtCellRk、BrtCellReal、BrtCellSt、BrtCellBool、BrtCellBlank、BrtCellIsst）

**内存管理**
- 全量堆外内存架构（零 GC 压力）
- ByteBufferAllocator（Java 8-22）
- MemorySegmentAllocator（Java 23+，Foreign Memory API）
- 基于 SPI 的分配器提供者机制
- 按大小分桶的内存池（64B/4KB/64KB/1MB/16MB）

**Multi-Release JAR**
- 三种产物变体：
  - `jxlsb-1.0.0.jar`（默认，JDK 8）
  - `jxlsb-1.0.0-jdk8.jar`（classifier: jdk8，JDK 8）
  - `jxlsb-1.0.0-jdk23.jar`（classifier: jdk23，JDK 23+）
- 根据 JVM 版本自动选择分配器

**性能**
- 比 Apache POI 快 2-3 倍
- 比 EasyExcel 快 2-2.5 倍
- 速度与 FastExcel 相当
- 文件体积比 XLSX 小 35-50%
- 256KB zip 缓冲优化
- SST 并发优化

### 性能基准

**100K 行 × 10 列：**
| 库 | 文件大小 | 写入时间 |
|---------|-----------|------------|
| jxlsb | 2.72 MB | 453 ms |
| FastExcel | 5.42 MB | 521 ms |
| EasyExcel | 4.21 MB | 1121 ms |
| POI | 4.16 MB | 1528 ms |

**1M 行 × 10 列：**
| 库 | 文件大小 | 写入时间 |
|---------|-----------|------------|
| jxlsb | 26.71 MB | 4647 ms |
| FastExcel | 55.00 MB | 4621 ms |
| EasyExcel | 42.54 MB | 9405 ms |
| POI | 42.25 MB | 8334 ms |

### 依赖

- SLF4J API（唯一依赖）
- 无 Apache POI、无 EasyExcel、无其他重型库

### 测试覆盖

- 130 个单元测试全部通过
- 内存泄漏检测测试
- 性能对比测试
- API 集成测试
- 模板填充测试

### 不支持

- 公式单元格
- 图表对象
- 条件格式
- 宏/VBA
- 尾部模板保留（仅支持表头模板）

### 代码质量

- 从代码审查中解决 P0-P2 问题
- XML 模板替换使用预编译 Pattern
- 实现 AutoCloseable 进行资源管理
- 异常类补充 serialVersionUID
- 抽取 EncryptionUtils 消除重复代码

### 文档

- README.md（中文） / README_en.md（英文）
- CHANGELOG.md（中文） / CHANGELOG_en.md（英文）
- MANUAL.md（中文） / MANUAL_en.md（英文）
- AGENTS.md（项目指南）
- MEMORY.md（本地开发笔记，不纳入版本管理）
