# Changelog

## 1.0.1 · Build 19 — 2026-10-29

- Detect higher build numbers within the same app version using a small release metadata asset. Keep manual downloads and Android-confirmed installation, APK integrity/signature checks and optional once-per-process checking.
- Organize Apps into App visibility, Kill All and Protection. Keep App exceptions available in both modes; show gray requirement text with green/amber information icons reflecting actual privileged access.
- Improve settings scrolling with directional comfort zones, natural list ends and restored submenu positions. Respect Smooth motion.
- Refine About information, use consistent update toggles, separate preset Restore, and simplify section headings.
- Add optional Remote button setup under About.
- Users of Build 18 must install this build manually once to enable same-version build detection. Settings are retained with the established signing key.

## 1.0.1 — 2026-10-28

- Add About links and an Updates submenu: optional once-per-process startup check, optional in-app update notice, manual checking, verified downloads and Android-confirmed installation. Allow automatic installation location for adopted USB storage; actual Shield USB testing remains pending.
- Group Apps settings into Closing, Protection and Visibility. Keep hidden-app Kill All exclusion immediately beneath App exceptions.

- Add Close animation speed: Normal (240 ms, default) or Slow (350 ms) for recording. Fade starts after 40% of the accelerating drop. Keep the close-preview offset through dismissal to avoid an upward twitch; row movement starts after the drop. Motion Off remains immediate.

- Add independent app-text placement: under the selected app or at the bottom of the screen. Names and details move together, with separate space for control hints. In Side selection mode, text stays horizontally centered even at the first and last app.
- Classic uses Medium rounded cards, 60% Charcoal dimming, bottom text, wide padding, elevated close hint and visible settings button and hidden control hints.
- Default/Clean keep text beneath the selected app; Clean+ keeps labels hidden.


## 1.0.0

- Add “Exclude hidden apps from Kill All”, enabled by default; protected apps remain excluded regardless.
- Standardize project naming as RecentAppsTV (Fork), keeping the repository, app label and launcher identity.
- Keep existing long-press timing and release-before-select protection.
- Fix Linux build setup and Unix wrapper line endings.


## Development history

- Optional Porter/Shizuku with automatic UsageStats fallback and permission recovery.
- Preserved application ID, TV launcher, navigation and task actions.
- Expanded appearance/settings, protection, hidden exceptions and safe closing controls.
- Default, Clean, Clean+ and Classic presets with persistent appearance restore; Clean keeps Normal padding.
- About identifies the fork repository and retains original-project attribution.
- Dedicated signing identity; public source excludes keys, device logs and screenshots.

The selected v14 overlay appearance and task logic are retained. This is an unofficial fork; upstream licensing remains unclarified. Built-in remapping, an updater and named custom profiles are not included.
