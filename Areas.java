package com.koc.hatirlatici;

/** Görev alanları ve her alan için önerilen uyarı aralığı / saat penceresi. */
public class Areas {
    public static final String[] NAMES = {
            "Genel", "Ders / Okul", "Kodlama / Proje", "Yabancı dil", "Spor / Hareket",
            "Su / Sağlık", "Kariyer / Başvuru", "Kitap / Kişisel gelişim", "Rutin / Uyku"
    };
    // aralık değeri ve birimi (0 = dakika, 1 = saat, 2 = gün)
    public static final int[] EVERY = {3, 2, 3, 6, 4, 90, 1, 1, 1};
    public static final int[] UNIT = {1, 1, 1, 1, 1, 0, 2, 2, 2};
    // uyarının çalabileceği saat penceresi (başlangıç dahil, bitiş hariç)
    public static final int[] START = {8, 9, 10, 9, 8, 8, 10, 18, 21};
    public static final int[] END = {23, 21, 23, 22, 21, 22, 20, 23, 23};
}
