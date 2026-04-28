import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ArrayList<Product> products = new ArrayList<>();
        Scanner input = new Scanner(System.in);

        // Kullanıcıdan veri al
        System.out.print("Ürün adı: ");
        String name = input.nextLine();

        System.out.print("Fiyat: ");
        double price = input.nextDouble();

        System.out.print("Stok: ");
        int stock = input.nextInt();

        // ID otomatik veriliyor
        Product p = new Product(1, name, price, stock);
        products.add(p);

        System.out.println("\n=== Ürün Listesi ===");
        for (Product pr : products) {
            pr.showInfo();
            System.out.println("------------");
            
        }
    }
}
