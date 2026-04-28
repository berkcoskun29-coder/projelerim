package chess3d;

public record Pos(int x, int y, int z) {
    public Pos {
        // İstersen burada aralık kontrolünü board'a bırakalım.
    }

    public Pos add(int dx, int dy, int dz) {
        return new Pos(x + dx, y + dy, z + dz);
    }
}
