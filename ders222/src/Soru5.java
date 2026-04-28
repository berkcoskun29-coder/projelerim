import java.util.Scanner;

public class Soru5 {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.print("1. sayıyı giriniz: ");
        int sayi1 = input.nextInt();

        System.out.print("2. sayıyı giriniz: ");
        int sayi2 = input.nextInt();

        int toplam = 0;
        int adet = 0;

        for(int i = sayi1; i <= sayi2; i++){

            if(i % 2 == 0){
                System.out.println(i);
                toplam += i;
                adet++;
            }
        }

        System.out.println("Çift sayı adedi: " + adet);
        System.out.println("Çift sayıların toplamı: " + toplam);
    }
}