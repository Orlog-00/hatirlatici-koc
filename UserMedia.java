package com.koc.hatirlatici;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import org.json.JSONArray;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Kullanıcının kendi seçtiği resim/video dosyaları (telefondan). */
public class UserMedia {

    public static List<String> list(Context c) {
        List<String> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(Store.get(c, "media", "[]"));
            for (int i = 0; i < a.length(); i++) l.add(a.getString(i));
        } catch (Exception e) {
            // boş liste
        }
        return l;
    }

    public static void add(Context c, String uri) {
        List<String> l = list(c);
        if (!l.contains(uri)) l.add(uri);
        JSONArray a = new JSONArray();
        for (String s : l) a.put(s);
        Store.put(c, "media", a.toString());
    }

    public static void clear(Context c) {
        Store.put(c, "media", "[]");
    }

    public static boolean isVideo(Context c, String uri) {
        try {
            String t = c.getContentResolver().getType(Uri.parse(uri));
            return t != null && t.startsWith("video");
        } catch (Exception e) {
            return false;
        }
    }

    /** Resmi küçülterek yükler; video ise bir karesini alır. Başarısızsa null. */
    public static Bitmap bitmap(Context c, String uri, int w, int h) {
        try {
            Uri u = Uri.parse(uri);
            if (isVideo(c, uri)) {
                MediaMetadataRetriever mr = new MediaMetadataRetriever();
                try {
                    mr.setDataSource(c, u);
                    return mr.getFrameAtTime(500000);
                } finally {
                    mr.release();
                }
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream is = c.getContentResolver().openInputStream(u);
            BitmapFactory.decodeStream(is, null, o);
            if (is != null) is.close();
            int s = 1;
            while (o.outWidth / (s * 2) >= w && o.outHeight / (s * 2) >= h) s *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = s;
            is = c.getContentResolver().openInputStream(u);
            Bitmap b = BitmapFactory.decodeStream(is, null, o2);
            if (is != null) is.close();
            return b;
        } catch (Exception e) {
            return null;
        }
    }
}
