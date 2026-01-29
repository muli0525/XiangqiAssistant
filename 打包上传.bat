@echo off
echo ========================================
echo 准备上传到 GitHub
echo ========================================
echo.

echo 由于网络限制，无法直接推送
echo 请按照以下步骤手动上传：
echo.
echo 1. 打开浏览器，访问：
echo    https://github.com/muli0525/XiangqiAssistant
echo.
echo 2. 点击 "Add file" - "Upload files"
echo.
echo 3. 将当前文件夹的所有文件拖入网页
echo    （不要拖整个文件夹，要拖里面的文件）
echo.
echo 4. 点击 "Commit changes"
echo.
echo 5. 等待 5-10 分钟自动编译
echo.
echo 6. 在 "Actions" 标签页下载 APK
echo.
echo ========================================
echo 按任意键打开 GitHub 网页...
pause >nul

start https://github.com/muli0525/XiangqiAssistant

echo.
echo 网页已打开，请按照上述步骤操作
echo.
pause
