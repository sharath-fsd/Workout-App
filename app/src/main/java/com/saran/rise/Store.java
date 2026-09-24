package com.saran.rise;

import android.content.*;
import java.time.*;
import java.util.*;
import org.json.*;

public class Store {
  public final SharedPreferences prefs;

  public Store(Context c) {
    prefs = c.getSharedPreferences("rise_v1", Context.MODE_PRIVATE);
    if (!prefs.getBoolean("seeded", false)) seed();
    recordDay();
  }

  public ArrayList<Task> tasks() {
    ArrayList<Task> a = new ArrayList<>();
    try {
      JSONArray j = new JSONArray(prefs.getString("tasks", "[]"));
      for (int i = 0; i < j.length(); i++) a.add(new Task(j.getJSONObject(i)));
    } catch (Exception ignored) {
    }
    a.sort(Comparator.comparingInt(t -> t.minute));
    return a;
  }

  public void tasks(List<Task> a) {
    JSONArray j = new JSONArray();
    for (Task t : a) j.put(t.json());
    prefs.edit().putString("tasks", j.toString()).commit();
    recordDay();
  }

  public Task task(int id) {
    for (Task t : tasks()) if (t.id == id) return t;
    return null;
  }

  public int nextId() {
    int n = prefs.getInt("nextId", 100);
    prefs.edit().putInt("nextId", n + 1).commit();
    return n;
  }

  public void save(Task t) {
    ArrayList<Task> a = tasks();
    a.removeIf(x -> x.id == t.id);
    a.add(t);
    tasks(a);
  }

  public void delete(int id) {
    ArrayList<Task> a = tasks();
    a.removeIf(x -> x.id == id);
    tasks(a);
  }

  public ZoneId zone() {
    try {
      return ZoneId.of(prefs.getString("zone", "Asia/Kolkata"));
    } catch (Exception e) {
      return ZoneId.systemDefault();
    }
  }

  public LocalDate today() {
    return LocalDate.now(zone());
  }

  public boolean done(int id, String date) {
    return prefs.getBoolean("done_" + id + "_" + date, false);
  }

  public void done(int id, String date, boolean value) {
    prefs.edit().putBoolean("done_" + id + "_" + date, value).commit();
    if (today().toString().equals(date)) recordDay();
  }

  public void recordDay() {
    int scheduled = 0;
    boolean all = true;
    for (Task task : tasks())
      if (task.on(today())) {
        scheduled++;
        if (!done(task.id, today().toString())) all = false;
      }
    prefs.edit().putBoolean("cleared_" + today(), scheduled > 0 && all).commit();
  }

  public JSONArray array(String key) {
    try {
      return new JSONArray(prefs.getString(key, "[]"));
    } catch (Exception e) {
      return new JSONArray();
    }
  }

  public void array(String key, JSONArray a) {
    prefs.edit().putString(key, a.toString()).commit();
  }

  public String dream() {
    JSONArray a = array("dreams");
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.optJSONObject(i);
      if (o != null && !o.optBoolean("achieved")) return o.optString("title", "your future");
    }
    return "your future";
  }

  private void seed() {
    ArrayList<Task> a = new ArrayList<>();
    a.add(
        new Task(
            1,
            "Wake up & get ready",
            "Personal",
            "Water if thirsty. Open the curtains. Put your phone away for the first 10 minutes.",
            420,
            127,
            10));
    a.add(
        new Task(
            2,
            "Home strength + core",
            "Workout",
            "Mon A / Wed B / Fri C. Open Train for the exact workout. Start with 2 sets; finish"
                + " with 2-3 good reps left.",
            435,
            21,
            40));
    a.add(
        new Task(
            3,
            "Indoor walk & mobility",
            "Workout",
            "Tue/Thu/Sat: 20-30 minutes of brisk marching or walking indoors. Sunday: easy 15-20"
                + " minutes. Build up gradually.",
            435,
            106,
            25));
    a.add(
        new Task(
            4,
            "Morning face care",
            "Care",
            "After exercise: gentle cleanse, moisturiser, broad-spectrum SPF 30+ before daylight"
                + " exposure.",
            480,
            127,
            5));
    a.add(
        new Task(
            5,
            "Protein breakfast",
            "Food",
            "Choose a Fuel breakfast. Add eggs, curd, milk, tofu or pulses. Eat enough to feel"
                + " satisfied.",
            490,
            127,
            20));
    a.add(
        new Task(
            6,
            "DSA • one focused problem",
            "Career",
            "5 min recall, 30 min solve, 10 min complexity + notes. Retry from memory tomorrow.",
            540,
            127,
            45));
    a.add(
        new Task(
            7,
            "Java coding / project",
            "Career",
            "Build one small feature, write meaningful tests, and make a clear commit. Start with 5"
                + " minutes.",
            600,
            63,
            60));
    a.add(
        new Task(
            8,
            "Balanced lunch",
            "Food",
            "Vegetables + rice or chapati + a real protein portion. Open Fuel for Indian options.",
            780,
            127,
            30));
    a.add(
        new Task(
            9,
            "Apply to suitable jobs",
            "Career",
            "Aim for 2-3 tailored applications on weekdays. Track company, role, date and next"
                + " action. Never pay a recruiter for an interview.",
            840,
            31,
            30));
    a.add(
        new Task(
            10,
            "Interview practice",
            "Career",
            "Explain a Java/SQL topic out loud; do one STAR story or mock question. Record one"
                + " improvement.",
            1020,
            63,
            30));
    a.add(
        new Task(
            11,
            "Indoor movement break",
            "Workout",
            "10-15 minutes of comfortable indoor walking. Build weekly brisk walking toward 150"
                + " minutes as fitness improves.",
            1080,
            127,
            15));
    a.add(
        new Task(
            12,
            "Dinner & tomorrow's prep",
            "Food",
            "Protein + vegetables + rice or chapati. Set out dumbbells and write tomorrow's"
                + " smallest first step.",
            1170,
            127,
            30));
    a.add(
        new Task(
            13,
            "Evening face care",
            "Care",
            "Gently cleanse and moisturise. No lemon, bleach or harsh scrubs. Keep pillowcase and"
                + " towel clean.",
            1260,
            127,
            5));
    a.add(
        new Task(
            14,
            "Wind down & sleep",
            "Personal",
            "Aim for 7-9 hours of sleep. Default lights out: 22:30 for a 07:00 wake-up.",
            1350,
            127,
            10));
    a.add(
        new Task(
            15,
            "Protein snack",
            "Food",
            "If needed: milk with roasted chana, or curd. Add fruit if hungry. Adjust to your day's"
                + " protein total.",
            960,
            127,
            10));
    a.add(
        new Task(
            16,
            "Weekly review & dream goals",
            "Personal",
            "Review training, DSA, applications and savings. Pick one small adjustment for the"
                + " coming week.",
            1110,
            64,
            15));
    tasks(a);
    JSONArray d = new JSONArray();
    String[] names = {
      "Royal Enfield GT 650",
      "iPhone 20 • future wish",
      "Apple headphones",
      "Great shoes",
      "Clothes I feel good in"
    };
    for (String name : names) {
      JSONObject o = new JSONObject();
      try {
        o.put("title", name)
            .put("target", 0)
            .put("saved", 0)
            .put("note", "Set your own budget and a realistic target date.")
            .put("achieved", false);
      } catch (Exception ignored) {
      }
      d.put(o);
    }
    array("dreams", d);
    prefs
        .edit()
        .putBoolean("seeded", true)
        .putString("name", "Saran")
        .putInt("age", 20)
        .putFloat("weight", 65f)
        .putFloat("height", 165f)
        .putString("zone", "Asia/Kolkata")
        .putBoolean("reminders", true)
        .putBoolean("nudge", true)
        .apply();
  }
}
