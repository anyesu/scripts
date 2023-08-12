@echo off & setlocal

set "proxy=%~1"
if "%proxy%" == "" set "proxy=socks5://127.0.0.1:1080"
endlocal & set all_proxy=%proxy%
set http_proxy=%all_proxy%
set https_proxy=%all_proxy%

echo The proxy has been set to "%all_proxy%"
echo;

curl -L cip.cc
