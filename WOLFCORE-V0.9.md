# HexStrike Mobile v0.9 — WolfCore Turbo (experimental)

This is a derivative of the uploaded v0.8 project. The original on-screen layout and existing buttons are preserved, except the version label now reads v0.9. The APK keeps the same package ID, so installing with `adb install -r` upgrades the existing installation. Back up any important app data first.

## Actual improvements

- **Streaming chat:** llama.cpp JNI delivers partial responses into the existing console approximately every 48 bytes.
- **Performance modes:** long-press **DEVICE CHECK** to cycle BATTERY (2 CPU threads), BALANCED (4), TURBO (6). A normal tap still performs DEVICE CHECK. These are experiments, not guaranteed performance improvements.
- **Smaller per-query context:** 2048 instead of 4096 tokens, to lower KV cache RAM consumption. Very long queries are rejected.
- **Performance metric:** wall-clock generation time in milliseconds displayed below each answer, including prompt processing; this is *not* tokens/sec.
- **Still entirely offline:** v0.8 CPU llama.cpp backend retained; **Vulkan inference is not implemented** in this edition. Android reporting Vulkan compute availability does not establish working llama.cpp Vulkan acceleration.

## Build on ScarfaceOS

The uploaded ZIP contains neither the ~1.12 GB GGUF nor llama.cpp source. Follow the existing README build instructions from this directory (including `scripts/prepare-model.sh` and cloning a compatible llama.cpp revision). Newer upstream llama.cpp revisions may change API and need additional porting.

```bash
cd ~/Downloads/HexStrike-Mobile-v0.9-WolfCore-Turbo
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/Android/Sdk"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
bash scripts/prepare-model.sh
git clone --depth 1 https://github.com/ggml-org/llama.cpp.git app/src/main/cpp/llama.cpp
~/Downloads/gradle-8.11.1/bin/gradle --no-daemon :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Important:** build and runtime performance have not been verified here because the 1.12 GB model, llama.cpp checkout, Android SDK and NDK are not provided in this environment. On-device benchmark against v0.8 is required. Original authorship/attribution and licence files preserved.
