@echo off & setlocal

set "proxy=%~1"
if "%proxy%" == "" set "proxy=socks5://127.0.0.1:1080"
set all_proxy=%proxy%
set http_proxy=%proxy%
set https_proxy=%proxy%

echo The proxy has been set to "%proxy%"
echo;

curl -L cip.cc
