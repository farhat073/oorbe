# Project Rules: Oorbitt Launcher

When the user mentions "oorbitt", "Oorbitt", or "oorbE", read this section to restore context and continue work:

## 1. Project Location
- Path: `/home/pocketpc/Projects/oorbE`

## 2. Technical Context & Memory
We completed the following tasks for the launcher:
1. **Settings Screen Refactoring**: Organized settings inside a categorized, scrollable `LazyColumn` in `SettingsScreen.kt`. It supports theme cycling, icon shape, wallpaper dim level, desktop grid column/row limits, custom icon/font sizes, app labels, search bar style, drawer columns, sorting mode, dock slots, and dock labels toggles.
2. **Biometric App Lock (`feature-applock`)**:
   - Schema: `LockedAppEntity` persists package/activity info in database. Bumped Room database version to `2` in `LauncherDatabase.kt` to trigger schema updates via `fallbackToDestructiveMigration()`.
   - Repositories: `AppRepository` tracks locked apps.
   - UI: `AppLockScreen.kt` manages locks. `HomeScreen.kt`, `DrawerScreen.kt`, and `SearchScreen.kt` intercept launches to present biometric auth (using `AuthManager`).
3. **Dependency Fixes**: Added `implementation(project(":core-security"))` to `feature-settings/build.gradle.kts`, `feature-drawer/build.gradle.kts`, and `feature-home/build.gradle.kts` to allow correct compilation.
4. **Current Status**: The app compiles successfully, database migrations are applied cleanly, and the updated build has been installed and tested on the connected device (`2210132G`).

## 3. Next Steps / Where We Left Off
- The app is installed on the connected device and runs cleanly without crashes.
- If the user wants to continue polishing, suggest adding security configurations, testing custom biometric timeouts, or setting lock rules (e.g. auto-lock on screen off).
