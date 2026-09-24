package com.saran.rise;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Native quest interface. All progress comes from the user's existing local records. */
final class QuestUi {
  final MainActivity a;
  final Store s;
  final Progress p;
  static final int CYAN = 0xff55b7ff, GREEN = 0xff39dfb4, GOLD = 0xffffcd65;

  QuestUi(MainActivity activity) {
    a = activity;
    s = a.store;
    p = new Progress(s.prefs.getAll(), s.tasks(), s.today());
  }

  void label(LinearLayout box, String value, int size, int color) {
    a.addText(box, value, size, color);
  }

  LinearLayout panel() {
    return a.card();
  }

  void bar(LinearLayout parent, int value, int max, int color) {
    ProgressBar bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
    bar.setMax(Math.max(1, max));
    bar.setProgress(value);
    bar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff26394d));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, a.dp(8));
    lp.setMargins(0, a.dp(4), 0, a.dp(10));
    parent.addView(bar, lp);
  }

  void home() {
    label(a.body, "R I S E   /   PLAYER SYSTEM", 11, CYAN);
    LinearLayout hero = panel(), row = a.row();
    row.addView(new Emblem(a, p.rank), new LinearLayout.LayoutParams(a.dp(76), a.dp(76)));
    LinearLayout identity = a.column();
    identity.setPadding(a.dp(14), 0, 0, 0);
    identity.addView(a.heading(s.prefs.getString("name", "Saran"), 28));
    label(identity, p.streak + " DAY STREAK", 12, GOLD);
    label(identity, "LEVEL " + p.level + "  /  RANK " + p.rank, 12, CYAN);
    row.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
    hero.addView(row);
    a.space(hero, 12);
    bar(hero, p.xp % 500, 500, CYAN);
    label(
        hero,
        (p.xp % 500) + " / 500 XP to next level     •     " + p.xp + " TOTAL XP",
        11,
        a.MUTED);
    LinearLayout reset = panel(), resetRow = a.row(), left = a.column(), right = a.column();
    label(left, "DAILY RESET IN", 10, a.MUTED);
    a.resetText = a.text("", 23, CYAN);
    a.resetText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    left.addView(a.resetText);
    label(
        right,
        s.today()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
            .toUpperCase(Locale.US),
        13,
        a.TEXT);
    label(right, s.today().getDayOfWeek().toString(), 12, GREEN);
    resetRow.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
    resetRow.addView(right, new LinearLayout.LayoutParams(0, -2, 1));
    reset.addView(resetRow);
    a.refreshReset();
    label(
        a.body,
        "RANK RUN / "
            + p.streakXp
            + " XP · Clear all scheduled quests to keep your streak. Miss a day: rank resets to E.",
        12,
        a.MUTED);
    a.body.addView(
        a.button("Daily bonus quests & timed event · +550 XP", false, () -> ChallengesUi.open(a)));
    if (!a.getSystemService(NotificationManager.class).areNotificationsEnabled()) {
      a.body.addView(a.button("Enable quest alarms", false, () -> a.notificationPermission()));
    }
    a.section("DAILY QUESTS");
    LinearLayout tools = a.row();
    tool(tools, "Share", () -> share());
    tool(tools, "Save", () -> a.exportBackup());
    tool(tools, "+ Add", () -> a.editTask(null));
    a.body.addView(tools);
    int total = 0, done = 0;
    for (Task t : s.tasks())
      if (t.on(s.today())) {
        total++;
        if (s.done(t.id, s.today().toString())) done++;
      }
    a.space(a.body, 12);
    label(
        a.body,
        done + " / " + total + " cleared   •   " + (total - done) + " remaining",
        12,
        a.MUTED);
    for (Task t : s.tasks())
      if (t.on(s.today()) && !s.done(t.id, s.today().toString())) quest(t, false);
    if (done == total) {
      LinearLayout clear = panel();
      label(clear, total == 0 ? "YOUR NEXT QUEST AWAITS" : "DAILY QUESTS CLEARED", 18, GREEN);
      label(
          clear,
          total == 0
              ? "Add a quest or choose repeat days to begin."
              : "Recovery is part of progress. Your routine returns on its next scheduled day.",
          13,
          a.MUTED);
    }
    if (done > 0) {
      final int count = done;
      a.body.addView(
          a.button(
              (a.showCompleted ? "Hide completed" : "Completed today") + " (" + count + ")",
              false,
              () -> {
                a.showCompleted = !a.showCompleted;
                a.render();
              }));
      if (a.showCompleted)
        for (Task t : s.tasks())
          if (t.on(s.today()) && s.done(t.id, s.today().toString())) quest(t, true);
    }
    a.space(a.body, 12);
    a.actions(a.body, "Focus timer", () -> a.focusDialog(), "All routines", () -> a.allTasks());
    label(a.body, "NEXT CHAPTER  /  " + s.dream(), 12, GOLD);
  }

  void tool(LinearLayout row, String title, Runnable action) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
    lp.setMargins(a.dp(3), 0, a.dp(3), 0);
    row.addView(a.button(title, false, action), lp);
  }

  void quest(Task t, boolean done) {
    LinearLayout card = panel(), main = a.row(), words = a.column();
    label(card, t.time() + "   /   " + t.category.toUpperCase(Locale.US), 10, a.MUTED);
    CheckBox cb = new CheckBox(a);
    cb.setButtonTintList(android.content.res.ColorStateList.valueOf(done ? GREEN : CYAN));
    cb.setText(done ? "Completed today" : "Mark complete");
    cb.setTextSize(10);
    cb.setTextColor(a.MUTED);
    cb.setChecked(done);
    cb.setMinHeight(a.dp(48));
    cb.setContentDescription((done ? "Undo completion of " : "Complete ") + t.title);
    TextView name = a.heading(t.title, 18);
    words.addView(name);
    String stat =
        t.category.equals("Workout")
            ? "STR"
            : t.category.equals("Career") ? "INT" : t.category.equals("Personal") ? "FOC" : "VIT";
    label(words, stat + " +10", 11, done ? a.MUTED : GREEN);
    main.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
    LinearLayout reward = a.column();
    label(reward, t.duration + " min", 17, a.TEXT);
    label(reward, done ? "XP EARNED" : "+50 XP", 12, CYAN);
    main.addView(reward);
    card.addView(main);
    LinearLayout controls = a.row();
    controls.addView(cb, new LinearLayout.LayoutParams(0, -2, 1));
    TextView menu = a.button("•••", false, () -> {});
    menu.setContentDescription("Options for " + t.title);
    menu.setOnClickListener(
        v -> {
          PopupMenu popup = new PopupMenu(a, menu);
          popup
              .getMenu()
              .add("View instructions")
              .setOnMenuItemClickListener(
                  item -> {
                    new AlertDialog.Builder(a)
                        .setTitle(t.title)
                        .setMessage(t.note + "\n\n" + a.repeatLabel(t))
                        .setPositiveButton("Close", null)
                        .show();
                    return true;
                  });
          popup
              .getMenu()
              .add("Edit quest & alarm")
              .setOnMenuItemClickListener(
                  item -> {
                    a.editTask(t);
                    return true;
                  });
          popup
              .getMenu()
              .add("Delete quest")
              .setOnMenuItemClickListener(
                  item -> {
                    new AlertDialog.Builder(a)
                        .setTitle("Delete " + t.title + "?")
                        .setMessage(
                            "Its future alarms will be cancelled. Earned completion records"
                                + " remain.")
                        .setPositiveButton(
                            "Delete",
                            (d, w) -> {
                              Reminders.cancel(a, t.id);
                              s.delete(t.id);
                              a.render();
                            })
                        .setNegativeButton("Cancel", null)
                        .show();
                    return true;
                  });
          popup.show();
        });
    controls.addView(menu, new LinearLayout.LayoutParams(a.dp(50), a.dp(48)));
    card.addView(controls);
    cb.setOnCheckedChangeListener(
        (button, value) -> {
          s.done(t.id, s.today().toString(), value);
          if (value) Reminders.clearExtras(a, t.id);
          a.render();
          if (value) Toast.makeText(a, "Quest cleared  ·  +50 XP", Toast.LENGTH_SHORT).show();
        });
  }

  void share() {
    Intent send =
        new Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(
                Intent.EXTRA_TEXT,
                "Rise progress: Level "
                    + p.level
                    + " • Rank "
                    + p.rank
                    + " • "
                    + p.xp
                    + " XP • "
                    + p.streak
                    + " day streak. One quest at a time.");
    a.startActivity(Intent.createChooser(send, "Share your progress"));
  }

  void hub() {
    a.body.addView(a.button("Choose a preset plan", true, () -> Plans.open(a)));
    a.title(
        "Training / habits / focus",
        "QUEST JOURNAL",
        "Choose your next action. Your own pace. Your own progression.");
    destination(
        "01 / TRAINING GROUNDS",
        "Home strength plan",
        "Three routines • 2–5 kg dumbbells • form images & tutorials",
        5);
    destination(
        "02 / DAILY FUEL",
        "Meals & protein",
        "Indian meal choices, portions and protein guidance",
        6);
    LinearLayout routines = panel();
    label(routines, "03 / CUSTOM QUESTS", 11, CYAN);
    label(routines, "Build your daily system", 23, a.TEXT);
    a.actions(
        routines, "+ Add quest", () -> a.editTask(null), "Manage routines", () -> a.allTasks());
    LinearLayout focus = panel();
    label(focus, "04 / INTELLIGENCE", 11, CYAN);
    label(focus, "Train your focus", 23, a.TEXT);
    a.actions(
        focus,
        "Focus timer",
        () -> a.focusDialog(),
        "Career guide",
        () -> articles("career", "Career quests"));
    destination(
        "05 / YOUR WHY", "Dream board", "Editable goals, next steps and savings progress", 7);
    LinearLayout care = panel();
    label(care, "06 / VITALITY", 11, CYAN);
    label(care, "Care & recovery", 23, a.TEXT);
    care.addView(a.button("Open care routine", false, () -> articles("care", "Care & recovery")));
  }

  void articles(String key, String title) {
    LinearLayout box = a.column();
    a.showArticles(box, key);
    a.dialog(title, box).setPositiveButton("Close", null).show();
  }

  void destination(String eyebrow, String title, String subtitle, int tab) {
    LinearLayout c = panel();
    label(c, eyebrow, 11, CYAN);
    label(c, title, 24, a.TEXT);
    label(c, subtitle, 13, a.MUTED);
    c.addView(
        a.button(
            "Open",
            false,
            () -> {
              a.tab = tab;
              a.render();
            }));
  }

  void analytics() {
    a.title("Player records", "YOUR ASCENT", "Every completed quest is a step forward.");
    LinearLayout top = panel();
    label(top, "LEVEL " + p.level + "   /   " + p.rank + " RANK", 24, CYAN);
    label(top, p.xp + " XP earned  •  " + p.completed + " quests cleared", 15, a.TEXT);
    bar(top, p.xp % 500, 500, CYAN);
    label(
        top,
        "50 XP per task • 500 XP per lifetime level. Rank uses fully cleared days and XP earned in"
            + " the current streak. Miss a day and rank returns to E; lifetime XP remains. Bonus"
            + " quests do not replace your daily routine.",
        12,
        a.MUTED);
    a.section("LAST 7 DAYS");
    LinearLayout chart = panel();
    int max = 1;
    for (int i = 0; i < 7; i++) max = Math.max(max, p.days.getOrDefault(s.today().minusDays(i), 0));
    for (int i = 6; i >= 0; i--) {
      LocalDate day = s.today().minusDays(i);
      int value = p.days.getOrDefault(day, 0);
      label(
          chart,
          day.format(DateTimeFormatter.ofPattern("EEE dd", Locale.ENGLISH))
              + "   /   "
              + value
              + " quests",
          12,
          a.TEXT);
      bar(chart, value, max, i == 0 ? GREEN : CYAN);
    }
    a.section("PLAYER ATTRIBUTES");
    LinearLayout stats = panel();
    String[] names = {"STR / Training", "INT / Career", "VIT / Food & care", "FOC / Personal"};
    for (int i = 0; i < 4; i++)
      label(stats, names[i] + "    " + p.stats[i] + " pts", 16, i % 2 == 0 ? GREEN : CYAN);
    label(
        stats,
        "Game points reflect logged habits, not physical or medical measurements. Deleted quests"
            + " count toward FOC.",
        12,
        a.MUTED);
    a.section("ACHIEVEMENTS");
    achievement("FIRST STEP", "Clear your first quest", p.completed, 1);
    achievement("MOMENTUM", "Clear 10 quests", p.completed, 10);
    achievement("CONSISTENCY", "Maintain a 3-day active streak", p.streak, 3);
    achievement("WEEK WARRIOR", "Maintain a 7-day active streak", p.streak, 7);
    achievement("CENTURY", "Clear 100 quests", p.completed, 100);
    LinearLayout ranks = panel();
    label(ranks, "RANK ROADMAP", 16, CYAN);
    label(
        ranks,
        "Rank requires BOTH streak days / streak XP:\n"
            + "E · starting rank\n"
            + "D · 1 day / 100 XP\n"
            + "C · 2 days / 250 XP\n"
            + "B · 3 days / 500 XP\n"
            + "A · 5 days / 900 XP\n"
            + "S · 7 days / 1,500 XP\n"
            + "SS · 10 days / 2,400 XP\n"
            + "SSS · 14 days / 3,600 XP\n"
            + "SSSS · 21 days / 5,000 XP\n\n"
            + "A day with no scheduled quests does not count. Add a light recovery task for rest"
            + " days. Historic clear days are kept; older records without a day snapshot use the"
            + " current routine.",
        14,
        a.TEXT);
    a.actions(
        a.body, "Body progress", () -> a.measurements(), "Training logs", () -> a.logs("training"));
  }

  void achievement(String title, String description, int value, int goal) {
    LinearLayout c = panel();
    label(
        c,
        (value >= goal ? "UNLOCKED / " : "IN PROGRESS / ") + title,
        14,
        value >= goal ? GOLD : a.MUTED);
    label(c, description + "   " + Math.min(value, goal) + "/" + goal, 13, a.TEXT);
    bar(c, Math.min(value, goal), goal, GOLD);
  }

  static class Emblem extends View {
    Paint paint = new Paint(3);
    String rank;

    Emblem(Context context, String rank) {
      super(context);
      this.rank = rank;
      setContentDescription("Player rank " + rank);
    }

    protected void onDraw(Canvas c) {
      float x = getWidth() / 2f, y = getHeight() / 2f, r = Math.min(x, y) - 5;
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(2);
      paint.setColor(CYAN);
      c.drawCircle(x, y, r, paint);
      paint.setColor(GREEN);
      c.drawArc(x - r + 7, y - r + 7, x + r - 7, y + r - 7, -80, 275, false, paint);
      Path hex = new Path();
      for (int i = 0; i < 6; i++) {
        double angle = Math.PI / 3 * i - Math.PI / 2;
        float px = x + (float) Math.cos(angle) * r * .66f,
            py = y + (float) Math.sin(angle) * r * .66f;
        if (i == 0) hex.moveTo(px, py);
        else hex.lineTo(px, py);
      }
      hex.close();
      paint.setColor(0xff29425c);
      paint.setStyle(Paint.Style.FILL);
      c.drawPath(hex, paint);
      paint.setColor(Color.WHITE);
      paint.setTextAlign(Paint.Align.CENTER);
      paint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
      paint.setTextSize(r * (rank.length() > 2 ? .36f : .64f));
      c.drawText(rank, x, y - paint.ascent() / 2 - paint.descent() / 2, paint);
    }
  }
}
