# Agent Guide

## Project Overview

IntelliJ IDEA plugin (2024.1+) that copies Java code locations as `package.Class:line` to clipboard.

Single-action plugin: `CopyCodeLocationAction.java` is the entire codebase.

## Build Commands

```bash
gradle runIde          # Launch sandbox IDE with plugin
gradle buildPlugin     # Package to build/distributions/*.zip
```

No test suite exists. Verification is manual via `runIde`.

## Key Technical Details

- **Java 17 toolchain** — enforced in `build.gradle.kts`
- **IntelliJ Platform Gradle Plugin 2.5.0** — not the legacy `intellij` plugin
- **Target**: IntelliJ IDEA Community 2024.1+ (`sinceBuild = "241"`)
- **Action ID**: `CodeLocationCopier.CopyClassLocation` (in `plugin.xml`)
- **Dependencies**: `com.intellij.modules.platform`, `com.intellij.java`

## Code Conventions

- Author tag on every method: `@author 孟祥宇`
- No comments except author tags — keep it that way
- Inner class `LineRange` is private to the action class

## Known Edge Case

Selection end offset: IDEA's `selectionEnd` often lands on next line's start when whole-line selecting. Code does `endOffset--` to avoid off-by-one (`CopyCodeLocationAction.java:78-79`).

## Install for Testing

After `gradle buildPlugin`, install via `Settings | Plugins | Install Plugin from Disk...` selecting the zip from `build/distributions/`.
