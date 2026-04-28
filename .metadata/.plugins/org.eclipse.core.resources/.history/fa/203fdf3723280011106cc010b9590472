import java.util.Scanner;

public class s6 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        double sayi1, sayi2;

        for (int i = 1; i <= 3; i++) {
            System.out.println("\n" + i + ". sayı çifti");

            System.out.print("1. sayı: ");
            sayi1 = scanner.nextDouble();

            System.out.print("2. sayı: ");
            sayi2 = scanner.nextDouble();

            double toplam = sayi1 + sayi2;
            double fark = sayi1 - sayi2;
            double carpim = sayi1 * sayi2;

            System.out.print("Toplam: " + toplam);
            System.out.print("  Fark: " + fark);
            System.out.print("  Çarpım: " + carpim);

            if (sayi2 != 0) {
                double bolum = sayi1 / sayi2;
                System.out.print("  Bölüm: " + bolum);
            } else {
                System.out.print("  Bölüm: Tanımsız");
            }
        }

        scanner.close();
    }
}