package com.saran.rise;

import android.content.*;
import java.time.*;

public class ReminderReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    Store s = new Store(c);
    Task t = s.task(i.getIntExtra("id", -1));
    if (t == null) return;
    String action = i.getAction(), date = i.getStringExtra("date");
    if (date == null) date = s.today().toString();
    if ("DONE".equals(action)) {
      s.done(t.id, date, true);
      Reminders.clearExtras(c, t.id);
      return;
    }
    if (!s.prefs.getBoolean("reminders", true) || !t.reminder) return;
    if ("FIRE".equals(action)) Reminders.schedule(c, t);
    if (!date.equals(s.today().toString()) || s.done(t.id, date)) return;
    if ("LATER".equals(action)) {
      Reminders.clearExtras(c, t.id);
      Reminders.at(c, t, "SNOOZE", date, System.currentTimeMillis() + 600000);
      return;
    }
    if ("NUDGE".equals(action) && !s.prefs.getBoolean("nudge", true)) return;
    Reminders.show(c, t, date, "NUDGE".equals(action));
    if ("FIRE".equals(action) && s.prefs.getBoolean("nudge", true)) {
      ZonedDateTime later = ZonedDateTime.now(s.zone()).plusMinutes(30);
      int m = later.getHour() * 60 + later.getMinute();
      if (m >= 420 && m < 1365)
        Reminders.at(c, t, "NUDGE", date, System.currentTimeMillis() + 1800000);
    }
  }
}
