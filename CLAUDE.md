# CLAUDE.md

Guidance for working in this repository.

## What this is

**Chora** (`com.craftworks.music`) — an Android music player that streams from Subsonic/OpenSubsonic and Navidrome servers, or plays local files via MediaStore. One APK serves phones, tablets, Android TV and Android Auto. Written in Kotlin with Jetpack Compose. This checkout is a fork (`origin` = `github.com/pjonesau/Chora`) of `CraftWorksMC/Chora`.

The upstream author describes the code as messy (early AI-assisted development). Expect duplication, global singletons, and some dead/commented code. Match surrounding style rather than imposing a new architecture.

## Build

Single Gradle module `:app` (root project is named `MusicPlayer`).

- Gradle 9.5.0 (wrapper), AGP 9.3.1 (built-in Kotlin support — no `kotlin-android` plugin is applied), Kotlin 2.4.x, KSP.
- **JDK 21 required** (`sourceCompatibility`/`targetCompatibility` = 21; CI uses Temurin 21).
- `compileSdk`/`targetSdk` = 37, `minSdk` = 23. Code references `Build.VERSION_CODES.CINNAMON_BUN`, so SDK platform 37 must be installed.
- Dependencies come from Google, Maven Central and **JitPack** (`ComposeFadingEdges`). Versions live in `gradle/libs.versions.toml` (a few are inline in `app/build.gradle.kts` / root `build.gradle.kts`).
- Core library desugaring is on.

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

Toolchain setup gotchas (learned setting up a fresh Linux machine):
- There is no `local.properties`; point Gradle at the SDK with `ANDROID_HOME=… ./gradlew …` or create a (git-ignored) `local.properties` with `sdk.dir=…`.
- The command-line tools only work from `<sdk>/cmdline-tools/latest/bin/` — unzipping straight into `<sdk>/cmdline-tools/` gives "Could not determine SDK root".
- Platforms from 36.1 on are named `major.minor`: `compileSdk = 37` needs **`platforms;android-37.0`** (`platforms;android-37` does not exist). Gradle fetches build-tools itself once licences are accepted (`sdkmanager --licenses`). The "sdkmanager is deprecated" warning is harmless.
- A clean `assembleRelease` takes ~2 minutes. The compiler emits ~20 pre-existing deprecation/lint warnings and "Unable to strip libandroidx.graphics.path.so, libdatastore_shared_counter.so" — all harmless; don't chase them unless asked.
- The APK is universal (AndroidX ships small native libs for arm64-v8a, armeabi-v7a, x86, x86_64), so one APK covers 32-bit Fire TV devices too.

Build-type quirks to be aware of:
- `release` has R8 minify + resource shrinking. It is signed from `keystore.properties` in the project root (or the file named by `$CHORA_KEYSTORE_PROPERTIES`; format in `keystore.properties.example`), and **falls back to the debug key** when neither exists. Keystores and `keystore.properties` are git-ignored; never commit them.
- Signing determines what can be updated in place. The debug key is per machine (`~/.android/debug.keystore`); upstream's GitHub/F-Droid/Play releases and each CI run use different keys. Installing over an install signed with another key fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` — the old app must be uninstalled first, which **wipes the configured servers** (provider config is encrypted with a device Keystore key and isn't backed up). Warn before any uninstall, and keep the debug keystore backed up if it is the release key.
- `debug` is `isDebuggable = false` and `isProfileable = true` (so `run-as`/debugger attach won't work on the debug build).
- `dependenciesInfo` is excluded from APK/bundle (F-Droid requirement).
- Version is set in `app/build.gradle.kts` (`versionCode`/`versionName`); fastlane changelogs in `fastlane/metadata/android/*/changelogs/<versionCode>.txt`.

CI (`.github/workflows/build.yml`) builds `assembleDebug` on every push and uploads the APK. There are **no unit or instrumented tests** in the repo, so verification means a successful `assembleRelease` plus manual testing on a device.

## Deploying to Fire TV / Android TV

Sideload over network ADB (enable Developer Options → ADB debugging on the device; the first connection must be approved on the TV screen, after which `~/.android/adbkey*` is trusted):

```bash
adb connect <tv-ip>:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```

- Fire OS 6/7/8 (Android 7.1/9/11) all exceed `minSdk` 23 and report `UI_MODE_TYPE_TELEVISION`, so the TV UI is used automatically. Vega OS Fire TV devices cannot run APKs at all.
- TVs are often in use by other people. Read-only `adb shell` queries and background `adb install`/`uninstall` don't show anything on screen; launching an app does. Check what is in the foreground first (`adb shell dumpsys activity activities | grep mResumedActivity`) and ask before doing anything visible.
- Check for an existing install and its signer before installing (`adb shell dumpsys package com.craftworks.music | grep -E "versionName|signatures"`) — see the signing note above.

## Architecture

```
ChoraApplication (@HiltAndroidApp)
 ├─ MigrationManager.init()     one-shot data migrations (SharedPreferences "MigrationStatus")
 └─ MediaProviderManager.init() loads encrypted provider config

MainActivity (single activity, Compose)
 ├─ phone/tablet: Scaffold + BottomSheetScaffold (mini player ↔ NowPlaying), NavigationBar / NavigationRail
 └─ TV: TvSideNavigation (tv-material NavigationDrawer) wraps each screen
     └─ SetupNavGraph (NavGraph.kt) — type-safe routes from data/model/Screen.kt

UI (Compose) → ViewModels (@HiltViewModel) → Repositories (@Singleton) → MediaProviderManager.currentProvider → MediaProvider impl

ChoraMediaLibraryService (Media3 MediaLibraryService, player/MusicService.kt)
 └─ ExoPlayer + MediaLibrarySession; UI talks to it only through a Media3 MediaController
```

### Package map (`app/src/main/java/com/craftworks/music/`)

| Package | Role |
|---|---|
| `MainActivity.kt`, `NavGraph.kt`, `Application.kt` | Entry points, top-level layout, navigation graph, TV side drawer, phone bottom bar |
| `data/model/` | Domain models (`MediaModel.Album/Song/Artist/Playlist/...`), `MediaQuery` request objects, sort enums, `Screen` routes, `ProviderFeature` flags, lyrics model |
| `data/providers/media/` | `MediaProvider` abstract class + `subsonic/`, `navidrome/`, `local/` implementations |
| `data/providers/lyrics/` | External lyrics sources: LRCLIB, NetEase, BiniLyrics, Unison |
| `data/repository/` | Thin `@Singleton` repositories that call the *current* provider and map models to Media3 `MediaItem` |
| `data/di/` | Hilt module (only lyrics data sources need explicit `@Provides`; the rest use `@Inject` constructors) |
| `managers/` | Global objects: `MediaProviderManager`, `DataRefreshManager`, `MigrationManager`, `TranscodeManager` |
| `managers/settings/` | DataStore-backed settings managers (Appearance, Playback, LocalData, MediaProvider, Misc) |
| `migrations/` | `Migration` implementations, run in order by `MigrationManager` |
| `player/` | `ChoraMediaLibraryService`, `MediaControllerManager` / `rememberManagedMediaController()` |
| `ui/screens/`, `ui/elements/`, `ui/playing/` | Phone/tablet UI. Each has a `tv/` sibling with the TV variant |
| `ui/viewmodels/` | One ViewModel per main screen; scoped to the `MainGraph` back-stack entry so they survive tab switches |
| `utils/` | Crypto, TTML parser, lyrics helpers, paging, misc |

### Key design choices

**Pluggable media providers.** `MediaProvider` is an abstract `@Serializable` class with a large API mirroring OpenSubsonic (albums, artists, playlists, radio, scrobble, play queue, lyrics, ratings, search…). Implementations:
- `SubsonicMediaProvider` — Ktorfit + Ktor/OkHttp against `rest/*.view` endpoints; auth is the Subsonic salted-MD5 token (`u`, `t`, `s`, `v`, `c=Chora`, `f=json`) injected by a client plugin. Optional trust-all TLS when `allowSelfSignedCert` is set.
- `NavidromeMediaProvider` — **extends** `SubsonicMediaProvider`, adds Navidrome's native REST API (`NavidromeService`) with a bearer token in `X-ND-Authorization`, refreshed under a `Mutex` on 401. Native API gives richer sorting/filtering; falls back to Subsonic endpoints via `super`.
- `LocalMediaProvider` — queries `MediaStore`; only supports `OFFLINE_PLAYBACK`.

Capabilities are advertised via `featureFlags: EnumSet<ProviderFeature>` and `supported*Sort` lists; the UI hides features (e.g. Radios/Playlists nav items) based on these. When adding a feature, add a flag rather than type-checking the provider.

**Provider registry is a global object.** `MediaProviderManager` holds all configured providers keyed by UUID plus `currentProvider: StateFlow`. Repositories read `MediaProviderManager.currentProvider.value` directly (not injected). Changing providers emits `DataRefreshManager.dataSourceChangedEvent`, which ViewModels collect to reload.

**Encrypted provider persistence.** Providers (including server credentials) are polymorphically serialized with kotlinx.serialization (`MediaProvider.serializerModule`, `@SerialName("subsonic"|"navidrome")`) into a DataStore file `providers.pb`, encrypted with AES/CBC using a key in the Android Keystore (`utils/CryptoData.kt`, `EncryptedMediaProviderSerializer`). The key is device-bound, so this file does not survive backup/restore to another device. New provider subclasses **must** be registered in `serializerModule`. Load/save uses `runBlocking`.

**Opaque media URIs resolved at play time.** Songs become `MediaItem`s with URI `media://<providerId>/<songId>` and metadata extras (`id`, `providerId`, `providerType`, `replayGain`, `artists`, …; accessors at the bottom of `MediaModel.kt`). A `ResolvingDataSource` in the service turns that into a real stream URL via the provider, applying the current transcoding bitrate/format (Wi‑Fi vs cellular from `TranscodeManager`). This keeps credentials and transcoding choices out of persisted queues.

**Playback lives entirely in the Media3 service.** `ChoraMediaLibraryService` owns ExoPlayer, ReplayGain (volume from track gain), scrobbling (timer loop, threshold from settings), lyrics prefetch on track change, sleep timer, play-queue save/resume, and the Android Auto browse tree (`onGetChildren`, `onSearch`, root nodes mirror enabled nav items). UI obtains a `MediaController` via `rememberManagedMediaController()` (singleton `MediaControllerManager`). Audio offload is enabled.

**One APK, two UIs.** TV is detected at runtime with `uiMode & UI_MODE_TYPE_MASK == UI_MODE_TYPE_TELEVISION` (in both `MainActivity` and `SetupNavGraph`). Each route then renders either the phone composable or the `Tv*` composable (androidx.tv `tv-material`/`tv-foundation`, focus groups, `FocusRequester`s, D‑pad `onKeyEvent` handlers). Shared ViewModels back both. Manifest declares `LEANBACK_LAUNCHER`, a TV banner (`@mipmap/ic_banner`), and `leanback`/`touchscreen` as not required. When changing a screen, check whether the `tv/` counterpart needs the same change.

**Settings.** A single Preferences DataStore named `settings` (`Context.dataStore` extension in `MainActivity.kt`) shared by all `*SettingsManager` classes, which expose `Flow`s and `suspend` setters. Some settings managers are constructed ad hoc in composables (`AppearanceSettingsManager(context)`) rather than injected.

**Lyrics.** `LyricsRepository` queries all enabled sources in parallel (order/enablement from `MediaProviderSettingsManager`) and prefers word-synced > line-synced > plain. Current lyrics are exposed through the global `LyricsState` object. Parsers handle LRC, TTML and a YAML format (snakeyaml-engine). `LyricsView` (`ui/playing/NowPlayingLyrics.kt`) is shared by phone and TV Now Playing.

**Starting playback.** Always go through `SongHelper`: `SongHelper.play(items, index, controller)` starts in order and **resets shuffle mode off**; `SongHelper.shuffle(items, controller)` turns shuffle on and starts at a random track (safe on empty lists). Don't set `mediaController.shuffleModeEnabled` directly at call sites.

**Migrations.** Append new `Migration` classes to `MigrationManager.Migrations`; the stored version is the list size. Never reorder.

### Other notes
- Network security config allows cleartext HTTP and user-installed CAs (self-hosted servers on LAN).
- Downloads use the system `DownloadManager` (`SongRepository`).
- Translations are managed through Crowdin; edit only `res/values/strings*.xml` (English) by hand. Strings are split into `strings`, `strings_actions`, `strings_dialogs`, `strings_screens`, `strings_settings`.
- `isMinifyEnabled` is on for release; if you add reflection/serialization-heavy code, verify `assembleRelease` and extend `app/proguard-rules.pro` as needed.

### Compose / player patterns to follow
- **Player listeners in composables:** register in a `DisposableEffect(mediaController)` and remove in `onDispose`. Run polling loops as children of a `LaunchedEffect` keyed on what they depend on, so they are cancelled automatically. Never create an unscoped `CoroutineScope(...)` or add a listener inside a `LaunchedEffect` without removing it — that leak existed in `LyricsView` and caused frozen or skipping lyrics.
- **TV remote keys:** Compose key events go up from the focused element through its parents, and the controls row in `TvNowPlaying` returns `false` for keys it doesn't handle. Handle global remote keys (e.g. channel up/down → next/previous) once on the outer container rather than in each focus group.

## Upstream and forks

- Sync with upstream `CraftWorksMC/Chora` `master`; this fork's work lives on `fork-ports` (one commit per ported fix, so they can be cherry-picked or upstreamed individually).
- Upstream merged a large provider/data refactor (PR #84 by Piripe, ~217 files) in September 2026. Forks based on earlier code conflict heavily, so port ideas from them by hand instead of merging.
- A review of all forks and open PRs (Oct 2026) led to porting four changes: the lyrics listener leak, shuffle reset, TV channel-key skip, and external-keystore signing. Remaining candidates, not yet ported:
  - Upstream draft PRs #110 (advanced queue) and #112 (song favourite button): wait for upstream to merge them.
  - LittleYe233/Chora `fork` branch: transcoded-stream duration fix (`DurationForcingMediaSource`), 10-band equalizer, and support for LRC lines with several timestamps. That last one is still a bug in master: only the first timestamp is used and the others stay in the text.
- To inspect forks without touching branches: `git fetch https://github.com/<owner>/<repo> <branch>:refs/forks/<owner>/<branch>`, then `git merge-tree --write-tree master refs/forks/...` to dry-run a merge.
