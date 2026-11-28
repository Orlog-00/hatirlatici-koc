package com.koc.hatirlatici;

import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Daha net konuşma→yazı: seçilen dilde tanır, konuşurken yazıyı canlı gösterir, bitince alternatif yazılışları
 * dokunulabilir öneri olarak sunar; metin kutusu düzenlenebilir olduğundan yanlış duyulan yer elle düzeltilir.
 * Her mikrofon basışı mevcut metne ekleme yapar.
 */
public class SpeechBox {
    public static View build(final MainActivity a, final EditText target, final String tag) {
        final LinearLayout box = Ui.col(a);
        final LinearLayout alts = Ui.col(a);
        final TextView status = Ui.tv(a, "Mikrofona dokun, net ve orta hızda konuş. Yanlış duyulursa aşağıdaki önerilerden seç ya da elle düzelt.", 12, Ui.SUB, false);
        final android.widget.Button mic = Ui.btn(a, "🎤 Konuş", 0xFF0E7490, null);
        final String[] base = {""};
        final boolean[] on = {false};

        mic.setOnClickListener(v -> {
            if (on[0]) {
                a.stopListen();
                on[0] = false;
                mic.setText("🎤 Konuş");
                status.setText("Durduruldu.");
                return;
            }
            alts.removeAllViews();
            String cur = target.getText().toString().trim();
            base[0] = cur.isEmpty() ? "" : cur + " ";
            on[0] = true;
            mic.setText("⏹ Dinliyorum… (bitirmek için dokun)");
            status.setText("Konuş…");
            a.listen(tag, new MainActivity.SttCb() {
                @Override
                public void partial(String s) {
                    target.setText(base[0] + s);
                    target.setSelection(target.getText().length());
                }

                @Override
                public void done(String best, List<String> list) {
                    on[0] = false;
                    mic.setText("🎤 Konuş");
                    target.setText(base[0] + best);
                    target.setSelection(target.getText().length());
                    status.setText("Duyduğum bu. Yanlışsa önerilerden seç ya da düzelt; eklemek için tekrar 🎤'a dokun.");
                    alts.removeAllViews();
                    int n = 0;
                    for (final String s : list) {
                        if (s == null || s.isEmpty() || s.equalsIgnoreCase(best) || n >= 3) continue;
                        n++;
                        TextView ch = Ui.chip(a, "↪ " + s, false, null);
                        ch.setOnClickListener(x -> {
                            target.setText(base[0] + s);
                            target.setSelection(target.getText().length());
                        });
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
                        lp.bottomMargin = Ui.dp(a, 4);
                        alts.addView(ch, lp);
                    }
                }

                @Override
                public void error(String msg) {
                    on[0] = false;
                    mic.setText("🎤 Konuş");
                    status.setText(msg);
                }
            });
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.dp(a, 4);
        box.addView(mic, lp);
        box.addView(status);
        box.addView(alts);
        box.setPadding(0, 0, 0, Ui.dp(a, 4));
        return box;
    }
}
