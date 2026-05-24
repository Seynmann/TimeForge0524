# TimeForge 安卓最终成品版

这是一个尽量简单、可直接打包的 Android 原生项目：

- Android Java 外壳
- WebView 本地界面
- HTML / CSS / JavaScript 实现主要功能
- 数据保存在 SharedPreferences，覆盖安装不丢数据
- 支持导出 JSON 备份
- 支持热力图导出分享图片
- 支持本地通知提醒

## 已包含功能

1. 今日作战台
   - 今日任务
   - 当前时间与今日时间进度环
   - 今日完成进度
   - 晨间签到 / 睡前签到
   - 重要日子倒计时

2. 日历系统
   - 周视图
   - 月视图
   - 新建日程/任务
   - 任务开始前提醒

3. 专注系统
   - 任务列表
   - 自动绑定计时器
   - 一键开始
   - 完成任务获得积分

4. 习惯系统
   - 习惯打卡
   - 最近一月热力图
   - 最近一年热力图
   - 全部习惯彩色热力图
   - 一键导出分享图片

5. 成长系统
   - 积分累计
   - 随机奖励
   - 早起奖励
   - 睡前奖励
   - 段位称号
   - 积分流水

6. 数据安全
   - 覆盖安装保留数据
   - JSON 导出备份
   - JSON 粘贴导入恢复

## 如何打包 APK

### 方法一：GitHub Actions 自动打包

1. 新建一个 GitHub 仓库。
2. 把本项目所有文件上传。
3. 打开仓库的 Actions 页面。
4. 运行 `Build Debug APK`。
5. 下载 Artifact：`TimeForge-debug-apk`。
6. 解压得到 `app-debug.apk`。
7. 使用 ADB 安装：

```bash
adb install -r app-debug.apk
```

### 方法二：Android Studio 打包

1. 用 Android Studio 打开项目根目录。
2. 等 Gradle Sync 完成。
3. 点击：Build → Build Bundle(s) / APK(s) → Build APK(s)。
4. APK 位置通常是：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 数据是否会保留

会。只要满足：

- 不卸载 App
- 不清除应用数据
- 包名不变：`com.timeforge.simple`
- 用 `adb install -r` 覆盖安装
- 后续继续使用同一项目和同一包名

数据一般都会保留。

如果担心丢数据，请在 App 内进入：

```text
成长 → 数据安全 → 导出备份
```

## 最重要的源码文件

界面和核心逻辑主要在：

```text
app/src/main/assets/index.html
```

Android 外壳和通知、分享功能在：

```text
app/src/main/java/com/timeforge/simple/MainActivity.java
app/src/main/java/com/timeforge/simple/AlarmReceiver.java
```
