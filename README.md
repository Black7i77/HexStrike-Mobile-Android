# HexStrike Mobile v0.8 — Bundled Offline AI (Experimental)

Unofficial standalone Android prototype. Includes Android-native local security checks, a compact bear-themed UI, and a llama.cpp JNI bridge for Qwen2.5 1.5B Instruct.

**This source ZIP does not contain the 1.12 GB model or llama.cpp source code.** The steps below download the model **into the Android app assets before building**, giving the resulting APK a bundled AI model that is loaded automatically at startup. This is intentionally not published to GitHub yet: model inference on the target phone remains unverified.

## Build on ScarfaceOS

```bash
cd ~/Downloads/HexStrike-Mobile-v0.8-Bundled-Offline-AI
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/Android/Sdk"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
bash scripts/prepare-model.sh
# Preserve the llama.cpp version that previously compiled in v0.7 if possible.
git clone --depth 1 https://github.com/ggml-org/llama.cpp.git app/src/main/cpp/llama.cpp
~/Downloads/gradle-8.11.1/bin/gradle --no-daemon :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The model takes about 1.12 GB in the APK and roughly another 1.12 GB of app-private phone storage after first-launch extraction. Allow generous free space and wait for the startup copy and model load to finish. Android build/install can be slow. Check that the phone can handle the memory load and generation speed. If automatic load fails, CHANGE MODEL still permits manual GGUF selection.

The model is **Qwen/Qwen2.5-1.5B-Instruct-GGUF** (`qwen2.5-1.5b-instruct-q4_k_m.gguf`, Apache-2.0), available at https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF . Observe the model's licence when redistributing. The app itself is an unofficial derivative; see `LICENSE-HEXSTRIKE` and `HEXSTRIKE-ATTRIBUTION.md` for upstream credit. llama.cpp also has its own licence in the cloned upstream directory. Do not describe this app as antivirus or as the full HexStrike desktop server.

For open-source GitHub, **keep the GGUF excluded** from Git and publish source separately from signed install packages. This is an experimental debug build, not a production release.
