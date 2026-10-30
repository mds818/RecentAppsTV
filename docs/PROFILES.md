# Saved profiles

Open Appearance presets → My profiles · Import / Export.

Save the current preferences with a name, then apply, rename, export or delete a saved profile. Undo last profile change restores the preferences from immediately before the latest profile application. Applying another profile replaces that checkpoint. Saved profiles survive Reset to defaults; deleting a saved profile is a separate confirmed action.

JSON import/export uses Android’s document picker. No storage permission or background service is added. A TV without a compatible document picker needs a document-provider/file-manager app for file transfer; saved profiles still work locally.

Profiles contain settings only. Hidden/protected app lists, dismissed history, permissions, private device state and the saved-profile library are excluded. Edge action menu choices are included.

## Compatibility

A document identifies itself as RecentAppsTV-profile, contains a name and a settings object. Missing settings leave the current preferences unchanged. Unknown keys, wrong types and unknown enum choices are skipped and counted in the review dialog. Explicit null means use the installed app’s default for that setting. Import never grants permissions or changes signing identity.

Primitive keys register their type when declared in SettingsStore.Keys. Enum choices are discovered through the normal enumPreference readers. Future settings do not need per-version profile converters. Old profiles can therefore apply the subset that the installed version understands. File size is limited to 64 KiB; there can be up to 30 saved profiles.

## Detailed review

Selecting a saved profile opens its review directly. **View changes** shows effective differences; **All profile settings** opens on All, with Changes and Compatibility filters. Readable names, app sections and current → profile values distinguish changes, unchanged options, missing values kept, and unsupported values skipped. OK on a row opens complete technical Details. The same browser is used for imported files. Reviewing does not apply settings, and reports remain local. Export, Rename and Delete are grouped under Manage profile.

## Protection feedback

Apps → Protection → Protection feedback: Off, Hint only, Animation only, or Animation + hint. A blocked DOWN action shows Protected from closing or Protected from dismissal. Animation only and Animation + hint add a short restrained nudge; Smooth motion off suppresses the movement. Allowed dismissals retain the normal dismissal animation.
