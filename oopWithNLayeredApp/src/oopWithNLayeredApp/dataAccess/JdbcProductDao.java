package oopWithNLayeredApp.dataAccess;

import oopWithNLayeredApp.entites.Product;

public class JdbcProductDao implements ProductDao {
	public void add(Product product) {
		//db erişim kodları buraya yazılır SQL
		System.out.println("JDBC ile veritabanına eklendi");
		}
}

//Hibernate