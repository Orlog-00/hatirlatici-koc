package com.koc.hatirlatici;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.AudioAttributes;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/** Bildirimler: görev uyarısı, kilit ekranı kartı ve "Görevlerim" kartı. */
public class Notifier {
    public static final int LOCK_ID = 7777, TASKS_ID = 7778;

    public static class Pick {
        public boolean anim;
        public String uri;
        public int theme = -1;
    }

    /** taskWp: -1 = ayarlara göre rastgele, 0..N = tema, 100 = kendi dosyalarım. */
    public static Pick pick(Context c, Random r, int taskWp) {
        Pick p = new Pick();
        String mode = Store.get(c, "wpmode", "2"); // 0 sabit, 1 hareketli, 2 ikisi de
        p.anim = "1".equals(mode) || ("2".equals(mode) && r.nextBoolean());
        List<String> media = UserMedia.list(c);
        if (taskWp == 100 && !media.isEmpty()) {
            p.uri = media.get(r.nextInt(media.size()));
            p.theme = -1;
            return p;
        }
        if (taskWp >= 0 && taskWp < Scenes.COUNT) {
            p.theme = taskWp;
            return p;
        }
        boolean useMedia = "1".equals(Store.get(c, "usemedia", "1")) && !media.isEmpty();
        if (useMedia && r.nextInt(3) == 0) {
            p.uri = media.get(r.nextInt(media.size()));
            p.theme = -1;
        } else {
            p.theme = r.nextInt(Scenes.COUNT);
        }
        return p;
    }

    public static Bitmap bg(Context c, Pick pk, char cat, int w, int h, long seed) {
        if (pk.uri != null) {
            Bitmap b = UserMedia.bitmap(c, pk.uri, w, h);
            if (b != null) return b;
        }
        return Scenes.scene(pk.theme < 0 ? (int) (seed & 0x7fffffff) % Scenes.COUNT : pk.theme, cat, w, h, seed);
    }

    // ----------------------------------------------------------- küçük resimler

    static final HashMap<String, Bitmap> THUMBS = new HashMap<>();

    /** Görev listesi için küçük duvar kağıdı resmi (önbellekli). */
    public static Bitmap thumb(Context c, int wp, int size) {
        String key = wp + "_" + size;
        Bitmap b = THUMBS.get(key);
        if (b != null && !b.isRecycled()) return b;
        if (wp == 100) {
            List<String> m = UserMedia.list(c);
            if (!m.isEmpty()) b = UserMedia.bitmap(c, m.get(0), size, size);
        }
        if (b == null) {
            int th = (wp >= 0 && wp < Scenes.COUNT) ? wp : 0;
            b = Scenes.scene(th, 'b', size, size, 7L + th);
        }
        if (b != null && wp != 100) THUMBS.put(key, b);
        return roundCrop(b, size);
    }

    static Bitmap roundCrop(Bitmap src, int size) {
        if (src == null) return null;
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Path clip = new Path();
        clip.addRoundRect(0, 0, size, size, size * 0.2f, size * 0.2f, Path.Direction.CW);
        cv.clipPath(clip);
        float sc = Math.max((float) size / src.getWidth(), (float) size / src.getHeight());
        float dx = (size - src.getWidth() * sc) / 2f;
        float dy = (size - src.getHeight() * sc) / 2f;
        cv.translate(dx, dy);
        cv.scale(sc, sc);
        cv.drawBitmap(src, 0, 0, p);
        return out;
    }

    // ----------------------------------------------------------- kanallar / intent

    static NotificationManager nm(Context c) {
        return (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static String channel(Context c, int sound, int vib) {
        String id = "s" + sound + "_v" + vib;
        NotificationManager m = nm(c);
        if (m.getNotificationChannel(id) == null) {
            NotificationChannel ch = new NotificationChannel(id,
                    "Uyarı: " + Sounds.NAMES[sound] + " / " + Sounds.VIB_NAMES[vib], NotificationManager.IMPORTANCE_HIGH);
            Uri u = Uri.parse("android.resource://" + c.getPackageName() + "/" + Sounds.RAW[sound]);
            ch.setSound(u, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            if (Sounds.VIBS[vib].length > 0) {
                ch.enableVibration(true);
                ch.setVibrationPattern(Sounds.VIBS[vib]);
            } else {
                ch.enableVibration(false);
            }
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            m.createNotificationChannel(ch);
        }
        return id;
    }

    static String quietChannel(Context c, String id, String name, int importance) {
        NotificationManager m = nm(c);
        if (m.getNotificationChannel(id) == null) {
            NotificationChannel ch = new NotificationChannel(id, name, importance);
            ch.setSound(null, null);
            ch.enableVibration(false);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            m.createNotificationChannel(ch);
        }
        return id;
    }

    public static PendingIntent openIntent(Context c, char cat, String text, String author, long seed,
                                           boolean anim, String uri, int theme, int req, int tab) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (text != null) {
            i.putExtra("cat", String.valueOf(cat));
            i.putExtra("text", text);
            i.putExtra("author", author);
            i.putExtra("seed", seed);
            i.putExtra("anim", anim);
            if (uri != null) i.putExtra("uri", uri);
            i.putExtra("theme", theme);
        }
        if (tab >= 0) i.putExtra("tab", tab);
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // ----------------------------------------------------------- görev uyarısı

    public static void show(Context c, Task t, boolean fin) {
        try {
            Quotes.Q q = Quotes.next(c);
            long seed = System.nanoTime();
            Random r = new Random(seed);
            Pick pk = pick(c, r, t.wp);
            Bitmap bg = bg(c, pk, q.cat, 960, 540, seed);
            Bitmap big = Wallpaper.compose(bg, q.text, q.author, 960, 540);
            Bitmap small = roundCrop(bg, 160);
            int req = (t.id + seed).hashCode();
            PendingIntent open = openIntent(c, q.cat, q.text, q.author, seed, pk.anim, pk.uri, pk.theme, req, -1);
            String chId = channel(c, t.sound, t.vib);

            Notification.Builder b = new Notification.Builder(c, chId)
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentTitle((fin ? "⏰ Süre doldu: " : "🔔 ") + t.title)
                    .setContentText(q.text)
                    .setLargeIcon(small)
                    .setStyle(new Notification.BigPictureStyle().bigPicture(big).bigLargeIcon((Bitmap) null)
                            .setSummaryText(q.text))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .setCategory(Notification.CATEGORY_REMINDER)
                    .setVisibility(Notification.VISIBILITY_PUBLIC);
            if ("1".equals(Store.get(c, "fullscreen", "0"))) b.setFullScreenIntent(open, true);
            nm(c).notify(t.id.hashCode(), b.build());

            if ("1".equals(Store.get(c, "lockcard", "0"))) lockCard(c, q, seed + 1);
        } catch (Exception e) {
            // bildirim gösterilemedi
        }
    }

    // ----------------------------------------------------------- kilit ekranı sözü kartı

    public static void lockCard(Context c, Quotes.Q q, long seed) {
        try {
            Random r = new Random(seed);
            Pick pk = pick(c, r, -1);
            Bitmap bg = bg(c, pk, q.cat, 800, 400, seed);
            Bitmap big = Wallpaper.compose(bg, q.text, q.author, 800, 400);
            PendingIntent open = openIntent(c, q.cat, q.text, q.author, seed, pk.anim, pk.uri, pk.theme, 7777, -1);
            String chId = quietChannel(c, "lock", "Kilit ekranı sözü", NotificationManager.IMPORTANCE_LOW);
            Notification n = new Notification.Builder(c, chId)
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentTitle("Günün sözü")
                    .setContentText(q.text)
                    .setStyle(new Notification.BigPictureStyle().bigPicture(big).bigLargeIcon((Bitmap) null)
                            .setSummaryText(q.text))
                    .setContentIntent(open)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .build();
            nm(c).notify(LOCK_ID, n);
        } catch (Exception e) {
            // yoksay
        }
    }

    public static void removeLockCard(Context c) {
        nm(c).cancel(LOCK_ID);
    }

    // ----------------------------------------------------------- "Görevlerim" kartı

    public static void updateTaskCard(Context c) {
        NotificationManager m = nm(c);
        if (!"1".equals(Store.get(c, "taskcard", "1"))) {
            m.cancel(TASKS_ID);
            return;
        }
        long now = System.currentTimeMillis();
        List<Task> act = new ArrayList<>();
        for (Task t : Store.tasks(c)) if (t.status(now) == Task.ACTIVE) act.add(t);
        if (act.isEmpty()) {
            m.cancel(TASKS_ID);
            return;
        }
        Collections.sort(act, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                return Long.compare(a.deadline, b.deadline);
            }
        });
        Notification.InboxStyle st = new Notification.InboxStyle();
        int n = Math.min(5, act.size());
        for (int i = 0; i < n; i++) {
            Task t = act.get(i);
            st.addLine("• " + t.title + "  —  " + Ui.remain(t.deadline - now));
        }
        if (act.size() > n) st.addLine("+" + (act.size() - n) + " görev daha");
        st.setSummaryText(act.size() + " aktif görev");
        String chId = quietChannel(c, "tasks", "Görevlerim kartı", NotificationManager.IMPORTANCE_LOW);
        Notification nt = new Notification.Builder(c, chId)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("Görevlerim")
                .setContentText(act.get(0).title + " — " + Ui.remain(act.get(0).deadline - now))
                .setStyle(st)
                .setContentIntent(openIntent(c, 'b', null, null, 0, false, null, -1, 7778, 0))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
        m.notify(TASKS_ID, nt);
    }
}
