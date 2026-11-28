package com.koc.hatirlatici;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Ana iskelet: sekmeler, sayfa geçişleri, yapay zekâ çağrıları, konuşma tanıma/okuma, tam ekran söz görünümü. */
public class MainActivity extends Activity {
    public interface Res {
        void on(String text, String err);
    }

    public interface JRes {
        void on(JSONObject o, String err);
    }

    public interface SttCb {
        void partial(String s);

        void done(String best, List<String> alts);

        void error(String msg);
    }

    FrameLayout root, bodyHost;
    View curBody;
    TextView fab;
    LinearLayout nav;
    TextView[] navBtns = new TextView[4];
    View overlay, busyView;
    TextView busyTxt;
    int tab = 0;
    Runnable back;
    Runnable afterPick, afterBackup;

    SpeechRecognizer sr;
    SttCb stt;
    String sttTag = "en-US";
    Runnable pendingListen;
    TextToSpeech tts;
    boolean ttsOk;
    String pendTtsText, pendTtsLang;
    VideoView video;

    static final String[] NAV = {"📋 Görevler", "🎯 Koç", "🌍 Dil", "⚙ Ayarlar"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.CARD);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));

        bodyHost = new FrameLayout(this);
        main.addView(bodyHost, new LinearLayout.LayoutParams(-1, 0, 1f));

        fab = Ui.tv(this, "+", 30, Color.WHITE, true);
        fab.setGravity(Gravity.CENTER);
        fab.setBackground(Ui.round(this, Ui.ACC, 32));
        fab.setVisibility(View.GONE);
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 62));
        fl.gravity = Gravity.BOTTOM | Gravity.END;
        fl.rightMargin = Ui.dp(this, 18);
        fl.bottomMargin = Ui.dp(this, 18);
        bodyHost.addView(fab, fl);

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Ui.CARD);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            TextView t = Ui.tv(this, NAV[i], 12.5f, Ui.SUB, true);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
            t.setOnClickListener(v -> showTab(idx));
            navBtns[i] = t;
            nav.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        main.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
        }
        try {
            Scheduler.scheduleAll(this);
            Notifier.updateTaskCard(this);
            if ("1".equals(Store.get(this, "lockcard", "0"))) {
                Notifier.lockCard(this, Quotes.next(this), System.nanoTime());
            }
        } catch (Exception e) {
            // yoksay
        }
        showTab(0);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handleIntent(i);
    }

    void handleIntent(Intent i) {
        if (i == null) return;
        if (i.hasExtra("text")) {
            String c = i.getStringExtra("cat");
            showQuote(c == null || c.isEmpty() ? 'b' : c.charAt(0), i.getStringExtra("text"),
                    i.getStringExtra("author") == null ? "" : i.getStringExtra("author"),
                    i.getLongExtra("seed", System.nanoTime()), i.getBooleanExtra("anim", false),
                    i.getStringExtra("uri"), i.getIntExtra("theme", -1));
            i.removeExtra("text");
        } else if (i.hasExtra("tab")) {
            showTab(i.getIntExtra("tab", 0));
            i.removeExtra("tab");
        }
    }

    // ------------------------------------------------------------ sayfa iskeleti

    public void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    public void showTab(int i) {
        tab = i;
        back = null;
        for (int k = 0; k < 4; k++) navBtns[k].setTextColor(k == i ? Color.WHITE : Ui.SUB);
        for (int k = 0; k < 4; k++) navBtns[k].setBackgroundColor(k == i ? Ui.CARD2 : Ui.CARD);
        if (i == 0) TasksUi.show(this);
        else if (i == 1) CoachUi.show(this);
        else if (i == 2) LangUi.show(this);
        else SettingsUi.show(this);
    }

    void setBody(View v) {
        if (curBody != null) bodyHost.removeView(curBody);
        curBody = v;
        bodyHost.addView(v, 0, new FrameLayout.LayoutParams(-1, -1));
        fab.setVisibility(View.GONE);
    }

    public void showFab(String text, Runnable r) {
        fab.setText(text);
        fab.setVisibility(View.VISIBLE);
        fab.bringToFront();
        fab.setOnClickListener(v -> r.run());
    }

    LinearLayout header(String title, Runnable backAction) {
        LinearLayout h = Ui.row(this);
        if (backAction != null) {
            TextView b = Ui.tv(this, "‹", 30, Color.WHITE, true);
            b.setPadding(0, 0, Ui.dp(this, 14), Ui.dp(this, 4));
            b.setOnClickListener(v -> backAction.run());
            h.addView(b);
        }
        TextView t = Ui.tv(this, title, 21, Color.WHITE, true);
        h.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        h.setPadding(0, 0, 0, Ui.dp(this, 10));
        return h;
    }

    /** Kaydırılabilir sayfa; içerik kutusunu döndürür. */
    public LinearLayout pageCol(String title, Runnable backAction) {
        back = backAction;
        LinearLayout r = Ui.col(this);
        int p = Ui.dp(this, 16);
        r.setPadding(p, p, p, 0);
        r.addView(header(title, backAction));
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(false);
        LinearLayout c = Ui.col(this);
        c.setPadding(0, 0, 0, Ui.dp(this, 96));
        sv.addView(c, new ViewGroup.LayoutParams(-1, -2));
        r.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        setBody(r);
        return c;
    }

    /** Sabit sayfa (sohbet gibi): başlık + içine kendi düzenini ekleyeceğin sütun. */
    public LinearLayout fixedPage(String title, Runnable backAction) {
        back = backAction;
        LinearLayout r = Ui.col(this);
        int p = Ui.dp(this, 16);
        r.setPadding(p, p, p, Ui.dp(this, 8));
        r.addView(header(title, backAction));
        setBody(r);
        return r;
    }

    @Override
    public void onBackPressed() {
        if (overlay != null) {
            hideOverlay();
        } else if (busyView != null) {
            // yapay zekâ beklenirken geri tuşu yoksayılır
        } else if (back != null) {
            back.run();
        } else if (tab != 0) {
            showTab(0);
        } else {
            super.onBackPressed();
        }
    }

    // ------------------------------------------------------------ bekleme ekranı + yapay zekâ

    public void busy(String msg) {
        idle();
        FrameLayout f = new FrameLayout(this);
        f.setBackgroundColor(0xCC0F1020);
        f.setClickable(true);
        LinearLayout c = Ui.col(this);
        c.setGravity(Gravity.CENTER);
        c.addView(new ProgressBar(this));
        busyTxt = Ui.tv(this, msg, 15, Color.WHITE, false);
        busyTxt.setGravity(Gravity.CENTER);
        busyTxt.setPadding(Ui.dp(this, 30), Ui.dp(this, 14), Ui.dp(this, 30), 0);
        c.addView(busyTxt);
        f.addView(c, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        busyView = f;
        root.addView(f, new FrameLayout.LayoutParams(-1, -1));
    }

    public void idle() {
        if (busyView != null) {
            root.removeView(busyView);
            busyView = null;
        }
    }

    public String key() {
        return Store.get(this, "apikey", "").trim();
    }

    public String modelLabel() {
        String m = Claude.model(this);
        for (int i = 0; i < Claude.MODELS.length; i++) if (Claude.MODELS[i].equals(m)) return Claude.MODEL_NAMES[i];
        return m;
    }

    boolean needKey() {
        if (key().isEmpty()) {
            toast("Önce Ayarlar > Yapay zekâ bölümüne API anahtarını gir.");
            return true;
        }
        return false;
    }

    /** Arka planda Claude'a sohbet isteği gönderir; sonucu ana iş parçacığında döndürür. busyMsg null ise ekran kilitlenmez. */
    public void aiChat(final String system, final JSONArray msgs, final int max, String busyMsg, final Res cb) {
        if (needKey()) {
            cb.on(null, "API anahtarı yok");
            return;
        }
        final boolean showBusy = busyMsg != null;
        if (showBusy) busy(busyMsg);
        final String k = key();
        final String model = Claude.model(this);
        new Thread(() -> {
            String out = null;
            String err = null;
            try {
                out = Claude.chat(k, model, system, msgs, max);
            } catch (Exception e) {
                err = e.getMessage() == null ? "Bağlantı hatası" : e.getMessage();
            }
            final String fo = out;
            final String fe = err;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                if (showBusy) idle();
                if (fe != null) toast(fe);
                cb.on(fo, fe);
            });
        }).start();
    }

    public void aiJson(final String system, final String user, final int max, String busyMsg, final JRes cb) {
        if (needKey()) {
            cb.on(null, "API anahtarı yok");
            return;
        }
        busy(busyMsg);
        final String k = key();
        final String model = Claude.model(this);
        new Thread(() -> {
            JSONObject out = null;
            String err = null;
            try {
                out = Claude.json(k, model, system, user, max);
            } catch (Exception e) {
                err = e.getMessage() == null ? "Yanıt anlaşılamadı, tekrar dene." : e.getMessage();
            }
            final JSONObject fo = out;
            final String fe = err;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                idle();
                if (fe != null) toast(fe);
                cb.on(fo, fe);
            });
        }).start();
    }

    // ------------------------------------------------------------ konuşma tanıma (mikrofon -> yazı)

    Intent speechIntent(String tag) {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L);
        return i;
    }

    public boolean listening() {
        return sr != null;
    }

    public void stopListen() {
        if (sr != null) {
            try {
                sr.cancel();
                sr.destroy();
            } catch (Exception e) {
                // yoksay
            }
            sr = null;
        }
    }

    /** Mikrofonu açar. Konuşurken kısmi sonuçları, bitince en iyi sonucu ve alternatifleri bildirir. */
    public void listen(final String tag, final SttCb cb) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingListen = () -> listen(tag, cb);
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 21);
            return;
        }
        stopListen();
        stt = cb;
        sttTag = tag;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            systemSpeech();
            return;
        }
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle p) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float v) {
            }

            @Override
            public void onBufferReceived(byte[] b) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onEvent(int t, Bundle p) {
            }

            @Override
            public void onPartialResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty() && stt != null) stt.partial(r.get(0));
            }

            @Override
            public void onResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                SttCb cur = stt;
                stopListen();
                stt = null;
                if (cur == null) return;
                if (r == null || r.isEmpty()) {
                    cur.error("Ses anlaşılamadı, biraz daha yüksek ve net söyle.");
                    return;
                }
                cur.done(r.get(0), new ArrayList<>(r.subList(1, r.size())));
            }

            @Override
            public void onError(int e) {
                SttCb cur = stt;
                stopListen();
                if (cur == null) return;
                if (e == SpeechRecognizer.ERROR_NO_MATCH || e == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    stt = null;
                    cur.error("Ses algılanmadı. Mikrofona yakın ve net konuşup tekrar dene.");
                } else if (e == SpeechRecognizer.ERROR_CLIENT) {
                    stt = null;
                } else {
                    systemSpeech();
                }
            }
        });
        try {
            sr.startListening(speechIntent(tag));
        } catch (Exception e) {
            stopListen();
            systemSpeech();
        }
    }

    /** Uygulama içi tanıma çalışmazsa telefonun kendi konuşma penceresine geçilir. */
    void systemSpeech() {
        try {
            startActivityForResult(speechIntent(sttTag), 11);
        } catch (Exception e) {
            SttCb cur = stt;
            stt = null;
            if (cur != null) cur.error("Bu telefonda konuşma tanıma bulunamadı. Klavyedeki mikrofon tuşunu dene.");
        }
    }

    // ------------------------------------------------------------ sesli okuma

    public void speak(String text, String lang) {
        if (tts == null) {
            pendTtsText = text;
            pendTtsLang = lang;
            tts = new TextToSpeech(this, st -> {
                ttsOk = st == TextToSpeech.SUCCESS;
                if (ttsOk && pendTtsText != null) {
                    doSpeak(pendTtsText, pendTtsLang);
                }
                pendTtsText = null;
            });
            return;
        }
        if (!ttsOk) {
            toast("Telefonda sesli okuma motoru bulunamadı.");
            return;
        }
        doSpeak(text, lang);
    }

    void doSpeak(String text, String lang) {
        try {
            tts.setLanguage(Locale.forLanguageTag(lang));
            tts.setSpeechRate(0.9f);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u");
        } catch (Exception e) {
            // yoksay
        }
    }

    public void stopSpeak() {
        if (tts != null && ttsOk) tts.stop();
    }

    // ------------------------------------------------------------ dosya seçimleri

    public void pickMedia(Runnable after) {
        afterPick = after;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, 10);
    }

    public void createBackup(Runnable after) {
        afterBackup = after;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "hatirlatici-koc-yedek.json");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, 12);
    }

    public void restoreBackup(Runnable after) {
        afterBackup = after;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, 13);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (req == 11) {
            SttCb cur = stt;
            stt = null;
            if (cur == null) return;
            if (res == RESULT_OK && d != null) {
                ArrayList<String> r = d.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (r != null && !r.isEmpty()) {
                    cur.done(r.get(0), new ArrayList<>(r.subList(1, r.size())));
                    return;
                }
            }
            cur.error("Ses anlaşılamadı.");
            return;
        }
        if (res != RESULT_OK || d == null) return;
        if (req == 10) {
            List<Uri> us = new ArrayList<>();
            ClipData cd = d.getClipData();
            if (cd != null) {
                for (int i = 0; i < cd.getItemCount(); i++) us.add(cd.getItemAt(i).getUri());
            } else if (d.getData() != null) {
                us.add(d.getData());
            }
            for (Uri u : us) {
                try {
                    getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception e) {
                    // kalıcı izin alınamadı
                }
                UserMedia.add(this, u.toString());
            }
            toast(us.size() + " dosya eklendi.");
            if (afterPick != null) afterPick.run();
        } else if (req == 12) {
            Uri u = d.getData();
            if (u == null) return;
            try {
                getContentResolver().takePersistableUriPermission(u,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception e) {
                // yoksay
            }
            Store.put(this, "backup_uri", u.toString());
            if (Backup.writeTo(this, u)) {
                Store.put(this, "backup_time", String.valueOf(System.currentTimeMillis()));
                toast("Yedek dosyası hazır. Bundan sonra her değişiklikte kendiliğinden güncellenecek.");
            } else {
                toast("Dosyaya yazılamadı, başka bir yer seç.");
            }
            if (afterBackup != null) afterBackup.run();
        } else if (req == 13) {
            Uri u = d.getData();
            if (u == null) return;
            String json = Backup.readFrom(this, u);
            if (json != null && Backup.restore(this, json)) {
                try {
                    Scheduler.scheduleAll(this);
                    Notifier.updateTaskCard(this);
                } catch (Exception e) {
                    // yoksay
                }
                toast("Yedek geri yüklendi: görevler, koç kayıtları, dil sonuçları ve sohbetler geldi.");
            } else {
                toast("Bu dosya bir Hatırlatıcı Koç yedeği değil.");
            }
            if (afterBackup != null) afterBackup.run();
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req == 21) {
            Runnable r = pendingListen;
            pendingListen = null;
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
                if (r != null) r.run();
            } else {
                toast("Mikrofon izni verilmedi. Telefon ayarlarından izin verebilirsin.");
            }
        }
    }

    // ------------------------------------------------------------ tam ekran söz görünümü

    public void hideOverlay() {
        if (overlay != null) {
            if (video != null) {
                try {
                    video.stopPlayback();
                } catch (Exception e) {
                    // yoksay
                }
                video = null;
            }
            root.removeView(overlay);
            overlay = null;
        }
    }

    public void showQuote(final char cat, final String text, final String author, final long seed,
                          final boolean anim, final String uri, final int theme) {
        hideOverlay();
        FrameLayout o = new FrameLayout(this);
        o.setBackgroundColor(Color.BLACK);
        o.setClickable(true);
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        View bgv = null;
        if (uri != null && UserMedia.isVideo(this, uri)) {
            try {
                video = new VideoView(this);
                video.setVideoURI(Uri.parse(uri));
                video.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    mp.setVolume(0f, 0f);
                    video.start();
                });
                FrameLayout.LayoutParams vl = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
                o.addView(video, vl);
                bgv = video;
            } catch (Exception e) {
                video = null;
            }
        }
        if (bgv == null) {
            if (uri == null && anim) {
                bgv = new AnimatedBackground(this, theme, cat, seed);
            } else {
                Bitmap bm = null;
                if (uri != null) bm = UserMedia.bitmap(this, uri, w, h);
                if (bm == null) bm = Scenes.scene(theme < 0 ? (int) (seed & 0x7fffffff) % Scenes.COUNT : theme, cat, w / 2, h / 2, seed);
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageBitmap(bm);
                bgv = iv;
            }
            o.addView(bgv, new FrameLayout.LayoutParams(-1, -1));
        }
        o.addView(new QuoteView(this, text, author), new FrameLayout.LayoutParams(-1, -1));

        LinearLayout bar = Ui.row(this);
        bar.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 28));
        android.widget.Button nb = Ui.btn(this, "↻ Yeni söz", 0x99000000, v -> {
            Quotes.Q q = Quotes.next(this);
            Random r = new Random();
            Notifier.Pick pk = Notifier.pick(this, r, -1);
            showQuote(q.cat, q.text, q.author, r.nextLong(), pk.anim, pk.uri, pk.theme);
        });
        android.widget.Button cb = Ui.btn(this, "Kapat", Ui.ACC, v -> hideOverlay());
        Ui.addW(bar, nb);
        Ui.addW(bar, cb);
        o.addView(bar, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        overlay = o;
        root.addView(o, new FrameLayout.LayoutParams(-1, -1));
    }

    @Override
    protected void onDestroy() {
        stopListen();
        if (tts != null) {
            try {
                tts.shutdown();
            } catch (Exception e) {
                // yoksay
            }
        }
        super.onDestroy();
    }
}
