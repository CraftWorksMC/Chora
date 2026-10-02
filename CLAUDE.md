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

Build-type quirks to be aware of:
- `release` has R8 minify + resource shrinking. It is signed from `keystore.properties` in the project root (or the file named by `$CHORA_KEYSTORE_PROPERTIES`; format in `keystore.properties.example`), and **falls back to the debug key** when neither exists. Keystores and `keystore.properties` are git-ignored; never commit them.
- `debug` is `isDebuggable = false` and `isProfileable = true` (so `run-as`/debugger attach won't work on the debug build).
- `dependenciesInfo` is excluded from APK/bundle (F-Droid requirement).
- Version is set in `app/build.gradle.kts` (`versionCode`/`versionName`); fastlane changelogs in `fastlane/metadata/android/*/changelogs/<versionCode>.txt`.

CI (`.github/workflows/build.yml`) builds `assembleDebug` on every push and uploads the APK. There are **no unit or instrumented tests** in the repo.

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

**Lyrics.** `LyricsRepository` queries all enabled sources in parallel (order/enablement from `MediaProviderSettingsManager`) and prefers word-synced > line-synced > plain. Current lyrics are exposed through the global `LyricsState` object. Parsers handle LRC, TTML and a YAML format (snakeyaml-engine).

**Migrations.** Append new `Migration` classes to `MigrationManager.Migrations`; the stored version is the list size. Never reorder.

### Other notes
- Network security config allows cleartext HTTP and user-installed CAs (self-hosted servers on LAN).
- Downloads use the system `DownloadManager` (`SongRepository`).
- Translations are managed through Crowdin; edit only `res/values/strings*.xml` (English) by hand. Strings are split into `strings`, `strings_actions`, `strings_dialogs`, `strings_screens`, `strings_settings`.
- `isMinifyEnabled` is on for release; if you add reflection/serialization-heavy code, verify `assembleRelease` and extend `app/proguard-rules.pro` as needed.
