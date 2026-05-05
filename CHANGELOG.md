<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# AxiosCodeGen Changelog

## [Unreleased]

### Added

- 支持忽略指定类型的方法参数、忽略 `@Deprecated` 方法。
- 支持无参数绑定注解的参数。

### Fixed

- 请求方式 value 为数组形式时，默认取第一个。
- 优化方法注释提取。

## [1.0.1] - 2026-03-15

### Added

- 生成的方法 JSDoc 添加 `@returns` 描述。

## [1.0.0] - 2025-09-10

### Added

- 迁移到 [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).
- 支持从 Editor 或 Project 右键菜单生成 axios 请求代码文件、从方法名或类名上通过 Intention 生成对应代码或注释片段。
- 使用 IDEA 内置模板。
- 支持插件动态加载、卸载。
