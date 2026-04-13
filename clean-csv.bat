@echo off
echo ========================================
echo 使用 Cleaning 类清洗 csv 包
echo ========================================
echo.
echo 功能:
echo 1. 替换包路径 org.apache.commons.csv -^> csv
echo 2. 删除注释和空行
echo 3. 规范化代码格式
echo.
echo ========================================
echo.

REM 设置编码为UTF-8
chcp 65001 >nul

REM 检查是否已编译
if not exist "target\classes\zju\cst\aces\runner\Cleaning.class" (
    echo [1/3] 编译 Cleaning.java ...
    javac -encoding UTF-8 -cp "lib/*" -d target/classes src/main/java/zju/cst/aces/runner/Cleaning.java 2>nul
    
    if %ERRORLEVEL% NEQ 0 (
        echo ✗ 编译失败
        echo.
        echo 建议使用 run-clean-csv.bat 代替
        echo.
        pause
        exit /b 1
    )
    echo ✓ 编译成功
) else (
    echo [1/3] 使用已编译的 Cleaning 类
)

echo.
echo [2/3] 执行清洗 csv 包 ...
echo.

REM 运行清洗方法
java -Dfile.encoding=UTF-8 -cp "lib/*;target/classes" zju.cst.aces.runner.Cleaning cleanOnly src/main/java/csv

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ✗ 清洗失败
    echo.
    pause
    exit /b 1
)

echo.
echo [3/3] 替换包路径 org.apache.commons.csv -^> csv ...
echo.

REM 使用PowerShell批量替换包路径
powershell -Command "$files = Get-ChildItem 'src/main/java/csv/*.java'; foreach ($file in $files) { $content = Get-Content $file.FullName -Raw -Encoding UTF8; $content = $content -replace 'package org\.apache\.commons\.csv', 'package csv'; $content = $content -replace 'import org\.apache\.commons\.csv\.', 'import csv.'; $content = $content -replace 'import static org\.apache\.commons\.csv\.', 'import static csv.'; $content = $content -replace 'org\.apache\.commons\.csv\.', 'csv.'; Set-Content $file.FullName $content -Encoding UTF8 -NoNewline; Write-Host \"  ✓ $($file.Name)\"; }"

echo.
echo ========================================
echo 完成！
echo ========================================
echo.
echo 下一步:
echo 1. 检查 csv 包中的文件
echo 2. 运行 mvn compile 测试编译
echo 3. 根据编译错误添加缺失的依赖
echo.
pause
