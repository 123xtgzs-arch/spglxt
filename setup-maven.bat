@echo off
chcp 65001 >nul
echo ========================================
echo  Maven 自动安装脚本
echo ========================================
echo.

REM 检查是否已安装Maven
mvn -version >nul 2>&1
if not errorlevel 1 (
    echo ✅ Maven 已安装！
    mvn -version
    echo.
    pause
    exit /b 0
)

echo 未检测到Maven，开始自动安装...
echo.

REM 设置Maven版本和下载地址
set MAVEN_VERSION=3.9.6
set MAVEN_HOME_DIR=C:\apache-maven-%MAVEN_VERSION%
set DOWNLOAD_URL=https://dlcdn.apache.org/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip

echo [1/5] 准备下载 Maven %MAVEN_VERSION%...
echo 下载地址: %DOWNLOAD_URL%
echo 安装位置: %MAVEN_HOME_DIR%
echo.

REM 检查安装目录是否已存在
if exist "%MAVEN_HOME_DIR%" (
    echo Maven目录已存在，跳过下载
    goto :configure
)

REM 创建临时目录
set TEMP_DIR=%TEMP%\maven-install
if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"

echo [2/5] 下载 Maven...
echo 正在下载... 请稍候（文件约9MB）
echo.

REM 使用PowerShell下载文件
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; try { Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%TEMP_DIR%\maven.zip' -ErrorAction Stop; Write-Host '✅ 下载完成' } catch { Write-Host '❌ 下载失败，尝试使用国内镜像...'; Invoke-WebRequest -Uri 'https://mirrors.aliyun.com/apache/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%TEMP_DIR%\maven.zip' }}"

if not exist "%TEMP_DIR%\maven.zip" (
    echo.
    echo ❌ 下载失败！
    echo.
    echo 请手动下载Maven:
    echo 官方地址: https://maven.apache.org/download.cgi
    echo 国内镜像: https://mirrors.aliyun.com/apache/maven/maven-3/
    echo.
    echo 下载后解压到: C:\apache-maven-%MAVEN_VERSION%
    echo 然后重新运行本脚本配置环境变量
    pause
    exit /b 1
)

echo.
echo [3/5] 解压 Maven...
powershell -Command "& {Expand-Archive -Path '%TEMP_DIR%\maven.zip' -DestinationPath 'C:\' -Force; Write-Host '✅ 解压完成'}"

REM 检查解压是否成功
if not exist "%MAVEN_HOME_DIR%\bin\mvn.cmd" (
    echo ❌ 解压失败！
    pause
    exit /b 1
)

:configure
echo.
echo [4/5] 配置环境变量...

REM 设置MAVEN_HOME环境变量
powershell -Command "& {[Environment]::SetEnvironmentVariable('MAVEN_HOME', '%MAVEN_HOME_DIR%', 'Machine'); Write-Host '✅ MAVEN_HOME 已设置'}"

REM 添加到PATH
powershell -Command "& {$path = [Environment]::GetEnvironmentVariable('Path', 'Machine'); if ($path -notlike '*%MAVEN_HOME_DIR%\bin*') { [Environment]::SetEnvironmentVariable('Path', $path + ';%MAVEN_HOME_DIR%\bin', 'Machine'); Write-Host '✅ PATH 已更新' } else { Write-Host '✅ PATH 已包含Maven' }}"

echo.
echo [5/5] 配置Maven镜像...

REM 创建Maven配置目录
set MAVEN_CONF=%USERPROFILE%\.m2
if not exist "%MAVEN_CONF%" mkdir "%MAVEN_CONF%"

REM 创建settings.xml配置文件
echo ^<?xml version="1.0" encoding="UTF-8"?^> > "%MAVEN_CONF%\settings.xml"
echo ^<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" >> "%MAVEN_CONF%\settings.xml"
echo           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >> "%MAVEN_CONF%\settings.xml"
echo           xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 >> "%MAVEN_CONF%\settings.xml"
echo           http://maven.apache.org/xsd/settings-1.0.0.xsd"^> >> "%MAVEN_CONF%\settings.xml"
echo. >> "%MAVEN_CONF%\settings.xml"
echo     ^<localRepository^>%USERPROFILE%\.m2\repository^</localRepository^> >> "%MAVEN_CONF%\settings.xml"
echo. >> "%MAVEN_CONF%\settings.xml"
echo     ^<mirrors^> >> "%MAVEN_CONF%\settings.xml"
echo         ^<mirror^> >> "%MAVEN_CONF%\settings.xml"
echo             ^<id^>aliyun^</id^> >> "%MAVEN_CONF%\settings.xml"
echo             ^<mirrorOf^>central^</mirrorOf^> >> "%MAVEN_CONF%\settings.xml"
echo             ^<name^>阿里云公共仓库^</name^> >> "%MAVEN_CONF%\settings.xml"
echo             ^<url^>https://maven.aliyun.com/repository/public^</url^> >> "%MAVEN_CONF%\settings.xml"
echo         ^</mirror^> >> "%MAVEN_CONF%\settings.xml"
echo     ^</mirrors^> >> "%MAVEN_CONF%\settings.xml"
echo ^</settings^> >> "%MAVEN_CONF%\settings.xml"

echo ✅ Maven镜像配置完成
echo.

echo ========================================
echo  ✅ Maven 安装完成！
echo ========================================
echo.
echo 安装位置: %MAVEN_HOME_DIR%
echo 配置文件: %MAVEN_CONF%\settings.xml
echo.
echo ==========================================
echo   重要提示
echo ==========================================
echo 1. 请关闭当前命令行窗口
echo 2. 重新打开新的命令行窗口  
echo 3. 运行 mvn -version 验证安装
echo 4. 如果显示版本信息则安装成功
echo 5. 然后运行 start.bat 启动项目
echo ==========================================
echo.

pause
