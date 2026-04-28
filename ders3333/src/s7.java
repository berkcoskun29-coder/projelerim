import java.util.Scanner;

public class s7 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int ogrNo;
        double vize1, vize2, fin, basari;

        for (int i = 1; i <= 3; i++) {
            System.out.println("\n" + i + ". öğrenci");

            System.out.print("Öğrenci No: ");
            ogrNo = scanner.nextInt();

            System.out.print("Vize 1: ");
            vize1 = scanner.nextDouble();

            System.out.print("Vize 2: ");
            vize2 = scanner.nextDouble();

            System.out.print("Final: ");
            fin = scanner.nextDouble();

            basari = ((vize1 + vize2) / 2) * 0.4 + fin * 0.6;

            System.out.println("No: " + ogrNo +
                               "  Vize1: " + vize1 +
                               "  Vize2: " + vize2 +
                               "  Final: " + fin +
                               "  Başarı: " + basari);
        }

        scanner.close();
    }
}