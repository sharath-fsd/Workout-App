package com.saran.rise;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;

/** Read-only media integration checks; does not reset personal data. */
public class MediaTest extends SmokeTest {
  @Override public void onStart() {
    Bundle result = new Bundle();
    try {
      Context c = getTargetContext();
      Store before = new Store(c);
      String tasks = before.prefs.getString("tasks", "");
      activity = (MainActivity) startActivitySync(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      waitForIdleSync();
      result.putString("assets", java.util.Arrays.toString(activity.getAssets().list("images")));
      result.putString("source", activity.getApplicationInfo().sourceDir);
      result.putString("package", activity.getPackageName());
      result.putString("rootAssets", java.util.Arrays.toString(activity.getAssets().list("")));
      JSONArray workouts = activity.content.getJSONArray("workouts");
      for (int i = 0; i < workouts.length(); i++) {
        JSONObject workout = workouts.getJSONObject(i);
        JSONArray exercises = workout.getJSONArray("exercises");
        for (int j = 0; j < exercises.length(); j++) {
          JSONObject media = activity.media.getJSONObject("exercises").getJSONObject(exercises.getJSONArray(j).getString(0));
          JSONArray pictures = media.getJSONArray("images");
          for (int k = 0; k < pictures.length(); k++)
            check(activity.loadPicture(pictures.getString(k)).getWidth() > 100, "Exercise image decodes");
          final Intent[] launched = new Intent[1];
          ActivityMonitor monitor = new ActivityMonitor() {
            @Override public ActivityResult onStartActivity(Intent intent) {
              launched[0] = intent;
              return new ActivityResult(Activity.RESULT_CANCELED, null);
            }
          };
          addMonitor(monitor);
          ui(() -> activity.watchVideo(media.optString("video")));
          removeMonitor(monitor);
          check(launched[0] != null && launched[0].getDataString().equals("https://www.youtube.com/watch?v=" + media.getString("video")), "Direct video intent matches exercise");
        }
        ui(() -> activity.workout(workout));
        screenshot("media-workout-" + i);
        closeDialog();
      }
      JSONArray food = activity.media.getJSONArray("food");
      for (int i = 0; i < food.length(); i++) check(activity.loadPicture(food.getJSONObject(i).getString("image")).getWidth() > 100, "Food image decodes");
      ui(() -> {activity.tab = 6; activity.render();});
      screenshot("media-food");
      ui(() -> {activity.tab = 5; activity.render();});
      screenshot("media-train");
      check(tasks.equals(new Store(c).prefs.getString("tasks", "")), "Media browsing preserves tasks");
      result.putString("result", "PASS: " + checks + " media checks");
      finish(Activity.RESULT_OK, result);
    } catch (Throwable e) {
      result.putString("error", android.util.Log.getStackTraceString(e));
      finish(Activity.RESULT_CANCELED, result);
    }
  }
}
