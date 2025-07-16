# GitHub Actions 故障排除指南

## 403权限错误解决方案

如果您在GitHub Actions中遇到类似以下的403错误：

```
⚠️ GitHub release failed with status: 403
undefined
retrying... (2 retries remaining)
❌ Too many retries. Aborting...
Error: Too many retries. github create Release失败
```

请按照以下步骤解决：

### 1. 检查仓库权限设置

1. 进入您的GitHub仓库
2. 点击 **Settings** 标签
3. 在左侧菜单中选择 **Actions** → **General**
4. 找到 **Workflow permissions** 部分
5. 选择 **Read and write permissions**
6. 勾选 **Allow GitHub Actions to create and approve pull requests**
7. 点击 **Save** 保存设置

### 2. 验证工作流权限配置

确保您的 `.github/workflows/release.yml` 文件包含正确的权限配置：

```yaml
permissions:
  contents: write  # 允许创建release和上传文件
  packages: write  # 允许上传包
  actions: read    # 允许读取actions
  checks: write    # 允许写入检查结果
```

### 3. 检查分支保护规则

如果您的main/master分支有保护规则：

1. 进入 **Settings** → **Branches**
2. 检查分支保护规则
3. 确保Actions有足够权限绕过保护规则（如果需要）

### 4. 验证标签和Release

确保：
- 标签格式正确（如 `v1.0.0`）
- 没有重复的标签
- 没有同名的Release已存在

### 5. 使用Personal Access Token（高级）

如果上述方法都不起作用，可以创建Personal Access Token：

1. 进入GitHub **Settings** → **Developer settings** → **Personal access tokens**
2. 创建新的token，权限包括：
   - `repo` (完整仓库权限)
   - `write:packages` (写入包权限)
3. 将token添加到仓库的Secrets中：
   - 仓库 **Settings** → **Secrets and variables** → **Actions**
   - 添加名为 `RELEASE_TOKEN` 的secret
4. 修改工作流使用自定义token：
   ```yaml
   - name: Create Release
     uses: softprops/action-gh-release@v1
     env:
       GITHUB_TOKEN: ${{ secrets.RELEASE_TOKEN }}
   ```

### 6. 验证构建

在推送标签之前，确保本地构建成功：

```bash
# 测试构建
mvn clean compile test package

# 检查JAR文件
java -jar target/irc-server-1.0.0.jar --version
```

### 7. 调试步骤

1. **查看详细日志**：
   - 进入Actions页面
   - 点击失败的工作流
   - 展开每个步骤查看详细错误

2. **检查网络和API限制**：
   - GitHub API可能有速率限制
   - 检查GitHub状态页面

3. **重新运行工作流**：
   - 在Actions页面点击 "Re-run jobs"
   - 有时临时网络问题会导致失败

### 8. 常见错误和解决方案

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| 403 Forbidden | 权限不足 | 检查仓库权限设置 |
| 422 Unprocessable Entity | 标签已存在 | 删除或使用不同标签 |
| 404 Not Found | 仓库不存在或无权限 | 检查仓库名称和权限 |
| Rate limit exceeded | API调用过多 | 等待或使用PAT |

### 9. 测试配置

创建测试标签来验证配置：

```bash
# 创建测试标签
git tag -a v0.0.1-test -m "Test release"
git push origin v0.0.1-test

# 如果成功，删除测试标签
git tag -d v0.0.1-test
git push origin --delete v0.0.1-test
```

### 10. 联系支持

如果问题仍然存在：

1. 检查[GitHub状态页面](https://www.githubstatus.com/)
2. 查看[GitHub Actions文档](https://docs.github.com/en/actions)
3. 在GitHub Community论坛寻求帮助
4. 联系GitHub支持

---

## 快速检查清单

- [ ] 仓库权限设置为 "Read and write permissions"
- [ ] 工作流文件包含正确的permissions配置
- [ ] 标签格式正确（v*.*.* 格式）
- [ ] 没有重复的标签或Release
- [ ] 本地构建成功
- [ ] 分支保护规则不冲突
- [ ] GitHub Actions已启用

按照这个清单逐项检查，通常可以解决大部分权限问题。 