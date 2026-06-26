@echo off
chcp 65001 >nul 2>&1
echo.
echo ========================================
echo   GraalVM 原生打包（与 ysname 相同）
echo ========================================
echo.
echo   1 = Windows 本地测试 (.exe，不能上传宝塔)
echo   2 = Docker 编译 Linux spglxt (宝塔用，无需 Java) 推荐
echo   3 = GitHub Actions / 宝塔 SSH 见 deploy/宝塔部署说明.md
echo.
choice /C 12 /M "请选择 1 或 2"
if errorlevel 2 goto :docker
if errorlevel 1 goto :native

:native
call "%~dp0build-native.bat"
exit /b %ERRORLEVEL%

:docker
call "%~dp0build-native-docker.bat"
exit /b %ERRORLEVEL%
