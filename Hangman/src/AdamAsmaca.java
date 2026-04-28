import java.util.*;

public class AdamAsmaca {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

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

        int secim = scanner.nextInt();
        scanner.nextLine(); // satır sonunu temizle

        String[] kelimeler;
        String[] ipuclar;

        switch (secim) {
            case 1: // Hayvan
                kelimeler = new String[]{"KEDİ", "KÖPEK", "ASLAN", "TİLKİ", "FİL"};
                ipuclar = new String[]{
                        "Evcil bir hayvan 🐱",
                        "Sadık dost olarak bilinir 🐶",
                        "Ormanın kralı 🦁",
                        "Kurnazlığı ile bilinir 🦊",
                        "Uzun hortumuyla tanınır 🐘"
                };
                break;
            case 2: // Şehir
                kelimeler = new String[]{"İSTANBUL", "ANKARA", "İZMİR", "BURSA", "ADANA"};
                ipuclar = new String[]{
                        "Boğazı ile ünlü şehir 🌉",
                        "Türkiye’nin başkenti 🏛️",
                        "Ege’nin incisi 🏖️",
                        "İskender kebabı ile meşhur 🥙",
                        "Kebapları ile ünlü sıcak şehir 🌶️"
                };
                break;
            case 3: // Eşya
                kelimeler = new String[]{"MASA", "SANDALYE", "BİLGİSAYAR", "KALEM", "LAMBA"};
                ipuclar = new String[]{
                        "Üzerinde çalışılır veya yemek yenir 🪑",
                        "Oturmak için kullanılır 💺",
                        "Yazı, oyun ve internet için kullanılır 💻",
                        "Yazı yazmak için kullanılır ✏️",
                        "Odayı aydınlatır 💡"
                };
                break;
            case 4: // Bitki
                kelimeler = new String[]{"GÜL", "LALE", "MENEKŞE", "KAKTÜS", "ORMAN"};
                ipuclar = new String[]{
                        "Dikenli ama çok güzel kokulu 🌹",
                        "Osmanlı’nın simgesi 🌷",
                        "Mor renkte küçük bir çiçek 🌸",
                        "Susuzluğa dayanıklı bitki 🌵",
                        "Birçok ağacı içinde barındırır 🌲"
                };
                break;
            case 5: // Ünlü
                kelimeler = new String[]{"AJDA PEKKAN", "CEM YILMAZ", "TARKAN", "ACUN ILICALI"};
                ipuclar = new String[]{
                        "Süperstar lakaplı şarkıcı 🎤",
                        "Komedyen ve oyuncu 😂",
                        "Megastar olarak bilinir 🎶",
                        "Yarışma programlarıyla tanınır 📺"
                };
                break;
            case 6: // Ülke
                kelimeler = new String[]{"TÜRKİYE", "ALMANYA", "FRANSA", "İTALYA", "JAPONYA"};
                ipuclar = new String[]{
                        "Anadolu’da yer alır 🇹🇷",
                        "Otomobil markalarıyla ünlü 🇩🇪",
                        "Eyfel Kulesi burada 🇫🇷",
                        "Pizza ve makarna ülkesi 🍕",
                        "Teknoloji ve anime ülkesi 🇯🇵"
                };
                break;
            case 7: // Film
                kelimeler = new String[]{"AVATAR", "TITANIC", "INCEPTION", "RECEP İVEDİK"};
                ipuclar = new String[]{
                        "Mavi uzaylıların yaşadığı film 🌌",
                        "Bir gemi kazasını anlatır 🚢",
                        "Rüyalar içinde rüyalar 🌙",
                        "Komedi filmi, yerli yapım 😂"
                };
                break;
            case 8: // Spor
                kelimeler = new String[]{"FUTBOL", "BASKETBOL", "TENİS", "YÜZME"};
                ipuclar = new String[]{
                        "Topla oynanan en popüler spor ⚽",
                        "Potaya top atılarak oynanır 🏀",
                        "Raketle oynanır 🎾",
                        "Suda yapılan spor 🏊"
                };
                break;
            case 9: // Yemek
                kelimeler = new String[]{"PİLAV", "KEBAP", "MAKARNA", "MANTI", "KARNIYARIK"};
                ipuclar = new String[]{
                        "Genellikle et yemeklerinin yanında yenir 🍚",
                        "Izgarada pişirilir 🍢",
                        "İtalyan mutfağından gelen yemek 🍝",
                        "Hamurun içinde kıyma bulunur 🥟",
                        "Patlıcanla yapılan nefis yemek 🍆"
                };
                break;
            default:
                System.out.println("Geçersiz seçim!");
                return;
        }

        // 🔹 Rastgele kelime seç
        int rastgele = new Random().nextInt(kelimeler.length);
        String kelime = kelimeler[rastgele].toUpperCase(Locale.ROOT);
        String ipucu = ipuclar[rastgele];

        char[] tahmin = new char[kelime.length()];
        for (int i = 0; i < kelime.length(); i++) {
            if (kelime.charAt(i) == ' ')
                tahmin[i] = ' ';
            else
                tahmin[i] = '_';
        }

        int yanlis = 0;
        int maxYanlis = 6;
        ArrayList<Character> kullanilan = new ArrayList<>();

        // 🔹 Oyun Döngüsü
        while (yanlis < maxYanlis) {
            System.out.println("\nKelime: " + String.valueOf(tahmin));
            System.out.println("Kalan hakkın: " + (maxYanlis - yanlis));
            System.out.println("Kullanılan harfler: " + kullanilan);
            System.out.print("Bir harf tahmin et (veya * ile kelimeyi tahmin et): ");

            String girdi = scanner.nextLine().trim().toUpperCase(Locale.ROOT);
            if (girdi.isEmpty()) {
                System.out.println("Boş giriş olmaz!");
                continue;
            }

            // 💥 TAM KELİME TAHMİNİ
            if (girdi.equals("*")) {
                System.out.print("Kelimenin tamamını tahmin et: ");
                String tamTahmin = scanner.nextLine().trim().toUpperCase(Locale.ROOT);

                if (tamTahmin.equals(kelime)) {
                    for (int i = 0; i < kelime.length(); i++) {
                        tahmin[i] = kelime.charAt(i);
                    }
                    System.out.println("🎉 Tebrikler! Kelimeyi doğru tahmin ettin!");
                    break;
                } else {
                    yanlis++;
                    System.out.println("❌ Yanlış kelime tahmini! Bir hakkın gitti.");
                    continue;
                }
            }

            char harf = girdi.charAt(0);
            if (kullanilan.contains(harf)) {
                System.out.println("⚠️ Bu harfi zaten denedin!");
                continue;
            }

            kullanilan.add(harf);

            if (kelime.contains(String.valueOf(harf))) {
                for (int i = 0; i < kelime.length(); i++) {
                    if (kelime.charAt(i) == harf) {
                        tahmin[i] = harf;
                    }
                }
            } else {
                yanlis++;
                System.out.println("❌ Yanlış tahmin! Kalan hakkın: " + (maxYanlis - yanlis));

                // 💡 SON HAK KALINCA İPUCU
                if (yanlis == maxYanlis - 1) {
                    System.out.println("💡 İpucu: " + ipucu);
                }

                if (yanlis >= maxYanlis) {
                    System.out.println("😢 Hakkın bitti! Kaybettin.");
                    System.out.println("Doğru kelime: " + kelime);
                    break;
                }
            }

            // ✅ Kazanma kontrolü
            if (String.valueOf(tahmin).equals(kelime)) {
                System.out.println("🎉 Tebrikler! Kelimeyi buldun: " + kelime);
                break;
            }
        }

        System.out.println("\nOyun bitti. Teşekkürler!");
    }
}
