package com.koc.hatirlatici;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/** Tüm veriler telefonda kalıcı olarak saklanır (uygulama/telefon kapansa da silinmez). */
public class Store {
    static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("koc", Context.MODE_PRIVATE);
    }

    public static List<Task> tasks(Context c) {
        List<Task> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p(c).getString("tasks", "[]"));
            for (int i = 0; i < a.length(); i++) l.add(Task.fromJson(a.getJSONObject(i)));
        } catch (Exception e) {
            // bozuk veri: boş liste
        }
        return l;
    }

    public static void saveTasks(Context c, List<Task> l) {
        JSONArray a = new JSONArray();
        try {
            for (Task t : l) a.put(t.toJson());
        } catch (Exception e) {
            return;
        }
        p(c).edit().putString("tasks", a.toString()).commit();
    }

    public static String get(Context c, String k, String d) {
        return p(c).getString(k, d);
    }

    public static void put(Context c, String k, String v) {
        p(c).edit().putString(k, v).commit();
    }
}
