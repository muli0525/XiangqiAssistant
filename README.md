# 象棋辅助工具 - Android 版

一个功能强大的安卓象棋辅助工具，支持屏幕识别和 AI 分析。

## 功能特性

### ✅ 已实现
- 悬浮窗显示
- 屏幕截图权限
- 基础 UI 界面
- 服务架构

### 🚧 需要完善
- Pikafish 引擎集成（需要编译 ARM 版本）
- OpenCV 棋盘识别算法
- 棋子 OCR 识别
- 性能优化

## 编译步骤

### 1. 安装 Android Studio

下载并安装最新版 Android Studio：
https://developer.android.com/studio

### 2. 打开项目

1. 启动 Android Studio
2. 选择 "Open an Existing Project"
3. 选择 `XiangqiAssistant_Android` 文件夹
4. 等待 Gradle 同步完成

### 3. 配置 SDK

确保安装了以下组件：
- Android SDK Platform 34
- Android SDK Build-Tools 34.0.0
- Android NDK（用于编译 C++ 引擎）

### 4. 编译 Pikafish 引擎（重要）

#### 方法 A：使用预编译版本
1. 从 GitHub 下载 ARM 版本：
   https://github.com/official-pikafish/Pikafish/releases
2. 下载 `pikafish-android-arm64-v8a` 和 `pikafish-android-armeabi-v7a`
3. 放入项目：
   ```
   app/src/main/jniLibs/arm64-v8a/libpikafish.so
   app/src/main/jniLibs/armeabi-v7a/libpikafish.so
   ```

#### 方法 B：自己编译
```bash
cd Pikafish/src
make build ARCH=armv8 COMP=ndk
make build ARCH=armv7 COMP=ndk
```

### 5. 编译 APK

#### 方法 A：使用 Android Studio
1. 点击菜单 `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. 等待编译完成（首次需要 10-30 分钟）
3. APK 位置：`app/build/outputs/apk/debug/app-debug.apk`

#### 方法 B：使用命令行
```bash
cd XiangqiAssistant_Android
./gradlew assembleDebug
```

### 6. 安装到手机

#### 方法 A：USB 连接
1. 手机开启开发者选项和 USB 调试
2. 连接电脑
3. Android Studio 点击 Run 按钮

#### 方法 B：直接安装 APK
1. 将 `app-debug.apk` 复制到手机
2. 手机上点击安装
3. 允许"未知来源"安装

## 使用说明

### 首次使用

1. **授予权限**
   - 悬浮窗权限
   - 截屏权限
   - 存储权限

2. **启动服务**
   - 点击"启动悬浮窗"
   - 点击"开始屏幕识别"

3. **使用悬浮窗**
   - 打开其他象棋 APP
   - 悬浮窗会自动识别棋盘
   - 显示 AI 建议走法

### 注意事项

⚠️ **重要提示**：
- 本工具仅供学习研究使用
- 请勿用于在线对弈作弊
- 使用本工具可能违反象棋平台服务条款
- 开发者不承担任何法律责任

## 技术架构

### 核心技术
- **语言**：Kotlin
- **UI 框架**：Android View + Material Design
- **图像处理**：OpenCV for Android
- **引擎**：Pikafish (C++)
- **并发**：Kotlin Coroutines

### 项目结构
```
app/src/main/
├── java/com/xiangqi/assistant/
│   ├── MainActivity.kt              # 主界面
│   ├── service/
│   │   ├── FloatingWindowService.kt # 悬浮窗服务
│   │   └── ScreenCaptureService.kt  # 截屏服务
│   ├── engine/
│   │   └── PikafishEngine.kt        # 引擎接口
│   └── vision/
│       └── BoardRecognizer.kt       # 棋盘识别
├── res/
│   └── layout/
│       ├── activity_main.xml        # 主界面布局
│       └── floating_window.xml      # 悬浮窗布局
└── AndroidManifest.xml              # 权限配置
```

## 开发计划

### 短期目标
- [ ] 完善 Pikafish 引擎集成
- [ ] 改进棋盘识别算法
- [ ] 添加棋子 OCR 识别
- [ ] 优化性能和电量消耗

### 长期目标
- [ ] 支持多种棋盘样式
- [ ] 添加手动摆棋功能
- [ ] 支持棋谱保存
- [ ] 添加开局库查询
- [ ] 支持残局练习

## 常见问题

### Q: 编译失败怎么办？
A: 
1. 检查 Android Studio 版本（建议最新版）
2. 检查 Gradle 版本
3. 清理项目：`Build` → `Clean Project`
4. 重新同步：`File` → `Sync Project with Gradle Files`

### Q: 识别不准确怎么办？
A: 
1. 确保棋盘清晰可见
2. 调整手机亮度
3. 避免反光
4. 当前版本识别算法较简单，需要进一步优化

### Q: 引擎不工作怎么办？
A: 
1. 检查是否正确放置了引擎文件
2. 检查文件权限
3. 查看 Logcat 日志

### Q: 悬浮窗无法显示？
A: 
1. 检查是否授予了悬浮窗权限
2. 部分手机需要在设置中手动开启
3. MIUI/EMUI 等系统可能有额外限制

## 许可证

本项目基于 GPL-3.0 许可证开源。

Pikafish 引擎遵循其原始许可证。

## 免责声明

本工具仅供学习和研究使用。使用本工具进行任何违反象棋平台服务条款或法律法规的行为，后果由使用者自行承担。开发者不对使用本工具造成的任何损失或法律问题负责。

## 联系方式

如有问题或建议，请提交 Issue。

---

**祝你编译顺利！** 🎉
