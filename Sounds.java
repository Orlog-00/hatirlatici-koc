package com.koc.hatirlatici;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class Sounds {
    public static final String[] NAMES = {
            "Zil", "Melodi", "Damla", "Nabız", "Alarm", "Gong",
            "Kristal", "Arp", "Fanfar", "Radar", "Kuş", "Oyun",
            "Çan", "Sihir", "Uyan", "Telefon"
    };
    public static final int[] RAW = {
            R.raw.s0, R.raw.s1, R.raw.s2, R.raw.s3, R.raw.s4, R.raw.s5,
            R.raw.s6, R.raw.s7, R.raw.s8, R.raw.s9, R.raw.s10, R.raw.s11,
            R.raw.s12, R.raw.s13, R.raw.s14, R.raw.s15
    };

    public static final String[] VIB_NAMES = {
            "Yok", "Kısa", "Çift", "Uzun", "Dalga", "Üçlü", "Kalp atışı", "SOS", "Artan", "Ritim", "Sürekli"
    };
    public static final long[][] VIBS = {
            {},
            {0, 200},
            {0, 200, 100, 200},
            {0, 700},
            {0, 100, 60, 100, 60, 100, 60, 400},
            {0, 120, 80, 120, 80, 120},
            {0, 90, 110, 90, 500, 90, 110, 90},
            {0, 100, 60, 100, 60, 100, 160, 300, 60, 300, 60, 300, 160, 100, 60, 100, 60, 100},
            {0, 80, 80, 140, 80, 220, 80, 320, 80, 450},
            {0, 150, 100, 150, 100, 300, 100, 150},
            {0, 1500}
    };

    public static void preview(Context c, int sound, int vib) {
        if (sound >= 0 && sound < RAW.length) playSound(c, sound);
        if (vib >= 0 && vib < VIBS.length) vibrate(c, vib);
    }

    public static void playSound(Context c, int sound) {
        try {
            final MediaPlayer mp = MediaPlayer.create(c, RAW[sound]);
            if (mp != null) {
                mp.setOnCompletionListener(m -> m.release());
                mp.start();
            }
        } catch (Exception e) {
            // ses çalınamadı
        }
    }

    public static void vibrate(Context c, int vib) {
        try {
            Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && VIBS[vib].length > 0) {
                v.vibrate(VibrationEffect.createWaveform(VIBS[vib], -1));
            }
        } catch (Exception e) {
            // titreşim yok
        }
    }
}
