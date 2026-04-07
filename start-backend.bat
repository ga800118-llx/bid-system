@echo off
cd /d "%~dp0backend"
echo Starting BidSystem backend...
"C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe" -jar target\bid-system-1.0.0.jar
pause
