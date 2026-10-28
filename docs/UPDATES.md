# Updates

Open **Settings → About → Updates**.

- **Automatic update checks**: on by default. Check the latest stable release once per app process, in the background. Reopening the recent-app overlay does not repeat the check. No periodic worker, service, or timer is installed. Off prevents the startup request; the manual button still works.
- **Update notification**: on by default. Show a brief in-app message when a newer release is found, at most once per version per process. This does not use Android push notifications or require notification permission. Turning it off does not disable checks.
- **Check for updates**: check manually. Connection failures are shown on this page and do not block the app switcher.
- **Download update**: downloads only after selection; progress and cancellation are available. Nothing installs automatically.
- **Install update**: Android may first require permission to install apps from this source. Return to this page and select Install again. Then select Continue installation and confirm in Android's installer. Cancellation keeps the current app.

Downloads come from the latest stable release of `mds818/RecentAppsTV`. The APK must have the expected size and SHA-256 checksum, the existing application ID and signing certificate, a higher version code, and the release's version name. A different upstream signing key cannot be updated in place. The updater never uninstalls an app as a workaround.

Updates use Android's PackageInstaller with automatic installation location rather than forcing internal storage. This accommodates adopted USB storage where Android permits it; Android still decides final placement. Portable USB storage is different from adopted device storage. Shield adopted-USB behavior requires hardware testing.

## Release requirements

Publish stable tags such as `v1.0.2`, with a higher Android version code and a universal APK named `RecentAppsTV-1.0.2.apk`. Retain the established release signing key. Include GitHub's SHA-256 asset digest or a `SHA256SUMS.txt` asset containing the exact APK filename and digest. Drafts and prereleases are excluded. Rebuilding the same version does not generate an automatic update offer; published upgrades must advance version name and version code.

## Android API references

- [Session parameters and install location](https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setInstallLocation(int))
- [PackageInstaller results and user confirmation](https://developer.android.com/reference/android/content/pm/PackageInstaller)
