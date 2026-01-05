# Java 文件编码批量转换为 UTF-8
# 用于修复中文乱码问题

Write-Host "开始转换 Java 文件编码为 UTF-8..." -ForegroundColor Green

# 查找所有 Java 文件
$javaFiles = Get-ChildItem -Path "src" -Filter "*.java" -Recurse -File

$count = 0
$errorCount = 0

foreach ($file in $javaFiles) {
    try {
        Write-Host "处理: $($file.FullName)" -ForegroundColor Yellow
        
        # 尝试读取文件内容（尝试多种编码）
        $content = $null
        
        # 首先尝试 UTF-8
        try {
            $content = Get-Content -Path $file.FullName -Encoding UTF8 -Raw
        } catch {
            Write-Host "  UTF-8 读取失败，尝试 GBK..." -ForegroundColor Gray
        }
        
        # 如果 UTF-8 失败，尝试 GBK (Windows 默认中文编码)
        if ($null -eq $content -or $content -match '\u00e9\u0083\u00a8') {
            try {
                # 使用 .NET 读取 GBK 编码
                $encoding = [System.Text.Encoding]::GetEncoding("GBK")
                $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
                $content = $encoding.GetString($bytes)
                Write-Host "  使用 GBK 编码读取" -ForegroundColor Cyan
            } catch {
                Write-Host "  GBK 读取失败，尝试默认编码..." -ForegroundColor Gray
                $content = Get-Content -Path $file.FullName -Raw
            }
        }
        
        # 保存为 UTF-8（无 BOM）
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText($file.FullName, $content, $utf8NoBom)
        
        Write-Host "  ✓ 转换成功" -ForegroundColor Green
        $count++
    }
    catch {
        Write-Host "  ✗ 转换失败: $($_.Exception.Message)" -ForegroundColor Red
        $errorCount++
    }
}

Write-Host "`n转换完成！" -ForegroundColor Green
Write-Host "成功: $count 个文件" -ForegroundColor Green
if ($errorCount -gt 0) {
    Write-Host "失败: $errorCount 个文件" -ForegroundColor Red
}

Write-Host "`n建议执行以下命令重新编译项目：" -ForegroundColor Cyan
Write-Host "mvn clean compile" -ForegroundColor Yellow

Read-Host "`n按 Enter 键退出"
