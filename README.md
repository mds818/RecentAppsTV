# RecentAppsTV (Fork)

**An Android TV app switcher you can make as clean or informative as you like.**

Choose a starting look, then adjust the details to suit your remote and your TV. Use it with **Usage Access alone**, or optionally connect **Porter / Shizuku** for actual task switching and closing.

[**Download APK**](https://github.com/mds818/RecentAppsTV/releases/latest) · [**Watch the showcase**](#see-it-in-use) · [Getting started](#getting-started)

## Choose your everyday look

Four presets, shown with the same selected app and background. Click a picture to view it at full size.

<table>
  <tr>
    <td width="50%">
      <strong>Default</strong><br>
      <a href="docs/images/default.png"><img src="docs/images/default.png" alt="Default preset with app names, details, header and remote guidance" width="480"></a><br>
      Names, details and guidance for an informative view.
    </td>
    <td width="50%">
      <strong>Clean</strong><br>
      <a href="docs/images/clean.png"><img src="docs/images/clean.png" alt="Clean preset with centered cards and app names" width="480"></a><br>
      Centered cards and app names, with fewer distractions.
    </td>
  </tr>
  <tr>
    <td width="50%">
      <strong>Clean+</strong><br>
      <a href="docs/images/clean-plus.png"><img src="docs/images/clean-plus.png" alt="Clean Plus preset with larger icons and no app names, header or Kill All control" width="480"></a><br>
      An icon-only view with compact padding and hidden controls.
    </td>
    <td width="50%">
      <strong>Classic</strong><br>
      <a href="docs/images/classic.png"><img src="docs/images/classic.png" alt="Classic preset with side layout, app names, details and guidance" width="480"></a><br>
      A side layout with names, details and remote guidance.
    </td>
  </tr>
</table>

## See it in use

https://github.com/user-attachments/assets/eb2a81f1-0f20-44a3-8ce1-12d05283680a

Browse in all four presets, then close an app from the middle of the row. The closing portion is labelled **0.5× slow motion** so the short animation is easier to see. [Download video](https://github.com/mds818/RecentAppsTV/releases/download/v1.0.0/RecentAppsTV-showcase.mp4).

Presets are starting points. Mix and match the settings afterward. Your previous appearance is saved when you first apply a preset and stays available through further preset changes and app restarts. **Restore previous appearance** brings it back. App lists and service/closing preferences are kept when applying presets.

## Works without Porter or Shizuku

Porter/Shizuku is **optional**. Everyday app browsing and launching can use Android's Usage Access permission, without root, USB debugging or wireless debugging.

| Mode | What you see | What you can do |
| --- | --- | --- |
| **Usage Access only** | Recently used apps, ordered by foreground usage | Browse and launch apps. History does not mean an app is running. |
| **Porter / Shizuku authorized** | Actual recent Android tasks | Switch tasks, close them and optionally force-stop apps. |

The app automatically switches between the available modes. You can also turn privileged access off in Settings. Closing controls are hidden in usage-history mode.

## Customize the details

Keep only the information and controls you want to see.

| Area | Your choices |
| --- | --- |
| **Layout and cards** | Centered or side selection; older apps to the left or right; card size, square/rounded/circle shape and padding. |
| **Colors and background** | Accent presets and an expanded color chart; background dim level and tint. |
| **Information** | App names, header and remote hints independently on/off; app details off, always on, or only with privileged access. |
| **Navigation and motion** | Show or hide the Settings button; elevated or minimal close hint; smooth motion or immediate movement. Hidden Settings remains available with UP twice. |
| **Closing controls** | Kill All with the apps, in the top controls, or hidden; optional confirmation; optional force-stop. |
| **App lists and protection** | Hide apps, protect apps from Kill All, optionally protect individual closes, and choose whether hidden apps are excluded from Kill All. |

Protected apps are always skipped by Kill All. Hidden apps are also skipped by default; turn off **Exclude hidden apps from Kill All** to include them. Closing features require authorized Porter/Shizuku access.

## App actions

Hold OK for app actions, or press DOWN twice to deliberately close a task when privileged access is available.

<table>
  <tr>
    <td width="50%">
      <strong>Long-press menu</strong><br>
      <a href="docs/images/context-menu.png"><img src="docs/images/context-menu.png" alt="Long-press menu with hide, close, protect and app-info actions" width="480"></a>
    </td>
    <td width="50%">
      <strong>Close with a second press</strong><br>
      <a href="docs/images/close-hint.png"><img src="docs/images/close-hint.png" alt="A downward preview and Press again to close hint beneath the selected app" width="480"></a>
    </td>
  </tr>
</table>

<details>
<summary><strong>See the settings and color chart</strong></summary>

![Organized settings categories](docs/images/settings.png)

![Accent presets and expanded color chart](docs/images/accent-colors.png)

### Settings tour

https://github.com/user-attachments/assets/2ce0d35b-b7cd-4480-8857-4062d9a3311c

The context menu, close hint, preset picker and color chart. [Download video](https://github.com/mds818/RecentAppsTV/releases/download/v1.0.0/RecentAppsTV-settings-tour.mp4).

</details>

## Getting started

Android 7.0+ (API 24). Tested on a Xiaomi TV Box running Android 14.

1. Install the APK from [Releases](https://github.com/mds818/RecentAppsTV/releases/latest).
2. Open **Recent Apps** and allow **Usage Access** for recently used app history.
3. Optionally start [Porter](https://porter.darken.eu/setup) or [Shizuku](https://shizuku.rikka.app/guide/setup/) and grant access for task management.
4. Choose an appearance preset, or adjust the settings individually.

No ads, analytics or app account required.

For a remote-button shortcut, use an external remapping app with this component:

```text
com.websnake.recentstv/.RecentsActivity
```

If an existing installation uses a different signing key, Android may require uninstalling it before installing this APK. Uninstalling clears its settings; see [signing and updates](docs/SIGNING.md).

## Remote controls

| Button | Action |
| --- | --- |
| LEFT / RIGHT | Browse apps |
| OK | Open the selected app or control |
| Hold OK | Open the app context menu |
| DOWN twice | Close the selected task when privileged access permits |
| UP | Cancel a close preview or move to the top controls |
| UP twice with Settings hidden | Open Settings |
| MENU | Open Settings |
| BACK | Cancel or dismiss |

When both top controls are visible, Kill All is on the left and Settings is on the right. UP focuses Settings first.

<details>
<summary><strong>Build from source</strong></summary>

Use JDK 17 and Android SDK platform/build-tools 35. Machine-specific Java/SDK paths and signing keys are not committed.

```powershell
./build-local.ps1 -JdkPath C:/path/to/jdk17 -SdkPath C:/path/to/android-sdk -Variant Debug
./build-local.ps1 -JdkPath C:/path/to/jdk17 -SdkPath C:/path/to/android-sdk -Variant Release
```

Alternatively configure JAVA_HOME/local.properties and run `gradlew.bat assembleDebug` or `assembleRelease`. Release output needs external signing. See [signing](docs/SIGNING.md) and [validation](docs/VALIDATION.md).

</details>

## Credits

Based on [Qutaiba-Khader/RecentAppsTV](https://github.com/Qutaiba-Khader/RecentAppsTV). See [project attribution](UPSTREAM.md) and [dependency notices](THIRD_PARTY_NOTICES.md).
