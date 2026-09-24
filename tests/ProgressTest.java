import com.saran.rise.*;
import java.time.*;
import java.util.*;

public class ProgressTest {
  static int checks=0;
  static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
  public static void main(String[] args){
    LocalDate day=LocalDate.of(2026,9,19);Map<String,Object> records=new HashMap<>();
    List<Task> tasks=Arrays.asList(new Task(1,"Exercise","Workout","",420,127,30));
    Progress p=new Progress(records,tasks,day);check(p.level==1&&p.rank.equals("E")&&p.xp==0,"New player");
    records.put("done_1_2026-09-19",true);p=new Progress(records,tasks,day);
    check(p.xp==50&&p.streak==1&&p.stats[0]==10,"First completion");
    records.put("done_1_2026-09-19",true);check(new Progress(records,tasks,day).xp==50,"Duplicate completion no extra reward");
    records.put("done_1_2026-09-19",false);check(new Progress(records,tasks,day).xp==0,"Undo reverses reward");
    records.put("done_1_2026-09-18",true);records.put("done_1_2026-09-17",true);p=new Progress(records,tasks,day);
    check(p.streak==2,"Yesterday's streak stays alive while today is pending");
    records.put("done_1_2026-09-19",true);check(new Progress(records,tasks,day).streak==3,"Consecutive days");
    check(new Progress(records,tasks,day.plusDays(2)).streak==0,"Missed day resets active streak");
    records.put("done_invalid",true);records.put("done_1_2026-09-20",true);check(new Progress(records,tasks,day).completed==3,"Malformed and future ignored");
    for(int count=1;count<=500;count++){
      Map<String,Object> history=new HashMap<>();for(int i=0;i<count;i++)history.put("done_"+(i+1)+"_2026-09-19",true);
      p=new Progress(history,tasks,day);check(p.xp==count*50&&p.level==1+count/10,"Level boundary "+count);
    }
    p=new Progress(records,Collections.emptyList(),day);check(p.xp==150&&p.stats[3]==30,"Deleted quests keep earned XP");
    List<Task> pair=Arrays.asList(tasks.get(0),new Task(2,"Study","Career","",600,127,20));
    Map<String,Object> partial=new HashMap<>();partial.put("done_1_"+day,true);
    check(new Progress(partial,pair,day).streak==0,"Partial day does not preserve streak");
    partial.put("done_2_"+day,true);check(new Progress(partial,pair,day).rank.equals("D"),"Cleared day and 100 XP unlock D");
    partial.put("cleared_"+day,false);check(new Progress(partial,pair,day).streak==0,"Explicit snapshot overrides legacy reconstruction");
    for(int tier=1;tier<Progress.RANKS.length;tier++){
      Map<String,Object> history=new HashMap<>();
      for(int i=0;i<Progress.RANK_DAYS[tier];i++)history.put("cleared_"+day.minusDays(i),true);
      for(int i=0;i<Progress.RANK_XP[tier]/50;i++)history.put("done_"+(i+1)+"_"+day,true);
      Progress rank=new Progress(history,tasks,day);
      check(rank.rank.equals(Progress.RANKS[tier]),"Both thresholds unlock "+Progress.RANKS[tier]);
      history.put("cleared_"+day,false);history.put("cleared_"+day.minusDays(1),false);
      rank=new Progress(history,tasks,day);check(rank.rank.equals("E")&&rank.streakXp==0&&rank.xp==Progress.RANK_XP[tier],"Miss resets rank, preserves lifetime XP");
    }
    Map<String,Object> rewards=new HashMap<>();rewards.put("bonus_"+day+"_0",true);rewards.put("bonus_"+day+"_1",true);rewards.put("qte_"+day,true);
    p=new Progress(rewards,tasks,day);check(p.xp==550&&p.rank.equals("E"),"Bonus XP does not replace daily clear");
    rewards.put("qte_"+day,true);check(new Progress(rewards,tasks,day).xp==550,"No repeated event reward");
    check(Challenges.daily(day).equals(Challenges.daily(day)),"Stable daily random draw");
    check(!Challenges.daily(day).get(0).equals(Challenges.daily(day).get(1)),"Distinct bonus activities");
    check(Challenges.inTime(1001,1000,500000),"Active quick event");
    check(!Challenges.inTime(301000,1000,500000),"Event expires at deadline");
    check(!Challenges.inTime(2000,1000,2000),"Event expires at midnight");
    check(!Challenges.inTime(999,1000,500000),"Clock rollback cannot extend event");
    System.out.println("PASS: "+checks+" progression/event checks");
  }
}
