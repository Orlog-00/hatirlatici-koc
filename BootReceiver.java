package com.koc.hatirlatici;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Telefon yeniden açılınca veya uygulama güncellenince tüm alarmları yeniden kurar. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Scheduler.scheduleAll(c);
        if ("1".equals(Store.get(c, "lockcard", "0"))) {
            Notifier.lockCard(c, Quotes.next(c), System.nanoTime());
        }
    }
}
