public class Soru4 {
    public static void main(String[] args) {

        int i = 1;
        int toplam = 0;

        while(i <= 100){

            if(i % 2 != 0 && i % 3 == 0 && i % 5 != 0){
                toplam += i;
            }

            i++;
        }

        System.out.println("Toplam: " + toplam);
    }
}