package application.service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import application.model.TabuCard; 
public class CardLoader {
	public static List <TabuCard> loadCards(){
		List<TabuCard> cards = new ArrayList<>(); 
	    cards.add(new TabuCard("Kitap", Arrays.asList("Okumak", "Sayfa", "Yazar", "Roman", "Kütüphane"
	            )));
	    cards.add(new TabuCard("Telefon", Arrays.asList(
                "Arama", "Mesaj", "Mobil", "Ekran", "Şarj"
        )));
	    cards.add(new TabuCard("Deniz", Arrays.asList(
                "Su", "Kum", "Yüzmek", "Sahil", "Mavi"
        )));
	    cards.add(new TabuCard("Kalem", Arrays.asList(
                "Yazmak", "Defter", "Mürekkep", "Silgi", "Kurşun"
        )));
	    cards.add(new TabuCard("Bilgisayar", Arrays.asList(
                "Klavye", "Mouse", "Ekran", "İnternet", "Laptop"
        )));

        cards.add(new TabuCard("Kahve", Arrays.asList(
                "İçmek", "Fincan", "Sıcak", "Türk", "Kafe"
        )));

        cards.add(new TabuCard("Araba", Arrays.asList(
                "Tekerlek", "Sürmek", "Motor", "Yol", "Direksiyon"
        )));
		
		return cards;
		
	}

}
