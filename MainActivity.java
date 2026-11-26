package com.koc.hatirlatici;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.IntConsumer;

public class MainActivity extends Activity {

    static final int BG = 0xFF0F172A, CARD = 0xFF1E293B, LINE = 0xFF334155,
            TXT = 0xFFF1F5F9, MUT = 0xFF94A3B8, ACC = 0xFF818CF8;

    // {Türkçe gösterim, konuşma/ses kodu, İngilizce ad}
    static final String[][] LANGS = {
            {"English (İngilizce)", "en-US", "English"},
            {"Deutsch (Almanca)", "de-DE", "German"},
            {"Español (İspanyolca)", "es-ES", "Spanish"},
            {"Français (Fransızca)", "fr-FR", "French"},
            {"Italiano (İtalyanca)", "it-IT", "Italian"},
            {"Português (Portekizce)", "pt-BR", "Portuguese"},
            {"Русский (Rusça)", "ru-RU", "Russian"},
            {"日本語 (Japonca)", "ja-JP", "Japanese"},
            {"한국어 (Korece)", "ko-KR", "Korean"},
            {"العربية (Arapça)", "ar-SA", "Arabic"},
            {"中文 (Çince)", "zh-CN", "Chinese"}
    };
    static final String[] LVLS = {"A1 (yeni başlayan)", "A2 (temel)", "B1 (orta)", "B2 (orta üstü)", "C1 (ileri)"};
    static final String[] TITLES = {"Görevler", "Kariyer koçu", "Yabancı dil", "Ayarlar"};

    final SimpleDateFormat DF = new SimpleDateFormat("d MMM yyyy HH:mm", new Locale("tr", "TR"));

    FrameLayout frame, body;
    ScrollView curScroll;
    Button[] navBtns = new Button[4];
    ImageView overlay;

    ArrayList<String[]> coachLog = new ArrayList<>();
    ArrayList<String[]> langLog = new ArrayList<>();
    LinearLayout coachChat, langChat, taskList;
    EditText langInput, coachInput;

    int langIdx = 0, lvlIdx = 1;
    int convLang = 0, convLvl = 1;
    boolean fix = true, speakOn = true, convFix = true;

    TextToSpeech tts;
    boolean ttsReady = false;
    SpeechRecognizer sr;

    // ---------------------------------------------------------------- yaşam döngüsü

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(0xFF0B1220);

        langIdx = parse(Store.get(this, "langIdx", "0"), 0);
        lvlIdx = parse(Store.get(this, "lvlIdx", "1"), 1);
        convLang = langIdx;
        convLvl = lvlIdx;
        loadCoachLog();

        tts = new TextToSpeech(this, status -> {
            ttsReady = (status == TextToSpeech.SUCCESS);
        });

        frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        body = new FrameLayout(this);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(0xFF0B1220);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            Button nb = new Button(this);
            nb.setText(TITLES[i]);
            nb.setAllCaps(false);
            nb.setBackgroundColor(Color.TRANSPARENT);
            nb.setOnClickListener(view -> switchTab(idx));
            navBtns[i] = nb;
            nav.addView(nb, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        frame.addView(root, new FrameLayout.LayoutParams(-1, -1));
        setContentView(frame);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        switchTab(0);
        Scheduler.scheduleAll(this);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handleIntent(i);
    }

    void handleIntent(Intent i) {
        if (i != null && i.hasExtra("text")) {
            String cat = i.getStringExtra("cat");
            String text = i.getStringExtra("text");
            long seed = i.getLongExtra("seed", System.nanoTime());
            i.removeExtra("text");
            showQuote(cat == null || cat.isEmpty() ? 'd' : cat.charAt(0), text, seed);
        }
    }

    @Override
    public void onBackPressed() {
        if (overlay != null) {
            frame.removeView(overlay);
            overlay = null;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (sr != null) sr.destroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == 2 && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) listen();
    }

    void showQuote(char cat, String text, long seed) {
        if (overlay != null) frame.removeView(overlay);
        int w = getResources().getDisplayMetrics().widthPixels / 2;
        int h = getResources().getDisplayMetrics().heightPixels / 2;
        Bitmap bm = Wallpaper.make(cat, w, h, text, seed);
        overlay = new ImageView(this);
        overlay.setScaleType(ImageView.ScaleType.CENTER_CROP);
        overlay.setImageBitmap(bm);
        overlay.setOnClickListener(view -> {
            frame.removeView(overlay);
            overlay = null;
        });
        frame.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
    }

    // ---------------------------------------------------------------- yardımcılar

    int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    int parse(String s, int d) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return d;
        }
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    GradientDrawable round(int fill, int radius, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    TextView tv(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    EditText et(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(0xFF64748B);
        e.setTextColor(TXT);
        e.setBackground(round(0xFF0B1220, 12, LINE));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        return e;
    }

    Button btn(String s, boolean primary, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(primary ? 0xFF0B1020 : TXT);
        b.setBackground(round(primary ? ACC : 0x00000000, 12, primary ? 0 : LINE));
        if (l != null) b.setOnClickListener(l);
        return b;
    }

    LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout card() {
        LinearLayout l = col();
        l.setBackground(round(CARD, 14, LINE));
        l.setPadding(dp(14), dp(14), dp(14), dp(14));
        return l;
    }

    void add(LinearLayout parent, View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        parent.addView(v, lp);
    }

    Spinner spin(String[] items, int sel) {
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
        s.setSelection(sel);
        return s;
    }

    void onSelect(Spinner s, IntConsumer c) {
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                c.accept(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {
            }
        });
    }

    TextView addBubble(LinearLayout parent, String text, boolean user) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(TXT);
        t.setTextSize(15);
        t.setTextIsSelectable(true);
        t.setPadding(dp(12), dp(8), dp(12), dp(8));
        t.setBackground(round(user ? 0xFF312E81 : 0xFF0B1220, 12, user ? 0 : LINE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = dp(8);
        lp.gravity = user ? Gravity.END : Gravity.START;
        if (user) lp.leftMargin = dp(40);
        else lp.rightMargin = dp(40);
        parent.addView(t, lp);
        return t;
    }

    void scrollEnd() {
        if (curScroll != null) curScroll.post(() -> curScroll.fullScroll(View.FOCUS_DOWN));
    }

    void switchTab(int t) {
        body.removeAllViews();
        LinearLayout c = col();
        c.setPadding(dp(16), dp(12), dp(16), dp(24));
        c.addView(tv(TITLES[t], 22, true, TXT));
        ScrollView sv = new ScrollView(this);
        sv.addView(c);
        body.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        curScroll = sv;
        if (t == 0) buildTasks(c);
        else if (t == 1) buildCoach(c);
        else if (t == 2) buildLang(c);
        else buildSettings(c);
        for (int i = 0; i < 4; i++) navBtns[i].setTextColor(i == t ? ACC : MUT);
    }

    String fmtEvery(long ms) {
        if (ms % 86400000L == 0) return (ms / 86400000L) + " gün";
        if (ms % 3600000L == 0) return (ms / 3600000L) + " saat";
        return (ms / 60000L) + " dk";
    }

    // ---------------------------------------------------------------- GÖREVLER

    void buildTasks(LinearLayout c) {
        LinearLayout form = card();
        add(c, form);
        form.addView(tv("Yeni görev", 18, true, TXT));

        final EditText title = et("Görev (örn: Her gün 1 saat kod yaz)");
        add(form, title);

        final Calendar dl = Calendar.getInstance();
        dl.add(Calendar.DAY_OF_YEAR, 7);
        dl.set(Calendar.SECOND, 0);
        final Button dBtn = btn("Bitiş: " + DF.format(dl.getTime()), false, null);
        dBtn.setOnClickListener(view -> {
            new DatePickerDialog(this, (dpk, yy, mm, dd) -> {
                dl.set(yy, mm, dd);
                new TimePickerDialog(this, (tpk, hh, mi) -> {
                    dl.set(Calendar.HOUR_OF_DAY, hh);
                    dl.set(Calendar.MINUTE, mi);
                    dl.set(Calendar.SECOND, 0);
                    dBtn.setText("Bitiş: " + DF.format(dl.getTime()));
                }, dl.get(Calendar.HOUR_OF_DAY), dl.get(Calendar.MINUTE), true).show();
            }, dl.get(Calendar.YEAR), dl.get(Calendar.MONTH), dl.get(Calendar.DAY_OF_MONTH)).show();
        });
        add(form, dBtn);

        form.addView(tv("Her ... uyar", 13, false, MUT));
        LinearLayout row = new LinearLayout(this);
        final EditText every = et("3");
        every.setInputType(InputType.TYPE_CLASS_NUMBER);
        every.setText("3");
        final Spinner unit = spin(new String[]{"dakika", "saat", "gün"}, 1);
        row.addView(every, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(unit, new LinearLayout.LayoutParams(0, -2, 1f));
        add(form, row);

        form.addView(tv("Uyarı sesi ve titreşim", 13, false, MUT));
        final Spinner snd = spin(Sounds.NAMES, 0);
        final Spinner vib = spin(Sounds.VIB_NAMES, 2);
        LinearLayout row2 = new LinearLayout(this);
        row2.addView(snd, new LinearLayout.LayoutParams(0, -2, 1f));
        row2.addView(vib, new LinearLayout.LayoutParams(0, -2, 1f));
        add(form, row2);

        LinearLayout row3 = new LinearLayout(this);
        Button test = btn("Dene", false, view ->
                Sounds.preview(this, snd.getSelectedItemPosition(), vib.getSelectedItemPosition()));
        Button addB = btn("Görevi ekle", true, view -> {
            String ttl = title.getText().toString().trim();
            if (ttl.isEmpty()) {
                toast("Görev adını yaz.");
                return;
            }
            long deadline = dl.getTimeInMillis();
            long now = System.currentTimeMillis();
            if (deadline <= now) {
                toast("Bitiş tarihi gelecekte olmalı.");
                return;
            }
            int n = parse(every.getText().toString(), 0);
            if (n < 1) {
                toast("Uyarı aralığı en az 1 olmalı.");
                return;
            }
            long[] mult = {60000L, 3600000L, 86400000L};
            Task t = new Task();
            t.id = UUID.randomUUID().toString();
            t.title = ttl;
            t.deadline = deadline;
            t.everyMs = n * mult[unit.getSelectedItemPosition()];
            t.created = now;
            t.next = now + t.everyMs;
            t.sound = snd.getSelectedItemPosition();
            t.vib = vib.getSelectedItemPosition();
            t.active = true;
            List<Task> ts = Store.tasks(this);
            ts.add(t);
            Store.saveTasks(this, ts);
            Scheduler.schedule(this, t);
            title.setText("");
            fillTasks();
            toast("Görev eklendi. Uygulama kapalıyken de uyarır.");
        });
        row3.addView(test, new LinearLayout.LayoutParams(0, -2, 1f));
        row3.addView(addB, new LinearLayout.LayoutParams(0, -2, 2f));
        add(form, row3);

        taskList = col();
        add(c, taskList);
        fillTasks();
    }

    void fillTasks() {
        if (taskList == null) return;
        taskList.removeAllViews();
        List<Task> ts = Store.tasks(this);
        if (ts.isEmpty()) {
            add(taskList, tv("Henüz görev yok.", 14, false, MUT));
            return;
        }
        long now = System.currentTimeMillis();
        for (Task t : ts) {
            final String tid = t.id;
            LinearLayout cd = card();
            cd.addView(tv(t.title, 17, true, TXT));
            cd.addView(tv("Her " + fmtEvery(t.everyMs) + " • " + Sounds.NAMES[t.sound]
                    + " • titreşim: " + Sounds.VIB_NAMES[t.vib], 13, false, MUT));
            cd.addView(tv("Bitiş: " + DF.format(new java.util.Date(t.deadline)), 13, false, MUT));
            String st;
            if (t.active) {
                st = "Sonraki uyarı: " + DF.format(new java.util.Date(Math.min(t.next, t.deadline)));
            } else if (t.deadline <= now) {
                st = "Süre doldu";
            } else {
                st = "Duraklatıldı";
            }
            cd.addView(tv(st, 13, false, ACC));
            LinearLayout r = new LinearLayout(this);
            r.addView(btn(t.active ? "Duraklat" : "Başlat", false, view -> toggleTask(tid)),
                    new LinearLayout.LayoutParams(0, -2, 1f));
            r.addView(btn("Sil", false, view -> deleteTask(tid)),
                    new LinearLayout.LayoutParams(0, -2, 1f));
            add(cd, r);
            add(taskList, cd);
        }
    }

    void toggleTask(String id) {
        List<Task> ts = Store.tasks(this);
        long now = System.currentTimeMillis();
        for (Task x : ts) {
            if (!x.id.equals(id)) continue;
            if (x.active) {
                x.active = false;
                Scheduler.cancel(this, x);
            } else if (x.deadline <= now) {
                toast("Bitiş tarihi geçmiş. Yeni görev ekle.");
            } else {
                x.active = true;
                x.next = now + x.everyMs;
                Scheduler.schedule(this, x);
            }
        }
        Store.saveTasks(this, ts);
        fillTasks();
    }

    void deleteTask(String id) {
        List<Task> ts = Store.tasks(this);
        List<Task> keep = new ArrayList<>();
        for (Task x : ts) {
            if (x.id.equals(id)) Scheduler.cancel(this, x);
            else keep.add(x);
        }
        Store.saveTasks(this, keep);
        fillTasks();
    }

    // ---------------------------------------------------------------- SOHBET ORTAK

    void ask(final String system, final ArrayList<String[]> log, final LinearLayout view,
             final boolean doSpeak, final boolean isCoach) {
        final String key = Store.get(this, "apikey", "").trim();
        if (key.isEmpty()) {
            toast("Önce Ayarlar sekmesinden API anahtarını gir.");
            return;
        }
        final JSONArray arr = new JSONArray();
        try {
            for (String[] m : log) {
                JSONObject o = new JSONObject();
                o.put("role", m[0]);
                o.put("content", m[1]);
                arr.put(o);
            }
        } catch (Exception e) {
            toast("Mesaj hazırlanamadı.");
            return;
        }
        final TextView wait = addBubble(view, "…", false);
        scrollEnd();
        new Thread(() -> {
            String r;
            try {
                r = Claude.chat(key, system, arr);
            } catch (Exception e) {
                r = "⚠ " + e.getMessage();
            }
            final String out = r;
            runOnUiThread(() -> {
                view.removeView(wait);
                if (!out.startsWith("⚠")) {
                    log.add(new String[]{"assistant", out, "0"});
                    if (isCoach) saveCoachLog();
                }
                addBubble(view, out, false);
                if (doSpeak && speakOn && !out.startsWith("⚠")) speak(out, convLang);
                scrollEnd();
            });
        }).start();
    }

    void loadCoachLog() {
        coachLog.clear();
        try {
            JSONArray a = new JSONArray(Store.get(this, "coach_log", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONArray m = a.getJSONArray(i);
                coachLog.add(new String[]{m.getString(0), m.getString(1), "0"});
            }
        } catch (Exception e) {
            coachLog.clear();
        }
    }

    void saveCoachLog() {
        while (coachLog.size() > 40) coachLog.remove(0);
        JSONArray a = new JSONArray();
        for (String[] m : coachLog) {
            JSONArray x = new JSONArray();
            x.put(m[0]);
            x.put(m[1]);
            a.put(x);
        }
        Store.put(this, "coach_log", a.toString());
    }

    // ---------------------------------------------------------------- KARİYER KOÇU

    String coachSystem() {
        return "Sen Türkçe konuşan, sıcak ama dürüst bir kariyer koçusun. Kullanıcının kariyer hedefi: "
                + nz(Store.get(this, "goal", "")) + ". Mevcut durumu: " + nz(Store.get(this, "nowst", ""))
                + ". Son haftalık ilerleme puanları (1-5): " + nz(Store.get(this, "scores", ""))
                + ". Kısa, somut ve uygulanabilir tavsiyeler ver; gerekirse tek bir soru sor; "
                + "abartılı övgüden kaçın; markdown kullanma.";
    }

    String nz(String s) {
        return s == null || s.trim().isEmpty() ? "belirtilmedi" : s.trim();
    }

    String bars() {
        String s = Store.get(this, "scores", "");
        if (s.isEmpty()) return "";
        String[] p = s.split(",");
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, p.length - 6);
        for (int i = from; i < p.length; i++) {
            int n = Math.max(0, Math.min(5, parse(p[i], 0)));
            for (int k = 0; k < 5; k++) sb.append(k < n ? "█" : "░");
            sb.append("  ").append(n).append("/5\n");
        }
        return sb.toString().trim();
    }

    void sendCoach(String text) {
        if (text.trim().isEmpty()) return;
        coachLog.add(new String[]{"user", text, "0"});
        saveCoachLog();
        addBubble(coachChat, text, true);
        ask(coachSystem(), coachLog, coachChat, false, true);
    }

    void buildCoach(LinearLayout c) {
        LinearLayout g = card();
        add(c, g);
        g.addView(tv("Hedefim", 18, true, TXT));
        final EditText goal = et("Kariyer hedefim (örn: 2027 yazında yazılım stajı)");
        goal.setText(Store.get(this, "goal", ""));
        final EditText now = et("Şu anki durumum");
        now.setText(Store.get(this, "nowst", ""));
        add(g, goal);
        add(g, now);
        add(g, btn("Kaydet", false, view -> {
            Store.put(this, "goal", goal.getText().toString().trim());
            Store.put(this, "nowst", now.getText().toString().trim());
            toast("Kaydedildi.");
        }));

        LinearLayout w = card();
        add(c, w);
        w.addView(tv("Haftalık değerlendirme", 18, true, TXT));
        w.addView(tv("Bu hafta hedefe ne kadar ilerledim? (1-5)", 13, false, MUT));
        final Spinner score = spin(new String[]{"1", "2", "3", "4", "5"}, 2);
        add(w, score);
        final EditText did = et("Ne yaptım? Beni ne engelledi?");
        add(w, did);
        final TextView prog = tv(bars(), 15, false, ACC);
        add(w, btn("Koça gönder", true, view -> {
            String sc = String.valueOf(score.getSelectedItemPosition() + 1);
            String old = Store.get(this, "scores", "");
            Store.put(this, "scores", old.isEmpty() ? sc : old + "," + sc);
            prog.setText(bars());
            String msg = "Haftalık durum: " + sc + "/5. " + did.getText().toString().trim();
            did.setText("");
            sendCoach(msg);
        }));
        add(w, prog);

        LinearLayout ch = card();
        add(c, ch);
        ch.addView(tv("Koçla sohbet", 18, true, TXT));
        coachChat = col();
        add(ch, coachChat);
        for (String[] m : coachLog) addBubble(coachChat, m[1], m[0].equals("user"));
        coachInput = et("Bir şey sor…");
        add(ch, coachInput);
        add(ch, btn("Gönder", true, view -> {
            String s = coachInput.getText().toString();
            coachInput.setText("");
            sendCoach(s);
        }));
        add(ch, btn("Sohbeti temizle", false, view -> {
            coachLog.clear();
            saveCoachLog();
            coachChat.removeAllViews();
        }));
    }

    // ---------------------------------------------------------------- YABANCI DİL

    String langSystem() {
        String name = LANGS[convLang][2];
        String lv = LVLS[convLvl].substring(0, 2);
        return "You are a friendly, patient " + name + " conversation partner for a Turkish-speaking learner at CEFR level "
                + lv + ". Reply ONLY in " + name + ", using vocabulary and grammar suitable for " + lv
                + ", in 1-3 short sentences, and always end with one simple follow-up question to keep the conversation going. "
                + (convFix
                ? "If the learner's last message contains mistakes, add a final separate line starting with \"✏️\" that gives the corrected sentence and a one-sentence explanation in Turkish. If the message was correct, do not add that line."
                : "Do not correct the learner's mistakes unless they ask.")
                + " Never use Turkish except inside the correction line. Do not use markdown.";
    }

    void langStart(boolean greet) {
        langLog.clear();
        if (langChat != null) langChat.removeAllViews();
        convLang = langIdx;
        convLvl = lvlIdx;
        convFix = fix;
        if (greet) {
            langLog.add(new String[]{"user", "Start the conversation: greet me and ask one easy question.", "1"});
            ask(langSystem(), langLog, langChat, true, false);
        }
    }

    void sendLang(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (langLog.isEmpty()) langStart(false);
        langLog.add(new String[]{"user", text.trim(), "0"});
        if (langChat != null) addBubble(langChat, text.trim(), true);
        ask(langSystem(), langLog, langChat, true, false);
    }

    void speak(String text, int idx) {
        if (!ttsReady || tts == null) return;
        String clean = text;
        int cut = clean.indexOf('✏');
        if (cut >= 0) clean = clean.substring(0, cut);
        clean = clean.replace("*", "").trim();
        if (clean.isEmpty()) return;
        int r = tts.setLanguage(Locale.forLanguageTag(LANGS[idx][1]));
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            toast("Telefonda bu dilin ses paketi yok (Ayarlar > Metin okuma çıkışı).");
            return;
        }
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "koc");
    }

    void listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("Bu telefonda ses tanıma servisi yok. Klavyedeki mikrofonu kullanabilirsin.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 2);
            return;
        }
        if (sr == null) {
            sr = SpeechRecognizer.createSpeechRecognizer(this);
            sr.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle p) {
                    toast("Dinliyorum…");
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float r) {
                }

                @Override
                public void onBufferReceived(byte[] b) {
                }

                @Override
                public void onEndOfSpeech() {
                }

                @Override
                public void onError(int e) {
                    toast("Ses anlaşılamadı, tekrar dene.");
                }

                @Override
                public void onResults(Bundle b) {
                    ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (r != null && !r.isEmpty()) sendLang(r.get(0));
                }

                @Override
                public void onPartialResults(Bundle b) {
                }

                @Override
                public void onEvent(int t, Bundle b) {
                }
            });
        }
        int use = langLog.isEmpty() ? langIdx : convLang;
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGS[use][1]);
        sr.startListening(i);
    }

    void buildLang(LinearLayout c) {
        LinearLayout g = card();
        add(c, g);
        g.addView(tv("Konuşma partneri", 18, true, TXT));
        g.addView(tv("Dil seç; seçtiğin dilde seninle sohbet eder, hatalarını Türkçe düzeltir.", 13, false, MUT));
        String[] names = new String[LANGS.length];
        for (int i = 0; i < LANGS.length; i++) names[i] = LANGS[i][0];
        Spinner ls = spin(names, langIdx);
        onSelect(ls, pos -> {
            langIdx = pos;
            Store.put(this, "langIdx", String.valueOf(pos));
        });
        add(g, ls);
        Spinner vs = spin(LVLS, lvlIdx);
        onSelect(vs, pos -> {
            lvlIdx = pos;
            Store.put(this, "lvlIdx", String.valueOf(pos));
        });
        add(g, vs);

        CheckBox cf = new CheckBox(this);
        cf.setText("Hatalarımı düzelt (Türkçe açıklama)");
        cf.setTextColor(TXT);
        cf.setChecked(fix);
        cf.setOnCheckedChangeListener((bv, on) -> fix = on);
        add(g, cf);
        CheckBox cs = new CheckBox(this);
        cs.setText("Cevapları sesli oku");
        cs.setTextColor(TXT);
        cs.setChecked(speakOn);
        cs.setOnCheckedChangeListener((bv, on) -> speakOn = on);
        add(g, cs);

        add(g, btn("Yeni sohbet başlat", true, view -> langStart(true)));

        LinearLayout ch = card();
        add(c, ch);
        langChat = col();
        add(ch, langChat);
        for (String[] m : langLog) {
            if (!m[2].equals("1")) addBubble(langChat, m[1], m[0].equals("user"));
        }
        langInput = et("Yaz…");
        add(ch, langInput);
        LinearLayout r = new LinearLayout(this);
        r.addView(btn("🎤 Konuş", false, view -> listen()), new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(btn("Gönder", true, view -> {
            String s = langInput.getText().toString();
            langInput.setText("");
            sendLang(s);
        }), new LinearLayout.LayoutParams(0, -2, 1f));
        add(ch, r);
    }

    // ---------------------------------------------------------------- AYARLAR

    void buildSettings(LinearLayout c) {
        LinearLayout a = card();
        add(c, a);
        a.addView(tv("Alarmın güvenle çalışması için", 18, true, TXT));
        a.addView(tv("1) Bildirim izni açık olmalı.\n2) Pil tasarrufu uygulamayı kısıtlamamalı: telefon ayarlarında bu uygulama için "
                + "pil kullanımını \"Kısıtlama yok / Sınırsız\" yap (Xiaomi, Oppo, Samsung, Huawei'de ayrıca "
                + "\"otomatik başlatma\"yı aç).", 13, false, MUT));
        add(a, btn("Bildirim ayarlarını aç", false, view -> {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(i);
        }));
        add(a, btn("Pil optimizasyonu ayarlarını aç", false, view ->
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))));
        if (Build.VERSION.SDK_INT >= 31) {
            add(a, btn("Tam zamanlı alarm iznini aç", false, view -> {
                Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                i.setData(Uri.parse("package:" + getPackageName()));
                try {
                    startActivity(i);
                } catch (Exception e) {
                    toast("Bu telefonda bu izin ayrıca gerekmiyor.");
                }
            }));
        }
        add(a, btn("Örnek uyarı gönder", true, view -> {
            Task t = new Task();
            t.id = "test";
            t.title = "Örnek görev";
            t.sound = 0;
            t.vib = 2;
            Notifier.show(this, t, false);
        }));

        LinearLayout l = card();
        add(c, l);
        l.addView(tv("Kilit ekranı kartı", 18, true, TXT));
        l.addView(tv("Kilit ekranında sürekli duran bir söz + duvar kağıdı bildirimi. Her uyarıda yenilenir.", 13, false, MUT));
        CheckBox lc = new CheckBox(this);
        lc.setText("Kilit ekranında söz kartı göster");
        lc.setTextColor(TXT);
        lc.setChecked("1".equals(Store.get(this, "lockcard", "0")));
        lc.setOnCheckedChangeListener((bv, on) -> {
            Store.put(this, "lockcard", on ? "1" : "0");
            if (on) Notifier.lockCard(this, Quotes.next(this), System.nanoTime());
            else Notifier.removeLockCard(this);
        });
        add(l, lc);

        LinearLayout s = card();
        add(c, s);
        s.addView(tv("Sesler", 18, true, TXT));
        for (int i = 0; i < Sounds.NAMES.length; i++) {
            final int k = i;
            add(s, btn("▶ " + Sounds.NAMES[i], false, view -> Sounds.preview(this, k, 0)));
        }

        LinearLayout k = card();
        add(c, k);
        k.addView(tv("Yapay zekâ anahtarı", 18, true, TXT));
        k.addView(tv("Koç ve dil sohbeti için kendi Anthropic API anahtarın gerekir (console.anthropic.com). "
                + "Kullanım ücrete tabidir (" + Claude.MODEL + " modeli, ucuz). Anahtar yalnızca bu telefonda saklanır.", 13, false, MUT));
        final EditText key = et("sk-ant-...");
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setText(Store.get(this, "apikey", ""));
        add(k, key);
        add(k, btn("Kaydet", true, view -> {
            Store.put(this, "apikey", key.getText().toString().trim());
            toast("Kaydedildi.");
        }));
    }
}
