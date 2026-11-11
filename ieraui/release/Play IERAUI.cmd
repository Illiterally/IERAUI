@echo off
setlocal
pushd "%~dp0"
set "IERAUI_FORGE_DIR=%~dp0"
if not exist "forge-gui-desktop-2.0.16-jar-with-dependencies.jar" goto wrongforge
if not exist "ieraui-addon-0.1.21.jar" goto missingaddon
if not exist "res\languages\en-US.properties" goto wrongforge
powershell.exe -NoProfile -NonInteractive -Command "try { $s=[IO.File]::OpenRead([IO.Path]::Combine($env:IERAUI_FORGE_DIR,'forge-gui-desktop-2.0.16-jar-with-dependencies.jar')); $h=[Security.Cryptography.SHA256]::Create(); try { $actual=[BitConverter]::ToString($h.ComputeHash($s)).Replace('-','') } finally { $s.Dispose(); $h.Dispose() }; if ($actual -ne '16B954C64EEB8444E99B109D91EC9C57CA394A0A07041B01B83B268E92D81A75') { exit 1 }; exit 0 } catch { exit 2 }" >nul 2>&1
if errorlevel 2 goto verifyfailed
if errorlevel 1 goto wrongforge
rem Initialize only missing files; updating the addon must never reset a profile.
if not exist "forge.profile.properties" copy /b "IERAUI\defaults\forge.profile.properties" "forge.profile.properties" >nul
rem A pre-existing custom profile may point elsewhere. Leave its preferences alone.
if not exist "data\preferences" mkdir "data\preferences"
if not exist "data\preferences\forge.preferences" copy /b "IERAUI\defaults\forge.preferences" "data\preferences\forge.preferences" >nul
set "IERAUI_JAVA=javaw.exe"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "IERAUI_JAVA=%JAVA_HOME%\bin\javaw.exe"
if exist "runtime\bin\javaw.exe" set "IERAUI_JAVA=%~dp0runtime\bin\javaw.exe"
if "%IERAUI_JAVA%"=="javaw.exe" where javaw.exe >nul 2>&1
if "%IERAUI_JAVA%"=="javaw.exe" if errorlevel 1 goto missingjava
start "IERAUI" "%IERAUI_JAVA%" -Xmx4096m -Dforge.enhancedRulesUi=true -Dsentry.enabled=false -Dio.netty.tryReflectionSetAccessible=true -Dfile.encoding=UTF-8 --add-opens java.desktop/java.beans=ALL-UNNAMED --add-opens java.desktop/javax.swing.border=ALL-UNNAMED --add-opens java.desktop/javax.swing.event=ALL-UNNAMED --add-opens java.desktop/sun.swing=ALL-UNNAMED --add-opens java.desktop/java.awt.image=ALL-UNNAMED --add-opens java.desktop/java.awt.color=ALL-UNNAMED --add-opens java.desktop/sun.awt.image=ALL-UNNAMED --add-opens java.desktop/javax.swing=ALL-UNNAMED --add-opens java.desktop/java.awt=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED -cp "ieraui-addon-0.1.21.jar;forge-gui-desktop-2.0.16-jar-with-dependencies.jar" forge.view.Main
popd
exit /b 0
:wrongforge
echo IERAUI 0.1.21 needs the official Forge Desktop 2.0.16 files.
echo Extract Forge 2.0.16 into a fresh folder, then add IERAUI there.
echo See IERAUI\SETUP.md for the download link and steps.
goto failed
:missingaddon
echo The IERAUI addon JAR is missing. Extract the entire addon ZIP here.
goto failed
:missingjava
echo Java 17 or later is required. See IERAUI\SETUP.md for help installing Java.
goto failed
:verifyfailed
echo IERAUI could not verify the Forge file. Check that the folder is readable
echo and Windows PowerShell is available, then try again.
:failed
pause
popd
exit /b 1
