<div align="center">
  <img src=".github/logo.png" width="160" height="160" align="center">
  
  # Summary You

  **Summarize YouTube Videos, Articles, Images and Documents with AI**

  [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE.txt)
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.3-purple.svg)](https://kotlinlang.org)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg)](https://developer.android.com/jetpack/compose)
</div>

---

## 📱 Screenshots

<div align="center">
  <img src="https://cdn.jsdelivr.net/gh/talosross/SummaryYou@refs/heads/master/.github/screenshots/screen1.webp" width="24%" alt="Home Screen"/>
  <img src="https://cdn.jsdelivr.net/gh/talosross/SummaryYou@refs/heads/master/.github/screenshots/screen2.webp" width="24%" alt="Share Feature"/>
  <img src="https://cdn.jsdelivr.net/gh/talosross/SummaryYou@refs/heads/master/.github/screenshots/screen3.webp" width="24%" alt="History"/>
</div>

## ⬇️ Download

<a href='https://play.google.com/store/apps/details?id=com.talosross.summaryyou'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='240'/></a>

You can also grab the FOSS APK from [GitHub Releases](https://github.com/talosross/SummaryYou/releases).

## ✨ Features

- 🎬 **YouTube & BiliBili** — Summarize videos via transcript/subtitle extraction
- 🌐 **Articles** — Extract and summarize any web article
- 📄 **Documents & Images** — Summarize PDFs, text files, and images (OCR)
- 📏 **Adjustable Length** — Short, medium, or long summaries
- 🔊 **Text-to-Speech** — Listen to your summaries
- 📋 **Instant Summary** — Summarize directly from the share sheet
- 📜 **History** — Browse, search, and manage past summaries
- 🔒 **Paywall Detection** — Warns you when content is behind a paywall
- 🎨 **Material 3 Expressive** — Dynamic color theming & OLED dark mode
- 🌍 **12 Languages** — AR, CS, DE, EN, ES, FR, IT, JA, PT, TR, UK, ZH

## 🤖 Supported LLM Providers

Bring Your Own Key (BYOK) — use any of these providers with your own API key:

| Provider | Notes |
|----------|-------|
| **OpenAI** | GPT-4.1, GPT-5, o3, o4-mini, … |
| **Google Gemini** | Gemini 2.0 Flash, 2.5 Pro, … |
| **Anthropic Claude** | Claude 3.5 Sonnet, Opus 4, … |
| **DeepSeek** | DeepSeek Chat & Reasoner |
| **Mistral** | Mistral AI models |
| **Qwen** | Alibaba Qwen models |
| **OpenRouter** | Access 100+ models via one key |
| **Ollama** | Run local models (Llama, Gemma, …) |

All providers support **custom model names** for new/unlisted models.

> The **Play Store version** also includes a built-in provider (no key required, paid).

## 🏗️ Build Flavors

| | **Play Store** | **FOSS** |
|---|---|---|
| **LLM Access** | Built-in provider (paid) + BYOK | BYOK only |
| **ML Kit** | Google Play Services (smaller APK) | Bundled model (larger APK) |
| **Distribution** | Google Play | GitHub Releases |
| **App ID** | `com.talosross.summaryyou` | `com.talosross.summaryyou.foss` |
| **Google Play Services** | Required | Not required |
| **Build** | — | `./gradlew assembleFossDebug` |

> **Note:** The Play Store flavor requires proprietary signing keys and Google Play Services configuration that are not included in this repository.

## 🛠️ Tech Stack

- **Kotlin 2.3** with **Jetpack Compose** & **Material 3 Expressive**
- **Hilt** for dependency injection
- **Room** for local history database
- **Ktor** for networking
- **Koog Agents** for LLM integration
- **Coil** for image loading
- **Navigation Compose** with type-safe routes

## 🏗️ Building from Source

```bash
git clone https://github.com/talosross/SummaryYou.git
cd SummaryYou
./gradlew assembleFossDebug
```

The APK will be at `app/build/outputs/apk/foss/debug/`.

## ⭐ Star History

![Star History Chart](https://api.star-history.com/svg?repos=talosross/SummaryYou&type=Timeline)

## 🙏 Credits

- **[kid1412621/NanoNova](https://github.com/kid1412621/SummaryExpressive)** — Major thanks for the [Summary Expressive](https://github.com/kid1412621/SummaryExpressive) fork, which introduced Material 3 Expressive UI, multi-provider LLM support, and significant architectural improvements. The codebase was merged back into this repository in 2026.
- UI inspiration from [Seal](https://github.com/JunkFood02/Seal)
- [Material color utilities](https://github.com/material-foundation/material-color-utilities)

## 📄 License

This project is licensed under the **[GNU General Public License v3.0](LICENSE.txt)**.

<div align="right">
  <a href="#summary-you">👆 Scroll to top</a>
</div>
