# jxlsb 开源版与商业版对比

## 版本说明

jxlsb 提供两个版本：
- **开源版 (Open Source)**：GitHub开源，Apache License 2.0
- **商业版 (Commercial)**：企业付费版本，包含高级功能

## 功能对比

| 功能 | 开源版 | 商业版 | 说明 |
|------|:------:|:------:|------|
| **基础功能** ||||
| 数字单元格 | ✅ | ✅ | 整数、浮点数 |
| 文本单元格 | ✅ | ✅ | SST优化 |
| 布尔单元格 | ✅ | ✅ | |
| 日期单元格 | ✅ | ✅ | Excel日期序列号 |
| 空白单元格 | ✅ | ✅ | |
| 数字格式 | ✅ | ✅ | 百分比、千分位、货币等 |
| 样式系统 | ✅ | ✅ | 字体、边框、填充、对齐 |
| 流式写入 | ✅ | ✅ | startSheet/writeRows/endSheet |
| 流式读取 | ✅ | ✅ | forEachRow/readRows |
| 模板填充 | ✅ | ✅ | fillBatch/fillAtMarker/startFill |
| 合并单元格 | ✅ | ✅ | 模板中合并单元格保留 |
| **高级功能** ||||
| 文件加密 | ❌ | ✅ | AES-128+SHA1，WPS/Excel兼容 |
| 文件解密 | ❌ | ✅ | 密码验证+数据解密 |
| 文本框水印 | ❌ | ✅ | 自定义文本、位置、颜色 |
| 重复水印 | ❌ | ✅ | 每N行重复一个水印 |
| 功能组合 | 部分 | ✅ | 模板+水印+加密组合 |
| **性能优化** ||||
| 堆外内存 | ✅ | ✅ | 零GC压力 |
| 基础性能优化 | ✅ | ✅ | 比POI快2-3倍 |
| 加密性能优化 | ❌ | ✅ | 加密开销<12% |
| **企业级支持** ||||
| 社区支持 | ✅ | ✅ | GitHub Issues |
| 商业支持 | ❌ | ✅ | 专属技术支持 |
| 定制开发 | ❌ | ✅ | 按需定制功能 |

## 详细说明

### 开源版适用场景

开源版适合：
- ✅ 个人开发者、小团队
- ✅ 数据导出/导入需求
- ✅ 预算有限的项目
- ✅ 不需要文件加密的场景
- ✅ 开源项目集成

**典型使用**：
```java
// 开源版 - 数据导出
XlsbWriter writer = XlsbWriter.builder()
    .path(Paths.get("output.xlsb"))
    .build();
writer.writeBatch("Sheet1", supplier, 1000, 10);
writer.close();

// 开源版 - 模板填充
XlsbWriter writer = XlsbWriter.builder()
    .template(Paths.get("template.xlsb"))
    .path(Paths.get("output.xlsb"))
    .build();
writer.fillBatch(0, dataList, 12, 8);
writer.close();
```

### 商业版适用场景

商业版适合：
- ✅ 企业级应用
- ✅ 需要数据安全（加密）
- ✅ 需要水印标识（防泄露）
- ✅ 政府/金融/医疗行业
- ✅ 需要技术支持的项目

**典型使用**：
```java
// 商业版 - 模板填充 + 水印 + 加密
Path tempPath = Paths.get("/tmp/temp.xlsb");

try (XlsbWriter writer = XlsbWriter.builder()
        .template(Paths.get("template.xlsb"))
        .path(tempPath)
        .build()) {
    writer.fillBatch(0, dataList, 12, 8);
    writer.setWatermark("内部资料");  // 商业版功能
}

// 加密（商业版功能）
StandardEncryptor encryptor = new StandardEncryptor();
encryptor.confirmPassword("password123");
byte[] encrypted = encryptor.encryptPackage(Files.readAllBytes(tempPath));
// 写入CFB格式...
```

## 性能对比

| 操作 | 开源版 | 商业版 |
|------|--------|--------|
| 100K行写入 | 453ms | 453ms |
| 100K行加密写入 | ❌ | 446ms (+11%) |
| 100K行解密读取 | ❌ | 43ms (+16%) |

**结论**：
- 基础性能相同
- 商业版加密开销极小（<12%）
- 商业版水印无额外开销

## 文件大小对比

| 内容 | 开源版 | 商业版加密 |
|------|--------|------------|
| 100K行数据 | 2.72MB | 2.72MB + 3KB |
| 水印 | ❌ | 0额外开销 |

加密文件增量固定约3KB（CFB结构），与数据规模无关。

## 许可证

| 版本 | 许可证 | 说明 |
|------|--------|------|
| 开源版 | Apache License 2.0 | 免费、开源、可商用 |
| 商业版 | 商业许可 | 付费、企业专属功能 |

## 获取方式

### 开源版

- GitHub: https://github.com/itcraft-cn/jxlsb
- Maven:
```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jxlsb</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 商业版

- 官网: https://itcraft.cn/jxlsb-commercial
- 联系: hellyguo#foxmail.com
- 提供专属License文件

## 分支管理

| 分支 | 内容 | 说明 |
|------|------|------|
| `main` | 开源版基础 | 稳定发布 |
| `dev` | 开源版开发 | 模板填充等功能 |
| `commercial` | 商业版 | 加密、水印等 |

**合并规则**：
- `dev` → `commercial`：开源版新功能合并到商业版
- 商业版高级功能**不**合并到开源版

## 技术支持

| 支持类型 | 开源版 | 商业版 |
|----------|--------|--------|
| GitHub Issues | ✅ | ✅ |
| 文档 | ✅ | ✅ |
| 邮件支持 | ❌ | ✅ |
| 专属技术支持 | ❌ | ✅ |
| 定制开发 | ❌ | ✅ |
| 响应时间 | 社区响应 | 24小时内 |

## 总结

- **开源版**：满足80%的数据导出需求，零成本
- **商业版**：满足企业级安全需求，付费增值

选择建议：
- 个人/小项目 → 开源版
- 企业/安全需求 → 商业版
