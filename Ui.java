package com.koc.hatirlatici;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

/** Kod ile arayüz kurmak için küçük yardımcılar (XML yok). */
public class Ui {
    public static final int BG = 0xFF0F1020, CARD = 0xFF1B1D36, CARD2 = 0xFF2A2D52, TXT = 0xFFF2F3FF,
            SUB = 0xFFA3A7CC, ACC = 0xFF8B5CF6, ACC2 = 0xFFEC4899, OK = 0xFF34D399, WARN = 0xFFFBBF24,
            BAD = 0xFFF87171;

    public interface Sel {
        void on(int i);
    }

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static GradientDrawable round(Context c, int color, float rDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, rDp));
        return g;
    }

    public static TextView tv(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0, 1.12f);
        return t;
    }

    public static EditText et(Context c, String hint, boolean multi) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(SUB);
        e.setTextColor(Color.WHITE);
        e.setTextSize(15);
        e.setBackground(round(c, CARD2, 12));
        int p = dp(c, 12);
        e.setPadding(p, p, p, p);
        e.setHighlightColor(0x667C3AED);
        if (multi) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT);
            e.setSingleLine(true);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            GradientDrawable cur = new GradientDrawable();
            cur.setColor(Color.WHITE);
            cur.setSize(dp(c, 2), dp(c, 22));
            e.setTextCursorDrawable(cur);
        }
        return e;
    }

    public static Button btn(Context c, String s, int color, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setSingleLine(true);
        b.setBackground(round(c, color, 12));
        b.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        b.setMinHeight(dp(c, 44));
        b.setMinimumHeight(dp(c, 44));
        b.setOnClickListener(l);
        return b;
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = col(c);
        l.setBackground(round(c, CARD, 16));
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        return l;
    }

    public static void add(ViewGroup parent, View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(parent.getContext(), 10);
        parent.addView(v, lp);
    }

    /** Yan yana eşit genişlikte ekle (satır içinde). */
    public static void addW(LinearLayout row, View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(row.getContext(), 6);
        row.addView(v, lp);
    }

    public static TextView label(Context c, String s) {
        TextView t = tv(c, s, 13, SUB, true);
        t.setPadding(dp(c, 2), dp(c, 6), 0, dp(c, 4));
        return t;
    }

    public static TextView chip(Context c, String s, boolean sel, View.OnClickListener l) {
        TextView t = tv(c, s, 13, Color.WHITE, false);
        t.setBackground(round(c, sel ? ACC : CARD2, 20));
        t.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        t.setGravity(Gravity.CENTER);
        t.setOnClickListener(l);
        return t;
    }

    public static View bar(Context c, int pct, int color) {
        pct = Math.max(0, Math.min(100, pct));
        LinearLayout l = new LinearLayout(c);
        l.setBackground(round(c, CARD2, 6));
        View f = new View(c);
        f.setBackground(round(c, color, 6));
        View e = new View(c);
        l.addView(f, new LinearLayout.LayoutParams(0, dp(c, 8), Math.max(pct, 1)));
        l.addView(e, new LinearLayout.LayoutParams(0, dp(c, 8), Math.max(100 - pct, 1)));
        return l;
    }

    public static Switch toggle(Context c, String label, boolean on, final java.util.function.Consumer<Boolean> cb) {
        Switch s = new Switch(c);
        s.setText(label);
        s.setTextColor(Color.WHITE);
        s.setTextSize(15);
        s.setChecked(on);
        s.setPadding(0, dp(c, 8), 0, dp(c, 8));
        s.setOnCheckedChangeListener((v, b) -> cb.accept(b));
        return s;
    }

    /** Koyu zeminde okunur (beyaz yazı, ▾ işaretli) açılır liste. */
    public static Spinner spin(final Context c, final String[] items, int sel, final Sel cb) {
        final Spinner sp = new Spinner(c);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(c, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                TextView t = tv(c, items[pos] + "  ▾", 15, Color.WHITE, false);
                t.setPadding(dp(c, 12), dp(c, 12), dp(c, 12), dp(c, 12));
                return t;
            }

            @Override
            public View getDropDownView(int pos, View cv, ViewGroup parent) {
                TextView t = tv(c, items[pos], 15, Color.WHITE, false);
                t.setBackgroundColor(CARD2);
                t.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
                return t;
            }
        };
        sp.setAdapter(ad);
        sp.setBackground(round(c, CARD2, 12));
        sp.setPopupBackgroundDrawable(round(c, CARD2, 12));
        sp.setSelection(Math.max(0, Math.min(sel, items.length - 1)), false);
        if (cb != null) {
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                boolean first = true;

                @Override
                public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    if (first) {
                        first = false;
                        return;
                    }
                    cb.on(pos);
                }

                @Override
                public void onNothingSelected(AdapterView<?> p) {
                }
            });
        }
        return sp;
    }

    static final HashSet<String> OPEN = new HashSet<>();

    /** Dokununca açılıp kapanan bölüm. İçerik kutusunu döndürür. */
    public static LinearLayout section(final Context c, LinearLayout parent, final String key, String title, String sub) {
        LinearLayout box = card(c);
        LinearLayout head = row(c);
        LinearLayout txt = col(c);
        txt.addView(tv(c, title, 17, Color.WHITE, true));
        if (sub != null && !sub.isEmpty()) txt.addView(tv(c, sub, 12, SUB, false));
        head.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView arrow = tv(c, OPEN.contains(key) ? "▴" : "▾", 20, SUB, true);
        head.addView(arrow);
        box.addView(head);
        final LinearLayout body = col(c);
        body.setPadding(0, dp(c, 10), 0, 0);
        body.setVisibility(OPEN.contains(key) ? View.VISIBLE : View.GONE);
        box.addView(body);
        head.setOnClickListener(v -> {
            boolean open = body.getVisibility() == View.VISIBLE;
            body.setVisibility(open ? View.GONE : View.VISIBLE);
            arrow.setText(open ? "▾" : "▴");
            if (open) OPEN.remove(key);
            else OPEN.add(key);
        });
        add(parent, box);
        return body;
    }

    // ------------------------------------------------------------ biçim

    public static String fmt(long t) {
        return new SimpleDateFormat("d MMM yyyy HH:mm", new Locale("tr")).format(new Date(t));
    }

    public static String fmtShort(long t) {
        return new SimpleDateFormat("d MMM HH:mm", new Locale("tr")).format(new Date(t));
    }

    public static String remain(long ms) {
        if (ms <= 0) return "süre doldu";
        long m = ms / 60000;
        if (m < 60) return Math.max(1, m) + " dk kaldı";
        long h = m / 60;
        if (h < 48) return h + " sa kaldı";
        return (h / 24) + " gün kaldı";
    }

    public static String every(long ms) {
        if (ms % 86400000L == 0) return (ms / 86400000L) + " günde bir";
        if (ms % 3600000L == 0) return (ms / 3600000L) + " saatte bir";
        return Math.max(1, ms / 60000L) + " dakikada bir";
    }
}
