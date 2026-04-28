package ders3333;
import java.util.Scanner;

public class Soru4 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int personelNo, haftaIciSaat, haftaSonuSaat;
        double brutMaas, netMaas, haftaIciUcret, haftaSonuUcret;

        for (int i = 1; i <= 3; i++) {
            System.out.println("\n" + i + ". personel bilgileri");

            System.out.print("Personel no: ");
            personelNo = scanner.nextInt();

            System.out.print("Brüt maaş: ");
            brutMaas = scanner.nextDouble();

            System.out.print("Hafta içi fazla mesai saati: ");
            haftaIciSaat = scanner.nextInt();

            System.out.print("Hafta sonu fazla mesai saati: ");
            haftaSonuSaat = scanner.nextInt();

            haftaIciUcret = haftaIciSaat * 100;
            haftaSonuUcret = haftaSonuSaat * 200;

            netMaas = brutMaas + haftaIciUcret + haftaSonuUcret;

            System.out.println("Personel No: " + personelNo +
                               "  Net Maaş: " + netMaas);
        }

        scanner.close();
    }
}