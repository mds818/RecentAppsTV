# Local Build 52 performance check

Compared with released Build 51 on the same Xiaomi Android 14 TV. Small sequential samples, not a general device guarantee. No background collector added.

| Measurement (median ms) | Build 51 | Build 52 |
| --- | ---: | ---: |
| View switch, first new frame | 367 | 295 |
| Context menu open | 101 | 84 |
| Context menu close | 54 | 35 |
| Return from Settings, retained row | 382 | 100 |
| Privileged app handoff accepted | 624 | 23 |
| Nonprivileged app handoff accepted | 9 | 10 |

Handoff acceptance is not the target app's loading time. Settings opening itself varied and did not improve reliably (168 vs 193 ms). Returning from Settings keeps content visible while refreshing; actions remain guarded during the refresh.

Cold opening from Activity construction to loaded frame, two process-cold samples per mode: fallback history 868 → 871 ms; privileged history 2033 → 1775 ms; tasks 1423 → 1375 ms. Results vary and exclude earlier process/provider initialization. Repeated openings in this comparison: 326 → 322, 472 → 424, and 378 → 356 ms respectively.

Implementation: correct direct task-move caller package; ordinary launch Intent instead of the slow shell monkey fallback; bounded 2 MiB icon pixel cache with memory/configuration invalidation; cached derived overlay items; retained row on Settings return; 100/180 ms view exit/entry durations before the existing animation speed multiplier. Actual task lists remain freshly queried.

Validation: release assembly, lint and 77 unit tests passed. Device transition checks covered both view directions, context menus, Settings return, and launches with/without privileged access. Preference snapshots and Usage Access restored after each benchmark; temporary helper removed and diagnostic logging reset. No source/API redesign or public upload.
