# VirtualCam（LSPosed 模块）

用于自研 App 端到端自动化测试：在 `Magisk + LSPosed` 环境中 Hook 相机输出，将拍照 JPEG 与录制视频最终文件替换为自定义素材。

## 支持范围（v1）

- 拍照：`Camera1`（`android.hardware.Camera.takePicture`）
- 录视频：`MediaRecorder`（`setOutputFile` + `stop` 后覆盖输出文件）

`Camera2` 及基于 `MediaCodec` 的自定义编码链路不在 v1 覆盖范围内。

## 使用方式（v1）

1. 构建并安装模块 APK（Android Studio 导入工程后 `assembleDebug`）。
2. 在模块设置页写入配置与素材（默认目录：`/sdcard/VirtualCam/`）。
3. 在 LSPosed 中勾选需要生效的目标 App（建议仅勾选你的自研 App 包名），重启目标 App。
4. 在目标 App 内拍照/录制，输出文件应被替换为自定义素材。

## 配置文件

默认路径：`/sdcard/VirtualCam/config.json`

示例：

```json
{
  "enabled": true,
  "enablePhoto": true,
  "enableVideo": true,
  "mode": "allowlist",
  "allowlist": ["com.example.yourapp"],
  "photoPath": "/sdcard/VirtualCam/photo.jpg",
  "videoPath": "/sdcard/VirtualCam/video.mp4"
}
```

## 注意事项

- 建议使用白名单模式，仅对自研 App 生效，避免影响其它应用。
- 目标 App 若无法读取 `/sdcard/VirtualCam/`（未授权存储权限等），替换可能失败。

