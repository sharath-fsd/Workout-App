package com.saran.rise;

import java.time.LocalDate;
import java.util.*;

/** Stable daily draw: reopening the app cannot reroll a reward. */
public final class Challenges {
  public static final String[] BONUS = {
    "Explain one solved DSA problem aloud, including its time complexity.",
    "Write three bullet points for a common interview question.",
    "Review one job description and note two skills to practice.",
    "Prepare your dumbbells and clear a safe space for your next planned workout.",
    "Choose a protein source for your next meal and note its portion.",
    "Put tomorrow's top three priorities in your quest list.",
    "Take a comfortable two-minute indoor movement break, if you feel well.",
    "Spend two minutes tidying your study space and removing distractions."
  };
  public static final String[] EVENTS = {
    "Within five minutes, explain the approach to a DSA problem you already solved.",
    "Within five minutes, practice your interview introduction once and note one improvement.",
    "Within five minutes, review one job listing and save its next application step."
  };

  public static List<String> daily(LocalDate date) {
    List<String> choices = new ArrayList<>(Arrays.asList(BONUS));
    Collections.shuffle(choices, new Random(date.toEpochDay() * 104729L + 71));
    return choices.subList(0, 2);
  }

  public static String event(LocalDate date) {
    return EVENTS[(int) Math.floorMod(date.toEpochDay() * 17L, EVENTS.length)];
  }

  public static boolean inTime(long now, long started, long midnight) {
    return started > 0 && now >= started && now < Math.min(started + 300000L, midnight);
  }
}
