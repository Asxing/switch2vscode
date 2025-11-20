# Switch2VSCode

[中文文档](README_zh.md)

A JetBrains IDE plugin that opens the current file or entire project in Visual Studio Code while keeping the caret position.

Inspired by and grateful to [qczone/switch2cursor](https://github.com/qczone/switch2cursor).

## 🌟 Features
- Open current file in VS Code at the same line/column (`Option/Alt+Shift+O`)
- Open the current project in VS Code (`Option/Alt+Shift+P`)
- Entry points: shortcuts, editor/project context menus, and Tools menu
- Configurable VS Code executable path (default `code`)
- Works with JetBrains IDEs 2022.3+

## 🛠️ Installation
1. Build the plugin: `./gradlew buildPlugin`
2. Install from disk: IDE → Settings/Preferences → Plugins → ⚙️ → Install Plugin from Disk... → choose the zip from `build/distributions`.

## 🚀 Usage
- Open project: Option/Alt+Shift+P, Project view context menu → `Open Project In VS Code`, or Tools → `Open Project In VS Code`
- Open current file: Option/Alt+Shift+O, editor context menu → `Open File In VS Code`, or Tools → `Open File In VS Code`

## ⚙️ Configuration
Settings/Preferences → Tools → Switch2VSCode → set the VS Code executable path (`code` by default). Shortcuts can be customized in Keymap settings.

## 🧑‍💻 Development
```bash
git clone https://github.com/Asxing/switch2vscode.git
cd switch2vscode
./gradlew buildPlugin
```

## 📄 License
This project is licensed under the [MIT License](LICENSE)

## 📮 Feedback
If you encounter issues or have suggestions, please open an issue: https://github.com/Asxing/switch2vscode/issues
