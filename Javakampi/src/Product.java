
public class Product {

	public static void main(String[] args) {

		int id;
		String name;
		double unitPrice;
		String detail;
	}

	 Product product1 = new Product();
     product1.id = 1;
     product1.name = "Lenovo V15";
     product1.unitPrice = 15000;
     product1.detail = "i5 işlemci, 16GB RAM";
     
     Product product2 = new Product();
     product2.id = 2;
     product2.name = "HP Pavilion";
     product2.unitPrice= 20000;
     product2.detail="i7 iŞLEMCİ, 32GB RAM";
     
     System.out.println(product1.name + " - " + product1.unitPrice + "  TL ");
     System.out.println(product2.name + " - " + product2.unitPrice + " TL ");
     

}
