package com.koc.hatirlatici;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Random;

/** Ayarlar sekmesi: dokununca açılan bölümler. */
public class SettingsUi {

    public static void show(final MainActivity a) {
        LinearLayout pg = a.pageCol("Ayarlar", null);

        notif(a, Ui.section(a, pg, "set_notif", "🔔 Bildirim ve alarm", "İzinler, tam ekran uyarı, test"));
        wallpaper(a, Ui.section(a, pg, "set_wp", "🖼 Arka plan / duvar kağıdı", "Sabit, hareketli, kendi dosyaların, tema galerisi"));
        lock(a, Ui.section(a, pg, "set_lock", "🔒 Kilit ekranı", "Söz kartı ve görev kartı"));
        sounds(a, Ui.section(a, pg, "set_snd", "🎵 Sesler ve titreşimler", "16 melodi, 11 titreşim — dinleyip dene"));
        ai(a, Ui.section(a, pg, "set_ai", "🤖 Yapay zekâ", a.key().isEmpty() ? "API anahtarı girilmemiş" : "Model: " + a.modelLabel()));
        backup(a, Ui.section(a, pg, "set_bak", "💾 Yedekleme ve geri yükleme", "Uygulama silinse bile verilerin kurtarılsın"));
        about(a, Ui.section(a, pg, "set_about", "ℹ Hakkında", "Sürüm ve notlar"));
    }

    static TextView note(MainActivity a, String s) {
        TextView t = Ui.tv(a, s, 12.5f, Ui.SUB, false);
        t.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 8));
        return t;
    }

    // ------------------------------------------------------------ bildirim

    static void notif(final MainActivity a, LinearLayout b) {
        b.addView(note(a, "Uyarıların uygulama kapalıyken ve telefon uykudayken de çalması için aşağıdaki izinlerin açık olması gerekir."));
        Ui.add(b, Ui.btn(a, "Bildirim ayarlarını aç", Ui.CARD2, v -> {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName());
            a.startActivity(i);
        }));
        if (Build.VERSION.SDK_INT >= 31) {
            Ui.add(b, Ui.btn(a, "Tam zamanlı alarm iznini kontrol et", Ui.CARD2, v -> {
                AlarmManager am = (AlarmManager) a.getSystemService(Context.ALARM_SERVICE);
                if (am.canScheduleExactAlarms()) {
                    a.toast("Tam zamanlı alarm izni zaten açık ✅");
                } else {
                    try {
                        a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + a.getPackageName())));
                    } catch (Exception e) {
                        a.toast("Telefon ayarlarından 'Alarmlar ve hatırlatıcılar' iznini aç.");
                    }
                }
            }));
        }
        Ui.add(b, Ui.btn(a, "Pil kısıtlamasını kaldır (önerilir)", Ui.CARD2, v -> {
            try {
                a.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                a.toast("Listeden Hatırlatıcı Koç'u bulup 'Kısıtlama yok' seç.");
            } catch (Exception e) {
                a.toast("Telefon ayarlarından pil kısıtlamasını kapat.");
            }
        }));
        b.addView(Ui.toggle(a, "Uyarı gelince tam ekran aç (ekran kapalıyken)", "1".equals(Store.get(a, "fullscreen", "0")),
                on -> Store.put(a, "fullscreen", on ? "1" : "0")));
        Ui.add(b, Ui.btn(a, "🔔 Test bildirimi gönder", Ui.ACC, v -> {
            Task t = new Task();
            t.id = "test";
            t.title = "Test görevi";
            t.deadline = System.currentTimeMillis() + 3600000L;
            Notifier.show(a, t, false);
            a.toast("Bildirim gönderildi. Gelmediyse bildirim iznini kontrol et.");
        }));
    }

    // ------------------------------------------------------------ duvar kağıdı

    static void wallpaper(final MainActivity a, LinearLayout b) {
        b.addView(Ui.label(a, "UYARILARDA GÖSTERİM"));
        int mode = 2;
        try {
            mode = Integer.parseInt(Store.get(a, "wpmode", "2"));
        } catch (Exception e) {
            // varsayılan
        }
        Ui.add(b, Ui.spin(a, new String[]{"Sadece sabit resimler", "Sadece hareketli", "Karışık (sabit + hareketli)"}, mode,
                i -> Store.put(a, "wpmode", String.valueOf(i))));
        b.addView(note(a, "Bildirim önizlemesindeki resim sabittir; bildirime dokununca açılan tam ekran söz hareketli olabilir."));
        b.addView(Ui.toggle(a, "Kendi dosyalarımı da karıştır", "1".equals(Store.get(a, "usemedia", "1")),
                on -> Store.put(a, "usemedia", on ? "1" : "0")));

        int n = UserMedia.list(a).size();
        final TextView cnt = Ui.tv(a, "Eklenen kendi dosyaların: " + n, 13, Ui.TXT, false);
        cnt.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
        b.addView(cnt);
        LinearLayout r = Ui.row(a);
        Ui.addW(r, Ui.btn(a, "📁 Resim/video ekle", Ui.ACC, v -> a.pickMedia(() -> cnt.setText("Eklenen kendi dosyaların: " + UserMedia.list(a).size()))));
        Ui.addW(r, Ui.btn(a, "Temizle", Ui.CARD2, v -> {
            UserMedia.clear(a);
            cnt.setText("Eklenen kendi dosyaların: 0");
        }));
        Ui.add(b, r);

        b.addView(Ui.label(a, "TEMA GALERİSİ (" + Scenes.COUNT + " tema)"));
        for (int i = 0; i < Scenes.COUNT; i += 2) {
            LinearLayout row = Ui.row(a);
            for (int k = i; k < Math.min(i + 2, Scenes.COUNT); k++) Ui.addW(row, themeCell(a, k));
            Ui.add(b, row);
        }
    }

    static View themeCell(final MainActivity a, final int th) {
        LinearLayout c = Ui.col(a);
        ImageView iv = new ImageView(a);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setImageBitmap(Notifier.thumb(a, th, 220));
        c.addView(iv, new LinearLayout.LayoutParams(-1, Ui.dp(a, 110)));
        TextView t = Ui.tv(a, Scenes.NAMES[th], 13, Color.WHITE, true);
        t.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 4));
        c.addView(t);
        LinearLayout br = Ui.row(a);
        Ui.addW(br, small(a, "Sabit", th, false));
        Ui.addW(br, small(a, "Hareketli", th, true));
        c.addView(br);
        return c;
    }

    static View small(final MainActivity a, String s, final int th, final boolean anim) {
        TextView t = Ui.chip(a, s, anim, v -> {
            Quotes.Q q = Quotes.next(a);
            a.showQuote(q.cat, q.text, q.author, new Random().nextLong(), anim, null, th);
        });
        return t;
    }

    // ------------------------------------------------------------ kilit ekranı

    static void lock(final MainActivity a, LinearLayout b) {
        b.addView(note(a, "Kilit ekranında bildirim içeriğinin görünmesi için telefonun Ayarlar > Kilit ekranı > Bildirimler bölümünde 'Tüm içeriği göster' seçili olmalı."));
        b.addView(Ui.toggle(a, "Günün sözü kartı (küçük duvar kağıdı + söz)", "1".equals(Store.get(a, "lockcard", "0")), on -> {
            Store.put(a, "lockcard", on ? "1" : "0");
            if (on) Notifier.lockCard(a, Quotes.next(a), System.nanoTime());
            else Notifier.removeLockCard(a);
        }));
        b.addView(Ui.toggle(a, "Görevlerim kartı (aktif görevler + kalan süre)", "1".equals(Store.get(a, "taskcard", "1")), on -> {
            Store.put(a, "taskcard", on ? "1" : "0");
            Notifier.updateTaskCard(a);
        }));
        Ui.add(b, Ui.btn(a, "Söz kartını şimdi yenile", Ui.CARD2, v -> {
            if ("1".equals(Store.get(a, "lockcard", "0"))) Notifier.lockCard(a, Quotes.next(a), System.nanoTime());
            else a.toast("Önce söz kartını aç.");
        }));
        Ui.add(b, Ui.btn(a, "Bildirim ayarlarını aç", Ui.CARD2, v -> {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName());
            a.startActivity(i);
        }));
    }

    // ------------------------------------------------------------ sesler

    static void sounds(final MainActivity a, LinearLayout b) {
        b.addView(Ui.label(a, "MELODİLER"));
        for (int i = 0; i < Sounds.NAMES.length; i += 2) {
            LinearLayout r = Ui.row(a);
            for (int k = i; k < Math.min(i + 2, Sounds.NAMES.length); k++) {
                final int s = k;
                Ui.addW(r, Ui.btn(a, "▶ " + Sounds.NAMES[k], Ui.CARD2, v -> Sounds.preview(a, s, -1)));
            }
            Ui.add(b, r);
        }
        b.addView(Ui.label(a, "TİTREŞİMLER"));
        for (int i = 0; i < Sounds.VIB_NAMES.length; i += 2) {
            LinearLayout r = Ui.row(a);
            for (int k = i; k < Math.min(i + 2, Sounds.VIB_NAMES.length); k++) {
                final int s = k;
                Ui.addW(r, Ui.btn(a, "📳 " + Sounds.VIB_NAMES[k], Ui.CARD2, v -> Sounds.preview(a, -1, s)));
            }
            Ui.add(b, r);
        }
        b.addView(note(a, "Her görev için ses ve titreşimi görev eklerken seçersin."));
    }

    // ------------------------------------------------------------ yapay zekâ

    static void ai(final MainActivity a, LinearLayout b) {
        b.addView(note(a, "Koç, yabancı dil sınavı/dersleri ve sohbetler için Claude API anahtarı gerekir (console.anthropic.com). "
                + "Kullandıkça API hesabından küçük bir ücret çekilir. Anahtar sadece bu telefonda saklanır, yedeğe yazılmaz."));
        final EditText k = Ui.et(a, "sk-ant-… API anahtarı", false);
        k.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        k.setText(Store.get(a, "apikey", ""));
        Ui.add(b, k);
        b.addView(Ui.label(a, "MODEL"));
        int mi = 0;
        for (int i = 0; i < Claude.MODELS.length; i++) if (Claude.MODELS[i].equals(Claude.model(a))) mi = i;
        Ui.add(b, Ui.spin(a, Claude.MODEL_NAMES, mi, i -> {
            Store.put(a, "model", Claude.MODELS[i]);
            a.toast("Model: " + Claude.MODEL_NAMES[i]);
        }));
        Ui.add(b, Ui.btn(a, "Kaydet", Ui.ACC, v -> {
            Store.put(a, "apikey", k.getText().toString().trim());
            a.toast("Anahtar kaydedildi.");
            show(a);
        }));
        Ui.add(b, Ui.btn(a, "Anahtarı test et", Ui.CARD2, v -> {
            Store.put(a, "apikey", k.getText().toString().trim());
            JSONArray m = new JSONArray();
            try {
                JSONObject o = new JSONObject();
                o.put("role", "user");
                o.put("content", "Reply with the single word: OK");
                m.put(o);
            } catch (Exception e) {
                return;
            }
            a.aiChat("Reply briefly.", m, 20, "Test ediliyor…", (txt, err) -> {
                if (txt != null) a.toast("✅ Çalışıyor. Kullanılan model: " + a.modelLabel());
            });
        }));
    }

    // ------------------------------------------------------------ yedekleme

    static void backup(final MainActivity a, final LinearLayout b) {
        b.addView(note(a, "Önemli: Android, uygulamayı kaldırdığında uygulamanın iç verilerini siler. Görev, koç kayıtları, dil sonuçları ve "
                + "sohbetlerin kaybolmaması için bir yedek dosyası seç: uygulama her değişiklikte bu dosyayı kendiliğinden günceller. "
                + "Dosya telefonunun belleğinde (örn. İndirilenler) kalır; uygulamayı silsen bile durur. Yeniden kurunca 'Yedekten geri yükle' ile hepsi gelir."));
        String uri = Store.get(a, "backup_uri", "");
        String tm = Store.get(a, "backup_time", "");
        String status;
        if (uri.isEmpty()) status = "⚠ Yedek dosyası seçilmedi.";
        else {
            long t = 0;
            try {
                t = Long.parseLong(tm);
            } catch (Exception e) {
                // yok
            }
            status = "✅ Yedek aktif" + (t > 0 ? " · son yazılma: " + Ui.fmt(t) : "");
        }
        TextView st = Ui.tv(a, status, 14, uri.isEmpty() ? Ui.WARN : Ui.OK, true);
        st.setPadding(0, 0, 0, Ui.dp(a, 8));
        b.addView(st);
        final Runnable refresh = () -> show(a);
        Ui.add(b, Ui.btn(a, uri.isEmpty() ? "Yedek dosyası oluştur" : "Yedek dosyasını değiştir", Ui.ACC, v -> a.createBackup(refresh)));
        Ui.add(b, Ui.btn(a, "Şimdi yedekle", Ui.CARD2, v -> {
            if (Store.get(a, "backup_uri", "").isEmpty()) {
                a.toast("Önce bir yedek dosyası oluştur.");
                return;
            }
            Backup.auto(a);
            a.toast("Yedeklendi.");
            show(a);
        }));
        Ui.add(b, Ui.btn(a, "Yedekten geri yükle", 0xFF047857, v -> a.restoreBackup(refresh)));
        b.addView(note(a, "API anahtarı güvenlik için yedeğe yazılmaz; geri yükledikten sonra yeniden girmen gerekir."));
    }

    // ------------------------------------------------------------ hakkında

    static void about(MainActivity a, LinearLayout b) {
        b.addView(Ui.tv(a, "Hatırlatıcı Koç v3.0\n\n• Görevler: tarihe göre sıralı liste, + ile ekleme, kilit ekranı kartı\n"
                + "• Koç: çoklu kariyer, kalıcı ilerleme kayıtları, sohbet geçmişi\n"
                + "• Dil: seviye sınavı, yol haritası, puanlı dilbilgisi, hedef dilde sohbet\n\n"
                + "Bu uygulama kişisel kullanım içindir.", 13.5f, Ui.TXT, false));
    }
}
