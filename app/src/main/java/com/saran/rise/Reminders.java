package com.saran.rise;

import android.app.*;
import android.content.*;
import android.os.Build;
import java.time.*;

public class Reminders {
  public static final String CHANNEL = "rise_alarm_v2";

  public static void channel(Context c) {
    NotificationManager n = c.getSystemService(NotificationManager.class);
    NotificationChannel ch =
        new NotificationChannel(CHANNEL, "Routine alarms", NotificationManager.IMPORTANCE_HIGH);
    ch.setDescription("Your workout, care, learning and personal reminders");
    ch.enableVibration(true);
    ch.setSound(
        android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
        new android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build());
    n.createNotificationChannel(ch);
  }

  static PendingIntent pending(Context c, int id, String kind, String date) {
    Intent i =
        new Intent(c, ReminderReceiver.class)
            .setAction(kind)
            .putExtra("id", id)
            .putExtra("date", date);
    int code =
        id * 10
            + (kind.equals("SNOOZE")
                ? 1
                : kind.equals("NUDGE")
                    ? 2
                    : kind.equals("DONE") ? 3 : kind.equals("LATER") ? 4 : 0);
    return PendingIntent.getBroadcast(
        c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  static void at(Context c, Task t, String kind, String date, long millis) {
    AlarmManager a = c.getSystemService(AlarmManager.class);
    PendingIntent p = pending(c, t.id, kind, date);
    try {
      if (Build.VERSION.SDK_INT < 31 || a.canScheduleExactAlarms()) {
        if ("FIRE".equals(kind)) {
          PendingIntent open =
              PendingIntent.getActivity(
                  c,
                  t.id,
                  new Intent(c, MainActivity.class),
                  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
          a.setAlarmClock(new AlarmManager.AlarmClockInfo(millis, open), p);
        } else a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, p);
      } else a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, p);
    } catch (SecurityException e) {
      a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, p);
    }
  }

  public static void schedule(Context c, Task t) {
    Store s = new Store(c);
    AlarmManager a = c.getSystemService(AlarmManager.class);
    a.cancel(pending(c, t.id, "FIRE", ""));
    if (!t.reminder || !s.prefs.getBoolean("reminders", true)) return;
    long next = t.next(System.currentTimeMillis(), s.zone());
    if (next > 0)
      at(c, t, "FIRE", Instant.ofEpochMilli(next).atZone(s.zone()).toLocalDate().toString(), next);
  }

  public static void all(Context c) {
    channel(c);
    Store s = new Store(c);
    for (Task t : s.tasks()) {
      if (!s.prefs.getBoolean("reminders", true)) cancel(c, t.id);
      else schedule(c, t);
    }
  }

  public static void clearExtras(Context c, int id) {
    AlarmManager a = c.getSystemService(AlarmManager.class);
    a.cancel(pending(c, id, "SNOOZE", ""));
    a.cancel(pending(c, id, "NUDGE", ""));
    c.getSystemService(NotificationManager.class).cancel(id);
  }

  public static void cancel(Context c, int id) {
    c.getSystemService(AlarmManager.class).cancel(pending(c, id, "FIRE", ""));
    clearExtras(c, id);
  }

  public static void show(Context c, Task t, String date, boolean followup) {
    channel(c);
    Store s = new Store(c);
    Intent open =
        new Intent(c, MainActivity.class)
            .putExtra("taskId", t.id)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pi =
        PendingIntent.getActivity(
            c, t.id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    String body =
        (followup ? "Start with 5 minutes. One small step toward " + s.dream() + ". " : "")
            + t.note;
    Notification.Builder builder =
        new Notification.Builder(c, CHANNEL)
            .setSmallIcon(com.saran.rise.R.drawable.ic_notification)
            .setContentTitle((followup ? "A gentle restart: " : "") + t.title)
            .setContentText(body)
            .setStyle(new Notification.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_ALARM);
    if (s.task(t.id) != null) {
      builder
          .addAction(
              new Notification.Action.Builder(null, "Done", pending(c, t.id, "DONE", date)).build())
          .addAction(
              new Notification.Action.Builder(
                      null, "Snooze 10 min", pending(c, t.id, "LATER", date))
                  .build());
    }
    Notification n = builder.build();
    try {
      c.getSystemService(NotificationManager.class).notify(t.id, n);
    } catch (SecurityException ignored) {
    }
  }
}
