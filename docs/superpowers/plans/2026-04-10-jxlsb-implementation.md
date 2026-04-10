# jxlsb Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a pure Java XLSB (Excel Binary Workbook) reader/writer library using off-heap memory for zero-GC performance.

**Architecture:** Pure off-heap memory architecture with MemoryBlock abstraction, BIFF12 record parsing, streaming API with Builder pattern, and multi-JDK version support (Java 8 ByteBuffer+Unsafe, Java 17 MemorySegment).

**Tech Stack:** Java SDK (8/17), SLF4J, JUnit 5, Mockito, JMH, Maven

---

## Phase 1: Foundation - Memory Management Layer (2周)

### Task 1.1: Project Structure Setup

**目标:** 创建Maven项目结构和基础配置

**文件:**
- `pom.xml` - Maven配置，依赖管理
- `src/main/java/cn/itcraft/jxlsb/` - 源码目录结构
- `src/main/java17/cn/itcraft/jxlsb/` - Java 17源码目录
- `src/test/java/cn/itcraft/jxlsb/` - 测试目录
- `.gitignore` - Git忽略配置

**验收:**
- mvn compile成功
- 目录结构符合设计文档

- [ ] 创建pom.xml（参考设计文档第十一章）
- [ ] 创建目录结构
- [ ] 配置.gitignore
- [ ] git commit

---

### Task 1.2: MemoryBlock Interface

**目标:** 定义堆外内存块抽象接口

**文件:**
- `src/main/java/cn/itcraft/jxlsb/memory/MemoryBlock.java`
- `src/main/java/cn/itcraft/jxlsb/memory/MemoryAllocationException.java`
- `src/main/java/cn/itcraft/jxlsb/exception/XlsbException.java`
- `src/test/java/cn/itcraft/jxlsb/memory/MemoryBlockTest.java`
- `src/test/java/cn/itcraft/jxlsb/memory/MockMemoryBlock.java`

**方法:** TDD - 先写测试，再实现接口

**验收:**
- 接口定义完整（putByte/getByte/putInt/getInt等）
- 测试通过（使用MockMemoryBlock）
- 符合编码规范

- [ ] 写测试：MemoryBlock接口的基本读写操作
- [ ] 运行测试（预期失败）
- [ ] 实现MemoryBlock接口（参考设计文档第三章）
- [ ] 实现MockMemoryBlock用于测试
- [ ] 实现XlsbException和MemoryAllocationException
- [ ] 运行测试（预期通过）
- [ ] git commit

---

### Task 1.3: OffHeapAllocator Interface

**目标:** 定义堆外内存分配器接口

**文件:**
- `src/main/java/cn/itcraft/jxlsb/memory/OffHeapAllocator.java`
- `src/test/java/cn/itcraft/jxlsb/memory/MockAllocator.java`

**方法:** TDD - 先写测试，再实现接口

**验收:**
- allocate/allocateFromPool/getTotalAllocated等方法定义
- 测试通过

- [ ] 写测试：Allocator分配内存块
- [ ] 运行测试（预期失败）
- [ ] 实现OffHeapAllocator接口（参考设计文档第三章）
- [ ] 实现MockAllocator用于测试
- [ ] 运行测试（预期通过）
- [ ] git commit

---

### Task 1.4: ByteBuffer Allocator (Java 8)

**目标:** 实现Java 8堆外内存分配器（ByteBuffer + Unsafe）

**文件:**
- `src/main/java/cn/itcraft/jxlsb/memory/impl/ByteBufferAllocator.java`
- `src/main/java/cn/itcraft/jxlsb/memory/impl/ByteBufferMemoryBlock.java`
- `src/test/java/cn/itcraft/jxlsb/memory/impl/ByteBufferAllocatorTest.java`

**方法:** TDD - 先写测试，再实现

**验收:**
- allocate分配堆外内存
- putInt/getInt小端序正确
- 边界检查正确（IndexOutOfBoundsException）
- 测试通过

- [ ] 写测试：ByteBufferAllocator分配和读写
- [ ] 运行测试（预期失败）
- [ ] 实现ByteBufferAllocator（Unsafe获取ByteBuffer地址）
- [ ] 实现ByteBufferMemoryBlock（Unsafe直接内存访问）
- [ ] 运行测试（预期通过）
- [ ] git commit

---

### Task 1.5: Memory Pool

**目标:** 实现内存池，复用堆外内存块

**文件:**
- `src/main/java/cn/itcraft/jxlsb/memory/MemoryPool.java`
- `src/main/java/cn/itcraft/jxlsb/memory/PooledMemoryBlock.java`
- `src/test/java/cn/itcraft/jxlsb/memory/MemoryPoolTest.java`

**方法:** TDD - 先写测试，再实现

**验收:**
- acquire从池获取内存块
- release归还内存块
- 分段池策略（64B/4KB/64KB/1MB/16MB）
- 测试通过

- [ ] 写测试：MemoryPool获取和归还内存块
- [ ] 运行测试（预期失败）
- [ ] 实现MemoryPool（分段池策略）
- [ ] 实现PooledMemoryBlock包装类
- [ ] 运行测试（预期通过）
- [ ] git commit

---

### Task 1.6: Allocator Factory (ServiceLoader)

**目标:** 实现工厂类，通过ServiceLoader自动加载最优分配器

**文件:**
- `src/main/java/cn/itcraft/jxlsb/memory/AllocatorFactory.java`
- `src/main/java/cn/itcraft/jxlsb/spi/AllocatorProvider.java`
- `src/main/java/cn/itcraft/jxlsb/spi/impl/ByteBufferAllocatorProvider.java`
- `src/main/resources/META-INF/services/cn.itcraft.jxlsb.spi.AllocatorProvider`
- `src/test/java/cn/itcraft/jxlsb/memory/AllocatorFactoryTest.java`

**方法:** TDD - 先写测试，再实现

**验收:**
- ServiceLoader机制工作
- 自动选择优先级最高的Provider
- createDefaultAllocator返回正确分配器
- 测试通过

- [ ] 写测试：AllocatorFactory创建默认分配器
- [ ] 运行测试（预期失败）
- [ ] 实现AllocatorProvider SPI接口
- [ ] 实现ByteBufferAllocatorProvider
- [ ] 实现AllocatorFactory（ServiceLoader加载）
- [ ] 创建ServiceLoader配置文件
- [ ] 运行测试（预期通过）
- [ ] git commit

---

### Phase 1 验收检查点

**验收命令:**
```bash
mvnd clean test
mvnd spotbugs:check
mvnd pmd:check
```

**验收标准:**
- 所有Phase 1测试通过
- 无SpotBugs/PMD警告
- git已提交

---

## Phase 2: XLSB Binary Format Layer (3周)

### Task 2.1: BiffRecord Base Class

**目标:** 实现BIFF12记录基类

**文件:**
- `src/main/java/cn/itcraft/jxlsb/format/BiffRecord.java`
- `src/test/java/cn/itcraft/jxlsb/format/BiffRecordTest.java`

**验收:**
- 记录类型、大小、数据块属性
- AutoCloseable实现
- writeTo抽象方法

- [ ] 写测试
- [ ] 运行测试（失败）
- [ ] 实现BiffRecord基类（参考设计文档第五章）
- [ ] 运行测试（通过）
- [ ] git commit

---

### Task 2.2: RecordParser

**目标:** 实现记录解析器，解析BIFF12二进制格式

**文件:**
- `src/main/java/cn/itcraft/jxlsb/format/RecordParser.java`
- `src/main/java/cn/itcraft/jxlsb/format/RecordHandler.java`
- `src/main/java/cn/itcraft/jxlsb/format/BiffRecordFactory.java`
- `src/main/java/cn/itcraft/jxlsb/format/record/UnknownRecord.java`
- `src/test/java/cn/itcraft/jxlsb/format/RecordParserTest.java`

**验收:**
- parse单条记录
- parseStream流式解析
- RecordParseException异常处理

- [ ] 写测试：解析单条记录和流式解析
- [ ] 运行测试（失败）
- [ ] 实现RecordParser
- [ ] 实现RecordHandler函数式接口
- [ ] 实现BiffRecordFactory
- [ ] 实现UnknownRecord
- [ ] 运行测试（通过）
- [ ] git commit

---

### Task 2.3: RecordWriter

**目标:** 实现记录写入器，写入BIFF12二进制格式

**文件:**
- `src/main/java/cn/itcraft/jxlsb/format/RecordWriter.java`
- `src/test/java/cn/itcraft/jxlsb/format/RecordWriterTest.java`

**验收:**
- writeRecordHeader写入记录头
- writeBytes/writeInt写入数据
- 测试通过

- [ ] 写测试
- [ ] 运行测试（失败）
- [ ] 实现RecordWriter
- [ ] 运行测试（通过）
- [ ] git commit

---

### Task 2.4: Core Record Types (10+核心记录)

**目标:** 实现10+核心BIFF12记录类型

**文件:**
- `src/main/java/cn/itcraft/jxlsb/format/record/BeginSheetRecord.java`
- `src/main/java/cn/itcraft/jxlsb/format/record/EndSheetRecord.java`
- `src/main/java/cn/itcraft/jxlsb/format/record/BeginRowRecord.java`
- `src/main/java/cn/itcraft/jxlsb/format/record/EndRowRecord.java`
- `src/main/java/cn/itcraft/jxlsb/format/record/CellRecord.java`
- `src/main/java/cn/itcraft/jxlsb/format/record/StringRecord.java`
- `src/main/java/cn/itcraft/jxlsb/format/record/BeginBookRecord.java`
- `src/main/java/cn/itcraft/jxlsb/format/record/EndBookRecord.java`
- `src/main/java/cn/itcraft/jxlsb/format/record/VersionRecord.java`
- `src/main/java/cn/itcraft/jxlsb/format/record/IndexRecord.java`
- `src/test/java/cn/itcraft/jxlsb/format/record/`

**验收:**
- 每个记录类型正确解析和写入
- 参考[MS-XLSB]规范附录A
- 测试通过

- [ ] 写测试：BeginSheetRecord
- [ ] 实现BeginSheetRecord
- [ ] 写测试：EndSheetRecord
- [ ] 实现EndSheetRecord
- [ ] 写测试：BeginRowRecord/EndRowRecord
- [ ] 实现BeginRowRecord/EndRowRecord
- [ ] 写测试：CellRecord（核心）
- [ ] 实现CellRecord（数值/文本/布尔）
- [ ] 写测试：StringRecord
- [ ] 实现StringRecord
- [ ] 写测试：BeginBookRecord/EndBookRecord
- [ ] 实现BeginBookRecord/EndBookRecord
- [ ] 写测试：VersionRecord/IndexRecord
- [ ] 实现VersionRecord/IndexRecord
- [ ] 运行所有测试
- [ ] git commit

---

### Task 2.5: XLSB Format Integration Test

**目标:** 集成测试，解析真实XLSB文件

**文件:**
- `src/test/resources/test-data/small.xlsb` - 小测试文件
- `src/test/resources/test-data/medium.xlsb` - 中测试文件
- `src/test/java/cn/itcraft/jxlsb/format/XlsbFormatIntegrationTest.java`

**验收:**
- 解析真实XLSB文件成功
- 记录类型识别正确
- 测试通过

- [ ] 创建测试数据文件（用Excel生成）
- [ ] 写集成测试：解析small.xlsb
- [ ] 写集成测试：解析medium.xlsb
- [ ] 运行测试（通过）
- [ ] git commit

---

### Phase 2 验收检查点

**验收命令:**
```bash
mvnd clean test
mvnd spotbugs:check
```

---

## Phase 3: Data Structure Layer (2周)

### Task 3.1: OffHeapCell

**目标:** 实现堆外单元格数据结构

**文件:**
- `src/main/java/cn/itcraft/jxlsb/data/OffHeapCell.java`
- `src/main/java/cn/itcraft/jxlsb/data/CellType.java`
- `src/test/java/cn/itcraft/jxlsb/data/OffHeapCellTest.java`

**验收:**
- setText/getText文本数据
- setNumber/getNumber数值数据
- setDate/getDate日期数据
- CellType枚举定义
- 测试通过

- [ ] 写测试
- [ ] 运行测试（失败）
- [ ] 实现CellType枚举
- [ ] 实现OffHeapCell（参考设计文档第四章）
- [ ] 运行测试（通过）
- [ ] git commit

---

### Task 3.2: OffHeapRow

**目标:** 实现堆外行数据结构

**文件:**
- `src/main/java/cn/itcraft/jxlsb/data/OffHeapRow.java`
- `src/test/java/cn/itcraft/jxlsb/data/OffHeapRowTest.java`

**验收:**
- getCell获取单元格
- setCell设置单元格
- rowIndex/columnCount属性
- 测试通过

- [ ] 写测试
- [ ] 运行测试（失败）
- [ ] 实现OffHeapRow
- [ ] 运行测试（通过）
- [ ] git commit

---

### Task 3.3: OffHeapSheet

**目标:** 实现堆外Sheet数据结构，支持流式读取

**文件:**
- `src/main/java/cn/itcraft/jxlsb/data/OffHeapSheet.java`
- `src/test/java/cn/itcraft/jxlsb/data/OffHeapSheetTest.java`

**验收:**
- nextRow流式创建行
- createRow写入创建行
- Iterator实现
- 测试通过

- [ ] 写测试
- [ ] 运行测试（失败）
- [ ] 实现OffHeapSheet
- [ ] 运行测试（通过）
- [ ] git commit

---

### Task 3.4: OffHeapWorkbook

**目标:** 实现堆外Workbook数据结构

**文件:**
- `src/main/java/cn/itcraft/jxlsb/data/OffHeapWorkbook.java`
- `src/test/java/cn/itcraft/jxlsb/data/OffHeapWorkbookTest.java`

**验收:**
- getSheet获取Sheet
- createSheet创建Sheet
- sheetCount属性
- 测试通过

- [ ] 写测试
- [ ] 运行测试（失败）
- [ ] 实现OffHeapWorkbook
- [ ] 运行测试（通过）
- [ ] git commit

---

### Phase 3 验收检查点

**验收命令:**
```bash
mvnd clean test
```

---

## Phase 4: IO Layer (1周)

### Task 4.1: OffHeapInputStream

**目标:** 实现堆外输入流，FileChannel直接读取堆外内存

**文件:**
- `src/main/java/cn/itcraft/jxlsb/io/OffHeapInputStream.java`
- `src/test/java/cn/itcraft/jxlsb/io/OffHeapInputStreamTest.java`

**验收:**
- readBlock读取内存块
- streamProcess流式处理
- 零拷贝实现
- 测试通过

- [ ] 写测试
- [ ] 运行测试（失败）
- [ ] 实现OffHeapInputStream
- [ ] 运行测试（通过）
- [ ] git commit

---

### Task 4.2: OffHeapOutputStream

**目标:** 实现堆外输出流，FileChannel直接写入堆外内存

**文件:**
- `src/main/java/cn/itcraft/jxlsb/io/OffHeapOutputStream.java`
- `src/test/java/cn/itcraft/jxlsb/io/OffHeapOutputStreamTest.java`

**验收:**
- writeBlock写入内存块
- writeBlocks批量写入
- 零拷贝实现
- 测试通过

- [ ] 写测试
- [ ] 运行测试（失败）
- [ ] 实现OffHeapOutputStream
- [ ] 运行测试（通过）
- [ ] git commit

---

### Task 4.3: FileChannelBlock

**目标:** 实现FileChannel内存块适配器

**文件:**
- `src/main/java/cn/itcraft/jxlsb/io/FileChannelBlock.java`
- `src/test/java/cn/itcraft/jxlsb/io/FileChannelBlockTest.java`

**验收:**
- ByteBuffer适配MemoryBlock接口
- 测试通过

- [ ] 写测试
- [ ] 运行测试（失败）
- [ ] 实现FileChannelBlock
- [ ] 运行测试（通过）
- [ ] git commit

---

### Phase 4 验收检查点

**验收命令:**
```bash
mvnd clean test
```

---

## Phase 5: API Layer (2周)

### Task 5.1: XlsbReader (流式读取API)

**目标:** 实现XLSB流式读取器，Builder模式

**文件:**
- `src/main/java/cn/itcraft/jxlsb/api/XlsbReader.java`
- `src/main/java/cn/itcraft/jxlsb/api/SheetHandler.java`
- `src/main/java/cn/itcraft/jxlsb/api/CellHandler.java`
- `src/test/java/cn/itcraft/jxlsb/api/XlsbReaderTest.java`

**验收:**
- Builder模式构建
- readSheets流式读取所有Sheet
- readSheet读取指定Sheet
- readCells读取单元格
- 函数式接口处理
- 测试通过

- [ ] 写测试
- [ ] 运行测试（失败）
- [ ] 实现SheetHandler/CellHandler函数式接口
- [ ] 实现XlsbReader.Builder
- [ ] 实现XlsbReader读取逻辑
- [ ] 运行测试（通过）
- [ ] git commit

---

### Task 5.2: XlsbWriter (流式写入API)

**目标:** 实现XLSB流式写入器，Builder模式

**文件:**
- `src/main/java/cn/itcraft/jxlsb/api/XlsbWriter.java`
- `src/main/java/cn/itcraft/jxlsb/api/CellData.java`
- `src/main/java/cn/itcraft/jxlsb/api/CellDataSupplier.java`
- `src/test/java/cn/itcraft/jxlsb/api/XlsbWriterTest.java`

**验收:**
- Builder模式构建
- createSheet创建Sheet
- writeSheet写入Sheet
- writeBatch批量写入优化
- 函数式接口
- 测试通过

- [ ] 写测试
- [ ] 运行测试（失败）
- [ ] 实现CellData数据结构
- [ ] 实现CellDataSupplier函数式接口
- [ ] 实现XlsbWriter.Builder
- [ ] 实现XlsbWriter写入逻辑
- [ ] 运行测试（通过）
- [ ] git commit

---

### Task 5.3: API Integration Test

**目标:** API集成测试，读写真实XLSB文件

**文件:**
- `src/test/java/cn/itcraft/jxlsb/api/XlsbApiIntegrationTest.java`

**验收:**
- 读取真实XLSB文件
- 写入并验证生成的XLSB文件
- 流式处理大文件
- 测试通过

- [ ] 写集成测试：读取small.xlsb
- [ ] 写集成测试：写入并读取验证
- [ ] 写集成测试：流式处理medium.xlsb
- [ ] 运行测试（通过）
- [ ] git commit

---

### Phase 5 验收检查点

**验收命令:**
```bash
mvnd clean test
```

---

## Phase 6: Java 17 Support (1周)

### Task 6.1: MemorySegmentAllocator (Java 17)

**目标:** 实现Java 17堆外内存分配器（Foreign Memory API）

**文件:**
- `src/main/java17/cn/itcraft/jxlsb/memory/impl/MemorySegmentAllocator.java`
- `src/main/java17/cn/itcraft/jxlsb/memory/impl/MemorySegmentBlock.java`
- `src/main/java17/cn/itcraft/jxlsb/spi/impl/MemorySegmentAllocatorProvider.java`
- `src/main/java17/resources/META-INF/services/cn.itcraft.jxlsb.spi.AllocatorProvider`
- `src/test/java17/cn/itcraft/jxlsb/memory/impl/MemorySegmentAllocatorTest.java`

**验收:**
- MemorySegment.allocateNative分配
- MemoryAccess API读写
- Provider优先级20（高于ByteBuffer）
- 测试通过

- [ ] 写测试（Java 17）
- [ ] 运行测试（失败）
- [ ] 实现MemorySegmentAllocator
- [ ] 实现MemorySegmentBlock
- [ ] 实现MemorySegmentAllocatorProvider
- [ ] 创建ServiceLoader配置
- [ ] 运行测试（通过）
- [ ] git commit

---

### Task 6.2: Multi-JDK Testing

**目标:** 多JDK版本测试

**验收:**
- Java 8测试通过（ByteBufferAllocator）
- Java 17测试通过（MemorySegmentAllocator）
- ServiceLoader自动选择最优实现

- [ ] Java 8环境运行测试
- [ ] Java 17环境运行测试
- [ ] 验证AllocatorFactory自动选择
- [ ] git commit

---

### Phase 6 验收检查点

**验收命令:**
```bash
# Java 8
JAVA_HOME=/usr/lib/jvm/java-8 mvnd clean test

# Java 17
JAVA_HOME=/usr/lib/jvm/java-17 mvnd clean test
```

---

## Phase 7: Performance Optimization (1周)

### Task 7.1: Memory Pool Optimization

**目标:** 优化内存池性能

**验收:**
- 预分配内存池容量
- 并发访问优化
- 内存池大小监控

- [ ] 性能测试：内存池并发访问
- [ ] 优化MemoryPool并发性能
- [ ] 添加内存池监控
- [ ] git commit

---

### Task 7.2: Batch Read/Write Optimization

**目标:** 批量读写优化

**验收:**
- 批量写入减少Record次数
- 批量读取缓存优化

- [ ] 性能测试：批量写入
- [ ] 优化RecordWriter批量写入
- [ ] 性能测试：批量读取
- [ ] 优化RecordParser批量读取
- [ ] git commit

---

### Task 7.3: JMH Performance Benchmark

**目标:** JMH性能基准测试

**文件:**
- `src/test/java/cn/itcraft/jxlsb/perf/XlsbReaderBenchmark.java`
- `src/test/java/cn/itcraft/jxlsb/perf/XlsbWriterBenchmark.java`

**验收:**
- 读取速度 >50MB/s
- 写入速度 >30MB/s
- 堆内存占用 <10MB
- GC次数 <5次/GB文件

- [ ] 编写JMH基准测试
- [ ] 运行基准测试
- [ ] 分析性能瓶颈
- [ ] 优化并重新测试
- [ ] git commit

---

### Task 7.4: Memory Leak Test

**目标:** 内存泄漏测试

**文件:**
- `src/test/java/cn/itcraft/jxlsb/memory/MemoryLeakTest.java`

**验收:**
- 循环读写无内存泄漏
- 所有资源正确释放

- [ ] 编写内存泄漏测试
- [ ] 运行测试（使用JMC监控）
- [ ] 修复内存泄漏问题
- [ ] git commit

---

### Phase 7 验收检查点

**验收命令:**
```bash
mvnd clean test
# 运行JMH基准测试
mvnd test -Dtest=XlsbReaderBenchmark,XlsbWriterBenchmark
```

---

## Phase 8: Documentation & Release (1周)

### Task 8.1: API Documentation

**目标:** 完善API文档（JavaDoc）

**验收:**
- 所有公共API有JavaDoc
- @author/@since标签
- 使用示例

- [ ] 完善MemoryBlock JavaDoc
- [ ] 完善OffHeapAllocator JavaDoc
- [ ] 完善XlsbReader/XlsbWriter JavaDoc
- [ ] 完善数据结构JavaDoc
- [ ] git commit

---

### Task 8.2: Usage Examples

**目标:** 编写使用示例

**文件:**
- `src/test/java/cn/itcraft/jxlsb/example/ReadExample.java`
- `src/test/java/cn/itcraft/jxlsb/example/WriteExample.java`

**验收:**
- 读取示例完整
- 写入示例完整

- [ ] 编写ReadExample
- [ ] 编写WriteExample
- [ ] git commit

---

### Task 8.3: README.md

**目标:** 编写项目README

**文件:**
- `README.md`

**验收:**
- 项目介绍
- 功能特性
- 快速开始
- API示例
- 性能指标
- 构建说明

- [ ] 编写README.md
- [ ] git commit

---

### Task 8.4: Final Integration Test

**目标:** 最终集成测试

**验收:**
- 所有测试通过
- 无SpotBugs/PMD警告
- 性能达标

- [ ] mvnd clean test
- [ ] mvnd spotbugs:check
- [ ] mvnd pmd:check
- [ ] JMH基准测试验证性能
- [ ] git commit

---

### Task 8.5: Release Preparation

**目标:** 发布准备

**验收:**
- pom.xml版本号设置
- Maven Central发布配置（可选）

- [ ] 更新pom.xml版本号
- [ ] 添加Maven Central发布配置（可选）
- [ ] git commit
- [ ] git tag v1.0.0

---

## 完整验收清单

### 功能验收
- [ ] 内存管理层完整（MemoryBlock, OffHeapAllocator, MemoryPool）
- [ ] Java 8 ByteBufferAllocator实现
- [ ] Java 17 MemorySegmentAllocator实现
- [ ] ServiceLoader自动选择机制
- [ ] 数据结构层完整（Cell, Row, Sheet, Workbook）
- [ ] BIFF12格式解析（10+核心记录）
- [ ] IO层零拷贝实现
- [ ] 流式API（XlsbReader/XlsbWriter）
- [ ] Builder模式构建
- [ ] 函数式接口处理

### 性能验收
- [ ] 读取速度 >50MB/s
- [ ] 写入速度 >30MB/s
- [ ] 堆内存占用 <10MB
- [ ] 堆外内存管理正确
- [ ] GC压力极小
- [ ] 无内存泄漏

### 质量验收
- [ ] 所有单元测试通过
- [ ] 所有集成测试通过
- [ ] SpotBugs无警告
- [ ] PMD无警告
- [ ] JavaDoc完整
- [ ] README.md完整
- [ ] 使用示例完整

### 文档验收
- [ ] 设计文档完整
- [ ] 实施计划完整
- [ ] API文档完整
- [ ] README.md完整

---

## 参考资料

- 设计文档：`docs/superpowers/specs/2026-04-10-jxlsb-design.md`
- [MS-XLSB]规范：`[MS-XLSB].pdf` 和 `[MS-XLSB]-251113.docx`
- 编码规范：`/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md`
- 构建工具：`/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md`

---

**计划完成！两种执行方式：**

**1. Subagent-Driven (推荐)** - 我为每个任务派遣独立子代理，任务间进行审查，快速迭代

**2. Inline Execution** - 在当前会话中使用executing-plans批量执行，带检查点审查

**选择哪种方式？**