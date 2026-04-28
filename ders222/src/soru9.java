import java.util.Scanner;

public class soru9 {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.print("1. sayıyı giriniz: ");
        int a = input.nextInt();

        System.out.print("2. sayıyı giriniz: ");
        int b = input.nextInt();

        int i = a;
        int toplam = 0;
        int adet = 0;

        while(i <= b){

            if(i % 2 == 0){
                toplam += i;
                adet++;
            }

            i++;
        }

        double ortalama = (double)toplam / adet;

        System.out.println("Çift sayıların ortalaması: " + ortalama);
    }
}