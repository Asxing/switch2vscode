# Switch2VSCode

[English](README.md)

一个 JetBrains IDE 插件，智能地在 VS Code 系列编辑器中打开文件和项目，同时保持光标位置和上下文。

灵感来源并感谢 [qczone/switch2cursor](https://github.com/qczone/switch2cursor) 项目。

## 🌟 1.2.0 核心亮点

### 🚀 智能多编辑器支持
- **6种 VS Code 系列编辑器**：Visual Studio Code、Cursor、Windsurf、AntiGravity、CatPaw、Trae
- **智能发现引擎**：零配置自动检测已安装编辑器
- **白名单过滤**：精准识别防止误报和不需要的条目

### 🔍 三种发现策略
- **快速发现 (< 1秒)**：仅命令行发现，适合日常使用
- **全面发现 (2-5秒)**：命令行 + 应用程序扫描，平衡方案（默认）
- **智能发现 (1-3秒，缓存)**：完整发现配合智能缓存，性能最优

### 🖥️ 跨平台优化
- **macOS**：应用程序包识别 + 增强 PATH 扫描
- **Windows**：注册表扫描 + 程序目录检测
- **Linux**：包管理器集成 + 桌面文件解析

## 🎯 支持的编辑器

Switch2VSCode 专注于 VS Code 生态系统，为以下编辑器提供原生支持：

| 编辑器 | 描述 | 自动发现 | 特色功能 |
|--------|------|----------|----------|
| **Visual Studio Code** | 微软官方 VS Code | ✅ | 完整功能支持 |
| **Cursor** | AI 驱动的代码编辑器 | ✅ | AI 辅助开发 |
| **Windsurf** | 协作编程环境 | ✅ | 实时协作 |
| **AntiGravity** | 轻量级代码编辑器 | ✅ | 快速启动 |
| **CatPaw** | 专注编程的编辑器 | ✅ | 简洁界面 |
| **Trae** | 现代代码编辑器 | ✅ | 现代化界面 |

> 💡 **为什么选择白名单？** 我们专注于 VS Code 生态系统，确保每个支持的编辑器都经过深度测试和优化。

## ✨ 功能特性

### 核心功能
- 在选定编辑器中打开当前文件并保持相同行列位置（`Option/Alt+Shift+O`）
- 在选定编辑器中打开整个项目（`Option/Alt+Shift+P`）
- 通过快捷键、编辑器/项目右键菜单和工具菜单访问
- 在 IDE 之间切换时保持光标位置和上下文
- 与 JetBrains IDE 2022.3+ 无缝协作

### 1.2.0 增强功能
#### 智能编辑器发现
- 自动检测已安装的 VS Code 系列编辑器
- 针对 macOS、Windows 和 Linux 的跨平台优化
- 白名单过滤防止误识别
- 多级验证确保发现的编辑器功能正常

#### 灵活的发现策略
选择最适合您工作流程的策略：
- **快速发现**：仅命令行，< 1 秒响应时间
- **全面发现**：平衡方案，深度扫描（默认）
- **智能发现**：完整扫描配合智能缓存

#### 零配置体验
- 大多数用户可以立即开始使用，无需手动配置
- 基于可用性和用户偏好的自动编辑器选择
- 自动发现失败时智能回退到手动配置

## 🚀 快速开始

### 30秒设置指南

1. **安装插件**：IDE → 插件 → 搜索 "Switch2VSCode" → 安装
2. **自动配置**：首次使用时自动发现已安装编辑器
3. **开始使用**：`Alt+Shift+O` 打开文件，`Alt+Shift+P` 打开项目

### 首次配置向导

插件会自动：
- 🔍 扫描已安装的 VS Code 系列编辑器
- ⚙️ 配置最佳设置
- ✅ 验证编辑器可用性
- 🎯 为您的系统推荐最佳发现策略

> 💡 **零配置体验**：大多数用户无需任何手动设置即可立即开始使用！

## 🔧 智能配置

### 发现策略选择

根据您的使用场景选择：

#### 🚀 快速发现（推荐日常使用）
```
特点：闪电般响应，< 1 秒
适用：已知编辑器位置，追求效率的用户
方式：仅命令行检测
```

#### 🔍 全面发现（推荐首次设置）
```
特点：深度扫描，2-5 秒
适用：初始配置，发现所有编辑器
方式：命令行 + 应用程序目录扫描
```

#### 🧠 智能发现（推荐高级用户）
```
特点：智能缓存，1-3 秒
适用：频繁切换，需要完整性
方式：完整发现 + 智能缓存
```

### 自动发现架构

Switch2VSCode 使用双层发现引擎：

1. **命令行层**：检测系统 PATH 中的编辑器
2. **应用程序层**：扫描系统应用程序目录
3. **智能过滤**：白名单机制确保精准匹配
4. **版本检测**：自动获取编辑器版本信息

## ⚙️ 配置

### 设置位置
Settings/Preferences → Tools → Switch2VSCode

### 配置选项
- **编辑器选择**：从自动发现的编辑器中选择
- **发现策略**：根据偏好选择快速/全面/智能
- **自定义路径**：自动发现失败时手动指定编辑器路径
- **高级选项**：自定义命令行参数和启动偏好

### 键盘快捷键
在键盘映射设置中搜索 "Switch2VSCode" 来自定义快捷键：
- 默认文件快捷键：`Option/Alt+Shift+O`
- 默认项目快捷键：`Option/Alt+Shift+P`

## 🛠️ 安装

### 从 JetBrains Marketplace 安装（推荐）
1. 打开您的 JetBrains IDE
2. 进入 Settings/Preferences → Plugins
3. 搜索 "Switch2VSCode"
4. 点击安装并重启 IDE

### 手动安装
1. 构建插件：`./gradlew buildPlugin`
2. 从磁盘安装：IDE → Settings/Preferences → Plugins → ⚙️ → Install Plugin from Disk...
3. 选择 `build/distributions` 目录下的 zip 文件

## 🔧 故障排除

### 自动发现问题
如果自动发现失败：
1. 尝试不同的发现策略（快速 → 全面 → 智能）
2. 在 Settings → Tools → Switch2VSCode 中手动指定编辑器路径
3. 确保您的编辑器已正确安装且可从命令行访问

### 常见解决方案
- **编辑器未找到**：检查编辑器是否已安装并在系统 PATH 中
- **打开错误的编辑器**：在插件设置中验证编辑器选择
- **命令失败**：检查编辑器可执行文件权限和路径

## 🧑‍💻 开发

```bash
git clone https://github.com/Asxing/switch2vscode.git
cd switch2vscode
./gradlew buildPlugin
```

### 构建和测试
- 构建：`./gradlew buildPlugin`
- 测试：`./gradlew test`
- 在 IDE 中运行：`./gradlew runIde`

## 🤝 贡献

我们欢迎贡献！请查看我们的[贡献指南](CONTRIBUTING.md)了解详情。

### 贡献领域
- 支持更多基于 VS Code 的编辑器
- 平台特定的发现改进
- UI/UX 增强
- 文档改进

## 📄 许可证

本项目使用 [MIT License](LICENSE) 许可证

## 📮 支持与反馈

- **问题报告**：[GitHub Issues](https://github.com/Asxing/switch2vscode/issues)
- **功能请求**：[GitHub Discussions](https://github.com/Asxing/switch2vscode/discussions)
- **文档**：[Wiki](https://github.com/Asxing/switch2vscode/wiki)

## 🌟 Star 历史

[![Star History Chart](https://api.star-history.com/svg?repos=Asxing/switch2vscode&type=Date)](https://star-history.com/#Asxing/switch2vscode&Date)

---

**Switch2VSCode** - 以智能和简洁连接 JetBrains IDE 与 VS Code 生态系统。