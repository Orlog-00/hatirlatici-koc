package com.koc.hatirlatici;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/** Hareketli arka plan: süzülen neon lekeler + yukarı akan parçacıklar. Hafif, ~30 kare/sn. */
public class AnimatedBackground extends View {
    final char cat;
    final long seed;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final long t0 = SystemClock.uptimeMillis();

    LinearGradient bg;
    RadialGradient vig;
    RadialGradient[] rgs;
    float[] rads;
    float[][] blobs;
    float[][] parts;

    public AnimatedBackground(Context c, char cat, long seed) {
        super(c);
        this.cat = cat;
        this.seed = seed;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (w <= 0 || h <= 0) return;
        Random r = new Random(seed);
        int[] pal = Wallpaper.palette(cat, r);
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
        parts = new float[40][];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new float[]{r.nextFloat(), r.nextFloat(), 0.02f + r.nextFloat() * 0.05f,
                    1.5f + r.nextFloat() * 2.5f, r.nextFloat() * 6.28f};
        }
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (bg == null) return;
        int w = getWidth();
        int h = getHeight();
        float t = (SystemClock.uptimeMillis() - t0) / 1000f;
        float dens = getResources().getDisplayMetrics().density;

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

        postInvalidateDelayed(33);
    }
}
