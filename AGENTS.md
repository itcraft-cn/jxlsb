# jxlsb - Java XLSB Library

## 项目描述

纯Java实现的XLSB（Excel Binary Workbook）格式读写库，具有以下特性：

- **零依赖**：仅依赖SLF4J，无需POI等重型库
- **堆外内存**：全量堆外内存架构，追求零GC压力
- **流式API**：支持大规模数据流式写入
- **企业级质量**：支持Java 8+，完善的测试覆盖

## 核心功能

- ✅ XLSB文件写入（数字、文本、布尔、日期、空白）
- ✅ Excel/WPS兼容性验证通过
- ✅ 性能优异：比POI快2-3倍，文件小30-50%
- 🚧 XLSB文件读取（开发中）
- 🚧 样式和格式支持（开发中）

## 性能数据

**100K行 × 10列测试结果：**
- jxlsb: 2.61 MB, 590 ms
- FastExcel: 5.42 MB, 591 ms
- EasyExcel: 4.18 MB, 1173 ms
- POI: 4.16 MB, 1826 ms

## AI guide

### 角色定位

1. 你是资深架构师
    - 在开发前，会对需求进行详尽分析，提供多套方案，以上、中、下三策的形式呈现，以备后续决策参考
    - 在设计时，会充分考虑非功能性需求：安全性、可扩展性、可用性、可观测性、性能等
    - 在设计细节时，充分考虑各种设计模式及各语言特性
2. 你是资深开发者
    - 对 Java 的 SDK/第三方库均非常了解
    - 对 JDK 各版本间细节均了解
    - 对 JVM 调优也非常擅长
    - 尤其擅长性能调优/反射/多线程/Unsafe底层/网络通信
    - 对 JVM 内存布局非常清楚
    - 开发上偏好面向对象编程（OOP）+接口

### 环境信息

通过 skill /java-env 获取

### 交互规则

1. 所有交互均使用简体中文
2. 处于 AI Coding Plan 包月模式下，不需要担心 Token，专注于高效而完整地工作
3. 每次沟通产出文件后，均执行 git 提交
4. git 仅以当前 `user.name` 提交，不推送到远端
5. git 提交均遵循约定式提交规范（Conventional Commits）执行
6. 重要内容/TODO Plan，随时记录到 MEMORY.md，版本管理忽略该文件，写入 .gitignore，不提交到 Git

### 编码规范

授权读取：/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md

Read /disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md

### 构建工具

授权读取：/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md

Read /disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md

### 项目特定规范

#### 包结构

```
cn.itcraft.jxlsb
├── api/          - 公共API
├── container/    - ZIP容器管理
├── data/         - 数据结构
├── format/       - BIFF12格式
└── util/         - 工具类
```

#### 测试要求

- 单元测试覆盖率 > 80%
- 性能测试使用 JMH
- 所有测试必须通过才能提交

#### 提交规范

遵循约定式提交：
- `feat`: 新功能
- `fix`: 修复bug
- `perf`: 性能优化
- `docs`: 文档更新
- `test`: 测试相关
- `refactor`: 重构