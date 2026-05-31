# FanVPN for Android

A free, open-source VPN client for users in mainland China.

FanVPN is a modified fork of
[ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid),
released under the **GNU General Public License v3.0**. The underlying
proxy engine is [mihomo](https://github.com/MetaCubeX/mihomo).

## Modifications

Changes made by the FanVPN project (since 2025) include rebranding and a
simplified user interface, build-time configuration sources, and
compatibility fixes for older Android versions. See the commit history
for the full set of changes.

## Build

1. Initialize submodules (fetches the mihomo core):

```bash
   git submodule update --init --recursive
```

2. Install **JDK 17**, the **Android SDK**, **CMake**, and **Golang**.

3. Create `local.properties` in the project root:

```properties
   sdk.dir=/path/to/Android/Sdk

   # Configuration sources - supply your own; not included in this repo.
   # Profiles and announcements are fetched from "<base>/android.yaml"
   # and "<base>/announce.json", trying each base in order.
   fanvpn.cfg.base.1=https://your-host-1
   fanvpn.cfg.base.2=https://your-host-2
   fanvpn.cfg.base.3=https://your-host-3
```

   With the bases left empty, the app still builds and runs but will not
   auto-import a profile.

4. (Optional, for signed release builds) place a `release.keystore` in
   the project root and create `signing.properties`:

```properties
   keystore.password=...
   key.alias=...
   key.password=...
```

5. Build: `./gradlew app:assembleMetaRelease`

## Releases

Official builds and their SHA-256 checksums are published at
https://fanvpn.net . Configuration server URLs and signing keys are
intentionally excluded from this repository.

## License

[GPL-3.0](LICENSE). Third-party notices are in [NOTICE](NOTICE).