#!/bin/bash

#Jenkins执行这个脚本:编译正式版APK,上传到GitHub Release
#需要的环境变量见build_android.sh和build_github.sh

#遇到错误立即停止
set -e

# 当前 sh 文件所在目录
script_dir="$(cd "$(dirname "$0")" && pwd)"

$script_dir/build_android.sh
$script_dir/build_github.sh
