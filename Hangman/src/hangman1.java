import java.util.*;

public class hangman1 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        Map<String, String[]> kategoriler = new HashMap<>();
        Map<String, Map<String, String>> ipuclar = new HashMap<>();
        
        System.out.println("🎮 ADAM ASMACA OYUNU 🎮");
        System.out.println("-------------------------");
        System.out.println("Kategori seçiniz:");
        System.out.println("1. Hayvan");
        System.out.println("2. Şehir");
        System.out.println("3. Eşya");
        System.out.println("4. Bitki");
        System.out.println("5. Ünlü");
        System.out.println("6. Ülke");
        System.out.println("7. Film");
        System.out.println("8. Spor");
        System.out.println("9. Yemek");
        System.out.print("Seçiminiz (1-9): ");


        // 🐾 Hayvan kategorisi
        kategoriler.put("Hayvan", new String[]{"KEDI", "KÖPEK", "ASLAN", "TAVŞAN", "KAPLUMBAĞA"});
        Map<String, String> hayvanIpuclari = new HashMap<>();
        hayvanIpuclari.put("KEDI", "Evcil ve mırlayan bir hayvan.");
        hayvanIpuclari.put("KÖPEK", "Sadakatiyle bilinir, havlar.");
        hayvanIpuclari.put("ASLAN", "Ormanın kralı.");
        hayvanIpuclari.put("TAVŞAN", "Havuç yemeyi sever, hızlı koşar.");
        hayvanIpuclari.put("KAPLUMBAĞA", "Yavaş yürür, kabuğu vardır.");
        ipuclar.put("Hayvan", hayvanIpuclari);

        // 🏙️ Şehir kategorisi
        kategoriler.put("Şehir", new String[]{"İSTANBUL", "ANKARA", "İZMİR", "BURSA", "TRABZON"});
        Map<String, String> sehirIpuclari = new HashMap<>();
        sehirIpuclari.put("İSTANBUL", "İki kıtayı birbirine bağlayan şehir.");
        sehirIpuclari.put("ANKARA", "Türkiye'nin başkenti.");
        sehirIpuclari.put("İZMİR", "Ege'nin incisi.");
        sehirIpuclari.put("BURSA", "Yeşil türbesiyle ünlü şehir.");
        sehirIpuclari.put("TRABZON", "Karadeniz’in futbol aşkıyla bilinen şehri.");
        ipuclar.put("Şehir", sehirIpuclari);

        // 🏠 Eşya kategorisi
        kategoriler.put("Eşya", new String[]{"MASA", "SANDALYE", "KAPI", "BİLGİSAYAR", "TELEFON"});
        Map<String, String> esyaIpuclari = new HashMap<>();
        esyaIpuclari.put("MASA", "Üzerinde yemek yenir veya çalışılır.");
        esyaIpuclari.put("SANDALYE", "Oturmak için kullanılır.");
        esyaIpuclari.put("KAPI", "Odaların girişinde bulunur.");
        esyaIpuclari.put("BİLGİSAYAR", "Kod yazmak ve oyun oynamak için birebir.");
        esyaIpuclari.put("TELEFON", "Cebimizdeki küçük bilgisayar.");
        ipuclar.put("Eşya", esyaIpuclari);

        // 🌿 Bitki kategorisi
        kategoriler.put("Bitki", new String[]{"GÜL", "PAPATYA", "MENEKŞE", "LALE", "KAKTÜS"});
        Map<String, String> bitkiIpuclari = new HashMap<>();
        bitkiIpuclari.put("GÜL", "Aşkın sembolü çiçek.");
        bitkiIpuclari.put("PAPATYA", "Seviyor-sevmiyor çiçeği.");
        bitkiIpuclari.put("MENEKŞE", "Mor renkte küçük çiçek.");
        bitkiIpuclari.put("LALE", "Osmanlı’nın sembol çiçeği.");
        bitkiIpuclari.put("KAKTÜS", "Susuzluğa dayanıklı dikenli bitki.");
        ipuclar.put("Bitki", bitkiIpuclari);

        // 🎬 Film kategorisi
        kategoriler.put("Film", new String[]{"AVATAR", "TITANIC", "INCEPTION", "HARRY POTTER", "BATMAN"});
        Map<String, String> filmIpuclari = new HashMap<>();
        filmIpuclari.put("AVATAR", "Mavi tenli yaratıkların dünyası.");
        filmIpuclari.put("TITANIC", "Batmaz denilen bir gemi.");
        filmIpuclari.put("INCEPTION", "Rüyaların içinde rüya.");
        filmIpuclari.put("HARRY POTTER", "Sihirli dünya ve Hogwarts.");
        filmIpuclari.put("BATMAN", "Gotham şehrinin kara şövalyesi.");
        ipuclar.put("Film", filmIpuclari);

        // ⚽ Spor kategorisi
        kategoriler.put("Spor", new String[]{"FUTBOL", "BASKETBOL", "YÜZME", "TENİS", "VOLEYBOL"});
        Map<String, String> sporIpuclari = new HashMap<>();
        sporIpuclari.put("FUTBOL", "Topla oynanan en popüler spor.");
        sporIpuclari.put("BASKETBOL", "Potaya top atılan spor.");
        sporIpuclari.put("YÜZME", "Suda yapılan bir spor.");
        sporIpuclari.put("TENİS", "Raketle oynanır.");
        sporIpuclari.put("VOLEYBOL", "Top file üzerinden atılır.");
        ipuclar.put("Spor", sporIpuclari);

        // 🍽️ Yemek kategorisi
        kategoriler.put("Yemek", new String[]{"PİZZA", "KÖFTE", "MAKARNA", "PİLAV", "DÖNER"});
        Map<String, String> yemekIpuclari = new HashMap<>();
        yemekIpuclari.put("PİZZA", "İtalyan mutfağının yıldızı.");
        yemekIpuclari.put("KÖFTE", "Izgarada pişirilir, yanında pilav gider.");
        yemekIpuclari.put("MAKARNA", "Haşlanır, genelde sosla yenir.");
        yemekIpuclari.put("PİLAV", "Tane tane olmalı.");
        yemekIpuclari.put("DÖNER", "Dik şekilde pişirilir, lavaşla yenir.");
        ipuclar.put("Yemek", yemekIpuclari);

        // 🌍 Ülke kategorisi
        kategoriler.put("Ülke", new String[]{"TÜRKİYE", "ALMANYA", "FRANSA", "İNGİLTERE", "JAPONYA"});
        Map<String, String> ulkeIpuclari = new HashMap<>();
        ulkeIpuclari.put("TÜRKİYE", "Anadolu’da bulunan ülke.");
        ulkeIpuclari.put("ALMANYA", "Otomobil endüstrisiyle ünlü.");
        ulkeIpuclari.put("FRANSA", "Eyfel Kulesi’nin ülkesi.");
        ulkeIpuclari.put("İNGİLTERE", "Big Ben ve yağmurlu hava.");
        ulkeIpuclari.put("JAPONYA", "Teknoloji ve anime ülkesi.");
        ipuclar.put("Ülke", ulkeIpuclari);

        // 🎭 Ünlü kategorisi
        kategoriler.put("Ünlü", new String[]{"ATATÜRK", "BARİŞ MANÇO", "CEM YILMAZ", "AJDA PEKKAN", "TARKAN"});
        Map<String, String> unluIpuclari = new HashMap<>();
        unluIpuclari.put("ATATÜRK", "Cumhuriyetimizin kurucusu.");
        unluIpuclari.put("BARİŞ MANÇO", "7'den 77'ye herkesin sevdiği sanatçı.");
        unluIpuclari.put("CEM YILMAZ", "Komedi denince akla gelir.");
        unluIpuclari.put("AJDA PEKKAN", "Süperstar lakabıyla bilinir.");
        unluIpuclari.put("TARKAN", "Pop müziğin megastarı.");
        ipuclar.put("Ünlü", unluIpuclari);
        
    

   
        }

      
        char[] tahmin = new char[kelime.length()];
        Arrays.fill(tahmin, '_');

        int yanlis = 0;
        int maxYanlis = 6;
        List<Character> kullanilan = new ArrayList<>();

        System.out.println("\nAdam Asmaca Oyununa Hoş Geldin!");
        System.out.println("Kategori: " + secim);
        System.out.println("Kelime: " + String.valueOf(tahmin));

        while (yanlis < maxYanlis) {
            System.out.print("Bir harf gir (* = kelime tahmini): ");
            String girdi = scanner.nextLine().trim().toUpperCase();

            if (girdi.isEmpty()) continue;

            if (girdi.equals("*")) {
                System.out.print("Kelimenin tamamını tahmin et: ");
                String tam = scanner.nextLine().trim().toUpperCase();
                if (tam.equals(kelime)) {
                    System.out.println("🎉 Tebrikler! Kelimeyi doğru bildin: " + kelime);
                    return;
                } else {
                    yanlis++;
                    System.out.println("❌ Yanlış tahmin!");
                    cizim(yanlis);
                    continue;
                }
            }

            char harf = girdi.charAt(0);
            if (kullanilan.contains(harf)) {
                System.out.println("Bu harfi zaten kullandın!");
                continue;
            }

            kullanilan.add(harf);
            boolean dogru = false;
            for (int i = 0; i < kelime.length(); i++) {
                if (kelime.charAt(i) == harf) {
                    tahmin[i] = harf;
                    dogru = true;
                }
            }

            if (!dogru) {
                yanlis++;
                System.out.println("❌ Yanlış tahmin!");
                cizim(yanlis);
            }

            System.out.println("Kelime: " + String.valueOf(tahmin));

            if (yanlis == maxYanlis - 1) {
                System.out.println("⚠️ Son hakkın! İpucu: " + ipucu);
            }

            if (String.valueOf(tahmin).equals(kelime)) {
                System.out.println("🎉 Kazandın! Kelime: " + kelime);
                return;
            }
        }

        System.out.println("\n💀 Kaybettin! Kelime: " + kelime);
    

    // --- ADAM ÇİZİMİ ---
    public static void cizim(int seviye) {
        String[] adam = {
        	        "  +---+\n      |\n      |\n      |\n      |\n=========",
        	        "  +---+\n  O   |\n      |\n      |\n      |\n=========",
        	        "  +---+\n  O   |\n  |   |\n      |\n      |\n=========",
        	        "  +---+\n  O   |\n /|   |\n      |\n      |\n=========",
        	        "  +---+\n  O   |\n /|\\  |\n      |\n      |\n=========",
        	        "  +---+\n  O   |\n /|\\  |\n /    |\n      |\n=========",
        	        "  +---+\n  O   |\n /|\\  |\n / \\  |\n      |\n========="
        };
        System.out.println(adam[seviye]);
    }
}

