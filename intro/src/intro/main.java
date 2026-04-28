 package intro;

public class main {

	public static void main(String[] args) {
        System.out.println("Hello world!");
		
        // değişken isimlendrmeleri javada camelCase yazılır
        String ortaMetin = "BC burada";
        String altMetin ="BC";
        
        System.out.println(ortaMetin);
        
        //integer tam sayı
        int vade = 12;
        
        //ondalıklı sayı
        double dolarDun =18.25;
        double dolarBugun =18.20;
        
        Boolean dolarNeDustuMu = false;
        
        String okYonu = "";
        
        if (dolarBugun<dolarDun) { //true
            okYonu = "down.svg";
	        System.out.println(okYonu);
		} else if (dolarBugun>dolarDun) {
			okYonu = "up.svg";
			System.out.println(okYonu);
	        
		}
        else {
        	okYonu = "eaual.svg";
			System.out.println(okYonu);
            
			
			//array  listeler
			
			String[] krediler = {"Hızlı kredi","Maaşını Halkbanktan","Mutlu Emekli"};

			for (int i = 0; i < krediler.length; i++) {
				System.out.println(krediler[i]);
			}
		} 
			
		
       
        
	}

}
 