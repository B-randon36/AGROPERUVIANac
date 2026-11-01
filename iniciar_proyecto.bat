@echo off
echo ========================================================
echo Iniciando ERP Agro Peruvian Foods (Backend y Frontend)...
echo ========================================================

start "BOMERP Backend" cmd /k "cd /d %~dp0backend && mvnw.cmd spring-boot:run"
start "BOMERP Frontend" cmd /k "cd /d %~dp0bomerp-frontend && npm.cmd start"

echo Servicios lanzados en ventanas independientes.

