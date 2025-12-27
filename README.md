# Switch2VSCode

[中文文档](README_zh.md)

A JetBrains IDE plugin that intelligently opens files and projects in VS Code-based editors while preserving caret position and context.

Inspired by and grateful to [qczone/switch2cursor](https://github.com/qczone/switch2cursor).

## 🌟 1.2.0 Key Highlights

### 🚀 Smart Multi-Editor Support
- **6 VS Code Family Editors**: Visual Studio Code, Cursor, Windsurf, AntiGravity, CatPaw, Trae
- **Intelligent Discovery Engine**: Automatically detects installed editors with zero configuration
- **White-list Filtering**: Precise recognition prevents false positives and unwanted entries

### 🔍 Three Discovery Strategies
- **Fast (< 1s)**: Command-line discovery only, perfect for daily use
- **Comprehensive (2-5s)**: Command-line + application scanning, balanced approach (default)
- **Smart (1-3s, cached)**: Full discovery with intelligent caching for optimal performance

### 🖥️ Cross-Platform Optimization
- **macOS**: Application bundle recognition + enhanced PATH scanning
- **Windows**: Registry scanning + program directory detection
- **Linux**: Package manager integration + desktop file parsing

## 🎯 Supported Editors

Switch2VSCode focuses on the VS Code ecosystem, providing native support for:

| Editor | Description | Auto-Discovery | Special Features |
|--------|-------------|----------------|------------------|
| **Visual Studio Code** | Microsoft's official VS Code | ✅ | Full feature support |
| **Cursor** | AI-powered code editor | ✅ | AI-assisted development |
| **Windsurf** | Collaborative coding environment | ✅ | Real-time collaboration |
| **AntiGravity** | Lightweight code editor | ✅ | Fast startup |
| **CatPaw** | Programming-focused editor | ✅ | Clean interface |
| **Trae** | Modern code editor | ✅ | Modern UI |

> 💡 **Why White-list?** We focus on the VS Code ecosystem to ensure every supported editor is thoroughly tested and optimized.

## ✨ Features

### Core Functionality
- Open current file in your chosen editor at the same line/column (`Option/Alt+Shift+O`)
- Open the entire project in your chosen editor (`Option/Alt+Shift+P`)
- Access via shortcuts, editor/project context menus, and Tools menu
- Preserve caret position and context when switching between IDEs
- Works seamlessly with JetBrains IDEs 2022.3+

### 1.2.0 Enhanced Features
#### Intelligent Editor Discovery
- Automatic detection of installed VS Code family editors
- Cross-platform optimization for macOS, Windows, and Linux
- White-list filtering prevents misidentification
- Multi-level verification ensures discovered editors are functional

#### Flexible Discovery Strategies
Choose the strategy that best fits your workflow:
- **Fast Discovery**: Command-line only, < 1 second response time
- **Comprehensive Discovery**: Balanced approach with deep scanning (default)
- **Smart Discovery**: Full scanning with intelligent caching

#### Zero-Configuration Experience
- Most users can start using immediately without manual configuration
- Automatic editor selection based on availability and user preferences
- Intelligent fallback to manual configuration if auto-discovery fails

## 🚀 Quick Start

### 30-Second Setup Guide

1. **Install Plugin**: IDE → Plugins → Search "Switch2VSCode" → Install
2. **Auto-Configuration**: First use automatically discovers installed editors
3. **Start Using**: `Alt+Shift+O` to open files, `Alt+Shift+P` to open projects

### First-Time Configuration Wizard

The plugin will automatically:
- 🔍 Scan for installed VS Code family editors
- ⚙️ Configure optimal settings
- ✅ Verify editor availability
- 🎯 Recommend the best discovery strategy for your system

> 💡 **Zero-Configuration Experience**: Most users can start using immediately without any manual setup!

## 🔧 Smart Configuration

### Discovery Strategy Selection

Choose based on your use case:

#### 🚀 Fast Discovery (Recommended for daily use)
```
Characteristics: Lightning-fast response, < 1 second
Best for: Known editor locations, efficiency-focused users
Method: Command-line detection only
```

#### 🔍 Comprehensive Discovery (Recommended for first-time setup)
```
Characteristics: Deep scanning, 2-5 seconds
Best for: Initial configuration, discovering all editors
Method: Command-line + application directory scanning
```

#### 🧠 Smart Discovery (Recommended for power users)
```
Characteristics: Intelligent caching, 1-3 seconds
Best for: Frequent switching, need completeness
Method: Full discovery + smart caching
```

### Auto-Discovery Architecture

Switch2VSCode uses a dual-layer discovery engine:

1. **Command-Line Layer**: Detects editors in system PATH
2. **Application Layer**: Scans system application directories
3. **Smart Filtering**: White-list mechanism ensures accurate matching
4. **Version Detection**: Automatically retrieves editor version information

## ⚙️ Configuration

### Settings Location
Settings/Preferences → Tools → Switch2VSCode

### Configuration Options
- **Editor Selection**: Choose from automatically discovered editors
- **Discovery Strategy**: Fast/Comprehensive/Smart based on your preference
- **Custom Path**: Manual editor path if auto-discovery fails
- **Advanced Options**: Custom command-line arguments and launch preferences

### Keyboard Shortcuts
Customize shortcuts in Keymap settings by searching for "Switch2VSCode":
- Default file shortcut: `Option/Alt+Shift+O`
- Default project shortcut: `Option/Alt+Shift+P`

## 🛠️ Installation

### From JetBrains Marketplace (Recommended)
1. Open your JetBrains IDE
2. Go to Settings/Preferences → Plugins
3. Search for "Switch2VSCode"
4. Click Install and restart your IDE

### Manual Installation
1. Build the plugin: `./gradlew buildPlugin`
2. Install from disk: IDE → Settings/Preferences → Plugins → ⚙️ → Install Plugin from Disk...
3. Select the zip file from `build/distributions`

## 🔧 Troubleshooting

### Auto-Discovery Issues
If automatic discovery fails:
1. Try different discovery strategies (Fast → Comprehensive → Smart)
2. Manually specify editor path in Settings → Tools → Switch2VSCode
3. Ensure your editor is properly installed and accessible from command line

### Common Solutions
- **Editor not found**: Check if the editor is installed and in system PATH
- **Wrong editor opens**: Verify editor selection in plugin settings
- **Command fails**: Check editor executable permissions and path

## 🧑‍💻 Development

```bash
git clone https://github.com/Asxing/switch2vscode.git
cd switch2vscode
./gradlew buildPlugin
```

### Building and Testing
- Build: `./gradlew buildPlugin`
- Test: `./gradlew test`
- Run in IDE: `./gradlew runIde`

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Areas for Contribution
- Support for additional VS Code-based editors
- Platform-specific discovery improvements
- UI/UX enhancements
- Documentation improvements

## 📄 License

This project is licensed under the [MIT License](LICENSE)

## 📮 Support & Feedback

- **Issues**: [GitHub Issues](https://github.com/Asxing/switch2vscode/issues)
- **Feature Requests**: [GitHub Discussions](https://github.com/Asxing/switch2vscode/discussions)
- **Documentation**: [Wiki](https://github.com/Asxing/switch2vscode/wiki)

## 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=Asxing/switch2vscode&type=Date)](https://star-history.com/#Asxing/switch2vscode&Date)

---

**Switch2VSCode** - Bridging JetBrains IDEs and VS Code ecosystem with intelligence and simplicity.