package com.saran.rise;

import java.time.LocalDate;
import java.util.*;

/** Derived from dated completion records: toggling a quest cannot award duplicate XP. */
public final class Progress {
  public final int completed, xp, level, streak, streakXp;
  public static final String[] RANKS = {"E", "D", "C", "B", "A", "S", "SS", "SSS", "SSSS"};
  public static final int[] RANK_DAYS = {0, 1, 2, 3, 5, 7, 10, 14, 21};
  public static final int[] RANK_XP = {0, 100, 250, 500, 900, 1500, 2400, 3600, 5000};
  public final String rank;
  public final Map<LocalDate, Integer> days = new TreeMap<>();
  public final int[] stats = new int[4];

  public Progress(Map<String, ?> values, List<Task> tasks, LocalDate today) {
    Map<Integer, String> categories = new HashMap<>();
    for (Task t : tasks) categories.put(t.id, t.category);
    int count = 0;
    for (Map.Entry<String, ?> entry : values.entrySet()) {
      if (!entry.getKey().startsWith("done_") || !Boolean.TRUE.equals(entry.getValue())) continue;
      try {
        String[] parts = entry.getKey().split("_", 3);
        int id = Integer.parseInt(parts[1]);
        LocalDate date = LocalDate.parse(parts[2]);
        if (date.isAfter(today)) continue;
        count++;
        days.put(date, days.getOrDefault(date, 0) + 1);
        String category = categories.getOrDefault(id, "Personal");
        int stat =
            category.equals("Workout")
                ? 0
                : category.equals("Career")
                    ? 1
                    : category.equals("Care") || category.equals("Food") ? 2 : 3;
        stats[stat] += 10;
      } catch (RuntimeException ignored) {
      }
    }
    Map<LocalDate, Integer> awards = new TreeMap<>();
    for (Map.Entry<LocalDate, Integer> day : days.entrySet())
      awards.put(day.getKey(), day.getValue() * 50);
    Set<LocalDate> cleared = new HashSet<>();
    // Old records have no clear-day snapshots: reconstruct them using the existing routine.
    for (LocalDate date : days.keySet()) {
      boolean all = true;
      int scheduled = 0;
      for (Task task : tasks)
        if (task.on(date)) {
          scheduled++;
          if (!Boolean.TRUE.equals(values.get("done_" + task.id + "_" + date))) all = false;
        }
      if (!values.containsKey("cleared_" + date) && scheduled > 0 && all) cleared.add(date);
    }
    for (Map.Entry<String, ?> entry : values.entrySet()) {
      String key = entry.getKey();
      try {
        if (key.startsWith("cleared_") && Boolean.TRUE.equals(entry.getValue())) {
          LocalDate date = LocalDate.parse(key.substring(8));
          if (!date.isAfter(today)) cleared.add(date);
        }
        if (Boolean.TRUE.equals(entry.getValue())
            && (key.matches("bonus_[0-9]{4}-[0-9]{2}-[0-9]{2}_[01]")
                || key.matches("qte_[0-9]{4}-[0-9]{2}-[0-9]{2}"))) {
          LocalDate date =
              LocalDate.parse(key.startsWith("bonus_") ? key.substring(6, 16) : key.substring(4));
          if (!date.isAfter(today))
            awards.put(date, awards.getOrDefault(date, 0) + (key.startsWith("bonus_") ? 150 : 250));
        }
      } catch (RuntimeException ignored) {
      }
    }
    completed = count;
    int lifetime = 0;
    for (int reward : awards.values()) lifetime += reward;
    xp = lifetime;
    level = 1 + xp / 500;
    LocalDate cursor = cleared.contains(today) ? today : today.minusDays(1);
    int run = 0;
    while (cleared.contains(cursor)) {
      run++;
      cursor = cursor.minusDays(1);
    }
    streak = run;
    int current = 0;
    if (streak > 0)
      for (Map.Entry<LocalDate, Integer> reward : awards.entrySet())
        if (reward.getKey().isAfter(cursor) && !reward.getKey().isAfter(today))
          current += reward.getValue();
    streakXp = current;
    int tier = 0;
    for (int i = 1; i < RANKS.length; i++)
      if (streak >= RANK_DAYS[i] && streakXp >= RANK_XP[i]) tier = i;
    rank = RANKS[tier];
  }
}
