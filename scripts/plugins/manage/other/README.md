# GitHub 自动更新脚本

这个脚本实现了从 GitHub 仓库拉取更新、比对文件变更、用户确认后应用更新的完整功能。

## 功能特性

- 🔄 自动从 GitHub 仓库检查更新
- 📋 显示文件变更列表（新增、删除、修改）
- ✅ 用户可选择性批准或拒绝特定文件更新
- 🛡️ 安全的更新机制，避免意外覆盖
- 📊 实时状态显示和进度反馈

## 使用方法

### 1. 设置 GitHub 仓库
```
/update set <仓库地址> [分支名]
```
示例：
```
/update set Anuken/Mindustry main
/update set myusername/server-repo develop
```

### 2. 检查更新
```
/update check
```
这会从 GitHub 获取最新代码并分析文件变更。

### 3. 查看待更新文件
```
/update list
```
显示所有检测到的文件变更，包括：
- 文件类型（新增/删除/修改）
- 文件路径
- 当前状态（待处理/已批准/已拒绝）

### 4. 批准或拒绝更新

#### 批准特定文件
```
/update approve <文件名或路径>
```
示例：
```
/update approve config/server.properties
/update approve scripts/
```

#### 批准所有文件
```
/update approve all
```

#### 拒绝特定文件
```
/update reject <文件名或路径>
```

#### 拒绝所有文件
```
/update reject all
```

### 5. 应用已批准的更新
```
/update apply
```
执行 `git pull` 应用所有已批准的变更。

### 6. 查看当前状态
```
/update status
```
显示：
- 当前配置的 GitHub 仓库
- 分支信息
- 文件变更统计

### 7. 获取帮助
```
/update help
```

## 使用流程示例

1. **初始化设置**
   ```
   /update set Anuken/Mindustry main
   ```

2. **检查更新**
   ```
   /update check
   ```

3. **查看变更**
   ```
   /update list
   ```

4. **选择性批准**
   ```
   /update approve config/
   /update approve scripts/
   /update reject sensitive-data.json
   ```

5. **应用更新**
   ```
   /update apply
   ```

6. **重启服务器**（建议）
   ```
   restart
   ```

## 安全特性

- **选择性更新**：只有用户明确批准的文件才会被更新
- **变更预览**：在应用前可以查看所有文件变更
- **状态追踪**：每个文件的批准/拒绝状态都会被记录
- **错误处理**：网络错误或 Git 操作失败时会提供详细错误信息

## 注意事项

1. **Git 仓库要求**：服务器目录必须是一个 Git 仓库
2. **网络连接**：需要能够访问 GitHub
3. **权限要求**：需要有执行 Git 命令的权限
4. **备份建议**：在应用重要更新前建议手动备份

## 故障排除

### Git fetch 失败
- 检查网络连接
- 确认 GitHub 仓库地址正确
- 检查 Git 配置

### 文件权限错误
- 确保服务器进程有写入权限
- 检查文件是否被其他进程占用

### 更新应用失败
- 查看详细错误信息
- 检查是否有文件冲突
- 考虑手动解决冲突后重试

## 技术实现

脚本使用以下技术：
- **Git 命令**：`git fetch`, `git diff`, `git pull`
- **异步处理**：使用 `CompletableFuture` 避免阻塞主线程
- **状态管理**：使用数据类跟踪文件变更状态
- **命令系统**：集成到 Mindustry 的控制台命令系统
