# HexStrike Mobile — Android v0.6

**Unofficial, standalone Android defensive security companion.**

HexStrike Mobile is an independent Android prototype inspired by the open-source HexStrike AI project. The application runs a lightweight rule-based local core without requiring a remote server, network connection, or manual IP setup.

## Features

- Compact red-and-black Android interface with HexStrike bear artwork
- Local core that starts when the app opens
- Read-only device information and basic security guidance
- Offline, rule-based chat and scrollable security console
- Fits Android system bars and keeps the chat field close to the console

## Current limitations

**This is a prototype, not an antivirus, a full HexStrike desktop port, or an on-device large language model.** It does not actively scan for or block malware. Chat replies are built-in rules rather than model-generated answers. It does not bundle or execute HexStrike's full collection of Linux security tools.

## Build on Linux

Requirements: JDK 21, compatible Android SDK (Platform 35), Gradle 8.11.1, and ADB for USB installation.

```bash
cd HexStrike-Mobile-v0.6-GitHub-Ready
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/Android/Sdk"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
~/Downloads/gradle-8.11.1/bin/gradle --no-daemon :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The release is supplied as source; build locally before installing. `local.properties`, build output and signing keys are excluded from Git.

## Special Thanks & Credits

### Original HexStrike AI Developer

**Muhammad Osama (0x4m4)**

Special thanks to Muhammad Osama for creating HexStrike AI and sharing his work with the open-source cybersecurity community. His work helped inspire this independent Android project. We recognise the effort, research and development behind the original framework.

**Original project:** https://github.com/0x4m4/hexstrike-ai

### Unofficial Android Edition

HexStrike Mobile is an independent, unofficial Android project. It is **not affiliated with, endorsed by, or officially maintained by** the original HexStrike AI developer. This Android prototype is not a direct port of all desktop server features.

Original HexStrike assets remain subject to their applicable copyright and licence conditions. The upstream MIT licence and attribution are included in [`LICENSE-HEXSTRIKE`](LICENSE-HEXSTRIKE) and [`HEXSTRIKE-ATTRIBUTION.md`](HEXSTRIKE-ATTRIBUTION.md).

Thank you to the original developer for contributing to the open-source cybersecurity community.

## Responsible use

Designed for defensive learning and user-controlled, on-device diagnostics. Only test systems you own or are explicitly authorised to test.
