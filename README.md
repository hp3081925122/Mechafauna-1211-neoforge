# 1.20.1 Forge 模板

这是一个无 Mixin、无自定义内容的最小 Forge 工作区，使用 Forge 47.4.23 与 Java 17。

```powershell
$env:JAVA_HOME = 'C:\Users\30819\.jdks\ms-17.0.18'
.\gradlew.bat build
.\gradlew.bat runClient
```

复制项目和批量改名请在上一级目录运行 `new-mod-from-template.ps1`。
