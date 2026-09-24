# Rise 

A native, offline Android application written in Java. It combines a home-training plan, Indian meals, skin-care and career routines, editable reminders, daily tasks and personal dreams.

## Downloads

- `output/Rise-Saran.apk`: signed personal-install APK; Android 8.0 / API 26 or newer.
- `output/pdf/Saran-12-Week-Home-Routine.pdf`: 12-page plan and setup instructions.
- `output/Saran-Routine.ics`: recurring calendar events in Asia/Kolkata.
- `output/Rise-Java-Source.zip`: app sources and build/test scripts.

The app uses native Android views and APIs, with no WebView or website wrapper. Version 1.1 adds offline exercise images, food/protein images and direct YouTube tutorial buttons. Tap images to enlarge. Videos open externally and need internet. There is no internet permission, account, advertising or analytics in Rise itself. The supplied personal photographs are not included.

Install version 2.1 over the earlier Rise APK to retain your data; do not uninstall first. You can also save a JSON backup in Profile before updating.

## New in 2.1 — Plans, streak ranks and events

Open **Quests > Choose a preset plan**. Four editable starter routines are included: **Balanced day**, **Fitness & recovery**, **Career builder**, and **Gentle restart**. Preview their times and days before adding them to your current schedule or confirming replacement. Adding the same preset again skips matching title/time/day entries. Replacement cancels the old schedule's alarms but retains completion history, XP, dreams and workout logs. All tasks can then be edited individually. Save the current routine as a named **My plan**, reapply it later, or delete the saved template. Up to 20 personal plans are supported, including in backup/restore. One-time tasks in a saved plan move to the day it is applied.

**Strict streak rule:** complete every scheduled task for the day before midnight in your selected timezone. Optional bonus quests are not required. Undoing a task or changing today's routine recalculates today's clear-day status. A day without scheduled tasks does not count; keep a light recovery/care quest on rest days. Yesterday's streak remains active while today's tasks are pending. Missing a full day resets the rank to E and starts a new rank-XP run; lifetime XP, levels and history remain. Historical clear-day snapshots stay stable; older completion records without snapshots are interpreted using the current routine.

Both requirements must be met to advance:

| Rank | Consecutive cleared days | XP earned in current streak |
|---|---:|---:|
| E | Starting rank | 0 |
| D | 1 | 100 |
| C | 2 | 250 |
| B | 3 | 500 |
| A | 5 | 900 |
| S | 7 | 1,500 |
| SS | 10 | 2,400 |
| SSS | 14 | 3,600 |
| SSSS | 21 | 5,000 |

On Home, open **Daily bonus quests & timed event**. Two shuffled daily activities award **150 XP each**. The draw stays fixed for that date, preventing rerolls. The **five-minute event awards 250 XP** when you complete its stated activity and claim before expiry. Its clock keeps running when closed and ends at midnight if earlier. An expired attempt cannot restart that day; new events appear the next day. Rewards are self-reported, once per item per day, and optional. There is no physical punishment or extra exercise penalty. Rank needs cleared days as well as XP, so bonuses help with the XP requirement but cannot skip streak days.

The older 2.0 progression description below documents the previous release; these 2.1 rules replace it.

## New in 2.0 — Quest edition

The original native interface has been redesigned with navy panels, cyan edges, rank badges and a four-tab layout inspired by your screenshot and the publicly described quest system at https://ariseworkout.com/. This is Rise, an independent personal app; it is not an official Arise release or a complete clone of that product.

- **Home:** daily quests, a midnight reset countdown in your configured timezone, player rank, XP, active streak, share sheet, backup export and completed-task reveal/undo. Share opens your phone's chooser; nothing is sent automatically. Save exports a JSON backup; normal progress saves automatically on this phone.
- **Quests:** home workouts, illustrated exercises and tutorial links, food/protein guidance, editable custom quests and repeat alarms, focus timer, career/care guides and your dream board.
- **Analytics:** seven-day completion chart, XP/level progress, game attributes, milestones, rank roadmap and links to body/training logs.
- **Profile:** your personal details, guides, reminder settings, backup/restore and app information. Earlier instructions referring to More now mean Profile; Train, Fuel and Dreams are accessible through Quests.
- **Workout timers:** open a workout, then **Workout & rest timer**. Start/pause/resume the stopwatch, choose 60/90-second rests, skip rest and finish/save the session to training history. One active workout timer is retained locally; if another workout is opened, its existing session name is shown. Stopwatch time continues outside the screen. Rest sounds play while Rise is open; use task alarms for background reminders.

Progress rules: each dated completed quest earns 50 XP; each 500 XP adds a level. Undo removes that completion's reward, so rechecking cannot duplicate XP. Existing completion records count automatically. A streak means consecutive days with at least one completion; yesterday's streak remains active while today's quests are pending. Stats are game points, not measurements of strength or health. Streak milestones reflect the currently active streak. Deleted quest records retain XP and are grouped under personal/FOC points. Rank thresholds are visible in Analytics.

The selected scope is workout timers and custom quests. Camera rep counting, cloud sync, multiplayer, shops and animated avatar classes are not included. The original training plan remains appropriate to your equipment; the screenshot's 100-rep/10-km examples are not substituted for it. The PDF remains the original starter guide.

## New in 1.2

- Mark complete immediately removes the task from the pending Today list. Open **Completed today** to view it, and uncheck **Completed today** on its card to undo. The routine is retained for future selected days.
- Edit a task, choose its time, select repeat days (or **Every day**), and enable **Alarm at selected time**. Save it. **More > Routine reminders** must also be on.
- Enable notifications and **Precise timing** in More. With this access, the selected occurrence is registered as an Android alarm-clock event, and delivery schedules its next repeat. Without precise timing access, Android may delay the reminder.
- Routine alarms use an alarm-sound notification and the alarm audio stream. Check the phone's alarm volume and Rise's **Routine alarms** notification channel. This is a notification sound, not a continuously ringing full-screen clock alarm. Device restrictions and Do Not Disturb can still affect delivery/sound. Tasks completed early are quiet for that date.

## Install on your phone

1. Transfer `Rise-Saran.apk` to the phone and open it from Files.
2. If prompted, temporarily allow that file-opening app to install unknown apps. Install Rise, then switch that permission off again.
3. Open Rise. Allow notifications. Under **More**, enable **Precise timing** if available and try **Test now**, then **Test in 1 min**.
4. If your phone restricts background operation, allow Rise to run in the background through its app settings. Test with the screen locked too.

This APK is signed for personal installation, not distributed through Google Play. Work-managed phones may prohibit sideloading. No app can guarantee that notifications will sound through silent mode, Do Not Disturb, force-stop or manufacturer battery restrictions. After force-stopping Rise, reopen it to restore future scheduling. Future reminders are also restored after reboot/unlock, app update and timezone/time changes. Snoozed follow-ups are intentionally cleared on reboot/time changes; your normal future schedule is restored.

## What works

- Today: daily completion, seven-day counts, add/edit/delete tasks, categories, notes, durations, reminder toggles, selected weekdays and one-time dates.
- Focus: 25-minute or five-minute timer. Countdown uses the saved finish time; sound is emitted only while Rise is open. This is not a background timer notification.
- Train: three home strength routines with form cues, exercise logs, body measurements and history deletion.
- Fuel: Indian breakfast/lunch/dinner/snack options and a protein estimate based on the editable weight.
- Dreams: add/edit/delete personal goals, notes, manual INR budgets and savings, completion status. A zero budget works for non-financial goals.
- More: profile, timezone, care/career guides, offline PDF viewer, reminder settings, JSON backup and restore.
- Notifications: Done, Snooze 10 min, and one optional follow-up after 30 minutes for an incomplete task. Follow-ups are limited to 07:00-22:45 in the selected timezone. Scheduled task notifications use the time you choose, including outside that window.

All task categories and dream names are editable. Exercise library text and the original PDF are the supplied starter guide; changing your profile does not rewrite that PDF. Past completion totals are computed against the current task schedule, so editing repeat days can change historical totals. No automatic bank, job-board or Google Calendar synchronization is included.

## Your starting routine

The starter profile is age 20, weight 65 kg, height assumed 165 cm (5 feet 5 inches), mixed vegetarian/non-vegetarian food, and 07:00 wake-up. Strength: Monday/Wednesday/Friday at 07:15. Other mornings: indoor movement. Skin care: 08:00 and 21:00. DSA: 09:00; Java: 10:00; weekday applications: 14:00; interview practice: 17:00. All task times can be edited.

The guide focuses on building strength and gradual body recomposition. It does not diagnose body fat from photos, prescribe crash dieting or promise a six-pack or a changed face shape. It contains exercise scaling, sets/reps, recovery, meal options, a weekly menu and sources. Read its safety and progression notes before training.

## Google Calendar

Calendar account access was not connected, so events were **not directly added** to `saransaran4232@gmail.com` and no email was sent.

On a computer, sign in to the intended Google account, open Google Calendar, choose **Settings → Import & export**, and import `Saran-Routine.ics` into a chosen calendar. A separate calendar named “Rise routine” makes it easy to edit or remove the imported plan. Import only once to avoid duplicates. Check event times and calendar notification settings on your phone after import. The events repeat from 17 September 2026 onward in Asia/Kolkata. Imported calendar events and in-app tasks are separate; editing one does not update the other.

Official instructions: https://support.google.com/calendar/answer/37118

## Backup and privacy

Export JSON through More before uninstalling or changing devices. Uninstalling removes local data. Restore checks the file format and task/profile ranges, then asks before replacing current data. Keep backups private; they contain tasks, goals and optional measurements. Lock-screen notification text may reveal task names: use Android's notification privacy controls if desired.

## Build from Java source

The verified APK is built using the official Android API 35 jar and build-tools 35.0.0, plus JDK 21. The app has no runtime dependencies beyond Android. `build-apk.ps1` compiles resources with AAPT2, Java with `javac`, converts bytecode with D8, aligns the APK and signs it with APK Signature Schemes v2/v3.

On this workspace:

```powershell
./build-apk.ps1
```

The script accepts `-SdkRoot` and `-JdkRoot`. It expects build tools at `build-tools/android-15` (the directory in Google's standalone 35.0.0 archive) and the platform at `platforms/android-35/android.jar`. For an SDK Manager installation, update the build-tool path to `build-tools/35.0.0` in the script. Download SDK components from Google's Android developer tools distribution and follow its license terms.

The project also contains standard Gradle files for Android Studio: AGP 8.9.1, compile/target SDK 35, min SDK 26. Use a compatible Gradle version such as 8.11.1 and a supported JDK when importing it. The manual script, not the Android Studio/Gradle route, is the verified build path in this workspace.

### Signing and updates

The personal signing key is in `tools/signing/rise-personal.jks` on the original workspace. It is **excluded from the source ZIP**. The build script uses a local development password; this is not a hardened commercial release-key workflow. Keep the existing key private and retain it if you want future APKs to update the current installation. Building on a new machine without that key generates a different key; Android then requires uninstalling the old app first. Export a data backup before uninstalling.

## Tests

`tests/TaskTest.java` exercises weekday masks, one-time tasks, date boundaries, timezone/DST behavior and JSON round trips (266 assertions).

```powershell
& 'C:/Program Files/Java/jdk-21/bin/javac.exe' -cp tools/json.jar -d build/tests app/src/main/java/com/saran/rise/Task.java tests/TaskTest.java
& 'C:/Program Files/Java/jdk-21/bin/java.exe' -cp 'tools/json.jar;build/tests' TaskTest
```

`tests/SmokeTest.java` is a native instrumentation test with no external UI-test framework. **It resets app data and is only for an isolated test emulator. Never run it against a personal phone containing real Rise data.** Build with `test-device.ps1`, install the signed test APK beside the app, grant notification/precise-alarm access and run:

```text
adb -s emulator-5556 shell am instrument -w com.saran.rise.tests/com.saran.rise.SmokeTest
```

The device suite exercises native screens, task creation/update/validation/deletion, dream savings, exercise and measurement logs, profile updates, PDF opening, backup validation and scheduled-notification delivery. See `output/VERIFICATION.md` for actual results and limits.

## Content sources

- WHO activity guidance: https://www.who.int/europe/publications/i/item/9789240014886
- NIH ODS exercise and protein: https://ods.od.nih.gov/factsheets/ExerciseAndAthleticPerformance-HealthProfessional/
- ICMR-NIN Dietary Guidelines for Indians: https://www.nin.res.in/dietaryguidelines/pdfjs/locale/DGI07052024P.pdf
- AAD skin care: https://www.aad.org/public/everyday-care/skin-care-secrets/routine/healthier-looking-skin
- AAD sunscreen: https://www.aad.org/media/stats-sunscreen
- Android scheduling behavior: https://developer.android.com/develop/background-work/services/alarms

Meal combinations, training schedule and career routine are a practical synthesis, not copied protocols. Protein values are estimates and vary by food, brand and preparation.
