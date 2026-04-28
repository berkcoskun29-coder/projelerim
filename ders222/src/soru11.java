import java.util.Scanner;

public class soru11 {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.print("Maaşınızı giriniz: ");
        int maas = input.nextInt();

        System.out.print("Kredi notunuzu giriniz: ");
        int krediNotu = input.nextInt();

        input.nextLine();

        System.out.print("Mevcut borcunuz var mı? (E/H): ");
        String borc = input.nextLine();

        if (krediNotu >= 1500) {

            if (maas >= 20000) {
                System.out.println("Kredi Onaylandı");
            }
            else if (maas >= 10000 && maas <= 19999) {

                if (borc.equalsIgnoreCase("H")) {
                    System.out.println("Kredi Onaylandı");
                } else {
                    System.out.println("İncelemeye alındı");
                }

            }

        }

        else if (krediNotu >= 1000 && krediNotu <= 1499) {

            if (maas >= 30000) {
                System.out.println("İncelemeye alındı");
            } else {
                System.out.println("Kredi reddedildi");
            }

        }

        else {

            System.out.println("Direkt Red");

        }

    }
}