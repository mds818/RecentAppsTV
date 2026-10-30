# 1.1.0 · Build 48

- Choose recent application history or Android tasks, with configurable view switching.
- Configure individual and bulk close/dismiss actions separately for privileged and fallback use; restore dismissed apps.
- Save named profiles, import/export JSON, review compatible/missing/unsupported settings and undo a profile change.
- Refine settings sections, remote navigation, edge actions, protection feedback and action animations.
- Keep Porter / Shizuku optional and retain the original package/component and release signing identity.

## Local Build 48

- Handle native profile/dialog D-pad focus navigation before Android's window-level focus sound path. Keep text editing, scrolling and focus behavior; do not mute system audio.

## Local Build 47

- Consume protection feedback once so returning from a menu cannot replay an old nudge.
- Add Animation only feedback and group protection options in their own submenu.
- Silence profile navigation and replace legacy dialogs with styled TV dialogs.
- Add an all-settings profile report with values, missing keys, skipped entries and reasons.

## Local Build 46

- Add named local profiles with save, apply, rename, delete, JSON import/export and one-step undo.
- Preview compatible/missing/skipped settings before import; preserve missing values and skip unsupported values without version migrations.
- Add configurable protection feedback for blocked DOWN actions. Smooth motion off suppresses the nudge.

## Local Build 45

- Restored cards rise at their final screen position while the existing row makes room, correcting sideways drift on left-side insertions.
- Add Allow dismissing protected apps under Protection (on by default). Closing remains protected; eligible DOWN actions can dismiss instead. Off skips protected apps in individual and bulk dismissal and removes context dismissal for them.
- Top-button visibility toggles consistently use Show only after pressing UP.

# Changelog

## Local 1.0.1 · Build 44

- Replace close-only speed with shared Animation speed: Fast / Normal / Slow, default Normal.
- Apply the same timing policy to individual close/dismiss, Restore insertion, row reflow, view fade transitions and bulk flights/stagger. Main-thread completion waits use the same durations as their UI animations.
- Keep focus highlights, navigation scrolling, button feedback, menu feedback and confirmation/message timeouts responsive and unchanged. Smooth motion off disables the action animations.
- Migrate the old Normal/Slow close setting to the corresponding global choice. Keep global speed selected while comparing presets; explicit Reset to defaults returns Normal.


## Local 1.0.1 · Build 43

- Add Restore in app menu: Off / On / Adaptive (default), under Dismissal. The action is last, below a divider, and appears only when dismissed entries exist.
- Adaptive checks direct Restore reachability in the current overlay: top Restore counts even when hidden until UP; an active edge menu containing Restore or a direct edge Restore also counts. The Settings page and a future empty-list recovery route do not suppress the shortcut.
- Reuse the existing count/icon confirmation and restore animation. Keep the longest context menu compact enough for TV use.
- Remember switched view is on in fresh defaults and all presets. Explicit existing choices are preserved until a preset is applied; preset undo captures the prior Remember value.


## Local 1.0.1 · Build 42

- Remove press-count text from control hints; the first-press prompt still explains confirmation where needed.
- Use Hide top button until UP / Hide Restore until UP consistently, preserving stored hide flags.
- Put Presets first, followed by Appearance, Controls, Apps, Advanced, About and Reset, with subtle dividers before customization, support and reset groups.
- Restore refreshes the existing carousel in place. Existing cards move into their new slots while returned cards fade/scale/rise into their sorted positions. Smooth motion off makes restoration immediate; an empty list starts at final centered geometry.
- All profiles and fresh defaults use Actions menu recovery and Recent-only top Restore. Clean+ uses Subtle view-change messages. Explicit existing preferences remain retained until changed or a preset is reapplied.


## Local 1.0.1 · Build 41

- Return control hints to a single neutral line.
- Hide empty bulk operations in normal menus. Recovery uses configured Switch/Settings items plus Restore, never Close all or Dismiss all.
- Add count/icon strips to close, dismiss and restore confirmations, with protected apps separated from close candidates. Previews are bounded to eight icons plus a remaining count.
- Fix app-card highlight while top Restore is selected; pin the hidden-settings hint vertically so top-button reveal does not shift it.
- Rename hide/reveal toggles to Always show top button (Off reveals on UP), preserving the saved hide behavior.
- Add Restore top placement Off / All views / Recent only.
- Make Undo last preset a one-step backup immediately before each preset, including manually edited preset-managed values.
- All presets use Adaptive. Restore is in top controls for all: always visible for Default/Classic, revealed on UP for Clean/Clean+. Clean+ uses privileged Close all at top, hidden until UP.


## Local 1.0.1 · Build 40

- Put Off on the left for both edge-action modes, without changing saved enum values.
- Move DOWN behavior and close-hint style into a dedicated submenu with shorter mode labels.
- Add Adaptive edge activation: one press with an actual Bulk/Switch tile in the app row, otherwise two.
- Add optional top Restore button with independent reveal-on-UP visibility and a dedicated restore-history icon shared by the menu.
- Add an optional edge-action line to control hints with direction, action and press count.
- Add opt-in empty-list recovery: Off / Actions menu / Restore. When dismissed entries leave the active list empty, recovery temporarily uses one press; Actions menu includes available actions and focuses Restore. Normal settings remain saved. No apps are restarted by restoring entries.


## Local 1.0.1 · Build 39

- Unify dismissal as Off / Recent apps only / Both views; retain stored legacy values and the same scope for individual and bulk dismissal.
- Separate Edge shortcut from View switching, with independent privileged/fallback actions and legacy behavior migration.
- Explain menu item availability with provider-aware info. Working fallback actions are neutral; unavailable dependencies are amber. DOWN fallback does not imply that Porter is required.
- Rename By view to Follow view and explain Tasks closes versus Recent apps dismisses; Dismiss all remains hide-only in the allowed views.
- Add Restore dismissed apps as a default optional edge-menu item with an optional cancel-first confirmation. It appears only when there are dismissed entries.
- Default preset: switch buttons hidden, two-press edge switch, dismissal enabled, fallback DOWN dismiss, privileged DOWN close-or-dismiss, task indicator on. Existing installations retain their current setup until applying Default. Restore previous also captures the action choices changed by Default.


## Local 1.0.1 · Build 38

- Shorten Controls and Apps with View switching, Dismissal and Bulk actions submenus; nest Edge menu contents under View switching.
- BACK restores the parent page and selected entry, including nested menus. Each submenu keeps its own scroll state.
- Hide configuration-dependent extras while preserving their stored values. Service availability does not hide preferences.
- Add a small indent and subtle vertical guide for directly dependent rows such as Reveal on UP, Dismiss in, Edge action and Close speed.
- Keep dismissal restore available even when dismissal is disabled. Keep privileged/fallback action choices independent.


## Local 1.0.1 · Build 37

- Add a subtle accent-tinted selected-row fill to the compact edge menu, retaining its thin outline and action icons.
- Show a divider above Settings only when Settings is visible and follows another action. The divider is decoration, not a focusable menu item.
- Preserve row height, menu behavior, Settings-off defaults and Smooth motion behavior.


## Local 1.0.1 · Build 36

- Settings is excluded from the default edge menu; explicit saved choices are retained.
- Empty menus show No actions available with back guidance rather than silently adding Settings.
- Refine edge menu with compact rows, action icons, a thin animated focus outline, lighter background dimming and a short fade/scale entrance. Smooth motion off disables those transitions.
- Clarify Dismiss in dependency: Enable dismiss is the master switch for individual dismissal and Dismiss all.


## Local 1.0.1 · Build 35

- Standardize conditional Header labels as Privileged only / Fallback only; show the actual provider in status text.
- Dismiss in defaults to Recent apps only, keeping actual Android tasks visible. Both views is available explicitly. Dismiss actions honor the chosen scope.
- Add independent Reveal top button on UP toggles for Bulk and Switch.
- Move edge menu contents into a submenu; pressing toward the app row closes the menu. Menu appears on the entered side.
- Explain Close or dismiss fallback in its description and status indicator.
- Clean enables Show task indicator. Applying/restoring presets includes task indicator and top visibility preferences.


## Local 1.0.1 · Build 34

- Fix stale/blank view-change messages with immutable notification events.
- Add conditional header visibility and view-dependent privileged Bulk actions.
- Add configurable edge actions and an optional action menu with capability filtering.
- Update Clean: hidden switch buttons, header on, two-press edge gesture.
- Preserve package identity, signing key, dismissal opt-in and safety checks.


## Local 1.0.1 · Build 24

- Simplify DOWN to Automatic / Dismiss / Off with an independent, opt-in Dismiss fallback. Preserve legacy close-only and explicit Automatic behavior.
- Add Always / Close only / Off bulk confirmations, migrating the old boolean choice.
- Apply per-setting access/status information consistently, including all Automatic modes, app details, hidden-app close exclusion and both protection choices. Remove the duplicated protection-heading warning.
- Remove the About/Updates contextual footer; preserve descriptions and header status.
- Local build only; not published.

## Local 1.0.1 · Build 23

- Add Close tasks / Dismiss / Automatic bulk action with matching labels, icons and cancel-first confirmation. Preserve Close tasks as default and cancel if the action changes while confirming.
- Add per-entry Automatic DOWN action; prioritize immediate actions in the long-press menu.
- Show real app category in both list sources instead of repeated usage-history warnings.
- Explain currently inactive settings through contextual information icons without disabling or changing saved choices.
- Local build only; not published.

## Local 1.0.1 · Build 22

- Add opt-in Dismiss from list with a separate restore action. Local records are cleared when Android reports newer foreground use; no new background collector is added.
- Add DOWN action: Close task, Dismiss or Off. Preserve Close task as default and keep long-press actions available.
- Add Automatic, Recently used apps and Android tasks list sources, optional selected-entry Task available badge, permission states, and safe mapping from history to real task actions.
- Preserve presets, protection, Kill All meaning and package identity. Dismiss and task badges default off.
- Local build only; not published.

## 1.0.1 · Build 20 — 2026-10-29

- Reduce menu top/bottom spacing. About and Updates share a version line and information/status line beneath their title. Remove permanent bottom information strips; preset restore guidance scrolls with content. Both update preference switches fit on entry. Preserve row sizes, remote navigation and comfort scrolling.
- Use compact heading spacing in home, settings sections, presets and installed-app lists.
- Remove the fixed bottom margin; show bottom information only for relevant selected options.

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

## Local Build 25 (1.0.1, unpublished)

- Use the detected Porter/Shizuku name consistently in dependency statuses.
- Automatic DOWN closes matching tasks only while privileged access is active; dismiss fallback applies only without access. Explicit Dismiss remains independent.

## Local Build 26 (1.0.1, unpublished)

- Independent DOWN rules with and without privileged access, selected automatically.
- Four active-access actions and two no-access actions replace Automatic and the fallback switch.
- Preserve prior behavior through migration; retain dismissal opt-in and task protection.

## Local Build 27 (1.0.1, unpublished)

- Separate bulk rules with and without privileged access; automatically switch between saved choices.
- Two placement choices, with visibility controlled independently per mode.
- Preserve legacy bulk behavior, Clean+ visibility, cancel-first confirmation and protection.

## Local Build 28 (1.0.1, unpublished)

- Clarify list fallback and dependencies for per-mode actions.
- Explain missing Usage Access for privileged DOWN dismissal.
- Clarify closing protection versus local dismissal.

## Local Build 29 (1.0.1, unpublished)

- Replace task-only dead end with automatic fallback to an available source.
- Simplify List source to two choices and migrate Automatic to task preference.
- Add a direct Grant Usage Access button, shown only when neither source is available.
- Reuse the existing foreground watcher to restore the preferred source when access returns.

## Local Build 30 (1.0.1, unpublished)

- Optional Button / Right edge / Both / Hidden view controls, default Button.
- Remember switched view, default off; automatic fallback never overwrites preference.
- Safe top-control focus, held-key suppression and switchable empty views.

## Local Build 31 (1.0.1, unpublished)

- Keep the overlay stable while only the app row transitions during switching.
- Independent top/app-row buttons, direction-aware Start/End edge shortcuts and optional two-press confirmation.
- Retain switch focus and offer an optional in-app view-change message.
- Frameless fading edge hints and compact empty-row guidance.

## Local Build 32 (1.0.1, unpublished)

- Equal top-button spacing and card-only, non-sliding source transitions.
- Stationary edge-hint fade-out.
- Both placement for Bulk and both ends of the row switch, with independent focus anchors.

## Local Build 33 (1.0.1, unpublished)

- Fix selected-card clipping during source fades using non-clipping modulated alpha.
- Gentle vertical fade/scale on app cards only; Smooth motion off remains immediate.
- Off / Subtle / Prominent view messages without the View: prefix; legacy On maps to Subtle.
