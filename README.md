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


## HexStrike Mobile v0.8 — Offline AI Beta

HexStrike Mobile is an unofficial experimental Android application
with local AI inference powered by llama.cpp.

### Features

- Real offline AI conversations
- Qwen2.5 1.5B Instruct tested on Motorola Moto G56 5G
- Native Android ARM64 inference
- Local device diagnostics
- Dark security console with red bear branding
- Import compatible GGUF language models
- No online AI account or API key required

### Download Your Own AI Model

1. Visit [Hugging Face GGUF Models](https://huggingface.co/models?library=gguf).
2. Choose a small, quantized GGUF instruct model.
3. Download the `.gguf` file onto your Android device.
4. Open HexStrike Mobile.
5. Tap **CHANGE MODEL**.
6. Select the downloaded GGUF file.
7. Wait for model loading to finish.
8. Start chatting offline.

Not every GGUF model is supported. Compatibility, speed, memory
requirements and storage usage depend on the device and model.

### Tested Configuration

- Device: Motorola Moto G56 5G
- AI: Qwen2.5 1.5B Instruct Q4_K_M
- Inference engine: llama.cpp
- Tested result: Successful offline text generation
- Bundled-model APK: Approximately 1.1 GB
- Observed Android app storage: Approximately 2.25 GB

### Privacy and Security

- AI generation runs on-device.
- No network connection is required for local inference.
- The tested Android build requests no app permissions.
- No cloud AI service is required.

HexStrike Mobile is NOT an antivirus or certified security scanner.

### Beta Limitations

- AI responses may take several minutes.
- Larger models may run slowly or fail to load.
- Imported models require additional storage.
- Conversation history and performance need further development.

### Attribution

This is an unofficial independent Android project inspired by
HexStrike AI. Original attribution and applicable license notices
are retained.
