# 解决汉字编码问题 - Git 配置

## 问题原因
Git 在不同系统间（Windows/Mac/Linux）传输文件时，可能会改变文件编码，导致汉字乱码。

## 解决方案

### 1. 在项目根目录创建 .gitattributes 文件

```gitattributes
# 所有文本文件使用 UTF-8 编码，LF 换行符
* text=auto eol=lf encoding=utf-8

# Java 文件强制使用 UTF-8 编码
*.java text eol=lf encoding=utf-8 working-tree-encoding=utf-8

# 其他常见文本文件
*.xml text eol=lf encoding=utf-8
*.properties text eol=lf encoding=utf-8
*.yml text eol=lf encoding=utf-8
*.yaml text eol=lf encoding=utf-8
*.json text eol=lf encoding=utf-8
*.js text eol=lf encoding=utf-8
*.ts text eol=lf encoding=utf-8
*.tsx text eol=lf encoding=utf-8
*.css text eol=lf encoding=utf-8
*.md text eol=lf encoding=utf-8

# 二进制文件
*.jpg binary
*.png binary
*.gif binary
*.pdf binary
*.jar binary
*.war binary
```

### 2. 配置 Git 全局编码

在命令行执行：

```bash
# 设置 Git 使用 UTF-8 编码
git config --global i18n.commitEncoding utf-8
git config --global i18n.logOutputEncoding utf-8

# Windows 系统额外配置
git config --global core.quotepath false
```

### 3. IntelliJ IDEA 配置（重要）

1. **设置文件编码**：
   - `File` → `Settings` → `Editor` → `File Encodings`
   - `Global Encoding`: UTF-8
   - `Project Encoding`: UTF-8
   - `Default encoding for properties files`: UTF-8
   - ✅ 勾选 `Transparent native-to-ascii conversion`
   - ✅ 勾选 `Create UTF-8 files: with NO BOM`

2. **设置换行符**：
   - `File` → `Settings` → `Editor` → `Code Style`
   - `Line separator`: Unix and macOS (\n)

### 4. 重新提交已有文件（如果需要）

如果之前的文件已经乱码，需要重新规范化：

```bash
# 1. 提交当前更改
git add .
git commit -m "Add .gitattributes"

# 2. 重新规范化所有文件
git add --renormalize .
git commit -m "Normalize line endings and encoding"

# 3. 推送到远程
git push
```

### 5. 团队成员需要同步

团队其他成员需要执行：

```bash
# 拉取最新代码
git pull

# 重新检出所有文件以应用新的编码设置
git rm --cached -r .
git reset --hard
```

## 最佳实践

### ✅ 推荐：使用枚举管理中文字符串

已创建的枚举类：
- `OrderStatus.java` - 订单状态
- `UserStatus.java` - 用户状态  
- `MemberLevel.java` - 会员等级

**优势**：
1. 汉字使用 Unicode 转义（\uXXXX），永不乱码
2. 集中管理，便于维护
3. 类型安全，避免硬编码
4. IDE 自动提示

**示例**：
```java
// ❌ 不推荐：直接写汉字
return "待付款";

// ✅ 推荐：使用枚举
return OrderStatus.getDescByCode(status);
```

### ⚠️ 如果必须在代码中写汉字

使用 Unicode 转义：

```java
// "待付款" 的 Unicode 转义
String status = "\u5f85\u4ed8\u6b3e";
```

**转换工具**：
- 在线工具：https://tool.chinaz.com/tools/unicode.aspx
- IDEA 插件：Native2Ascii

## 验证编码是否正确

```bash
# 查看文件编码
file -i yourfile.java

# 应该显示：
# yourfile.java: text/x-java; charset=utf-8
```

## 总结

本次修改已完成：
1. ✅ 创建了 3 个枚举类（OrderStatus、UserStatus、MemberLevel）
2. ✅ 替换了所有 Controller 中的硬编码中文
3. ✅ 汉字使用 Unicode 转义，永不乱码
4. ✅ 提供了完整的 Git 和 IDEA 配置方案

**下次添加中文字符串时**：
- 优先使用枚举
- 或者使用 Unicode 转义
- 确保 IDEA 和 Git 配置正确
