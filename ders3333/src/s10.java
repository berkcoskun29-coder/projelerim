import java.util.Scanner;
import java.util.Arrays;

public class s10 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int[] sayilar = new int[3];

        for (int i = 1; i <= 2; i++) {
            System.out.println("\n" + i + ". üçlü sayı");

            for (int j = 0; j < 3; j++) {
                System.out.print((j + 1) + ". sayı: ");
                sayilar[j] = scanner.nextInt();
            }

            Arrays.sort(sayilar);

            System.out.println("Sıralı: " +
                sayilar[0] + " " +
                sayilar[1] + " " +
                sayilar[2]);
        }

        scanner.close();
    }
}