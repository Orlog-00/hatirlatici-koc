package com.koc.hatirlatici;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Görevler sekmesi: kayıtlı görev listesi (sıralı, küçük duvar kağıdıyla) + "+" ile görev ekleme sayfası. */
public class TasksUi {
    static final String[] SORTS = {"Yaklaşan tarihe göre", "Yeni eklenene göre", "Uyarı sayısına göre", "A-Z"};

    interface Mut {
        void f(Task t);
    }

    static void update(MainActivity a, String id, Mut m) {
        List<Task> ts = Store.tasks(a);
        for (Task t : ts) {
            if (t.id.equals(id)) {
                m.f(t);
                break;
            }
        }
        Store.saveTasks(a, ts);
    }

    public static void show(final MainActivity a) {
        LinearLayout pg = a.pageCol("Görevlerim", null);
        final long now = System.currentTimeMillis();
        List<Task> all = Store.tasks(a);
        int nAct = 0, nDone = 0;
        for (Task t : all) {
            int s = t.status(now);
            if (s == Task.ACTIVE) nAct++;
            if (s == Task.DONE) nDone++;
        }
        pg.addView(Ui.tv(a, nAct + " aktif · " + nDone + " tamamlanan · toplam " + all.size(), 13, Ui.SUB, false));

        int sortSel = 0;
        try {
            sortSel = Integer.parseInt(Store.get(a, "tasksort", "0"));
        } catch (Exception e) {
            // varsayılan
        }
        final int fs = sortSel;
        LinearLayout sr = Ui.row(a);
        sr.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8));
        TextView sl = Ui.tv(a, "Sırala: ", 14, Ui.SUB, true);
        sr.addView(sl);
        Spinner sp = Ui.spin(a, SORTS, sortSel, i -> {
            Store.put(a, "tasksort", String.valueOf(i));
            show(a);
        });
        sr.addView(sp, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        pg.addView(sr);

        if (all.isEmpty()) {
            LinearLayout e = Ui.card(a);
            e.addView(Ui.tv(a, "Henüz görev yok", 17, Color.WHITE, true));
            e.addView(Ui.tv(a, "Sağ alttaki + tuşuna dokunup ilk görevini ekle. Görevler telefon kapansa da silinmez; "
                    + "uyarılar uygulama kapalıyken de çalar.", 14, Ui.SUB, false));
            Ui.add(pg, e);
        }

        String[] titles = {"Aktif", "Duraklatılan", "Süresi dolan", "Tamamlanan"};
        int[] order = {Task.ACTIVE, Task.PAUSED, Task.EXPIRED, Task.DONE};
        for (int k = 0; k < 4; k++) {
            List<Task> l = new ArrayList<>();
            for (Task t : all) if (t.status(now) == order[k]) l.add(t);
            if (l.isEmpty()) continue;
            final boolean doneSec = order[k] == Task.DONE;
            Collections.sort(l, new Comparator<Task>() {
                @Override
                public int compare(Task x, Task y) {
                    if (doneSec) return Long.compare(y.completedAt, x.completedAt);
                    if (fs == 1) return Long.compare(y.created, x.created);
                    if (fs == 2) return Integer.compare(y.done, x.done);
                    if (fs == 3) return x.title.compareToIgnoreCase(y.title);
                    return Long.compare(x.deadline, y.deadline);
                }
            });
            pg.addView(Ui.label(a, titles[k].toUpperCase() + " (" + l.size() + ")"));
            for (Task t : l) Ui.add(pg, taskCard(a, t, now));
        }
        a.showFab("+", () -> form(a, null));
    }

    static View taskCard(final MainActivity a, final Task t, long now) {
        LinearLayout c = Ui.card(a);
        LinearLayout top = Ui.row(a);
        ImageView iv = new ImageView(a);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (t.wp == -1) {
            iv.setImageBitmap(Notifier.thumb(a, -1, 120));
        } else {
            iv.setImageBitmap(Notifier.thumb(a, t.wp, 120));
        }
        top.addView(iv, new LinearLayout.LayoutParams(Ui.dp(a, 56), Ui.dp(a, 56)));
        LinearLayout tx = Ui.col(a);
        tx.setPadding(Ui.dp(a, 12), 0, 0, 0);
        int st = t.status(now);
        TextView title = Ui.tv(a, t.title, 17, Color.WHITE, true);
        if (st == Task.DONE) title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        tx.addView(title);
        String l2;
        if (st == Task.DONE) l2 = "✅ Tamamlandı: " + Ui.fmtShort(t.completedAt);
        else if (st == Task.EXPIRED) l2 = "⌛ Son tarih geçti: " + Ui.fmtShort(t.deadline);
        else l2 = "Son: " + Ui.fmtShort(t.deadline) + " · " + Ui.remain(t.deadline - now);
        tx.addView(Ui.tv(a, l2, 13, st == Task.EXPIRED ? Ui.BAD : st == Task.DONE ? Ui.OK : Ui.SUB, false));
        top.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(top);

        String area = t.area >= 0 && t.area < Areas.NAMES.length ? Areas.NAMES[t.area] : "Genel";
        TextView d = Ui.tv(a, area + " · " + Ui.every(t.everyMs) + " · " + t.windowText() + " · " + t.daysText()
                + "\n🔔 " + Sounds.NAMES[t.sound] + " · " + Sounds.VIB_NAMES[t.vib] + " · " + t.done + " uyarı gitti", 12.5f, Ui.SUB, false);
        d.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8));
        c.addView(d);

        LinearLayout r = Ui.row(a);
        final String id = t.id;
        if (st == Task.ACTIVE) {
            Ui.addW(r, Ui.btn(a, "✔ Tamamladım", 0xFF047857, v -> {
                update(a, id, x -> {
                    x.completed = true;
                    x.completedAt = System.currentTimeMillis();
                    x.active = false;
                    Scheduler.cancel(a, x);
                });
                a.toast("Tebrikler! 🎉");
                show(a);
            }));
            Ui.addW(r, Ui.btn(a, "⏸", Ui.CARD2, v -> {
                update(a, id, x -> {
                    x.active = false;
                    Scheduler.cancel(a, x);
                });
                show(a);
            }));
        } else if (st == Task.PAUSED) {
            Ui.addW(r, Ui.btn(a, "▶ Devam", Ui.ACC, v -> {
                update(a, id, x -> resume(a, x));
                show(a);
            }));
        } else if (st == Task.DONE) {
            Ui.addW(r, Ui.btn(a, "↩ Geri al", Ui.CARD2, v -> {
                update(a, id, x -> {
                    x.completed = false;
                    x.completedAt = 0;
                    if (x.deadline > System.currentTimeMillis()) resume(a, x);
                });
                show(a);
            }));
        }
        if (st != Task.DONE) Ui.addW(r, Ui.btn(a, "✎", Ui.CARD2, v -> form(a, t)));
        Ui.addW(r, Ui.btn(a, "🗑", 0xFF7F1D1D, v -> new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Silinsin mi?")
                .setMessage("\"" + t.title + "\" görevi kalıcı olarak silinecek.")
                .setPositiveButton("Sil", (dg, w) -> {
                    List<Task> ts = Store.tasks(a);
                    for (int i = 0; i < ts.size(); i++) {
                        if (ts.get(i).id.equals(id)) {
                            Scheduler.cancel(a, ts.get(i));
                            ts.remove(i);
                            break;
                        }
                    }
                    Store.saveTasks(a, ts);
                    show(a);
                })
                .setNegativeButton("Vazgeç", null).show()));
        c.addView(r);
        return c;
    }

    static void resume(MainActivity a, Task x) {
        long now = System.currentTimeMillis();
        x.active = true;
        x.next = Math.min(x.adjust(now + Math.max(60000L, x.everyMs)), x.deadline);
        Scheduler.schedule(a, x);
    }

    // ------------------------------------------------------------ ekleme / düzenleme formu

    static void form(final MainActivity a, final Task ex) {
        final LinearLayout pg = a.pageCol(ex == null ? "Yeni görev" : "Görevi düzenle", () -> show(a));
        final Calendar dl = Calendar.getInstance();
        final long nowT = System.currentTimeMillis();
        if (ex != null && ex.deadline > 0) {
            dl.setTimeInMillis(ex.deadline);
        } else {
            dl.add(Calendar.DAY_OF_YEAR, 7);
            dl.set(Calendar.HOUR_OF_DAY, 20);
            dl.set(Calendar.MINUTE, 0);
        }
        final int[] sel = {ex == null ? 0 : ex.area, ex == null ? 0 : ex.sound, ex == null ? 1 : ex.vib};
        final int[] unit = {1};
        final int[] hrs = {ex == null ? Areas.START[0] : ex.startHour, ex == null ? Areas.END[0] : ex.endHour};
        final int[] days = {ex == null ? 127 : ex.days};
        final int[] wpSel = {ex == null ? 0 : (ex.wp == -1 ? 0 : ex.wp == 100 ? Scenes.COUNT + 1 : ex.wp + 1)};

        pg.addView(Ui.label(a, "GÖREV"));
        final EditText title = Ui.et(a, "Ne yapacaksın? (örn. Analiz sınavına çalış)", false);
        if (ex != null) title.setText(ex.title);
        Ui.add(pg, title);

        pg.addView(Ui.label(a, "ALAN"));
        final EditText every = Ui.et(a, "kaç", false);
        every.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final Spinner unitSp = Ui.spin(a, new String[]{"dakikada bir", "saatte bir", "günde bir"}, 1, i -> unit[0] = i);
        final Spinner startSp = Ui.spin(a, hourNames(0, 23), hrs[0], i -> hrs[0] = i);
        final Spinner endSp = Ui.spin(a, hourNames(1, 24), hrs[1] - 1, i -> hrs[1] = i + 1);
        if (ex != null) {
            long m = ex.everyMs / 60000L;
            if (m % 1440 == 0) {
                unit[0] = 2;
                every.setText(String.valueOf(m / 1440));
            } else if (m % 60 == 0) {
                unit[0] = 1;
                every.setText(String.valueOf(m / 60));
            } else {
                unit[0] = 0;
                every.setText(String.valueOf(m));
            }
            unitSp.setSelection(unit[0]);
        } else {
            every.setText(String.valueOf(Areas.EVERY[0]));
            hrs[0] = Areas.START[0];
            hrs[1] = Areas.END[0];
        }
        final boolean isNew = ex == null;
        Spinner areaSp = Ui.spin(a, Areas.NAMES, sel[0], i -> {
            sel[0] = i;
            if (isNew) {
                every.setText(String.valueOf(Areas.EVERY[i]));
                unit[0] = Areas.UNIT[i];
                unitSp.setSelection(unit[0]);
                hrs[0] = Areas.START[i];
                hrs[1] = Areas.END[i];
                startSp.setSelection(hrs[0]);
                endSp.setSelection(hrs[1] - 1);
            }
        });
        Ui.add(pg, areaSp);
        pg.addView(Ui.tv(a, "Alan seçince önerilen uyarı aralığı ve saatler otomatik gelir; istersen değiştir.", 12, Ui.SUB, false));

        pg.addView(Ui.label(a, "SON TARİH VE SAAT"));
        LinearLayout dr = Ui.row(a);
        final android.widget.Button db = Ui.btn(a, "", Ui.CARD2, null);
        final android.widget.Button tb = Ui.btn(a, "", Ui.CARD2, null);
        final Runnable upd = () -> {
            db.setText("📅 " + new java.text.SimpleDateFormat("d MMM yyyy", new java.util.Locale("tr")).format(dl.getTime()));
            tb.setText("🕒 " + new java.text.SimpleDateFormat("HH:mm", new java.util.Locale("tr")).format(dl.getTime()));
        };
        upd.run();
        db.setOnClickListener(v -> new DatePickerDialog(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK, (p, y, m, d) -> {
            dl.set(y, m, d);
            upd.run();
        }, dl.get(Calendar.YEAR), dl.get(Calendar.MONTH), dl.get(Calendar.DAY_OF_MONTH)).show());
        tb.setOnClickListener(v -> new TimePickerDialog(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK, (p, h, m) -> {
            dl.set(Calendar.HOUR_OF_DAY, h);
            dl.set(Calendar.MINUTE, m);
            upd.run();
        }, dl.get(Calendar.HOUR_OF_DAY), dl.get(Calendar.MINUTE), true).show());
        Ui.addW(dr, db);
        Ui.addW(dr, tb);
        Ui.add(pg, dr);

        pg.addView(Ui.label(a, "UYARI SIKLIĞI"));
        LinearLayout er = Ui.row(a);
        Ui.addW(er, every);
        Ui.addW(er, unitSp);
        Ui.add(pg, er);

        pg.addView(Ui.label(a, "UYARININ ÇALABİLECEĞİ SAATLER"));
        LinearLayout hr = Ui.row(a);
        Ui.addW(hr, startSp);
        hr.addView(Ui.tv(a, "–", 16, Ui.SUB, true));
        Ui.addW(hr, endSp);
        Ui.add(pg, hr);

        pg.addView(Ui.label(a, "GÜNLER"));
        LinearLayout dayRow = Ui.row(a);
        String[] dn = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
        for (int i = 0; i < 7; i++) {
            final int bit = 1 << i;
            final TextView ch = Ui.chip(a, dn[i], (days[0] & bit) != 0, null);
            ch.setOnClickListener(v -> {
                days[0] ^= bit;
                if ((days[0] & 127) == 0) days[0] = bit;
                ch.setBackground(Ui.round(a, (days[0] & bit) != 0 ? Ui.ACC : Ui.CARD2, 20));
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.rightMargin = Ui.dp(a, 4);
            dayRow.addView(ch, lp);
        }
        Ui.add(pg, dayRow);

        pg.addView(Ui.label(a, "SES VE TİTREŞİM"));
        Ui.add(pg, Ui.spin(a, Sounds.NAMES, sel[1], i -> {
            sel[1] = i;
            Sounds.preview(a, i, -1);
        }));
        Ui.add(pg, Ui.spin(a, Sounds.VIB_NAMES, sel[2], i -> {
            sel[2] = i;
            Sounds.preview(a, -1, i);
        }));
        Ui.add(pg, Ui.btn(a, "▶ Seçili sesi ve titreşimi dene", Ui.CARD2, v -> Sounds.preview(a, sel[1], sel[2])));

        pg.addView(Ui.label(a, "ARKA PLAN (DUVAR KAĞIDI)"));
        final ImageView prev = new ImageView(a);
        prev.setScaleType(ImageView.ScaleType.CENTER_CROP);
        final Runnable updPrev = () -> {
            int wp = wpSel[0] == 0 ? -1 : wpSel[0] == Scenes.COUNT + 1 ? 100 : wpSel[0] - 1;
            prev.setImageBitmap(Notifier.thumb(a, wp, 140));
        };
        updPrev.run();
        String[] wn = new String[Scenes.COUNT + 2];
        wn[0] = "🎲 Rastgele (Ayarlar'a göre)";
        for (int i = 0; i < Scenes.COUNT; i++) wn[i + 1] = Scenes.NAMES[i];
        wn[Scenes.COUNT + 1] = "📁 Kendi dosyalarım";
        LinearLayout wr = Ui.row(a);
        wr.addView(prev, new LinearLayout.LayoutParams(Ui.dp(a, 64), Ui.dp(a, 64)));
        Spinner wsp = Ui.spin(a, wn, wpSel[0], i -> {
            wpSel[0] = i;
            updPrev.run();
        });
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wl.leftMargin = Ui.dp(a, 10);
        wr.addView(wsp, wl);
        Ui.add(pg, wr);

        Ui.add(pg, Ui.btn(a, ex == null ? "Görevi kaydet" : "Değişiklikleri kaydet", Ui.ACC, v -> {
            String ti = title.getText().toString().trim();
            if (ti.isEmpty()) {
                a.toast("Görev adını yaz.");
                return;
            }
            long n = System.currentTimeMillis();
            dl.set(Calendar.SECOND, 0);
            long deadline = dl.getTimeInMillis();
            if (deadline <= n) {
                a.toast("Son tarih ileri bir zaman olmalı.");
                return;
            }
            int val;
            try {
                val = Integer.parseInt(every.getText().toString().trim());
            } catch (Exception e) {
                val = 0;
            }
            if (val < 1) {
                a.toast("Uyarı sıklığı en az 1 olmalı.");
                return;
            }
            long ms = val * (unit[0] == 0 ? 60000L : unit[0] == 1 ? 3600000L : 86400000L);
            if (hrs[1] <= hrs[0]) {
                a.toast("Bitiş saati başlangıçtan sonra olmalı.");
                return;
            }
            List<Task> ts = Store.tasks(a);
            Task t = null;
            if (ex != null) {
                for (Task x : ts) if (x.id.equals(ex.id)) t = x;
            }
            boolean wasPaused = ex != null && !ex.active && ex.deadline > n && !ex.completed;
            if (t == null) {
                t = new Task();
                t.id = UUID.randomUUID().toString();
                t.created = n;
                ts.add(t);
            } else {
                Scheduler.cancel(a, t);
            }
            t.title = ti;
            t.area = sel[0];
            t.deadline = deadline;
            t.everyMs = ms;
            t.sound = sel[1];
            t.vib = sel[2];
            t.startHour = hrs[0];
            t.endHour = hrs[1];
            t.days = days[0];
            t.wp = wpSel[0] == 0 ? -1 : wpSel[0] == Scenes.COUNT + 1 ? 100 : wpSel[0] - 1;
            t.completed = false;
            t.active = !wasPaused;
            t.next = Math.min(t.adjust(n + ms), deadline);
            if (t.active) Scheduler.schedule(a, t);
            Store.saveTasks(a, ts);
            a.toast("Kaydedildi. İlk uyarı: " + Ui.fmtShort(t.next));
            show(a);
        }));
    }

    static String[] hourNames(int from, int to) {
        String[] s = new String[to - from + 1];
        for (int i = 0; i < s.length; i++) s[i] = String.format("%02d:00", from + i);
        return s;
    }
}
