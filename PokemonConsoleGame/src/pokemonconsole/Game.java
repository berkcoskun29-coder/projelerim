package pokemonconsole;

import java.util.Random;
import java.util.Scanner;

public class Game {
    private Player player;
    private BattleSystem battleSystem;
    private Scanner scanner;
    private WorldMap worldMap;
    private Random random;

    public Game() {
        this.player = new Player("Berk"); // istersen değiştir
        this.battleSystem = new BattleSystem();
        this.scanner = new Scanner(System.in);
        this.random = new Random();

        // 10x10 basit harita
        this.worldMap = new WorldMap(10, 10);

        createStarterPokemon();
        giveStarterItems();
    }

    private void createStarterPokemon() {
        // Başlangıç Pokémon'u oluştur
        Pokemon starter = new Pokemon("Flamemon", 5, 35, 12, 8);
        starter.addMove(new Move("Tackle", 35, 100));
        starter.addMove(new Move("Quick Hit", 25, 95));
        starter.addMove(new Move("Fire Bite", 40, 90));

        player.addPokemon(starter);
    }

    private void giveStarterItems() {
        // Oyuncuya başlangıçta 5 Pokéball ver
        player.addPokeballs(5);
    }

    public void start() {
        System.out.println("🎮 Konsol Pokémon Benzeri Oyuna Hoş Geldin, " + player.getName() + "!");
        boolean running = true;

        while (running) {
            System.out.println("\n=== ANA MENÜ ===");
            System.out.println("1) Haritada dolaş (keşif)");
            System.out.println("2) Direkt vahşi Pokémon ile savaş (test)");
            System.out.println("3) Takımı / Pokéball sayısını göster");
            System.out.println("4) Çıkış");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    explore();
                    break;
                case "2":
                    startWildBattle(); // eskisi gibi direkt savaş
                    break;
                case "3":
                    player.showParty();
                    break;
                case "4":
                    running = false;
                    break;
                default:
                    System.out.println("Geçersiz seçim.");
            }
        }

        System.out.println("Oyun kapatıldı. Görüşürüz!");
    }

    // KEŞİF MODU
    private void explore() {
        boolean exploring = true;
        System.out.println("\nKeşif moduna girdin. W/A/S/D ile dolaş, Q ile menüye dön.");

        while (exploring) {
            int x = player.getX();
            int y = player.getY();

            // Mevcut kare
            Tile tile = worldMap.getTile(x, y);
            System.out.println("\n📍 Konumun: (" + x + ", " + y + ")");
            if (tile != null) {
                System.out.println("Zemin: " + tile.toString() + (tile.isGrass() ? " (Burada Pokémon çıkabilir)" : ""));
            } else {
                System.out.println("Haritanın dışındasın (bu yazıyı görmemen lazım normalde 😊).");
            }

            System.out.println("Komut gir: W (yukarı), A (sol), S (aşağı), D (sağ), Q (çıkış)");
            String input = scanner.nextLine().trim().toUpperCase();

            int oldX = player.getX();
            int oldY = player.getY();

            switch (input) {
                case "W":
                    player.moveUp();
                    break;
                case "A":
                    player.moveLeft();
                    break;
                case "S":
                    player.moveDown();
                    break;
                case "D":
                    player.moveRight();
                    break;
                case "Q":
                    exploring = false;
                    continue;
                default:
                    System.out.println("Geçersiz komut.");
                    continue;
            }

            // Harita dışına çıktıysa geri al
            if (!worldMap.isInsideMap(player.getX(), player.getY())) {
                System.out.println("🚧 Haritanın dışına çıkamazsın!");
                // eski konuma dön
                // (hareketleri geri almanın en basit yolu)
                // direk eski x,y set etmek:
                // ama Player'da setX/setY yok, o yüzden küçük bir hack:
                // move'u tersine uygula
                if (input.equals("W")) {
                    player.moveDown();
                } else if (input.equals("S")) {
                    player.moveUp();
                } else if (input.equals("A")) {
                    player.moveRight();
                } else if (input.equals("D")) {
                    player.moveLeft();
                }
                continue;
            }

            // Yeni kareyi al
            Tile newTile = worldMap.getTile(player.getX(), player.getY());

            // Eğer çimlik alana girdiyse belli bir ihtimalle vahşi Pokémon çıkart
            if (newTile != null && newTile.isGrass()) {
                int chance = random.nextInt(100); // 0-99
                if (chance < 30) { // %30 ihtimalle savaş
                    System.out.println("\n🌿 Çimliklerde gezinirken bir şey kıpırdadı...");
                    Pokemon wild = PokemonFactory.createRandomWildPokemon();
                    battleSystem.startBattle(player, wild);

                } else {
                    System.out.println("Etrafta şimdilik vahşi Pokémon yok gibi...");
                }
            } else {
                System.out.println("Burası sakin bir bölge. (Çimlik değil)");
            }
        }

        System.out.println("Keşif modundan çıktın.");
    }

   


    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}
