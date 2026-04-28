package chess3d;

import java.util.Arrays;

public class Board {
    public static final int W = 8;
    public static final int H = 8;
    public static final int L = 3; // layers (z)

    private final Piece[][][] grid = new Piece[W][H][L];

    public boolean inBounds(Pos p) {
        return p.x() >= 0 && p.x() < W
            && p.y() >= 0 && p.y() < H
            && p.z() >= 0 && p.z() < L;
    }

    public Piece get(Pos p) {
        if (!inBounds(p)) return null;
        return grid[p.x()][p.y()][p.z()];
    }

    public void set(Pos p, Piece piece) {
        if (!inBounds(p)) throw new IllegalArgumentException("Out of bounds: " + p);
        grid[p.x()][p.y()][p.z()] = piece;
    }

    public void clear(Pos p) {
        set(p, null);
    }

    public Board copy() {
        Board b = new Board();
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                for (int z = 0; z < L; z++) {
                    b.grid[x][y][z] = this.grid[x][y][z];
                }
            }
        }
        return b;
    }


    public void apply(Move m) {
        Piece p = get(m.from());
        if (p == null) throw new IllegalStateException("No piece at " + m.from());
        set(m.to(), p);
        clear(m.from());
    }
}
