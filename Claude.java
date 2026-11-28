package com.koc.hatirlatici;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Kendi Anthropic API anahtarınla Claude'a bağlanır (koç, dil sohbeti, sınav ve dersler için). */
public class Claude {
    public static final String[] MODELS = {"claude-haiku-5-5", "claude-sonnet-5-5", "claude-opus-5-5"};
    public static final String[] MODEL_NAMES = {
            "Claude Haiku 5.5 (hızlı, en ucuz)",
            "Claude Sonnet 5.5 (daha akıllı, orta fiyat)",
            "Claude Opus 5.5 (en güçlü, en pahalı)"
    };

    public static String model(Context c) {
        String m = Store.get(c, "model", MODELS[0]);
        for (String x : MODELS) if (x.equals(m)) return m;
        return MODELS[0];
    }

    public static String chat(String apiKey, String model, String system, JSONArray messages, int maxTokens)
            throws Exception {
        URL u = new URL("https://api.anthropic.com/v1/messages");
        HttpURLConnection h = (HttpURLConnection) u.openConnection();
        h.setRequestMethod("POST");
        h.setConnectTimeout(20000);
        h.setReadTimeout(120000);
        h.setRequestProperty("content-type", "application/json");
        h.setRequestProperty("x-api-key", apiKey);
        h.setRequestProperty("anthropic-version", "2023-06-01");
        h.setDoOutput(true);

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("system", system);
        body.put("messages", messages);

        OutputStream os = h.getOutputStream();
        os.write(body.toString().getBytes("UTF-8"));
        os.close();

        int code = h.getResponseCode();
        String text = read(code < 400 ? h.getInputStream() : h.getErrorStream());
        if (code >= 400) {
            String msg = text;
            try {
                msg = new JSONObject(text).getJSONObject("error").getString("message");
            } catch (Exception e) {
                // ham metni göster
            }
            if (code == 401) msg = "API anahtarı geçersiz. Ayarlar > Yapay zekâ bölümünden kontrol et.";
            if (code == 404) msg = "Model bulunamadı (" + model + "). Ayarlar'dan başka model seç. " + msg;
            throw new Exception("Hata " + code + ": " + msg);
        }
        JSONArray content = new JSONObject(text).getJSONArray("content");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            JSONObject part = content.getJSONObject(i);
            if ("text".equals(part.optString("type"))) sb.append(part.optString("text"));
        }
        return sb.toString().trim();
    }

    /** Yalnızca JSON döndürmesi istenen çağrı: yanıttan ilk { ... son } arasını ayıklar. */
    public static JSONObject json(String apiKey, String model, String system, String user, int maxTokens)
            throws Exception {
        JSONArray a = new JSONArray();
        JSONObject m = new JSONObject();
        m.put("role", "user");
        m.put("content", user);
        a.put(m);
        String t = chat(apiKey, model, system, a, maxTokens);
        int s = t.indexOf('{');
        int e = t.lastIndexOf('}');
        if (s < 0 || e <= s) throw new Exception("Yapay zekâ yanıtı anlaşılamadı, tekrar dene.");
        return new JSONObject(t.substring(s, e + 1));
    }

    static String read(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String l;
        while ((l = br.readLine()) != null) sb.append(l).append('\n');
        br.close();
        return sb.toString();
    }
}
