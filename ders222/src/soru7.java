import java.util.Scanner;

public class soru7 {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.print("1. sayıyı giriniz: ");
        int a = input.nextInt();

        System.out.print("2. sayıyı giriniz: ");
        int b = input.nextInt();

        System.out.print("3. sayıyı giriniz: ");
        int c = input.nextInt();

        int enBuyuk = a;
        int enKucuk = a;

        if(b > enBuyuk) enBuyuk = b;
        if(c > enBuyuk) enBuyuk = c;

        if(b < enKucuk) enKucuk = b;
        if(c < enKucuk) enKucuk = c;

        double ortalama = (a + b + c) / 3.0;

        System.out.println("En Büyük: " + enBuyuk);
        System.out.println("En Küçük: " + enKucuk);
        System.out.println("Ortalama: " + ortalama);
    }
}