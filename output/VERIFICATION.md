# Rise 2.1 verification

Verified 23 September 2026. Current release: version 2.1 / code 5, Android 8+.

- Signed APK installed successfully on Android 15 / API 35.
- 51 new native device checks passed: all four preset routines and seven-day coverage, valid times, duplicate-plan prevention, personal template saving, plan backup validation, schedule replacement, strict clear-day recording and undo, bonus reward persistence, five-minute event start/claim/expiry, and preservation of streak/event state in backups.
- 69 existing on-device checks passed on this release, covering tasks, hide/reveal/undo, alarms and repeat scheduling, profiles, logs, PDF and workout/rest timers.
- 536 pure-Java progression/event checks passed, including every rank through SSSS, missed-day rank reset while retaining lifetime XP, strict full-day requirements, deterministic daily draws and event time boundaries.
- 266 scheduling/serialization checks passed. Asset, image, PDF/calendar and APK v2/v3 signature checks passed.
- Visually inspected the new Home, preset/my-plan and bonus-event screens. Preview saved to screenshots/Rise-preview.png.
- The first preset UI test stopped before finding the name field while accessibility initialized. After initializing the test accessibility connection and allowing the dialog to settle, the final integration run passed all 51 checks. No app-runtime failure was observed in that test.
- Timed activities are self-reported, not camera verified. Tests ran on an emulator, not the user's physical phone. Device permission/battery/volume limitations still apply to alarms.
- APK SHA-256: `C51C5A8CD3B23B1EC7BFA3139C4164D92489504E3D2A9D1EDD110FF89863B43B`.

## Previous 2.0 verification

Verified 19 September 2026. Current release: version 2.0 / code 4, Android 8+.

- Installed successfully over the previous signed app on Android 15 / API 35.
- 69 native device checks passed: custom quest editing, completion hide/reveal/undo, profile, dreams, logs, offline PDF, alarms and next repeat, plus workout stopwatch start/pause, paused stability, rest countdown/skip, session saving and Analytics rendering.
- 55 native media checks passed: exercise and food images still decode, all tutorial intents match, three workouts and the food/training pages render, and browsing preserves tasks.
- 509 progression checks passed, including empty profile, duplicate completion, undo, current/yesterday streak handling, missed days, invalid/future records, 500 level-boundary cases and retained XP after deleting a quest definition.
- 266 schedule/serialization checks passed. Asset/PDF/calendar checks and APK v2/v3 signature verification passed.
- Visually inspected captured Home, Quest journal, Analytics and workout timer screens. The release preview is in screenshots/Rise-preview.png; a clean Home capture is in screenshots/Rise-home.png.
- This is an original native Java quest interface inspired by the supplied screenshot and https://ariseworkout.com/. Prioritized features are workout timers and custom quests. Camera rep counting, cloud sync, multiplayer and cosmetic shops are not implemented.
- Rest timer audio is foreground-only; persistent task alarms remain available. No physical phone or audible speaker output was tested. Existing dated completion records supply XP automatically; there is no account migration or remote service.
- APK SHA-256: `1E12A30CA8F3B5B8E1D7F1C6440A9AF88B9EE56B5C477BD42E0ACBDFE1BD7BDF`.

## Previous 1.2 verification

Verified 19 September 2026. Current release: version 1.2 / code 3, Android 8+.

- Installed successfully as an update on the Android 15 emulator.
- All 57 on-device checks passed, including immediate hiding after Mark complete, revealing completed tasks, undo/restoring a task, Android AlarmClock registration, actual alarm delivery, registration of the next repeat, and the alarm-sound channel configuration.
- All 266 scheduling/serialization checks passed, including selected weekday masks, one-time events, timezones and date boundaries.
- APK v2/v3 signatures and ZIP header paths verified. Offline image, PDF and calendar asset checks passed.
- Sound configuration was tested programmatically; audible sound on the user's physical phone was not tested. Notifications, precise alarm access and phone volume/settings must be enabled.
- Completion history and task definitions remain stored locally. Future scheduled days show the recurring task again.
- Current SHA-256: `3AD39B5EC41BC490A48DA00F427B72F3E09F9093FD03C22DC7AD6ACFDC8191A6`.

## Previous 1.1 verification

Updated 18 September 2026. Version 1.1 / code 2 is the current APK; the 1.0 results below are retained as historical baseline checks.

- Installed the signed 1.1 update successfully on Android 15 / API 35.
- Re-ran the full existing regression suite on 1.1: all 49 on-device checks passed, including task editing, logs, dreams, profile, PDF and reminder delivery.
- Passed 55 native media checks: decoded all exercise images and five food images, checked direct external YouTube intents for every exercise entry, rendered all three workouts and the food screen, and confirmed browsing preserves task data.
- Verified all 14 distinct tutorial titles and creators through YouTube oEmbed on 18 September 2026. Full playback and regional availability were not tested.
- Fixed Windows ZIP header separators during APK packaging; tested both local and central ZIP header names to prevent Android asset lookup failures.
- Visually inspected the new native workout and food screens. Images work offline; videos need an external browser/YouTube and internet.
- Verified the APK signature (v2/v3), calendar and PDF assets, all 18 illustrated exercise entries and five food images.
- Current APK size and SHA-256 are recorded in `Rise-Saran.apk.sha256` and the release script output. No physical phone was tested.

## Original 1.0 baseline

Verified 17 September 2026.

## Delivered build

- APK: `Rise-Saran.apk`
- Package: `com.saran.rise`
- Version: 1.0 / code 1
- Minimum Android: 8.0 / API 26
- Target / compile API: 35
- APK bytes: 78,499
- SHA-256: `E975FA431C0F5283B7C294824F17CBD3489A6C270D4E621BDC0C930B1D6F38B4`
- APK Signature Scheme v2 and v3 verified using the official Android apksigner.
- Built with official Android build-tools 35.0.0 and JDK 21 using the checked-in manual build script.

## Completed checks

- 266 JVM scheduling and serialization assertions passed: all nonempty weekday masks, one-time dates, expired events, midnight, timezone/DST and JSON round trips.
- Installed successfully on the official Android 15 / API 35 x86_64 emulator.
- 49 native instrumentation checks passed, covering the five main screens, task add/edit/validation/delete, completion storage, dream creation and savings, exercise logs, measurement logs, editable profile/protein calculation, workout instructions, offline PDF rendering, valid/invalid backup validation, notification posting, completion action and actual AlarmManager delivery to the receiver.
- Rebooted the emulator. All 16 seeded routine alarms were restored automatically; no manual app launch was needed after boot for this restoration check.
- Captured and visually inspected native app screens and the offline PDF viewer. A composite preview is in `screenshots/Rise-preview.png`.
- Rendered and visually inspected all 12 PDF pages. Confirmed text-bearing pages, source hyperlinks and no accidental blank pages.
- Validated 18 unique recurring calendar events, 18 display alarms, component nesting, recurrence start weekdays, event durations, timezone identifiers, line folding and CRLF format.
- Verified the APK contains native DEX code, the exact delivered PDF and complete local workout/meal/care/career content. No supplied photographs are embedded.

## Limits of verification

- No physical phone was connected. Android 8-14 and Android 16+ were not separately device-tested. API 26 is the declared minimum; compatibility with every manufacturer or managed-device policy is not guaranteed.
- Reminder delivery was tested with notification and precise-alarm permissions enabled. Approximate fallback is implemented, but manufacturer-specific battery management, long Doze periods, notification denial, silent mode and Do Not Disturb were not exhaustively tested.
- Backup schema validation was exercised. Cross-device file-picker export/import was not exercised end-to-end on physical devices.
- The standard Gradle/Android Studio project is included, but the manual build path is the one verified here.
- Calendar events were validated as an importable ICS file. They were not imported into a live Google account. No calendar connector was available and no email was sent.
- The guide is a beginner routine based on the supplied age/weight and assumed height; it is not a medical assessment or a promise of a specific physique.

The instrumentation runner is destructive to the isolated emulator's Rise test data and must not be run against a real phone containing personal data.
