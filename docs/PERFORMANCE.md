# Local Build 50: startup performance

Measurements on the Xiaomi TV box, Android 14. Each configuration used two force-stopped process starts and four repeated Activity openings. Automatic update checks were temporarily disabled in both runs to avoid network variation. These are small local samples, not results for every TV. The timed interval starts when the Activity is constructed and ends at a Compose frame with the loaded overlay; it excludes earlier process/provider initialization.

| Repeated opening | Before | After | Reduction |
| --- | ---: | ---: | ---: |
| Recent apps without privileged access | 508 ms | 354 ms | 30% |
| Recent apps with Porter | 591 ms | 422 ms | 29% |
| Android tasks with Porter | 514 ms | 375 ms | 27% |

Cold results varied; there was no consistent improvement across every configuration. Do not extrapolate these figures to older hardware without measuring it.

## Changes

- Avoid redundant reloads from the initial binder callback and onResume. Binder changes during loading are caught by the final access-state check.
- Keep foreground-event history in process memory. Every opening still queries fresh overlapping events; reconcile the seven-day window at least once per minute when used. No timer, file or background collector is added.
- Invalidate history when Usage Access is denied. Clock discontinuities, expiration and failed queries are handled explicitly.
- Sort timestamps before checking launchability so PackageManager only resolves enough valid entries for the visible list.
- Skip history/marker refresh in task view when dismissal does not apply there, and skip unchanged DataStore transactions.
- Actual task lists are still read fresh. Existing permission polling and service-death protections remain.

Developer timing logs are opt-in via the RecentsPerf DEBUG log tag. They contain stage names and durations, not package names; normal operation does not collect them.

Validation: cache tests cover incremental merging, full reconciliation, permission invalidation including an in-flight query, clock changes/expiry, failed queries and isolated result snapshots. TV checks covered warm-cache dismissal/relaunch, permission revoke/regrant and actual task actions. Original preferences, Usage Access and the developer log property were restored after testing.
