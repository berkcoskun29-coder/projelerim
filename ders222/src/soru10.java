import java.util.Scanner;

public class soru10 {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.print("Yaşınızı giriniz: ");
        int yas = input.nextInt();

        input.nextLine();

        System.out.print("Sağlık raporu var mı? (E/H): ");
        String saglik = input.nextLine();

        System.out.print("Sabıka kaydı var mı? (E/H): ");
        String sabika = input.nextLine();

        if (yas < 18) {

            System.out.println("Başvuru yapılamaz.");

        } else {

            if (saglik.equalsIgnoreCase("H")) {

                System.out.println("Eksik belge.");

            } else {

                if (sabika.equalsIgnoreCase("H")) {
                    System.out.println("Başvuru kabul edildi.");
                } else {
                    System.out.println("Başvuru reddedildi.");
                }

            }

        }
    }
}