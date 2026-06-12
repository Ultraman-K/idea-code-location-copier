# Code Location Copier

## 痛点

在 IDEA 之外使用 AI Agent 工具（如 Claude Code、Cursor、Copilot Chat 等）时，一个高频痛点：**你在 IDE 里选中了一段代码想让 Agent 分析，但 Agent 无法感知你在 IDEA 中的光标位置和选区**。你只能手动描述"帮我看看 UserService.java 第 128 行附近的方法"，或者复制代码片段过去，但 Agent 不知道这段代码来自哪个文件、哪一行。

这个插件就是把当前光标/选区的位置信息（`类全限定名:起止行号`）一键复制到剪贴板，粘贴给外部 Agent 即可让 Agent 精确定位代码位置。

IntelliJ IDEA 2024+ 插件。选中 Java 代码后触发 Action，复制当前位置：

```text
com.xxx.service.UserService:128-147
```

单行或无选区时：

```text
com.xxx.service.UserService:128
```

## 功能

- 支持 Java 文件。
- 包名和类名从 PSI 获取，不解析源码字符串。
- 行号从 1 开始。
- 有选区时复制选区起止行。
- 无选区时复制当前光标行。
- 自动复制到系统剪贴板。
- 注册为 IDEA Action，可绑定快捷键。

## 关键文件

- `src/main/java/com/example/ideacodelocation/CopyCodeLocationAction.java`：Action 实现。
- `src/main/resources/META-INF/plugin.xml`：插件声明、依赖、Action 注册、默认快捷键。
- `build.gradle.kts`：IntelliJ Platform Gradle Plugin 构建配置。

## plugin.xml 配置

```xml
<depends>com.intellij.modules.platform</depends>
<depends>com.intellij.java</depends>

<actions>
    <action id="CodeLocationCopier.CopyClassLocation"
            class="com.example.ideacodelocation.CopyCodeLocationAction"
            text="Copy Class Location"
            description="Copy qualified class name and selected line range">
        <add-to-group group-id="EditorPopupMenu" anchor="last"/>
        <keyboard-shortcut keymap="$default" first-keystroke="ctrl alt shift L"/>
    </action>
</actions>
```

默认快捷键是 `Ctrl + Alt + Shift + L`。也可以在 IDEA：

```text
Settings | Keymap | 搜索 Copy Class Location
```

重新绑定。

如果复制出来是这种格式：

```text
com/example/service/UserService.java:128
```

说明触发的是 IDEA 内置 `Copy Reference`，不是这个插件 Action。请确认快捷键绑定到 `Copy Class Location`。

## 本地运行

在本目录执行：

```bash
gradle runIde
```

打包插件：

```bash
gradle buildPlugin
```

打包产物在：

```text
build/distributions/
```

仓库已包含打包好的插件包：

```text
build/distributions/idea-code-location-copier-1.0.3.zip
```

不需要本地构建时，可以直接下载该 zip 安装。

安装方式：

```text
Settings | Plugins | Install Plugin from Disk...
```

选择上面的 zip 文件，或选择从 GitHub 下载的 `idea-code-location-copier-1.0.3.zip`。

## 快捷键不生效排查

1. 重新打包并安装新版 zip。IDEA 对已安装插件和 Keymap 有缓存，同版本覆盖时可能仍使用旧 Action。

```bash
gradle buildPlugin
```

2. 安装新版后重启 IDEA。
3. 打开：

```text
Settings | Keymap
```

4. 搜索：

```text
Copy Class Location
```

5. 只给这个 Action 绑定快捷键，删除旧的 `Copy Code Location` 绑定。
6. 用：

```text
Help | Find Action
```

搜索并执行 `Copy Class Location`。如果这里能复制，说明插件正常，问题只在快捷键冲突。

如果复制结果是：

```text
com/example/service/UserService.java:128
```

说明触发的是 IDEA 内置 `Copy Reference`，不是本插件。

## 行号边界

整行选择时，IDEA 的 selectionEnd 可能落在下一行行首。实现里对非空选区使用 `endOffset - 1`，避免输出多一行。
