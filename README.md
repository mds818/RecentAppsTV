# RecentAppsTV-Fallback

A remote-friendly recent-app switcher for Android TV and Google TV, based on [Qutaiba-Khader/RecentAppsTV](https://github.com/Qutaiba-Khader/RecentAppsTV).

Porter/Shizuku is optional. Authorized privileged access provides actual Android tasks, switching and closing. Without it, Android Usage Access provides recently used apps you can launch. History entries do not mean an app is running. Fallback requires no debugging, root or privileged manager.

## Features

- Automatic native-task / usage-history fallback, with an optional privileged-mode switch.
- D-pad navigation, held-OK context menu and deliberate two-press closing.
- Protected apps, hidden-app exceptions, optional Kill All confirmation and force-stop.
- Centered/side layout, left/right browsing, card size/shape/padding, custom accent chart and background controls.
- Independent app names, details, header, Settings button and help visibility.
- Default, Clean, Clean+ and Classic presets with a persistent pre-preset appearance restore point.
- No ads, analytics, account or app-operated external service.

## Setup

Android 7.0+ (API 24). Testing has focused on a Xiaomi TV Box running Android 14; other TV firmware needs broader testing.

1. Install the signed APK from [Releases](https://github.com/mds818/RecentAppsTV/releases).
2. Open Recent Apps and allow Usage Access if using history mode.
3. Optionally start [Porter](https://porter.darken.eu/setup) or [Shizuku](https://shizuku.rikka.app/guide/setup/) and grant this app access. Closing controls appear only in authorized task mode.

To open from a remote button, use an external remapping app with this preserved component:

```text
com.websnake.recentstv/.RecentsActivity
```

## Controls

| Button | Action |
| --- | --- |
| LEFT / RIGHT | Browse apps |
| OK | Open the selected app/control |
| Hold OK | App context menu |
| DOWN twice | Close a task when privileged access permits |
| UP | Cancel close preview, or move toward top controls |
| UP twice with Settings hidden | Open Settings; first press shows a notice or selects top Kill All |
| MENU | Open Settings |
| BACK | Cancel or dismiss |

When both top controls are visible, Kill All is left and Settings is right; UP focuses Settings first. Hidden apps are excluded from Kill All. Protected apps are always skipped by Kill All; individual protection has its own switch.

## Presets

| Preset | Starting appearance |
| --- | --- |
| Default | Reset appearance, keeping app lists and closing/service choices |
| Clean | Centered, app names, Normal padding, app-row Kill All; header/gear/help/details off |
| Clean+ | Centered icons, Compact padding; no labels/header/gear/help; Kill All hidden |
| Classic | Side layout, names/guidance, app-row Kill All |

All start with Large circular cards, Green, 80% Black dim, Minimal close hint and smooth motion. Default/Classic details appear only with privileged access. The first applied preset saves the previous appearance across further presets and app restarts. Restore consumes that checkpoint; full Reset also clears it and app lists.

## Build

Use JDK 17 and Android SDK platform/build-tools 35. Keys and machine-specific Java/SDK paths are not committed.

```powershell
./build-local.ps1 -JdkPath C:/path/to/jdk17 -SdkPath C:/path/to/android-sdk -Variant Debug
./build-local.ps1 -JdkPath C:/path/to/jdk17 -SdkPath C:/path/to/android-sdk -Variant Release
```

Alternatively configure JAVA_HOME/local.properties and run gradlew.bat assembleDebug or assembleRelease. Release output is unsigned until signed externally; see [signing](docs/SIGNING.md) and [validation](docs/VALIDATION.md).

## Attribution and status

Based on [Qutaiba-Khader/RecentAppsTV](https://github.com/Qutaiba-Khader/RecentAppsTV), with original attribution retained and no upstream endorsement claimed. The upstream repository had no declared license when checked. This is an unofficial bugfix fork with limited planned maintenance. Upstream licensing has not been clarified; no permission from the upstream author or blanket license for their code is asserted. See [attribution](UPSTREAM.md) and [dependency notices](THIRD_PARTY_NOTICES.md).

No screenshots, device logs, personal profiles or installed-app inventories are included. Screenshots can be added later after privacy review.
