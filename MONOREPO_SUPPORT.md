# Mono Repository (Mono Repo) 支持

## 📋 概述

从v1.1.0开始，Javalang-analyzing-cli增强了对Mono Repository（单体代码库）的支持，能够自动发现和分析多模块项目中的所有测试用例。

## 🔄 改进前 vs 改进后

### 改进前 (v1.0.x)
- ❌ 只搜索根目录下的 `src/test/java`
- ❌ 无法处理多模块项目
- ❌ 对于mono repo结构会遗漏大量测试用例

### 改进后 (v1.1.0+)
- ✅ 递归搜索整个项目目录
- ✅ 自动发现所有子模块的测试目录
- ✅ 智能过滤，跳过构建输出目录
- ✅ 支持标准Maven/Gradle多模块结构

## 🏗️ 支持的项目结构

### 单模块项目
```
project-root/
├── src/test/java/           ← 检测到
└── build.gradle
```

### 多模块Maven项目
```
project-root/
├── module-a/
│   └── src/test/java/       ← 检测到
├── module-b/
│   └── src/test/java/       ← 检测到
├── core/
│   └── src/test/java/       ← 检测到
└── pom.xml
```

### 多模块Gradle项目
```
project-root/
├── app/
│   └── src/test/java/       ← 检测到
├── lib/
│   └── src/test/java/       ← 检测到
├── common/
│   └── src/test/java/       ← 检测到
└── build.gradle
```

### 嵌套模块项目
```
project-root/
├── backend/
│   ├── api/src/test/java/   ← 检测到
│   └── core/src/test/java/  ← 检测到
├── frontend/
│   └── tests/               ← 跳过（非Java）
└── shared/
    └── src/test/java/       ← 检测到
```

## 🚀 使用方法

### 基本用法
```bash
java -jar Javalang-analyzing-cli-1.1.0-all.jar ParseTestCaseToLlmContext \
    --project /path/to/mono-repo \
    --output-dir ./results
```

### 输出示例
```
Found 3 test source directories:
  - /path/to/mono-repo/module1/src/test/java
  - /path/to/mono-repo/module2/src/test/java
  - /path/to/mono-repo/core/src/test/java
Processing file: /path/to/mono-repo/module1/src/test/java/com/example/Module1Test.java
  Found test method: testFeatureA
  Found @BeforeEach method: setUp
  Successfully wrote: results/mono-repo_com.example.Module1Test_testFeatureA.json
...
ParseTestCaseToLlmContext: Found 15 test cases in 3 directories, Generated 15 JSON files.
```

## 🔍 检测算法

### 目录识别规则
工具使用以下规则识别测试目录：

1. **标准模式**: 路径以 `/src/test/java` 结尾
2. **多模块模式**: 包含 `/src/test/` 且以 `/java` 结尾
3. **嵌套检查**: 确保 `java` → `test` → `src` 的层级结构

### 智能过滤
自动跳过以下目录：
- 构建输出: `/build/`, `/target/`, `/bin/`, `/out/`
- 版本控制: `/.git/`, `/.svn/`
- 工具缓存: `/.gradle/`, `/.m2/`
- 隐藏目录: 以 `.` 开头的目录
- Node.js: `/node_modules/`

### 去重机制
- 自动移除嵌套的重复目录
- 只保留最具体的测试源目录

## ⚠️ 已知限制

### 1. 构建工具要求
- 根目录或子模块需要有有效的构建文件（`pom.xml` 或 `build.gradle`）
- 用于依赖解析和classpath配置

### 2. 依赖解析
- 如果依赖无法解析，可能影响AST解析质量
- 工具会尝试从本地缓存中查找通用测试依赖作为fallback

### 3. 性能考虑
- 大型mono repo可能需要较长处理时间
- 建议使用 `--threads` 参数并行处理

## 🛠️ 故障排除

### 问题：找不到测试目录
```
No test source roots found in project: /path/to/project
```
**解决方案**：
1. 确保项目使用标准的 `src/test/java` 结构
2. 检查目录权限
3. 确认不在被过滤的目录中

### 问题：AST解析失败
```
Error executing task: IllegalArgumentException
```
**解决方案**：
1. 确保项目根目录有构建文件
2. 检查依赖是否正确配置
3. 尝试运行 `gradle build` 或 `mvn compile` 确保项目能正常构建

### 问题：部分模块未处理
**解决方案**：
1. 检查每个子模块是否有独立的构建文件
2. 确认测试目录结构符合标准
3. 查看日志中的具体错误信息

## 📊 性能优化建议

### 大型项目
```bash
# 使用多线程处理
java -jar Javalang-analyzing-cli-1.1.0-all.jar ParseTestCaseToLlmContext \
    --project /path/to/large-mono-repo \
    --threads 8 \
    --output-dir ./results
```

### 内存配置
```bash
# 为大型项目增加内存
java -Xmx4g -jar Javalang-analyzing-cli-1.1.0-all.jar ...
```

## 🔄 升级指南

### 从v1.0.x升级
1. 下载新版本JAR文件
2. 命令行参数保持不变
3. 输出格式向后兼容
4. 新版本会自动发现更多测试用例

### 验证升级效果
```bash
# 比较分析结果数量
echo "v1.0.x结果数量: $(ls old-results/*.json | wc -l)"
echo "v1.1.0结果数量: $(ls new-results/*.json | wc -l)"
```

## 🤝 贡献

如果您的mono repo项目结构不被支持，请：
1. 提交GitHub Issue描述项目结构
2. 提供目录结构示例
3. 我们会考虑添加支持 