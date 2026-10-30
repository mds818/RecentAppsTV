## Local Build 45

- Restored cards rise at their final screen position while the existing row makes room, correcting sideways drift on left-side insertions.
- Add Allow dismissing protected apps under Protection (on by default). Closing remains protected; eligible DOWN actions can dismiss instead. Off skips protected apps in individual and bulk dismissal and removes context dismissal for them.
- Top-button visibility toggles consistently use Show only after pressing UP.

# Shared action-animation speed — Build 44

## Local 1.0.1 · Build 44

- Replace close-only speed with shared Animation speed: Fast / Normal / Slow, default Normal.
- Apply the same timing policy to individual close/dismiss, Restore insertion, row reflow, view fade transitions and bulk flights/stagger. Main-thread completion waits use the same durations as their UI animations.
- Keep focus highlights, navigation scrolling, button feedback, menu feedback and confirmation/message timeouts responsive and unchanged. Smooth motion off disables the action animations.
- Migrate the old Normal/Slow close setting to the corresponding global choice. Keep global speed selected while comparing presets; explicit Reset to defaults returns Normal.

# Adaptive app-menu Restore — Build 43

## Local 1.0.1 · Build 43

- Add Restore in app menu: Off / On / Adaptive (default), under Dismissal. The action is last, below a divider, and appears only when dismissed entries exist.
- Adaptive checks direct Restore reachability in the current overlay: top Restore counts even when hidden until UP; an active edge menu containing Restore or a direct edge Restore also counts. The Settings page and a future empty-list recovery route do not suppress the shortcut.
- Reuse the existing count/icon confirmation and restore animation. Keep the longest context menu compact enough for TV use.
- Remember switched view is on in fresh defaults and all presets. Explicit existing choices are preserved until a preset is applied; preset undo captures the prior Remember value.

# Restore insertion animation and settings home — Build 42

## Local 1.0.1 · Build 42

- Remove press-count text from control hints; the first-press prompt still explains confirmation where needed.
- Use Hide top button until UP / Hide Restore until UP consistently, preserving stored hide flags.
- Put Presets first, followed by Appearance, Controls, Apps, Advanced, About and Reset, with subtle dividers before customization, support and reset groups.
- Restore refreshes the existing carousel in place. Existing cards move into their new slots while returned cards fade/scale/rise into their sorted positions. Smooth motion off makes restoration immediate; an empty list starts at final centered geometry.
- All profiles and fresh defaults use Actions menu recovery and Recent-only top Restore. Clean+ uses Subtle view-change messages. Explicit existing preferences remain retained until changed or a preset is reapplied.

# Smarter recovery and preset undo — Build 41

## Local 1.0.1 · Build 41

- Return control hints to a single neutral line.
- Hide empty bulk operations in normal menus. Recovery uses configured Switch/Settings items plus Restore, never Close all or Dismiss all.
- Add count/icon strips to close, dismiss and restore confirmations, with protected apps separated from close candidates. Previews are bounded to eight icons plus a remaining count.
- Fix app-card highlight while top Restore is selected; pin the hidden-settings hint vertically so top-button reveal does not shift it.
- Rename hide/reveal toggles to Always show top button (Off reveals on UP), preserving the saved hide behavior.
- Add Restore top placement Off / All views / Recent only.
- Make Undo last preset a one-step backup immediately before each preset, including manually edited preset-managed values.
- All presets use Adaptive. Restore is in top controls for all: always visible for Default/Classic, revealed on UP for Clean/Clean+. Clean+ uses privileged Close all at top, hidden until UP.

# Adaptive activation and empty-list recovery — Build 40

## Local 1.0.1 · Build 40

- Put Off on the left for both edge-action modes, without changing saved enum values.
- Move DOWN behavior and close-hint style into a dedicated submenu with shorter mode labels.
- Add Adaptive edge activation: one press with an actual Bulk/Switch tile in the app row, otherwise two.
- Add optional top Restore button with independent reveal-on-UP visibility and a dedicated restore-history icon shared by the menu.
- Add an optional edge-action line to control hints with direction, action and press count.
- Add opt-in empty-list recovery: Off / Actions menu / Restore. When dismissed entries leave the active list empty, recovery temporarily uses one press; Actions menu includes available actions and focuses Restore. Normal settings remain saved. No apps are restarted by restoring entries.

# Dismissal and per-mode edge actions — Build 39

## Local 1.0.1 · Build 39

- Unify dismissal as Off / Recent apps only / Both views; retain stored legacy values and the same scope for individual and bulk dismissal.
- Separate Edge shortcut from View switching, with independent privileged/fallback actions and legacy behavior migration.
- Explain menu item availability with provider-aware info. Working fallback actions are neutral; unavailable dependencies are amber. DOWN fallback does not imply that Porter is required.
- Rename By view to Follow view and explain Tasks closes versus Recent apps dismisses; Dismiss all remains hide-only in the allowed views.
- Add Restore dismissed apps as a default optional edge-menu item with an optional cancel-first confirmation. It appears only when there are dismissed entries.
- Default preset: switch buttons hidden, two-press edge switch, dismissal enabled, fallback DOWN dismiss, privileged DOWN close-or-dismiss, task indicator on. Existing installations retain their current setup until applying Default. Restore previous also captures the action choices changed by Default.

# Settings organization — Build 38

## Local 1.0.1 · Build 38

- Shorten Controls and Apps with View switching, Dismissal and Bulk actions submenus; nest Edge menu contents under View switching.
- BACK restores the parent page and selected entry, including nested menus. Each submenu keeps its own scroll state.
- Hide configuration-dependent extras while preserving their stored values. Service availability does not hide preferences.
- Add a small indent and subtle vertical guide for directly dependent rows such as Reveal on UP, Dismiss in, Edge action and Close speed.
- Keep dismissal restore available even when dismissal is disabled. Keep privileged/fallback action choices independent.

# Edge menu refinement — Build 36

## Local 1.0.1 · Build 36

- Settings is excluded from the default edge menu; explicit saved choices are retained.
- Empty menus show No actions available with back guidance rather than silently adding Settings.
- Refine edge menu with compact rows, action icons, a thin animated focus outline, lighter background dimming and a short fade/scale entrance. Smooth motion off disables those transitions.
- Clarify Dismiss in dependency: Enable dismiss is the master switch for individual dismissal and Dismiss all.

# Dismissal scope and hidden top controls — Build 35

## Local 1.0.1 · Build 35

- Standardize conditional Header labels as Privileged only / Fallback only; show the actual provider in status text.
- Dismiss in defaults to Recent apps only, keeping actual Android tasks visible. Both views is available explicitly. Dismiss actions honor the chosen scope.
- Add independent Reveal top button on UP toggles for Bulk and Switch.
- Move edge menu contents into a submenu; pressing toward the app row closes the menu. Menu appears on the entered side.
- Explain Close or dismiss fallback in its description and status indicator.
- Clean enables Show task indicator. Applying/restoring presets includes task indicator and top visibility preferences.

Tasks are Android task entries, not proof of a live process. No new polling or background logging is added. Recent-only dismissal uses existing local timestamps and does not filter the Tasks list; dismissal actions are unavailable in Tasks unless Both views is selected.

# Configurable view and edge actions — Build 34

View-change notifications are immutable events containing their destination label, rendered independently of carousel layout. They retain the same text throughout their lifetime and fade-out. Header visibility offers Off, Always, With access, or Without access.

Bulk action with privileged access adds By view: Close all in Tasks; Dismiss all in Recent apps. Dismiss still requires opt-in; otherwise the recent-view bulk button is hidden. The without-access choice remains independent.

Edge action offers Switch view, Close or dismiss all, Dismiss all, or Menu. The existing Off / One press / Two presses gesture and Start / End / Both placement apply to each action. Menu contents are configurable; unavailable actions are omitted. An empty menu retains Settings as a safe escape. Explicit edge close requests never turn into dismissal if access disappears during confirmation. Existing protection, hidden-app exclusions and confirmation preferences apply.

Clean now hides switch buttons, shows its header, and uses a two-press edge gesture. Applying it retains a restorable snapshot including header and edge-gesture preferences. Existing preferences migrate without enabling dismissal.

Earlier build notes below are historical where they differ.

# Switch visuals — Build 33

Only app cards use a short vertical fade/scale transition. Controls remain unchanged. Alpha uses ModulateAlpha with clipping disabled so enlarged selected-card outlines/glows remain circular during the transition. Smooth motion disables the effect. View-change message offers Off (default), Subtle (1.6 seconds), Prominent (larger text plus icon, 2.4 seconds). Labels are simply Tasks or Recent apps. Legacy boolean On maps to Subtle and Off to Off. Prominent sits slightly higher to avoid compact empty-state text. No action/gesture behavior changed. This build uses a focused visual check rather than repeating the full button matrix.

# Stable switching and placement — Build 32

Top buttons use equal spacing. Manual source switches fade app-card content only: action tiles, labels, header and other information no longer share the row fade/scale. A source revision snaps the carousel and slot layout to its new target instead of tweening between source layouts; ordinary browsing/removal motion remains unchanged. Focused controls keep their identity. App labels and current-source text update in place as required by the new selection.

Bulk button location offers App row / Top controls / Both. Row switch position offers Start / End / Both. Each row switch has a unique side identity, so Start and End independently retain focus across lists of different lengths. Both edge shortcuts work independently, including a single-item/empty row; a two-press confirmation remains tied to the chosen side.

The edge hint uses final layout coordinates, not the scrolling animation value. Its last visible position/text/width are retained during exit, so it fades where it appeared instead of following another selection. Hints hide during source loading. Current controls, permissions, fallback behavior, protection and Remember preferences otherwise remain unchanged.

Earlier build notes below are historical where they differ.

# View switching — Build 31

Controls groups view settings together:
- Switch buttons: Top controls (default), App row, Both, Hidden.
- Edge shortcut: Off (default), One press, Two presses.
- Row switch position: Start (default, beside newest apps), End (beside oldest).
- Remember switched view: Off (default), On.
- Show view-change message: Off (default), On.

Row tiles and edge shortcuts follow the logical row position: Start is physically right when older apps appear left, and left when older apps appear right. End reverses that. Top controls retain ordinary arrow navigation. Two presses arms a three-second hint; only a fresh second press switches, and holding the key cannot arm or trigger repeats. The indicator fades, has no filled frame, and uses a subtle accent shadow without a perpetual animation.

Manual switching keeps the overlay/header/top controls mounted while loading. Only the row and its labels fade/scale slightly, using Smooth motion; with motion off the row swaps directly. Top-button, row-tile and edge focus are restored to the same control/edge in the new list. Empty lists keep the switch accessible and use compact text when a row tile is present. Optional view-change text is an in-app, short-lived message, not an Android notification. Edge/message jobs are cancelled on pause; no recurring timer, background service or permission was added.

Legacy Build 30 switch modes migrate to independent button placement and edge choices. Existing explicit one-press edge behavior is retained. Appearance presets affect button placement and row position, while remembering and edge interaction preferences remain operational choices. Clean+ hides buttons but need not disable an explicitly configured edge gesture.

The following notes describe earlier iterations and are superseded where they differ.

# View controls — Build 30

Controls → View switch offers Button (default), Right edge, Both, Hidden. With privileged access, the button sits between top Bulk and Settings controls, separated from Bulk by extra spacing. Its focused label names the destination (Tasks or Recent apps), while the header names the current view. UP defaults to Settings when visible. With the gear hidden, a visible view button is the safe top-control focus; another UP opens Settings. The app-row shortcut operates only at the physical right edge, independent of browse direction, including the bulk tile if it occupies that edge. A fresh RIGHT press switches; held-key repeats never trigger the shortcut. A small arrow hint names the destination. Top-row arrows retain normal navigation.

Remember switched view defaults off. Off uses an in-memory override until the next overlay resume; On persists an intentional switch as List source. Automatic fallback never changes the saved source. Both controls disappear without authorized app-enabled privileged access, leaving history fallback available. Requesting Recent apps without Usage Access opens Android permission settings instead of silently failing. Empty views retain a switch control (or RIGHT hint in edge-only mode), including after local dismissal. Clean+ hides switch controls; appearance Restore includes the control mode but leaves Remember unchanged.

# Current list behavior — Build 29

List source has two choices: Android tasks when available, or Recently used. Task preference uses actual tasks with authorized app-enabled Porter/Shizuku access, otherwise Android usage history. History preference uses usage history when allowed and temporarily uses tasks if Usage Access is missing but privileged access is available. Only when both sources are unavailable is the Grant Usage Access screen shown. The visible source switches live using the existing foreground permission watcher and binder listeners. No saved preference is rewritten by fallback. Existing Automatic maps to task preference; existing Android tasks gains automatic fallback. The separate DOWN and Bulk rules follow actual privileged availability independently of the displayed source. A task/history change does not imply that an app process is running.

The following sections include historical build notes; earlier task-only/no-fallback descriptions are superseded by Build 29.

# App lists and dismissal (local Build 24)

Defaults retain the previous behavior: Automatic list, DOWN closes actual tasks, Dismiss off, task indicator off.

Settings → Apps offers Automatic, Recently used apps and Android tasks. Automatic uses tasks when authorized and usage history otherwise. Recently used apps always uses Usage Access, matching entries against Android tasks when privileged access is available. Task available is an optional badge on the selected history entry; it does not claim the app has a running process. Android tasks explicitly requires enabled, authorized Porter/Shizuku and explains unavailable access rather than silently changing the selected source.

Enable dismiss adds Dismiss from list to long press. It hides an app locally without closing, force-stopping or protecting it. Usage Access is required to detect a later foreground use. The tiny local record stores the package and last observed foreground timestamp. Android's existing foreground events are read when the list opens or an app is dismissed; no new background logger, service or timer is added. Reappearance happens on the next list load after newer use is reported. Device reporting delays may require reopening the overlay. Restore dismissed apps clears only these temporary records. App exceptions and protected apps are untouched. Turning Dismiss off reveals the list without losing the saved records.

Controls → DOWN action has two independent saved choices. With privileged access: Close only / Close or dismiss / Dismiss only / Off. Without privileged access: Dismiss / Off. The app automatically selects the appropriate rule as authorized app-enabled access changes. Close or dismiss closes matching tasks and dismisses unmatched entries, but never converts a blocked protected-task close into dismissal. The Enable dismiss master switch gates every local dismissal, including these actions and long press. With it off, Close or dismiss becomes close-only and Dismiss only does nothing; saved choices remain editable. The old Automatic selector and Dismiss fallback switch are removed from the UI.

Close task and Kill All act only on actual Android tasks. In usage history, a closed task's app may still appear after a reload because closing a task does not delete usage history. Its Task available badge disappears when no task matches. Launching an unmatched history entry uses its normal launch intent. Without privileged access, task-closing and protection actions are absent from the long-press menu.

## Bulk actions and contextual information

Apps → Bulk actions has independent rules: With privileged access = Close all / Dismiss all / Hidden; Without privileged access = Dismiss all / Hidden. The app switches to the applicable saved rule automatically. Button location = App row / Top controls and applies to both modes. Hidden belongs to each action rule, not the location selector. Close all acts on actual tasks and respects hidden-app exclusion and protection. Dismiss all only hides the visible entries and requires Enable dismiss plus Usage Access; it does not close tasks or exempt them from later closing. Restore dismissed apps brings entries back. Confirmation = Always / Close only / Off. Dialogs default to Cancel and retain action-specific wording. An availability change cancels the operation instead of changing its meaning.

Long-press actions are ordered Dismiss, Close task, Protect/Unprotect, Hide app (App exceptions), App info; unavailable operations are omitted. App category comes from Android package metadata in both sources. Privileged only shows it while privileged access is active, including usage history. The repeated usage-history warning under each app has been removed; task presence remains an independent optional badge.

Information icons explain the current effect of the selected option. Porter/Usage Access/Enable dismiss requirements use amber when missing; intentional dependencies such as DOWN Off, Smooth motion Off, the wrong list for task badges, hidden bulk controls, or disabled names/details use neutral explanations. Choices stay editable and saved. Informational text remains gray; only its icon is colored. The list rechecks focused-row visibility when changing an option adds an explanation.

## Build 24 controls and migration

DOWN now offers Automatic / Dismiss / Off. Enable dismiss controls the local feature, while Dismiss fallback independently controls whether Automatic DOWN may hide entries while privileged access is inactive. The fallback switch appears only while Enable dismiss is on, and is off for a new configuration. Explicit Dismiss and long press do not depend on that fallback switch. Legacy Close task migrates to Automatic with fallback off; a legacy explicitly selected Automatic retains its fallback switch preference. Migration uses stored enum names, not UI positions.

Bulk confirmation offers Always (new default), Close only and Off. Close only confirms actual closing but immediately performs local bulk dismissal. The previous confirmation boolean maps to Always or Off, preserving the old choice.

Automatic list, DOWN and bulk choices show a green information icon with Porter active, amber with Porter inactive, and text naming the current source/action or why there is no action. Amber can describe a functioning fallback rather than an error. App details, hidden-app close exclusion and both protection rows have individual information lines; the Protection heading carries no duplicated status. About and Updates have no contextual bottom information bar; descriptions and update status remain in their existing rows/header.

When adding a setting that depends on access, list source, another switch or current action, extend SettingsEffectInfo.kt with a per-row explanation. Do not depend on a section heading to convey availability. Keep information text gray and color only the icon; never silently rewrite or disable a user's saved choice. Regression tests cover the required per-row status and Automatic color semantics.

## Build 25 provider status and Automatic DOWN

All per-setting dependency statuses use the detected manager name from ShizukuManager.managerName: a connected Porter backend takes priority, otherwise a connected Shizuku service, otherwise the installed manager offered for startup. The same name is passed to every settings effect explanation. Status text consistently uses “Porter · Active” / “Porter · Not active” (or Shizuku). Inactive means unavailable to this app, including app-local access disabled.

Automatic DOWN is service-based: with privileged access it closes matching Android tasks only, respecting protection; an unmatched history entry is unchanged even with Dismiss fallback enabled. Without privileged access, Enable dismiss plus Dismiss fallback allows local hiding. Explicit Dismiss still works with or without privileged access. This is a deliberate refinement of the earlier unmatched-task fallback. Android tasks do not prove a process is currently running.

## Build 26 independent access rules

Both DOWN choices are adjacent under Controls → DOWN action. Inactive-mode information says the choice is saved for when that mode applies. Enable dismiss remains under Apps → Dismiss from list. No extra timers or background collection are added.

New keys down_with_access and down_without_access override legacy choices independently. Existing Build 25 Automatic migrates to Close only with access and Dismiss/Off without access according to its fallback preference. Existing Dismiss migrates to Dismiss in both modes, and Off to Off in both. New configurations retain Close only with access, Off without access, and dismissal disabled. Changing either new rule never rewrites the other. Historical build notes below describe earlier UI iterations, not current controls.

## Build 27 independent bulk rules

New bulk_with_access and bulk_without_access keys take precedence over legacy bulk_action. Close tasks migrates to Close all / Hidden; Dismiss to Dismiss all / Dismiss all; Automatic to Close all / Dismiss all. Legacy hidden placement and Clean+ hide both modes. The old hidden location remains readable internally for appearance snapshots, but never appears as a placement choice. Editing a bulk rule or placement first stores both displayed rules and clears that legacy visibility mask. This prevents changing placement from accidentally revealing a hidden button or enabling the other access mode. Appearance presets still hide/show controls without overwriting saved operational action choices, and Restore appearance remains available.

Both mode choices remain editable. Active-mode captions explain the actual action or missing dismissal requirements; the other mode states that it is saved for later. Placement and confirmation rows explain when the button is hidden. No polling or permissions were added.

## Build 28 dependency explanations

Android tasks explicitly warns that losing privileged access stops the list instead of falling back. Both without-access action rows explain when this task-only source prevents their use. Automatic and Recently used explain their different list behavior. With-access DOWN dismissal warns when Usage Access is missing, including the partial Close or dismiss behavior where matching tasks can still close. Protected apps keeps its count and states that protection prevents closing, not local hiding. No action, preference default or fallback behavior changed.
