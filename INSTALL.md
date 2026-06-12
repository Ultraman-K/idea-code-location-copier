# Code Location Copier 插件安装教程

## 1. 安装插件

1. 打开 IntelliJ IDEA
2. 进入 `Settings | Plugins`
3. 点击右上角齿轮图标 → `Install Plugin from Disk...`
4. 选择 `build/distributions/` 下的 zip 文件
5. 点击 `OK`，重启 IDEA

## 3. 使用方法

### 快捷键

默认快捷键：`Ctrl + Alt + Shift + L`

### 右键菜单

选中代码后右键 → `Copy Class Location`

### 复制结果

单行或无选区：
```
com.example.service.UserService:128
```

多行选区：
```
com.example.service.UserService:128-147
```

## 4. 配置浮动工具栏（选中代码后自动出现的工具栏）

插件已注册为 Intention Action，安装后会自动出现在：
- **浮动工具栏**：选中代码后自动出现的小工具栏
- **Alt+Enter 菜单**：光标处按 Alt+Enter 出现的意图菜单

如果浮动工具栏中没有显示，可以手动添加：

1. 选中任意代码，等浮动工具栏出现
2. 点击工具栏右侧的 `⋮`（三个点）
3. 选择 `Customize Toolbar...`
4. 在弹出的窗口中搜索 `Copy Class Location`
5. 点击 `OK`

## 5. 自定义快捷键

`Settings | Keymap` → 搜索 `Copy Class Location` → 右键添加自定义快捷键

## 6. 常见问题

### 快捷键不生效

1. 重新打包安装新版：`gradle buildPlugin`
2. 重启 IDEA
3. `Settings | Keymap` → 搜索 `Copy Class Location` → 确认快捷键绑定正确
4. 删除旧的 `Copy Code Location` 绑定（如有）

### 复制结果格式不对

如果复制出来是 `com/example/service/UserService.java:128` 这种格式，说明触发的是 IDEA 内置的 `Copy Reference`，不是本插件。请确认快捷键绑定到 `Copy Class Location`。
