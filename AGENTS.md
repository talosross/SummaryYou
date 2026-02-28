# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Project Overview

SummaryYou is an AI/LLM summarizer FOSS Android app that summarizes YouTube videos, web articles, images, and documents. It follows [MAD](https://developer.android.com/courses/pathways/android-architecture) principles using pure Kotlin + Jetpack Compose + Material 3 Expressive. The app is BYOK (Bring Your Own Key), allowing users to configure their own LLM API keys.

## Common Commands

### Building the App
```bash
# Clean and build debug APK (foss flavor)
./gradlew clean assembleFossDebug

# Build specific release variants
./gradlew assemblePlaystoreRelease
./gradlew assembleFossRelease

# Compile only (fast check)
./gradlew compileFossDebugKotlin
```

### Testing
```bash
# Run unit tests (foss flavor)
./gradlew testFossDebugUnitTest

# Run all unit tests
./gradlew clean test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run a specific test class
./gradlew testFossDebugUnitTest --tests="com.talosross.summaryyou.YouTubeExtractorTest"
```

### Code Quality
```bash
# Run lint analysis
./gradlew lint
```

## Code Style Guidelines
- Must follow best practices
- Make the code structure as simple as possible
- Follow [Kotlin style guide](https://developer.android.com/kotlin/style-guide)

## Dev Environment Tips
- Use `./gradlew clean assembleFossDebug` to verify the build
- Use `./gradlew testFossDebugUnitTest` to run tests
- Configure SDK paths in `local.properties`
- Set up signing keys in `keystore-playstore.properties` and `keystore-foss.properties` for release builds

## Architecture & Code Structure

### Technology Stack
- **Language**: Kotlin 2.3 (language version 2.3, JVM 21)
- **UI Framework**: Jetpack Compose with Material 3 Expressive (`1.5.0-alpha12`)
- **DI**: Dagger/Hilt 2.59
- **Database**: Room 2.8.4 (SQLite) with `fallbackToDestructiveMigration`
- **Networking**: Ktor 3.4.0
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Jetpack Navigation Compose with type-safe `@Serializable` routes
- **ML Kit**: For text recognition from images (build flavor dependent)
- **LLM Integration**: Koog agents library (`ai.koog:koog-agents:0.6.0`)
- **Serialization**: kotlinx.serialization 1.10.0
- **HTML Parsing**: Jsoup 1.22.1
- **Image Loading**: Coil 2.7.0

### Build Flavors
The app uses two product flavors:
- **playstore**: Uses Google Play Services ML Kit (smaller APK size, requires Google Play Services). Includes proxy summarizer and Play Integrity API.
- **foss**: Bundles ML model in APK (larger package size, works without Google Play Services). App ID suffix `.foss`.

Configure in `app/build.gradle.kts` under the `productFlavors` block.

### Project Structure

#### Core Application (`app/src/main/kotlin/com/talosross/summaryyou/`)
- **`App.kt`**: Application class with `@HiltAndroidApp` annotation
- **`MainActivity.kt`**: Main activity handling deep links, share intents, and initialization
- **`InstantSummaryActivity.kt`**: Overlay activity for instant summaries via share sheet
- **`UserPreferencesRepository.kt`**: DataStore-backed user preferences

#### Dependency Injection (`di/`)
- **`AppModule.kt`**: Hilt module providing UserPreferencesRepository, Room database + DAO, HistoryRepository, LLMHandler (with injected UserPreferencesRepository), ProxySummarizer, and Ktor HTTP client
- **`FlavorConfig.kt`**: Build-flavor-specific configuration interface

#### Data Layer (`data/`)
- **`AppDatabase.kt`**: Room database definition (`summary_you_db`) with destructive migration fallback
- **`HistoryDao.kt`**: DAO for history CRUD and paging operations
- **`HistoryRepository.kt`**: Repository managing history data
- **`converters/`**: Type converters for Room (SummaryType, VideoSubtype, SummaryLength)

#### Domain Models (`model/`)
- **`SummaryData.kt`**: Core summary result data model
- **`SummarySource.kt`**: Sealed class representing input sources (`Video`, `Article`, `Text`, `Document`, `None`)
- **`HistorySummary.kt`**: History entry Room entity
- **`SummaryType.kt`**: Supported content types (YouTube, BiliBili, article, image, document, text)
- **`VideoSubtype.kt`**: Video platform classifications
- **`SummaryException.kt`**: Custom exception types with `fromMessage()` factory and `getUserMessageResId()` for user-facing error mapping
- **`ExtractedContent.kt`**: Content extraction result model

#### LLM Integration (`llm/`)
- **`LLMHandler.kt`**: Core handler for LLM interactions using Koog agent graph strategy. `UserPreferencesRepository` is injected via constructor (not manually created).
- **`AIProvider.kt`**: Provider enum with model lists (Integrated, OpenAI, Gemini, Claude, DeepSeek, Mistral, Qwen, Ollama, OpenRouter)
- **`ProxySummarizer.kt`**: Proxy-based summarizer for playstore flavor
- **`Prompts.kt`**: Prompt templates for different content types and summary lengths
- **`CustomModel.kt`**: Custom model configuration
- **`tools/`**: Content extraction tools
  - `YouTubeTranscriptTool.kt`: YouTube transcript extraction (companion exposes `isYouTubeLink()` and `extractVideoId()` for testability)
  - `BiliBiliSubtitleTool.kt`: BiliBili subtitle extraction
  - `ArticleExtractorTool.kt`: Web article content extraction via Jsoup
  - `FileExtractorTool.kt`: Document/image parsing (ML Kit flavor-dependent)

#### Utilities (`util/`)
- **`UrlUtils.kt`**: `extractHttpUrl()` top-level function — extracts first HTTP(S) URL from text
- **`LocaleUtil.kt`**: Locale helper utilities
- **`MarkdownParser.kt`**: Markdown-to-AnnotatedString conversion

#### ViewModels (`vm/`)
- **`AppViewModel.kt`**: App-level state, onboarding flow, settings management, deep link / share intent handling
- **`SummaryViewModel.kt`**: Main summarization orchestration — determines `SummarySource`, delegates to `LLMHandler` or `ProxySummarizer`
- **`HistoryViewModel.kt`**: History browsing, searching, deletion with Paging 3
- **`UiState.kt`**: State data classes (`SettingsUiState`, `SummarizationState`, `AppStartAction`)

#### UI Layer (`ui/`)
- **`Nav.kt`**: Type-safe route definitions as `@Serializable sealed interface Nav` with `Home`, `Onboarding`, `History`, and `Settings(highlight: String? = null)`
- **`AppNavigation.kt`**: Navigation graph using `composable<Nav.Home>`, `composable<Nav.Settings>` etc. (type-safe, no string-based routing)
- **`page/`**: Screen composables
  - `HomeScreen.kt`: Main summary screen (orchestration only)
  - `HomeComponents.kt`: Extracted sub-composables — `HomeTopAppBar`, `InputSection`, `LengthSelector`, `FloatingActionButtons`, `ErrorMessage`
  - `SettingsScreen.kt`: App configuration screen (orchestration + `SettingsContent`)
  - `SettingsComponents.kt`: Extracted sub-composables — `SettingsGroup`, `ThemeSettingsDialog`, `AIProviderSettingsDialog`, `ModelSettingsDialog`, `RadioButtonItem`, `AIProviderItem`
  - `HistoryScreen.kt`: History browser with paging
  - `OnboardingScreen.kt`: First-run setup wizard
  - `BilibiliLoginScreen.kt`: BiliBili WebView authentication
- **`component/`**: Reusable UI components
  - `InstantSummaryDialog.kt`: Overlay summary dialog (extracted from `InstantSummaryActivity`)
  - `SummaryCard.kt`: Summary result card with copy, share, TTS
  - `ClickablePasteIcon.kt`: Paste/clear icon button
  - `LogoIcon.kt`: App logo composable
- **`theme/`**: Material 3 theming (colors, typography, theme)

### Supported Content Types

| Type | Source | Processing Method |
|------|--------|-------------------|
| YouTube videos | Video URL | Transcript extraction via `YouTubeTranscriptTool` |
| BiliBili videos | Video URL | Subtitle extraction via `BiliBiliSubtitleTool` |
| Articles | URL | Content extraction via `ArticleExtractorTool` |
| Images | File/URI | ML Kit text recognition (flavor-dependent) |
| Documents | File/URI | File parsing via `FileExtractorTool` |
| Text | Direct input | Direct LLM processing |

### LLM Providers

The app supports multiple LLM providers configured in `AIProvider.kt`:
- **Integrated**: Built-in Google AI (playstore flavor only, uses proxy)
- **OpenAI**: Models from `OpenAIModels` (Completion-capable, non-audio)
- **Gemini**: Models from `GoogleModels` (Completion-capable)
- **Claude**: Models from `AnthropicModels` (Completion-capable, version-sorted)
- **DeepSeek**: Models from `DeepSeekModels` (Completion-capable)
- **Mistral**: Models from `MistralAIModels` (Completion-capable)
- **Qwen**: Models from `DashscopeModels` (Completion-capable)
- **Ollama**: Local models (Llama, Qwen, Gemma — mandatory base URL)
- **OpenRouter**: Models from `OpenRouterModels` (Completion-capable)
- **Custom models**: User-configurable via custom model name text field in settings

All providers support custom model names entered manually.

## Key Configuration Files

### `build.gradle.kts` (root)
Defines plugin versions:
- Android Gradle Plugin: 9.0.0
- Kotlin: 2.3.0
- KSP: 2.3.4
- Hilt: 2.59

### `app/build.gradle.kts`
Main app configuration:
- Min SDK: 28 (Android 9)
- Target/Compile SDK: 36
- Java 21 toolchain, Kotlin language version 2.3
- Compose BOM: 2026.01.00
- Material 3: 1.5.0-alpha12 (expressive alpha)
- Room: 2.8.4
- Navigation Compose: 2.9.6
- Signing configs: `keystore-playstore.properties` and `keystore-foss.properties`

### `local.properties`
Local SDK paths + proxy URL + GCP project number (not tracked in git)

### `keystore-*.properties`
Release signing keys (not tracked in git)

## Development Guidelines

### Code Style
- Follow [Kotlin Android Style Guide](https://developer.android.com/kotlin/style-guide)
- Keep code structure as simple as possible
- Follow Android best practices
- Use `internal` visibility for composables shared across files within the same package

### Architecture Patterns
- **MVVM** (Model-View-ViewModel)
- **Repository pattern** for data access
- **Hilt DI** for all injectable components — avoid manual instantiation of repositories
- **StateFlow** for reactive UI state
- **Type-safe navigation** using `@Serializable sealed interface` routes (no string-based routing)
- **Composable extraction** — keep screen files as orchestration layers; extract sub-composables into `*Components.kt` files
- **Domain model separation** — models like `SummarySource` live in `model/` package, not nested inside ViewModels

### Dependency Injection
- All major components are Hilt-injectable (`@Singleton`)
- `LLMHandler` receives `UserPreferencesRepository`, `Context`, and `HttpClient` via constructor injection
- Never manually create `UserPreferencesRepository` — always inject via Hilt

### Room Database
- Database name: `summary_you_db`
- Main entity: `HistorySummary` with DAO operations
- Migration strategy: `fallbackToDestructiveMigration(dropAllTables = true)`
- Type converters in `converters/` package
- Schema export to `$projectDir/schemas`

### Navigation
- Routes defined as `@Serializable sealed interface Nav` in `ui/Nav.kt`
- Use `composable<Nav.Route>` and `navController.navigate(Nav.Route)` — never use string-based routes
- Route parameters are type-safe (e.g., `Nav.Settings(highlight = "ai")`)

### Material 3 Expressive
The app uses Material 3 Expressive alpha features for enhanced UI:
- `LoadingIndicator`, `MediumFlexibleTopAppBar`, `ToggleFloatingActionButton`, `FloatingActionButtonMenu`, `ButtonGroupDefaults`
- Material version: `1.5.0-alpha12`

## Testing
- **Unit tests** in `src/test/kotlin/com/talosross/summaryyou/`
  - `YouTubeExtractorTest.kt`: 16 tests for video ID extraction and YouTube link detection
  - `SummaryExceptionTest.kt`: 11 tests for `SummaryException.fromMessage()` factory
  - `UrlExtractionTest.kt`: 9 tests for URL extraction regex behavior
  - `ExampleUnitTest.kt`: Basic sanity test
- **Instrumented tests** in `src/androidTest/kotlin/`
- Run unit tests: `./gradlew testFossDebugUnitTest`
- Run instrumented tests: `./gradlew connectedAndroidTest`

## Build & Release

### Development
- Debug builds are debuggable with application ID suffix `.debug`
- FOSS debug builds: `assembleFossDebug`
- Playstore debug builds: `assemblePlaystoreDebug`

### Release
- Release builds are minified (`isMinifyEnabled = true`) and shrunk (`isShrinkResources = true`)
- Signing is flavor-specific and optional if the properties file is missing
- Two distribution channels:
  1. **GitHub Releases**: FOSS variant (`.foss` suffix)
  2. **Google Play Store**: Playstore variant with Google-managed signing

### Version Info
Current version: **2.0.0** (versionCode 2026022822)
- Update version in `app/build.gradle.kts` lines 24-25

## Permissions & Security
The app requires:
- Internet access (for LLM API calls and content extraction)
- Storage access (for document/image processing)
- Clipboard access (for paste and instant summary features)
- Overlay permission (for instant summary overlay via share sheet)
- Camera permission (for camera-based image summarization)

## Key Dependencies
| Dependency | Version | Purpose |
|---|---|---|
| `ai.koog:koog-agents` | 0.6.0 | LLM agent framework |
| `io.ktor:ktor-client-android` | 3.4.0 | HTTP client |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.10.0 | JSON serialization |
| `org.jsoup:jsoup` | 1.22.1 | HTML parsing |
| `io.coil-kt:coil-compose` | 2.7.0 | Image loading |
| `androidx.room:room-*` | 2.8.4 | Local database |
| `com.google.dagger:hilt-android` | 2.59 | Dependency injection |
| `androidx.navigation:navigation-compose` | 2.9.6 | Type-safe navigation |
| `androidx.paging:paging-compose` | 3.3.6 | Paging support |
| `androidx.datastore:datastore-preferences` | 1.2.0 | Preferences storage |

## Build Warnings & Notes
- ML Kit dependency is flavor-specific (see `app/build.gradle.kts` dependencies)
- ProGuard/R8 rules configured in `app/proguard-rules.pro`
- Some META-INF resources excluded from APK packaging
- Lint rule: `MissingTranslation` disabled for localization flexibility
- `hiltViewModel()` deprecation warning — will be migrated to `androidx.hilt.lifecycle.viewmodel.compose` package
