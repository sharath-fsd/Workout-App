package com.saran.rise;

import java.time.*;
import org.json.JSONObject;

public class Task {
  public int id, minute, days, duration;
  public String title, category, note, date;
  public boolean reminder;

  public Task(
      int id, String title, String category, String note, int minute, int days, int duration) {
    this.id = id;
    this.title = title;
    this.category = category;
    this.note = note;
    this.minute = minute;
    this.days = days;
    this.duration = duration;
    this.date = "";
    this.reminder = true;
  }

  public Task(JSONObject o) {
    id = o.optInt("id");
    title = o.optString("title");
    category = o.optString("category", "Personal");
    note = o.optString("note");
    minute = o.optInt("minute", 420);
    days = o.optInt("days", 127);
    duration = o.optInt("duration", 20);
    date = o.optString("date");
    reminder = o.optBoolean("reminder", true);
  }

  public JSONObject json() {
    JSONObject o = new JSONObject();
    try {
      o.put("id", id)
          .put("title", title)
          .put("category", category)
          .put("note", note)
          .put("minute", minute)
          .put("days", days)
          .put("duration", duration)
          .put("date", date)
          .put("reminder", reminder);
    } catch (Exception ignored) {
    }
    return o;
  }

  public boolean on(LocalDate d) {
    return days == 0
        ? d.toString().equals(date)
        : (days & (1 << (d.getDayOfWeek().getValue() - 1))) != 0;
  }

  public long next(long now, ZoneId zone) {
    ZonedDateTime z = Instant.ofEpochMilli(now).atZone(zone);
    for (int i = 0; i < 9; i++) {
      LocalDate d = z.toLocalDate().plusDays(i);
      long at = d.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli();
      if (on(d) && at > now) return at;
    }
    if (days == 0) {
      try {
        long at =
            LocalDate.parse(date)
                .atTime(minute / 60, minute % 60)
                .atZone(zone)
                .toInstant()
                .toEpochMilli();
        return at > now ? at : -1;
      } catch (Exception ignored) {
      }
    }
    return -1;
  }

  public String time() {
    return String.format(java.util.Locale.US, "%02d:%02d", minute / 60, minute % 60);
  }
}
