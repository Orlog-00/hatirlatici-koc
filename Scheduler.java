package com.koc.hatirlatici;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

/** Sistem alarmı (AlarmManager) kullanır: uygulama kapalı, telefon uykuda olsa da çalışır. */
public class Scheduler {

    static PendingIntent pi(Context c, Task t) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction("koc.ALARM");
        i.putExtra("id", t.id);
        return PendingIntent.getBroadcast(c, t.id.hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void schedule(Context c, Task t) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        long at = Math.min(t.next, t.deadline);
        PendingIntent p = pi(c, t);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        }
    }

    public static void cancel(Context c, Task t) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi(c, t));
    }

    public static void scheduleAll(Context c) {
        List<Task> ts = Store.tasks(c);
        for (Task t : ts) {
            if (t.active) schedule(c, t);
        }
    }
}
