<div align="center">

<img src="docs/banner.gif" alt="Internet Radio" width="100%">

<br>

[![Android 8.0+](https://img.shields.io/badge/Android%208.0+-1f2a38?style=flat-square&logo=android&logoColor=63abff)](#get-the-app)
[![Kotlin](https://img.shields.io/badge/Kotlin%202.3-1f2a38?style=flat-square&logo=kotlin&logoColor=63abff)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1f2a38?style=flat-square&logo=jetpackcompose&logoColor=63abff)](https://developer.android.com/compose)
[![Media3](https://img.shields.io/badge/Media3-1f2a38?style=flat-square&logo=android&logoColor=63abff)](https://developer.android.com/media/media3)
[![17 languages](https://img.shields.io/badge/17%20languages-1f2a38?style=flat-square&logo=googletranslate&logoColor=63abff)](#-look--feel)
[![MIT](https://img.shields.io/badge/MIT-1f2a38?style=flat-square&logo=opensourceinitiative&logoColor=63abff)](LICENSE)

**Thousands of radio stations from all over the world, in one clean, ad-free Android app.**<br>
Genres, collections, smart search, favorites with folders and background playback.

<br>

<a href="https://www.rustore.ru/catalog/app/com.tohn95.internetradio"><img src="docs/btn-rustore.svg" alt="Get it on RuStore" height="52"></a>
&nbsp;
<a href="https://github.com/bboyJohnn/InternetRadio/releases/latest"><img src="docs/btn-apk.svg" alt="Download APK" height="52"></a>

</div>

<br>

## Screens

<div align="center">

| Home | Player | Discover |
|:---:|:---:|:---:|
| <img src="docs/screens/01-home.png" width="250"> | <img src="docs/screens/02-player.png" width="250"> | <img src="docs/screens/03-discover.png" width="250"> |
| **Search** | **Favorites** | **Settings** |
| <img src="docs/screens/04-search.png" width="250"> | <img src="docs/screens/05-favorites.png" width="250"> | <img src="docs/screens/06-settings.png" width="250"> |

</div>

The player **takes its colors from the station logo**, shows what is playing right now and
opens the track in **Spotify** or **YouTube Music** with one tap, or just copies its name.

<br>

## Features

<table>
<tr>
<td width="50%" valign="top">

### 🧭 Discover
- **Home**: genres, popular, a new station, "For you", the trending chart and what is on air in your country
- **Collections**: I'm feeling lucky, mixes, moods, decades
- **World radio**: flags of the biggest radio countries
- **Smart search** by country, genre, format and bitrate
- Lists sorted by listeners or A–Z, with search inside

</td>
<td width="50%" valign="top">

### ▶️ Player
- **Now playing**: the live track title from the stream
- **Station colors**: background and accents from the logo
- **Volume arc** around the spinning vinyl
- Spotify · YouTube Music · copy · 👍 vote for the station
- **Sleep timer** with a soft fade-out
- Station info: codec, bitrate, votes, homepage, share

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📡 Always on air
- **Background playback** with a styled notification, ♡ and ⏮ ⏭
- **Auto-reconnect** as soon as the network is back
- **Live edge**: after a long pause you hear what is on now
- Pauses when the headphones are unplugged
- **Home-screen widget**: station name and play / pause

</td>
<td width="50%" valign="top">

### 💙 Your library
- **Favorites** with folders, like playlists; one station can be in many
- **Your own station**: add any stream by its URL
- **Find a logo** on the station's website
- **History** grouped by day, with search

</td>
</tr>
<tr>
<td colspan="2" valign="top">

### 🎨 Look & feel
- **Live theme**: any hue and saturation (OKLCH) or one of 12 presets
- Light, dark, system and true black (OLED); **Material You** colors on Android 12+
- Animated waves and soft ambient backgrounds
- **17 languages**: EN RU DE FR ES PT IT PL TR NL AR HI ID VI JA KO ZH, right-to-left for Arabic

</td>
</tr>
</table>

> No ads, no sign-up, no tracking. It does not need Google services and works on any Android 8.0+ device.

<br>

## Get the app

- 🛒 [**RuStore**](https://www.rustore.ru/catalog/app/com.tohn95.internetradio): the store version with auto-updates. **Recommended.**
- 📦 [**APK from Releases**](https://github.com/bboyJohnn/InternetRadio/releases/latest): the same signed build, installed by hand.

To install the APK by hand, allow "Install unknown apps" for your browser or file manager when Android asks.

<br>

## Build from source

You need JDK 17+ (the JBR bundled with Android Studio is fine) and Android SDK 36.

```bash
git clone https://github.com/bboyJohnn/InternetRadio.git
cd InternetRadio
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
```

The APK appears in `app/build/outputs/apk/debug/`. Tests and lint:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
```

**Release build.** `./gradlew assembleRelease` signs with your own key from `keystore.properties` in the
project root (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Git ignores that file. Without it,
the release build uses the debug key.

<br>

## Tech stack

| Part | Built with |
|---|---|
| **UI** | Kotlin · Jetpack Compose · Material 3 · custom OKLCH theme engine |
| **Audio** | AndroidX Media3 (ExoPlayer + MediaSession), HLS, ICY metadata |
| **Data** | [radio-browser.info](https://www.radio-browser.info/) API · Retrofit · OkHttp · kotlinx.serialization |
| **Storage** | Room (favorites, folders, history, offline cache) · DataStore |
| **Images** | Coil 3 · AndroidX Palette (colors from logos) |
| **Wiring** | Hilt · Navigation Compose · Glance (widget) · AppCompat per-app language |

<details>
<summary><b>Repo layout</b></summary>

```
app/src/main/java/com/tohn95/internetradio/
  data/          radio-browser client, Room database, repository, logo finder
  playback/      Media3 service, player controller, reconnect, sleep timer
  ui/            Compose screens: home, search, player, favorites, history, settings
  ui/theme/      OKLCH palette and live theme
  widget/        Glance home-screen widget
app/src/main/res/  17 translations (values-*), icons, genre pictures, widget
app/src/test/      unit tests: reconnect, sleep timer, metadata, API client, theme
docs/              README banner, buttons and screenshots
```

</details>

<br>

## Credits

- 📻 Station catalog: [radio-browser.info](https://www.radio-browser.info/), the open community radio database
- 🖼 Genre pictures: CC0 photos via [Openverse](https://openverse.org/), listed in [docs/genre-images-credits.txt](docs/genre-images-credits.txt)
- 🎧 Streams belong to their radio stations; the app only plays their public links
- 🐛 Found a bug? Open an [issue](https://github.com/bboyJohnn/InternetRadio/issues)

<br>

<div align="center">

Made with 📻 · [MIT License](LICENSE)

</div>
