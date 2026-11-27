package com.koc.hatirlatici;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sosyal medya tarzı duvar kağıdı: koyu degrade + parlayan neon "aurora" lekeleri,
 * cam halkalar, film greni, vinyet ve büyük kalın yazı. Hepsi kodla çizilir (internet gerekmez).
 */
public class Wallpaper {

    // {üst renk, alt renk, parlama 1, parlama 2, parlama 3}
    static final int[][][] PAL = {
            { // z: zor zamanlar -> derin lacivert/mor + umut veren sıcak parlamalar
                    {0xFF0B1026, 0xFF2A0F4D, 0xFFFFB347, 0xFFFF5E78, 0xFF6C5CE7},
                    {0xFF0A0F1F, 0xFF13294B, 0xFF00C2FF, 0xFF7B61FF, 0xFFFFC857}
            },
            { // d: disiplin -> neon siyah
                    {0xFF05070F, 0xFF0B1B3A, 0xFF00F5D4, 0xFF4361EE, 0xFFF72585},
                    {0xFF0D0D0D, 0xFF1A1A2E, 0xFFE94560, 0xFF0F3460, 0xFFFFD369}
            },
            { // u: umut -> gün batımı
                    {0xFF1B0B3B, 0xFF5C1A4F, 0xFFFF6B6B, 0xFFFFB86B, 0xFFFFE66D},
                    {0xFF0F2027, 0xFF2C5364, 0xFFFFA17F, 0xFFFF5E62, 0xFFF9D423}
            },
            { // b: başlangıç -> aurora
                    {0xFF03120E, 0xFF0B3B2E, 0xFF00FFA3, 0xFF00B4D8, 0xFFB5FF6B},
                    {0xFF0A1128, 0xFF1B3A4B, 0xFF72EFDD, 0xFF7400B8, 0xFFFFD60A}
            }
    };

    public static int[] palette(char cat, Random r) {
        int k = cat == 'z' ? 0 : cat == 'd' ? 1 : cat == 'u' ? 2 : 3;
        int[][] l = PAL[k];
        return l[r.nextInt(l.length)];
    }

    /** Yazısız arka plan. */
    public static Bitmap background(char cat, int w, int h, long seed) {
        Random r = new Random(seed);
        int[] pal = palette(cat, r);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        boolean diag = r.nextBoolean();
        p.setShader(new LinearGradient(0, 0, diag ? w : 0, h, pal[0], pal[1], Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);
        p.setShader(null);

        int n = 4 + r.nextInt(3);
        for (int i = 0; i < n; i++) {
            float cx = r.nextFloat() * w;
            float cy = r.nextFloat() * h;
            float rad = Math.max(w, h) * (0.28f + r.nextFloat() * 0.35f);
            int col = pal[2 + i % 3];
            p.setShader(new RadialGradient(cx, cy, rad, (col & 0x00FFFFFF) | 0xB4000000,
                    col & 0x00FFFFFF, Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, rad, p);
        }
        p.setShader(null);

        // cam halkalar
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, w / 360f));
        p.setColor(Color.WHITE);
        for (int i = 0; i < 3; i++) {
            p.setAlpha(18 + r.nextInt(30));
            cv.drawCircle(r.nextFloat() * w, r.nextFloat() * h, w * (0.12f + r.nextFloat() * 0.25f), p);
        }
        p.setStyle(Paint.Style.FILL);

        // film greni
        int dots = Math.min(w * h / 250, 6000);
        for (int i = 0; i < dots; i++) {
            p.setColor(r.nextBoolean() ? Color.WHITE : Color.BLACK);
            p.setAlpha(10 + r.nextInt(22));
            float x = r.nextFloat() * w;
            float y = r.nextFloat() * h;
            cv.drawRect(x, y, x + 1.5f, y + 1.5f, p);
        }

        // vinyet
        p.setAlpha(255);
        p.setColor(Color.BLACK);
        p.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.78f,
                0x00000000, 0xB0000000, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);
        p.setShader(null);
        return bm;
    }

    static List<String> wrap(Paint tp, String text, float maxW) {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : text.split(" ")) {
            String t = line.isEmpty() ? word : line + " " + word;
            if (tp.measureText(t) > maxW && !line.isEmpty()) {
                lines.add(line);
                line = word;
            } else {
                line = t;
            }
        }
        if (!line.isEmpty()) lines.add(line);
        return lines;
    }

    /** Saydam katman: hafif karartma + büyük tırnak işareti + kalın söz + ince çizgi + yazar. */
    public static Bitmap quoteLayer(String text, String author, int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, 0, h, 0x55000000, 0x99000000, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);
        p.setShader(null);

        boolean hasAuthor = author != null && !author.isEmpty();
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.WHITE);
        tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        tp.setTextAlign(Paint.Align.CENTER);

        float size = w / 13f;
        float maxW = w * 0.82f;
        List<String> lines;
        float lh;
        float bh;
        while (true) {
            tp.setTextSize(size);
            lines = wrap(tp, text, maxW);
            lh = size * 1.32f;
            bh = size * 1.5f + lines.size() * lh + size * 0.9f + (hasAuthor ? size * 0.9f : 0f);
            if (bh <= h * 0.74f || size < 14f) break;
            size *= 0.92f;
        }
        tp.setShadowLayer(size * 0.28f, 0, size * 0.06f, 0xAA000000);

        float top = (h - bh) / 2f;
        Paint qp = new Paint(Paint.ANTI_ALIAS_FLAG);
        qp.setColor(0xFFFFD166);
        qp.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        qp.setTextAlign(Paint.Align.CENTER);
        qp.setTextSize(size * 2.4f);
        cv.drawText("“", w / 2f, top + size * 2.0f, qp);

        float y = top + size * 1.5f;
        for (int i = 0; i < lines.size(); i++) {
            cv.drawText(lines.get(i), w / 2f, y + lh * (i + 0.85f), tp);
        }
        y += lines.size() * lh + size * 0.35f;

        Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setColor(0xFFFFD166);
        float bw = w * 0.14f;
        float bt = Math.max(3f, size * 0.08f);
        cv.drawRoundRect(new RectF(w / 2f - bw / 2f, y, w / 2f + bw / 2f, y + bt), bt, bt, lp);

        if (hasAuthor) {
            Paint ap = new Paint(Paint.ANTI_ALIAS_FLAG);
            ap.setColor(0xDDFFFFFF);
            ap.setTextAlign(Paint.Align.CENTER);
            ap.setTextSize(size * 0.52f);
            ap.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            cv.drawText("— " + author, w / 2f, y + bt + size * 0.8f, ap);
        }
        return bm;
    }

    /** Arka plan resmini (merkezden kırparak) yerleştirir, istenirse üstüne sözü çizer. */
    public static Bitmap compose(Bitmap bg, String text, String author, int w, int h) {
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        if (bg != null) {
            float sc = Math.max((float) w / bg.getWidth(), (float) h / bg.getHeight());
            float dx = (w - bg.getWidth() * sc) / 2f;
            float dy = (h - bg.getHeight() * sc) / 2f;
            Matrix m = new Matrix();
            m.postScale(sc, sc);
            m.postTranslate(dx, dy);
            cv.drawBitmap(bg, m, new Paint(Paint.FILTER_BITMAP_FLAG));
        }
        if (text != null && !text.isEmpty()) {
            Bitmap layer = quoteLayer(text, author, w, h);
            cv.drawBitmap(layer, 0, 0, null);
            layer.recycle();
        }
        return out;
    }
}
