public class Main {
    public static void main(String[] args) {
        Product p1 = new Product(1, "Laptop", 15000.0, 5);
        Product p2 = new Product(2, "Telefon", 9000.0, 10);

        p1.showInfo();
        System.out.println("-----------");
        p2.showInfo();
    }
}

