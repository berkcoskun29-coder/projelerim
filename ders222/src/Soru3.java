import java.util.Scanner;

public class Soru3 {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.print("Bir sayı giriniz: ");
        int sayi = input.nextInt();

        boolean asal = true;

        if(sayi <= 1){
            asal = false;
        }

        for(int i = 2; i < sayi; i++){
            if(sayi % i == 0){
                asal = false;
                break;
            }
        }

        if(asal)
            System.out.println("Asal sayıdır.");
        else
            System.out.println("Asal sayı değildir.");
    }
}