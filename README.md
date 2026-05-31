# FanVPN for Android

English | [中文](#中文)

A free, open-source VPN client for users in mainland China. FanVPN is a
modified fork of [ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid),
released under the **GNU GPL-3.0**. The underlying proxy engine is
[mihomo](https://github.com/MetaCubeX/mihomo).

## Modifications

Changes by the FanVPN project (since 2025): rebranding and a simplified
user interface, build-time configuration sources, and compatibility fixes
for older Android versions. See the commit history for details.

## Build

1. Initialize submodules (fetches the mihomo core):

```bash
   git submodule update --init --recursive
```

2. Install **JDK 17**, the **Android SDK**, **CMake**, and **Golang**.

3. Create `local.properties` in the project root:

```properties
   sdk.dir=/path/to/Android/Sdk

   # Configuration sources — supply your own; not included in this repo.
   # Profiles and announcements are fetched from "<base>/android.yaml"
   # and "<base>/announce.json", trying each base in order.
   fanvpn.cfg.base.1=https://your-host-1
   fanvpn.cfg.base.2=https://your-host-2
   fanvpn.cfg.base.3=https://your-host-3
```

   With the bases left empty, the app still builds and runs but will not
   auto-import a profile.

4. *(Optional, for signed release builds)* place a `release.keystore` in
   the project root and create `signing.properties`:

```properties
   keystore.password=...
   key.alias=...
   key.password=...
```

5. Build: `./gradlew app:assembleMetaRelease`

## Releases

Official builds and their SHA-256 checksums are published at
[fanvpn.net](https://fanvpn.net). Configuration server URLs and signing
keys are intentionally excluded from this repository.

## License

[GPL-3.0](LICENSE). Third-party notices are in [NOTICE](NOTICE).

---

# 中文

面向中国大陆用户的免费开源翻墙客户端。FanVPN 基于
[ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid)
修改,以 **GNU GPL-3.0** 许可发布,内核为
[mihomo](https://github.com/MetaCubeX/mihomo)。

## 修改说明

FanVPN 项目(自 2025 年起)的改动包括:品牌化与界面精简、构建期注入的配置源、
以及针对旧版本 Android 的兼容性修复。完整改动见提交历史。

## 构建

1. 初始化子模块(拉取 mihomo 内核):

```bash
   git submodule update --init --recursive
```

2. 安装 **JDK 17**、**Android SDK**、**CMake**、**Golang**。

3. 在项目根目录创建 `local.properties`:

```properties
   sdk.dir=/path/to/Android/Sdk

   # 配置源 —— 请自行填写;本仓库不含真实地址。
   # App 从 "<base>/android.yaml" 和 "<base>/announce.json" 拉取配置与公告,按顺序尝试。
   fanvpn.cfg.base.1=https://your-host-1
   fanvpn.cfg.base.2=https://your-host-2
   fanvpn.cfg.base.3=https://your-host-3
```

   留空也能编译运行,只是不会自动导入配置。

4. *(可选,用于签名发布版)* 在根目录放入 `release.keystore` 并创建 `signing.properties`:

```properties
   keystore.password=...
   key.alias=...
   key.password=...
```

5. 构建:`./gradlew app:assembleMetaRelease`

## 发布

官方构建及其 SHA-256 校验值发布于 [fanvpn.net](https://fanvpn.net)。
配置服务器地址与签名密钥**有意不包含**在本仓库中。

## 许可证

[GPL-3.0](LICENSE)。第三方组件声明见 [NOTICE](NOTICE)。
