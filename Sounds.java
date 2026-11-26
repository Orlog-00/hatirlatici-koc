package com.koc.hatirlatici;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class Sounds {
    public static final String[] NAMES = {"Zil", "Melodi", "Damla", "Nabız", "Alarm", "Gong"};
    public static final int[] RAW = {R.raw.s0, R.raw.s1, R.raw.s2, R.raw.s3, R.raw.s4, R.raw.s5};

    public static final String[] VIB_NAMES = {"Yok", "Kısa", "Çift", "Uzun", "Dalga"};
    public static final long[][] VIBS = {
            {},
            {0, 200},
            {0, 200, 100, 200},
            {0, 700},
            {0, 100, 60, 100, 60, 100, 60, 400}
    };

    public static void preview(Context c, int sound, int vib) {
        try {
            final MediaPlayer mp = MediaPlayer.create(c, RAW[sound]);
            if (mp != null) {
                mp.setOnCompletionListener(m -> m.release());
                mp.start();
            }
        } catch (Exception e) {
            // ses çalınamadı
        }
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
