# Presets and saved profiles

Choose a preset and review it before applying. Full preset (recommended) starts with the same defaults as Reset to defaults, then applies the preset overrides. This prevents old action choices leaking into a different preset. Appearance only changes visual settings and button layout while retaining action preferences, including edge actions, DOWN, bulk actions and dismissal.

View changes shows grouped current → new values from the effective configuration. Reviewing or cancelling does not write preferences. The preview and application share the same candidate-building code.

Both modes preserve hidden/protected app lists, dismissal history, saved profiles, privileged-access preference, safety/protection confirmations and update preferences. Android permissions are unchanged. Presets do not enable privileged access or start its manager.

Undo last preset restores the configuration immediately before the most recent application, not the factory defaults. Each application replaces that single restore point. Earlier appearance-only restore points remain supported. My profiles remains available for multiple saved configurations and import/export.

Implementation: registered preferences automatically join full presets, apart from a small personal/safety exclusion list. Preset-specific values remain short overrides; no complete settings document is maintained for every built-in preset. Visual-only selection reuses the appearance-key group with action preferences excluded. The review compares effective values, so legacy/default fallbacks are resolved before display. New fields receive readable fallback labels in the review.

## Saved and imported profiles

Apply opens a review with View changes and All profile settings. View changes shows only effective differences, grouped into appearance, actions, access/updates and protection/confirmation settings. All profile settings retains the per-option compatibility report, including missing and skipped values. Missing settings are kept, unsupported values are skipped, and explicit default values are resolved before preview. Previewing never applies settings. The saved/imported profile path keeps its existing profile Undo checkpoint.

The preset review precomputes both apply modes from a single settings snapshot. Mode switches use those cached results, without clearing the count or briefly replacing the View changes label with loading text. The final transaction still derives the configuration from current stored settings.

Selecting a saved profile opens its review directly. Manage profile groups Export, Rename and Delete separately from reviewing/applying, and BACK returns to the same review. Deleting still asks for confirmation and leaves current settings unchanged.

On a built-in preset, OK on either apply-mode card moves focus to Apply; a separate OK is required to commit. Left/Right selects Full preset or Appearance only while the mode cards or View changes row are selected, and inside the change list. Cached results keep switches immediate.

## All profile settings

Saved and imported profiles share the same browser, opening on All. Groups follow Appearance, Controls, Apps, Advanced and Updates; legacy and unsupported options have a Compatibility group. Changes shows effective differences; Compatibility includes missing, unsupported, partly supported and legacy entries. Missing values are kept, not treated as failures.

Rows show current and effective resulting values, with explicit status text as well as colour. OK opens Details with the full key, current/result values, original profile value and compatibility explanation. Large technical values are split into bounded scrollable blocks; the original values are retained. BACK returns through Details, the list and the apply review without applying anything.

The same validated candidate used by the profile application is decoded for preview. Per-key effective contributions distinguish real changes from legacy choices overridden by newer preferences. This is presentation logic, not an import conversion or a profile-format migration. The JSON format remains unchanged and future missing/unknown entries remain supported.
