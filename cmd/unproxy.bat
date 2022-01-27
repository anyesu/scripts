@echo off

set all_proxy=
set http_proxy=
set https_proxy=

echo The proxy has been cleared
echo;

curl -L cip.cc
