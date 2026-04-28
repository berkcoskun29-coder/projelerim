package pokemonconsole;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private List<Pokemon> party; // En fazla 6 Pokémon
    private int pokeballs;       // Pokéball sayısı

    // Harita konumu
    private int x;
    private int y;

    public Player(String name) {
        this.name = name;
        this.party = new ArrayList<>();
        this.pokeballs = 0;
        this.x = 0; // başlangıç konumu
        this.y = 0;
    }

    public void addPokemon(Pokemon p) {
        if (party.size() < 6) {
            party.add(p);
            System.out.println("✅ " + p.getName() + " takıma katıldı!");
        } else {
            System.out.println("Takım dolu! (En fazla 6 Pokémon)");
        }
    }

    public Pokemon getFirstHealthyPokemon() {
        for (Pokemon p : party) {
            if (!p.isFainted()) {
                return p;
            }
        }
        return null;
    }

    public void showParty() {
        if (party.isEmpty()) {
            System.out.println("Takımında hiç Pokémon yok.");
        } else {
            System.out.println("👥 Takımındaki Pokémonlar:");
            for (int i = 0; i < party.size(); i++) {
                System.out.println((i + 1) + ") " + party.get(i).toString());
            }
        }
        System.out.println("🎯 Pokéball sayısı: " + pokeballs);
        System.out.println("📍 Konum: (" + x + ", " + y + ")");
    }

    // Pokéball işlemleri
    public void addPokeballs(int count) {
        if (count > 0) {
            pokeballs += count;
            System.out.println("🎁 " + count + " adet Pokéball aldın. Toplam: " + pokeballs);
        }
    }

    public boolean usePokeball() {
        if (pokeballs <= 0) {
            System.out.println("Hiç Pokéball'un yok!");
            return false;
        }
        pokeballs--;
        System.out.println("Bir Pokéball kullandın. Kalan: " + pokeballs);
        return true;
    }

    public int getPokeballs() {
        return pokeballs;
    }

    // Harita hareketleri
    public void moveUp() {
        y--;
    }

    public void moveDown() {
        y++;
    }

    public void moveLeft() {
        x--;
    }

    public void moveRight() {
        x++;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getName() {
        return name;
    }

    public List<Pokemon> getParty() {
        return party;
    }
}
