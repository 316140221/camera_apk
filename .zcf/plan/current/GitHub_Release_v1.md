## 上下文

- 目标：推送到 GitHub 新建仓库（public），并用 GitHub Actions 自动构建 Release APK。
- 仓库：git@github.com:316140221/camera_apk.git
- 触发：tag 以 `v*` 开头时发布 Release。

## 交付物（v1）

- GitHub Actions 工作流：构建 Release APK 并发布到 GitHub Release。
- CI 侧签名：从 GitHub Secrets 解码 keystore 并生成 `keystore.properties`。

## 验收标准（v1）

- 推送 `v*` tag 后自动生成 Release，附件含 `app-release.apk`。

## 实施步骤（原子操作）

1. 增加 `.github/workflows/release.yml` 工作流。
2. 配置远程仓库并提交变更。
3. 打 tag 并推送触发 Release。
4. 提供 Secrets 填写指引（keystore base64、密码等）。

