package com.koc.hatirlatici;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Yabancı dil sekmesi: gerçekçi 5 bölümlü seviye sınavı (dilbilgisi, okuma, dinleme, yazma, konuşma), yol haritası,
 * Duolingo tarzı dilbilgisi dersleri, seviyeye uygun SADECE hedef dilde sohbet ve kalıcı geçmiş.
 */
public class LangUi {
    static final String[] NAMES = {"İngilizce", "Almanca", "Fransızca", "İspanyolca", "İtalyanca", "Rusça", "Japonca", "Korece", "Arapça"};
    static final String[] EN = {"English", "German", "French", "Spanish", "Italian", "Russian", "Japanese", "Korean", "Arabic"};
    static final String[] TAGS = {"en-US", "de-DE", "fr-FR", "es-ES", "it-IT", "ru-RU", "ja-JP", "ko-KR", "ar-SA"};
    static final String[] LV = {"A1", "A2", "B1", "B2", "C1", "C2"};
    static final String TESTS = "lang_tests", CHATS = "lang_chats", LESSONS = "lang_lessons";

    static final String[][] TOPICS = {
            {"Kişi zamirleri ve 'olmak' fiili", "Sayılar, saat ve tarihler", "Şimdiki zaman (temel fiil çekimi)",
                    "Tanımlık / artikel yapısı", "Çoğul isimler", "Olumsuz cümle kurma", "Soru cümleleri", "Sahiplik ve 'sahip olmak'"},
            {"Geçmiş zaman (basit)", "Gelecek zaman ve planlar", "Karşılaştırma ve üstünlük", "Sıklık zarfları ve günlük rutin",
                    "Yardımcı / modal fiiller (yapabilmek, zorunda olmak)", "Edatlar ve yer-yön tarifi", "Sayılabilir-sayılamayan ve miktar", "Temel bağlaçlar"},
            {"Geçmişin şimdiyle ilişkisi (perfect yapıları)", "Geçmiş zamanların farkları", "Şart cümleleri (gerçek ve varsayımsal)",
                    "Edilgen çatı", "Dolaylı anlatım", "İlgi (bağıl) cümleleri", "Fiil + mastar / fiil + isim-fiil kalıpları", "Deyimsel / ayrılabilen fiil öbekleri"},
            {"Karma şart cümleleri", "Edilgenin ileri kullanımı", "Gelecekte tamamlanmış ve sürmekte zamanlar", "Bağlayıcılar ve akademik geçişler",
                    "Sıfat / zarf yapıları ve vurgu", "Dilek ve varsayım kipleri", "Dolaylı sorular", "Anlatı zamanları"},
            {"Devrik yapılar ve vurgu", "Resmî ve yazılı üslup", "Kalıp ifadeler ve deyimler", "Karmaşık yan cümleler",
                    "Soyut ve akademik ifadeler", "Modal ile çıkarım nüansları", "Eksiltili ve kısaltılmış yapılar", "Konuşma dili / resmî dil farkı"}
    };

    // ------------------------------------------------------------ veri

    static int sel(MainActivity a) {
        try {
            return Math.max(0, Math.min(NAMES.length - 1, Integer.parseInt(Store.get(a, "lang_sel", "0"))));
        } catch (Exception e) {
            return 0;
        }
    }

    static List<JSONObject> list(MainActivity a, String key, int lang) {
        List<JSONObject> l = new ArrayList<>();
        JSONArray ar = Store.arr(a, key);
        for (int i = 0; i < ar.length(); i++) {
            JSONObject o = ar.optJSONObject(i);
            if (o != null && o.optInt("lang", 0) == lang) l.add(o);
        }
        return l;
    }

    static void append(MainActivity a, String key, JSONObject o) {
        JSONArray ar = Store.arr(a, key);
        ar.put(o);
        Store.putArr(a, key, ar);
    }

    static JSONObject latestTest(MainActivity a, int lang) {
        List<JSONObject> t = list(a, TESTS, lang);
        return t.isEmpty() ? null : t.get(t.size() - 1);
    }

    static String level(MainActivity a, int lang) {
        JSONObject t = latestTest(a, lang);
        if (t != null && !t.optString("level").isEmpty()) return t.optString("level");
        String m = Store.get(a, "lang_manual_" + lang, "");
        return m.isEmpty() ? "A1" : m;
    }

    static int xp(MainActivity a, int lang) {
        int x = 0;
        for (JSONObject o : list(a, LESSONS, lang)) x += o.optInt("xp");
        return x;
    }

    static int lvIdx(String l) {
        for (int i = 0; i < LV.length; i++) if (LV[i].equals(l)) return i;
        return 0;
    }

    // ------------------------------------------------------------ ana sayfa

    public static void show(final MainActivity a) {
        final int lang = sel(a);
        LinearLayout pg = a.pageCol("Yabancı Dil", null);

        pg.addView(Ui.label(a, "ÖĞRENDİĞİN DİL"));
        Ui.add(pg, Ui.spin(a, NAMES, lang, i -> {
            Store.put(a, "lang_sel", String.valueOf(i));
            show(a);
        }));

        LinearLayout lc = Ui.card(a);
        JSONObject t = latestTest(a, lang);
        String lv = level(a, lang);
        LinearLayout lr = Ui.row(a);
        TextView badge = Ui.tv(a, lv, 28, Color.WHITE, true);
        badge.setBackground(Ui.round(a, Ui.ACC, 14));
        badge.setPadding(Ui.dp(a, 16), Ui.dp(a, 8), Ui.dp(a, 16), Ui.dp(a, 8));
        lr.addView(badge);
        LinearLayout lt = Ui.col(a);
        lt.setPadding(Ui.dp(a, 12), 0, 0, 0);
        lt.addView(Ui.tv(a, NAMES[lang] + " seviyen", 15, Color.WHITE, true));
        lt.addView(Ui.tv(a, t != null ? "Sınavla belirlendi: " + Ui.fmt(t.optLong("ts"))
                : "Henüz sınav yapmadın. Şimdilik bu seviye varsayılıyor.", 12, Ui.SUB, false));
        lt.addView(Ui.tv(a, "⭐ " + xp(a, lang) + " puan · " + list(a, LESSONS, lang).size() + " ders", 12, Ui.WARN, false));
        lr.addView(lt, new LinearLayout.LayoutParams(0, -2, 1f));
        lc.addView(lr);
        if (t == null) {
            TextView ml = Ui.tv(a, "Ya da seviyeni elle seç:", 12, Ui.SUB, false);
            ml.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 4));
            lc.addView(ml);
            lc.addView(Ui.spin(a, LV, lvIdx(lv), i -> Store.put(a, "lang_manual_" + lang, LV[i])));
        }
        Ui.add(pg, lc);

        Ui.add(pg, Ui.btn(a, "📝 Seviye belirleme sınavı (5 bölüm)", Ui.ACC, v -> examIntro(a)));
        Ui.add(pg, Ui.btn(a, "🗺 Yol haritam ve zayıf yönlerim", 0xFF0E7490, v -> roadmap(a)));
        Ui.add(pg, Ui.btn(a, "📚 Dilbilgisi dersleri (puanlı)", 0xFF047857, v -> lessonList(a, lvIdx(level(a, lang)))));
        Ui.add(pg, Ui.btn(a, "💬 " + NAMES[lang] + " konuşma sohbeti (sadece " + NAMES[lang] + ")", 0xFFB45309, v -> scenarios(a)));
        Ui.add(pg, Ui.btn(a, "🕘 Geçmiş: sınavlar, dersler, sohbetler", Ui.CARD2, v -> history(a)));
        pg.addView(Ui.tv(a, "Tüm sınav sonuçların, derslerin ve sohbetlerin tarih/saatle kalıcı saklanır. "
                + "Yapay zekâ için Ayarlar > Yapay zekâ'dan API anahtarı gerekir. Model: " + (a.key().isEmpty() ? "—" : a.modelLabel()), 12, Ui.SUB, false));
    }

    // ------------------------------------------------------------ seviye sınavı

    static class Ex {
        int lang;
        String est = "A1";
        String gr = "";
        int rdC, rdT, liC, liT;
        String writing = "";
        List<String> wrong = new ArrayList<>();
        List<String[]> speak = new ArrayList<>();
        JSONObject pack;
    }

    static LinearLayout examPage(final MainActivity a, String title) {
        return a.pageCol(title, () -> new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Sınavdan çıkılsın mı?")
                .setMessage("Çıkarsan bu sınavdaki ilerleme kaybolur.")
                .setPositiveButton("Çık", (d, w) -> {
                    a.stopListen();
                    a.stopSpeak();
                    show(a);
                })
                .setNegativeButton("Devam et", null).show());
    }

    static void examIntro(final MainActivity a) {
        final int lang = sel(a);
        LinearLayout pg = a.pageCol(NAMES[lang] + " seviye sınavı", () -> show(a));
        LinearLayout c = Ui.card(a);
        c.addView(Ui.tv(a, "Gerçek sınav gibi 5 bölüm", 17, Color.WHITE, true));
        c.addView(Ui.tv(a, "1) Dilbilgisi ve kelime (15 soru, A1'den C1'e kolaydan zora)\n"
                + "2) Okuma anlama (metin + sorular)\n"
                + "3) Dinleme (metni sesli dinlersin, soruları cevaplarsın)\n"
                + "4) Yazma (kısa bir kompozisyon)\n"
                + "5) Konuşma (3 soruyu sesli cevaplarsın)\n\n"
                + "İlk bölümdeki başarına göre diğer bölümler seviyene uyarlanır. Sonunda genel seviyen (A1–C2), "
                + "bölüm puanların, hata yaptığın yerler ve haftalık yol haritan çıkarılır ve kalıcı saklanır.\n\n"
                + "Süre: yaklaşık 20-30 dk. Sessiz bir yerde, kulaklık ve mikrofon izniyle çöz. Çıkarsan ilerleme kaybolur.\n"
                + "Konuşmada telefon konuşmanı yazıya çevirir; yanlış duyduğu yeri düzeltebilirsin.", 14, Ui.TXT, false));
        Ui.add(pg, c);
        Ui.add(pg, Ui.btn(a, "Sınava başla", Ui.ACC, v -> {
            if (a.key().isEmpty()) {
                a.toast("Önce Ayarlar > Yapay zekâ bölümüne API anahtarını gir.");
                return;
            }
            Ex ex = new Ex();
            ex.lang = lang;
            examGrammar(a, ex);
        }));
    }

    static final String EXAMINER = "You are an expert CEFR language examiner. Output ONLY valid JSON, no markdown, no commentary.";

    static void examGrammar(final MainActivity a, final Ex ex) {
        String user = "Create a placement test for learners of " + EN[ex.lang] + ". Write exactly 15 multiple-choice questions: "
                + "3 each for CEFR levels A1, A2, B1, B2, C1 in that order (easy to hard), mixing grammar and vocabulary. "
                + "Questions and options must be in " + EN[ex.lang] + " (e.g. sentences with a blank ___ or short direct questions). "
                + "Exactly 4 options, exactly one correct. 'answer' is the 0-based index of the correct option. "
                + "'explain' is a one-sentence explanation in Turkish. JSON: "
                + "{\"questions\":[{\"level\":\"A1\",\"q\":\"...\",\"options\":[\"..\",\"..\",\"..\",\"..\"],\"answer\":0,\"explain\":\"...\"}]}";
        a.aiJson(EXAMINER, user, 5000, "Sınav hazırlanıyor… (15-40 sn)", (o, err) -> {
            List<Mcq.Q> qs = o == null ? new ArrayList<>() : Mcq.parse(o.optJSONArray("questions"));
            if (qs.size() < 8) {
                if (err == null) a.toast("Sorular hazırlanamadı, tekrar dene.");
                show(a);
                return;
            }
            LinearLayout pg = examPage(a, "Bölüm 1/5 · Dilbilgisi ve kelime");
            LinearLayout host = Ui.col(a);
            pg.addView(host);
            Mcq.run(a, host, qs, false, false, (all, correct, xp) -> {
                int[] c = new int[5];
                int[] n = new int[5];
                StringBuilder sb = new StringBuilder();
                for (Mcq.Q q : all) {
                    int li = Math.min(4, lvIdx(q.level));
                    n[li]++;
                    if (q.ok) c[li]++;
                    else ex.wrong.add("[" + q.level + "] " + q.q + " | doğru: " + q.opts[q.ans]);
                }
                int run = 0;
                for (int i = 0; i < 5; i++) {
                    sb.append(LV[i]).append(": ").append(c[i]).append("/").append(n[i]).append("  ");
                    if (n[i] > 0 && c[i] * 3 >= n[i] * 2 && run == i) run++;
                }
                ex.gr = sb.toString().trim() + " (toplam " + correct + "/" + all.size() + ")";
                ex.est = LV[Math.max(0, Math.min(4, run == 0 ? 0 : run - 1))];
                examPack(a, ex);
            });
        });
    }

    static void examPack(final MainActivity a, final Ex ex) {
        String user = "A learner of " + EN[ex.lang] + " has an estimated level of " + ex.est + ". Create the remaining placement test sections at about this level, all in "
                + EN[ex.lang] + ": (1) reading: a passage of 90-150 words and 4 multiple-choice questions; (2) listening: a short dialogue or monologue script "
                + "of 60-100 words and 3 multiple-choice questions that can only be answered by listening to it; (3) writing: one writing task prompt "
                + "(expect 60-100 words); (4) speaking: 3 speaking prompts of increasing difficulty. Multiple-choice format: exactly 4 options, 'answer' = 0-based "
                + "index of the correct option, 'explain' = one Turkish sentence. JSON: "
                + "{\"reading\":{\"text\":\"..\",\"questions\":[{\"q\":\"..\",\"options\":[\"..\",\"..\",\"..\",\"..\"],\"answer\":0,\"explain\":\"..\"}]},"
                + "\"listening\":{\"script\":\"..\",\"questions\":[...]},\"writing\":{\"prompt\":\"..\"},\"speaking\":{\"prompts\":[\"..\",\"..\",\"..\"]}}";
        a.aiJson(EXAMINER, user, 6000, "Okuma, dinleme, yazma ve konuşma bölümleri hazırlanıyor…", (o, err) -> {
            if (o == null || o.optJSONObject("reading") == null || o.optJSONObject("listening") == null) {
                if (err == null) a.toast("Bölümler hazırlanamadı, tekrar dene.");
                show(a);
                return;
            }
            ex.pack = o;
            examReading(a, ex);
        });
    }

    static void examReading(final MainActivity a, final Ex ex) {
        JSONObject r = ex.pack.optJSONObject("reading");
        List<Mcq.Q> qs = Mcq.parse(r.optJSONArray("questions"));
        LinearLayout pg = examPage(a, "Bölüm 2/5 · Okuma");
        pg.addView(Ui.tv(a, "Metni oku, sonra soruları cevapla.", 13, Ui.SUB, false));
        LinearLayout c = Ui.card(a);
        TextView tx = Ui.tv(a, r.optString("text"), 15, Ui.TXT, false);
        tx.setTextIsSelectable(true);
        c.addView(tx);
        Ui.add(pg, c);
        LinearLayout host = Ui.col(a);
        pg.addView(host);
        ex.rdT = qs.size();
        if (qs.isEmpty()) {
            examListening(a, ex);
            return;
        }
        Mcq.run(a, host, qs, false, false, (all, correct, xp) -> {
            ex.rdC = correct;
            examListening(a, ex);
        });
    }

    static void examListening(final MainActivity a, final Ex ex) {
        final JSONObject l = ex.pack.optJSONObject("listening");
        List<Mcq.Q> qs = Mcq.parse(l.optJSONArray("questions"));
        LinearLayout pg = examPage(a, "Bölüm 3/5 · Dinleme");
        pg.addView(Ui.tv(a, "Metin yazılı gösterilmez. Dinle (istediğin kadar tekrar edebilirsin) ve soruları cevapla.", 13, Ui.SUB, false));
        LinearLayout r = Ui.row(a);
        Ui.addW(r, Ui.btn(a, "▶ Dinle", Ui.ACC, v -> a.speak(l.optString("script"), TAGS[ex.lang])));
        Ui.addW(r, Ui.btn(a, "⏹ Durdur", Ui.CARD2, v -> a.stopSpeak()));
        Ui.add(pg, r);
        LinearLayout host = Ui.col(a);
        pg.addView(host);
        ex.liT = qs.size();
        if (qs.isEmpty()) {
            examWriting(a, ex);
            return;
        }
        Mcq.run(a, host, qs, false, false, (all, correct, xp) -> {
            a.stopSpeak();
            ex.liC = correct;
            examWriting(a, ex);
        });
    }

    static void examWriting(final MainActivity a, final Ex ex) {
        JSONObject w = ex.pack.optJSONObject("writing");
        LinearLayout pg = examPage(a, "Bölüm 4/5 · Yazma");
        LinearLayout c = Ui.card(a);
        c.addView(Ui.tv(a, "Görev (" + NAMES[ex.lang] + " yaz):", 12, Ui.SUB, true));
        c.addView(Ui.tv(a, w == null ? "Write about your daily life." : w.optString("prompt"), 16, Color.WHITE, false));
        Ui.add(pg, c);
        final EditText et = Ui.et(a, NAMES[ex.lang] + " yaz (en az 40 karakter)…", true);
        et.setMinLines(8);
        Ui.add(pg, et);
        Ui.add(pg, Ui.btn(a, "Gönder ve devam et →", Ui.ACC, v -> {
            String t = et.getText().toString().trim();
            if (t.length() < 40) {
                a.toast("Biraz daha uzun yaz (en az 40 karakter).");
                return;
            }
            ex.writing = t;
            examSpeaking(a, ex, 0);
        }));
    }

    static void examSpeaking(final MainActivity a, final Ex ex, final int idx) {
        JSONObject sp = ex.pack.optJSONObject("speaking");
        JSONArray ps = sp == null ? null : sp.optJSONArray("prompts");
        final int total = ps == null ? 0 : Math.min(3, ps.length());
        if (idx >= total) {
            examFinish(a, ex);
            return;
        }
        final String prompt = ps.optString(idx);
        LinearLayout pg = examPage(a, "Bölüm 5/5 · Konuşma " + (idx + 1) + "/" + total);
        LinearLayout c = Ui.card(a);
        c.addView(Ui.tv(a, "Soru:", 12, Ui.SUB, true));
        c.addView(Ui.tv(a, prompt, 17, Color.WHITE, true));
        Ui.add(pg, c);
        Ui.add(pg, Ui.btn(a, "🔊 Soruyu dinle", Ui.CARD2, v -> a.speak(prompt, TAGS[ex.lang])));
        final EditText et = Ui.et(a, "Konuştukların burada yazıya dökülür…", true);
        et.setMinLines(4);
        pg.addView(SpeechBox.build(a, et, TAGS[ex.lang]));
        Ui.add(pg, et);
        Ui.add(pg, Ui.btn(a, idx + 1 < total ? "Sonraki soru →" : "Sınavı bitir ve değerlendir", Ui.ACC, v -> {
            String t = et.getText().toString().trim();
            if (t.length() < 5) {
                a.toast("Önce soruyu cevapla (mikrofona dokun ya da yaz).");
                return;
            }
            a.stopListen();
            ex.speak.add(new String[]{prompt, t});
            examSpeaking(a, ex, idx + 1);
        }));
    }

    static void examFinish(final MainActivity a, final Ex ex) {
        StringBuilder d = new StringBuilder();
        d.append("Language: ").append(EN[ex.lang]).append("\n");
        d.append("Grammar/vocabulary correct by level: ").append(ex.gr).append("\n");
        d.append("Missed grammar questions:\n");
        for (String w : ex.wrong) d.append(" - ").append(w).append("\n");
        d.append("Reading: ").append(ex.rdC).append("/").append(ex.rdT).append("  Listening: ").append(ex.liC).append("/").append(ex.liT).append("\n");
        d.append("Writing task: ").append(ex.pack.optJSONObject("writing") == null ? "" : ex.pack.optJSONObject("writing").optString("prompt")).append("\n");
        d.append("Learner's writing:\n").append(ex.writing).append("\n");
        d.append("Speaking (transcribed by phone speech recognition, so ignore punctuation, capitalization and tiny recognition glitches):\n");
        for (String[] s : ex.speak) d.append(" Q: ").append(s[0]).append("\n A: ").append(s[1]).append("\n");
        String user = d + "\nEvaluate this learner like a CEFR examiner. Write all human-readable text in TURKISH (keep target-language examples in " + EN[ex.lang] + "). JSON only: "
                + "{\"level\":\"A1|A2|B1|B2|C1|C2\",\"scores\":{\"grammar\":0-100,\"reading\":0-100,\"listening\":0-100,\"writing\":0-100,\"speaking\":0-100},"
                + "\"summary\":\"3-4 sentences\",\"strengths\":[\"..\"],\"weak\":[{\"topic\":\"..\",\"detail\":\"specific mistakes seen, quote the learner's wrong sentence and give the corrected one\"}],"
                + "\"roadmap\":[{\"period\":\"1. hafta\",\"focus\":\"..\",\"tasks\":[\"concrete task with daily minutes\",\"..\"]}]} "
                + "Give 3-6 weak areas and a 4-6 period roadmap that starts from this learner's actual level and mistakes.";
        a.aiJson(EXAMINER, user, 4500, "Sonuçlar değerlendiriliyor ve yol haritası hazırlanıyor…", (o, err) -> {
            if (o == null) {
                new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                        .setTitle("Değerlendirme yapılamadı")
                        .setMessage("Cevapların hâlâ duruyor. İnternetini kontrol edip tekrar deneyebilirsin.")
                        .setCancelable(false)
                        .setPositiveButton("Tekrar dene", (dg, w) -> examFinish(a, ex))
                        .setNegativeButton("Çık", (dg, w) -> show(a)).show();
                return;
            }
            try {
                String lv = o.optString("level");
                if (lvIdx(lv) == 0 && !"A1".equals(lv)) o.put("level", ex.est);
                o.put("id", UUID.randomUUID().toString());
                o.put("lang", ex.lang);
                o.put("ts", System.currentTimeMillis());
                o.put("gr", ex.gr);
                o.put("rd", ex.rdC + "/" + ex.rdT);
                o.put("li", ex.liC + "/" + ex.liT);
                o.put("writing", ex.writing);
                JSONArray sa = new JSONArray();
                for (String[] s : ex.speak) {
                    JSONObject so = new JSONObject();
                    so.put("q", s[0]);
                    so.put("a", s[1]);
                    sa.put(so);
                }
                o.put("speaking_log", sa);
                append(a, TESTS, o);
            } catch (Exception e) {
                a.toast("Sonuç kaydedilemedi.");
            }
            report(a, o, () -> show(a));
        });
    }

    // ------------------------------------------------------------ sonuç raporu / yol haritası

    static void report(final MainActivity a, JSONObject t, Runnable back) {
        LinearLayout pg = a.pageCol("Sınav sonucu", back);
        int lang = t.optInt("lang");
        LinearLayout top = Ui.card(a);
        LinearLayout r = Ui.row(a);
        TextView badge = Ui.tv(a, t.optString("level", "?"), 34, Color.WHITE, true);
        badge.setBackground(Ui.round(a, Ui.ACC, 16));
        badge.setPadding(Ui.dp(a, 18), Ui.dp(a, 10), Ui.dp(a, 18), Ui.dp(a, 10));
        r.addView(badge);
        LinearLayout tx = Ui.col(a);
        tx.setPadding(Ui.dp(a, 12), 0, 0, 0);
        tx.addView(Ui.tv(a, NAMES[Math.max(0, Math.min(lang, NAMES.length - 1))] + " genel seviyen", 16, Color.WHITE, true));
        tx.addView(Ui.tv(a, "📅 " + Ui.fmt(t.optLong("ts")), 12, Ui.SUB, false));
        r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1f));
        top.addView(r);
        TextView sm = Ui.tv(a, t.optString("summary"), 14, Ui.TXT, false);
        sm.setPadding(0, Ui.dp(a, 10), 0, 0);
        top.addView(sm);
        Ui.add(pg, top);

        JSONObject sc = t.optJSONObject("scores");
        if (sc != null) {
            LinearLayout c = Ui.card(a);
            c.addView(Ui.tv(a, "Bölüm puanların", 15, Color.WHITE, true));
            String[] k = {"grammar", "reading", "listening", "writing", "speaking"};
            String[] n = {"Dilbilgisi / kelime", "Okuma", "Dinleme", "Yazma", "Konuşma"};
            for (int i = 0; i < 5; i++) {
                int v = sc.optInt(k[i]);
                TextView l = Ui.tv(a, n[i] + " — %" + v, 13, Ui.SUB, false);
                l.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 3));
                c.addView(l);
                c.addView(Ui.bar(a, v, v >= 70 ? Ui.OK : v >= 45 ? Ui.WARN : Ui.BAD));
            }
            Ui.add(pg, c);
        }
        JSONArray st = t.optJSONArray("strengths");
        if (st != null && st.length() > 0) {
            LinearLayout c = Ui.card(a);
            c.addView(Ui.tv(a, "💪 Güçlü yönlerin", 15, Ui.OK, true));
            for (int i = 0; i < st.length(); i++) c.addView(Ui.tv(a, "• " + st.optString(i), 14, Ui.TXT, false));
            Ui.add(pg, c);
        }
        JSONArray wk = t.optJSONArray("weak");
        if (wk != null && wk.length() > 0) {
            pg.addView(Ui.label(a, "NEREDE HATA YAPIYORSUN / NEYİ ÇALIŞMALISIN"));
            for (int i = 0; i < wk.length(); i++) {
                JSONObject w = wk.optJSONObject(i);
                if (w == null) continue;
                LinearLayout c = Ui.card(a);
                c.addView(Ui.tv(a, "⚠ " + w.optString("topic"), 15, Ui.WARN, true));
                TextView dt = Ui.tv(a, w.optString("detail"), 14, Ui.TXT, false);
                dt.setTextIsSelectable(true);
                c.addView(dt);
                Ui.add(pg, c);
            }
        }
        JSONArray rm = t.optJSONArray("roadmap");
        if (rm != null && rm.length() > 0) {
            pg.addView(Ui.label(a, "🗺 YOL HARİTAN"));
            for (int i = 0; i < rm.length(); i++) {
                JSONObject p = rm.optJSONObject(i);
                if (p == null) continue;
                LinearLayout c = Ui.card(a);
                c.addView(Ui.tv(a, p.optString("period") + " · " + p.optString("focus"), 15, Color.WHITE, true));
                JSONArray ts = p.optJSONArray("tasks");
                for (int k = 0; ts != null && k < ts.length(); k++) c.addView(Ui.tv(a, "☐ " + ts.optString(k), 14, Ui.TXT, false));
                Ui.add(pg, c);
            }
        }
        JSONArray sl = t.optJSONArray("speaking_log");
        if (sl != null && sl.length() > 0) {
            LinearLayout c = Ui.card(a);
            c.addView(Ui.tv(a, "🎤 Konuşma cevapların (kayıt)", 14, Ui.SUB, true));
            for (int i = 0; i < sl.length(); i++) {
                JSONObject so = sl.optJSONObject(i);
                if (so != null) c.addView(Ui.tv(a, "S: " + so.optString("q") + "\nC: " + so.optString("a") + "\n", 13, Ui.TXT, false));
            }
            if (!t.optString("writing").isEmpty()) c.addView(Ui.tv(a, "✍ Yazın:\n" + t.optString("writing"), 13, Ui.TXT, false));
            Ui.add(pg, c);
        }
        Ui.add(pg, Ui.btn(a, "📚 Zayıf konular için derslere git", 0xFF047857, v -> lessonList(a, lvIdx(t.optString("level")))));
    }

    static void roadmap(final MainActivity a) {
        JSONObject t = latestTest(a, sel(a));
        if (t == null) {
            LinearLayout pg = a.pageCol("Yol haritam", () -> show(a));
            LinearLayout c = Ui.card(a);
            c.addView(Ui.tv(a, "Henüz seviye sınavı yok. Yol haritan sınav sonucuna göre çıkarılır.", 15, Ui.TXT, false));
            Ui.add(pg, c);
            Ui.add(pg, Ui.btn(a, "📝 Sınava başla", Ui.ACC, v -> examIntro(a)));
            return;
        }
        report(a, t, () -> show(a));
    }

    // ------------------------------------------------------------ dilbilgisi dersleri (Duolingo tarzı)

    static void lessonList(final MainActivity a, final int lvI) {
        final int lang = sel(a);
        final int li = Math.max(0, Math.min(4, lvI));
        LinearLayout pg = a.pageCol(NAMES[lang] + " · Dilbilgisi", () -> show(a));
        pg.addView(Ui.tv(a, "Konu anlatımını oku, soruları çöz: ilk seferde bilirsen 10 puan, yanlışlar sonda tekrar sorulur (doğru yaparsan 5 puan) ve düzeltilir.", 12, Ui.SUB, false));
        LinearLayout cr = Ui.row(a);
        cr.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8));
        for (int i = 0; i < 5; i++) {
            final int k = i;
            TextView ch = Ui.chip(a, LV[i], i == li, v -> lessonList(a, k));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.rightMargin = Ui.dp(a, 6);
            cr.addView(ch, lp);
        }
        pg.addView(cr);
        List<JSONObject> done = list(a, LESSONS, lang);
        for (final String topic : TOPICS[li]) {
            int best = -1;
            int times = 0;
            for (JSONObject o : done) {
                if (LV[li].equals(o.optString("level")) && topic.equals(o.optString("topic"))) {
                    times++;
                    int p = o.optInt("total") == 0 ? 0 : 100 * o.optInt("correct") / o.optInt("total");
                    best = Math.max(best, p);
                }
            }
            LinearLayout c = Ui.card(a);
            LinearLayout r = Ui.row(a);
            r.addView(Ui.tv(a, best < 0 ? "○" : best >= 80 ? "✅" : "◐", 20, best >= 80 ? Ui.OK : Ui.SUB, true));
            LinearLayout tx = Ui.col(a);
            tx.setPadding(Ui.dp(a, 12), 0, 0, 0);
            tx.addView(Ui.tv(a, topic, 15, Color.WHITE, true));
            tx.addView(Ui.tv(a, best < 0 ? "Henüz çözülmedi" : "En iyi: %" + best + " · " + times + " kez", 12, Ui.SUB, false));
            r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1f));
            c.addView(r);
            c.setOnClickListener(v -> startLesson(a, lang, li, topic));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = Ui.dp(a, 8);
            pg.addView(c, lp);
        }
    }

    static void startLesson(final MainActivity a, final int lang, final int li, final String topic) {
        if (a.key().isEmpty()) {
            a.toast("Önce Ayarlar > Yapay zekâ bölümüne API anahtarını gir.");
            return;
        }
        String user = "Teach a grammar lesson for a Turkish-speaking learner of " + EN[lang] + " at CEFR level " + LV[li]
                + ". Topic (in Turkish): \"" + topic + "\". If this topic does not exist in " + EN[lang]
                + ", teach the closest equivalent concept in that language. "
                + "Write 'explain' in TURKISH: a clear, friendly explanation with the rule, a small table-like list of forms, 4-6 example sentences in " + EN[lang]
                + " each followed by its Turkish translation, and 2-3 common mistakes Turkish speakers make (max 250 words, plain text, no markdown symbols like ** or #). "
                + "Then write exactly 10 multiple-choice exercise questions in " + EN[lang] + " about this topic at " + LV[li]
                + " level (fill the blank ___, choose the correct form, or short translation choices), 4 options each, 'answer' = 0-based index, 'explain' = one Turkish sentence saying why. JSON: "
                + "{\"explain\":\"..\",\"questions\":[{\"q\":\"..\",\"options\":[\"..\",\"..\",\"..\",\"..\"],\"answer\":0,\"explain\":\"..\"}]}";
        a.aiJson("You are an excellent language teacher. Output ONLY valid JSON, no markdown fences.", user, 5000,
                "Ders hazırlanıyor…", (o, err) -> {
                    List<Mcq.Q> qs = o == null ? new ArrayList<>() : Mcq.parse(o.optJSONArray("questions"));
                    if (qs.size() < 4) {
                        if (err == null) a.toast("Ders hazırlanamadı, tekrar dene.");
                        return;
                    }
                    lessonExplain(a, lang, li, topic, o.optString("explain"), qs);
                });
    }

    static void lessonExplain(final MainActivity a, final int lang, final int li, final String topic, final String explain,
                              final List<Mcq.Q> qs) {
        LinearLayout pg = a.pageCol(topic, () -> lessonList(a, li));
        LinearLayout c = Ui.card(a);
        c.addView(Ui.tv(a, "📖 Konu anlatımı · " + LV[li], 13, Ui.SUB, true));
        TextView t = Ui.tv(a, explain, 15, Ui.TXT, false);
        t.setTextIsSelectable(true);
        t.setPadding(0, Ui.dp(a, 8), 0, 0);
        c.addView(t);
        Ui.add(pg, c);
        Ui.add(pg, Ui.btn(a, "▶ Örnekleri sesli dinle", Ui.CARD2, v -> a.speak(explain.replaceAll("\\(.*?\\)", ""), TAGS[lang])));
        Ui.add(pg, Ui.btn(a, "Alıştırmaya başla ✏", Ui.ACC, v -> lessonRun(a, lang, li, topic, explain, qs)));
    }

    static void lessonRun(final MainActivity a, final int lang, final int li, final String topic, final String explain,
                          final List<Mcq.Q> qs) {
        a.stopSpeak();
        for (Mcq.Q q : qs) {
            q.ok = false;
            q.answered = false;
        }
        LinearLayout pg = a.pageCol(topic + " · alıştırma", () -> lessonExplain(a, lang, li, topic, explain, qs));
        LinearLayout host = Ui.col(a);
        pg.addView(host);
        Mcq.run(a, host, qs, true, true, (all, correct, xp) -> {
            try {
                JSONObject o = new JSONObject();
                o.put("id", UUID.randomUUID().toString());
                o.put("lang", lang);
                o.put("level", LV[li]);
                o.put("topic", topic);
                o.put("ts", System.currentTimeMillis());
                o.put("correct", correct);
                o.put("total", all.size());
                o.put("xp", xp);
                append(a, LESSONS, o);
            } catch (Exception e) {
                a.toast("Sonuç kaydedilemedi.");
            }
            LinearLayout rp = a.pageCol("Ders bitti 🎉", () -> lessonList(a, li));
            LinearLayout c = Ui.card(a);
            c.addView(Ui.tv(a, "⭐ +" + xp + " puan", 26, Ui.WARN, true));
            c.addView(Ui.tv(a, "İlk denemede doğru: " + correct + " / " + all.size(), 16, Color.WHITE, true));
            Ui.add(rp, c);
            boolean any = false;
            for (Mcq.Q q : all) {
                if (q.ok) continue;
                if (!any) {
                    rp.addView(Ui.label(a, "ÇALIŞILMASI GEREKENLER (düzeltmeleriyle)"));
                    any = true;
                }
                LinearLayout w = Ui.card(a);
                w.addView(Ui.tv(a, q.q, 14, Color.WHITE, true));
                w.addView(Ui.tv(a, "✔ Doğru: " + q.opts[q.ans], 14, Ui.OK, false));
                if (!q.explain.isEmpty()) w.addView(Ui.tv(a, q.explain, 13, Ui.SUB, false));
                Ui.add(rp, w);
            }
            if (!any) rp.addView(Ui.tv(a, "Hepsini ilk seferde bildin, harika!", 14, Ui.OK, false));
            Ui.add(rp, Ui.btn(a, "Tekrar çöz", Ui.ACC, v -> lessonRun(a, lang, li, topic, explain, qs)));
            Ui.add(rp, Ui.btn(a, "Konulara dön", Ui.CARD2, v -> lessonList(a, li)));
        });
    }

    // ------------------------------------------------------------ hedef dilde sohbet

    static final String[] SCEN = {"Günlük sohbet", "Tanışma ve kendini anlatma", "Kafede / restoranda sipariş", "Alışveriş",
            "Seyahat ve otel", "İş / staj görüşmesi", "Doktorda", "Serbest konu"};
    static final String[] SCEN_EN = {"casual everyday chat", "meeting someone new and talking about yourself", "ordering at a cafe or restaurant",
            "shopping", "travel and hotel", "a job or internship interview", "at the doctor", "free conversation on any topic the learner likes"};

    static void scenarios(final MainActivity a) {
        final int lang = sel(a);
        LinearLayout pg = a.pageCol(NAMES[lang] + " sohbet", () -> show(a));
        String lv = level(a, lang);
        pg.addView(Ui.tv(a, "Yapay zekâ seninle SADECE " + NAMES[lang] + " konuşur ve " + lv
                + " seviyene uygun basit tutar. " + (latestTest(a, lang) == null
                ? "Seviyeni belirlemek için önce sınava girmeni öneririm."
                : "Seviyen sınavda belirlendi."), 13, Ui.SUB, false));
        pg.addView(Ui.label(a, "SENARYO SEÇ"));
        for (int i = 0; i < SCEN.length; i++) {
            final int k = i;
            Ui.add(pg, Ui.btn(a, SCEN[i], Ui.CARD2, v -> {
                if (a.key().isEmpty()) {
                    a.toast("Önce Ayarlar > Yapay zekâ bölümüne API anahtarını gir.");
                    return;
                }
                try {
                    JSONObject m = new JSONObject();
                    m.put("lang", lang);
                    m.put("level", level(a, lang));
                    m.put("scen", k);
                    JSONObject s = Sessions.create(a, CHATS, m);
                    openChat(a, s.optString("id"));
                } catch (Exception e) {
                    a.toast("Sohbet açılamadı.");
                }
            }));
        }
    }

    static void openChat(final MainActivity a, String sid) {
        JSONObject s = Sessions.find(a, CHATS, sid);
        if (s == null) {
            show(a);
            return;
        }
        final int lang = s.optInt("lang");
        final String lv = s.optString("level", "A1");
        final int sc = Math.max(0, Math.min(SCEN.length - 1, s.optInt("scen")));
        LinearLayout page = a.fixedPage(NAMES[lang] + " · " + SCEN[sc] + " · " + lv, () -> show(a));
        page.addView(Ui.toggle(a, "🔊 Cevapları otomatik sesli oku", "1".equals(Store.get(a, "autospeak", "0")),
                b -> Store.put(a, "autospeak", b ? "1" : "0")));
        final String sys = "You are a friendly " + EN[lang] + " conversation partner for a Turkish-speaking learner at CEFR level " + lv + ". "
                + "Speak ONLY in " + EN[lang] + ", never in Turkish or any other language - even if the learner writes Turkish, answer in simple " + EN[lang]
                + " and gently encourage them to continue in " + EN[lang] + ". Use only vocabulary and grammar suitable for " + lv + ". "
                + "Scenario: " + SCEN_EN[sc] + ". Keep every reply to 1-3 short sentences and end with one simple question. "
                + "If the learner makes a mistake, continue naturally and add a new last line starting with '✏️' that gives the corrected version in very simple "
                + EN[lang] + ". The learner's text may come from speech recognition, so ignore tiny recognition glitches.";
        ChatUi.build(a, page, CHATS, sid, () -> sys,
                "Start the conversation now: greet me briefly and begin the scenario with your first question.", TAGS[lang], TAGS[lang]);
    }

    // ------------------------------------------------------------ geçmiş

    static void history(final MainActivity a) {
        final int lang = sel(a);
        LinearLayout pg = a.pageCol("Geçmiş · " + NAMES[lang], () -> show(a));
        pg.addView(Ui.tv(a, "Kayıtlar silinemez ve düzeltilemez.", 12, Ui.SUB, false));

        pg.addView(Ui.label(a, "SEVİYE SINAVLARI"));
        List<JSONObject> tests = list(a, TESTS, lang);
        if (tests.isEmpty()) pg.addView(Ui.tv(a, "Henüz yok.", 13, Ui.SUB, false));
        for (int i = tests.size() - 1; i >= 0; i--) {
            final JSONObject t = tests.get(i);
            LinearLayout c = Ui.card(a);
            c.addView(Ui.tv(a, t.optString("level") + " · " + Ui.fmt(t.optLong("ts")), 15, Color.WHITE, true));
            c.addView(Ui.tv(a, "Dilbilgisi: " + t.optString("gr"), 12, Ui.SUB, false));
            c.setOnClickListener(v -> report(a, t, () -> history(a)));
            Ui.add(pg, c);
        }

        pg.addView(Ui.label(a, "DERSLER"));
        List<JSONObject> ls = list(a, LESSONS, lang);
        if (ls.isEmpty()) pg.addView(Ui.tv(a, "Henüz yok.", 13, Ui.SUB, false));
        for (int i = ls.size() - 1; i >= 0; i--) {
            JSONObject o = ls.get(i);
            LinearLayout c = Ui.card(a);
            c.addView(Ui.tv(a, o.optString("level") + " · " + o.optString("topic"), 14, Color.WHITE, true));
            c.addView(Ui.tv(a, Ui.fmt(o.optLong("ts")) + " · " + o.optInt("correct") + "/" + o.optInt("total") + " ilk denemede · ⭐ " + o.optInt("xp"), 12, Ui.SUB, false));
            Ui.add(pg, c);
        }

        pg.addView(Ui.label(a, "SOHBETLER"));
        List<JSONObject> cs = list(a, CHATS, lang);
        if (cs.isEmpty()) pg.addView(Ui.tv(a, "Henüz yok.", 13, Ui.SUB, false));
        for (int i = cs.size() - 1; i >= 0; i--) {
            final JSONObject s = cs.get(i);
            LinearLayout c = Ui.card(a);
            int sc = Math.max(0, Math.min(SCEN.length - 1, s.optInt("scen")));
            c.addView(Ui.tv(a, SCEN[sc] + " · " + s.optString("level") + " · " + Ui.fmt(s.optLong("created")), 14, Color.WHITE, true));
            c.addView(Ui.tv(a, Sessions.preview(s), 12, Ui.SUB, false));
            c.setOnClickListener(v -> openChat(a, s.optString("id")));
            Ui.add(pg, c);
        }
    }
}
