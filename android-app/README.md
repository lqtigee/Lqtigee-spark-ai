# Lqtigee Android App

Native Android WebView shell for the Lqtigee Java service.

Default server URL:

```text
http://118.24.15.133:20261
```

The app opens `/sessions` and lets the user change the server address from the offline screen.

## Debug APK

The current debug APK is built at:

```text
android-app/release/Lqtigee-debug.apk
```

Build it locally with:

```bash
ANDROID_HOME=/home/lqtiger/Android/Sdk ./build-debug-apk.sh
```

This repository includes a manual debug build script because the current server is `aarch64` and the Android Gradle Plugin downloads an `x86_64` `aapt2` binary. The script uses qemu for `aapt2` and `zipalign`, then signs the APK with a debug key.
