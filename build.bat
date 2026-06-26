@echo off
chcp 65001
echo ========================================
echo  次元方舟视频管理系统 - 构建脚本
echo ========================================
echo.

echo [1/2] 清理旧的构建文件...
call mvn clean
if errorlevel 1 (
    echo ❌ 清理失败
    pause
    exit /b 1
)
echo ✅ 清理完成

echo.
echo [2/2] 打包应用...
call mvn package -Dmaven.test.skip=true
if errorlevel 1 (
    echo ❌ 打包失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ 构建成功!
echo ========================================
echo.
echo JAR文件位置: target\spglxt-video-system.jar
echo.
echo 运行命令:
echo   java -jar target\spglxt-video-system.jar
echo.

pause
