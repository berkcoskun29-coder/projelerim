import java.util.Scanner;

public class s8 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int sayi1, sayi2;

        for (int i = 1; i <= 2; i++) {
            System.out.println("\n" + i + ". sayı çifti");

            System.out.print("1. sayı: ");
            sayi1 = scanner.nextInt();

            System.out.print("2. sayı: ");
            sayi2 = scanner.nextInt();

            if (sayi2 != 0) {
                int mod = sayi1 % sayi2;
                System.out.println("Mod (kalan): " + mod);
            } else {
                System.out.println("0'a bölme hatası!");
            }
        }

        scanner.close();
    }
}