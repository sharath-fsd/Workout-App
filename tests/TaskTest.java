import com.saran.rise.Task;
import java.time.*;

public class TaskTest {
  static int checks = 0;

  static void check(boolean ok, String name) {
    checks++;
    if (!ok) throw new AssertionError(name);
  }

  static long at(String s, ZoneId z) {
    return LocalDateTime.parse(s).atZone(z).toInstant().toEpochMilli();
  }

  public static void main(String[] args) {
    ZoneId z = ZoneId.of("Asia/Kolkata");
    Task t = new Task(1, "Strength", "Workout", "", 435, 21, 40);
    check(t.on(LocalDate.of(2026, 9, 14)), "Monday enabled");
    check(!t.on(LocalDate.of(2026, 9, 15)), "Tuesday disabled");
    check(t.on(LocalDate.of(2026, 9, 16)), "Wednesday enabled");
    check(
        t.next(at("2026-09-16T07:00", z), z) == at("2026-09-16T07:15", z), "Same day before time");
    check(
        t.next(at("2026-09-16T07:15", z), z) == at("2026-09-18T07:15", z),
        "At time picks next eligible day");
    check(t.next(at("2026-09-18T08:00", z), z) == at("2026-09-21T07:15", z), "Weekend skipped");
    Task once = new Task(2, "Once", "Personal", "", 0, 0, 1);
    once.date = "2026-10-20";
    check(
        once.next(at("2026-09-16T09:00", z), z) == at("2026-10-20T00:00", z),
        "One-time date beyond 9 days");
    check(once.next(at("2026-10-20T00:01", z), z) == -1, "Expired one-time does not repeat");
    check(!once.on(LocalDate.of(2026, 10, 19)), "One-time hidden on other days");
    Task daily = new Task(3, "Daily", "Care", "", 420, 127, 5);
    check(
        daily.next(at("2026-09-16T23:59", z), z) == at("2026-09-17T07:00", z), "Midnight rollover");
    Task copy = new Task(t.json());
    check(
        copy.id == t.id && copy.days == 21 && copy.minute == 435 && copy.reminder,
        "JSON round trip");
    ZoneId ny = ZoneId.of("America/New_York");
    daily.minute = 150;
    long next = daily.next(at("2026-03-08T01:00", ny), ny);
    check(next > at("2026-03-08T01:00", ny), "DST gap resolves forward");
    for (int mask = 1; mask < 128; mask++) {
      daily.days = mask;
      long now = at("2026-09-16T19:00", z);
      long n = daily.next(now, z);
      check(n > now && n - now <= 8L * 86400000, "Every valid weekly mask schedules in future");
      check(
          daily.on(Instant.ofEpochMilli(n).atZone(z).toLocalDate()), "Chosen date belongs to mask");
    }
    System.out.println("PASS: " + checks + " scheduling and serialization checks");
  }
}
