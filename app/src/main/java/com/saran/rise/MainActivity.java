package com.saran.rise;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.*;
import android.graphics.pdf.PdfRenderer;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
  static final int BG = 0xff060e20,
      PANEL = 0xff142039,
      TEXT = 0xffeef5ff,
      MUTED = 0xffa9b9cf,
      LIME = 0xff55b7ff,
      ORANGE = 0xffffcd65;
  Store store;
  JSONObject content;
  JSONObject media = new JSONObject();
  final android.util.LruCache<String, Bitmap> pictures =
      new android.util.LruCache<String, Bitmap>(12 * 1024 * 1024) {
        protected int sizeOf(String key, Bitmap value) {
          return value.getByteCount();
        }
      };
  LinearLayout root, body, nav;
  int tab = 0;
  String filter = "All";
  boolean showCompleted = false;
  Handler handler = new Handler();
  TextView timerText;
  TextView resetText, workoutClock, restClock;
  Runnable eventRefresh;
  LocalDate displayedDate;
  boolean resumed = false;
  final String[] cats = {"Workout", "Care", "Food", "Career", "Personal"};

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    store = new Store(this);
    try {
      content = new JSONObject(read(getAssets().open("content.json")));
      media = new JSONObject(read(getAssets().open("media.json")));
    } catch (Exception e) {
      content = new JSONObject();
    }
    tab = state == null ? 0 : state.getInt("tab", 0);
    Reminders.all(this);
    render();
    if (!store.prefs.getBoolean("welcomed", false)) {
      store.prefs.edit().putBoolean("welcomed", true).apply();
      new AlertDialog.Builder(this)
          .setTitle("Welcome to your next chapter")
          .setMessage(
              "Your routine starts at 07:00. Everything is editable.\n\n"
                  + "Built for you: age 20, 65 kg, mixed Indian diet. Height is assumed to be 165"
                  + " cm.\n\n"
                  + "Allow notifications for reminders, then open More to test delivery. Your"
                  + " information stays on this phone.")
          .setPositiveButton("Let's begin", (d, w) -> notificationPermission())
          .setNegativeButton("Later", null)
          .show();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    resumed = true;
    Reminders.all(this);
    render();
    handler.post(tick);
  }

  @Override
  protected void onPause() {
    super.onPause();
    resumed = false;
    handler.removeCallbacks(tick);
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    out.putInt("tab", tab);
  }

  @Override
  protected void onNewIntent(Intent i) {
    super.onNewIntent(i);
    setIntent(i);
    tab = 0;
    render();
  }

  @Override
  public void onBackPressed() {
    if (tab != 0) {
      tab = 0;
      render();
    } else super.onBackPressed();
  }

  int dp(float v) {
    return (int) (v * getResources().getDisplayMetrics().density + .5f);
  }

  GradientDrawable bg(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  TextView text(String s, int size, int color) {
    TextView v = new TextView(this);
    v.setText(s);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setLineSpacing(dp(2), 1);
    return v;
  }

  TextView heading(String s, int size) {
    TextView t = text(s, size, TEXT);
    t.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
    return t;
  }

  void addText(LinearLayout parent, String s, int size, int color) {
    TextView t = text(s, size, color);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(8);
    parent.addView(t, p);
  }

  LinearLayout column() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.VERTICAL);
    return v;
  }

  LinearLayout row() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.HORIZONTAL);
    v.setGravity(Gravity.CENTER_VERTICAL);
    return v;
  }

  TextView button(String s, boolean primary, Runnable action) {
    TextView v = text(s, 14, primary ? BG : TEXT);
    v.setTypeface(null, Typeface.BOLD);
    v.setGravity(Gravity.CENTER);
    v.setMinHeight(dp(48));
    v.setPadding(dp(12), dp(10), dp(12), dp(10));
    v.setBackground(
        new RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x33FFFFFF),
            bg(primary ? LIME : 0xff1c314b, 10),
            null));
    v.setClickable(true);
    v.setFocusable(true);
    v.setOnClickListener(x -> action.run());
    return v;
  }

  void actions(LinearLayout parent, String a, Runnable ar, String b, Runnable br) {
    LinearLayout r = row();
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
    lp.setMargins(0, dp(5), dp(5), dp(5));
    r.addView(button(a, true, ar), lp);
    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -2, 1);
    rp.setMargins(dp(5), dp(5), 0, dp(5));
    r.addView(button(b, false, br), rp);
    parent.addView(r);
  }

  LinearLayout card() {
    LinearLayout c = column();
    c.setPadding(dp(17), dp(17), dp(17), dp(17));
    c.setBackground(new HudDrawable(getResources().getDisplayMetrics().density));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(12);
    body.addView(c, p);
    return c;
  }

  void space(LinearLayout p, int n) {
    Space s = new Space(this);
    p.addView(s, new LinearLayout.LayoutParams(1, dp(n)));
  }

  void section(String s) {
    TextView t = heading(s, 23);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, dp(17), 0, dp(12));
    body.addView(t, p);
  }

  void title(String eyebrow, String h, String sub) {
    addText(body, eyebrow.toUpperCase(Locale.US), 11, LIME);
    TextView t = heading(h, 35);
    body.addView(t);
    space(body, 8);
    addText(body, sub, 14, MUTED);
    space(body, 12);
  }

  void render() {
    timerText = null;
    resetText = null;
    root = column();
    root.setBackground(
        new GradientDrawable(
            GradientDrawable.Orientation.TL_BR, new int[] {0xff06182b, 0xff060d21, 0xff140c24}));
    root.setOnApplyWindowInsetsListener(
        (v, i) -> {
          v.setPadding(
              i.getSystemWindowInsetLeft(),
              i.getSystemWindowInsetTop(),
              i.getSystemWindowInsetRight(),
              i.getSystemWindowInsetBottom());
          return i.consumeSystemWindowInsets();
        });
    setContentView(root);
    root.requestApplyInsets();
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    body = column();
    body.setPadding(dp(20), dp(22), dp(20), dp(20));
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    nav = row();
    nav.setPadding(dp(6), dp(8), dp(6), dp(8));
    nav.setBackgroundColor(BG);
    String[] names = {"⌂\nHome", "✓\nQuests", "▥\nAnalytics", "◇\nProfile"};
    for (int i = 0; i < names.length; i++) {
      final int x = i;
      int selectedTab = tab >= 5 ? 1 : tab == 4 ? 3 : tab;
      TextView n = text(names[i], 12, i == selectedTab ? LIME : MUTED);
      android.text.SpannableString caption = new android.text.SpannableString(names[i]);
      caption.setSpan(new android.text.style.AbsoluteSizeSpan(22, true), 0, 1, 0);
      n.setText(caption);
      n.setGravity(Gravity.CENTER);
      n.setTypeface(null, i == tab ? Typeface.BOLD : Typeface.NORMAL);
      n.setMinHeight(dp(48));
      n.setBackground(bg(i == selectedTab ? PANEL : BG, 13));
      n.setOnClickListener(
          v -> {
            tab = x;
            render();
          });
      n.setFocusable(true);
      n.setContentDescription(names[i] + (i == tab ? ", selected" : ""));
      nav.addView(n, new LinearLayout.LayoutParams(0, -1, 1));
    }
    root.addView(nav, new LinearLayout.LayoutParams(-1, dp(66)));
    if (tab >= 5)
      body.addView(
          button(
              "‹ Quest journal",
              false,
              () -> {
                tab = 1;
                render();
              }));
    switch (tab) {
      case 1:
        new QuestUi(this).hub();
        break;
      case 2:
        new QuestUi(this).analytics();
        break;
      case 3:
        more();
        break;
      case 4:
        more();
        break;
      case 5:
        train();
        break;
      case 6:
        fuel();
        break;
      case 7:
        dreams();
        break;
      default:
        today();
    }
  }

  void today() {
    displayedDate = store.today();
    new QuestUi(this).home();
  }

  void legacyToday() {
    LocalDate d = store.today();
    title(
        d.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH)),
        "Rise, " + store.prefs.getString("name", "Saran") + ".",
        "A stronger body. A sharper mind. One day at a time.");
    ArrayList<Task> tasks = store.tasks();
    int total = 0, done = 0;
    for (Task t : tasks)
      if (t.on(d)) {
        total++;
        if (store.done(t.id, d.toString())) done++;
      }
    LinearLayout hero = card();
    LinearLayout line = row();
    LinearLayout words = column();
    addText(words, "YOUR DAILY MOMENTUM", 11, LIME);
    addText(words, done + " of " + total + " complete", 24, TEXT);
    addText(
        words,
        total > 0 && done == total
            ? "You showed up. Enjoy your evening."
            : "The next small action is enough.",
        13,
        MUTED);
    line.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
    Ring ring = new Ring(total == 0 ? 0 : (float) done / total);
    line.addView(ring, new LinearLayout.LayoutParams(dp(76), dp(76)));
    hero.addView(line);
    space(hero, 12);
    addText(hero, "Working toward " + store.dream(), 14, LIME);
    if (!getSystemService(NotificationManager.class).areNotificationsEnabled()) {
      LinearLayout c = card();
      addText(c, "Reminders need notification access", 16, ORANGE);
      c.addView(button("Enable notifications", false, () -> notificationPermission()));
    }
    actions(body, "+ Add a task", () -> editTask(null), "All routines", () -> allTasks());
    body.addView(button("Focus timer · 25 min or just 5", false, () -> focusDialog()));
    section("Your day");
    int shown = 0;
    for (Task t : tasks)
      if (t.on(d) && !store.done(t.id, d.toString())) {
        taskCard(t, true);
        shown++;
      }
    if (shown == 0)
      addText(
          body,
          total > 0
              ? "All done for today. Your next scheduled day starts fresh."
              : "No tasks for today. Add a one-time task or choose repeat days.",
          15,
          MUTED);
    if (done > 0) {
      body.addView(
          button(
              (showCompleted ? "Hide completed" : "Completed today") + " (" + done + ")",
              false,
              () -> {
                showCompleted = !showCompleted;
                render();
              }));
      if (showCompleted) {
        addText(body, "Uncheck a task to move it back to Your day.", 13, MUTED);
        for (Task t : tasks) if (t.on(d) && store.done(t.id, d.toString())) taskCard(t, true);
      }
    }
    section("Last seven days");
    StringBuilder history = new StringBuilder();
    for (int i = 6; i >= 0; i--) {
      LocalDate x = d.minusDays(i);
      int count = 0, n = 0;
      for (Task t : tasks)
        if (t.on(x)) {
          n++;
          if (store.done(t.id, x.toString())) count++;
        }
      history
          .append(x.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)))
          .append(": ")
          .append(count)
          .append("/")
          .append(n)
          .append(i == 0 ? "" : "   ");
    }
    addText(body, history.toString(), 13, MUTED);
    addText(
        body,
        "History reflects your current routine; editing repeat days can change past totals.",
        11,
        MUTED);
  }

  void focusDialog() {
    LinearLayout f = column();
    addText(f, "FOCUS / START SMALL", 11, LIME);
    timerText = heading("25:00", 32);
    f.addView(timerText);
    addText(
        f,
        "Timer continues counting if you leave. Its finish sound plays only while the app is open.",
        12,
        MUTED);
    actions(
        f,
        "Start 25 min",
        () -> startTimer(25),
        "Reset",
        () -> {
          store.prefs.edit().remove("timerEnd").apply();
          updateTimer();
        });
    f.addView(button("Just 5 minutes", false, () -> startTimer(5)));
    updateTimer();
    dialog("Focus on one thing", f).setPositiveButton("Close", null).show();
  }

  void taskCard(Task t, boolean check) {
    LinearLayout c = card();
    boolean done = store.done(t.id, store.today().toString());
    boolean past =
        store
                .today()
                .atTime(t.minute / 60, t.minute % 60)
                .atZone(store.zone())
                .toInstant()
                .toEpochMilli()
            < System.currentTimeMillis();
    addText(
        c,
        t.time()
            + "  /  "
            + t.category.toUpperCase(Locale.US)
            + (check && past && !done ? "  /  READY WHEN YOU ARE" : ""),
        11,
        done ? MUTED : LIME);
    TextView name = heading(t.title, 21);
    c.addView(name);
    space(c, 6);
    addText(c, t.note, 13, MUTED);
    if (!check) addText(c, repeatLabel(t), 12, ORANGE);
    if (check) {
      CheckBox cb = new CheckBox(this);
      cb.setText(done ? "Completed today" : "Mark complete");
      cb.setTextColor(TEXT);
      cb.setChecked(done);
      cb.setMinHeight(dp(48));
      cb.setOnCheckedChangeListener(
          (b, v) -> {
            store.done(t.id, store.today().toString(), v);
            if (v) Reminders.clearExtras(this, t.id);
            render();
          });
      c.addView(cb);
    }
    actions(
        c,
        "Edit",
        () -> editTask(t),
        "Delete",
        () ->
            new AlertDialog.Builder(this)
                .setTitle("Delete this task?")
                .setMessage(t.title + " will be removed and its reminders cancelled.")
                .setPositiveButton(
                    "Delete",
                    (d, w) -> {
                      Reminders.cancel(this, t.id);
                      store.delete(t.id);
                      render();
                    })
                .setNegativeButton("Keep", null)
                .show());
  }

  String repeatLabel(Task t) {
    if (t.days == 0) return "Once: " + t.date;
    String[] ds = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    StringBuilder s = new StringBuilder("Repeats: ");
    for (int j = 0; j < 7; j++) if ((t.days & (1 << j)) != 0) s.append(ds[j]).append(" ");
    return s.toString();
  }

  void allTasks() {
    LinearLayout list = column();
    for (Task t : store.tasks()) {
      TextView b =
          button(t.time() + "  " + t.title + "\n" + repeatLabel(t), false, () -> editTask(t));
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
      p.bottomMargin = dp(8);
      list.addView(b, p);
    }
    dialog("All routines", list).setPositiveButton("Close", null).show();
  }

  EditText field(LinearLayout parent, String label, String value, int type) {
    TextView fieldLabel = text(label, 13, MUTED);
    parent.addView(fieldLabel);
    EditText e = new EditText(this);
    e.setId(View.generateViewId());
    fieldLabel.setLabelFor(e.getId());
    e.setTextColor(TEXT);
    e.setTextSize(16);
    e.setInputType(type);
    e.setText(value);
    e.setSelectAllOnFocus(true);
    e.setMinHeight(dp(48));
    parent.addView(e, new LinearLayout.LayoutParams(-1, -2));
    space(parent, 10);
    return e;
  }

  AlertDialog.Builder dialog(String title, LinearLayout inner) {
    ScrollView s = new ScrollView(this);
    inner.setPadding(dp(20), dp(10), dp(20), dp(16));
    s.addView(inner);
    return new AlertDialog.Builder(this).setTitle(title).setView(s);
  }

  void error(String msg) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
  }

  void editTask(Task old) {
    LinearLayout f = column();
    EditText name =
        field(
            f,
            "Task name",
            old == null ? "" : old.title,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    EditText note =
        field(
            f,
            "Instructions / your reason",
            old == null ? "" : old.note,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    addText(f, "Category", 13, MUTED);
    Spinner cat = new Spinner(this);
    cat.setAdapter(
        new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, cats));
    if (old != null) cat.setSelection(Math.max(0, Arrays.asList(cats).indexOf(old.category)));
    f.addView(cat);
    final int[] minute = {old == null ? 9 * 60 : old.minute};
    TextView time =
        button(
            String.format(Locale.US, "Time: %02d:%02d", minute[0] / 60, minute[0] % 60),
            false,
            () -> {});
    time.setOnClickListener(
        v ->
            new TimePickerDialog(
                    this,
                    (p, h, m) -> {
                      minute[0] = h * 60 + m;
                      time.setText(String.format(Locale.US, "Time: %02d:%02d", h, m));
                    },
                    minute[0] / 60,
                    minute[0] % 60,
                    true)
                .show());
    f.addView(time);
    space(f, 12);
    EditText duration =
        field(
            f,
            "Duration (minutes)",
            "" + (old == null ? 20 : old.duration),
            InputType.TYPE_CLASS_NUMBER);
    Switch recurring = new Switch(this);
    recurring.setText("Repeat on selected days");
    recurring.setTextColor(TEXT);
    recurring.setMinHeight(dp(48));
    recurring.setChecked(old == null || old.days != 0);
    f.addView(recurring);
    LinearLayout daysBox = column();
    CheckBox[] boxes = new CheckBox[7];
    String[] ds = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    for (int j = 0; j < 7; j++) {
      boxes[j] = new CheckBox(this);
      boxes[j].setText(ds[j]);
      boxes[j].setTextColor(TEXT);
      boxes[j].setChecked(old == null || (old.days & (1 << j)) != 0);
      daysBox.addView(boxes[j]);
    }
    f.addView(daysBox);
    actions(
        f,
        "Every day",
        () -> {
          recurring.setChecked(true);
          for (CheckBox box : boxes) box.setChecked(true);
        },
        "Weekdays",
        () -> {
          recurring.setChecked(true);
          for (int j = 0; j < boxes.length; j++) boxes[j].setChecked(j < 5);
        });
    final LocalDate[] date = {
      old != null && old.days == 0 ? LocalDate.parse(old.date) : store.today()
    };
    TextView dateButton = button("Date: " + date[0], false, () -> {});
    dateButton.setOnClickListener(
        v ->
            new DatePickerDialog(
                    this,
                    (p, y, m, d) -> {
                      date[0] = LocalDate.of(y, m + 1, d);
                      dateButton.setText("Date: " + date[0]);
                    },
                    date[0].getYear(),
                    date[0].getMonthValue() - 1,
                    date[0].getDayOfMonth())
                .show());
    f.addView(dateButton);
    daysBox.setVisibility(recurring.isChecked() ? View.VISIBLE : View.GONE);
    dateButton.setVisibility(recurring.isChecked() ? View.GONE : View.VISIBLE);
    recurring.setOnCheckedChangeListener(
        (b, v) -> {
          daysBox.setVisibility(v ? View.VISIBLE : View.GONE);
          dateButton.setVisibility(v ? View.GONE : View.VISIBLE);
        });
    Switch reminder = new Switch(this);
    reminder.setText("Alarm at selected time");
    reminder.setTextColor(TEXT);
    reminder.setMinHeight(dp(48));
    reminder.setChecked(old == null || old.reminder);
    f.addView(reminder);
    addText(
        f,
        "Plays an alarm notification at this time on each selected day. Enable notifications and"
            + " Precise timing in More. Completed tasks stay quiet for that day.",
        12,
        MUTED);
    AlertDialog a =
        dialog(old == null ? "Add a task" : "Edit task", f)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create();
    a.setOnShowListener(
        x ->
            a.getButton(-1)
                .setOnClickListener(
                    v -> {
                      String n = name.getText().toString().trim();
                      if (n.isEmpty() || n.length() > 150) {
                        name.setError("Use 1-150 characters");
                        return;
                      }
                      int dur;
                      try {
                        dur = Integer.parseInt(duration.getText().toString());
                        if (dur < 1 || dur > 600) throw new Exception();
                      } catch (Exception e) {
                        duration.setError("Use 1-600 minutes");
                        return;
                      }
                      int mask = 0;
                      if (recurring.isChecked()) {
                        for (int j = 0; j < 7; j++) if (boxes[j].isChecked()) mask |= 1 << j;
                        if (mask == 0) {
                          error("Select at least one repeat day.");
                          return;
                        }
                      } else if (date[0].isBefore(store.today())) {
                        error("Choose today or a future date.");
                        return;
                      }
                      Task t =
                          new Task(
                              old == null ? store.nextId() : old.id,
                              n,
                              cat.getSelectedItem().toString(),
                              note.getText().toString(),
                              minute[0],
                              mask,
                              dur);
                      t.date = mask == 0 ? date[0].toString() : "";
                      t.reminder = reminder.isChecked();
                      Reminders.cancel(this, t.id);
                      store.save(t);
                      Reminders.schedule(this, t);
                      a.dismiss();
                      render();
                      if (t.reminder
                          && t.days == 0
                          && t.next(System.currentTimeMillis(), store.zone()) < 0)
                        error("Saved. This time has passed, so no reminder is scheduled.");
                    }));
    a.show();
  }

  Bitmap loadPicture(String file) throws IOException {
    Bitmap cached = pictures.get(file);
    if (cached != null) return cached;
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    try (InputStream in = getAssets().open("rise_visuals/" + file)) {
      BitmapFactory.decodeStream(in, null, options);
    }
    options.inSampleSize = 1;
    while (options.outWidth / options.inSampleSize > 1000) options.inSampleSize *= 2;
    options.inJustDecodeBounds = false;
    try (InputStream in = getAssets().open("rise_visuals/" + file)) {
      cached = BitmapFactory.decodeStream(in, null, options);
    }
    if (cached == null) throw new IOException("Invalid image");
    pictures.put(file, cached);
    return cached;
  }

  void picture(LinearLayout parent, String file, String description, int height) {
    try {
      ImageView image = new ImageView(this);
      image.setImageBitmap(loadPicture(file));
      image.setScaleType(ImageView.ScaleType.FIT_CENTER);
      image.setContentDescription(description + ". Tap to enlarge.");
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(height));
      lp.setMargins(0, dp(10), 0, dp(10));
      parent.addView(image, lp);
      image.setOnClickListener(
          v -> {
            LinearLayout preview = column();
            try {
              ImageView large = new ImageView(this);
              large.setImageBitmap(loadPicture(file));
              large.setAdjustViewBounds(true);
              large.setContentDescription(description);
              preview.addView(large, new LinearLayout.LayoutParams(-1, -2));
              addText(preview, description, 14, MUTED);
              dialog("Movement & meal reference", preview).setPositiveButton("Close", null).show();
            } catch (IOException e) {
              error("Image could not be opened.");
            }
          });
    } catch (IOException e) {
      addText(parent, description, 13, MUTED);
    }
  }

  void watchVideo(String id) {
    if (!id.matches("[A-Za-z0-9_-]{11}")) {
      error("Tutorial link is unavailable.");
      return;
    }
    try {
      startActivity(
          new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + id)));
    } catch (ActivityNotFoundException e) {
      error("Install YouTube or a browser to open this tutorial.");
    }
  }

  void train() {
    title(
        "Home / 2-5 kg dumbbells",
        "Earn your strength.",
        "Three full-body sessions. Arms and abs included. Recovery counts too.");
    LinearLayout intro = card();
    addText(intro, "12 WEEKS / START WITH CONTROL", 11, LIME);
    addText(
        intro,
        "Weeks 1-2: 2 sets each. Rest 60-90 seconds. Finish with 2-3 good reps left.",
        16,
        TEXT);
    addText(
        intro,
        "Warm up: 2 min marching, 8 squats, 8 hip hinges, shoulder circles, 6 wall push-ups. In"
            + " weeks 3-12, add a third set to the first four moves only if recovered.",
        14,
        MUTED);
    intro.addView(button("Read the full offline guide", false, () -> guide()));
    JSONArray ws = content.optJSONArray("workouts");
    if (ws != null)
      for (int i = 0; i < ws.length(); i++) {
        final JSONObject w = ws.optJSONObject(i);
        LinearLayout c = card();
        picture(
            c, media.optJSONArray("workoutCovers").optString(i), "Workout movement preview", 175);
        addText(c, w.optString("day").toUpperCase(Locale.US) + " / 30-45 MIN", 11, LIME);
        addText(c, w.optString("name"), 24, TEXT);
        addText(c, "6 movements · full body · no gym", 13, MUTED);
        c.addView(button("Open workout", true, () -> workout(w)));
      }
    section("Recovery days");
    LinearLayout c = card();
    addText(c, "Tue / Thu / Sat", 18, TEXT);
    addText(
        c,
        "20-30 minutes of brisk indoor walking or low-impact dance; build up from 10-15 minutes if"
            + " needed. Sunday: easy movement. Build toward 150 minutes of moderate movement per"
            + " week.",
        14,
        MUTED);
    addText(
        c,
        "If you have one of each weight, train one side at a time. Avoid unequal weights together."
            + " Stop for sharp pain or dizziness.",
        13,
        ORANGE);
    actions(
        body, "Exercise history", () -> logs("training"), "Body progress", () -> measurements());
  }

  void workout(JSONObject w) {
    LinearLayout f = column();
    f.addView(button("Workout & rest timer", true, () -> workoutTimer(w.optString("name"))));
    addText(
        f,
        "Warm up first. The listed prescription is for weeks 1-2. In later weeks, add a third set"
            + " to the first four exercises when recovered. All weights are starting suggestions.",
        14,
        MUTED);
    JSONArray ex = w.optJSONArray("exercises");
    for (int j = 0; j < ex.length(); j++) {
      JSONArray e = ex.optJSONArray(j);
      String name = e.optString(0), reps = e.optString(1), cue = e.optString(2);
      addText(f, name, 21, TEXT);
      addText(f, reps, 15, LIME);
      JSONObject visual = media.optJSONObject("exercises").optJSONObject(name);
      if (visual != null) {
        JSONArray images = visual.optJSONArray("images");
        for (int k = 0; k < images.length(); k++)
          picture(
              f,
              images.optString(k),
              visual.optString("alt") + " / image " + (k + 1),
              images.length() == 1 ? 210 : 190);
        addText(f, visual.optString("note"), 12, MUTED);
        addText(f, visual.optString("channel") + " / " + visual.optString("title"), 12, LIME);
        f.addView(button("Watch on YouTube", true, () -> watchVideo(visual.optString("video"))));
      }
      addText(f, cue, 14, MUTED);
      f.addView(button("Log " + name, false, () -> logExercise(name)));
      space(f, 20);
    }
    addText(
        f,
        "When you reach the top rep range with good form twice, increase weight if available, then"
            + " reset reps. At 5 kg, try slower lowering before adding more volume.",
        14,
        ORANGE);
    dialog(w.optString("name"), f).setPositiveButton("Close", null).show();
  }

  void logExercise(String name) {
    LinearLayout f = column();
    EditText sets = field(f, "Sets completed", "2", InputType.TYPE_CLASS_NUMBER);
    EditText reps =
        field(f, "Reps / seconds per set (example: 12, 12)", "", InputType.TYPE_CLASS_TEXT);
    EditText weight =
        field(
            f,
            "Dumbbell kg (0 for body weight)",
            "0",
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    EditText notes =
        field(
            f,
            "How it felt / any variation",
            "",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    AlertDialog a =
        dialog(name, f)
            .setPositiveButton("Save log", null)
            .setNegativeButton("Cancel", null)
            .create();
    a.setOnShowListener(
        x ->
            a.getButton(-1)
                .setOnClickListener(
                    v -> {
                      try {
                        int n = Integer.parseInt(sets.getText().toString());
                        double kg = Double.parseDouble(weight.getText().toString());
                        if (n < 1
                            || n > 20
                            || kg < 0
                            || kg > 100
                            || !Double.isFinite(kg)
                            || reps.getText().toString().trim().isEmpty()) throw new Exception();
                        JSONObject o = new JSONObject();
                        o.put("title", name)
                            .put("date", store.today().toString())
                            .put(
                                "detail",
                                n
                                    + " sets · "
                                    + reps.getText()
                                    + " reps/sec · "
                                    + kg
                                    + " kg\n"
                                    + notes.getText());
                        JSONArray logs = store.array("training");
                        logs.put(o);
                        store.array("training", logs);
                        a.dismiss();
                        error("Exercise logged.");
                      } catch (Exception e) {
                        error("Enter 1-20 sets, repetitions and a valid weight.");
                      }
                    }));
    a.show();
  }

  void logs(String key) {
    LinearLayout f = column();
    JSONArray a = store.array(key);
    if (a.length() == 0)
      addText(f, "No logs yet. Your first entry is a starting point.", 15, MUTED);
    for (int i = a.length() - 1; i >= 0; i--) {
      final int ix = i;
      JSONObject o = a.optJSONObject(i);
      if (o == null) continue;
      addText(f, o.optString("date") + " / " + o.optString("title"), 17, TEXT);
      addText(f, o.optString("detail"), 14, MUTED);
      f.addView(
          button(
              "Delete entry",
              false,
              () ->
                  new AlertDialog.Builder(this)
                      .setTitle("Delete this entry?")
                      .setPositiveButton(
                          "Delete",
                          (d, w) -> {
                            JSONArray current = store.array(key);
                            boolean found = false;
                            for (int j = 0; j < current.length(); j++) {
                              if (current.optJSONObject(j) != null
                                  && current.optJSONObject(j).toString().equals(o.toString())) {
                                current.remove(j);
                                found = true;
                                break;
                              }
                            }
                            store.array(key, current);
                            error(
                                found
                                    ? "Entry deleted. Reopen history to refresh."
                                    : "This entry was already deleted.");
                          })
                      .setNegativeButton("Cancel", null)
                      .show()));
      space(f, 14);
    }
    dialog("Progress history", f).setPositiveButton("Close", null).show();
  }

  void fuel() {
    title(
        "Indian food / daily rhythm",
        "Fuel your progress.",
        "Regular meals, enough protein, room for foods you enjoy.");
    LinearLayout c = card();
    addText(c, "YOUR STARTER TARGET", 11, LIME);
    addText(c, proteinTarget(), 28, TEXT);
    addText(
        c,
        "Food first. No crash diet. Adjust portions using energy, strength and 3-4 week trends.",
        14,
        MUTED);
    showArticles(body, "fuel");
  }

  String proteinTarget() {
    double kg = store.prefs.getFloat("weight", 65f);
    return Math.round(kg * 1.4) + "-" + Math.round(kg * 1.6) + " g protein / day";
  }

  void showArticles(LinearLayout target, String key) {
    JSONArray a = content.optJSONArray(key);
    if (a == null) return;
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.optJSONObject(i);
      LinearLayout c;
      if (target == body) c = card();
      else {
        c = column();
        target.addView(c);
        space(target, 16);
      }
      addText(c, o.optString("title"), 21, TEXT);
      if (key.equals("fuel") && i < media.optJSONArray("food").length()) {
        JSONObject food = media.optJSONArray("food").optJSONObject(i);
        picture(c, food.optString("image"), food.optString("alt"), 205);
        addText(c, food.optString("caption"), 12, LIME);
      }
      String b = o.optString("body");
      if (key.equals("fuel") && i == 0)
        b =
            "For a healthy adult doing this training, a practical starting range is about 1.4-1.6"
                + " g/kg per day. Your current profile gives "
                + proteinTarget()
                + ". Spread protein across meals. If you have kidney disease or a prescribed diet,"
                + " ask your clinician before raising protein.";
      addText(c, b, 14, MUTED);
    }
  }

  void dreams() {
    title(
        "Dream big / act daily",
        "Your reason to rise.",
        "Make the dream personal. Give it a next step.");
    body.addView(button("+ Add a dream", true, () -> editDream(-1)));
    space(body, 18);
    JSONArray a = store.array("dreams");
    if (a.length() == 0)
      addText(
          body,
          "Add a dream: a job, a skill, a trip or something you want to save for.",
          16,
          MUTED);
    for (int i = 0; i < a.length(); i++) {
      final int ix = i;
      JSONObject o = a.optJSONObject(i);
      LinearLayout c = card();
      addText(c, o.optBoolean("achieved") ? "ACHIEVED" : "IN PROGRESS", 11, LIME);
      addText(c, o.optString("title"), 25, TEXT);
      double target = o.optDouble("target"), saved = o.optDouble("saved");
      addText(
          c,
          target > 0
              ? "₹" + money(saved) + " saved of ₹" + money(target)
              : "Set a budget when you're ready",
          15,
          ORANGE);
      if (target > 0) {
        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(1000);
        p.setProgress((int) Math.min(1000, saved / target * 1000));
        p.setProgressTintList(android.content.res.ColorStateList.valueOf(LIME));
        c.addView(p);
        space(c, 10);
      }
      addText(c, o.optString("note"), 14, MUTED);
      actions(
          c,
          "Edit / savings",
          () -> editDream(ix),
          "Delete",
          () ->
              new AlertDialog.Builder(this)
                  .setTitle("Delete this dream?")
                  .setPositiveButton(
                      "Delete",
                      (d, w) -> {
                        JSONArray ds = store.array("dreams");
                        ds.remove(ix);
                        store.array("dreams", ds);
                        render();
                      })
                  .setNegativeButton("Keep", null)
                  .show());
    }
    addText(
        body,
        "Budgets and savings are manual. Future product names are wishes; availability and prices"
            + " are not assumed.",
        12,
        MUTED);
  }

  String money(double d) {
    return String.format(new Locale("en", "IN"), "%,.0f", d);
  }

  void editDream(int ix) {
    JSONArray ds = store.array("dreams");
    JSONObject o = ix >= 0 ? ds.optJSONObject(ix) : new JSONObject();
    LinearLayout f = column();
    EditText title = field(f, "Dream / goal", o.optString("title"), InputType.TYPE_CLASS_TEXT);
    EditText target =
        field(
            f,
            "Target budget in INR (0 for a non-money goal)",
            "" + o.optDouble("target", 0),
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    EditText saved =
        field(
            f,
            "Saved so far in INR",
            "" + o.optDouble("saved", 0),
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    EditText note =
        field(
            f,
            "Why it matters / next step / target date",
            o.optString("note"),
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    CheckBox done = new CheckBox(this);
    done.setText("Achieved");
    done.setTextColor(TEXT);
    done.setChecked(o.optBoolean("achieved"));
    f.addView(done);
    AlertDialog a =
        dialog(ix < 0 ? "New dream" : "Edit dream", f)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create();
    a.setOnShowListener(
        x ->
            a.getButton(-1)
                .setOnClickListener(
                    v -> {
                      try {
                        String name = title.getText().toString().trim();
                        double t = Double.parseDouble(target.getText().toString()),
                            s = Double.parseDouble(saved.getText().toString());
                        if (name.isEmpty()
                            || name.length() > 150
                            || t < 0
                            || s < 0
                            || !Double.isFinite(t)
                            || !Double.isFinite(s)
                            || t > 1e10
                            || s > 1e10) throw new Exception();
                        JSONObject n = new JSONObject();
                        n.put("title", name)
                            .put("target", t)
                            .put("saved", s)
                            .put("note", note.getText().toString())
                            .put("achieved", done.isChecked());
                        JSONArray current = store.array("dreams");
                        if (ix < 0) current.put(n);
                        else current.put(ix, n);
                        store.array("dreams", current);
                        a.dismiss();
                        render();
                      } catch (Exception e) {
                        error("Add a name and valid non-negative amounts.");
                      }
                    }));
    a.show();
  }

  void more() {
    title(
        "Your system / your rules",
        "Make it yours.",
        "Edit your routine, protect your progress, and keep it simple.");
    LinearLayout c = card();
    addText(
        c,
        store.prefs.getString("name", "Saran") + " / age " + store.prefs.getInt("age", 20),
        22,
        TEXT);
    addText(
        c,
        store.prefs.getFloat("weight", 65f)
            + " kg · "
            + store.prefs.getFloat("height", 165f)
            + " cm · "
            + store.prefs.getString("zone", "Asia/Kolkata"),
        14,
        MUTED);
    c.addView(button("Edit profile", true, () -> profile()));
    section("Your guides");
    actions(
        body,
        "Face care",
        () -> articlesDialog("Face care", "care"),
        "Career plan",
        () -> articlesDialog("Skills & career", "career"));
    actions(body, "Full PDF guide", () -> guide(), "Body progress", () -> measurements());
    section("Reminders");
    LinearLayout r = card();
    Switch enabled = new Switch(this);
    enabled.setText("Routine reminders");
    enabled.setTextColor(TEXT);
    enabled.setMinHeight(dp(48));
    enabled.setChecked(store.prefs.getBoolean("reminders", true));
    enabled.setOnCheckedChangeListener(
        (b, v) -> {
          store.prefs.edit().putBoolean("reminders", v).apply();
          Reminders.all(this);
          if (v) notificationPermission();
        });
    r.addView(enabled);
    Switch nudge = new Switch(this);
    nudge.setText("One gentle follow-up after 30 min");
    nudge.setTextColor(TEXT);
    nudge.setMinHeight(dp(48));
    nudge.setChecked(store.prefs.getBoolean("nudge", true));
    nudge.setOnCheckedChangeListener(
        (b, v) -> {
          store.prefs.edit().putBoolean("nudge", v).apply();
          if (!v)
            for (Task t : store.tasks())
              getSystemService(AlarmManager.class)
                  .cancel(Reminders.pending(this, t.id, "NUDGE", ""));
        });
    r.addView(nudge);
    addText(
        r,
        "Follow-ups only between 07:00 and 22:45. Done cancels the follow-up; Snooze delays by 10"
            + " minutes.",
        13,
        MUTED);
    boolean exact =
        Build.VERSION.SDK_INT < 31 || getSystemService(AlarmManager.class).canScheduleExactAlarms();
    addText(
        r,
        "Notifications: "
            + (getSystemService(NotificationManager.class).areNotificationsEnabled()
                ? "allowed"
                : "off")
            + "\nTiming: "
            + (exact ? "precise access enabled" : "approximate until precise access is allowed"),
        14,
        ORANGE);
    actions(
        r,
        "Notifications",
        () -> notificationPermission(),
        "Precise timing",
        () -> exactPermission());
    actions(
        r,
        "Test now",
        () -> {
          Task t =
              new Task(
                  99999,
                  "You are ready to rise",
                  "Personal",
                  "Your reminder channel works. Take one small step toward " + store.dream() + ".",
                  0,
                  127,
                  5);
          Reminders.show(this, t, store.today().toString(), false);
          error("Test notification sent. Check your notification shade.");
        },
        "Test in 1 min",
        () -> testLater());
    r.addView(
        button(
            "Open phone app settings",
            false,
            () ->
                startActivity(
                    new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())))));
    addText(
        r,
        "Notifications can be delayed or silent because of battery limits, Do Not Disturb or phone"
            + " settings. After force-stop, reopen Rise. This app cannot detect laziness or"
            + " guarantee a wake-up alarm.",
        12,
        MUTED);
    section("Your data");
    actions(body, "Export backup", () -> exportBackup(), "Restore backup", () -> importBackup());
    LinearLayout privacy = card();
    addText(privacy, "Offline by design", 20, TEXT);
    addText(
        privacy,
        "No account, ads, internet permission or photo uploads. Your photos are not in this app."
            + " Back up before uninstalling; uninstall removes local data. Backups contain personal"
            + " information. App edits do not sync to Google Calendar.",
        14,
        MUTED);
    privacy.addView(
        button(
            "Images & tutorial credits",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Images & tutorials")
                    .setMessage(media.optString("credits"))
                    .setPositiveButton("Close", null)
                    .show()));
    addText(privacy, "Rise 2.1 · Streak edition · Java · Android 8+", 12, LIME);
  }

  void articlesDialog(String title, String key) {
    LinearLayout f = column();
    showArticles(f, key);
    dialog(title, f).setPositiveButton("Close", null).show();
  }

  void notificationPermission() {
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 42);
    } else
      startActivity(
          new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
              .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
  }

  void exactPermission() {
    if (Build.VERSION.SDK_INT >= 31) {
      try {
        startActivity(
            new Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:" + getPackageName())));
      } catch (Exception e) {
        error("Use your phone's Alarms & reminders settings.");
      }
    } else error("Precise reminders are available on this Android version.");
  }

  void testLater() {
    LocalDateTime later = LocalDateTime.now(store.zone()).plusMinutes(1);
    Task t =
        new Task(
            store.nextId(),
            "Reminder delivery test",
            "Personal",
            "This test is saved as a one-time task. Mark it done, then delete it from All"
                + " routines.",
            later.getHour() * 60 + later.getMinute(),
            0,
            1);
    t.date = later.toLocalDate().toString();
    store.save(t);
    Reminders.schedule(this, t);
    error(
        "Test set for " + t.time() + ". Leave the app open in the background and check delivery.");
  }

  void profile() {
    LinearLayout f = column();
    EditText name =
        field(f, "Name", store.prefs.getString("name", "Saran"), InputType.TYPE_CLASS_TEXT);
    EditText age =
        field(
            f, "Age (adult plan)", "" + store.prefs.getInt("age", 20), InputType.TYPE_CLASS_NUMBER);
    EditText kg =
        field(
            f,
            "Weight in kg",
            "" + store.prefs.getFloat("weight", 65),
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    EditText cm =
        field(
            f,
            "Height in cm (5 ft 5 in is about 165)",
            "" + store.prefs.getFloat("height", 165),
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    EditText zone =
        field(
            f,
            "Reminder timezone (e.g. Asia/Kolkata)",
            store.prefs.getString("zone", "Asia/Kolkata"),
            InputType.TYPE_CLASS_TEXT);
    addText(
        f,
        "This starter guide is for adults. A new weight updates the protein estimate, not the"
            + " original PDF or meal portions. It does not create a medical prescription.",
        13,
        MUTED);
    AlertDialog a =
        dialog("Your profile", f)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create();
    a.setOnShowListener(
        x ->
            a.getButton(-1)
                .setOnClickListener(
                    v -> {
                      try {
                        String n = name.getText().toString().trim(),
                            z = zone.getText().toString().trim();
                        int years = Integer.parseInt(age.getText().toString());
                        float k = Float.parseFloat(kg.getText().toString()),
                            h = Float.parseFloat(cm.getText().toString());
                        if (n.isEmpty()
                            || n.length() > 50
                            || years < 18
                            || years > 100
                            || k < 30
                            || k > 300
                            || h < 100
                            || h > 230
                            || !Float.isFinite(k)
                            || !Float.isFinite(h)) throw new Exception();
                        ZoneId.of(z);
                        store
                            .prefs
                            .edit()
                            .putString("name", n)
                            .putInt("age", years)
                            .putFloat("weight", k)
                            .putFloat("height", h)
                            .putString("zone", z)
                            .commit();
                        for (Task t : store.tasks()) Reminders.clearExtras(this, t.id);
                        Reminders.all(this);
                        a.dismiss();
                        render();
                      } catch (Exception e) {
                        error(
                            "Check name, adult age, measurements and timezone (for example"
                                + " Asia/Kolkata).");
                      }
                    }));
    a.show();
  }

  void measurements() {
    LinearLayout f = column();
    addText(
        f,
        "Weekly trends matter more than one number. These are optional; strength and energy are"
            + " progress too.",
        14,
        MUTED);
    EditText kg =
        field(
            f,
            "Weight kg (optional)",
            "",
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    EditText waist =
        field(
            f,
            "Waist cm (optional)",
            "",
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    EditText note =
        field(
            f,
            "Strength, sleep, energy or notes",
            "",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    f.addView(button("View history", false, () -> logs("measurements")));
    AlertDialog a =
        dialog("Body progress", f)
            .setPositiveButton("Save entry", null)
            .setNegativeButton("Close", null)
            .create();
    a.setOnShowListener(
        x ->
            a.getButton(-1)
                .setOnClickListener(
                    v -> {
                      try {
                        String k = kg.getText().toString().trim(),
                            w = waist.getText().toString().trim(),
                            n = note.getText().toString().trim();
                        if (k.isEmpty() && w.isEmpty() && n.isEmpty()) throw new Exception();
                        if (!k.isEmpty()) {
                          float val = Float.parseFloat(k);
                          if (!Float.isFinite(val) || val < 30 || val > 300) throw new Exception();
                        }
                        if (!w.isEmpty()) {
                          float val = Float.parseFloat(w);
                          if (!Float.isFinite(val) || val < 30 || val > 250) throw new Exception();
                        }
                        JSONObject o = new JSONObject();
                        o.put("title", "Body & wellbeing")
                            .put("date", store.today().toString())
                            .put(
                                "detail",
                                (k.isEmpty() ? "" : "Weight: " + k + " kg\n")
                                    + (w.isEmpty() ? "" : "Waist: " + w + " cm\n")
                                    + n);
                        JSONArray all = store.array("measurements");
                        all.put(o);
                        store.array("measurements", all);
                        a.dismiss();
                        error("Progress saved.");
                      } catch (Exception e) {
                        error("Add a valid measurement or a note.");
                      }
                    }));
    a.show();
  }

  final Runnable tick =
      new Runnable() {
        public void run() {
          if (resumed) {
            updateTimer();
            refreshReset();
            updateWorkoutClocks();
            if (eventRefresh != null) eventRefresh.run();
            handler.postDelayed(this, 1000);
          }
        }
      };

  void startTimer(int min) {
    store.prefs.edit().putLong("timerEnd", System.currentTimeMillis() + min * 60000L).apply();
    updateTimer();
  }

  void refreshReset() {
    if (resetText == null) return;
    if (displayedDate != null && !displayedDate.equals(store.today())) {
      showCompleted = false;
      render();
      return;
    }
    long seconds =
        Math.max(
            0,
            java.time.Duration.between(
                    ZonedDateTime.now(store.zone()),
                    store.today().plusDays(1).atStartOfDay(store.zone()))
                .getSeconds());
    resetText.setText(
        String.format(
            Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60));
  }

  long workoutMillis() {
    long elapsed = store.prefs.getLong("workoutElapsed", 0);
    long start = store.prefs.getLong("workoutStart", 0);
    return Math.max(0, elapsed + (start == 0 ? 0 : System.currentTimeMillis() - start));
  }

  void workoutTimer(String workout) {
    LinearLayout f = column();
    if (store.prefs.getString("workoutName", "").isEmpty())
      store.prefs.edit().putString("workoutName", workout).apply();
    addText(f, store.prefs.getString("workoutName", workout), 20, TEXT);
    workoutClock = heading("00:00", 40);
    f.addView(workoutClock);
    addText(f, "SESSION STOPWATCH", 11, LIME);
    actions(
        f,
        "Start / resume",
        () -> {
          if (store.prefs.getLong("workoutStart", 0) == 0)
            store.prefs.edit().putLong("workoutStart", System.currentTimeMillis()).apply();
          updateWorkoutClocks();
        },
        "Pause",
        () -> {
          long elapsed = workoutMillis();
          store.prefs.edit().putLong("workoutElapsed", elapsed).putLong("workoutStart", 0).apply();
          updateWorkoutClocks();
        });
    space(f, 18);
    restClock = heading("Ready for your next set", 22);
    f.addView(restClock);
    actions(
        f,
        "Rest 60 sec",
        () -> {
          store.prefs.edit().putLong("restEnd", System.currentTimeMillis() + 60000).apply();
          updateWorkoutClocks();
        },
        "Rest 90 sec",
        () -> {
          store.prefs.edit().putLong("restEnd", System.currentTimeMillis() + 90000).apply();
          updateWorkoutClocks();
        });
    f.addView(
        button(
            "Skip rest",
            false,
            () -> {
              store.prefs.edit().remove("restEnd").apply();
              updateWorkoutClocks();
            }));
    addText(
        f,
        "Your stopwatch continues when you leave this screen. Rest sounds play while Rise is open."
            + " Log exercise sets separately in the workout.",
        12,
        MUTED);
    f.addView(
        button(
            "Finish & save session",
            true,
            () -> {
              long duration = workoutMillis();
              if (duration < 1000) {
                error("Start a session before saving.");
                return;
              }
              try {
                JSONObject entry =
                    new JSONObject()
                        .put("title", store.prefs.getString("workoutName", workout))
                        .put("date", store.today().toString())
                        .put(
                            "detail",
                            "Workout session: "
                                + String.format(
                                    Locale.US,
                                    "%02d:%02d",
                                    duration / 60000,
                                    (duration / 1000) % 60));
                JSONArray records = store.array("training");
                records.put(entry);
                store.array("training", records);
                store
                    .prefs
                    .edit()
                    .remove("workoutStart")
                    .remove("workoutElapsed")
                    .remove("restEnd")
                    .remove("workoutName")
                    .apply();
                updateWorkoutClocks();
                error("Session saved to training history.");
              } catch (Exception e) {
                error("Could not save session.");
              }
            }));
    updateWorkoutClocks();
    dialog("WORKOUT TIMER", f).setPositiveButton("Close", null).show();
  }

  void updateWorkoutClocks() {
    if (workoutClock != null) {
      long seconds = workoutMillis() / 1000;
      workoutClock.setText(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60));
    }
    long end = store.prefs.getLong("restEnd", 0),
        remaining = Math.max(0, end - System.currentTimeMillis());
    if (restClock != null)
      restClock.setText(
          end == 0
              ? "Ready for your next set"
              : remaining == 0 ? "Rest complete" : "REST  " + ((remaining + 999) / 1000) + " sec");
    if (end > 0 && remaining == 0) {
      store.prefs.edit().remove("restEnd").apply();
      if (resumed) {
        try {
          ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60);
          tone.startTone(ToneGenerator.TONE_PROP_ACK, 300);
          handler.postDelayed(() -> tone.release(), 500);
        } catch (Exception ignored) {
        }
      }
    }
  }

  void updateTimer() {
    long end = store.prefs.getLong("timerEnd", 0),
        remaining = end == 0 ? 1500000 : Math.max(0, end - System.currentTimeMillis());
    if (timerText != null)
      timerText.setText(
          String.format(Locale.US, "%02d:%02d", remaining / 60000, (remaining / 1000) % 60));
    if (end > 0 && remaining == 0) {
      store.prefs.edit().remove("timerEnd").apply();
      if (resumed) {
        error("Focus block complete. Take a short break.");
        try {
          ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60);
          tone.startTone(ToneGenerator.TONE_PROP_ACK, 300);
          handler.postDelayed(() -> tone.release(), 500);
        } catch (Exception ignored) {
        }
      }
    }
  }

  void guide() {
    try {
      File file = new File(getCacheDir(), "guide.pdf");
      try (InputStream in = getAssets().open("guide.pdf");
          OutputStream out = new FileOutputStream(file)) {
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
      }
      ParcelFileDescriptor fd =
          ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
      PdfRenderer pdf = new PdfRenderer(fd);
      LinearLayout f = column();
      TextView pageLabel = text("", 14, LIME);
      f.addView(pageLabel);
      ImageView iv = new ImageView(this);
      iv.setAdjustViewBounds(true);
      iv.setContentDescription(
          "PDF guide page. Text version is available in Train, Fuel and the care/career guides.");
      f.addView(iv, new LinearLayout.LayoutParams(-1, -2));
      final int[] page = {0};
      final Bitmap[] bitmap = {null};
      Runnable draw =
          () -> {
            PdfRenderer.Page p = pdf.openPage(page[0]);
            int width = Math.min(1400, getResources().getDisplayMetrics().widthPixels);
            Bitmap b =
                Bitmap.createBitmap(
                    width, width * p.getHeight() / p.getWidth(), Bitmap.Config.ARGB_8888);
            b.eraseColor(Color.WHITE);
            p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            p.close();
            iv.setImageBitmap(b);
            if (bitmap[0] != null) bitmap[0].recycle();
            bitmap[0] = b;
            pageLabel.setText("Page " + (page[0] + 1) + " of " + pdf.getPageCount());
          };
      actions(
          f,
          "Previous",
          () -> {
            if (page[0] > 0) {
              page[0]--;
              draw.run();
            }
          },
          "Next",
          () -> {
            if (page[0] < pdf.getPageCount() - 1) {
              page[0]++;
              draw.run();
            }
          });
      AlertDialog a = dialog("Your offline guide", f).setPositiveButton("Close", null).create();
      a.setOnDismissListener(
          d -> {
            iv.setImageDrawable(null);
            if (bitmap[0] != null) bitmap[0].recycle();
            pdf.close();
            try {
              fd.close();
            } catch (Exception ignored) {
            }
          });
      a.show();
      draw.run();
    } catch (Exception e) {
      error("Could not open the guide: " + e.getMessage());
    }
  }

  String read(InputStream in) throws IOException {
    try (InputStream input = in;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[8192];
      int n;
      while ((n = input.read(b)) != -1) {
        out.write(b, 0, n);
        if (out.size() > 5 * 1024 * 1024) throw new IOException("File is too large");
      }
      return out.toString("UTF-8");
    }
  }

  void exportBackup() {
    Intent i =
        new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType("application/json")
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_TITLE, "Rise-backup-" + store.today() + ".json");
    startActivityForResult(i, 70);
  }

  void importBackup() {
    new AlertDialog.Builder(this)
        .setTitle("Restore a backup?")
        .setMessage(
            "After you choose a valid Rise backup, it will replace this phone's current Rise data."
                + " Export a backup first if you want to keep it.")
        .setPositiveButton(
            "Choose file",
            (d, w) -> {
              Intent i =
                  new Intent(Intent.ACTION_OPEN_DOCUMENT)
                      .setType("*/*")
                      .addCategory(Intent.CATEGORY_OPENABLE);
              startActivityForResult(i, 71);
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  @Override
  protected void onActivityResult(int req, int result, Intent data) {
    super.onActivityResult(req, result, data);
    if (result != RESULT_OK || data == null) return;
    try {
      if (req == 70) {
        JSONObject all = new JSONObject();
        all.put("format", "rise-backup-1");
        JSONObject values = new JSONObject();
        for (Map.Entry<String, ?> e : store.prefs.getAll().entrySet())
          if (!e.getKey().equals("timerEnd")) values.put(e.getKey(), e.getValue());
        all.put("values", values);
        try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
          out.write(all.toString(2).getBytes("UTF-8"));
        }
        error("Backup exported. Keep it private.");
      } else if (req == 71) {
        JSONObject backup =
            new JSONObject(read(getContentResolver().openInputStream(data.getData())));
        JSONObject values = validateBackup(backup);
        new AlertDialog.Builder(this)
            .setTitle("Replace local Rise data?")
            .setMessage(
                "This valid backup contains "
                    + new JSONArray(values.getString("tasks")).length()
                    + " tasks. Current tasks, dreams and logs will be replaced.")
            .setPositiveButton(
                "Restore",
                (d, w) -> {
                  try {
                    for (Task t : store.tasks()) Reminders.cancel(this, t.id);
                    SharedPreferences.Editor ed = store.prefs.edit().clear();
                    Iterator<String> keys = values.keys();
                    while (keys.hasNext()) {
                      String k = keys.next();
                      Object value = values.get(k);
                      if (value instanceof Boolean) ed.putBoolean(k, (Boolean) value);
                      else if (k.equals("weight") || k.equals("height"))
                        ed.putFloat(k, ((Number) value).floatValue());
                      else if (k.equals("age") || k.equals("nextId"))
                        ed.putInt(k, ((Number) value).intValue());
                      else if (value instanceof String) ed.putString(k, (String) value);
                    }
                    ed.putBoolean("seeded", true).putBoolean("welcomed", true).commit();
                    store = new Store(this);
                    Reminders.all(this);
                    render();
                    error("Backup restored.");
                  } catch (Exception e) {
                    error("Restore failed: " + e.getMessage());
                  }
                })
            .setNegativeButton("Cancel", null)
            .show();
      }
    } catch (Exception e) {
      error("Could not use this file: " + e.getMessage());
    }
  }

  JSONObject validateBackup(JSONObject b) throws Exception {
    if (!"rise-backup-1".equals(b.optString("format"))) throw new Exception("Not a Rise backup");
    JSONObject v = b.getJSONObject("values");
    JSONArray tasks = new JSONArray(v.getString("tasks"));
    if (tasks.length() > 1000) throw new Exception("Too many tasks");
    Set<Integer> ids = new HashSet<>();
    int max = 0;
    for (int i = 0; i < tasks.length(); i++) {
      Task t = new Task(tasks.getJSONObject(i));
      if (t.id < 1
          || t.id > 1000000
          || !ids.add(t.id)
          || t.minute < 0
          || t.minute > 1439
          || t.days < 0
          || t.days > 127
          || t.duration < 1
          || t.duration > 600
          || t.title.trim().isEmpty()
          || t.title.length() > 150) throw new Exception("Invalid task");
      if (t.days == 0) LocalDate.parse(t.date);
      max = Math.max(max, t.id);
    }
    ZoneId.of(v.optString("zone", "Asia/Kolkata"));
    int age = v.optInt("age", 20);
    double kg = v.optDouble("weight", 65), h = v.optDouble("height", 165);
    if (age < 18
        || age > 100
        || kg < 30
        || kg > 300
        || h < 100
        || h > 230
        || !Double.isFinite(kg)
        || !Double.isFinite(h)) throw new Exception("Invalid profile");
    for (String key : new String[] {"dreams", "training", "measurements", "savedPlans"}) {
      JSONArray a = new JSONArray(v.optString(key, "[]"));
      if (a.length() > 10000) throw new Exception("Too many entries");
      for (int i = 0; i < a.length(); i++) {
        JSONObject o = a.getJSONObject(i);
        if (key.equals("savedPlans")) {
          if (a.length() > 20
              || o.optString("name").trim().isEmpty()
              || o.optString("name").length() > 80
              || o.getJSONArray("tasks").length() > 1000) throw new Exception("Invalid saved plan");
          JSONArray entries = o.getJSONArray("tasks");
          for (int n = 0; n < entries.length(); n++) {
            Task t = new Task(entries.getJSONObject(n));
            if (t.title.trim().isEmpty()
                || t.title.length() > 150
                || t.minute < 0
                || t.minute > 1439
                || t.days < 0
                || t.days > 127
                || t.duration < 1
                || t.duration > 600) throw new Exception("Invalid plan task");
          }
        }
        if (key.equals("dreams")) {
          double t = o.optDouble("target", 0), s = o.optDouble("saved", 0);
          if (o.optString("title").isEmpty()
              || t < 0
              || s < 0
              || !Double.isFinite(t)
              || !Double.isFinite(s)
              || t > 1e10
              || s > 1e10) throw new Exception("Invalid dream");
        }
      }
    }
    JSONObject clean = new JSONObject();
    String[] names = {
      "tasks",
      "dreams",
      "training",
      "measurements",
      "savedPlans",
      "name",
      "zone",
      "age",
      "weight",
      "height",
      "reminders",
      "nudge"
    };
    for (String k : names)
      if (v.has(k)) {
        Object val = v.get(k);
        if (k.equals("age") || k.equals("weight") || k.equals("height")) {
          if (!(val instanceof Number)) throw new Exception("Invalid profile type");
        } else if (k.equals("reminders") || k.equals("nudge")) {
          if (!(val instanceof Boolean)) throw new Exception("Invalid preference");
        } else if (!(val instanceof String)) throw new Exception("Invalid field type");
        clean.put(k, val);
      }
    Iterator<String> keys = v.keys();
    while (keys.hasNext()) {
      String k = keys.next();
      if (k.matches("done_[0-9]+_[0-9]{4}-[0-9]{2}-[0-9]{2}") && v.get(k) instanceof Boolean)
        clean.put(k, v.get(k));
      if (k.matches(
              "(cleared_|qte_)[0-9]{4}-[0-9]{2}-[0-9]{2}|bonus_[0-9]{4}-[0-9]{2}-[0-9]{2}_[01]")
          && v.get(k) instanceof Boolean) clean.put(k, v.get(k));
      if (k.matches("qte_start_[0-9]{4}-[0-9]{2}-[0-9]{2}") && v.get(k) instanceof String) {
        long start = Long.parseLong(v.getString(k));
        if (start < 0) throw new Exception("Invalid event time");
        clean.put(k, v.get(k));
      }
    }
    clean.put("nextId", max + 1);
    return clean;
  }

  class Ring extends View {
    float value;
    Paint p = new Paint(3);

    Ring(float v) {
      super(MainActivity.this);
      value = v;
      setContentDescription(Math.round(v * 100) + " percent complete");
    }

    protected void onDraw(Canvas c) {
      float w = getWidth(), h = getHeight();
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(dp(7));
      p.setColor(0xff3c4b3d);
      RectF r = new RectF(dp(6), dp(6), w - dp(6), h - dp(6));
      c.drawOval(r, p);
      p.setColor(LIME);
      p.setStrokeCap(Paint.Cap.ROUND);
      c.drawArc(r, -90, 360 * value, false, p);
      p.setStyle(Paint.Style.FILL);
      p.setTextAlign(Paint.Align.CENTER);
      p.setTypeface(Typeface.DEFAULT_BOLD);
      p.setTextSize(dp(18));
      c.drawText(Math.round(value * 100) + "%", w / 2, h / 2 + dp(6), p);
    }
  }
}
