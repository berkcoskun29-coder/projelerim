package oop1;

public class main {

	public static void main(String[] args) {
		String mesaj = "Vade oranı";

		Product product1 = new Product();
		// set value
		product1.setName("Delonghi Kahve Makinesi");
		product1.setDiscount(7);
		product1.setUnitPrice(7500);
		product1.setUnitsInStock(3);
		product1.setImageUrl("image1.jpg");
		// get
		// System.out.println(product1.name);

		Product product2 = new Product();
		// set value
		product2.setName("Smeg Kahve Makinesi");
		product2.setDiscount(7);
		product2.setUnitPrice(7500);
		product2.setUnitsInStock(3);
		product2.setImageUrl("image2.jpg");

		Product product3 = new Product();
//set value
		product3.setName("Bc Kahve Makinesi");
		product3.setDiscount(7);
		product3.setUnitPrice(7500);
		product3.setUnitsInStock(3);
		product3.setImageUrl("image3.jpg");

		Product product4 = new Product();
//set value
		product4.setName("Kitchen Kahve Makinesi");
		product4.setDiscount(7);
		product4.setUnitPrice(7500);
		product4.setUnitsInStock(3);
		product4.setImageUrl("image4.jpg");
		
		Product[] products = {product1,product2,product3,product4};
		
		System.out.println("<ul>");
		for (Product product : products) {
			System.out.println("<li>" + product.getName() + "<//li>");
			}
             System.out.println("</ul>");
             
             IndividualCustomer individualCustomer =new IndividualCustomer();
            individualCustomer.setId(1);
            individualCustomer.setPhone("05222222");
            individualCustomer.setCustomerNumber("12345");
            individualCustomer.setFirstName("Berk");
            individualCustomer.setLastName("Coskun"); 
            
            CorporateCustomer corporateCustomer = new CorporedCustomer();
            corporateCustomer.setId(2);
            corporateCustomer.setCompnayName("Kodlama.io");
            corporateCustomer.setPhone("053333333");
            corporateCustomer.setTaxNumber("1111111111");
            corporateCustomer.setCustomerNumber("54321");
            
            Customer[] customer = {individualCustomer,corporateCustomer};
	}

}
