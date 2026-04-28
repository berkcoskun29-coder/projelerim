import java.util.Scanner;

public class soru6 {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.print("Alışveriş tutarını giriniz: ");
        double tutar = input.nextDouble();

        double kargo;

        if(tutar < 100){
            kargo = 5;
        }else{
            kargo = 0;
        }

        double toplam = tutar + kargo;

        System.out.println("Alışveriş Tutarı: " + tutar);
        System.out.println("Kargo Ücreti: " + kargo);
        System.out.println("Ödenecek Tutar: " + toplam);
    }
}