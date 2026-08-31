# 项目协作约定

- 写代码时控制耦合度，保持可维护性，避免补丁式修改；为关键逻辑添加注释。
- 本地及代理执行编译、验证时，只使用 `local` 渠道，例如 `:app:assembleLocalDebug`、`:app:assembleLocalRelease`。
- 不执行 `google` 渠道编译，也不执行可能同时构建 Google 的聚合任务（如 `assemble`、`assembleDebug`、`assembleRelease`、`build`）。
- Google 正式渠道的 GitHub 构建配置仅做静态检查，不通过实际编译验证。
