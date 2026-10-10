# 项目协作约定

- 写代码时控制耦合度，保持可维护性，避免补丁式修改；为关键逻辑添加注释。
- 本地及代理执行编译、验证时，只使用 `local` 渠道，例如 `:app:assembleLocalDebug`、`:app:assembleLocalRelease`。
- 不执行 `google` 渠道编译，也不执行可能同时构建 Google 的聚合任务（如 `assemble`、`assembleDebug`、`assembleRelease`、`build`）。
- 本机及本地代理禁止执行任何 Google 渠道编译、测试或打包任务，包括 `compileGoogle*`、`testGoogle*`、`assembleGoogle*`、`bundleGoogle*`，避免本机关联。
- Google 渠道在本机只允许静态检查源码和 CI 配置；实际编译、测试及打包仅由 GitHub Actions 执行。
