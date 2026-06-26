@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion
cd /d "%~dp0"

set "OUT=release\spglxt"
set "IMAGE=spglxt-native-builder"
set "CONTAINER=spglxt-native-export"

echo ========================================
echo   Docker: Windows build Linux spglxt
echo   GraalVM native for Baota (no Java)
echo ========================================
echo.

where docker >nul 2>&1
if errorlevel 1 (
  echo [FAIL] Docker not installed or not running
  echo.
  echo Install Docker Desktop: https://www.docker.com/products/docker-desktop/
  echo Or build on Baota: deploy\build-native-linux.sh
  pause
  exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
  echo [FAIL] Docker daemon not running. Start Docker Desktop first.
  pause
  exit /b 1
)

echo [1/4] Docker build Linux native binary (15-30 min first time) ...
docker build -f deploy\Dockerfile.native -t %IMAGE% --target builder .
if errorlevel 1 (
  echo.
  echo [FAIL] Docker build failed
  echo If native compile errors, send the log for reflect-config fixes.
  pause
  exit /b 1
)

echo [2/4] Extract spglxt from container ...
docker rm -f %CONTAINER% >nul 2>&1
docker create --name %CONTAINER% %IMAGE% >nul
if errorlevel 1 (
  echo [FAIL] docker create failed
  pause
  exit /b 1
)

if exist "release" rmdir /S /Q release
mkdir "%OUT%\config"

docker cp %CONTAINER%:/project/target/spglxt "%OUT%\spglxt"
docker rm -f %CONTAINER% >nul 2>&1

if not exist "%OUT%\spglxt" (
  echo [FAIL] spglxt binary not extracted
  pause
  exit /b 1
)

echo [3/4] Copy config ...
if exist "config" xcopy /E /I /Y "config\*" "%OUT%\config\" >nul
if exist ".env" (copy /Y ".env" "%OUT%\config\.env" >nul) else (copy /Y ".env.example" "%OUT%\config\.env" >nul)

echo [4/4] Create spglxt.zip ...
powershell -NoProfile -Command "Compress-Archive -Path 'release\spglxt' -DestinationPath 'spglxt.zip' -Force" 2>nul

echo.
echo ========================================
echo   DONE - same layout as ysname
echo ========================================
echo.
echo   release\spglxt\
echo     spglxt       Linux native (~90MB, NO Java on server)
echo     config\      settings
echo.
echo   spglxt.zip     upload to Baota
echo   Baota Go project: start file spglxt, port 8080
echo.
pause
exit /b 0
