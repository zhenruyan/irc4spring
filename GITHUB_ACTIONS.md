# GitHub Actions 使用说明

本项目包含两个GitHub Actions工作流，用于持续集成和自动发布。

## 工作流概述

### 1. CI工作流 (`.github/workflows/ci.yml`)

**触发条件：**
- 推送到 `main` 或 `develop` 分支
- 创建针对 `main` 或 `develop` 分支的Pull Request

**功能：**
- 自动运行单元测试
- 生成测试报告
- 构建JAR文件
- 代码质量检查（SpotBugs、Checkstyle）
- 上传构建产物

### 2. Release工作流 (`.github/workflows/release.yml`)

**触发条件：**
- 推送版本标签（格式：`v*`，如 `v1.0.0`）
- 手动触发（workflow_dispatch）

**功能：**
- 自动编译项目
- 创建发布包（tar.gz和zip格式）
- 生成SHA256校验和
- 创建GitHub Release
- 上传发布文件

## 如何创建Release

### 方法1：使用Git标签（推荐）

1. 确保代码已提交到main分支
2. 创建并推送版本标签：

```bash
# 创建标签
git tag -a v1.0.0 -m "Release version 1.0.0"

# 推送标签到远程仓库
git push origin v1.0.0
```

3. GitHub Actions会自动触发，创建Release并上传文件

### 方法2：手动触发

1. 在GitHub仓库页面，点击 "Actions" 标签
2. 选择 "Build and Release" 工作流
3. 点击 "Run workflow" 按钮
4. 选择分支并点击 "Run workflow"

## Release产物

每次发布会生成以下文件：

### 主要文件
- `irc4spring-{version}.tar.gz` - Linux/macOS完整包
- `irc4spring-{version}.zip` - Windows完整包  
- `irc-server-1.0.0.jar` - 独立JAR文件

### 校验和文件
- `irc4spring-{version}.tar.gz.sha256` - tar.gz文件的SHA256校验和
- `irc4spring-{version}.zip.sha256` - zip文件的SHA256校验和

### 完整包内容
每个完整包包含：
- `irc-server-1.0.0.jar` - 主程序
- `start.sh` - 启动脚本
- `README.md` - 英文说明文档
- `README_zh.md` - 中文说明文档
- `application.yml.example` - 配置文件示例

## 版本命名规范

建议使用语义化版本控制：

- `v1.0.0` - 主要版本
- `v1.1.0` - 次要版本（新功能）
- `v1.0.1` - 补丁版本（bug修复）
- `v1.0.0-beta.1` - 预发布版本

## 配置要求

### 仓库设置
确保仓库具有以下权限：
- Actions权限已启用
- 可以创建Release
- 可以上传文件到Release

### 分支保护
建议为main分支设置保护规则：
- 要求Pull Request审查
- 要求状态检查通过
- 要求分支是最新的

## 故障排除

### 常见问题

1. **Actions失败：权限不足**
   - 检查仓库的Actions权限设置
   - 确认GITHUB_TOKEN有足够权限

2. **编译失败：Java版本问题**
   - 确保使用Java 21
   - 检查pom.xml中的Java版本配置

3. **测试失败**
   - 检查测试代码
   - 确保所有依赖都正确

4. **Release创建失败**
   - 检查标签格式是否正确
   - 确认没有重复的标签
   - 检查Release权限

### 调试步骤

1. 查看Actions日志：
   - 进入仓库的Actions页面
   - 点击失败的工作流
   - 查看详细日志

2. 本地测试：
   ```bash
   # 运行测试
   mvn clean test
   
   # 构建项目
   mvn clean package
   
   # 检查JAR文件
   java -jar target/irc-server-1.0.0.jar --help
   ```

3. 验证标签：
   ```bash
   # 查看所有标签
   git tag
   
   # 查看特定标签
   git show v1.0.0
   ```

## 自定义配置

### 修改触发条件
编辑 `.github/workflows/release.yml`：

```yaml
on:
  push:
    tags:
      - 'v*'          # 版本标签
      - 'release-*'   # 添加其他标签格式
  workflow_dispatch:    # 手动触发
```

### 修改发布内容
编辑release步骤中的body内容：

```yaml
body: |
  ## 自定义发布说明
  
  ### 新功能
  - 功能1
  - 功能2
  
  ### 修复
  - 修复1
  - 修复2
```

### 添加更多文件
修改发布文件列表：

```yaml
files: |
  irc4spring-${{ steps.get_version.outputs.VERSION }}.tar.gz
  irc4spring-${{ steps.get_version.outputs.VERSION }}.zip
  target/irc-server-1.0.0.jar
  docs/manual.pdf  # 添加更多文件
```

## 最佳实践

1. **版本管理**
   - 使用语义化版本控制
   - 在CHANGELOG.md中记录变更
   - 为每个版本创建详细的Release说明

2. **测试**
   - 确保所有测试通过后再创建Release
   - 在多个环境中测试Release包

3. **文档**
   - 保持README文件更新
   - 为每个Release提供详细说明
   - 包含升级指南

4. **安全**
   - 定期更新Actions版本
   - 使用最小权限原则
   - 检查依赖安全性

## 监控和通知

可以添加通知功能，在Release创建后发送通知：

```yaml
- name: Notify on success
  if: success()
  run: |
    echo "Release ${{ steps.get_version.outputs.VERSION }} created successfully!"
    # 可以添加Slack、邮件等通知
``` 