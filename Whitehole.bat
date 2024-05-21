@echo off
echo Running Whitehole using Java Version (Java 11 or newer REQUIRED):
java --version
java --add-exports=java.desktop/sun.awt=ALL-UNNAMED -Dsun.java2d.uiScale=1.0 -jar Whitehole.jar
if %errorlevel% GEQ 1 (
    echo Whitehole exited with error code %errorlevel%
    pause
)