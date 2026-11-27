package com.koc.hatirlatici;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Her uyarıda farklı söz: karıştırılmış deste, bitmeden aynı söz tekrar etmez. */
public class Quotes {
    public static class Q {
        public char cat;
        public String text;
        public String author = "";
    }

    // {kategori, söz, yazar}   z = zor zamanlar, d = disiplin, u = umut, b = başlangıç
    static final String[][] ALL = {
            {"z", "Bazen tek bir cümle, en karanlık gecede bile yolunu bulmana yeter: Bu da geçecek, sen ise kalacaksın.", ""},
            {"z", "Zor günler seni kırmak için değil, kim olduğunu hatırlatmak için gelir.", ""},
            {"z", "Dertli olduğun gün bile bir adım atabiliyorsan, aslında düşündüğünden çok daha güçlüsün.", ""},
            {"z", "Bir cümle hayatını değiştirebilir: \"Bugün pes etmeyeceğim.\" Sadece bugün için.", ""},
            {"z", "En yorgun olduğun an, vazgeçmeye en yakın değil; çoğu zaman başarıya en yakın olduğun andır.", ""},
            {"z", "Yıkıldığını hissettiğin yerde yeni bir temel atılıyor olabilir.", ""},
            {"z", "Kimse sana kolay olacağını söylemedi, ama değeceğini ben söylüyorum.", ""},
            {"z", "Bugün mükemmel olmak zorunda değilsin; sadece dünden bir adım ileride ol.", ""},
            {"z", "Gece ne kadar uzun olursa olsun, sabah mutlaka gelir. Sen sadece uyanık kal.", ""},
            {"z", "Düştüğün yerde kalma; çünkü kalktığın yerden hikâyen yeniden başlıyor.", ""},
            {"z", "Yorgunluğun geçer, pişmanlığın geçmez. Bir adım daha at.", ""},
            {"z", "Şu an ağır gelen şeyler seni ezmiyor, sağlamlaştırıyor.", ""},
            {"z", "Kimse görmese de ilerlemen gerçek. Sessiz emek en gür sonucu getirir.", ""},
            {"z", "Önüne çıkan engel, yolun kendisine dönüşür.", "Marcus Aurelius"},
            {"z", "Yaşamak için bir nedeni olan, hemen her nasıla katlanabilir.", "Friedrich Nietzsche"},
            {"z", "Bugün sadece ayakta kalabildiysen bile, bu bir zaferdir.", ""},
            {"z", "Karanlık, yıldızların görünmesi için gereklidir.", ""},
            {"d", "Motivasyon gelip geçer; seni sonuca taşıyan şey alışkanlıktır.", ""},
            {"d", "Küçük adımlar sıkıcı görünür, ta ki bir gün dönüp bakıp ne kadar yol geldiğini görene kadar.", ""},
            {"d", "İstediğin hayat, bugün ertelediğin şeyin içinde saklı.", ""},
            {"d", "Her gün %1 daha iyi olan biri, bir yıl sonra tanınmaz hale gelir.", ""},
            {"d", "Disiplin, kendine verdiğin sözü kimse bakmıyorken tutmaktır.", ""},
            {"d", "Bugünkü emeğin, gelecekteki sen için yazılmış bir teşekkür mektubudur.", ""},
            {"d", "Başlamak için hazır olmayı bekleme; hazır olma hissi başladıktan sonra gelir.", ""},
            {"d", "Ertelediğin her iş bir gün faiziyle geri döner. Şimdi yap, rahat et.", ""},
            {"d", "Hayatta en hakiki mürşit ilimdir.", "Mustafa Kemal Atatürk"},
            {"d", "Ne kadar yavaş gittiğin önemli değil; durmadığın sürece ilerliyorsun.", ""},
            {"d", "Bugünün ter damlası, yarının gururu olur.", ""},
            {"d", "Her sabah iki seçenek var: uykuna devam et ya da hayallerinin peşinden git.", ""},
            {"d", "Zor olan başlamak; sonrası alışkanlık.", ""},
            {"d", "Bugün yapmadığın şey yarın iki kat ağırlaşır. Şimdi yap.", ""},
            {"d", "Disiplin, bugünü yönetmektir; bugünü yöneten geleceği seçer.", ""},
            {"u", "Hayallerin seni korkutuyorsa, doğru yerdesin demektir.", ""},
            {"u", "Bir gün bugünü \"her şeyin başladığı gün\" diye anlatacaksın.", ""},
            {"u", "Henüz olmadı diye hiç olmayacak anlamına gelmez.", ""},
            {"u", "İçindeki potansiyel, bahanelerinden her zaman daha büyüktür.", ""},
            {"u", "Bu hafta attığın küçük adım, gelecek yılın büyük kapısını aralayabilir.", ""},
            {"u", "Kendine inandığın an, yolun yarısını tamamlamışsın demektir.", ""},
            {"u", "Senin için yazılan hikâyenin en güzel bölümü henüz gelmedi.", ""},
            {"u", "Güneş her gün yeniden doğar; sen de.", ""},
            {"u", "Doğru yolda yavaş gitmek, yanlış yolda koşmaktan iyidir.", ""},
            {"u", "Hayal ettiğin hayat, planladığın günlerin toplamıdır.", ""},
            {"b", "Yeni bir sayfa açmak için yılbaşını bekleme; bu an da bir başlangıç.", ""},
            {"b", "İlk adımı atmak için yolun tamamını görmen gerekmez.", ""},
            {"b", "Bugün, geri kalan hayatının ilk günü. Onu nasıl kullanacaksın?", ""},
            {"b", "Mükemmel plan yok; ama bugün yapılan ortalama bir adım, yarın yapılacak mükemmel bir plandan iyidir.", ""},
            {"b", "Cesaret, korkmamak değil; korkuya rağmen ilk dakikayı başlatabilmektir.", ""},
            {"b", "Bir dakika ayır, sadece başla. Gerisi kendiliğinden gelir.", ""},
            {"b", "Yarım saatlik bir başlangıç, hiç başlamamaktan sonsuz kere iyidir.", ""},
            {"b", "Büyük işler, küçük bir \"başlıyorum\" ile başlar.", ""},
            {"b", "İlk satırı yaz, ilk sayfayı aç, ilk adımı at. Gerisi akar.", ""},
            {"b", "Başlamak için mükemmel anı bekleme; şimdi yeterince iyi.", ""}
    };

    public static Q next(Context c) {
        List<Integer> deck = new ArrayList<>();
        String d = Store.get(c, "deck", "");
        if (!d.isEmpty()) {
            for (String s : d.split(",")) {
                try {
                    int v = Integer.parseInt(s.trim());
                    if (v >= 0 && v < ALL.length) deck.add(v);
                } catch (NumberFormatException e) {
                    // atla
                }
            }
        }
        if (deck.isEmpty()) {
            for (int i = 0; i < ALL.length; i++) deck.add(i);
            Collections.shuffle(deck);
        }
        int idx = deck.remove(deck.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < deck.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(deck.get(i));
        }
        Store.put(c, "deck", sb.toString());

        Q q = new Q();
        q.cat = ALL[idx][0].charAt(0);
        q.text = ALL[idx][1];
        q.author = ALL[idx][2];
        return q;
    }
}
