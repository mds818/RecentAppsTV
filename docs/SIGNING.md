# Release signing

The prepared public APK uses a dedicated RSA-4096 certificate. Private keys/passwords and former development keys are excluded from source and CI. Gradle produces a non-debuggable unsigned Release APK; signing happens separately with the maintainer's private key. Keep the keystore and recoverable password securely backed up, and use the same release identity for subsequent updates.

Build with JDK 17 and SDK platform/build-tools 35. Sign app/build/outputs/apk/release/app-release-unsigned.apk using apksigner. Supply passwords through protected files or environment variables, not literal command-line arguments or committed Gradle properties. Verify with apksigner verify --verbose --print-certs. A public certificate/fingerprint is safe to distribute; a private key is not.

## Existing installations

The component stays com.websnake.recentstv/.RecentsActivity. An ordinary APK signed with the new key cannot directly update independently signed upstream/development builds.

An optional local migration build (-PmigrationBuild=true) targets Android 9+ and can carry a v3 certificate lineage authorized by the old local development key, with rotation-min-sdk-version 28. The normal public APK retains Android 7+ support. Test migration on-device before promising it; it cannot migrate the original upstream signature without its private key. After migration, updates and rollback builds must use the new release key; the prepared lineage grants no rollback capability to the old key.

Do not upload historic local source ZIPs containing development keystores, private signing files, or machine/device backups.
