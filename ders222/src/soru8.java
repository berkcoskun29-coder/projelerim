import java.util.Scanner;

public class soru8 {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.print("Terim sayısını giriniz: ");
        int n = input.nextInt();

        int a = 0, b = 1;

        System.out.print("Fibonacci Dizisi: ");

        for(int i = 1; i <= n; i++){

            System.out.print(a + " ");

            int next = a + b;
            a = b;
            b = next;
        }
    }
}