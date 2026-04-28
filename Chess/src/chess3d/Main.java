package chess3d;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Board b = new Board();
        Rules r = new Rules();

        // Beyaz vezir: ortada, etrafı boş
        Pos q = new Pos(3, 3, 1);
        b.set(q, new Piece(PieceType.QUEEN, Color.WHITE));

        // Bazı bloklar / hedef taşlar
        b.set(new Pos(6, 3, 1), new Piece(PieceType.PAWN, Color.WHITE)); // kendi taşı: x+ yönünü bloklar
        b.set(new Pos(3, 6, 1), new Piece(PieceType.PAWN, Color.BLACK)); // rakip: y+ yönünde capture olmalı
        b.set(new Pos(5, 5, 2), new Piece(PieceType.BISHOP, Color.BLACK)); // space diagonal üzerinde capture testi

        List<Move> qm = r.pseudoLegalMoves(b, q);
        System.out.println("Queen moves: " + qm.size());
        qm.stream().limit(30).forEach(System.out::println);

        // Beyaz knight testi
        Pos n = new Pos(4, 4, 1);
        b.set(n, new Piece(PieceType.KNIGHT, Color.WHITE));

        List<Move> nm = r.pseudoLegalMoves(b, n);
        System.out.println("\nKnight moves: " + nm.size());
        nm.forEach(System.out::println);

        // Beyaz king testi
        Pos k = new Pos(0, 0, 0);
        b.set(k, new Piece(PieceType.KING, Color.WHITE));

        List<Move> km = r.pseudoLegalMoves(b, k);
        System.out.println("\nKing moves from corner: " + km.size());
        km.forEach(System.out::println);
    }
}
