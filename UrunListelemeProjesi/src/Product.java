public class Product {
    private int id;
    private String name;
    private double price;
    private int stock;

    public Product(int id, String name, double price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public void showInfo() {
        System.out.println("Ürün ID: " + id);
        System.out.println("Ürün Adı: " + name);
        System.out.println("Fiyat: " + price + " TL");
        System.out.println("Stok: " + stock);
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}













