package com.koc.hatirlatici;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Ortak sohbet görünümü (koç + dil sohbeti). Mesajlar tarih/saatle kalıcı kaydedilir, silinemez/değiştirilemez. */
public class ChatUi {
    public interface Sys {
        String get();
    }

    /**
     * @param opening  oturum boşsa yapay zekâya gönderilecek gizli ilk istek (null ise yok)
     * @param ttsLang  doluysa yapay zekâ cevapları sesli okunabilir
     * @param sttLang  mikrofon dili (null ise mikrofon yok)
     */
    public static void build(final MainActivity a, LinearLayout page, final String key, final String sid,
                             final Sys sys, final String opening, final String ttsLang, final String sttLang) {
        // model bilgisi
        TextView mdl = Ui.tv(a, a.key().isEmpty()
                ? "⚠ API anahtarı girilmemiş (Ayarlar > Yapay zekâ). Geçmişi okuyabilirsin."
                : "🤖 Kullanılan model: " + a.modelLabel(), 12, a.key().isEmpty() ? Ui.WARN : Ui.SUB, false);
        mdl.setPadding(0, 0, 0, Ui.dp(a, 6));
        page.addView(mdl);

        final ScrollView sv = new ScrollView(a);
        final LinearLayout list = Ui.col(a);
        sv.addView(list, new ViewGroup.LayoutParams(-1, -2));
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        JSONObject s = Sessions.find(a, key, sid);
        JSONArray msgs = s == null ? new JSONArray() : s.optJSONArray("msgs");
        if (msgs != null) {
            for (int i = 0; i < msgs.length(); i++) {
                JSONObject m = msgs.optJSONObject(i);
                if (m != null && m.optInt("h", 0) == 0) bubble(a, list, m.optString("r"), m.optString("t"), m.optLong("ts"), ttsLang);
            }
        }
        scrollDown(sv);

        // giriş alanı
        final EditText in = Ui.et(a, "Mesajını yaz…", false);
        in.setSingleLine(false);
        in.setMaxLines(4);
        if (sttLang != null) page.addView(SpeechBox.build(a, in, sttLang));
        LinearLayout bar = Ui.row(a);
        bar.setPadding(0, Ui.dp(a, 6), 0, 0);
        final android.widget.Button send = Ui.btn(a, "Gönder", Ui.ACC, null);
        LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        il.rightMargin = Ui.dp(a, 8);
        bar.addView(in, il);
        bar.addView(send);
        page.addView(bar);

        final boolean[] waiting = {false};
        final Runnable ask = new Runnable() {
            @Override
            public void run() {
                waiting[0] = true;
                final TextView typing = Ui.tv(a, "…yazıyor", 13, Ui.SUB, false);
                list.addView(typing);
                scrollDown(sv);
                a.aiChat(sys.get(), history(a, key, sid), 1200, null, (txt, err) -> {
                    list.removeView(typing);
                    waiting[0] = false;
                    if (txt == null || txt.isEmpty()) return;
                    Sessions.addMsg(a, key, sid, "assistant", txt, false);
                    bubble(a, list, "assistant", txt, System.currentTimeMillis(), ttsLang);
                    scrollDown(sv);
                    if (ttsLang != null && "1".equals(Store.get(a, "autospeak", "0"))) a.speak(txt, ttsLang);
                });
            }
        };
        send.setOnClickListener(v -> {
            String t = in.getText().toString().trim();
            if (t.isEmpty() || waiting[0]) return;
            if (a.key().isEmpty()) {
                a.toast("Önce Ayarlar > Yapay zekâ bölümüne API anahtarını gir.");
                return;
            }
            a.stopListen();
            in.setText("");
            Sessions.addMsg(a, key, sid, "user", t, false);
            bubble(a, list, "user", t, System.currentTimeMillis(), null);
            scrollDown(sv);
            ask.run();
        });

        JSONObject s2 = Sessions.find(a, key, sid);
        boolean empty = s2 == null || s2.optJSONArray("msgs") == null || s2.optJSONArray("msgs").length() == 0;
        if (empty && opening != null && !a.key().isEmpty()) {
            Sessions.addMsg(a, key, sid, "user", opening, true);
            ask.run();
        }
    }

    static void scrollDown(final ScrollView sv) {
        sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }

    /** Oturumdaki mesajlardan API'ye gidecek diziyi kurar (aynı rolleri birleştirir, ilk mesaj kullanıcıdan olur). */
    static JSONArray history(MainActivity a, String key, String sid) {
        JSONArray out = new JSONArray();
        JSONObject s = Sessions.find(a, key, sid);
        if (s == null) return out;
        JSONArray ms = s.optJSONArray("msgs");
        List<String[]> l = new ArrayList<>();
        for (int i = 0; ms != null && i < ms.length(); i++) {
            JSONObject m = ms.optJSONObject(i);
            if (m == null) continue;
            String r = "user".equals(m.optString("r")) ? "user" : "assistant";
            String t = m.optString("t");
            if (l.isEmpty() && r.equals("assistant")) continue;
            if (!l.isEmpty() && l.get(l.size() - 1)[0].equals(r)) {
                l.get(l.size() - 1)[1] += "\n\n" + t;
            } else {
                l.add(new String[]{r, t});
            }
        }
        try {
            for (String[] x : l) {
                JSONObject o = new JSONObject();
                o.put("role", x[0]);
                o.put("content", x[1]);
                out.put(o);
            }
        } catch (Exception e) {
            // boş
        }
        return out;
    }

    static void bubble(final MainActivity a, LinearLayout list, String role, final String text, long ts, final String ttsLang) {
        boolean me = "user".equals(role);
        LinearLayout b = Ui.col(a);
        b.setBackground(Ui.round(a, me ? Ui.ACC : Ui.CARD, 16));
        int p = Ui.dp(a, 12);
        b.setPadding(p, p, p, p);
        TextView t = Ui.tv(a, text, 15, Color.WHITE, false);
        t.setTextIsSelectable(true);
        b.addView(t);
        LinearLayout meta = Ui.row(a);
        meta.addView(Ui.tv(a, Ui.fmt(ts), 10.5f, me ? 0xCCFFFFFF : Ui.SUB, false),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!me && ttsLang != null) {
            TextView sp = Ui.tv(a, "🔊 Dinle", 12, Ui.SUB, true);
            sp.setOnClickListener(v -> a.speak(text, ttsLang));
            meta.addView(sp);
        }
        meta.setPadding(0, Ui.dp(a, 4), 0, 0);
        b.addView(meta);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.gravity = me ? Gravity.END : Gravity.START;
        lp.bottomMargin = Ui.dp(a, 8);
        lp.leftMargin = me ? Ui.dp(a, 40) : 0;
        lp.rightMargin = me ? 0 : Ui.dp(a, 40);
        list.addView(b, lp);
    }
}
