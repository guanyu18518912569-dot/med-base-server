# 解决 Java 文件中文乱码问题

## 问题原因
Java 源文件中的中文字符"全部"等出现乱码，原因可能是：
1. 文件保存时使用了错误的编码（如 GBK）
2. Git 在提交/拉取时改变了编码
3. IDE 编码设置不一致
4. Windows 系统默认编码不是 UTF-8

## 解决方案（已实施）

### 1. Maven 配置（已完成）
在 `pom.xml` 中已配置：
```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
</properties>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <encoding>UTF-8</encoding>
    </configuration>
</plugin>
```

### 2. EditorConfig（新增）
创建了 `.editorconfig` 文件，强制所有编辑器使用 UTF-8 编码

### 3. Git 配置（新增）
创建了 `.gitattributes` 文件，确保 Git 不会改变文件编码

### 4. PowerShell 转换脚本（新增）
创建了 `fix_encoding.ps1` 脚本，可以批量转换文件编码为 UTF-8

## 立即执行的修复步骤

### 步骤 1：运行编码修复脚本
```powershell
cd d:\code\base\server-ui\med-base-server
.\fix_encoding.ps1
```

### 步骤 2：检查 IDE 设置

#### IDEA/IntelliJ
1. File → Settings → Editor → File Encodings
2. 设置以下选项：
   - Global Encoding: UTF-8
   - Project Encoding: UTF-8
   - Default encoding for properties files: UTF-8
   - ✓ Transparent native-to-ascii conversion

#### VS Code
1. 打开设置（Ctrl+,）
2. 搜索 "files.encoding"
3. 设置为 "utf8"

#### Eclipse
1. Window → Preferences → General → Workspace
2. Text file encoding: Other → UTF-8

### 步骤 3：Git 配置
```bash
git config core.autocrlf false
git config core.safecrlf true
```

### 步骤 4：重新构建项目
```bash
mvn clean compile
```

## 验证修复

1. 打开 `WxAppController.java` 文件
2. 检查编辑器右下角显示的编码（应该是 UTF-8）
3. 检查中文字符显示是否正常
4. 如果显示乱码，执行：
   ```
   文件另存为 → 编码选择 UTF-8 → 保存
   ```

## 预防措施

1. **团队约定**：所有成员使用相同的 IDE 编码设置
2. **代码审查**：提交前检查中文字符
3. **CI/CD**：在构建流程中验证文件编码
4. **使用 .editorconfig**：统一编辑器配置

## 紧急修复方法

如果再次出现乱码，快速修复：

### 方法 1：IDE 转换
在 IDEA 中：
1. 右键点击文件
2. File Encoding → UTF-8
3. Convert（转换，而不是 Reload）

### 方法 2：命令行转换（单个文件）
```powershell
$content = Get-Content -Path "WxAppController.java" -Encoding Default
$content | Out-File -FilePath "WxAppController.java" -Encoding UTF8
```

### 方法 3：使用脚本（批量）
运行 `fix_encoding.ps1` 脚本

## 常见编码问题对照表

| 原始文本 | GBK 显示 | UTF-8 正确显示 |
|---------|---------|---------------|
| 全部    | 鍏ㄩ儴  | 全部          |
| 微信    | 寰俊    | 微信          |
| 用户    | 鐢ㄦ埛  | 用户          |

如果看到类似"鍏ㄩ儴"的字符，说明文件是 GBK 编码，需要转换为 UTF-8。
