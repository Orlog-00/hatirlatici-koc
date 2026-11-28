package com.koc.hatirlatici;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Çoktan seçmeli soru çalıştırıcı: sınav modu (geri bildirimsiz) ve ders modu (anında düzeltme + yanlışları sonda tekrar). */
public class Mcq {
    public static class Q {
        public String q, explain = "", level = "";
        public String[] opts;
        public int ans;
        public boolean ok;       // ilk denemede doğru mu
        public boolean answered;
    }

    public interface Done {
        void on(List<Q> all, int correct, int xp);
    }

    public static List<Q> parse(JSONArray a) {
        List<Q> l = new ArrayList<>();
        if (a == null) return l;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            JSONArray op = o.optJSONArray("options");
            if (op == null || op.length() < 2) continue;
            Q q = new Q();
            q.q = o.optString("q");
            q.opts = new String[op.length()];
            for (int k = 0; k < op.length(); k++) q.opts[k] = op.optString(k);
            q.ans = Math.max(0, Math.min(o.optInt("answer"), op.length() - 1));
            q.explain = o.optString("explain");
            q.level = o.optString("level");
            if (!q.q.isEmpty()) l.add(q);
        }
        return l;
    }

    /**
     * @param feedback    her cevaptan sonra doğru/yanlış göster
     * @param repeatWrong yanlışları sonda bir kez daha sor
     */
    public static void run(final MainActivity a, final LinearLayout host, final List<Q> qs, final boolean feedback,
                           final boolean repeatWrong, final Done done) {
        final List<Q> queue = new ArrayList<>(qs);
        final int[] pos = {0};
        final int[] xp = {0};
        final List<Q> retried = new ArrayList<>();
        step(a, host, qs, queue, pos, xp, retried, feedback, repeatWrong, done);
    }

    static void step(final MainActivity a, final LinearLayout host, final List<Q> all, final List<Q> queue,
                     final int[] pos, final int[] xp, final List<Q> retried, final boolean feedback,
                     final boolean repeatWrong, final Done done) {
        host.removeAllViews();
        if (pos[0] >= queue.size()) {
            int correct = 0;
            for (Q q : all) if (q.ok) correct++;
            done.on(all, correct, xp[0]);
            return;
        }
        final Q q = queue.get(pos[0]);
        final boolean isRetry = retried.contains(q);
        LinearLayout card = Ui.card(a);
        card.addView(Ui.tv(a, (isRetry ? "Tekrar · " : "") + "Soru " + (pos[0] + 1) + " / " + queue.size()
                + (feedback ? "   ·   ⭐ " + xp[0] + " puan" : ""), 12, Ui.SUB, true));
        card.addView(Ui.bar(a, 100 * pos[0] / Math.max(1, queue.size()), Ui.ACC));
        TextView qt = Ui.tv(a, q.q, 18, Color.WHITE, true);
        qt.setPadding(0, Ui.dp(a, 12), 0, Ui.dp(a, 8));
        card.addView(qt);
        Ui.add(host, card);

        // seçenekleri karıştır
        final List<Integer> order = new ArrayList<>();
        for (int i = 0; i < q.opts.length; i++) order.add(i);
        Collections.shuffle(order);
        final List<Button> bs = new ArrayList<>();
        final LinearLayout fb = Ui.col(a);
        final boolean[] locked = {false};
        for (final int oi : order) {
            final Button b = Ui.btn(a, q.opts[oi], Ui.CARD2, null);
            b.setSingleLine(false);
            b.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            b.setPadding(Ui.dp(a, 14), Ui.dp(a, 10), Ui.dp(a, 14), Ui.dp(a, 10));
            bs.add(b);
            b.setOnClickListener(v -> {
                if (locked[0]) return;
                locked[0] = true;
                boolean right = oi == q.ans;
                if (!isRetry && !q.answered) q.ok = right;
                q.answered = true;
                if (right) xp[0] += isRetry ? 5 : 10;
                if (!right && repeatWrong && !isRetry) {
                    queue.add(q);
                    retried.add(q);
                }
                if (!feedback) {
                    pos[0]++;
                    step(a, host, all, queue, pos, xp, retried, feedback, repeatWrong, done);
                    return;
                }
                for (int k = 0; k < bs.size(); k++) {
                    int real = order.get(k);
                    if (real == q.ans) bs.get(k).setBackground(Ui.round(a, 0xFF047857, 12));
                    else if (bs.get(k) == b) bs.get(k).setBackground(Ui.round(a, 0xFFB91C1C, 12));
                }
                fb.addView(Ui.tv(a, right ? "✅ Doğru!" + (isRetry ? " (+5)" : " (+10)")
                        : "❌ Yanlış. Doğru cevap: " + q.opts[q.ans], 16, right ? Ui.OK : Ui.BAD, true));
                if (!q.explain.isEmpty()) {
                    TextView ex = Ui.tv(a, q.explain, 14, Ui.TXT, false);
                    ex.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
                    fb.addView(ex);
                }
                if (!right && repeatWrong && !isRetry) {
                    fb.addView(Ui.tv(a, "Bu soru sonda tekrar sorulacak.", 12, Ui.WARN, false));
                }
                Button next = Ui.btn(a, "Devam →", Ui.ACC, x -> {
                    pos[0]++;
                    step(a, host, all, queue, pos, xp, retried, feedback, repeatWrong, done);
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = Ui.dp(a, 8);
                fb.addView(next, lp);
            });
            Ui.add(host, b);
        }
        Ui.add(host, fb);
    }
}
