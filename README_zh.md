# Switch2VSCode

[English](README.md)

一个 JetBrains IDE 插件，用相同的快捷键在 VS Code 中打开当前文件或整个工程，并保持光标位置。

灵感来源并感谢 [qczone/switch2cursor](https://github.com/qczone/switch2cursor) 项目。

## 🌟 功能
- `Option/Alt+Shift+O`：在 VS Code 中打开当前文件并跳转到相同行列
- `Option/Alt+Shift+P`：在 VS Code 中打开当前工程
- 支持快捷键、编辑器/项目右键菜单、Tools 菜单
- VS Code 可执行文件路径可配置（默认 `code`）
- 支持 JetBrains IDE 2022.3+

## 🛠️ 安装
1. 运行 `./gradlew buildPlugin` 构建插件
2. IDE → Settings/Preferences → Plugins → ⚙️ → Install Plugin from Disk...，选择 `build/distributions` 下的压缩包

## 🚀 使用
- 打开工程：Option/Alt+Shift+P，或项目视图右键 → `Open Project In VS Code`，或 Tools → `Open Project In VS Code`
- 打开当前文件：Option/Alt+Shift+O，或编辑器右键 → `Open File In VS Code`，或 Tools → `Open File In VS Code`

## ⚙️ 配置
Settings/Preferences → Tools → Switch2VSCode → 设置 VS Code 可执行文件路径（默认 `code`）；快捷键可在 Keymap 中自定义。

## 🧑‍💻 开发
```bash
git clone https://github.com/Asxing/switch2vscode.git
cd switch2vscode
./gradlew buildPlugin
```

## 📄 许可证
本项目使用 [MIT License](LICENSE)

## 📮 反馈
如有问题或建议，请在 GitHub 提交 Issue：https://github.com/Asxing/switch2vscode/issues
