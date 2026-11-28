package com.koc.hatirlatici;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

/** Sohbet oturumları: her mesaj tarih/saatle kalıcı kaydedilir, sonradan değiştirilemez. */
public class Sessions {

    public static JSONArray all(Context c, String key) {
        return Store.arr(c, key);
    }

    public static JSONObject create(Context c, String key, JSONObject meta) {
        try {
            JSONObject s = meta == null ? new JSONObject() : meta;
            s.put("id", UUID.randomUUID().toString());
            s.put("created", System.currentTimeMillis());
            s.put("msgs", new JSONArray());
            JSONArray a = all(c, key);
            a.put(s);
            Store.putArr(c, key, a);
            return s;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static JSONObject find(Context c, String key, String id) {
        JSONArray a = all(c, key);
        for (int i = 0; i < a.length(); i++) {
            JSONObject s = a.optJSONObject(i);
            if (s != null && id.equals(s.optString("id"))) return s;
        }
        return null;
    }

    /** Mesajı oturuma ekler; hidden=true ise ekranda gösterilmez (sadece yapay zekâ için). */
    public static void addMsg(Context c, String key, String id, String role, String text, boolean hidden) {
        try {
            JSONArray a = all(c, key);
            for (int i = 0; i < a.length(); i++) {
                JSONObject s = a.optJSONObject(i);
                if (s != null && id.equals(s.optString("id"))) {
                    JSONObject m = new JSONObject();
                    m.put("r", role);
                    m.put("t", text);
                    m.put("ts", System.currentTimeMillis());
                    if (hidden) m.put("h", 1);
                    s.getJSONArray("msgs").put(m);
                    break;
                }
            }
            Store.putArr(c, key, a);
        } catch (Exception e) {
            // kaydedilemedi
        }
    }

    public static String preview(JSONObject s) {
        JSONArray m = s.optJSONArray("msgs");
        if (m != null) {
            for (int i = 0; i < m.length(); i++) {
                JSONObject x = m.optJSONObject(i);
                if (x != null && "user".equals(x.optString("r")) && x.optInt("h", 0) == 0) {
                    String t = x.optString("t");
                    return t.length() > 60 ? t.substring(0, 60) + "…" : t;
                }
            }
        }
        return "(boş sohbet)";
    }
}
