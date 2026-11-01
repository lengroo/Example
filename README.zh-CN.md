**语言：** [English](README.md) | [中文](README.zh-CN.md)

# LibXposed 示例

一个最小化的、现代的 libxposed 模块（API 101/102），适用于 LSPosed / Vector。

启动界面使用 Compose + MiUIX 构建
（`top.yukonga.miuix.kmp:miuix-ui-android:0.9.4`，见
https://github.com/compose-miuix-ui/miuix）：`MainActivity` 是一个
`ComponentActivity`，调用 `setContent { MiuixTheme { Scaffold { ... } } }`，
包含一个 `SmallTopAppBar` 和一个由 `Card` 组成的 `LazyColumn`。Hook 本身
（`ExampleModule`）没有变化，也不使用 Compose 或 MiUIX。

## 它能做什么

没有任何破坏性行为，也不会改变任何应用的行为：

- 模块加载时，会以 `LibXposedExample` 标签向 logcat 输出日志。
- 当某个已选定作用域的应用就绪时，会记录该应用的包名。
- 它会 Hook 已选定作用域应用中的 `android.app.Activity.onResume()`，
  并记录每次恢复的 Activity 的类名，然后原样调用原始方法。
  不修改任何参数，也不替换任何返回值。

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
  下文的手动构建说明中提到了该 AAR 的 d8 问题，见下文。

## 构建（常规方式）

```sh
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

需要 JDK 17、Android SDK 35，以及一个可正常工作的 Gradle 守护进程。

## 构建（手动方式，本仓库附带 APK 所用的方式）

在构建该 APK 的虚拟机中，Gradle 守护进程无法与其客户端通信
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

关于 service AAR 的说明：build-tools 34.0.0 的 d8 在处理
`io.github.libxposed.service.HookedTarget$State` 时会因
NullPointerException 而失败，因此手动构建只打包了模块自身的
类、Compose / MiUIX / activity 依赖库和 kotlin-stdlib，没有打包
service AAR。Hook 本身并不使用 service API，所以生成的 APK
仍然是一个有效的模块；如果你用 Gradle 构建，service 类会像
往常一样被包含进来。

## 安装与试用

1. 在装有 LSPosed（API 101+）或 Vector（API 102）的设备上安装。
   该 APK 使用调试密钥签名，仅供测试使用。
2. 在 LSPosed Manager 中启用“LibXposed Example”并选择一个作用域应用。
3. 强制停止并重新启动该作用域应用（或重启设备）。
4. 查看 logcat：

```sh
adb logcat -s LibXposedExample
```

你应该能看到“Package ready: …”和“Activity resumed: …”这样的日志。
如果什么都没看到，请检查模块是否已启用、作用域是否包含你启动的
应用，以及该应用是否在启用后确实被重启过。

## 文件

- `app/src/main/java/com/example/libxposedexample/ExampleModule.kt`
  —— Hook 入口
- `app/src/main/java/com/example/libxposedexample/MainActivity.kt`
  —— 一个简单的启动说明页面
- `app/src/main/AndroidManifest.xml`
- `app/src/main/resources/META-INF/xposed/{module.prop,java_init.list,scope.list}`
- `app/build.gradle.kts`、`gradle/libs.versions.toml`、`build-manual-compose.sh`
