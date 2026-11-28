package com.koc.hatirlatici;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * Hareketli arka plan. Sahne bir kez (yarı çözünürlükte) çizilir, üstüne hafif efektler eklenir
 * (yıldız, bulut, yağmur, dalga, kıvılcım...). ~30 kare/sn, düşük işlemci kullanımı.
 */
public class AnimatedBackground extends View {
    final int theme;
    final char cat;
    final long seed;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final long t0 = SystemClock.uptimeMillis();
    final RectF tmp = new RectF();
    final Path path = new Path();

    int th = 0;
    int[] fx;
    float dens = 1f;

    Bitmap base;
    RectF dst;

    // aurora (tema 0)
    LinearGradient bg;
    RadialGradient vig;
    RadialGradient[] rgs;
    float[] rads;
    float[][] blobs;
    float[][] parts;

    // efekt verileri
    float[][] stars, clouds, glints, flies, sand, rain, petals, shapes;
    float[] rainLines;
    static final int[] NEON = {0xFFFF2BD6, 0xFF22D3EE, 0xFFFDE047, 0xFF7C3AED};

    public AnimatedBackground(Context c, int theme, char cat, long seed) {
        super(c);
        this.theme = theme;
        this.cat = cat;
        this.seed = seed;
    }

    static float[][] fill(Random r, int n, int k) {
        float[][] a = new float[n][k];
        for (int i = 0; i < n; i++) for (int j = 0; j < k; j++) a[i][j] = r.nextFloat();
        return a;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (w <= 0 || h <= 0) return;
        dens = getResources().getDisplayMetrics().density;
        th = Scenes.themeFor(theme, seed);
        fx = Scenes.FX[th];
        Random r = new Random(seed + 1);
        if (th == 0) {
            int[] pal = Wallpaper.palette(cat, new Random(seed));
            bg = new LinearGradient(0, 0, w, h, pal[0], pal[1], Shader.TileMode.CLAMP);
            int n = 5;
            rgs = new RadialGradient[n];
            rads = new float[n];
            blobs = new float[n][];
            for (int i = 0; i < n; i++) {
                float rad = Math.max(w, h) * (0.3f + r.nextFloat() * 0.3f);
                int col = pal[2 + i % 3];
                rgs[i] = new RadialGradient(0, 0, rad, (col & 0x00FFFFFF) | 0xB4000000,
                        col & 0x00FFFFFF, Shader.TileMode.CLAMP);
                rads[i] = rad;
                blobs[i] = new float[]{r.nextFloat(), r.nextFloat(),
                        0.25f + r.nextFloat() * 0.5f, 0.25f + r.nextFloat() * 0.5f, r.nextFloat() * 6.28f};
            }
            vig = new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.78f,
                    0x00000000, 0xB0000000, Shader.TileMode.CLAMP);
            parts = fill(r, 40, 5);
            for (float[] pt : parts) {
                pt[2] = 0.02f + pt[2] * 0.05f;
                pt[3] = 1.5f + pt[3] * 2.5f;
                pt[4] = pt[4] * 6.28f;
            }
            return;
        }
        base = Scenes.scene(th, cat, Math.max(1, w / 2), Math.max(1, h / 2), seed);
        dst = new RectF(0, 0, w, h);
        stars = fill(r, 60, 4);
        for (float[] s : stars) {
            s[1] *= 0.55f;
            s[2] *= 6.28f;
            s[3] = 0.8f + s[3] * 1.6f;
        }
        clouds = fill(r, 6, 4);
        for (float[] c : clouds) {
            c[0] = 0.04f + c[0] * 0.46f;
            c[1] = 0.01f + c[1] * 0.02f;
            c[2] = 0.35f + c[2] * 0.35f;
        }
        glints = fill(r, 14, 3);
        flies = fill(r, 26, 5);
        for (float[] f : flies) {
            f[2] = 0.3f + f[2];
            f[3] = 0.3f + f[3];
            f[4] *= 6.28f;
        }
        sand = fill(r, 50, 4);
        for (float[] s : sand) {
            s[2] = 0.03f + s[2] * 0.06f;
            s[3] *= 6.28f;
        }
        rain = fill(r, 100, 3);
        for (float[] q : rain) q[2] = 0.7f + q[2] * 0.8f;
        rainLines = new float[rain.length * 4];
        petals = fill(r, 40, 6);
        for (float[] q : petals) {
            q[2] = 0.04f + q[2] * 0.05f;
            q[3] = 0.6f + q[3] * 1.2f;
            q[4] *= 6.28f;
            q[5] = 3f + q[5] * 4f;
        }
        shapes = fill(r, 12, 6);
        for (float[] s : shapes) {
            s[2] = 0.05f + s[2] * 0.1f;
            s[3] = 0.01f + s[3] * 0.03f;
            s[4] *= 6.28f;
            s[5] = (float) Math.floor(s[5] * 4f);
        }
    }

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth();
        int h = getHeight();
        float t = (SystemClock.uptimeMillis() - t0) / 1000f;
        if (th == 0) {
            if (bg == null) return;
            drawAurora(cv, w, h, t);
        } else {
            if (base == null) return;
            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            p.setAlpha(255);
            cv.drawBitmap(base, null, dst, p);
            for (int f : fx) drawFx(cv, f, w, h, t);
        }
        postInvalidateDelayed(33);
    }

    void drawAurora(Canvas cv, int w, int h, float t) {
        p.setStyle(Paint.Style.FILL);
        p.setAlpha(255);
        p.setShader(bg);
        cv.drawRect(0, 0, w, h, p);
        for (int i = 0; i < blobs.length; i++) {
            float[] b = blobs[i];
            float x = w * (b[0] + 0.22f * (float) Math.sin(t * b[2] + b[4]));
            float y = h * (b[1] + 0.18f * (float) Math.cos(t * b[3] + b[4]));
            cv.save();
            cv.translate(x, y);
            p.setShader(rgs[i]);
            cv.drawCircle(0, 0, rads[i], p);
            cv.restore();
        }
        p.setShader(null);
        p.setColor(Color.WHITE);
        for (float[] pt : parts) {
            float y = (((pt[1] - t * pt[2]) % 1f) + 1f) % 1f * h;
            float x = w * pt[0] + (float) Math.sin(t * 0.8f + pt[4]) * w * 0.02f;
            p.setAlpha(60 + (int) (80 * Math.abs(Math.sin(t + pt[4]))));
            cv.drawCircle(x, y, pt[3] * dens, p);
        }
        p.setAlpha(255);
        p.setColor(Color.BLACK);
        p.setShader(vig);
        cv.drawRect(0, 0, w, h, p);
        p.setShader(null);
    }

    void drawFx(Canvas cv, int f, int w, int h, float t) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setAlpha(255);
        switch (f) {
            case Scenes.FX_STARS:
                p.setColor(Color.WHITE);
                for (float[] s : stars) {
                    p.setAlpha(60 + (int) (150 * Math.abs(Math.sin(t * (0.6f + s[2] * 0.2f) + s[2]))));
                    cv.drawCircle(s[0] * w, s[1] * h, s[3] * dens, p);
                }
                break;
            case Scenes.FX_SHOOT: {
                float cyc = 7f;
                float k = (t % cyc) / cyc;
                if (k < 0.1f) {
                    int idx = (int) (t / cyc);
                    Random rr = new Random(idx * 31L + seed);
                    float x0 = w * (0.4f + rr.nextFloat() * 0.55f);
                    float y0 = h * (0.04f + rr.nextFloat() * 0.25f);
                    float pr = k / 0.1f;
                    float hx = x0 - pr * w * 0.38f;
                    float hy = y0 + pr * h * 0.15f;
                    float len = w * 0.2f;
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(2f * dens);
                    p.setColor(Color.WHITE);
                    p.setAlpha((int) (230 * (1f - pr)));
                    cv.drawLine(hx + len * 0.93f, hy - len * 0.37f, hx, hy, p);
                }
                break;
            }
            case Scenes.FX_CLOUDS:
                p.setColor(Color.WHITE);
                p.setAlpha(36);
                for (float[] c : clouds) {
                    float x = ((c[3] + t * c[1]) % 1.5f - 0.25f) * w;
                    float y = c[0] * h;
                    float s = c[2] * w;
                    tmp.set(x, y, x + s, y + s * 0.32f);
                    cv.drawOval(tmp, p);
                    tmp.set(x + s * 0.25f, y - s * 0.1f, x + s * 0.8f, y + s * 0.26f);
                    cv.drawOval(tmp, p);
                    tmp.set(x + s * 0.5f, y + s * 0.04f, x + s * 1.1f, y + s * 0.34f);
                    cv.drawOval(tmp, p);
                }
                break;
            case Scenes.FX_WAVES:
                Scenes.waves(cv, w, h, h * Scenes.OCEAN_HZ, t * 1.2f, 0xFF04223F, 55);
                break;
            case Scenes.FX_GLINT:
                p.setColor(0xFFFFF1C9);
                for (float[] g : glints) {
                    p.setAlpha(40 + (int) (200 * Math.abs(Math.sin(t * (0.8f + g[2]) + g[1] * 9f))));
                    float x = g[0] * w;
                    float y = h * (0.62f + g[1] * 0.36f);
                    float sz = 10f * dens * (0.5f + g[1]);
                    tmp.set(x - sz, y - dens, x + sz, y + dens);
                    cv.drawOval(tmp, p);
                }
                break;
            case Scenes.FX_FIREFLY:
                p.setColor(0xFFFFF59D);
                for (float[] fl : flies) {
                    float x = (fl[0] + 0.04f * (float) Math.sin(t * fl[2] + fl[4])) * w;
                    float y = (0.3f + fl[1] * 0.6f + 0.03f * (float) Math.cos(t * fl[3] + fl[4])) * h;
                    float fk = 0.5f + 0.5f * (float) Math.sin(t * 1.7f + fl[4] * 3f);
                    p.setAlpha((int) (40 * fk));
                    cv.drawCircle(x, y, 7f * dens, p);
                    p.setAlpha(60 + (int) (180 * fk));
                    cv.drawCircle(x, y, 2f * dens, p);
                }
                break;
            case Scenes.FX_SAND:
                p.setColor(0xFFFFD9A0);
                p.setAlpha(90);
                for (float[] s : sand) {
                    float x = ((s[0] + t * s[2]) % 1f) * w;
                    float y = (0.3f + s[1] * 0.65f + 0.01f * (float) Math.sin(t * 2f + s[3])) * h;
                    cv.drawCircle(x, y, 1.2f * dens, p);
                }
                break;
            case Scenes.FX_RAIN: {
                for (int i = 0; i < rain.length; i++) {
                    float[] q = rain[i];
                    float x = q[0] * w;
                    float y = ((q[1] + t * q[2]) % 1f) * h;
                    rainLines[i * 4] = x;
                    rainLines[i * 4 + 1] = y;
                    rainLines[i * 4 + 2] = x - 3f * dens;
                    rainLines[i * 4 + 3] = y + 16f * dens * q[2];
                }
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.3f * dens);
                p.setColor(0xFFBFD7FF);
                p.setAlpha(110);
                cv.drawLines(rainLines, 0, rainLines.length, p);
                break;
            }
            case Scenes.FX_PETALS:
                p.setColor(0xFFFFC1D6);
                p.setAlpha(210);
                for (float[] q : petals) {
                    float y = ((q[1] + t * q[2]) % 1f) * h;
                    float x = (q[0] + 0.05f * (float) Math.sin(t * q[3] + q[4])) * w;
                    float sz = q[5] * dens;
                    cv.save();
                    cv.rotate(t * 40f * q[3] + q[4] * 50f, x, y);
                    tmp.set(x - sz, y - sz * 0.55f, x + sz, y + sz * 0.55f);
                    cv.drawOval(tmp, p);
                    cv.restore();
                }
                break;
            case Scenes.FX_SHAPES:
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2f * dens);
                for (int i = 0; i < shapes.length; i++) {
                    float[] s = shapes[i];
                    float y = (((s[1] - t * s[3]) % 1f) + 1f) % 1f * h;
                    float x = s[0] * w;
                    float sz = s[2] * w;
                    p.setColor(NEON[(int) s[5] % 4]);
                    p.setAlpha(150);
                    cv.save();
                    cv.rotate(t * 30f * (s[3] * 20f) + s[4] * 50f, x, y);
                    int type = i % 3;
                    if (type == 0) {
                        cv.drawCircle(x, y, sz, p);
                    } else if (type == 1) {
                        tmp.set(x - sz, y - sz, x + sz, y + sz);
                        cv.drawRect(tmp, p);
                    } else {
                        path.reset();
                        path.moveTo(x, y - sz);
                        path.lineTo(x + sz, y + sz);
                        path.lineTo(x - sz, y + sz);
                        path.close();
                        cv.drawPath(path, p);
                    }
                    cv.restore();
                }
                break;
            default:
                break;
        }
    }
}
