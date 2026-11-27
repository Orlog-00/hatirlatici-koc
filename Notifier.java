package com.koc.hatirlatici;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.net.Uri;

import java.util.List;
import java.util.Random;

public class Notifier {

    /** Bu uyarı için seçilen görünüm: hareketli mi, kendi dosyan mı. */
    public static class Pick {
        public boolean anim;
        public String uri = "";
    }

    static int num(String s, int d) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return d;
        }
    }

    /** Ayarlardaki Tür (sabit/hareketli/karışık) ve Kaynak (otomatik/kendi dosyam/karışık) seçimini uygular. */
    public static Pick pick(Context c, Random r) {
        Pick pk = new Pick();
        int type = num(Store.get(c, "wptype", "2"), 2);
        pk.anim = type == 1 || (type == 2 && r.nextBoolean());
        int src = num(Store.get(c, "wpsrc", "0"), 0);
        List<String> media = UserMedia.list(c);
        if (!media.isEmpty() && (src == 1 || (src == 2 && r.nextBoolean()))) {
            pk.uri = media.get(r.nextInt(media.size()));
        }
        return pk;
    }

    static Bitmap bg(Context c, Quotes.Q q, long seed, Pick pk, int w, int h) {
        Bitmap b = null;
        if (!pk.uri.isEmpty()) {
            b = UserMedia.bitmap(c, pk.uri, w, h);
            if (b == null) pk.uri = "";
        }
        if (b == null) b = Wallpaper.background(q.cat, w, h, seed);
        return b;
    }

    /** Android'de ses ve titreşim kanala bağlıdır; her ses+titreşim ikilisi için bir kanal açılır. */
    static String channel(Context c, int sound, int vib) {
        String id = "s" + sound + "_v" + vib;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(id) == null) {
            NotificationChannel ch = new NotificationChannel(id,
                    "Uyarı: " + Sounds.NAMES[sound] + " / titreşim " + Sounds.VIB_NAMES[vib],
                    NotificationManager.IMPORTANCE_HIGH);
            Uri u = Uri.parse("android.resource://" + c.getPackageName() + "/" + Sounds.RAW[sound]);
            ch.setSound(u, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            long[] pat = Sounds.VIBS[vib];
            if (pat.length > 0) {
                ch.enableVibration(true);
                ch.setVibrationPattern(pat);
            } else {
                ch.enableVibration(false);
            }
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
        return id;
    }

    static PendingIntent openIntent(Context c, int req, Quotes.Q q, long seed, Pick pk) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("cat", String.valueOf(q.cat));
        i.putExtra("text", q.text);
        i.putExtra("author", q.author);
        i.putExtra("seed", seed);
        i.putExtra("anim", pk.anim);
        i.putExtra("uri", pk.uri);
        return PendingIntent.getActivity(c, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void show(Context c, Task t, boolean fin) {
        Quotes.Q q = Quotes.next(c);
        long seed = System.nanoTime();
        Pick pk = pick(c, new Random(seed));
        Bitmap bg = bg(c, q, seed, pk, 1080, 540);
        Bitmap big = Wallpaper.compose(bg, q.text, q.author, 1080, 540);
        Bitmap small = Wallpaper.compose(bg, null, null, 192, 192);
        String title = fin ? "Süre doldu: " + t.title : t.title;
        PendingIntent pi = openIntent(c, t.id.hashCode(), q, seed, pk);

        Notification.Builder b = new Notification.Builder(c, channel(c, t.sound, t.vib))
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(title)
                .setContentText(q.text)
                .setLargeIcon(small)
                .setStyle(new Notification.BigPictureStyle().bigPicture(big).bigLargeIcon((Bitmap) null))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if ("1".equals(Store.get(c, "fullscreen", "0"))) b.setFullScreenIntent(pi, true);

        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(t.id.hashCode(), b.build());

        if ("1".equals(Store.get(c, "lockcard", "0"))) lockCard(c, q, seed);
    }

    /** Kilit ekranında sürekli duran, sessiz söz kartı (küçük duvar kağıdı + söz). */
    public static void lockCard(Context c, Quotes.Q q, long seed) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel("lock") == null) {
            NotificationChannel ch = new NotificationChannel("lock", "Kilit ekranı kartı",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        Pick pk = pick(c, new Random(seed));
        Bitmap bg = bg(c, q, seed, pk, 1080, 540);
        Bitmap big = Wallpaper.compose(bg, q.text, q.author, 1080, 540);
        Bitmap small = Wallpaper.compose(bg, null, null, 192, 192);
        Notification.Builder b = new Notification.Builder(c, "lock")
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("Bugünün sözü")
                .setContentText(q.text)
                .setLargeIcon(small)
                .setStyle(new Notification.BigPictureStyle().bigPicture(big).bigLargeIcon((Bitmap) null))
                .setContentIntent(openIntent(c, 7777, q, seed, pk))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        nm.notify(7777, b.build());
    }

    public static void removeLockCard(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(7777);
    }
}
