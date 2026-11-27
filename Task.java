package com.koc.hatirlatici;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class Task {
    public String id = "";
    public String title = "";
    public long deadline, everyMs, next, created;
    public int sound, vib, done;
    public boolean active = true;

    // alan, uyarı saat penceresi ve günler (bit 0 = Pazartesi ... bit 6 = Pazar)
    public int area = 0;
    public int startHour = 0;
    public int endHour = 24;
    public int days = 127;

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("deadline", deadline);
        o.put("everyMs", everyMs);
        o.put("next", next);
        o.put("created", created);
        o.put("sound", sound);
        o.put("vib", vib);
        o.put("done", done);
        o.put("active", active);
        o.put("area", area);
        o.put("startHour", startHour);
        o.put("endHour", endHour);
        o.put("days", days);
        return o;
    }

    public static Task fromJson(JSONObject o) {
        Task t = new Task();
        t.id = o.optString("id");
        t.title = o.optString("title");
        t.deadline = o.optLong("deadline");
        t.everyMs = o.optLong("everyMs", 3600000L);
        t.next = o.optLong("next");
        t.created = o.optLong("created");
        t.sound = o.optInt("sound");
        t.vib = o.optInt("vib");
        t.done = o.optInt("done");
        t.active = o.optBoolean("active", true);
        t.area = o.optInt("area", 0);
        t.startHour = o.optInt("startHour", 0);
        t.endHour = o.optInt("endHour", 24);
        t.days = o.optInt("days", 127);
        if (t.endHour <= t.startHour) {
            t.startHour = 0;
            t.endHour = 24;
        }
        if ((t.days & 127) == 0) t.days = 127;
        return t;
    }

    boolean dayOk(Calendar c) {
        int dow = c.get(Calendar.DAY_OF_WEEK); // 1 = Pazar ... 7 = Cumartesi
        int idx = (dow + 5) % 7;               // 0 = Pazartesi ... 6 = Pazar
        return (days & (1 << idx)) != 0;
    }

    /** Verilen zamanı, görevin izin verilen gün/saat penceresine denk gelen ilk ana kaydırır. */
    public long adjust(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        for (int i = 0; i < 400; i++) {
            int h = c.get(Calendar.HOUR_OF_DAY);
            if (dayOk(c)) {
                if (h >= startHour && h < endHour) return c.getTimeInMillis();
                if (h < startHour) {
                    c.set(Calendar.HOUR_OF_DAY, startHour);
                    c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    return c.getTimeInMillis();
                }
            }
            c.add(Calendar.DAY_OF_YEAR, 1);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }
        return t;
    }

    public String windowText() {
        return String.format("%02d:00-%02d:00", startHour, endHour);
    }

    public String daysText() {
        if ((days & 127) == 127) return "her gün";
        String[] n = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((days & (1 << i)) != 0) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(n[i]);
            }
        }
        return sb.toString();
    }
}
