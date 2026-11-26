package com.koc.hatirlatici;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Kendi Anthropic API anahtarınla Claude'a bağlanır (koç ve dil sohbeti için). */
public class Claude {
    static final String MODEL = "claude-haiku-5-5";

    public static String chat(String apiKey, String system, JSONArray messages) throws Exception {
        URL u = new URL("https://api.anthropic.com/v1/messages");
        HttpURLConnection h = (HttpURLConnection) u.openConnection();
        h.setRequestMethod("POST");
        h.setConnectTimeout(15000);
        h.setReadTimeout(60000);
        h.setRequestProperty("content-type", "application/json");
        h.setRequestProperty("x-api-key", apiKey);
        h.setRequestProperty("anthropic-version", "2023-06-01");
        h.setDoOutput(true);

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("max_tokens", 600);
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
            if (code == 401) msg = "API anahtarı geçersiz. Ayarlar'dan kontrol et.";
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
