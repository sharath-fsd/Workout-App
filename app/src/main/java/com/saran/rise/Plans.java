package com.saran.rise;

import android.app.*;
import android.text.InputType;
import android.widget.*;
import java.util.*;
import org.json.*;

final class Plans {
  static final String[] NAMES = {
    "Balanced day", "Fitness & recovery", "Career builder", "Gentle restart"
  };

  static Task task(String title, String category, String note, int minute, int days, int duration) {
    return new Task(0, title, category, note, minute, days, duration);
  }

  static List<Task> preset(int index) {
    List<Task> tasks = new ArrayList<>();
    if (index == 0 || index == 1) {
      tasks.add(
          task(
              "Home strength A / B / C",
              "Workout",
              "Mon: A, Wed: B, Fri: C. Use the workout guide's sets, recovery and form cues.",
              440,
              21,
              35));
      tasks.add(
          task(
              "Easy indoor movement",
              "Workout",
              "Comfortable indoor walking or low-impact movement. Begin with 10 minutes if needed.",
              440,
              42,
              20));
      tasks.add(
          task(
              "Sunday recovery",
              "Care",
              "Easy mobility and recovery. Rest is part of the plan.",
              440,
              64,
              10));
      tasks.add(
          task(
              "Protein with lunch",
              "Food",
              "Choose a protein source and vegetables; use the meal guide's portions.",
              780,
              127,
              25));
    }
    if (index == 0 || index == 2) {
      tasks.add(
          task(
              "DSA practice",
              "Career",
              "Solve one suitable problem. Explain the idea and complexity; review mistakes.",
              540,
              127,
              45));
      tasks.add(
          task(
              "Apply for suitable jobs",
              "Career",
              "Tailor and submit one or two relevant applications; track the next step.",
              960,
              31,
              30));
    }
    if (index == 2) {
      tasks.add(
          task(
              "Build a coding project",
              "Career",
              "Make one small, tested improvement and record what you learned.",
              615,
              31,
              60));
      tasks.add(
          task(
              "Interview practice",
              "Career",
              "Practice one technical or behavioral answer aloud.",
              840,
              31,
              25));
      tasks.add(
          task(
              "Movement break",
              "Workout",
              "Take a comfortable indoor walk and move away from your desk.",
              1080,
              127,
              15));
    }
    if (index == 3) {
      tasks.add(
          task(
              "Start the day",
              "Personal",
              "Get ready, open the curtains and choose one priority.",
              420,
              127,
              10));
      tasks.add(
          task(
              "Ten minutes of movement",
              "Workout",
              "Easy indoor walking at a comfortable pace. Stop if unwell.",
              440,
              127,
              10));
      tasks.add(
          task(
              "One small coding step",
              "Career",
              "Review one solved problem or practice for 20 focused minutes.",
              600,
              127,
              20));
    }
    tasks.add(
        task(
            "Morning face care",
            "Care",
            "Gentle cleanse, moisturize and sunscreen when needed; follow the care guide.",
            425,
            127,
            5));
    tasks.add(
        task(
            "Evening care & tomorrow",
            "Care",
            "Cleanse, moisturize and prepare tomorrow's essentials.",
            1290,
            127,
            10));
    return tasks;
  }

  static void open(MainActivity a) {
    LinearLayout f = a.column();
    a.addText(
        f,
        "Choose a starting routine. Preview its days and times, then add it or replace your"
            + " schedule. Every quest remains editable.",
        14,
        a.MUTED);
    for (int i = 0; i < NAMES.length; i++) {
      final int index = i;
      f.addView(a.button(NAMES[i], false, () -> preview(a, NAMES[index], preset(index))));
      a.space(f, 8);
    }
    f.addView(a.button("Save current routine as my plan", true, () -> save(a)));
    JSONArray saved = a.store.array("savedPlans");
    for (int i = 0; i < saved.length(); i++) {
      final JSONObject plan = saved.optJSONObject(i);
      if (plan == null) continue;
      f.addView(
          a.button(
              "My plan: " + plan.optString("name"),
              false,
              () -> {
                try {
                  List<Task> tasks = new ArrayList<>();
                  JSONArray items = plan.getJSONArray("tasks");
                  for (int j = 0; j < items.length(); j++) {
                    Task t = new Task(items.getJSONObject(j));
                    if (t.title.trim().isEmpty()
                        || t.title.length() > 150
                        || t.minute < 0
                        || t.minute > 1439
                        || t.days < 0
                        || t.days > 127
                        || t.duration < 1
                        || t.duration > 600) throw new Exception();
                    tasks.add(t);
                  }
                  preview(a, plan.optString("name"), tasks);
                } catch (Exception e) {
                  a.error("This saved plan is invalid.");
                }
              }));
      f.addView(a.button("Delete saved plan: "+plan.optString("name"),false,()->
          new AlertDialog.Builder(a).setTitle("Delete saved plan?").setMessage("Your active routine stays unchanged.")
              .setPositiveButton("Delete",(dialog,which)->{
                JSONArray current=a.store.array("savedPlans");
                for(int k=0;k<current.length();k++)if(plan.toString().equals(current.optJSONObject(k).toString())){current.remove(k);break;}
                a.store.array("savedPlans",current);a.error("Saved plan deleted. Reopen plans to refresh.");
              }).setNegativeButton("Cancel",null).show()));
    }
    a.dialog("PRESET & MY PLANS", f).setPositiveButton("Close", null).show();
  }

  static void save(MainActivity a) {
    LinearLayout f = a.column();
    EditText name = a.field(f, "Plan name", "My routine", InputType.TYPE_CLASS_TEXT);
    AlertDialog dialog =
        a.dialog("Save my plan", f)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      String title = name.getText().toString().trim();
                      if (title.isEmpty() || title.length() > 80) {
                        name.setError("Use 1–80 characters");
                        return;
                      }
                      try {
                        JSONArray plans = a.store.array("savedPlans");
                        if (plans.length() >= 20) {
                          a.error("Up to 20 saved plans are supported.");
                          return;
                        }
                        JSONArray tasks = new JSONArray();
                        for (Task t : a.store.tasks()) tasks.put(t.json());
                        if (tasks.length() == 0) {
                          a.error("Add a quest before saving a plan.");
                          return;
                        }
                        plans.put(new JSONObject().put("name", title).put("tasks", tasks));
                        a.store.array("savedPlans", plans);
                        dialog.dismiss();
                        a.error("Plan saved. Reopen plans to find it.");
                      } catch (Exception e) {
                        a.error("Could not save plan.");
                      }
                    }));
    dialog.show();
  }

  static void preview(MainActivity a, String name, List<Task> templates) {
    LinearLayout f = a.column();
    a.addText(
        f,
        "Times use "
            + a.store.zone()
            + ". One-time tasks in saved plans move to today. Past times next fire on the next"
            + " selected day.",
        12,
        a.MUTED);
    for (Task t : templates)
      a.addText(
          f,
          t.time()
              + "  "
              + t.title
              + "\n"
              + (t.days == 0 ? "Today" : a.repeatLabel(t))
              + " · "
              + t.duration
              + " min",
          14,
          a.TEXT);
    AlertDialog preview =
        a.dialog(name, f)
            .setPositiveButton("Add to routine", null)
            .setNeutralButton("Replace routine", null)
            .setNegativeButton("Close", null)
            .create();
    preview.setOnShowListener(
        d -> {
          preview
              .getButton(-1)
              .setOnClickListener(
                  v -> {
                    apply(a, templates, false);
                    preview.dismiss();
                    a.error("Plan added. Edit its quests to customize it.");
                  });
          preview
              .getButton(-3)
              .setOnClickListener(
                  v ->
                      new AlertDialog.Builder(a)
                          .setTitle("Replace current routine?")
                          .setMessage(
                              "This replaces your task schedule and cancels its alarms. Completion"
                                  + " history, XP, dreams and workout logs remain. Today's"
                                  + " clear-day status is recalculated.")
                          .setPositiveButton(
                              "Replace",
                              (x, w) -> {
                                apply(a, templates, true);
                                preview.dismiss();
                                a.error("Routine replaced.");
                              })
                          .setNegativeButton("Cancel", null)
                          .show());
        });
    preview.show();
  }

  static void apply(MainActivity a, List<Task> templates, boolean replace) {
    List<Task> tasks = replace ? new ArrayList<>() : a.store.tasks();
    if (replace) for (Task old : a.store.tasks()) Reminders.cancel(a, old.id);
    for (Task template : templates) {
      boolean duplicate = false;
      for (Task existing : tasks)
        if (existing.title.equals(template.title)
            && existing.minute == template.minute
            && existing.days == template.days) {
          duplicate = true;
          break;
        }
      if (duplicate) continue;
      Task task =
          new Task(
              a.store.nextId(),
              template.title,
              template.category,
              template.note,
              template.minute,
              template.days,
              template.duration);
      task.reminder = template.reminder;
      task.date = task.days == 0 ? a.store.today().toString() : "";
      tasks.add(task);
    }
    a.store.tasks(tasks);
    Reminders.all(a);
    a.render();
  }
}
