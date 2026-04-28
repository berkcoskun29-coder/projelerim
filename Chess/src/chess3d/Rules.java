package chess3d;

import java.util.ArrayList;
import java.util.List;

public class Rules {

    public List<Move> pseudoLegalMoves(Board b, Pos from) {
        Piece piece = b.get(from);
        if (piece == null) return List.of();

        return switch (piece.type()) {
            case ROOK -> genRays(b, from, ROOK_DIRS);
            case BISHOP -> genRays(b, from, BISHOP_DIRS);
            case QUEEN -> {
                List<Move> all = new ArrayList<>();
                all.addAll(genRays(b, from, ROOK_DIRS));
                all.addAll(genRays(b, from, BISHOP_DIRS));
                yield all;
            }
            case KING -> genKing(b, from);
            case KNIGHT -> genKnight(b, from);
            case PAWN -> List.of(); // Adım 4'te
            default -> List.of();
        };
    }

    // ---------- Ray pieces (rook/bishop/queen) ----------

    private static final int[][] ROOK_DIRS = {
            { 1, 0, 0}, {-1, 0, 0},
            { 0, 1, 0}, { 0,-1, 0},
            { 0, 0, 1}, { 0, 0,-1}
    };

    private static final int[][] BISHOP_DIRS = {
            // 2 eksenli diagonaller
            { 1, 1, 0}, { 1,-1, 0}, {-1, 1, 0}, {-1,-1, 0},
            { 1, 0, 1}, { 1, 0,-1}, {-1, 0, 1}, {-1, 0,-1},
            { 0, 1, 1}, { 0, 1,-1}, { 0,-1, 1}, { 0,-1,-1},
            // 3 eksenli (space diagonal)
            { 1, 1, 1}, { 1, 1,-1}, { 1,-1, 1}, { 1,-1,-1},
            {-1, 1, 1}, {-1, 1,-1}, {-1,-1, 1}, {-1,-1,-1}
    };

    private List<Move> genRays(Board b, Pos from, int[][] dirs) {
        Piece mover = b.get(from);
        List<Move> moves = new ArrayList<>();

        for (int[] d : dirs) {
            int dx = d[0], dy = d[1], dz = d[2];
            Pos p = from.add(dx, dy, dz);

            while (b.inBounds(p)) {
                Piece target = b.get(p);

                if (target == null) {
                    moves.add(new Move(from, p));
                } else {
                    // doluysa: rakipse capture, kendi ise dur
                    if (target.color() != mover.color()) {
                        moves.add(new Move(from, p));
                    }
                    break;
                }

                p = p.add(dx, dy, dz);
            }
        }
        return moves;
    }

    // ---------- King ----------

    private List<Move> genKing(Board b, Pos from) {
        Piece mover = b.get(from);
        List<Move> moves = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    Pos to = from.add(dx, dy, dz);
                    if (!b.inBounds(to)) continue;

                    Piece target = b.get(to);
                    if (target == null || target.color() != mover.color()) {
                        moves.add(new Move(from, to));
                    }
                }
            }
        }
        return moves;
    }

    // ---------- Knight (|dx|,|dy|,|dz| = {2,1,0}) ----------

    private List<Move> genKnight(Board b, Pos from) {
        Piece mover = b.get(from);
        List<Move> moves = new ArrayList<>();

        int[][] deltas = knightDeltas3D(); // 24 hamle

        for (int[] d : deltas) {
            Pos to = from.add(d[0], d[1], d[2]);
            if (!b.inBounds(to)) continue;

            Piece target = b.get(to);
            if (target == null || target.color() != mover.color()) {
                moves.add(new Move(from, to));
            }
        }

        return moves;
    }

    private int[][] knightDeltas3D() {
        // (2,1,0) permütasyonları * işaretleri => 24 tane
        List<int[]> list = new ArrayList<>();

        int[][] perms = {
                {2, 1, 0}, {2, 0, 1},
                {1, 2, 0}, {1, 0, 2},
                {0, 2, 1}, {0, 1, 2}
        };

        int[] signs = { -1, 1 };

        for (int[] p : perms) {
            for (int sx : signs) {
                for (int sy : signs) {
                    for (int sz : signs) {
                        int dx = p[0] * sx;
                        int dy = p[1] * sy;
                        int dz = p[2] * sz;

                        // 0 olan eksende işaret anlamsız; duplicate üretir.
                        // Bunu engellemek için sadece "0 ise +1" kabul edelim.
                        if (p[0] == 0 && sx == -1) continue;
                        if (p[1] == 0 && sy == -1) continue;
                        if (p[2] == 0 && sz == -1) continue;

                        list.add(new int[]{dx, dy, dz});
                    }
                }
            }
        }

        // 24 garanti olmalı
        return list.toArray(new int[0][]);
    }
 // --- Adım 3: Legal moves (kendi şahını açıkta bırakma yok) ---

    public List<Move> legalMoves(Board b, Pos from) {
        Piece mover = b.get(from);
        if (mover == null) return List.of();

        List<Move> candidates = pseudoLegalMoves(b, from);
        List<Move> legal = new ArrayList<>();

        for (Move m : candidates) {
            Board copy = b.copy();
            copy.apply(m);

            if (!isInCheck(copy, mover.color())) {
                legal.add(m);
            }
        }
        return legal;
    }

    public boolean isInCheck(Board b, Color side) {
        Pos kingPos = findKing(b, side);
        if (kingPos == null) throw new IllegalStateException("King not found for " + side);
        return isSquareAttacked(b, kingPos, side.opposite());
    }

    private Pos findKing(Board b, Color side) {
        for (int x = 0; x < Board.W; x++) {
            for (int y = 0; y < Board.H; y++) {
                for (int z = 0; z < Board.L; z++) {
                    Pos p = new Pos(x, y, z);
                    Piece pc = b.get(p);
                    if (pc != null && pc.color() == side && pc.type() == PieceType.KING) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    private boolean isSquareAttacked(Board b, Pos square, Color bySide) {
        // Tüm taşları gez, bySide taşlarının pseudo hamleleri square'e geliyor mu bak.
        for (int x = 0; x < Board.W; x++) {
            for (int y = 0; y < Board.H; y++) {
                for (int z = 0; z < Board.L; z++) {
                    Pos from = new Pos(x, y, z);
                    Piece pc = b.get(from);
                    if (pc == null || pc.color() != bySide) continue;

                    // pawn'ı şimdilik atlıyoruz (Adım 4'te ekleyeceğiz)
                    if (pc.type() == PieceType.PAWN) continue;

                    for (Move m : pseudoLegalMoves(b, from)) {
                        if (m.to().equals(square)) return true;
                    }
                }
            }
        }
        return false;
    }

}
