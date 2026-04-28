package model;

import java.util.ArrayList;
import java.util.List;

public final class Board {
    private final List<Tile> tiles;

    public Board(List<Tile> tiles) {
        if (tiles == null || tiles.size() < 10) throw new IllegalArgumentException("Board tiles invalid");
        this.tiles = List.copyOf(tiles);
    }

    public int size() { return tiles.size(); }

    public Tile get(int index) {
        int i = Math.floorMod(index, tiles.size());
        return tiles.get(i);
    }

    public List<Tile> tiles() { return tiles; }

    public static Board createDefaultBoard() {
        List<Tile> t = new ArrayList<>();
        t.add(new Tile(0, "START", TileType.START));

        for (int i = 1; i <= 38; i++) {
            TileType type;
            String name;

            if (i == 10) { type = TileType.ISLAND; name = "KAÇIŞ ADASI"; }
            else if (i == 20) { type = TileType.AIRPORT; name = "ADNAN MENDERES"; }
            else if (i == 5 || i == 15 || i == 25) { type = TileType.RESORT; name = "TATİL KÖYÜ " + (i==5?1:i==15?2:3); }
            else if (i == 8 || i == 18 || i == 28) { type = TileType.TAX; name = "VERGİ DAİRESİ"; }
            else if (i == 3 || i == 13 || i == 23 || i == 33) { type = TileType.CHANCE; name = "ŞANS"; }
            else { type = TileType.PROPERTY; name = "ŞEHİR " + i; }

            if (type == TileType.PROPERTY) {
                // ŞİMDİLİK SABİT FİYAT/KİRA (sonra Tokyo-Berlin-LV gibi özelleştiririz)
                int price = 200_000;
                int rent  = 50_000;
                t.add(new PropertyTile(i, name, price, rent));
            } else {
                t.add(new Tile(i, name, type));
            }
        }

        // 39'u property yapalım
        t.add(new PropertyTile(39, "FESTİVAL ALANI", 250_000, 70_000));
        return new Board(t);
    }

}
