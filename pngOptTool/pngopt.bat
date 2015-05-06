@echo off

if [%1] == [] (
echo 请输入配置文件的路径！
)

set loopCnt=%2
if [%loopCnt%] == [] (
set loopCnt=5
)

for /f "tokens=*" %%i in (%1) do (
REM for /f "eol=: delims=" %%i in ('dir %1 /b /s /a-d ^|findstr /c:".png" ^|findstr /live ".9.png" ^|findstr /live ".opt.png"') do (
echo 压缩图片：%%~fi
for /l %%j in (1,1,%loopCnt%) do (
rem echo execute pngquant
pngquant %%i --ext .opt.png --quality 96 --force
if exist %%~dpni.opt.png (
call :pngout %%~dpni.opt.png
call :advpng %%~dpni.opt.png
call :advdef %%~dpni.opt.png
call :filechoose %%~dpni.opt.png %%i
)
)
)

@echo:
@echo:
echo 压缩完成！ 
GOTO:EOF

:pngout
rem @echo:
rem echo execute pngout
pngout %* /knpTc /q
GOTO:EOF

:advpng
rem @echo:
rem echo execute advpng
advpng -z4 %* -q
GOTO:EOF

:advdef
rem @echo:
rem echo execute advdef
advdef -z4 %* -q
GOTO:EOF

:filechoose
rem echo filechoose
if %~z1 lss %~z2 (
	del %2
	rename %~f1 %~nx2
)else del %1 
GOTO:EOF

