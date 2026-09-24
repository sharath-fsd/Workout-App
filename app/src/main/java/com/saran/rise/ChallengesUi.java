package com.saran.rise;

import android.app.AlertDialog;
import android.widget.*;
import java.time.*;
import java.util.*;

final class ChallengesUi {
  static void open(MainActivity a) {
    Store s = a.store;
    LocalDate date = s.today();
    String day = date.toString();
    long midnight = date.plusDays(1).atStartOfDay(s.zone()).toInstant().toEpochMilli();
    LinearLayout f = a.column();
    a.addText(f, "DAILY DRAW  /  UP TO +550 XP", 13, QuestUi.GOLD);
    a.addText(
        f,
        "Two rotating bonus quests and one five-minute event. Optional, self-reported and limited"
            + " to one reward each per day. They do not replace your daily routine for streak"
            + " credit.",
        13,
        a.MUTED);
    List<String> choices = Challenges.daily(date);
    TextView[] buttons = new TextView[2];
    for (int i = 0; i < 2; i++) {
      final String key = "bonus_" + day + "_" + i;
      a.addText(f, "BONUS " + (i + 1) + "  /  +150 XP", 12, QuestUi.GREEN);
      a.addText(f, choices.get(i), 17, a.TEXT);
      final TextView button =
          a.button(
              s.prefs.getBoolean(key, false) ? "Reward claimed" : "Complete & claim +150 XP",
              true,
              () -> {});
      button.setEnabled(!s.prefs.getBoolean(key, false));
      button.setOnClickListener(
          v -> {
            if (!s.today().equals(date)) {
              a.error("A new day has started. Reopen bonus quests.");
              return;
            }
            if (!s.prefs.getBoolean(key, false)) {
              s.prefs.edit().putBoolean(key, true).commit();
              button.setText("Reward claimed");
              button.setEnabled(false);
              a.error("Bonus complete · +150 XP");
            }
          });
      f.addView(button);
      buttons[i] = button;
      a.space(f, 18);
    }
    a.addText(f, "QUICK-TIME EVENT  /  +250 XP", 13, QuestUi.GOLD);
    a.addText(f, Challenges.event(date), 17, a.TEXT);
    a.addText(
        f,
        "Start when ready. Claim only after completing the activity and before the timer expires."
            + " Leaving the screen does not pause it; an expired attempt cannot restart today.",
        12,
        a.MUTED);
    String startKey = "qte_start_" + day, claimedKey = "qte_" + day;
    TextView clock = a.heading("READY / 05:00", 27);
    f.addView(clock);
    TextView start = a.button("Start 5-minute event", true, () -> {}),
        claim = a.button("Completed · claim +250 XP", false, () -> {});
    f.addView(start);
    f.addView(claim);
    Runnable refresh =
        () -> {
          long began = 0;
          try {
            began = Long.parseLong(s.prefs.getString(startKey, "0"));
          } catch (NumberFormatException ignored) {
          }
          long now = System.currentTimeMillis();
          boolean earned = s.prefs.getBoolean(claimedKey, false),
              active = Challenges.inTime(now, began, midnight) && s.today().equals(date);
          start.setEnabled(began == 0 && !earned && s.today().equals(date));
          start.setAlpha(start.isEnabled() ? 1 : .45f);
          claim.setEnabled(active && !earned);
          claim.setAlpha(claim.isEnabled() ? 1 : .45f);
          long seconds = Math.max(0, (Math.min(began + 300000L, midnight) - now + 999) / 1000);
          clock.setText(
              earned
                  ? "CLEARED / +250 XP"
                  : began == 0
                      ? "READY / 05:00"
                      : active
                          ? String.format(
                              Locale.US, "%02d:%02d REMAINING", seconds / 60, seconds % 60)
                          : "EXPIRED / TRY TOMORROW");
        };
    start.setOnClickListener(
        v -> {
          if (s.today().equals(date) && !s.prefs.contains(startKey)) {
            s.prefs.edit().putString(startKey, Long.toString(System.currentTimeMillis())).commit();
          }
          refresh.run();
        });
    claim.setOnClickListener(
        v -> {
          long began = 0;
          try {
            began = Long.parseLong(s.prefs.getString(startKey, "0"));
          } catch (NumberFormatException ignored) {
          }
          if (s.today().equals(date)
              && Challenges.inTime(System.currentTimeMillis(), began, midnight)
              && !s.prefs.getBoolean(claimedKey, false)) {
            s.prefs.edit().putBoolean(claimedKey, true).commit();
            a.error("Timed event cleared · +250 XP");
          }
          refresh.run();
        });
    a.eventRefresh = refresh;
    refresh.run();
    AlertDialog dialog =
        a.dialog("BONUS QUESTS & EVENTS", f).setPositiveButton("Close", null).create();
    dialog.setOnDismissListener(
        d -> {
          a.eventRefresh = null;
          a.render();
        });
    dialog.show();
  }
}
