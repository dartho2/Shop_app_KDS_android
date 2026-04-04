@echo off
echo Czyszczenie projektu...
call gradlew.bat clean
echo.
echo Usuwanie cache KSP...
rmdir /s /q app\build\generated\ksp 2>nul
rmdir /s /q app\.ksp 2>nul
rmdir /s /q .gradle\ksp 2>nul
echo.
echo Build debug...
call gradlew.bat assembleDebug
echo.
echo Gotowe!
pause

