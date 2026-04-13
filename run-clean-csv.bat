@echo off
chcp 65001 >nul
echo ========================================
echo CSV包清洗工具
echo ========================================
echo.
echo 功能:
echo 1. 替换包路径 org.apache.commons.lang3 -^> lang3
echo 2. 删除注释和空行
echo 3. 检查缺失的导包
echo.
echo ========================================
echo.

echo [1/2] 编译清洗工具...
javac -encoding UTF-8 SimpleCleanCSV.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ✗ 编译失败！
    echo.
    pause
    exit /b 1
)

echo ✓ 编译成功
echo.
echo [2/2] 执行清洗...
echo.

java -Dfile.encoding=UTF-8 SimpleCleanCSV

echo.
echo ========================================
echo.
echo 清洗完成！
echo.
echo 下一步:
echo 1. 检查上面的"可能缺失的导包"提示
echo 2. 根据提示添加依赖或修改代码
echo 3. 运行 mvn compile 测试编译
echo.
pause
