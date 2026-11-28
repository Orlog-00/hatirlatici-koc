package com.koc.hatirlatici;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.Random;

/** 10 farklı duvar kağıdı teması (hepsi kodla çizilir). Hareketli sürümü AnimatedBackground'dadır. */
public class Scenes {
    public static final String[] NAMES = {
            "Aurora", "Gece Şehri", "Gün Batımı Dağları", "Okyanus", "Galaksi",
            "Orman Sisi", "Çöl", "Neon Yağmur", "Sakura", "Neon Geometri",
            "Uçurumu Aş", "Zirveye Tırmanış", "Disiplin (siyah-beyaz)"
    };
    public static final int COUNT = NAMES.length;
    public static final float OCEAN_HZ = 0.58f;

    public static final int FX_BLOBS = 0, FX_STARS = 1, FX_SHOOT = 2, FX_CLOUDS = 3, FX_WAVES = 4,
            FX_GLINT = 5, FX_FIREFLY = 6, FX_SAND = 7, FX_RAIN = 8, FX_PETALS = 9, FX_SHAPES = 10;
    public static final int[][] FX = {
            {FX_BLOBS}, {FX_STARS, FX_SHOOT}, {FX_CLOUDS}, {FX_WAVES, FX_GLINT}, {FX_STARS, FX_SHOOT},
            {FX_FIREFLY, FX_CLOUDS}, {FX_SAND, FX_CLOUDS}, {FX_RAIN}, {FX_PETALS}, {FX_SHAPES},
            {FX_CLOUDS}, {FX_CLOUDS, FX_SAND}, {FX_STARS}
    };

    public static int themeFor(int theme, long seed) {
        if (theme >= 0 && theme < COUNT) return theme;
        return (int) ((seed & 0x7fffffffL) % COUNT);
    }

    public static Bitmap scene(int theme, char cat, int w, int h, long seed) {
        int th = themeFor(theme, seed);
        if (th == 0) return Wallpaper.background(cat, w, h, seed);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        Random r = new Random(seed);
        switch (th) {
            case 1: nightCity(cv, w, h, r); break;
            case 2: sunsetMountains(cv, w, h, r); break;
            case 3: ocean(cv, w, h, r); break;
            case 4: galaxy(cv, w, h, r); break;
            case 5: forest(cv, w, h, r); break;
            case 6: desert(cv, w, h, r); break;
            case 7: neonRain(cv, w, h, r); break;
            case 8: sakura(cv, w, h, r); break;
            case 9: neonGeo(cv, w, h, r); break;
            case 10: cliffJump(cv, w, h, r); break;
            case 11: boulder(cv, w, h, r); break;
            default: discipline(cv, w, h, r); break;
        }
        Paint p = paint();
        p.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.8f, 0x00000000, 0x99000000,
                Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);
        return bm;
    }

    // ------------------------------------------------------------ yardımcılar

    static Paint paint() {
        return new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    static void sky(Canvas cv, int w, int h, float bottomFrac, int... cols) {
        float[] pos = new float[cols.length];
        for (int i = 0; i < cols.length; i++) pos[i] = i / (float) (cols.length - 1);
        Paint p = paint();
        p.setShader(new LinearGradient(0, 0, 0, h * bottomFrac, cols, pos, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);
    }

    static void glow(Canvas cv, float x, float y, float rad, int col, int alpha) {
        Paint p = paint();
        p.setShader(new RadialGradient(x, y, rad, (col & 0x00FFFFFF) | (alpha << 24), col & 0x00FFFFFF,
                Shader.TileMode.CLAMP));
        cv.drawCircle(x, y, rad, p);
    }

    static void disc(Canvas cv, float x, float y, float rad, int col) {
        Paint p = paint();
        p.setColor(col);
        cv.drawCircle(x, y, rad, p);
    }

    static void stars(Canvas cv, int w, int h, Random r, int n, float maxYFrac) {
        Paint p = paint();
        p.setColor(0xFFFFFFFF);
        float sc = Math.max(w / 540f, 0.6f);
        for (int i = 0; i < n; i++) {
            p.setAlpha(70 + r.nextInt(150));
            cv.drawCircle(r.nextFloat() * w, r.nextFloat() * h * maxYFrac, (0.6f + r.nextFloat() * 1.6f) * sc, p);
        }
    }

    static void ridge(Canvas cv, int w, int h, Random r, float baseY, float amp, int col, int steps) {
        Path path = new Path();
        path.moveTo(0, h);
        for (int i = 0; i <= steps; i++) {
            path.lineTo(w * i / (float) steps, baseY - r.nextFloat() * amp);
        }
        path.lineTo(w, h);
        path.close();
        Paint p = paint();
        p.setColor(col);
        cv.drawPath(path, p);
    }

    static void skyline(Canvas cv, int w, int h, Random r, float baseY, int col, float maxH, int winCol) {
        Paint p = paint();
        Paint wp = paint();
        wp.setColor(winCol);
        float x = -w * 0.02f;
        while (x < w) {
            float bw = w * (0.05f + r.nextFloat() * 0.07f);
            float bh = maxH * (0.3f + r.nextFloat() * 0.7f);
            p.setColor(col);
            cv.drawRect(x, baseY - bh, x + bw, h, p);
            if (winCol != 0) {
                for (float wy = baseY - bh + bw * 0.15f; wy < baseY - bw * 0.2f; wy += bw * 0.22f) {
                    for (float wx = x + bw * 0.12f; wx < x + bw - bw * 0.15f; wx += bw * 0.2f) {
                        if (r.nextFloat() < 0.35f) {
                            wp.setAlpha(120 + r.nextInt(135));
                            cv.drawRect(wx, wy, wx + bw * 0.09f, wy + bw * 0.12f, wp);
                        }
                    }
                }
            }
            x += bw * (0.9f + r.nextFloat() * 0.2f);
        }
    }

    static void pines(Canvas cv, int w, int h, Random r, float baseY, int col, float maxH) {
        Paint p = paint();
        p.setColor(col);
        float x = -w * 0.03f;
        while (x < w * 1.03f) {
            float tw = w * (0.06f + r.nextFloat() * 0.07f);
            float th = maxH * (0.55f + r.nextFloat() * 0.45f);
            Path t = new Path();
            t.moveTo(x, baseY);
            t.lineTo(x + tw / 2f, baseY - th);
            t.lineTo(x + tw, baseY);
            t.close();
            cv.drawPath(t, p);
            x += tw * 0.62f;
        }
        cv.drawRect(0, baseY - 2, w, h, p);
    }

    static void fog(Canvas cv, int w, float y, float hh, int alpha) {
        Paint p = paint();
        p.setShader(new LinearGradient(0, y, 0, y + hh, new int[]{0x00FFFFFF, (alpha << 24) | 0x00FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRect(0, y, w, y + hh, p);
    }

    static void dunes(Canvas cv, int w, int h, Random r, float baseY, float amp, int col) {
        Path path = new Path();
        path.moveTo(0, h);
        path.lineTo(0, baseY);
        float ph = r.nextFloat() * 6.28f;
        float fr = 1.2f + r.nextFloat() * 1.2f;
        for (int x = 0; x <= w; x += Math.max(6, w / 90)) {
            path.lineTo(x, baseY + amp * (float) Math.sin(x * fr * 6.28f / w + ph));
        }
        path.lineTo(w, h);
        path.close();
        Paint p = paint();
        p.setColor(col);
        cv.drawPath(path, p);
    }

    /** Okyanus dalgaları: hem sabit resimde hem animasyonda (faz = zaman) kullanılır. */
    public static void waves(Canvas cv, int w, int h, float hz, float phase, int baseCol, int alphaBase) {
        Paint p = paint();
        float sc = w / 540f;
        for (int k = 0; k < 3; k++) {
            float y = hz + (h - hz) * (0.18f + k * 0.28f);
            float amp = h * (0.008f + k * 0.006f);
            Path path = new Path();
            path.moveTo(0, h);
            path.lineTo(0, y);
            for (int x = 0; x <= w; x += Math.max(8, w / 60)) {
                path.lineTo(x, y + amp * (float) Math.sin(x * (0.012f + k * 0.004f) / sc + phase * (1f + k * 0.4f) + k));
            }
            path.lineTo(w, h);
            path.close();
            p.setColor(baseCol);
            p.setAlpha(alphaBase + k * 35);
            cv.drawPath(path, p);
        }
    }

    static float qp(float a, float b, float c, float t) {
        float u = 1f - t;
        return u * u * a + 2f * u * t * b + t * t * c;
    }

    // ------------------------------------------------------------ temalar

    static void nightCity(Canvas cv, int w, int h, Random r) {
        sky(cv, w, h, 1f, 0xFF140A33, 0xFF3B1569, 0xFFC4457A, 0xFFFFA77A);
        stars(cv, w, h, r, 70, 0.5f);
        glow(cv, w * 0.72f, h * 0.2f, w * 0.45f, 0xFFFFE6B3, 70);
        disc(cv, w * 0.72f, h * 0.2f, w * 0.075f, 0xFFFFF3D6);
        skyline(cv, w, h, r, h * 0.80f, 0xFF3A1B5C, h * 0.22f, 0);
        skyline(cv, w, h, r, h * 0.90f, 0xFF120826, h * 0.28f, 0xFFFFE08A);
    }

    static void sunsetMountains(Canvas cv, int w, int h, Random r) {
        sky(cv, w, h, 1f, 0xFF1E1B4B, 0xFF7C3AED, 0xFFFB7185, 0xFFFDBA74);
        float sx = w * (0.35f + r.nextFloat() * 0.3f);
        float sy = h * 0.58f;
        glow(cv, sx, sy, w * 0.75f, 0xFFFFD27D, 200);
        disc(cv, sx, sy, w * 0.11f, 0xFFFFF1B8);
        ridge(cv, w, h, r, h * 0.66f, h * 0.10f, 0xFF6B2D8C, 9);
        ridge(cv, w, h, r, h * 0.76f, h * 0.12f, 0xFF3F1B63, 8);
        ridge(cv, w, h, r, h * 0.88f, h * 0.13f, 0xFF1A0B2E, 7);
    }

    static void ocean(Canvas cv, int w, int h, Random r) {
        float hz = h * OCEAN_HZ;
        sky(cv, w, h, OCEAN_HZ, 0xFF0F2557, 0xFF6D3B8E, 0xFFFF7A59, 0xFFFFD29D);
        float sx = w * 0.5f;
        glow(cv, sx, hz - w * 0.02f, w * 0.7f, 0xFFFFD27D, 190);
        disc(cv, sx, hz - w * 0.02f, w * 0.1f, 0xFFFFF1B8);
        Paint p = paint();
        p.setShader(new LinearGradient(0, hz, 0, h, 0xFF7A4B6E, 0xFF031A33, Shader.TileMode.CLAMP));
        cv.drawRect(0, hz, w, h, p);
        p.setShader(null);
        p.setColor(0xFFFFE1A8);
        for (int k = 0; k < 24; k++) {
            float y = hz + k * (h - hz) / 26f;
            float bw = w * 0.14f * (1f + k * 0.08f);
            p.setAlpha(Math.max(10, 150 - k * 6));
            cv.drawRect(sx - bw / 2f, y, sx + bw / 2f, y + Math.max(2f, h * 0.004f), p);
        }
        waves(cv, w, h, hz, 0f, 0xFF04223F, 60);
    }

    static void galaxy(Canvas cv, int w, int h, Random r) {
        sky(cv, w, h, 1f, 0xFF050014, 0xFF1B0B3B, 0xFF2E1065);
        glow(cv, w * 0.25f, h * 0.3f, w * 0.7f, 0xFFD946EF, 90);
        glow(cv, w * 0.8f, h * 0.6f, w * 0.8f, 0xFF22D3EE, 70);
        glow(cv, w * 0.5f, h * 0.85f, w * 0.9f, 0xFF7C3AED, 90);
        stars(cv, w, h, r, 160, 1f);
        float px = w * 0.72f;
        float py = h * 0.24f;
        float pr = w * 0.13f;
        Paint p = paint();
        p.setShader(new RadialGradient(px - pr * 0.3f, py - pr * 0.3f, pr * 1.4f, 0xFFFFD6A5, 0xFF8B3A62,
                Shader.TileMode.CLAMP));
        cv.drawCircle(px, py, pr, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(pr * 0.12f);
        p.setColor(0xAAFFE7C2);
        cv.save();
        cv.rotate(-22, px, py);
        cv.drawOval(new RectF(px - pr * 1.9f, py - pr * 0.45f, px + pr * 1.9f, py + pr * 0.45f), p);
        cv.restore();
    }

    static void forest(Canvas cv, int w, int h, Random r) {
        sky(cv, w, h, 1f, 0xFF0A2E2A, 0xFF1B6B52, 0xFFB6F2D0);
        glow(cv, w * 0.5f, h * 0.35f, w * 0.8f, 0xFFEFFFD0, 90);
        pines(cv, w, h, r, h * 0.60f, 0xFF2F8F6B, h * 0.18f);
        fog(cv, w, h * 0.50f, h * 0.18f, 120);
        pines(cv, w, h, r, h * 0.72f, 0xFF146B50, h * 0.21f);
        fog(cv, w, h * 0.62f, h * 0.18f, 110);
        pines(cv, w, h, r, h * 0.86f, 0xFF0B3B2E, h * 0.24f);
        pines(cv, w, h, r, h * 1.0f, 0xFF04180F, h * 0.26f);
    }

    static void desert(Canvas cv, int w, int h, Random r) {
        sky(cv, w, h, 1f, 0xFF2A1450, 0xFFB8326A, 0xFFFF8A5C, 0xFFFFD08A);
        glow(cv, w * 0.65f, h * 0.5f, w * 0.7f, 0xFFFFD08A, 190);
        disc(cv, w * 0.65f, h * 0.5f, w * 0.1f, 0xFFFFF0C4);
        dunes(cv, w, h, r, h * 0.62f, h * 0.05f, 0xFFC2603A);
        dunes(cv, w, h, r, h * 0.74f, h * 0.06f, 0xFF8E3B22);
        dunes(cv, w, h, r, h * 0.88f, h * 0.05f, 0xFF4B1D14);
    }

    static void neonRain(Canvas cv, int w, int h, Random r) {
        sky(cv, w, h, 1f, 0xFF050B1F, 0xFF0B1F4D, 0xFF2A1060);
        glow(cv, w * 0.25f, h * 0.6f, w * 0.6f, 0xFFFF2BD6, 90);
        glow(cv, w * 0.8f, h * 0.55f, w * 0.6f, 0xFF22D3EE, 90);
        skyline(cv, w, h, r, h * 0.74f, 0xFF0B1230, h * 0.30f, 0);
        skyline(cv, w, h, r, h * 0.86f, 0xFF050816, h * 0.34f, 0xFF67E8F9);
        Paint p = paint();
        p.setShader(new LinearGradient(0, h * 0.86f, 0, h, 0xFF0A0F2A, 0xFF05060F, Shader.TileMode.CLAMP));
        cv.drawRect(0, h * 0.86f, w, h, p);
        int[] neon = {0xFFFF2BD6, 0xFF22D3EE};
        for (int i = 0; i < 14; i++) {
            float x = r.nextFloat() * w;
            int col = neon[r.nextInt(2)];
            p.setShader(new LinearGradient(0, h * 0.86f, 0, h, (col & 0x00FFFFFF) | 0x99000000,
                    col & 0x00FFFFFF, Shader.TileMode.CLAMP));
            cv.drawRect(x, h * 0.86f, x + w * 0.012f, h, p);
        }
    }

    static void sakura(Canvas cv, int w, int h, Random r) {
        sky(cv, w, h, 1f, 0xFFEFA9C9, 0xFFFFD5C2, 0xFFC7B6F2);
        glow(cv, w * 0.7f, h * 0.25f, w * 0.5f, 0xFFFFFFFF, 140);
        disc(cv, w * 0.7f, h * 0.25f, w * 0.14f, 0xFFFFF7F0);
        ridge(cv, w, h, r, h * 0.78f, h * 0.10f, 0xFF9D7FC9, 8);
        ridge(cv, w, h, r, h * 0.90f, h * 0.10f, 0xFF6D4FA0, 7);
        Paint p = paint();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w * 0.012f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(0xFF3B1F2B);
        Path br = new Path();
        br.moveTo(-w * 0.05f, h * 0.10f);
        br.quadTo(w * 0.25f, h * 0.02f, w * 0.55f, h * 0.20f);
        br.quadTo(w * 0.70f, h * 0.28f, w * 0.95f, h * 0.26f);
        cv.drawPath(br, p);
        p.setStyle(Paint.Style.FILL);
        int[] pink = {0xFFFFC1D6, 0xFFFFA6C1, 0xFFFFE0EA};
        for (int i = 0; i < 80; i++) {
            float t = r.nextFloat();
            float x;
            float y;
            if (r.nextBoolean()) {
                x = qp(-w * 0.05f, w * 0.25f, w * 0.55f, t);
                y = qp(h * 0.10f, h * 0.02f, h * 0.20f, t);
            } else {
                x = qp(w * 0.55f, w * 0.70f, w * 0.95f, t);
                y = qp(h * 0.20f, h * 0.28f, h * 0.26f, t);
            }
            x += (r.nextFloat() - 0.5f) * w * 0.12f;
            y += (r.nextFloat() - 0.5f) * h * 0.06f;
            p.setColor(pink[r.nextInt(3)]);
            p.setAlpha(200 + r.nextInt(55));
            cv.drawCircle(x, y, w * (0.012f + r.nextFloat() * 0.02f), p);
        }
    }

    static void neonGeo(Canvas cv, int w, int h, Random r) {
        sky(cv, w, h, 1f, 0xFF0A0A1F, 0xFF1B0B3B);
        glow(cv, w * 0.3f, h * 0.3f, w * 0.7f, 0xFF7C3AED, 110);
        glow(cv, w * 0.8f, h * 0.7f, w * 0.7f, 0xFF06B6D4, 100);
        int[] neon = {0xFFFF2BD6, 0xFF22D3EE, 0xFFFDE047, 0xFF7C3AED};
        Paint p = paint();
        p.setStyle(Paint.Style.STROKE);
        // perspektif ızgara
        float hz = h * 0.72f;
        p.setColor(0xFFFF2BD6);
        p.setAlpha(110);
        p.setStrokeWidth(Math.max(1.5f, w * 0.003f));
        for (int k = -8; k <= 8; k++) {
            cv.drawLine(w / 2f + k * w * 0.09f, h, w / 2f + k * w * 0.012f, hz, p);
        }
        for (int k = 0; k < 8; k++) {
            float y = hz + (h - hz) * (float) Math.pow(k / 8f, 1.8f);
            cv.drawLine(0, y, w, y, p);
        }
        // neon şekiller (iki geçiş: geniş soluk + ince parlak)
        for (int i = 0; i < 12; i++) {
            float cx = r.nextFloat() * w;
            float cy = r.nextFloat() * h * 0.66f;
            float s = w * (0.06f + r.nextFloat() * 0.16f);
            int col = neon[r.nextInt(4)];
            int type = r.nextInt(3);
            for (int pass = 0; pass < 2; pass++) {
                p.setColor(col);
                p.setAlpha(pass == 0 ? 60 : 220);
                p.setStrokeWidth(pass == 0 ? w * 0.014f : w * 0.004f);
                if (type == 0) {
                    cv.drawCircle(cx, cy, s, p);
                } else if (type == 1) {
                    cv.drawRect(cx - s, cy - s, cx + s, cy + s, p);
                } else {
                    Path t = new Path();
                    t.moveTo(cx, cy - s);
                    t.lineTo(cx + s, cy + s);
                    t.lineTo(cx - s, cy + s);
                    t.close();
                    cv.drawPath(t, p);
                }
            }
        }
    }

    // ------------------------------------------------------------ motivasyon silüetleri

    /** Çubuk silüet: noktalar (kalça merkezli, s biriminde) = baş, boyun, sol el, sağ el, sol ayak, sağ ayak. */
    static void figure(Canvas cv, float x, float y, float s, int col, float[] q) {
        Paint p = paint();
        p.setColor(col);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(s * 0.55f);
        cv.drawLine(x + q[2] * s, y + q[3] * s, x, y, p);
        p.setStrokeWidth(s * 0.26f);
        cv.drawLine(x + q[2] * s, y + q[3] * s, x + q[4] * s, y + q[5] * s, p);
        cv.drawLine(x + q[2] * s, y + q[3] * s, x + q[6] * s, y + q[7] * s, p);
        p.setStrokeWidth(s * 0.34f);
        cv.drawLine(x, y, x + q[8] * s, y + q[9] * s, p);
        cv.drawLine(x, y, x + q[10] * s, y + q[11] * s, p);
        p.setStyle(Paint.Style.FILL);
        cv.drawCircle(x + q[0] * s, y + q[1] * s, s * 0.32f, p);
    }

    static final float[] POSE_LEAP = {0.15f, -1.7f, 0.05f, -1.2f, -0.9f, -2.1f, 0.95f, -2.0f, -1.3f, 0.9f, 1.2f, 0.6f};
    static final float[] POSE_PUSH = {0.95f, -1.35f, 0.7f, -0.95f, 1.5f, -0.9f, 1.6f, -0.45f, -0.5f, 1.2f, 0.6f, 1.25f};
    static final float[] POSE_STAND = {0f, -1.7f, 0f, -1.2f, -0.55f, 0.6f, 0.55f, 0.6f, -0.3f, 1.6f, 0.3f, 1.6f};

    static void cliffJump(Canvas cv, int w, int h, Random r) {
        sky(cv, w, h, 1f, 0xFFB43C12, 0xFFFF8A1F, 0xFFFFC447);
        glow(cv, w * 0.5f, h * 0.36f, w * 0.8f, 0xFFFFF0B0, 220);
        disc(cv, w * 0.5f, h * 0.36f, w * 0.1f, 0xFFFFFBE6);
        Paint p = paint();
        p.setColor(0xFF120804);
        Path l = new Path();
        l.moveTo(0, h * 0.58);
        l.lineTo(w * 0.30f, h * 0.58f);
        l.lineTo(w * 0.34f, h * 0.63f);
        l.lineTo(w * 0.27f, h * 0.74f);
        l.lineTo(w * 0.20f, h);
        l.lineTo(0, h);
        l.close();
        cv.drawPath(l, p);
        Path rr = new Path();
        rr.moveTo(w, h * 0.64f);
        rr.lineTo(w * 0.70f, h * 0.64f);
        rr.lineTo(w * 0.65f, h * 0.71f);
        rr.lineTo(w * 0.71f, h * 0.82f);
        rr.lineTo(w * 0.80f, h);
        rr.lineTo(w, h);
        rr.close();
        cv.drawPath(rr, p);
        figure(cv, w * 0.5f, h * 0.45f, w * 0.045f, 0xFF120804, POSE_LEAP);
    }

    static void boulder(Canvas cv, int w, int h, Random r) {
        sky(cv, w, h, 1f, 0xFF2A1450, 0xFFB8326A, 0xFFFF8A3D, 0xFFFFD08A);
        glow(cv, w * 0.78f, h * 0.5f, w * 0.8f, 0xFFFFD27D, 210);
        disc(cv, w * 0.78f, h * 0.5f, w * 0.09f, 0xFFFFF1B8);
        skyline(cv, w, h, r, h * 0.78f, 0xFF4A2250, h * 0.12f, 0);
        Paint p = paint();
        p.setColor(0xFF140A10);
        Path sl = new Path();
        sl.moveTo(0, h);
        sl.lineTo(0, h * 0.95f);
        int steps = 14;
        for (int i = 1; i <= steps; i++) {
            float fx = w * i / (float) steps;
            sl.lineTo(fx, h * 0.95f - h * 0.40f * i / steps - r.nextFloat() * h * 0.015f);
        }
        sl.lineTo(w, h);
        sl.close();
        cv.drawPath(sl, p);
        float bx = w * 0.64f;
        float by = h * 0.95f - h * 0.40f * 0.64f;
        float rad = w * 0.15f;
        Paint bp = paint();
        bp.setShader(new RadialGradient(bx + rad * 0.3f, by - rad * 1.3f, rad * 1.6f, 0xFFE0904A, 0xFF1E0F12,
                Shader.TileMode.CLAMP));
        cv.drawCircle(bx, by - rad * 0.85f, rad, bp);
        float px = w * 0.34f;
        float py = h * 0.95f - h * 0.40f * 0.34f;
        figure(cv, px, py - w * 0.075f, w * 0.05f, 0xFF140A10, POSE_PUSH);
    }

    static void discipline(Canvas cv, int w, int h, Random r) {
        sky(cv, w, h, 1f, 0xFF030303, 0xFF1A1A1A, 0xFF2E2E2E);
        glow(cv, w * 0.5f, h * 0.56f, w * 0.85f, 0xFFFFFFFF, 130);
        Paint p = paint();
        p.setShader(new LinearGradient(0, h * 0.7f, 0, h, 0xFF3A3A3A, 0xFF050505, Shader.TileMode.CLAMP));
        cv.drawRect(0, h * 0.7f, w, h, p);
        p.setShader(null);
        p.setColor(0xFFFFFFFF);
        for (int i = 0; i < 160; i++) {
            p.setAlpha(8 + r.nextInt(22));
            cv.drawCircle(r.nextFloat() * w, r.nextFloat() * h, 1f + r.nextFloat() * 2f, p);
        }
        figure(cv, w * 0.5f, h * 0.60f, w * 0.06f, 0xFF050505, POSE_STAND);
    }
}
