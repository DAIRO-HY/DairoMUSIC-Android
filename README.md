# DairoMUSIC-Android

[DairoMUSIC](https://github.com/DAIRO-HY/DairoMUSIC) 的 Android 客户端，Jetpack Compose 编写。功能、界面和代码结构与 [DairoMUSIC-IOS](https://github.com/DAIRO-HY/DairoMUSIC-IOS) 保持一致，类名、包名、文件名一一对应，方便两边同步维护。

## 运行环境

- Android 8.0（API 26）及以上，支持手机和平板（arm64-v8a、armeabi-v7a、x86_64）
- 编译：JDK 25（Gradle 会通过 foojay 自动下载）、Android SDK 37

## 依赖

| 依赖 | 说明 | 引用方式 |
|---|---|---|
| [DairoUI_Android](https://github.com/DAIRO-HY/DairoUI_Android) | 设置页风格的 UI 组件、Toast、Loading、主题 | `../DairoUI_Android` 存在时直接引用本地源码（includeBuild），否则使用 jitpack |
| [DairoDFS](https://github.com/DAIRO-HY/DairoDFS) 的 Android SDK | 文件下载、边播边缓存、离线文件管理 | `app/libs/dairo-file-sdk-*.aar` |

更新 SDK：在 DairoDFS 里执行 `./dairo-file-sdk/build.sh android`，把 `dairo-file-sdk/dist/android/` 下的 AAR 复制到 `app/libs/`（文件名变了时同时修改 `app/build.gradle.kts`）。

## 签名

签名信息不在代码里，按下面的顺序读取，都没有时 debug 使用默认的调试签名，release 生成未签名的 APK：

1. 环境变量 `DAIRO_KEYSTORE_FILE`、`DAIRO_KEYSTORE_PASSWORD`、`DAIRO_KEY_ALIAS`、`DAIRO_KEY_PASSWORD`（Jenkins 用，见 `Jenkinsfile`）
2. 项目根目录的 `keystore.properties`（本机用，格式见 `keystore.properties.example`，不提交到 git）

## 和 iOS 版的差异

- 没有毛玻璃效果，浮动的播放条、Tab 栏、搜索框用不透明背景加阴影
- 后台播放、通知栏/锁屏控制使用 Media3 的 `MediaSessionService`（`player/PlaybackService.kt`）
- 实时频谱通过 ExoPlayer 音频处理链上的 `TeeAudioProcessor` 获取 PCM 数据，不需要录音权限
- 登录票据用 Android Keystore 加密保存（`util/KeychainUtil.kt`）；设备标识由 ANDROID_ID 计算，卸载重装后不变
- "关于"页面保留"检查更新"（不经过应用商店）
- 保存视频到相册：Android 10 及以上不需要权限，Android 9 及以下需要存储权限

## 隐私政策

[PRIVACY.md](https://github.com/DAIRO-HY/DairoMUSIC-IOS/blob/main/PRIVACY.md)（和 iOS 版共用，App 内的"隐私政策"页面按 Android 的权限调整了措辞）

## 开源协议

[Apache License 2.0](./LICENSE)
