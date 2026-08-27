<img width="1920" height="1150" alt="271e9c8d-97e7-41ee-9fb4-a8d8e4af627d" src="https://github.com/user-attachments/assets/9defdd1b-26e7-4550-9187-ad1b1538a5fe" />
#如图所示
从 Eastern 客户端提取的 OneConfig 风格 ClickGUI 源码与资源（`com.eastern.ui.oneconfig` 包）。

## 包含内容

```
OneConfig-Extract/
├── src/
│   ├── main/
│   │   ├── java/com/eastern/ui/oneconfig/
│   │   │   ├── OneConfigStyleGuiScreen.java    # OneConfig 风格 GUI 主屏
│   │   │   ├── OneConfigIcon.java              # SVG 图标渲染器（tint / 缩放缓存）
│   │   │   └── music/
│   │   │       └── MusicPlayerPage.java        # OneConfig 音乐播放器页面
│   │   └── resources/assets/eastern/
│   │       ├── oc/                             # OneConfig v0 SVG 图标（34 个）
│   │       ├── oc1/                            # OneConfig v1 SVG 图标（72 个）
│   │       └── fonts/                          # 渲染所需字体（7 个）
└── README.md
```

## 界面规格

- **窗口**：1280×800 设计尺寸，自适应 UI 缩放（`getScaleFactor` 逻辑）
- **侧栏**：244px，分类导航 + Credits + Preferences + Edit HUD + Close
- **内容区**：卡片 4 列布局（Mods 页），选项面板 + 全宽控件（Slider/Dropdown/KeyBind/TextInput）
- **动效**：EaseOutExpo 页面切换、EaseInOutQuad 开关、EaseOutQuad 滚动
- **配色**：PolyGlass 深色玻璃拟态（GRAY_800/850/900 系）+ 主题色派生 PRIMARY 色阶

## 使用方式

把 `src` 目录整体并入你的工程，注册 GUI 时：

```java
mc.setScreen(new OneConfigStyleGuiScreen());
```

图标资源路径固定为 `/assets/eastern/...`，字体通过 `SkiaFontManager` 加载：
- `inter-regular / inter-medium / inter-semibold / inter-bold`
- `minecraft-bold`
- `regular / medium`（鸿蒙中文字体，CJK 回退）

不会就用Ai打滑,源码内包含的服务器地址已经废用，音乐部分服务器请自行部署

## 依赖（未随包提供）

以下为 Eastern 客户端核心类，本包仅依赖它们，未包含：

| 依赖 | 说明 |
| --- | --- |
| `com.eastern.module.*` | 模块系统（Module / Category / Value 体系） |
| `com.eastern.module.impl.render.ClickGui` | 主题色来源（`getThemeColor()`） |
| `com.eastern.config.impl.ModuleConfig` | 配置加载/保存 |
| `com.eastern.ui.hud.HUDDesignerScreen` | Edit HUD 跳转目标 |
| `com.eastern.util.IMinecraft` | MC 实例访问 |
| `com.eastern.util.animation.anime.Animation / Easings` | 缓动动画 |
| `com.eastern.util.font.SkiaFontManager` | 字体注册与获取 |
| `com.eastern.util.skia.SkiaRenderer / BlurUtil` | Skia 渲染管线与背景模糊 |
| `com.eastern.music.*` | MusicPlayerPage 依赖（MusicController / MusicLibrary / MusicPlayerEngine） |

## 协议说明

OneConfig 以 **LGPL-3.0** 协议开源（[Polyfrost/OneConfig](https://github.com/Polyfrost/OneConfig)）。
本实现仅按 LGPL 标准参考其界面设计规格，界面为独立实现（Skia 渲染）。

#此OneconfigClickgui仅为参考oneconfigv0进行拙略的仿制并加以改造
#没啥好说的，这个需要自己用ai打滑。
不喜勿喷

##
Yasl-26.8.27
