import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        ArrayList<Product> products = new ArrayList<>();

        products.add(new Product(1, "Laptop", 15000.0, 5));
        products.add(new Product(2, "Telefon", 9000.0, 10));
        products.add(new Product(3, "Kulaklık", 1200.0, 25));
        products.add(new Product(4, "Tişört", 300.0, 50));

        System.out.println("=== Ürün Listesi ===");
        for (Product p : products) {
            p.showInfo();
            System.out.println("------------");
        }
    }
}
