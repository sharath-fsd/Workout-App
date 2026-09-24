package com.saran.rise;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    Store s = new Store(c);
    for (Task t : s.tasks()) Reminders.clearExtras(c, t.id);
    Reminders.all(c);
  }
}
