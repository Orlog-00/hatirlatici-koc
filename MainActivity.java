package com.koc.hatirlatici;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.KeyguardManager;
import android.app.TimePickerDialog;
import android.content.Context;
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
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.function.IntConsumer;

public class MainActivity extends Activity {

    static final int BG = 0xFF0F172A, CARD = 0xFF1E293B, LINE = 0xFF334155,
            TXT = 0xFFF1F5F9, MUT = 0xFF94A3B8, ACC = 0xFF818CF8, FIELD = 0xFF0B1220;

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
    static final String[] NAV = {"Görevler", "Koç", "Dil", "Ayarlar"};
    static final String[] DAYS = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};

    final SimpleDateFormat DF = new SimpleDateFormat("d MMM yyyy HH:mm", new Locale("tr", "TR"));

    FrameLayout frame, body, overlay;
    VideoView overlayVideo;
    ScrollView curScroll;
    Button[] navBtns = new Button[4];
    int tab = 0;
    boolean fromAlert = false;

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
            nb.setText(NAV[i]);
            nb.setAllCaps(false);
            nb.setTextSize(13);
            nb.setSingleLine(true);
            nb.setIncludeFontPadding(false);
            nb.setMinHeight(0);
            nb.setMinimumHeight(0);
            nb.setPadding(0, dp(14), 0, dp(14));
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
            String author = i.getStringExtra("author");
            String uri = i.getStringExtra("uri");
            long seed = i.getLongExtra("seed", System.nanoTime());
            boolean anim = i.getBooleanExtra("anim", false);
            i.removeExtra("text");
            fromAlert = true;
            showQuote(cat == null || cat.isEmpty() ? 'd' : cat.charAt(0), text,
                    author == null ? "" : author, seed, anim, uri == null ? "" : uri);
        }
    }

    @Override
    public void onBackPressed() {
        if (overlay != null) closeOverlay();
        else super.onBackPressed();
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

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 10 && res == RESULT_OK && data != null) {
            List<Uri> us = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    us.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                us.add(data.getData());
            }
            for (Uri u : us) {
                try {
                    getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception e) {
                    // kalıcı izin verilemedi
                }
                UserMedia.add(this, u.toString());
            }
            toast(us.size() + " dosya eklendi.");
            if (tab == 3) switchTab(3);
        }
    }

    // ---------------------------------------------------------------- tam ekran söz

    void closeOverlay() {
        if (overlay == null) return;
        if (overlayVideo != null) {
            try {
                overlayVideo.stopPlayback();
            } catch (Exception e) {
                // yoksay
            }
            overlayVideo = null;
        }
        frame.removeView(overlay);
        overlay = null;
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (fromAlert && km != null && km.isKeyguardLocked()) finish();
        fromAlert = false;
    }

    void showQuote(char cat, String text, String author, long seed, boolean anim, String uri) {
        if (overlay != null) {
            frame.removeView(overlay);
            overlay = null;
        }
        final int sw = getResources().getDisplayMetrics().widthPixels;
        final int sh = getResources().getDisplayMetrics().heightPixels;
        FrameLayout o = new FrameLayout(this);
        o.setBackgroundColor(Color.BLACK);

        boolean own = uri != null && !uri.isEmpty();
        boolean video = own && UserMedia.isVideo(this, uri);

        if (own && video && anim) {
            final VideoView vv = new VideoView(this);
            vv.setVideoURI(Uri.parse(uri));
            vv.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.setVolume(0f, 0f);
                int vw = mp.getVideoWidth();
                int vh = mp.getVideoHeight();
                if (vw > 0 && vh > 0) {
                    float fit = Math.min((float) sw / vw, (float) sh / vh);
                    float cover = Math.max((float) sw / vw, (float) sh / vh);
                    vv.setScaleX(cover / fit);
                    vv.setScaleY(cover / fit);
                }
                mp.start();
            });
            vv.setOnErrorListener((mp, what, extra) -> true);
            o.addView(vv, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
            overlayVideo = vv;
        } else {
            Bitmap bm = null;
            if (own) bm = UserMedia.bitmap(this, uri, sw / 2, sh / 2);
            if (bm == null) {
                if (anim) {
                    o.addView(new AnimatedBackground(this, cat, seed), new FrameLayout.LayoutParams(-1, -1));
                } else {
                    bm = Wallpaper.background(cat, sw / 2, sh / 2, seed);
                }
            }
            if (bm != null) {
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageBitmap(bm);
                o.addView(iv, new FrameLayout.LayoutParams(-1, -1));
                if (anim) iv.animate().scaleX(1.15f).scaleY(1.15f).setDuration(14000).start();
            }
        }

        QuoteView qv = new QuoteView(this, text, author);
        qv.setAlpha(0f);
        qv.setTranslationY(dp(28));
        o.addView(qv, new FrameLayout.LayoutParams(-1, -1));
        qv.animate().alpha(1f).translationY(0f).setStartDelay(250).setDuration(900).start();

        TextView hint = tv("Kapatmak için dokun", 12, false, 0x99FFFFFF);
        hint.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        hp.bottomMargin = dp(28);
        o.addView(hint, hp);

        o.setOnClickListener(view -> closeOverlay());
        overlay = o;
        frame.addView(o, new FrameLayout.LayoutParams(-1, -1));
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
        t.setIncludeFontPadding(false);
        t.setLineSpacing(0f, 1.15f);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    EditText et(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(0xFF64748B);
        e.setTextColor(TXT);
        e.setTextSize(15);
        e.setBackground(round(FIELD, 12, LINE));
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        e.setHighlightColor(0x66818CF8);
        // Görünür imleç (parlak çizgi)
        if (Build.VERSION.SDK_INT >= 29) {
            GradientDrawable cur = new GradientDrawable();
            cur.setColor(0xFFFFFFFF);
            cur.setSize(dp(2), dp(22));
            e.setTextCursorDrawable(cur);
        }
        return e;
    }

    Button btn(String s, boolean primary, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setIncludeFontPadding(false);
        b.setMinHeight(dp(46));
        b.setMinimumHeight(dp(46));
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
        lp.topMargin = dp(10);
        parent.addView(v, lp);
    }

    void label(LinearLayout parent, String s) {
        TextView t = tv(s, 13, false, MUT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(14);
        parent.addView(t, lp);
    }

    LinearLayout row(View a, View b) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1f);
        l1.rightMargin = dp(4);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, -2, 1f);
        l2.leftMargin = dp(4);
        r.addView(a, l1);
        r.addView(b, l2);
        return r;
    }

    Spinner spin(String[] items, int sel) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int p, View cv, ViewGroup par) {
                TextView t = (TextView) super.getView(p, cv, par);
                t.setText(getItem(p) + "  ▾");
                t.setTextColor(TXT);
                t.setTextSize(15);
                t.setSingleLine(true);
                t.setPadding(dp(12), dp(12), dp(12), dp(12));
                return t;
            }

            @Override
            public View getDropDownView(int p, View cv, ViewGroup par) {
                TextView t = (TextView) super.getDropDownView(p, cv, par);
                t.setText(getItem(p));
                t.setTextColor(TXT);
                t.setTextSize(15);
                t.setBackgroundColor(CARD);
                t.setPadding(dp(16), dp(14), dp(16), dp(14));
                return t;
            }
        };
        s.setAdapter(ad);
        s.setSelection(sel);
        s.setBackground(round(FIELD, 12, LINE));
        s.setPopupBackgroundDrawable(round(CARD, 12, LINE));
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
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        t.setBackground(round(user ? 0xFF312E81 : FIELD, 12, user ? 0 : LINE));
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
        tab = t;
        body.removeAllViews();
        LinearLayout c = col();
        c.setPadding(dp(16), dp(14), dp(16), dp(28));
        c.addView(tv(TITLES[t], 24, true, TXT));
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

    void dayStyle(TextView d, boolean on) {
        d.setTextColor(on ? 0xFF0B1020 : MUT);
        d.setBackground(round(on ? ACC : FIELD, 10, on ? 0 : LINE));
    }

    // ---------------------------------------------------------------- GÖREVLER

    void buildTasks(LinearLayout c) {
        LinearLayout form = card();
        add(c, form);
        form.addView(tv("Yeni görev", 18, true, TXT));

        final EditText title = et("Görev (örn: Her gün 1 saat kod yaz)");
        add(form, title);

        label(form, "Alan (seçince önerilen aralık otomatik gelir)");
        final Spinner area = spin(Areas.NAMES, 0);
        add(form, area);

        label(form, "Bitiş tarihi");
        final Calendar dl = Calendar.getInstance();
        dl.add(Calendar.DAY_OF_YEAR, 7);
        dl.set(Calendar.SECOND, 0);
        final Button dBtn = btn(DF.format(dl.getTime()), false, null);
        dBtn.setOnClickListener(view -> {
            new DatePickerDialog(this, (dpk, yy, mm, dd) -> {
                dl.set(yy, mm, dd);
                new TimePickerDialog(this, (tpk, hh, mi) -> {
                    dl.set(Calendar.HOUR_OF_DAY, hh);
                    dl.set(Calendar.MINUTE, mi);
                    dl.set(Calendar.SECOND, 0);
                    dBtn.setText(DF.format(dl.getTime()));
                }, dl.get(Calendar.HOUR_OF_DAY), dl.get(Calendar.MINUTE), true).show();
            }, dl.get(Calendar.YEAR), dl.get(Calendar.MONTH), dl.get(Calendar.DAY_OF_MONTH)).show();
        });
        add(form, dBtn);

        label(form, "Kaç aralıkla uyarsın?");
        final EditText every = et("3");
        every.setInputType(InputType.TYPE_CLASS_NUMBER);
        every.setText("3");
        final Spinner unit = spin(new String[]{"dakikada bir", "saatte bir", "günde bir"}, 1);
        add(form, row(every, unit));

        label(form, "Hangi saatler arasında uyarsın? (bu saatlerin dışında susar)");
        String[] startItems = new String[24];
        String[] endItems = new String[24];
        for (int i = 0; i < 24; i++) {
            startItems[i] = String.format("%02d:00", i);
            endItems[i] = String.format("%02d:00", i + 1);
        }
        final Spinner startSp = spin(startItems, 8);
        final Spinner endSp = spin(endItems, 22);
        add(form, row(startSp, endSp));

        label(form, "Hangi günler?");
        final boolean[] dayOn = {true, true, true, true, true, true, true};
        LinearLayout days = new LinearLayout(this);
        for (int i = 0; i < 7; i++) {
            final int di = i;
            final TextView d = new TextView(this);
            d.setText(DAYS[i]);
            d.setTextSize(12);
            d.setGravity(Gravity.CENTER);
            d.setIncludeFontPadding(false);
            d.setPadding(0, dp(12), 0, dp(12));
            dayStyle(d, true);
            d.setOnClickListener(view -> {
                dayOn[di] = !dayOn[di];
                dayStyle(d, dayOn[di]);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.leftMargin = dp(2);
            lp.rightMargin = dp(2);
            days.addView(d, lp);
        }
        add(form, days);

        label(form, "Uyarı sesi ve titreşim");
        final Spinner snd = spin(Sounds.NAMES, 0);
        final Spinner vib = spin(Sounds.VIB_NAMES, 2);
        add(form, row(snd, vib));

        onSelect(area, pos -> {
            every.setText(String.valueOf(Areas.EVERY[pos]));
            unit.setSelection(Areas.UNIT[pos]);
            startSp.setSelection(Areas.START[pos]);
            endSp.setSelection(Areas.END[pos] - 1);
        });

        Button test = btn("Sesi dene", false, view ->
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
            int sh = startSp.getSelectedItemPosition();
            int eh = endSp.getSelectedItemPosition() + 1;
            if (eh <= sh) {
                toast("Bitiş saati, başlangıç saatinden büyük olmalı.");
                return;
            }
            int mask = 0;
            for (int i = 0; i < 7; i++) if (dayOn[i]) mask |= (1 << i);
            if (mask == 0) {
                toast("En az bir gün seç.");
                return;
            }
            long[] mult = {60000L, 3600000L, 86400000L};
            Task t = new Task();
            t.id = UUID.randomUUID().toString();
            t.title = ttl;
            t.area = area.getSelectedItemPosition();
            t.deadline = deadline;
            t.everyMs = n * mult[unit.getSelectedItemPosition()];
            t.created = now;
            t.startHour = sh;
            t.endHour = eh;
            t.days = mask;
            t.next = t.adjust(now + t.everyMs);
            if (t.next > t.deadline) t.next = t.deadline;
            t.sound = snd.getSelectedItemPosition();
            t.vib = vib.getSelectedItemPosition();
            t.active = true;
            List<Task> ts = Store.tasks(this);
            ts.add(t);
            Store.saveTasks(this, ts);
            Scheduler.schedule(this, t);
            title.setText("");
            fillTasks();
            toast("Görev eklendi. İlk uyarı: " + DF.format(new java.util.Date(t.next)));
        });
        add(form, row(test, addB));

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
            String areaName = t.area >= 0 && t.area < Areas.NAMES.length ? Areas.NAMES[t.area] : "Genel";
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(6);
            cd.addView(tv(areaName + " • her " + fmtEvery(t.everyMs), 13, false, ACC), lp);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-1, -2);
            lp2.topMargin = dp(4);
            cd.addView(tv(t.windowText() + " • " + t.daysText(), 13, false, MUT), lp2);
            LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(-1, -2);
            lp3.topMargin = dp(4);
            cd.addView(tv(Sounds.NAMES[t.sound] + " • titreşim: " + Sounds.VIB_NAMES[t.vib], 13, false, MUT), lp3);
            LinearLayout.LayoutParams lp4 = new LinearLayout.LayoutParams(-1, -2);
            lp4.topMargin = dp(4);
            cd.addView(tv("Bitiş: " + DF.format(new java.util.Date(t.deadline)), 13, false, MUT), lp4);
            String st;
            if (t.active) {
                st = "Sonraki uyarı: " + DF.format(new java.util.Date(Math.min(t.next, t.deadline)));
            } else if (t.deadline <= now) {
                st = "Süre doldu";
            } else {
                st = "Duraklatıldı";
            }
            LinearLayout.LayoutParams lp5 = new LinearLayout.LayoutParams(-1, -2);
            lp5.topMargin = dp(8);
            cd.addView(tv(st, 14, true, TXT), lp5);
            Button b1 = btn(t.active ? "Duraklat" : "Başlat", false, view -> toggleTask(tid));
            Button b2 = btn("Sil", false, view -> deleteTask(tid));
            add(cd, row(b1, b2));
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
                x.next = x.adjust(now + x.everyMs);
                if (x.next > x.deadline) x.next = x.deadline;
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
        label(w, "Bu hafta hedefe ne kadar ilerledim? (1-5)");
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
        label(g, "Dil");
        Spinner ls = spin(names, langIdx);
        onSelect(ls, pos -> {
            langIdx = pos;
            Store.put(this, "langIdx", String.valueOf(pos));
        });
        add(g, ls);
        label(g, "Seviye");
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
        Button mic = btn("🎤 Konuş", false, view -> listen());
        Button send = btn("Gönder", true, view -> {
            String s = langInput.getText().toString();
            langInput.setText("");
            sendLang(s);
        });
        add(ch, row(mic, send));
    }

    // ---------------------------------------------------------------- AYARLAR

    void buildSettings(LinearLayout c) {
        // --- arka plan / duvar kağıdı
        LinearLayout wp = card();
        add(c, wp);
        wp.addView(tv("Arka plan / duvar kağıdı", 18, true, TXT));
        wp.addView(tv("Bildirimdeki resim hep sabittir (Android bildirimde animasyona izin vermez). "
                + "Hareket, bildirime dokununca açılan tam ekranda görünür.", 13, false, MUT));

        label(wp, "Tür");
        Spinner typeSp = spin(new String[]{"Sabit", "Hareketli", "Karışık (her uyarıda rastgele)"},
                parse(Store.get(this, "wptype", "2"), 2));
        onSelect(typeSp, pos -> Store.put(this, "wptype", String.valueOf(pos)));
        add(wp, typeSp);

        label(wp, "Kaynak");
        Spinner srcSp = spin(new String[]{"Otomatik (kodla üretilir)", "Kendi resim/videolarım", "İkisi karışık"},
                parse(Store.get(this, "wpsrc", "0"), 0));
        onSelect(srcSp, pos -> Store.put(this, "wpsrc", String.valueOf(pos)));
        add(wp, srcSp);

        final TextView cnt = tv(UserMedia.list(this).size() + " dosya seçili", 13, true, ACC);
        add(wp, cnt);
        add(wp, btn("Telefondan resim / video ekle", false, view -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i, 10);
        }));
        add(wp, btn("Seçilenleri temizle", false, view -> {
            UserMedia.clear(this);
            cnt.setText("0 dosya seçili");
        }));

        CheckBox fs = new CheckBox(this);
        fs.setText("Uyarı gelince tam ekran aç (kilit ekranında da)");
        fs.setTextColor(TXT);
        fs.setChecked("1".equals(Store.get(this, "fullscreen", "0")));
        fs.setOnCheckedChangeListener((bv, on) -> Store.put(this, "fullscreen", on ? "1" : "0"));
        add(wp, fs);
        if (Build.VERSION.SDK_INT >= 34) {
            add(wp, btn("Tam ekran izni (Android 14+)", false, view -> {
                try {
                    Intent i = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                    i.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                } catch (Exception e) {
                    toast("Bu telefonda bu izin ayrıca gerekmiyor.");
                }
            }));
        }
        add(wp, btn("Önizle", true, view -> {
            Quotes.Q q = Quotes.next(this);
            long seed = System.nanoTime();
            Notifier.Pick pk = Notifier.pick(this, new Random(seed));
            fromAlert = false;
            showQuote(q.cat, q.text, q.author, seed, pk.anim, pk.uri);
        }));

        // --- alarmın güvenle çalışması
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

        // --- kilit ekranı kartı
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

        // --- sesler
        LinearLayout s = card();
        add(c, s);
        s.addView(tv("Sesler", 18, true, TXT));
        for (int i = 0; i < Sounds.NAMES.length; i++) {
            final int si = i;
            add(s, btn("▶ " + Sounds.NAMES[i], false, view -> Sounds.preview(this, si, 0)));
        }

        // --- API anahtarı
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
