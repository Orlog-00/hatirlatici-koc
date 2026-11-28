package com.koc.hatirlatici;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * Tüm veriyi (görevler, kariyer kayıtları, sohbetler, sınav sonuçları...) telefondaki bir dosyaya yedekler.
 * Uygulama silinse bile dosya telefonda kalır; yeniden kurunca "Yedekten geri yükle" ile her şey döner.
 */
public class Backup {
    static final String[] SKIP = {"apikey", "deck", "backup_uri", "backup_time"};
    static final String APP = "hatirlatici-koc";

    static boolean skip(String k) {
        for (String s : SKIP) if (s.equals(k)) return true;
        return false;
    }

    public static String export(Context c) throws Exception {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, ?> e : Store.p(c).getAll().entrySet()) {
            if (skip(e.getKey())) continue;
            if (e.getValue() instanceof String) o.put(e.getKey(), e.getValue());
        }
        o.put("_app", APP);
        o.put("_ts", System.currentTimeMillis());
        return o.toString();
    }

    public static boolean restore(Context c, String json) {
        try {
            JSONObject o = new JSONObject(json);
            if (!APP.equals(o.optString("_app"))) return false;
            SharedPreferences.Editor ed = Store.p(c).edit();
            Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                if (k.startsWith("_") || skip(k)) continue;
                ed.putString(k, o.getString(k));
            }
            ed.commit();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean writeTo(Context c, Uri u) {
        try {
            OutputStream os = c.getContentResolver().openOutputStream(u, "wt");
            if (os == null) return false;
            os.write(export(c).getBytes("UTF-8"));
            os.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String readFrom(Context c, Uri u) {
        try {
            InputStream is = c.getContentResolver().openInputStream(u);
            if (is == null) return null;
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            is.close();
            return bo.toString("UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    /** Yedek dosyası seçilmişse her değişiklikte otomatik güncellenir. */
    public static void auto(Context c) {
        String u = Store.p(c).getString("backup_uri", "");
        if (u.isEmpty()) return;
        if (writeTo(c, Uri.parse(u))) {
            Store.p(c).edit().putString("backup_time", String.valueOf(System.currentTimeMillis())).apply();
        }
    }
}
