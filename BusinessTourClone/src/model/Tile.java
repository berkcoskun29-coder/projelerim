package model;

public class Tile {
    private final int id;
    private final String name;
    private final TileType type;

    public Tile(int id, String name, TileType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public int id() { return id; }
    public String name() { return name; }
    public TileType type() { return type; }
}
