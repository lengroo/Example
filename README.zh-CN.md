**语言：** [English](README.md) | [中文](README.zh-CN.md)

# LibXposed 示例

一个最小化的、现代的 libxposed 模块（API 101/102），适用于 LSPosed / Vector。

它只做一件事，而且做得很直观：向 logcat 记录某个已选定作用域
应用的加载，以及该应用中某个 Activity 的恢复。它不改变任何
应用行为，不修改任何参数，也不替换任何返回值。

## 应用有两个界面

打开应用会直接进入 **MiUIX 界面**
（`ComposeActivity`，一个用 Compose + MiUIX `0.9.4` 构建的
`ComponentActivity`，见 https://github.com/compose-miuix-ui/miuix）。
它用 MiUIX 的 `Card` 展示：

- **About** —— 模块简介。
- **Install and try** —— 安装、启用和查看日志的步骤。
- **What it does** —— 只记录日志的承诺。
- **From the 1.0 interface** —— 原始框架界面的完整文本，
  原样保留。

MiUIX 顶栏右上角有一个按钮 **"Original"**（原始界面），
点击可切换到**原始界面**（`MainActivity`）。这个按钮和下面的
诊断功能都是调试功能：只在调试版中出现（见“调试版与发布版”）。
这个界面是特意只用
框架类构建的——不用 Compose、不用 MiUIX、不用 androidx——所以
即使 MiUIX 界面无法启动，它仍然可以正常工作。它展示：

- 同样的简介和 1.0 时代的文本。
- **Library checks（依赖库检查）** —— 每个关键的 Compose /
  MiUIX / androidx 类是否真的能加载（用 `Class.forName` 检查），
  显示为 `OK` / `FAIL`，而不是直接崩溃。
- **Last crash（上次崩溃）** —— 如果 MiUIX 界面曾经崩溃，
  `CrashLog` 会保存准确的堆栈信息，下次启动时在这里显示，
  并提供清除按钮。
- **"Open MiUIX interface"** —— 返回 MiUIX 界面。

两个界面都隐藏了系统标题栏（`NoActionBar`），所以每个界面
只显示自己的标题。

## Hook 能做什么

- 模块加载时，会以 `LibXposedExample` 标签向 logcat 输出日志。
- 当某个已选定作用域的应用就绪时，会记录该应用的包名。
- 它会 Hook 已选定作用域应用中的
  `android.app.Activity.onResume()`，记录每次恢复的 Activity
  的类名和包名，然后原样调用原始方法。

作用域被有意留空（`META-INF/xposed/scope.list` 中只有注释）：
请自己在 LSPosed Manager 中选择目标应用。
这样可以让示例保持通用，避免针对任何特定的第三方应用。

## 工作原理（现代 API，已对照 api-102.0.0 验证）

- 入口类：`com.example.libxposedexample.ExampleModule`
  继承自 `io.github.libxposed.api.XposedModule`，带有公开的无参
  构造函数。框架会自行调用 `attachFramework()`。
- 生命周期：`onModuleLoaded(ModuleLoadedParam)`、
  `onPackageReady(PackageReadyParam)`（由 `isFirstPackage` 守卫）。
- Hook：`hook(method).intercept { chain -> ...; chain.proceed() }`。
  `Chain.thisObject` 是接收者，`Chain.proceed()` 运行原始方法，
  `Chain.proceed(newArgs)` 则会用新参数运行原始方法。
- 发现机制（Manifest 中没有旧版元数据）：
  - `src/main/resources/META-INF/xposed/java_init.list` —— 入口类名
  - `src/main/resources/META-INF/xposed/module.prop` ——
    `minApiVersion=101`、`targetApiVersion=102`、`staticScope=false`、
    `exceptionMode=protective`
  - `src/main/resources/META-INF/xposed/scope.list` —— 默认作用域
- 依赖：`io.github.libxposed:api:102.0.0` 是 `compileOnly`
  （框架会在被 Hook 的进程运行时提供它）。
  `io.github.libxposed:service:102.0.0` 在 Gradle 构建中是
  `implementation`（它提供与框架通信的 Provider）；
  下文的手动构建说明中提到了该 AAR 的 d8 问题。

## 安装与试用

1. 在装有 LSPosed（API 101+）或 Vector（API 102）的设备上安装。
   该 APK 使用调试密钥签名，仅供测试使用。
   发布版本见 [Releases](https://github.com/lengroo/Example/releases) 页面。
2. 在 LSPosed Manager 中启用“LibXposed Example”并选择一个作用域应用。
3. 强制停止并重新启动该作用域应用（或重启设备）。
4. 查看 logcat：

```sh
adb logcat -s LibXposedExample
```

你应该能看到“Package ready: …”和“Activity resumed: …”这样的日志。
如果什么都没看到，请检查模块是否已启用、作用域是否包含你启动的
应用，以及该应用是否在启用后确实被重启过。

## 构建（常规方式）

```sh
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

需要 JDK 17、Android SDK 35，以及一个可正常工作的 Gradle 守护进程。

## 构建（手动方式，发布版 APK 所用的方式）

在构建发布版 APK 的虚拟机中，Gradle 守护进程无法与其客户端通信
（即使是一个极小的 Gradle 项目也会同样失败），
因此改为一步一步手动构建 APK：

```sh
./build-manual-compose.sh
# app-debug.apk
```

该脚本依次运行：`aapt2 compile/link`（应用 + 依赖库 AAR 的资源，
生成 R 类）→ 带 Compose 编译器插件的 `kotlinc` →
为生成的 R 类运行 `javac` → `d8`（独立的 R8/D8 8.11.32，因为
build-tools 34.0.0 的 d8 在处理多个较新的 androidx 类时会崩溃）→
zip 组装（加入 `classes*.dex` 和 `META-INF/xposed/*`）→
`zipalign` → `apksigner`（调试密钥）。

它需要 `build-manual-compose/jars/` 中的依赖库 jar（从 MiUIX /
Compose / activity AAR 中提取的 classes.jar，以及
lifecycle-common 和 kotlinx-coroutines）；该目录是本地构建缓存，
不属于源码树。

手动构建有两个经验教训，记录在构建脚本中，手动重建时值得了解：

- **生成的 R 类必须打包进 dex。** 它们与应用类分开存放，
  而 `ViewCompat` 这样的库代码会把 `androidx.core.R$id`
  当作非 final 字段引用，所以丢掉它们的构建会在运行时以
  `NoClassDefFoundError` 失败。
- **拆分出来的产物必须一并包含，不能只包含父产物。**
  `androidx.core.viewtree.ViewTree` 不在 `core-1.16.0` 里——它在
  `core-viewtree 1.0.0` 里；MiUIX 的
  `RuntimeShader_androidKt` 也不在 `miuix-ui` 里——它在
  `miuix-shader-android 0.9.4` 里，那是 `miuix-squircle` 的依赖。
  缺少其中任何一个，MiUIX 界面一开始使用就会崩溃。

关于 service AAR 的说明：build-tools 34.0.0 的 d8 在处理
`io.github.libxposed.service.HookedTarget$State` 时会因
NullPointerException 而失败，因此手动构建只打包了模块自身的
类、Compose / MiUIX / activity 依赖库和 kotlin-stdlib，没有打包
service AAR。Hook 本身并不使用 service API，所以生成的 APK
仍然是一个有效的模块；如果你用 Gradle 构建，service 类会像
往常一样被包含进来。

## 文件

- `app/src/main/java/com/example/libxposedexample/ExampleModule.kt`
  —— Hook 入口
- `app/src/main/java/com/example/libxposedexample/ComposeActivity.kt`
  —— MiUIX 界面（默认入口）
- `app/src/main/java/com/example/libxposedexample/MainActivity.kt`
  —— 原始框架界面：依赖库检查、崩溃日志，以及返回 MiUIX 的按钮
- `app/src/main/java/com/example/libxposedexample/CrashLog.kt`
  —— 保存未捕获的 MiUIX 崩溃，供原始界面展示
- `app/src/main/AndroidManifest.xml`
- `app/src/main/resources/META-INF/xposed/{module.prop,java_init.list,scope.list}`
- `app/build.gradle.kts`、`gradle/libs.versions.toml`、`build-manual-compose.sh`

## 版本说明

- **1.0** —— 原始框架界面。
- **1.1.0–1.1.1** —— 首次 MiUIX 重构；在某些设备上启动即崩溃，
  因为 dex 中缺少真正的运行时类。
- **1.1.2–1.1.6** —— 诊断拆分（框架启动器 + MiUIX Activity），
  然后根据启动器中显示的崩溃日志，一次补上一个缺失的类：
  `FastSafeIterableMap`、`ViewTree`，再到 MiUIX 的
  `RuntimeShader_androidKt`。
- **1.1.7** —— MiUIX 成为默认入口，隐藏系统标题栏，
  底部按钮可切换回原始界面。
- **1.1.8** —— 切换按钮移到顶栏，同时发布调试版和发布版
  两个 APK（见下文）。
- **1.1.9** —— 去掉标题栏下方多余的空白：删除 1.1.4 增加的
  32dp 顶部内边距和 16dp 占位，只保留 Scaffold 自身的顶栏
  内边距加上 "About" 上方 12dp 的内容内边距。从 1.1.9 起，
  只打包和发布调试版。
- **1.1.10** —— 切换界面时复用已有的 Activity
  （`FLAG_ACTIVITY_REORDER_TO_FRONT`），而不是每次点击都
  创建一个新的，这样返回栈永远不会超过两个界面，返回时
  也不再需要按切换次数多次返回。
- **1.1.11** —— 启动器（桌面）图标更换为用户提供的图片，
  以 192x192 的 `mipmap-xxxhdpi/ic_launcher.png` 提供，
  并在 manifest 中设为应用图标和圆形图标。

## Telegram 转发

发布新版本时，GitHub Actions 工作流
（`.github/workflows/notify-telegram.yml`）会自动通过 Telegram
Bot API 把发布版 APK 发送到 Telegram 频道。它需要两个仓库
密钥（Settings > Secrets and variables > Actions）：
`TELEGRAM_BOT_TOKEN` 和 `TELEGRAM_CHAT_ID`。这两个值都不
会存放在本仓库中——工作流只引用密钥名称。如需在不发布新版本
的情况下测试，可在 Actions 标签页手动运行该工作流，并填入
一个已有版本的标签。

## 调试版与发布版

同一份源码可以产出两个版本，依据 APK 是否可调试来区分
（`DebugFeatures`，检查 `ApplicationInfo.FLAG_DEBUGGABLE`）：

- **调试版**（`app-debug.apk`，`aapt2 --debug-mode`，以及
  Gradle 的 debug 构建类型）：MiUIX 顶栏显示 "Original"
  切换按钮，原始界面显示依赖库检查和上次崩溃，
  `CrashLog` 会记录崩溃。从 1.1.9 起，只打包和发布这个版本。
- **发布版**（`app-release.apk`，不加 `--debug-mode`，以及
  Gradle 的 release 构建类型）：不出现上述任何调试界面。
  MiUIX 界面是干净的，没有切换按钮。从 1.1.9 起不再打包，
  这是用户的决定。

两者在这里都用本地调试密钥签名（仅供测试）；正式发布版
需要自己的发布密钥。注意：诊断类仍然存在于发布版的 dex
中，只是界面不可达——没有切换按钮可以打开它们。
