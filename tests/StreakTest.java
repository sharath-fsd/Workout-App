package com.saran.rise;

import android.app.*;
import android.content.*;
import android.os.*;
import java.util.*;
import org.json.*;

public class StreakTest extends SmokeTest {
  @Override public void onStart(){Bundle result=new Bundle();
    try{
      Context c=getTargetContext();Store s=new Store(c);
      activity=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
      getUiAutomation();SystemClock.sleep(400);screenshot("streak-start");
      ArrayList<Task> original=s.tasks();String day=s.today().toString();
      for(int i=0;i<4;i++){
        List<Task> plan=Plans.preset(i);check(plan.size()>=4,"Preset has a complete routine");
        Set<Integer> days=new HashSet<>();for(Task t:plan){check(t.minute>=0&&t.minute<1440&&t.duration>0,"Valid preset time and duration");for(int d=0;d<7;d++)if((t.days&(1<<d))!=0)days.add(d);}
        check(days.size()==7,"Preset includes all seven days");
      }
      ui(()->Plans.apply(activity,Plans.preset(3),false));int added=s.tasks().size();
      ui(()->Plans.apply(activity,Plans.preset(3),false));check(s.tasks().size()==added,"Adding same preset does not duplicate quests");
      ui(()->Plans.save(activity));SystemClock.sleep(800);set(fields().get(0),"Device verified plan");click("Save");
      check(s.array("savedPlans").length()>0,"Personal routine template saved");
      JSONObject backup=new JSONObject().put("format","rise-backup-1").put("values",new JSONObject(s.prefs.getAll()));
      check(activity.validateBackup(backup).has("savedPlans"),"Backup keeps personal plans");
      ui(()->Plans.open(activity));screenshot("plans-2.1");closeDialog();
      ui(()->Plans.apply(activity,Plans.preset(0),true));check(s.tasks().size()==Plans.preset(0).size(),"Replace installs only the selected plan");
      for(Task t:s.tasks())Reminders.cancel(c,t.id);s.tasks(original);Reminders.all(c);
      for(Task t:original)if(t.on(s.today()))s.done(t.id,day,true);
      check(s.prefs.getBoolean("cleared_"+day,false),"Completing all today's tasks records a clear day");
      for(Task t:original)if(t.on(s.today())){s.done(t.id,day,false);break;}
      check(!s.prefs.getBoolean("cleared_"+day,true),"Undo revokes today's clear-day credit");
      s.prefs.edit().remove("bonus_"+day+"_0").remove("bonus_"+day+"_1").remove("qte_"+day).remove("qte_start_"+day).commit();
      ui(()->ChallengesUi.open(activity));click("Complete & claim +150 XP");
      check(s.prefs.getBoolean("bonus_"+day+"_0",false),"Bonus claim persists");
      closeDialog();ui(()->ChallengesUi.open(activity));check(textVisible("Reward claimed"),"Claimed reward cannot be claimed again on reopen");
      click("Start 5-minute event");long started=Long.parseLong(s.prefs.getString("qte_start_"+day,"0"));check(started>0,"Timed event start persists");
      click("Completed · claim +250 XP");check(s.prefs.getBoolean("qte_"+day,false),"Timely event reward persists");
      closeDialog();s.prefs.edit().putBoolean("qte_"+day,false).putString("qte_start_"+day,Long.toString(System.currentTimeMillis()-301000)).commit();
      ui(()->ChallengesUi.open(activity));check(textVisible("EXPIRED / TRY TOMORROW"),"Expired event displayed after reopening");screenshot("events-2.1");closeDialog();
      backup=new JSONObject().put("format","rise-backup-1").put("values",new JSONObject(s.prefs.getAll()));
      JSONObject restored=activity.validateBackup(backup);check(restored.has("cleared_"+day)&&restored.has("qte_start_"+day)&&restored.has("bonus_"+day+"_0"),"Backup keeps clear days and event state");
      ui(()->{activity.tab=0;activity.render();});screenshot("home-2.1");
      result.putString("result","PASS: "+checks+" preset/streak/event device checks");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){try{screenshot("streak-failure");}catch(Exception ignored){}result.putString("error",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
  }
}
