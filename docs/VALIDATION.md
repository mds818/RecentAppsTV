# Validation

Device: Xiaomi MiTV-AFMU0, Android 14, authorized Porter and Usage Access. Input was primarily ADB/instrumentation; physical-remote and broader-device testing remain useful.

The selected local v14 behavior passed two live Porter stop/start cycles in one app process, including recovery to history/permission explanation and back to actual tasks. Live Usage Access denial/grant, held/repeated OK, empty-state Settings/Exit, presets and appearance restoration were checked.

Controlled disposable-app checks passed real multi-task removal, force-stop On/Off, individual protection On/Off, canceled/confirmed/immediate Kill All, and hidden/protected exclusions. Original non-test tasks remained; fixtures were removed and settings restored. No private package inventories or device logs are included here.

A short Debug/Release comparison used three cold and three process-warm launches per mode plus two 24-key navigation journeys per mode/build. Porter-mode initial display median was 999 ms Debug versus 286 ms Release; reported pooled jank was 28.49% versus 6.16%. History also improved. These are local observations, not fully-loaded startup timing, a formal benchmark or a 60-fps guarantee. Some jank remained.

Those checks used the local v14 signature. The dedicated-key candidate and any signing migration require final verification. The initial release candidate retained that behavior; 1.0.0 adds optional hidden-app inclusion for Kill All. Standalone Shizuku, prolonged soak and broad manufacturer coverage are not claimed.

1.0.0 adds a tested Kill All candidate policy: default hidden exclusions, opt-in inclusion, protected hidden apps, history-only entries and all-protected lists. JVM unit tests cover candidate selection. On-device verification of the new switch is not claimed.
