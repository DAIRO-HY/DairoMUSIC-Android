#!/bin/bash

#编译正式版APK,输出到 build/DairoMUSIC-{版本号}.apk
#第一次运行时自动安装JDK和Android SDK到 ~/develop_tool,Gradle由项目里的gradlew自动下载
#
#签名需要的环境变量(在Jenkins里配置,不要写到脚本里):
#  DAIRO_MUSIC_KEYSTORE_FILE      签名文件的路径
#  DAIRO_MUSIC_KEYSTORE_PASSWORD  签名文件的密码
#  DAIRO_MUSIC_KEY_ALIAS          密钥别名
#  DAIRO_MUSIC_KEY_PASSWORD       密钥的密码
#没有设置时生成未签名的APK,这时直接报错退出,不能发布未签名的APK

#遇到错误立即停止
set -e

# 当前 sh 文件所在目录
script_dir="$(cd "$(dirname "$0")" && pwd)"

# 项目目录
project_root="$(cd "$script_dir/../.." && pwd)"

#开发工具安装目录
develop_tool_dir=$(echo ~)/develop_tool
mkdir -p $develop_tool_dir

# 获取版本号
version=$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' $project_root/app/build.gradle.kts)
echo "版本号:$version"

#先创建文件夹
mkdir -p $project_root/build

#CPU架构,JDK和Android SDK只区分x64和aarch64
case "$(uname -m)" in
  x86_64|amd64) arch="x64" ;;
  aarch64|arm64) arch="aarch64" ;;
  *) echo "不支持的CPU架构:$(uname -m)"; exit 1 ;;
esac

#--------------------------------------安装JDK--------------------------------------START
#项目需要JDK 25(和DairoUI_Android一致),从Adoptium下载
jdk_dir=$develop_tool_dir/jdk-25
if [ ! -d $jdk_dir ]; then
    cd /tmp
    curl -L -o openjdk.tar.gz "https://api.adoptium.net/v3/binary/latest/25/ga/linux/${arch}/jdk/hotspot/normal/eclipse"
    rm -rf /tmp/jdk-25-extract
    mkdir -p /tmp/jdk-25-extract
    tar -zxf openjdk.tar.gz -C /tmp/jdk-25-extract
    rm openjdk.tar.gz

    #压缩包里只有一个目录,目录名带小版本号,统一改成jdk-25
    mv /tmp/jdk-25-extract/* $jdk_dir
    rm -rf /tmp/jdk-25-extract
else
    echo "JDK already exists."
fi

# 配置JAVA_HOME环境变量
export JAVA_HOME="$jdk_dir"
export PATH=$JAVA_HOME/bin:$PATH
java --version
#--------------------------------------安装JDK--------------------------------------END

#修改 .gradle 存放位置,默认在~/.gradle
export GRADLE_USER_HOME="${develop_tool_dir}/.gradle"

#--------------------------------------安装Android SDK--------------------------------------START
if [ ! -d $develop_tool_dir/AndroidSDK/cmdline-tools/latest ]; then
    mkdir -p $develop_tool_dir/AndroidSDK/cmdline-tools
    cd $develop_tool_dir/AndroidSDK/cmdline-tools
    curl -o commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip
    unzip -q commandlinetools.zip
    rm commandlinetools.zip
    mv cmdline-tools latest
else
  echo "Android SDK command line tools already exist."
fi
export ANDROID_HOME="${develop_tool_dir}/AndroidSDK"
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:${PATH}"

#修改 .android 存放位置,默认在~/.android
export ANDROID_USER_HOME="${develop_tool_dir}/.android"

sdkmanager --version

#和app/build.gradle.kts里的compileSdk一致,已经安装的不会重复下载
yes | sdkmanager "build-tools;37.0.0" "platform-tools" "platforms;android-37.2" > /dev/null

#添加授权
yes | sdkmanager --licenses > /dev/null
#--------------------------------------安装Android SDK--------------------------------------END

#--------------------------------------编译APK--------------------------------------START
if [ -z "$DAIRO_MUSIC_KEYSTORE_FILE" ]; then
  echo "没有设置签名的环境变量DAIRO_MUSIC_KEYSTORE_FILE,不能编译正式版"
  exit 1
fi

cd $project_root

#local.properties里是本机的Android SDK路径,编译服务器上用ANDROID_HOME
rm -f local.properties

./gradlew clean :app:assembleRelease --no-daemon --console=plain

apk=$project_root/app/build/outputs/apk/release/app-release.apk
if [ ! -f $apk ]; then
  echo "没有找到签名的APK:$apk"
  exit 1
fi
mv $apk $project_root/build/DairoMUSIC-$version.apk
echo "编译完成:build/DairoMUSIC-$version.apk"
#--------------------------------------编译APK--------------------------------------END
