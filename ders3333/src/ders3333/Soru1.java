package ders3333;
import java.util.Scanner;

public class Soru1 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        double kelvin, celsius;

        for (int i = 1; i <= 5; i++) {
            System.out.print(i + ". Kelvin değerini girin: ");
            kelvin = scanner.nextDouble();

            celsius = kelvin - 273;

            System.out.println("Celsius değeri: " + celsius);
        }

        scanner.close();
    }
}