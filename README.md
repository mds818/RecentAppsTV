# Recent Apps TV

A lightweight **Recent Apps / overview switcher for Android TV & Google TV** — for the
many TV devices that have **no system "recent apps" UI**. It appears as a translucent
**dialog over whatever app is running**, lets you switch tasks, dismiss a single task
(D-pad swipe-to-dismiss), or kill everything — using the **remote / D-pad only**.

Because Android TV restricts the APIs needed to read the task list and remove tasks, the
app uses **[Shizuku](https://shizuku.rikka.app/)** to obtain the elevated permissions at
runtime (no root required).

> Built natively in **Kotlin + Jetpack Compose for TV** from a Claude Design handoff.

---

## Features

- **RTL icon carousel** over the running app — Kill-All tile pinned far-right, most-recent
  app next to it, older apps flowing left; the strip clamps so Kill-All is always visible.
  The focused app's name is shown as a single label centered along the bottom.
- **D-pad only**:
  - **← / →** move focus · **OK** switch to that app · **OK on Kill-All** clears everything · **Back** closes.
  - **Down** arms the kill flow: the card peeks with a *"Press again to remove"* hint; **Down again** flies it off and removes it. **Up** while peeking cancels.
  - **Up** (when not peeking) jumps focus to the **settings gear** (top-right).
- **Long-press OK** on a card → context menu: **Add to exceptions**, **Kill app**, **App info**.
- **Real removal** — removes *every* task for an app (apps can own several) and, optionally,
  **force-stops** it so it can't relaunch. Disabled/uninstalled apps are filtered out.
- **Settings** (D-pad): Show Kill-All, Force-stop on kill, Card size (S/M/L), Card shape
  (Square/Rounded/Circle), **Accent colour** (5 presets), **Background dim** (0–100 % bar),
  **Background colour**, and an **Exceptions** list (per-app hide toggles). Persisted in DataStore.
- **Shizuku gate** when the service isn't running / not granted, and an **Empty** state
  (Settings still reachable).
- **Themeable** — one accent colour drives every focus ring and the Kill-All tile.

## Launching it

There is **no in-app "how to open" trigger** by design. The overlay is launched externally:

- It registers as a **leanback launcher** entry, and
- publishes a **static + dynamic launcher shortcut**.

Bind a remote button to it (via a button-remapper / shortcut app) using the component:

```
com.websnake.recentstv/.RecentsActivity        (action: com.websnake.recentstv.OPEN_RECENTS)
```

Settings: `com.websnake.recentstv/.SettingsActivity` (also via the in-overlay gear, or MENU).

## Requirements

- Android TV / Google TV, **Android 7+ (minSdk 24)**.
- **Shizuku** installed and running, with permission granted to this app.

## How it works (Shizuku)

The privileged work is isolated in `RecentTasksRepository`, run under the Shizuku **shell**
identity (which holds `REMOVE_TASKS`, `MANAGE_ACTIVITY_TASKS`, `FORCE_STOP_PACKAGES`):

| Action  | Mechanism |
|---------|-----------|
| read    | shell `am stack list` / `dumpsys activity recents` (the privileged `getRecentTasks` binder only returns the caller's own task under the shell identity) |
| switch  | `IActivityTaskManager.moveTaskToFront` (reflection) → launch-intent fallback |
| dismiss | `IActivityTaskManager.removeTask(taskId)` for every task of the package (+ optional `forceStopPackage`) |
| kill all| `removeTask` per task (+ optional force-stop) |

`HiddenApiBypass` lifts the hidden-API blocklist; `ShizukuBinderWrapper` routes the calls.

## Architecture

```
com.websnake.recentstv
├─ RecentsActivity        // translucent dialog; scene routing + D-pad handling
├─ SettingsActivity       // leanback-style settings + exceptions sub-screen
├─ RecentsApp             // HiddenApiBypass init + dynamic shortcut
├─ shizuku/ShizukuManager // binder lifecycle + permission flow
├─ data/                  // RecentTasksRepository, PackageRepository, SettingsStore (DataStore)
└─ ui/                    // theme tokens + LocalAccent, overlay carousel, context menu, settings, states
```

**Smoothness notes** (weak TV GPU): animated values are read via `State.value` *inside*
`graphicsLayer{}` (draw phase, no recomposition); the carousel is a single translated layer
(`wrapContentWidth(unbounded=true)` so it sizes to content); focus glow is a `drawBehind`
radial gradient (no `RenderEffect` blur); cards are keyed and `@Immutable`; icons are
downscaled. Ship a **release** build — non-debuggable Compose is dramatically smoother.

## Building

```bash
# Android SDK + JDK 17 required
./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

## Credits

Original design medium: HTML/CSS/JS prototype (Claude Design). Shizuku by RikkaApps.
