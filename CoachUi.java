package com.koc.hatirlatici;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Koç sekmesi: birden çok kariyer alanı, her biri için hedef + zamanla biriken (silinemez/düzeltilemez) ilerleme
 * kayıtları, başarılan kariyerler ve kayıtlı koç sohbetleri. Her şey Store'a (yedeklenir) tarih/saatle yazılır.
 */
public class CoachUi {
    static final String CAREERS = "careers", ENTRIES = "career_entries", SESSIONS = "coach_sessions";
    static final String[] TEMPO = {
            "1 · Hiç ilerleyemedim", "2 · Çok az ilerledim", "3 · Orta: planım kadar gittim",
            "4 · İyi gittim", "5 · Harika: planın önündeyim"};

    // ------------------------------------------------------------ veri yardımcıları

    static List<JSONObject> careers(MainActivity a) {
        List<JSONObject> l = new ArrayList<>();
        JSONArray ar = Store.arr(a, CAREERS);
        for (int i = 0; i < ar.length(); i++) if (ar.optJSONObject(i) != null) l.add(ar.optJSONObject(i));
        return l;
    }

    static JSONObject career(MainActivity a, String id) {
        for (JSONObject o : careers(a)) if (id.equals(o.optString("id"))) return o;
        return null;
    }

    /** Bir kariyerin kayıtları, eskiden yeniye. */
    static List<JSONObject> entries(MainActivity a, String cid) {
        List<JSONObject> l = new ArrayList<>();
        JSONArray ar = Store.arr(a, ENTRIES);
        for (int i = 0; i < ar.length(); i++) {
            JSONObject o = ar.optJSONObject(i);
            if (o != null && cid.equals(o.optString("career"))) l.add(o);
        }
        Collections.sort(l, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject x, JSONObject y) {
                return Long.compare(x.optLong("ts"), y.optLong("ts"));
            }
        });
        return l;
    }

    static int latestPct(MainActivity a, String cid) {
        List<JSONObject> e = entries(a, cid);
        return e.isEmpty() ? 0 : e.get(e.size() - 1).optInt("pct");
    }

    static void setStatus(MainActivity a, String id, String st) {
        JSONArray ar = Store.arr(a, CAREERS);
        for (int i = 0; i < ar.length(); i++) {
            JSONObject o = ar.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) {
                try {
                    o.put("status", st);
                    if ("achieved".equals(st)) o.put("achievedAt", System.currentTimeMillis());
                } catch (Exception e) {
                    // yoksay
                }
            }
        }
        Store.putArr(a, CAREERS, ar);
    }

    // ------------------------------------------------------------ ana sayfa

    public static void show(final MainActivity a) {
        LinearLayout pg = a.pageCol("Kariyer Koçum", null);

        LinearLayout info = Ui.card(a);
        info.addView(Ui.tv(a, "🤖 " + (a.key().isEmpty() ? "Koç sohbeti için Ayarlar > Yapay zekâ'dan API anahtarı gir."
                : "Koç modeli: " + a.modelLabel()), 13, a.key().isEmpty() ? Ui.WARN : Ui.SUB, false));
        info.addView(Ui.tv(a, "Tüm kariyer, ilerleme ve sohbet kayıtların tarih/saatle saklanır ve silinemez/düzeltilemez. "
                + "Telefon kapansa da silinmez. Uygulamayı kaldırsan bile kurtarmak için Ayarlar > Yedekleme'den "
                + "bir yedek dosyası seç.", 12, Ui.SUB, false));
        Ui.add(pg, info);

        LinearLayout how = Ui.section(a, pg, "coach_how", "ℹ İlerleme kaydı ve 1-5 tempo nasıl çalışır?",
                "Haftalık değerlendirmenin açıklaması");
        how.addView(Ui.tv(a, "Her kariyer alanı için zaman zaman (örn. haftada bir) yeni bir ilerleme kaydı eklersin. "
                + "Her kayıtta üç şey girersin:\n\n"
                + "• Toplam ilerleme (%): hedefine şu an ne kadar yaklaştığını 0-100 arası işaretlersin. "
                + "Önceki kayda göre farkı (+5 gibi) otomatik gösterilir.\n\n"
                + "• Tempo (1-5): O dönemde planına göre nasıl gittiğinin puanı. Yüzde 'neredeyim', tempo ise 'bu hafta nasıl gittim' demektir:\n"
                + "   1 = Hiç ilerleyemedim\n   2 = Çok az ilerledim\n   3 = Orta, planım kadar\n   4 = İyi gittim\n   5 = Harika, planın önündeyim\n\n"
                + "• Neler yaptım / nerede takıldım: Önceki kayıttan bu yana yaptıklarını yazarsın; böylece geriye bakıp "
                + "ne kadar yol aldığını görürsün.\n\n"
                + "Kayıtlar sonradan değiştirilemez; bu sayede dürüst bir ilerleme tarihçen oluşur. Koç da bu kayıtları okuyarak yorum yapar.",
                13.5f, Ui.TXT, false));

        // aktif / başarılan / duraklatılan
        List<JSONObject> all = careers(a);
        String[] st = {"active", "achieved", "paused"};
        String[] tt = {"AKTİF KARİYERLER", "🏆 BAŞARDIĞIM / BİTİRDİĞİM KARİYERLER", "DURAKLATILAN"};
        int total = 0;
        for (int k = 0; k < 3; k++) {
            List<JSONObject> l = new ArrayList<>();
            for (JSONObject o : all) {
                String s = o.optString("status", "active");
                if (s.equals(st[k])) l.add(o);
            }
            if (l.isEmpty()) continue;
            total += l.size();
            pg.addView(Ui.label(a, tt[k] + " (" + l.size() + ")"));
            for (final JSONObject o : l) Ui.add(pg, careerCard(a, o));
        }
        if (total == 0) {
            LinearLayout e = Ui.card(a);
            e.addView(Ui.tv(a, "Henüz kariyer alanı yok", 17, Color.WHITE, true));
            e.addView(Ui.tv(a, "İlgilendiğin alanları ekle (örn. Yazılım, Drone, Akademik kariyer). Birden fazla olabilir.", 14, Ui.SUB, false));
            Ui.add(pg, e);
        }

        Ui.add(pg, Ui.btn(a, "➕ Yeni kariyer alanı ekle", Ui.ACC, v -> newCareer(a)));
        Ui.add(pg, Ui.btn(a, "💬 Koçla genel sohbet başlat", 0xFF0E7490, v -> startChat(a, "", "")));
        Ui.add(pg, Ui.btn(a, "🕘 Geçmiş koç sohbetleri", Ui.CARD2, v -> history(a)));
    }

    static View careerCard(final MainActivity a, final JSONObject o) {
        final String id = o.optString("id");
        LinearLayout c = Ui.card(a);
        c.addView(Ui.tv(a, o.optString("name"), 18, Color.WHITE, true));
        c.addView(Ui.tv(a, "🎯 " + o.optString("goal"), 13, Ui.SUB, false));
        List<JSONObject> es = entries(a, id);
        int pct = es.isEmpty() ? 0 : es.get(es.size() - 1).optInt("pct");
        LinearLayout r = Ui.row(a);
        r.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 4));
        r.addView(Ui.tv(a, "%" + pct, 15, Ui.OK, true));
        r.addView(Ui.tv(a, "   " + es.size() + " kayıt" + (es.isEmpty() ? "" : " · son: " + Ui.fmtShort(es.get(es.size() - 1).optLong("ts"))), 12, Ui.SUB, false));
        c.addView(r);
        c.addView(Ui.bar(a, pct, "achieved".equals(o.optString("status")) ? Ui.WARN : Ui.OK));
        c.setOnClickListener(v -> careerPage(a, id));
        TextView open = Ui.tv(a, "Aç ›", 13, Ui.ACC, true);
        open.setPadding(0, Ui.dp(a, 8), 0, 0);
        c.addView(open);
        return c;
    }

    static void newCareer(final MainActivity a) {
        LinearLayout pg = a.pageCol("Yeni kariyer alanı", () -> show(a));
        pg.addView(Ui.label(a, "ALAN ADI"));
        final EditText name = Ui.et(a, "örn. Yazılım mühendisliği", false);
        Ui.add(pg, name);
        pg.addView(Ui.label(a, "HEDEFİN"));
        final EditText goal = Ui.et(a, "Bu alanda neye ulaşmak istiyorsun? (örn. 1 yıl içinde staj bulmak)", true);
        Ui.add(pg, goal);
        Ui.add(pg, Ui.btn(a, "Kaydet", Ui.ACC, v -> {
            String n = name.getText().toString().trim();
            String g = goal.getText().toString().trim();
            if (n.isEmpty() || g.isEmpty()) {
                a.toast("Alan adını ve hedefini yaz.");
                return;
            }
            try {
                JSONObject o = new JSONObject();
                o.put("id", UUID.randomUUID().toString());
                o.put("name", n);
                o.put("goal", g);
                o.put("status", "active");
                o.put("created", System.currentTimeMillis());
                JSONArray ar = Store.arr(a, CAREERS);
                ar.put(o);
                Store.putArr(a, CAREERS, ar);
                careerPage(a, o.getString("id"));
            } catch (Exception e) {
                a.toast("Kaydedilemedi.");
            }
        }));
    }

    // ------------------------------------------------------------ kariyer sayfası

    static void careerPage(final MainActivity a, final String id) {
        final JSONObject o = career(a, id);
        if (o == null) {
            show(a);
            return;
        }
        LinearLayout pg = a.pageCol(o.optString("name"), () -> show(a));
        String stat = o.optString("status", "active");

        LinearLayout top = Ui.card(a);
        top.addView(Ui.tv(a, "🎯 Hedef", 12, Ui.SUB, true));
        top.addView(Ui.tv(a, o.optString("goal"), 16, Color.WHITE, false));
        top.addView(Ui.tv(a, "Eklendi: " + Ui.fmt(o.optLong("created"))
                + ("achieved".equals(stat) ? "\n🏆 Başarıldı: " + Ui.fmt(o.optLong("achievedAt")) : ""), 12, Ui.SUB, false));
        List<JSONObject> es = entries(a, id);
        int pct = es.isEmpty() ? 0 : es.get(es.size() - 1).optInt("pct");
        TextView pt = Ui.tv(a, "Şu anki ilerleme: %" + pct, 15, Ui.OK, true);
        pt.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 4));
        top.addView(pt);
        top.addView(Ui.bar(a, pct, Ui.OK));
        Ui.add(pg, top);

        if ("active".equals(stat)) {
            Ui.add(pg, Ui.btn(a, "➕ Yeni ilerleme kaydı ekle", Ui.ACC, v -> entryForm(a, id)));
        }
        Ui.add(pg, Ui.btn(a, "💬 Bu kariyer hakkında koçla konuş", 0xFF0E7490, v -> startChat(a, id, o.optString("name"))));

        LinearLayout sr = Ui.row(a);
        if ("active".equals(stat)) {
            Ui.addW(sr, Ui.btn(a, "🏆 Başardım / Bitirdim", 0xFF047857, v -> new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle("Başarılan kariyerlere taşınsın mı?")
                    .setMessage("Kayıtların silinmez, 'Başardığım kariyerler' bölümüne geçer.")
                    .setPositiveButton("Evet", (d, w) -> {
                        setStatus(a, id, "achieved");
                        careerPage(a, id);
                    }).setNegativeButton("Vazgeç", null).show()));
            Ui.addW(sr, Ui.btn(a, "⏸ Duraklat", Ui.CARD2, v -> {
                setStatus(a, id, "paused");
                careerPage(a, id);
            }));
        } else {
            Ui.addW(sr, Ui.btn(a, "▶ Tekrar aktif yap", Ui.ACC, v -> {
                setStatus(a, id, "active");
                careerPage(a, id);
            }));
        }
        Ui.add(pg, sr);

        pg.addView(Ui.label(a, "İLERLEME GEÇMİŞİ (" + es.size() + " kayıt, düzeltilemez)"));
        if (es.isEmpty()) {
            pg.addView(Ui.tv(a, "Henüz kayıt yok. İlk kaydını ekleyince burada tarih sırasıyla birikir.", 14, Ui.SUB, false));
        }
        for (int i = es.size() - 1; i >= 0; i--) {
            JSONObject e = es.get(i);
            int prev = i > 0 ? es.get(i - 1).optInt("pct") : 0;
            int delta = e.optInt("pct") - prev;
            LinearLayout c = Ui.card(a);
            c.addView(Ui.tv(a, "📅 " + Ui.fmt(e.optLong("ts")), 12, Ui.SUB, true));
            c.addView(Ui.tv(a, "%" + e.optInt("pct") + "   (" + (delta >= 0 ? "+" : "") + delta + " önceki kayda göre)", 16, Ui.OK, true));
            int tp = Math.max(1, Math.min(5, e.optInt("tempo", 3)));
            c.addView(Ui.tv(a, "Tempo: " + TEMPO[tp - 1], 13, Ui.WARN, false));
            TextView did = Ui.tv(a, "Yaptıklarım:\n" + e.optString("did"), 14, Ui.TXT, false);
            did.setPadding(0, Ui.dp(a, 6), 0, 0);
            did.setTextIsSelectable(true);
            c.addView(did);
            if (!e.optString("block").isEmpty()) {
                TextView bl = Ui.tv(a, "Takıldığım yer:\n" + e.optString("block"), 14, Ui.SUB, false);
                bl.setPadding(0, Ui.dp(a, 6), 0, 0);
                bl.setTextIsSelectable(true);
                c.addView(bl);
            }
            Ui.add(pg, c);
        }
    }

    static void entryForm(final MainActivity a, final String cid) {
        final JSONObject car = career(a, cid);
        if (car == null) return;
        LinearLayout pg = a.pageCol("Yeni ilerleme kaydı", () -> careerPage(a, cid));
        pg.addView(Ui.tv(a, car.optString("name") + " · Hedef: " + car.optString("goal"), 13, Ui.SUB, false));
        List<JSONObject> es = entries(a, cid);
        final int prevPct = es.isEmpty() ? 0 : es.get(es.size() - 1).optInt("pct");
        if (!es.isEmpty()) {
            JSONObject last = es.get(es.size() - 1);
            LinearLayout lc = Ui.card(a);
            lc.addView(Ui.tv(a, "Önceki kaydın (" + Ui.fmtShort(last.optLong("ts")) + ") — %" + prevPct, 12, Ui.SUB, true));
            lc.addView(Ui.tv(a, last.optString("did"), 13, Ui.TXT, false));
            Ui.add(pg, lc);
        }

        pg.addView(Ui.label(a, "1) TOPLAM İLERLEME"));
        final TextView pv = Ui.tv(a, "%" + prevPct, 22, Ui.OK, true);
        pg.addView(pv);
        final SeekBar sb = new SeekBar(a);
        sb.setMax(100);
        sb.setProgress(prevPct);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                int d = p - prevPct;
                pv.setText("%" + p + "   (" + (d >= 0 ? "+" : "") + d + " önceki kayda göre)");
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        Ui.add(pg, sb);

        pg.addView(Ui.label(a, "2) BU DÖNEMKİ TEMPON (1-5)"));
        pg.addView(Ui.tv(a, "Planına göre bu dönem nasıl gittin? Yüzde 'neredeyim', tempo ise 'bu hafta nasıl gittim' demek.", 12, Ui.SUB, false));
        final int[] tempo = {3};
        final List<TextView> chips = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int ti = i;
            final TextView ch = Ui.chip(a, TEMPO[i], i == 2, null);
            ch.setGravity(android.view.Gravity.START);
            ch.setOnClickListener(v -> {
                tempo[0] = ti + 1;
                for (int k = 0; k < chips.size(); k++) chips.get(k).setBackground(Ui.round(a, k == ti ? Ui.ACC : Ui.CARD2, 20));
            });
            chips.add(ch);
            Ui.add(pg, ch);
        }

        pg.addView(Ui.label(a, "3) ÖNCEKİ KAYDINDAN BU YANA NE YAPTIN?"));
        final EditText did = Ui.et(a, "Yaptığın somut işleri yaz…", true);
        Ui.add(pg, did);
        pg.addView(Ui.label(a, "4) NEREDE TAKILDIN? (isteğe bağlı)"));
        final EditText block = Ui.et(a, "Zorlandığın, aksattığın şeyler…", true);
        Ui.add(pg, block);
        pg.addView(Ui.tv(a, "⚠ Kaydedildikten sonra bu kayıt değiştirilemez.", 12, Ui.WARN, false));

        Ui.add(pg, Ui.btn(a, "Kaydet", Ui.ACC, v -> {
            String d = did.getText().toString().trim();
            if (d.isEmpty()) {
                a.toast("Neler yaptığını kısaca yaz.");
                return;
            }
            try {
                JSONObject e = new JSONObject();
                e.put("id", UUID.randomUUID().toString());
                e.put("career", cid);
                e.put("ts", System.currentTimeMillis());
                e.put("pct", sb.getProgress());
                e.put("tempo", tempo[0]);
                e.put("did", d);
                e.put("block", block.getText().toString().trim());
                JSONArray ar = Store.arr(a, ENTRIES);
                ar.put(e);
                Store.putArr(a, ENTRIES, ar);
            } catch (Exception ex) {
                a.toast("Kaydedilemedi.");
                return;
            }
            a.toast("Kaydedildi ✅");
            if (a.key().isEmpty()) {
                careerPage(a, cid);
            } else {
                new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                        .setTitle("Koça gönderilsin mi?")
                        .setMessage("Koç bu kaydı okuyup yorum yapsın ve sonraki adımlar önersin mi?")
                        .setPositiveButton("Koça gönder", (dg, w) -> startChat(a, cid, car.optString("name")))
                        .setNegativeButton("Şimdi değil", (dg, w) -> careerPage(a, cid)).show();
            }
        }));
    }

    // ------------------------------------------------------------ koç sohbeti

    static String snapshot(MainActivity a, String focusId) {
        StringBuilder sb = new StringBuilder();
        for (JSONObject o : careers(a)) {
            String id = o.optString("id");
            sb.append("- ").append(o.optString("name")).append(" [").append(o.optString("status", "active"))
                    .append("] hedef: ").append(o.optString("goal")).append("\n");
            List<JSONObject> es = entries(a, id);
            int from = Math.max(0, es.size() - (id.equals(focusId) ? 6 : 2));
            for (int i = from; i < es.size(); i++) {
                JSONObject e = es.get(i);
                sb.append("    · ").append(Ui.fmtShort(e.optLong("ts"))).append(" %").append(e.optInt("pct"))
                        .append(" tempo ").append(e.optInt("tempo")).append("/5 — yaptı: ").append(e.optString("did"))
                        .append(e.optString("block").isEmpty() ? "" : " — takıldı: " + e.optString("block")).append("\n");
            }
        }
        return sb.length() == 0 ? "(henüz kariyer kaydı yok)" : sb.toString();
    }

    static void startChat(final MainActivity a, final String cid, final String cname) {
        if (a.key().isEmpty()) {
            a.toast("Önce Ayarlar > Yapay zekâ bölümüne API anahtarını gir.");
            return;
        }
        try {
            JSONObject meta = new JSONObject();
            meta.put("career", cid);
            meta.put("title", cname.isEmpty() ? "Genel sohbet" : cname);
            JSONObject s = Sessions.create(a, SESSIONS, meta);
            openChat(a, s.optString("id"), true);
        } catch (Exception e) {
            a.toast("Sohbet açılamadı.");
        }
    }

    static void openChat(final MainActivity a, final String sid, boolean fresh) {
        final JSONObject s = Sessions.find(a, SESSIONS, sid);
        if (s == null) {
            show(a);
            return;
        }
        final String cid = s.optString("career");
        LinearLayout page = a.fixedPage("Koç · " + s.optString("title"), () -> {
            if (cid.isEmpty()) show(a);
            else careerPage(a, cid);
        });
        String opening;
        if (cid.isEmpty()) {
            opening = "Merhaba koçum. Kariyer kayıtlarım aşağıda. Kısaca durumumu değerlendir ve bana ilk sorunu sor.\n\n" + snapshot(a, "");
        } else {
            opening = "Kariyer alanım: " + s.optString("title") + ". Son ilerleme kayıtlarım aşağıda. Kayıtlara bakarak "
                    + "ilerlememi dürüstçe değerlendir (yüzdeler, tempo ve yazdıklarım arasındaki ilişkiye dikkat et), "
                    + "önümüzdeki hafta için 3 somut adım öner ve bana bir soru sor.\n\n" + snapshot(a, cid);
        }
        ChatUi.build(a, page, SESSIONS, sid, () -> system(a, cid), opening, null, "tr-TR");
    }

    static String system(MainActivity a, String focusId) {
        return "Sen kullanıcının kişisel kariyer koçusun. Türkçe, sıcak ama dürüst ve net konuş. Kullanıcı Yazılım Mühendisliği "
                + "öğrencisi. Genel öğüt yerine somut, küçük ve bu haftaya sığan adımlar öner. Cevapların kısa olsun (en fazla ~200 kelime), "
                + "her cevabın sonunda tek bir yönlendirici soru sor. Kullanıcının ilerleme kayıtlarını bilgin olarak kullan, "
                + "uydurma bilgi verme.\n\nKullanıcının kariyer alanları ve kayıtları:\n" + snapshot(a, focusId);
    }

    static void history(final MainActivity a) {
        LinearLayout pg = a.pageCol("Geçmiş koç sohbetleri", () -> show(a));
        pg.addView(Ui.tv(a, "Eski sohbetlerin silinemez ve düzeltilemez; istediğin zaman açıp devam edebilirsin.", 12, Ui.SUB, false));
        JSONArray all = Sessions.all(a, SESSIONS);
        if (all.length() == 0) pg.addView(Ui.tv(a, "\nHenüz sohbet yok.", 14, Ui.SUB, false));
        for (int i = all.length() - 1; i >= 0; i--) {
            final JSONObject s = all.optJSONObject(i);
            if (s == null) continue;
            LinearLayout c = Ui.card(a);
            c.addView(Ui.tv(a, s.optString("title", "Sohbet") + " · " + Ui.fmt(s.optLong("created")), 14, Color.WHITE, true));
            c.addView(Ui.tv(a, Sessions.preview(s), 13, Ui.SUB, false));
            c.setOnClickListener(v -> openChat(a, s.optString("id"), false));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(a, 8);
            pg.addView(c, lp);
        }
    }
}
