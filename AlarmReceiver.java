package com.koc.hatirlatici;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String id = intent.getStringExtra("id");
        if (id == null) return;
        List<Task> ts = Store.tasks(c);
        Task t = null;
        for (Task x : ts) {
            if (id.equals(x.id)) {
                t = x;
                break;
            }
        }
        if (t == null || !t.active) return;

        long now = System.currentTimeMillis();
        boolean fin = now >= t.deadline - 2000;
        // Telefon kapalıyken kaçan alarm, izin verilen saat/gün dışında çalmasın
        boolean quiet = !fin && t.adjust(now) != now;
        if (!quiet) Notifier.show(c, t, fin);

        if (fin) {
            t.active = false;
        } else {
            while (t.next <= now) t.next += t.everyMs;
            t.next = t.adjust(t.next);
            if (t.next > t.deadline) t.next = t.deadline;
            Scheduler.schedule(c, t);
        }
        Store.saveTasks(c, ts);
    }
}
