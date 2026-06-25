@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "PROJECT_DIR=%~dp0"
set "BUILD_DIR=%TEMP%\VeroPrinterStandaloneJavaBuild"
set "PKG_DIR="
set "CLASSES_DIR=%BUILD_DIR%\classes"
set "SOURCES_LIST=%BUILD_DIR%\sources.txt"
set "OUT_DIR=%PROJECT_DIR%..\..\assets\libs\vero\"
set "JAR_FILE=%OUT_DIR%veroprinter-custom.jar"
set "BCNEWLAND_AAR=%OUT_DIR%bcnewland-release-279-20241009.aar"
set "POSMPAPI_AAR=%OUT_DIR%posmpapi-1.01.21-partnersRelease.aar"
set "POSITIVO_AAR=%OUT_DIR%positivo-printer-1.00.00.aar"

echo [INFO] Projeto Java: %PROJECT_DIR%

if "%JAVA_HOME%"=="" call :find_java_home
if "%JAVA_HOME%"=="" (
  echo [ERRO] JAVA_HOME nao definido e nao foi localizado automaticamente.
  echo [ERRO] Defina manualmente, por exemplo:
  echo         set JAVA_HOME=C:\Program Files\Java\jdk-17
  exit /b 1
)
echo [INFO] JAVA_HOME=%JAVA_HOME%

if "%ANDROID_HOME%"=="" if not "%ANDROID_SDK_ROOT%"=="" set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
if "%ANDROID_HOME%"=="" call :find_android_home
if "%ANDROID_HOME%"=="" (
  echo [ERRO] ANDROID_HOME ou ANDROID_SDK_ROOT nao definido e nao foi localizado automaticamente.
  echo [ERRO] Defina manualmente a pasta raiz do SDK Android.
  exit /b 1
)
echo [INFO] ANDROID_HOME=%ANDROID_HOME%

set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAR=%JAVA_HOME%\bin\jar.exe"

if not exist "%JAVAC%" (
  echo [ERRO] javac.exe nao encontrado em "%JAVAC%".
  exit /b 1
)
if not exist "%JAR%" (
  echo [ERRO] jar.exe nao encontrado em "%JAR%".
  exit /b 1
)

set "ANDROID_JAR="
if not "%ANDROID_PLATFORM%"=="" (
  if exist "%ANDROID_HOME%\platforms\%ANDROID_PLATFORM%\android.jar" (
    set "ANDROID_JAR=%ANDROID_HOME%\platforms\%ANDROID_PLATFORM%\android.jar"
  )
)
if "%ANDROID_JAR%"=="" call :find_android_jar
if "%ANDROID_JAR%"=="" (
  echo [ERRO] android.jar nao encontrado em "%ANDROID_HOME%\platforms".
  exit /b 1
)
echo [INFO] ANDROID_JAR=%ANDROID_JAR%

if not exist "%BCNEWLAND_AAR%" (
  echo [ERRO] AAR nao encontrado: %BCNEWLAND_AAR%
  exit /b 1
)
if not exist "%POSMPAPI_AAR%" (
  echo [ERRO] AAR nao encontrado: %POSMPAPI_AAR%
  exit /b 1
)
if not exist "%POSITIVO_AAR%" (
  echo [ERRO] AAR nao encontrado: %POSITIVO_AAR%
  exit /b 1
)
call :find_java_sources
if not exist "%PKG_DIR%" (
  echo [ERRO] Pasta Java nao encontrada dentro de: %PROJECT_DIR%
  echo [ERRO] Era esperado localizar VeroPrinterRouter.java e PrintBitmapRenderer.java.
  exit /b 1
)
echo [INFO] PKG_DIR=%PKG_DIR%

echo [INFO] Limpando build anterior...
if exist "%BUILD_DIR%" rd /s /q "%BUILD_DIR%"

echo [INFO] Criando diretorios temporarios...
mkdir "%BUILD_DIR%" >nul 2>&1
mkdir "%CLASSES_DIR%" >nul 2>&1
if errorlevel 1 (
  echo [ERRO] Nao foi possivel criar a estrutura temporaria.
  exit /b 1
)

> "%SOURCES_LIST%" (
  for %%I in ("%PKG_DIR%*.java") do @echo %%~fI
)
if not exist "%SOURCES_LIST%" (
  echo [ERRO] Nao foi possivel gerar a lista de fontes Java.
  exit /b 1
)

echo [INFO] Extraindo classes.jar do AAR da Newland...
pushd "%BUILD_DIR%"
"%JAR%" xf "%BCNEWLAND_AAR%" classes.jar
set "JAR_XF_RC=%ERRORLEVEL%"
popd
if not "%JAR_XF_RC%"=="0" (
  echo [ERRO] Falha ao extrair classes.jar do AAR da Newland.
  exit /b 1
)
if not exist "%BUILD_DIR%\classes.jar" (
  echo [ERRO] classes.jar da Newland nao encontrado apos extracao.
  exit /b 1
)
move /y "%BUILD_DIR%\classes.jar" "%BUILD_DIR%\bcnewland-classes.jar" >nul

echo [INFO] Extraindo classes.jar do AAR do posmpapi...
pushd "%BUILD_DIR%"
"%JAR%" xf "%POSMPAPI_AAR%" classes.jar
set "JAR_XF_RC=%ERRORLEVEL%"
popd
if not "%JAR_XF_RC%"=="0" (
  echo [ERRO] Falha ao extrair classes.jar do AAR do posmpapi.
  exit /b 1
)
if not exist "%BUILD_DIR%\classes.jar" (
  echo [ERRO] classes.jar do posmpapi nao encontrado apos extracao.
  exit /b 1
)
move /y "%BUILD_DIR%\classes.jar" "%BUILD_DIR%\posmpapi-classes.jar" >nul

echo [INFO] Extraindo classes.jar do AAR da Positivo...
pushd "%BUILD_DIR%"
"%JAR%" xf "%POSITIVO_AAR%" classes.jar
set "JAR_XF_RC=%ERRORLEVEL%"
popd
if not "%JAR_XF_RC%"=="0" (
  echo [ERRO] Falha ao extrair classes.jar do AAR da Positivo.
  exit /b 1
)
if not exist "%BUILD_DIR%\classes.jar" (
  echo [ERRO] classes.jar da Positivo nao encontrado apos extracao.
  exit /b 1
)
move /y "%BUILD_DIR%\classes.jar" "%BUILD_DIR%\positivo-classes.jar" >nul

echo [INFO] Compilando wrapper Java da Vero...
"%JAVAC%" -encoding UTF-8 -source 8 -target 8 -cp "%ANDROID_JAR%;%BUILD_DIR%\bcnewland-classes.jar;%BUILD_DIR%\posmpapi-classes.jar;%BUILD_DIR%\positivo-classes.jar" -d "%CLASSES_DIR%" ^
  @"%SOURCES_LIST%"
if errorlevel 1 (
  echo [ERRO] Falha na compilacao Java.
  exit /b 1
)

echo [INFO] Gerando JAR customizado...
if exist "%JAR_FILE%" del /f /q "%JAR_FILE%"
pushd "%CLASSES_DIR%"
"%JAR%" cf "%JAR_FILE%" .
set "JAR_RC=%ERRORLEVEL%"
popd
if not "%JAR_RC%"=="0" (
  echo [ERRO] Falha ao gerar o JAR customizado.
  exit /b 1
)
if not exist "%JAR_FILE%" (
  echo [ERRO] O JAR nao foi gerado: %JAR_FILE%
  exit /b 1
)

echo [OK] JAR gerado com sucesso:
echo      %JAR_FILE%
echo.
echo [INFO] Proximo passo:
echo        1. Clean no projeto Delphi
echo        2. Build para Android
echo        3. Instalar o APK no terminal
exit /b 0

:find_java_home
for /f "delims=" %%I in ('where javac 2^>nul') do (
  set "JAVAC_FOUND=%%I"
  goto :java_found
)
goto :eof

:java_found
for %%I in ("!JAVAC_FOUND!\..\..") do set "JAVA_HOME=%%~fI"
goto :eof

:find_android_home
if exist "%LOCALAPPDATA%\Android\Sdk\platforms" (
  set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
  goto :eof
)
for %%V in (37.0 23.0 22.0 21.0 20.0) do (
  for /d %%I in ("%PUBLIC%\Documents\Embarcadero\Studio\%%V\CatalogRepository\AndroidSDK-*") do (
    if exist "%%~fI\platforms" (
      set "ANDROID_HOME=%%~fI"
      goto :eof
    )
  )
)
goto :eof

:find_android_jar
for /f "delims=" %%I in ('dir /b /ad /o-n "%ANDROID_HOME%\platforms" 2^>nul') do (
  if exist "%ANDROID_HOME%\platforms\%%I\android.jar" (
    set "ANDROID_JAR=%ANDROID_HOME%\platforms\%%I\android.jar"
    goto :eof
  )
)
goto :eof

:find_java_sources
for /r "%PROJECT_DIR%com" %%I in (VeroPrinterRouter.java) do (
  if exist "%%~dpIPrintBitmapRenderer.java" (
    set "PKG_DIR=%%~dpI"
    goto :eof
  )
)
goto :eof
