$file = 'D:\code\base\server-ui\med-base-server\src\main\java\med\base\server\controller\OmsOrderController.java'
$content = Get-Content $file -Raw -Encoding UTF8
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($file, $content, $utf8NoBom)
Write-Host 'File re-saved without BOM'

