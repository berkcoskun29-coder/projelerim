
public class Odev9 {

	public static void main(String[] args) {
        String text = "Hatun";
        String ters = "";
        
        for(int i =text.length()-1; i>=0; i-- ) {
        	ters += text.charAt(i);
        	
        }
        System.out.println("ters: " + ters );
	}

}
