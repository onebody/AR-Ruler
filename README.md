# AR测距助手 (AR Measure Helper)

一款基于ARCore技术的增强现实距离测量应用，帮助用户快速准确地测量现实世界中的距离。

## 功能特性

- **AR实时测量**：利用增强现实技术，在实景画面中进行精准测量
- **十字架瞄准**：画面中央的红色十字架标记，方便用户精确瞄准目标
- **多点测量**：支持起点和终点标记，自动计算两点间距离
- **两种模式**：支持水平测距和垂直测高模式切换
- **单位切换**：支持米和英尺两种单位显示
- **精度提示**：实时显示测量精度等级和操作建议

## 技术栈

- **平台**：Android (API 24+)
- **AR引擎**：ARCore 1.32.0
- **渲染**：OpenGL ES 2.0
- **架构**：MVP (Model-View-Presenter)

## 使用说明

1. 打开应用，等待AR相机初始化完成
2. 移动手机使屏幕中央的红色十字架对准第一个测量点
3. 点击屏幕任意位置标记起点
4. 移动手机使十字架对准第二个测量点
5. 点击屏幕标记终点
6. 系统自动计算并显示两点间的距离

## 项目结构

```
android/
├── app/
│   ├── src/main/java/com/phonecl/armeasure/
│   │   ├── MainActivity.java          # 主界面
│   │   ├── core/                      # 核心算法
│   │   │   └── DistanceCalculator.java
│   │   ├── data/                      # 数据管理
│   │   │   └── ARDataManager.java
│   │   ├── presenter/                 # 业务逻辑
│   │   │   ├── MeasurePresenter.java
│   │   │   └── MeasureContract.java
│   │   └── render/                    # AR渲染
│   │       └── ARRenderer.java
│   └── src/main/res/
│       ├── layout/
│       │   └── activity_main.xml
│       └── drawable/
│           ├── circle_green.xml
│           ├── circle_red.xml
│           └── circle_ring.xml
└── build.gradle
```

## 安装步骤

```bash
# 克隆项目
git clone https://github.com/onebody/AR-Ruler.git

# 进入项目目录
cd AR-Ruler

# 使用Android Studio打开项目或直接编译
cd android
./gradlew assembleDebug

# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request！