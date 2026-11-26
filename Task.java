package com.koc.hatirlatici;

import org.json.JSONException;
import org.json.JSONObject;

public class Task {
    public String id = "";
    public String title = "";
    public long deadline, everyMs, next, created;
    public int sound, vib, done;
    public boolean active = true;

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
        return t;
    }
}
