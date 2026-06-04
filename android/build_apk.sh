#!/bin/bash
# AR测量APP编译脚本
# 使用方法：在终端中执行 ./build_apk.sh

echo "========================================"
echo "AR测量APP 编译脚本"
echo "========================================"

# 进入项目目录
cd "$(dirname "$0")"

# 检查local.properties
if [ ! -f "local.properties" ]; then
    echo "正在检测Android SDK路径..."
    
    # 尝试自动检测SDK路径
    if [ -d "$HOME/Library/Android/sdk" ]; then
        SDK_PATH="$HOME/Library/Android/sdk"
    elif [ -d "$HOME/Android/Sdk" ]; then
        SDK_PATH="$HOME/Android/Sdk"
    elif [ -d "$ANDROID_HOME" ]; then
        SDK_PATH="$ANDROID_HOME"
    else
        echo "错误：未找到Android SDK，请手动设置"
        echo "请创建 local.properties 文件，内容如下："
        echo "sdk.dir=/path/to/your/android/sdk"
        exit 1
    fi
    
    echo "sdk.dir=$SDK_PATH" > local.properties
    echo "已设置SDK路径: $SDK_PATH"
fi

# 赋予gradlew执行权限
chmod +x gradlew

# 清理旧的编译文件
echo ""
echo "正在清理旧编译文件..."
./gradlew clean

# 编译Debug APK
echo ""
echo "正在编译Debug APK..."
./gradlew assembleDebug

# 检查编译结果
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo ""
    echo "========================================"
    echo "编译成功！"
    echo "========================================"
    echo ""
    echo "APK位置："
    echo "$(pwd)/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    
    # 尝试打开输出目录
    if command -v open &> /dev/null; then
        open app/build/outputs/apk/debug/
    elif command -v xdg-open &> /dev/null; then
        xdg-open app/build/outputs/apk/debug/
    fi
else
    echo ""
    echo "编译失败，请检查错误信息"
    exit 1
fi
