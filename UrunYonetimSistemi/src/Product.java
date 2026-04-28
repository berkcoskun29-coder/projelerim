
public class Product {
	private int id;
	private String name;
	private double price;
	private int stock;
	
	public Product (int id, String name , double price , int stock) {
		this.id=id;
		this.name=name;
		this.price=price;
		this.stock=stock;
	}
	
	public void showInfo() {
		System.out.println("Ürün ID:" + id);
		System.out.println("Ürün Adı:" + name );
		System.out.println("Fiyat:" + price + "TL");
		System.out.println("Stok:"+ stock);
		
	}
	public double getPrice() {
		return price;
	}
	public String getName() {
		return name;
	}
	public void sell(int amount) {
		if (stock>= amount) {
			stock-= amount;
			System.out.println(amount + " adet satıldı. Kalan stok:" + stock);
		} else {
			System.out.println("Yetersiz stok!");
			
		}
	}

}
