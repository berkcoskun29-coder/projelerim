package ders3333;
import java.util.Scanner;

public class Soru2 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        double fahrenheit, celsius;

        for (int i = 1; i <= 3; i++) {
            System.out.print(i + ". Fahrenheit değerini girin: ");
            fahrenheit = scanner.nextDouble();

            celsius = (fahrenheit - 32) / 1.8;

            System.out.println("Celsius karşılığı: " + celsius);
        }

        scanner.close();
    }
}