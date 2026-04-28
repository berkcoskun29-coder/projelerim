package proje911;
import java.util.*;

public class proje911 {

    // 🎬 1. Kategorilere göre kelime listeleri
    private static final Map<String, String[]> KATEGORILER = new LinkedHashMap<>();

    static {
        KATEGORILER.put("HAYVAN", new String[]{
            "KEDİ", "KÖPEK", "ASLAN", "KAPLUMBAĞA", "TİLKİ", "KARTAL", "FİL", "YUNUS", "AT", "PANDA", "KAPLAN" , "TİMSAH", "ZÜRAFA" ,  "BÖCÜK"
        });
        KATEGORILER.put("ŞEHİR", new String[]{
            "İSTANBUL", "ANKARA", "İZMİR", "BURSA", "ADANA", "TRABZON", "ESKİŞEHİR", "GAZİANTEP", "ANTALYA", "SAMSUN"
        });
        KATEGORILER.put("EŞYA", new String[]{
            "MASA", "SANDALYE", "BİLGİSAYAR", "KALEM", "DEFTER", "LAMBA", "TELEVİZYON", "CEP TELEFONU", "YASTIK", "AYNA"
        });
        KATEGORILER.put("BİTKİ", new String[]{
            "GÜL", "KAKTÜS", "AYÇİÇEĞİ", "LAVANTA", "ORKİDE", "MENEKŞE", "ZEYTİN", "PALMİYE", "ÇAM", "NANE"
        });
        KATEGORILER.put("ÜNLÜ", new String[]{
            "TARKAN", "CEM YILMAZ", "HADİSE", "BURAK ÖZÇİVİT", "BEREN SAAT", "MURAT BOZ", "DEMET AKALIN", "KENAN İMİRZALIOĞLU"
        });
        KATEGORILER.put("ÜLKE", new String[]{
            "TÜRKİYE", "FRANSA", "ALMANYA", "İTALYA", "JAPONYA", "MISIR", "BREZİLYA", "İSPANYA", "KANADA", "RUSYA"
        });
        KATEGORILER.put("FİLM", new String[]{
            "HARRY POTTER", "YÜZÜKLERİN EFENDİSİ", "TITANIC", "İNCEPTION", "AVATAR", "GLADIATOR", "ESARETİN BEDELİ"
        });
        KATEGORILER.put("SPOR", new String[]{
            "FUTBOL", "BASKETBOL", "VOLEYBOL", "TENİS", "YÜZME", "KOŞU", "HALTER", "KAYAK", "BOKS", "GOLF"
        });
        KATEGORILER.put("YEMEK", new String[]{
            "MANTI", "KARNIYARIK", "KEBAP", "PİZZA", "SARMAS", "PİLAV", "LAHMACUN", "BÖREK", "KURUFASULYE", "DÖNER"
        });
    }
   



    // ASCII adam çizimleri
    private static final String[] HANGMAN = {
        "  +---+\n      |\n      |\n      |\n      |\n=========",
        "  +---+\n  O   |\n      |\n      |\n      |\n=========",
        "  +---+\n  O   |\n  |   |\n      |\n      |\n=========",
        "  +---+\n  O   |\n /|   |\n      |\n      |\n=========",
        "  +---+\n  O   |\n /|\\  |\n      |\n      |\n=========",
        "  +---+\n  O   |\n /|\\  |\n /    |\n      |\n=========",
        "  +---+\n  O   |\n /|\\  |\n / \\  |\n      |\n========="
        
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        System.out.println("🎯 === ADAM ASMACA: KATEGORİLİ TÜRKÇE SÜRÜM ===\n");

        // 🔸 Kategori listesini göster
        List<String> kategoriler = new ArrayList<>(KATEGORILER.keySet());
        for (int i = 0; i < kategoriler.size(); i++) {
            System.out.println((i + 1) + ". " + kategoriler.get(i));
        }

        // 🔸 Kullanıcı kategori seçsin
        System.out.print("\nBir kategori numarası seç: ");
        int secim;
        try {
            secim = Integer.parseInt(scanner.nextLine());
        } catch (Exception e) {
            System.out.println("Geçersiz seçim!");
            return;
        }

        if (secim < 1 || secim > kategoriler.size()) {
            System.out.println("Geçerli bir kategori numarası gir!");
            return;
        }

        String secilenKategori = kategoriler.get(secim - 1);
        String[] kelimeler = KATEGORILER.get(secilenKategori);
        String kelime = kelimeler[random.nextInt(kelimeler.length)].toUpperCase(Locale.ROOT);

        System.out.println("\nKategori: " + secilenKategori);
        System.out.println("Kelime uzunluğu: " + kelime.length() + " harf\n");

        char[] tahmin = new char[kelime.length()];
        Arrays.fill(tahmin, '_');

        Set<Character> girilenHarfler = new HashSet<>();
        int yanlis = 0;
        int maxYanlis = HANGMAN.length - 1;

        // 🔁 Oyun döngüsü
        while (yanlis < maxYanlis && new String(tahmin).contains("_")) {
            System.out.println(HANGMAN[yanlis]);
            System.out.println("\nKelime: " + kelimeGoster(kelime, tahmin));
            System.out.println("Girilen harfler: " + girilenHarfler);
            System.out.println("Kalan hakkın: " + (maxYanlis - yanlis));
            System.out.print("Bir harf tahmin et: ");

            String girdi = scanner.nextLine().trim().toUpperCase(Locale.ROOT);
            if (girdi.isEmpty()) {
                System.out.println("Boş giriş olmaz!");
                continue;
           }
         // ⭐ Eğer kullanıcı '*' girerse, kelimenin tamamını tahmin etmek istiyor demektir
            if (girdi.equals("*")) {
                System.out.print("Kelimenin tamamını tahmin et: ");
                String tamTahmin = scanner.nextLine().trim().toUpperCase(Locale.ROOT);

                if (tamTahmin.equals(kelime)) {
                    // Doğru tahmin -> tüm harfleri aç
                    for (int i = 0; i < kelime.length(); i++) {
                        tahmin[i] = kelime.charAt(i);
                    }
                    System.out.println("🎉 Tebrikler! Kelimeyi doğru tahmin ettin!");
                    break; // oyun biter
                } else {
                    yanlis++;
                    System.out.println("❌ Yanlış kelime tahmini! Bir hakkın gitti.");
                    continue; // tekrar döngüye gir
                }
            }


            
            char harf = girdi.charAt(0);
            if (!Character.isLetter(harf)) {
                System.out.println("Lütfen harf gir!");
                continue;
            }

            if (girilenHarfler.contains(harf)) {
                System.out.println("Bu harfi zaten denedin!");
                continue;
            }

            girilenHarfler.add(harf);

            if (kelime.indexOf(harf) >= 0) {
                for (int i = 0; i < kelime.length(); i++) {
                    if (kelime.charAt(i) == harf) tahmin[i] = harf;
                }
                System.out.println("✅ Doğru!");
            } else {
                yanlis++;
                System.out.println("❌ Yanlış tahmin!");
            }

            System.out.println();
        }

        // 🏁 Oyun sonucu
        if (!new String(tahmin).contains("_")) {
            System.out.println("🎉 Tebrikler! Kelimeyi buldun: " + kelime);
        } else {
            System.out.println(HANGMAN[yanlis]);
            System.out.println("💀 Kaybettin! Doğru kelime: " + kelime);
        }

        scanner.close();
    }

    // Yardımcı: kelimeyi boşluklu biçimde göster
    private static String kelimeGoster(String kelime, char[] tahmin) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kelime.length(); i++) {
            if (kelime.charAt(i) == ' ') sb.append("  ");
            else sb.append(tahmin[i]).append(' ');
        }
        return sb.toString().trim();
    }
}
