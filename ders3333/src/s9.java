import java.util.Scanner;

public class s9 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        double sayi, artis, yeniDeger;

        for (int i = 1; i <= 4; i++) {
            System.out.print("\n" + i + ". sayıyı girin: ");
            sayi = scanner.nextDouble();

            artis = sayi * 0.10;
            yeniDeger = sayi + artis;

            System.out.println("Sayı: " + sayi +
                               "  Artış: " + artis +
                               "  Yeni Değer: " + yeniDeger);
        }

        scanner.close();
    }
}