package com.koc.hatirlatici;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Duvar kağıdını kodla çizer: internet ve görsel dosyası gerekmez, her seferinde farklı çıkar. */
public class Wallpaper {

    static String[][] palettes(char cat) {
        switch (cat) {
            case 'z':
                return new String[][]{
                        {"#0f172a", "#1e3a8a", "#f59e0b"},
                        {"#1f2937", "#4c1d95", "#fb7185"},
                        {"#0c4a6e", "#1e1b4b", "#fde68a"}};
            case 'd':
                return new String[][]{
                        {"#052e16", "#065f46", "#fbbf24"},
                        {"#111827", "#1d4ed8", "#22d3ee"},
                        {"#3f0d12", "#9f1239", "#fecaca"}};
            case 'u':
                return new String[][]{
                        {"#fb923c", "#ec4899", "#fef3c7"},
                        {"#0ea5e9", "#6366f1", "#fde68a"},
                        {"#f43f5e", "#f59e0b", "#fff7ed"}};
            default:
                return new String[][]{
                        {"#0f766e", "#2563eb", "#fef08a"},
                        {"#7c3aed", "#db2777", "#fde68a"},
                        {"#064e3b", "#0891b2", "#e0f2fe"}};
        }
    }

    /** text null ise sadece resim (küçük ikon için). */
    public static Bitmap make(char cat, int w, int h, String text, long seed) {
        Random r = new Random(seed);
        String[][] pals = palettes(cat);
        String[] pc = pals[r.nextInt(pals.length)];
        int c0 = Color.parseColor(pc[0]);
        int c1 = Color.parseColor(pc[1]);
        int c2 = Color.parseColor(pc[2]);

        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float s = w / 1080f;

        p.setShader(new LinearGradient(0, 0, r.nextBoolean() ? w : 0, h, c0, c1, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);
        p.setShader(null);

        int shape = r.nextInt(4);
        if (shape == 0) {
            p.setColor(Color.WHITE);
            for (int i = 0; i < 60; i++) {
                p.setAlpha(60 + r.nextInt(180));
                cv.drawCircle(r.nextFloat() * w, r.nextFloat() * h * 0.8f, (1 + r.nextFloat() * 3) * Math.max(s, 0.6f), p);
            }
            p.setColor(c2);
            p.setAlpha(235);
            cv.drawCircle(w * (0.2f + r.nextFloat() * 0.6f), h * (0.15f + r.nextFloat() * 0.2f), w * 0.09f, p);
        } else if (shape == 1) {
            p.setColor(c2);
            p.setAlpha(220);
            cv.drawCircle(w * (0.25f + r.nextFloat() * 0.5f), h * (0.3f + r.nextFloat() * 0.15f), w * 0.2f, p);
            for (int i = 0; i < 6; i++) {
                p.setAlpha((int) (r.nextFloat() * 50));
                cv.drawCircle(r.nextFloat() * w, r.nextFloat() * h, w * (0.04f + r.nextFloat() * 0.12f), p);
            }
        } else if (shape == 2) {
            p.setColor(c2);
            for (int k = 0; k < 4; k++) {
                float y = h * (0.5f + k * 0.13f);
                float a = h * (0.03f + r.nextFloat() * 0.04f);
                Path path = new Path();
                path.moveTo(0, y);
                path.quadTo(w * 0.25f, y - a * 2, w * 0.5f, y);
                path.quadTo(w * 0.75f, y + a * 2, w, y);
                path.lineTo(w, h);
                path.lineTo(0, h);
                path.close();
                p.setAlpha(30 + k * 25);
                cv.drawPath(path, p);
            }
        } else {
            p.setColor(c2);
            p.setAlpha(235);
            cv.drawCircle(w * (0.2f + r.nextFloat() * 0.6f), h * 0.25f, w * 0.1f, p);
            p.setColor(Color.BLACK);
            for (int k = 0; k < 3; k++) {
                float base = h * (0.62f + k * 0.1f);
                Path path = new Path();
                path.moveTo(0, h);
                for (int i = 0; i <= 6; i++) {
                    path.lineTo(w * i / 6f, base - r.nextFloat() * h * (0.14f - k * 0.03f));
                }
                path.lineTo(w, h);
                path.close();
                p.setAlpha(45 + k * 45);
                cv.drawPath(path, p);
            }
        }

        if (text != null && !text.isEmpty()) drawText(cv, text, w, h);
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

    static void drawText(Canvas cv, String text, int w, int h) {
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.WHITE);
        tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        tp.setTextAlign(Paint.Align.CENTER);
        float size = w / 15f;
        float maxW = w * 0.78f;
        List<String> lines;
        float lh;
        float bh;
        while (true) {
            tp.setTextSize(size);
            lines = wrap(tp, text, maxW);
            lh = size * 1.3f;
            bh = lines.size() * lh + w * 0.08f;
            if (bh <= h * 0.86f || size < 14f) break;
            size *= 0.92f;
        }
        float top = (h - bh) / 2f;
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setColor(0x66000000);
        cv.drawRoundRect(new RectF(w * 0.07f, top, w * 0.93f, top + bh), 36f, 36f, bp);
        for (int i = 0; i < lines.size(); i++) {
            cv.drawText(lines.get(i), w / 2f, top + w * 0.04f + lh * (i + 0.8f), tp);
        }
    }
}
