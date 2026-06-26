@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion
cd /d "%~dp0"

set "OUT=release\spglxt"
set "NATIVE_BIN=target\spglxt.exe"

echo ========================================
echo   GraalVM Native Image Build (Windows)
echo   Output: spglxt.exe + config/
echo   No JVM required to run
echo ========================================
echo.

:: ---------- 1. GraalVM ----------
echo [1/5] Check GraalVM ...
if not defined JAVA_HOME (
  echo [WARN] JAVA_HOME not set
)
java -version 2>&1 | findstr /I "GraalVM" >nul
if errorlevel 1 (
  echo [FAIL] GraalVM not detected in java -version
  echo.
  echo Install GraalVM JDK 17/21 to path without spaces, e.g. D:\Dev\graalvm-jdk-21
  echo Set JAVA_HOME and add %%JAVA_HOME%%\bin to PATH
  echo Then run: gu install native-image
  pause
  exit /b 1
)
java -version 2>&1 | findstr /I /C:"version"
native-image --version >nul 2>&1
if errorlevel 1 (
  echo [FAIL] native-image not found. Run: gu install native-image
  pause
  exit /b 1
)
echo [OK] GraalVM + native-image
echo.

:: ---------- 2. MSVC (Visual Studio Build Tools) ----------
echo [2/5] Check MSVC compiler ...
where cl >nul 2>&1
if errorlevel 1 (
  echo [INFO] cl.exe not in PATH, loading vcvars64.bat ...
  call :load_vcvars
  where cl >nul 2>&1
  if errorlevel 1 (
    echo [FAIL] MSVC not found
    echo.
    echo Install Visual Studio Build Tools 2022 with:
    echo   - Desktop development with C++
    echo   - MSVC v143 build tools
    echo   - Windows 10/11 SDK
    echo.
    echo Or run this script from: x64 Native Tools Command Prompt for VS
    pause
    exit /b 1
  )
)
echo [OK] MSVC cl.exe available
echo.

:: ---------- 3. Maven ----------
echo [3/5] Check Maven ...
call :find_maven
if not defined MAVEN_CMD (
  echo [FAIL] Maven not found
  pause
  exit /b 1
)
echo [OK] !MAVEN_CMD!
echo.

:: ---------- 4. Native compile ----------
echo [4/5] mvn -Pnative package (10-20 min, need 4GB+ RAM) ...
echo       Use x64 Native Tools CMD if this fails with vcvars error
echo.
set "MAVEN_OPTS=-Xmx4g"
if exist "settings.xml" (
  call "!MAVEN_CMD!" -B clean package -Pnative -DskipTests -s "%~dp0settings.xml"
) else (
  call "!MAVEN_CMD!" -B clean package -Pnative -DskipTests
)
if errorlevel 1 (
  echo.
  echo [FAIL] Native build failed
  echo.
  echo Common fixes:
  echo   1. Run from "x64 Native Tools Command Prompt for VS 2022"
  echo   2. set MAVEN_OPTS=-Xmx8g
  echo   3. GraalVM path must have NO Chinese characters and NO spaces
  echo   4. See META-INF/native-image/ for reflect config
  pause
  exit /b 1
)

if not exist "%NATIVE_BIN%" (
  if exist "target\spglxt-video-system.exe" set "NATIVE_BIN=target\spglxt-video-system.exe"
)
if not exist "%NATIVE_BIN%" (
  echo [FAIL] Native exe not found in target\
  pause
  exit /b 1
)
echo [OK] Native exe: %NATIVE_BIN%
echo.

:: ---------- 5. Pack release (spglxt + config only) ----------
echo [5/5] Pack release\spglxt\ ...
if exist "release" rmdir /S /Q release
mkdir "%OUT%\config"
copy /Y "%NATIVE_BIN%" "%OUT%\spglxt.exe" >nul
if exist "config" xcopy /E /I /Y "config\*" "%OUT%\config\" >nul
if exist ".env" (copy /Y ".env" "%OUT%\config\.env" >nul) else (copy /Y ".env.example" "%OUT%\config\.env" >nul)

powershell -NoProfile -Command "Compress-Archive -Path 'release\spglxt' -DestinationPath 'spglxt-win.zip' -Force" 2>nul

echo.
echo ========================================
echo   BUILD OK
echo ========================================
echo.
echo   release\spglxt\
echo     spglxt.exe    native exe, no JVM needed
echo     config\       settings
echo.
echo   spglxt-win.zip  Windows release package
echo.
echo   Run: release\spglxt\spglxt.exe
echo.
echo   NOTE: This is Windows .exe only.
echo   For Baota Linux use deploy\build-native-linux.sh on server.
echo.
pause
exit /b 0

:: ---------- Find vcvars64.bat ----------
:load_vcvars
for %%P in (
  "C:\Program Files\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
  "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
  "C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars64.bat"
  "C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat"
  "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
  "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat"
) do (
  if exist %%P (
    call %%P >nul 2>&1
    goto :eof
  )
)
goto :eof

:find_maven
set "MAVEN_CMD="
where mvn >nul 2>&1 && set "MAVEN_CMD=mvn" && goto :eof
if exist "C:\apache-maven-3.9.6\bin\mvn.cmd" set "MAVEN_CMD=C:\apache-maven-3.9.6\bin\mvn.cmd"
if exist "%~dp0tools\maven\bin\mvn.cmd" set "MAVEN_CMD=%~dp0tools\maven\bin\mvn.cmd"
goto :eof
