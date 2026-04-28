package pokemonconsole;

public class Tile {
    private boolean grass; // true ise çimlik alan

    public Tile(boolean grass) {
        this.grass = grass;
    }

    public boolean isGrass() {
        return grass;
    }

    @Override
    public String toString() {
        return grass ? "Çimlik" : "Düz zemin";
    }
}
