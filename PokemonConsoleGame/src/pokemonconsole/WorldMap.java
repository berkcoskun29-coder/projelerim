package pokemonconsole;

import java.util.Random;

public class WorldMap {
    private Tile[][] tiles;
    private int width;
    private int height;
    private Random random = new Random();

    public WorldMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        generateBasicMap();
    }

    private void generateBasicMap() {
        // Basit: %40 çim, %60 normal zemin
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean grass = random.nextInt(100) < 40;
                tiles[x][y] = new Tile(grass);
            }
        }
    }

    public Tile getTile(int x, int y) {
        if (!isInsideMap(x, y)) {
            return null;
        }
        return tiles[x][y];
    }

    public boolean isInsideMap(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
