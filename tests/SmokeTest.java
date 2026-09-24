package com.saran.rise;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.*;
import java.util.*;
import org.json.*;

public class SmokeTest extends Instrumentation {
  int checks = 0;
  MainActivity activity;

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  void check(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }

  void ui(Runnable r) {
    runOnMainSync(r);
    waitForIdleSync();
    SystemClock.sleep(200);
  }

  void screenshot(String name) throws Exception {
    Bitmap b = getUiAutomation().takeScreenshot();
    try (FileOutputStream out =
        new FileOutputStream(
            new File(getTargetContext().getExternalFilesDir(null), name + ".png"))) {
      b.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    b.recycle();
  }

  ArrayList<AccessibilityNodeInfo> fields() {
    ArrayList<AccessibilityNodeInfo> a = new ArrayList<>();
    walk(getUiAutomation().getRootInActiveWindow(), a);
    return a;
  }

  void walk(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> a) {
    if (n == null) return;
    if ("android.widget.EditText".contentEquals(n.getClassName())) a.add(n);
    for (int i = 0; i < n.getChildCount(); i++) walk(n.getChild(i), a);
  }

  void set(AccessibilityNodeInfo node, String value) {
    Bundle b = new Bundle();
    b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
    check(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b), "Set input text");
  }

  void click(String label) {
    AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
    for (AccessibilityNodeInfo n : root.findAccessibilityNodeInfosByText(label)) {
      if (n.getText() != null
          && label.equalsIgnoreCase(n.getText().toString())
          && n.isClickable()) {
        check(n.performAction(AccessibilityNodeInfo.ACTION_CLICK), "Click " + label);
        SystemClock.sleep(250);
        waitForIdleSync();
        return;
      }
    }
    throw new AssertionError("Button not found: " + label);
  }

  void closeDialog() {
    sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
    waitForIdleSync();
    SystemClock.sleep(200);
  }

  boolean textVisible(String value) {
    AccessibilityNodeInfo n = getUiAutomation().getRootInActiveWindow();
    return n != null && !n.findAccessibilityNodeInfosByText(value).isEmpty();
  }

  android.widget.TextView findText(View root, String label) {
    if (root instanceof android.widget.TextView
        && label.contentEquals(((android.widget.TextView) root).getText()))
      return (android.widget.TextView) root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) {
        android.widget.TextView found = findText(group.getChildAt(i), label);
        if (found != null) return found;
      }
    }
    return null;
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    try {
      Context c = getTargetContext();
      Store previous = new Store(c);
      for (Task t : previous.tasks()) Reminders.cancel(c, t.id);
      previous.prefs.edit().clear().commit();
      Store s = new Store(c);
      s.prefs.edit().putBoolean("welcomed", true).commit();
      activity =
          (MainActivity)
              startActivitySync(
                  new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      waitForIdleSync();
      SystemClock.sleep(400);
      check(s.tasks().size() == 16, "Seed contains the full 16-task routine");
      check(s.array("dreams").length() == 5, "Five editable starting dreams");
      for (int i = 0; i < 5; i++) {
        final int index = i;
        ui(
            () -> {
              activity.tab = index;
              activity.render();
            });
        screenshot("screen-" + i);
        check(activity.body.getChildCount() > 0, "Screen " + i + " renders");
      }
      ui(() -> activity.editTask(null));
      ArrayList<AccessibilityNodeInfo> f = fields();
      check(f.size() == 3, "Task editor fields present");
      set(f.get(0), "Smoke task");
      set(f.get(1), "A meaningful test task");
      click("Save");
      Task added = null;
      for (Task t : new Store(c).tasks()) if (t.title.equals("Smoke task")) added = t;
      check(added != null, "Task creation persists");
      final Task editing = added;
      ui(() -> activity.editTask(editing));
      f = fields();
      set(f.get(0), "");
      click("Save");
      check(
          new Store(c).task(editing.id).title.equals("Smoke task"),
          "Empty title rejected without mutation");
      f = fields();
      set(f.get(0), "Edited task");
      click("Save");
      check(new Store(c).task(editing.id).title.equals("Edited task"), "Task update persists");
      ui(() -> { activity.tab = 0; activity.render(); });
      check(findText(activity.body, "Edited task") != null, "Pending task is on Today");
      ui(() -> {
        ViewGroup card = (ViewGroup) findText(activity.body, "Edited task").getParent();
        while (findText(card, "Mark complete") == null) card = (ViewGroup) card.getParent();
        ((android.widget.CheckBox) findText(card, "Mark complete")).setChecked(true);
      });
      check(findText(activity.body, "Edited task") == null, "Mark complete hides task immediately");
      ui(() -> findText(activity.body, "Completed today (1)").performClick());
      check(findText(activity.body, "Edited task") != null, "Completed task can be revealed");
      ui(() -> {
        ViewGroup card = (ViewGroup) findText(activity.body, "Edited task").getParent();
        while (findText(card, "Completed today") == null) card = (ViewGroup) card.getParent();
        ((android.widget.CheckBox) findText(card, "Completed today")).setChecked(false);
      });
      check(!s.done(editing.id, s.today().toString()), "Uncheck restores pending state");
      check(findText(activity.body, "Edited task") != null, "Restored task is visible");
      new ReminderReceiver()
          .onReceive(
              c,
              new Intent("DONE").putExtra("id", editing.id).putExtra("date", s.today().toString()));
      check(
          new Store(c).done(editing.id, s.today().toString()),
          "Notification Done persists completion");
      Reminders.cancel(c, editing.id);
      s.delete(editing.id);
      check(new Store(c).task(editing.id) == null, "Task deletion persists");
      ui(() -> activity.editDream(-1));
      f = fields();
      check(f.size() == 4, "Dream editor fields");
      set(f.get(0), "A tested dream");
      set(f.get(1), "10000");
      set(f.get(2), "2500");
      set(f.get(3), "Save a little every week");
      click("Save");
      check(new Store(c).array("dreams").length() == 6, "Dream created");
      check(
          new Store(c).array("dreams").getJSONObject(5).getDouble("saved") == 2500,
          "Savings persisted");
      ui(
          () -> {
            activity.tab = 3;
            activity.render();
          });
      screenshot("dream-savings");
      JSONArray ds = s.array("dreams");
      ds.remove(5);
      s.array("dreams", ds);
      ui(() -> activity.logExercise("Goblet squat"));
      f = fields();
      set(f.get(1), "12, 12");
      set(f.get(2), "5");
      click("Save log");
      check(new Store(c).array("training").length() == 1, "Exercise log saved");
      ui(() -> activity.measurements());
      f = fields();
      set(f.get(0), "65");
      set(f.get(1), "80");
      set(f.get(2), "Feeling stronger");
      click("Save entry");
      check(new Store(c).array("measurements").length() == 1, "Measurement log saved");
      ui(() -> activity.profile());
      f = fields();
      set(f.get(2), "66");
      click("Save");
      check(new Store(c).prefs.getFloat("weight", 0) == 66, "Profile updates persist");
      check(activity.proteinTarget().equals("92-106 g protein / day"), "Protein estimate updates");
      s.prefs.edit().putFloat("weight", 65).commit();
      ui(
          () -> {
            activity.tab = 1;
            activity.render();
            activity.workout(activity.content.optJSONArray("workouts").optJSONObject(0));
          });
      check(textVisible("Goblet squat"), "Workout instructions displayed");
      screenshot("workout-instructions");
      closeDialog();
      ui(() -> activity.guide());
      check(textVisible("Page 1 of 12"), "Offline PDF opened");
      screenshot("offline-guide");
      closeDialog();
      JSONObject backup = new JSONObject();
      JSONObject values = new JSONObject();
      for (Map.Entry<String, ?> e : s.prefs.getAll().entrySet())
        values.put(e.getKey(), e.getValue());
      backup.put("format", "rise-backup-1").put("values", values);
      JSONObject valid = activity.validateBackup(backup);
      check(
          new JSONArray(valid.getString("tasks")).length() == 16,
          "Backup validates original tasks");
      JSONObject invalid = new JSONObject(backup.toString());
      JSONArray bad = new JSONArray(invalid.getJSONObject("values").getString("tasks"));
      bad.getJSONObject(0).put("minute", 1440);
      invalid.getJSONObject("values").put("tasks", bad.toString());
      boolean rejected = false;
      try {
        activity.validateBackup(invalid);
      } catch (Exception expected) {
        rejected = true;
      }
      check(rejected, "Invalid imported reminder time rejected");
      Task notification = s.tasks().get(0);
      s.done(notification.id, s.today().toString(), false);
      Reminders.show(c, notification, s.today().toString(), false);
      SystemClock.sleep(250);
      check(
          c.getSystemService(NotificationManager.class).getActiveNotifications().length > 0,
          "Notification posts successfully");
      new ReminderReceiver()
          .onReceive(
              c,
              new Intent("DONE")
                  .putExtra("id", notification.id)
                  .putExtra("date", s.today().toString()));
      check(s.done(notification.id, s.today().toString()), "Notification completion action works");
      boolean cleared = false;
      for (int poll = 0; poll < 20 && !cleared; poll++) {
        SystemClock.sleep(100);
        cleared = true;
        for (android.service.notification.StatusBarNotification active : c.getSystemService(NotificationManager.class).getActiveNotifications())
          if (active.getId() == notification.id) cleared = false;
      }
      check(cleared, "Done clears the completed task's notification");
      s.done(notification.id, s.today().toString(), false);
      final Task timed =
          new Task(
              s.nextId(),
              "Alarm delivery verification",
              "Personal",
              "Actual AlarmManager dispatch test",
              0,
              127,
              1);
      timed.date = s.today().toString();
      s.save(timed);
      long trigger = System.currentTimeMillis() + 6000;
      Reminders.at(c, timed, "FIRE", s.today().toString(), trigger);
      check(c.getSystemService(AlarmManager.class).getNextAlarmClock() != null
          && c.getSystemService(AlarmManager.class).getNextAlarmClock().getTriggerTime() == trigger,
          "Selected alarm registered with Android AlarmClock");
      boolean delivered = false;
      for (int poll = 0; poll < 20 && !delivered; poll++) {
        SystemClock.sleep(1000);
        for (android.service.notification.StatusBarNotification n :
            c.getSystemService(NotificationManager.class).getActiveNotifications())
          if (n.getId() == timed.id) delivered = true;
      }
      check(delivered, "Actual scheduled alarm delivered to receiver");
      check(c.getSystemService(AlarmManager.class).getNextAlarmClock() != null,
          "Recurring alarm schedules the next occurrence after delivery");
      NotificationChannel channel = c.getSystemService(NotificationManager.class).getNotificationChannel(Reminders.CHANNEL);
      check(channel.getSound() != null && channel.getAudioAttributes().getUsage() == android.media.AudioAttributes.USAGE_ALARM,
          "Routine channel uses alarm sound and alarm audio stream");
      Reminders.cancel(c, timed.id);
      s.delete(timed.id);
      ui(() -> activity.workoutTimer("Test workout"));
      click("Start / resume");
      SystemClock.sleep(1200);
      click("Pause");
      long paused = activity.workoutMillis();
      check(paused >= 1000 && s.prefs.getLong("workoutStart", 0) == 0, "Workout stopwatch pauses");
      SystemClock.sleep(200);
      check(activity.workoutMillis() == paused, "Paused stopwatch stays fixed");
      click("Rest 60 sec");
      check(s.prefs.getLong("restEnd", 0) > System.currentTimeMillis(), "Rest timer starts");
      click("Skip rest");
      check(s.prefs.getLong("restEnd", 0) == 0, "Rest timer can be skipped");
      int previousLogs = s.array("training").length();
      click("Finish & save session");
      check(s.array("training").length() == previousLogs + 1, "Finished workout session saved");
      check(activity.workoutMillis() == 0, "Saved session resets stopwatch");
      screenshot("quest-timer");
      closeDialog();
      ui(() -> {activity.tab=2;activity.render();});
      check(findText(activity.body,"YOUR ASCENT") != null,"Analytics renders progression");
      screenshot("quest-analytics");
      ui(() -> {activity.tab=1;activity.render();});
      screenshot("quest-journal");
      ui(
          () -> {
            activity.tab = 0;
            activity.render();
          });
      screenshot("today-final");
      result.putString(
          "stream",
          "PASS: " + checks + " on-device checks. Screenshots in target external files.\n");
      finish(Activity.RESULT_OK, result);
    } catch (Throwable e) {
      try {
        screenshot("failure");
      } catch (Exception ignored) {
      }
      result.putString(
          "stream",
          "FAIL after "
              + checks
              + " checks: "
              + e
              + "\n"
              + android.util.Log.getStackTraceString(e));
      finish(Activity.RESULT_CANCELED, result);
    }
  }
}
