
public class Product {
	
	
	int id;
    String name;
    double unitPrice;
    String detail;
    
    
    public class ProductDemo {
        public static void main(String[] args) {
            Product product1 = new Product();
            product1.id = 1;
            product1.name = "Laptop";
            product1.unitPrice = 15000;
            product1.detail = "16 GB RAM, 512 SSD";

            System.out.println("Ürün: " + product1.name);
        }
    }


}
