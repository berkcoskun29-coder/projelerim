import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ArrayList<Product> products = new ArrayList<>();
        Scanner input = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=== Ürün Yönetim Sistemi ===");
            System.out.println("1 - Ürün Listele");
            System.out.println("2 - Yeni Ürün Ekle");
            System.out.println("3 - Fiyata Göre Sırala");
            System.out.println("4 - Ürün Satış Yap");
            System.out.println("5 - Çıkış");
            System.out.print("Seçiminiz: ");
            int secim = input.nextInt();
            input.nextLine(); 

            switch (secim) {
                case 1:
                    if (products.isEmpty()) {
                        System.out.println("Henüz ürün yok.");
                    } else {
                        System.out.println("\n=== Ürün Listesi ===");
                        for (Product p : products) {
                            p.showInfo();
                            System.out.println("------------");
                        }
                    }
                    break;

                case 2:
                    System.out.print("Ürün adı: ");
                    String name = input.nextLine();
                    System.out.print("Fiyat: ");
                    double price = input.nextDouble();
                    System.out.print("Stok: ");
                    int stock = input.nextInt();

                    products.add(new Product(products.size() + 1, name, price, stock));
                    System.out.println("Ürün eklendi!");
                    break;

                case 3:
                    if (products.isEmpty()) {
                        System.out.println("Liste boş.");
                    } else {
                        Collections.sort(products, Comparator.comparingDouble(Product::getPrice));
                        System.out.println("Ürünler fiyata göre sıralandı!");
                    }
                    break;

                case 4:
                    if (products.isEmpty()) {
                        System.out.println("Satış yapılacak ürün yok.");
                    } else {
                        System.out.print("Satış yapılacak ürün ID: ");
                        int id = input.nextInt();
                        System.out.print("Satılacak adet: ");
                        int adet = input.nextInt();

                        if (id > 0 && id <= products.size()) {
                            products.get(id - 1).sell(adet);
                        } else {
                            System.out.println("Geçersiz ID!");
                        }
                    }
                    break;

                case 5:
                    running = false;
                    System.out.println("Program sonlandırıldı.");
                    break;

                default:
                    System.out.println("Hatalı seçim!");
            }
        }
    }
}

	


