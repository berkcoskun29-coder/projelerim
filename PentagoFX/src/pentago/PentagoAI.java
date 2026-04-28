package pentago;

import java.util.*;

public class PentagoAI {

    public enum Difficulty {
        EASY("Kolay"),
        NORMAL("Orta"),
        HARD("Zor");

        public final String label;
        Difficulty(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public static class Move {
        public int r, c;
        public int quadrant;
        public boolean clockwise;

        public Move(int r, int c, int quadrant, boolean clockwise) {
            this.r = r;
            this.c = c;
            this.quadrant = quadrant;
            this.clockwise = clockwise;
        }
    }

    public static class Advice {
        public final Move move;
        public final String reason;
        public final double score;
        public Advice(Move move, String reason, double score) {
            this.move = move;
            this.reason = reason;
            this.score = score;
        }
    }

    private final Random rnd = new Random();

    public Move chooseMove(int[][] board, int player, Difficulty diff) {
        Advice a = getBestAdvice(board, player, diff);
        return a == null ? null : a.move;
    }

    public List<Advice> getTopAdvices(int[][] board, int player, Difficulty diff, int n) {
        int topKRoot = switch (diff) {
            case EASY -> 10;
            case NORMAL -> 16;
            case HARD -> 24;
        };
        int topKReply = switch (diff) {
            case EASY -> 6;
            case NORMAL -> 12;
            case HARD -> 18;
        };

        List<ScoredMove> candidates = scoreRootMoves(board, player, topKRoot);
        if (candidates.isEmpty()) return List.of();

        // Depth settings: easy = depth1-ish, normal/hard = depth2 minimax
        List<Advice> advices = new ArrayList<>();

        if (diff == Difficulty.EASY) {
            // Choose top by eval only (fast)
            candidates.sort((a, b) -> Double.compare(b.score, a.score));
            for (int i = 0; i < Math.min(n, candidates.size()); i++) {
                Move m = candidates.get(i).move;
                int[][] b1 = apply(board, player, m);
                double sc = evaluate(b1, player);
                String reason = buildReason(board, player, m, b1, diff);
                advices.add(new Advice(m, reason, sc));
            }
            return advices;
        }

        // Depth2 minimax: our move -> opponent best reply
        int opp = 3 - player;

        List<ScoredMove> ranked = new ArrayList<>();
        for (ScoredMove sm : candidates) {
            Move m = sm.move;
            int[][] b1 = apply(board, player, m);

            // if immediate loss after our move, penalize
            double value;
            if (hasFive(b1, opp)) {
                value = -9e6;
            } else if (hasFive(b1, player)) {
                value = 9e6;
            } else {
                List<ScoredMove> replies = scoreRootMoves(b1, opp, topKReply);
                double worst = 1e18;
                if (replies.isEmpty()) {
                    worst = evaluate(b1, player);
                } else {
                    for (ScoredMove rsm : replies) {
                        int[][] b2 = apply(b1, opp, rsm.move);
                        double sc;
                        if (hasFive(b2, opp)) sc = -9e6;
                        else if (hasFive(b2, player)) sc = 9e6;
                        else sc = evaluate(b2, player);
                        if (sc < worst) worst = sc;
                    }
                }
                value = worst;
            }
            ranked.add(new ScoredMove(m, value));
        }

        ranked.sort((a, b) -> Double.compare(b.score, a.score));

        for (int i = 0; i < Math.min(n, ranked.size()); i++) {
            Move m = ranked.get(i).move;
            int[][] b1 = apply(board, player, m);
            String reason = buildReason(board, player, m, b1, diff);
            advices.add(new Advice(m, reason, ranked.get(i).score));
        }
        return advices;
    }

    private Advice getBestAdvice(int[][] board, int player, Difficulty diff) {
        List<Advice> top = getTopAdvices(board, player, diff, 1);
        return top.isEmpty() ? null : top.get(0);
    }

    // ---------------- Reason builder ----------------

    private String buildReason(int[][] before, int player, Move m, int[][] after, Difficulty diff) {
        int opp = 3 - player;

        // 1) winning now?
        if (hasFive(after, player)) return "Bu hamle direkt kazanıyor (5'li tamamlıyor).";

        // 2) was opponent about to win? does this prevent?
        boolean oppHadWin = opponentHasWinningMove(before, opp);
        boolean oppStillHasWin = opponentHasWinningMove(after, opp);
        if (oppHadWin && !oppStillHasWin) return "Rakibin kazanma tehdidini kesiyor (kritik savunma).";
        if (oppHadWin) return "Rakibin tehdidini azaltıyor ama tamamen bitirmeyebilir.";

        // 3) creates strong threat (4-in-window potential)
        int bestMineBefore = bestWindowCount(before, player);
        int bestMineAfter = bestWindowCount(after, player);
        if (bestMineAfter >= 4 && bestMineAfter > bestMineBefore) return "Güçlü tehdit kuruyor (4'lüye yaklaşıyor).";
        if (bestMineAfter > bestMineBefore) return "Konumunu güçlendiriyor (daha iyi çizgi potansiyeli).";

        // 4) center / generic
        return (diff == Difficulty.HARD)
                ? "Uzun vadede en iyi pozisyon değeri (merkez + çizgi dengesi)."
                : "Dengeli hamle (pozisyonu iyileştiriyor).";
    }

    private boolean opponentHasWinningMove(int[][] board, int opp) {
        List<int[]> empties = empties(board);
        for (int[] cell : empties) {
            int r = cell[0], c = cell[1];
            for (int q = 0; q < 4; q++) {
                for (boolean cw : new boolean[]{true, false}) {
                    Move m = new Move(r, c, q, cw);
                    int[][] b1 = apply(board, opp, m);
                    if (hasFive(b1, opp)) return true;
                }
            }
        }
        return false;
    }

    private int bestWindowCount(int[][] b, int p) {
        int best = 0;

        // windows of 5, count p stones if no enemy in window
        // horizontal
        for (int r = 0; r < 6; r++) for (int c = 0; c <= 1; c++)
            best = Math.max(best, windowCount(b, p, r, c, 0, 1));
        // vertical
        for (int c = 0; c < 6; c++) for (int r = 0; r <= 1; r++)
            best = Math.max(best, windowCount(b, p, r, c, 1, 0));
        // diag TL->BR
        for (int r = 0; r <= 1; r++) for (int c = 0; c <= 1; c++)
            best = Math.max(best, windowCount(b, p, r, c, 1, 1));
        // diag TR->BL
        for (int r = 0; r <= 1; r++) for (int c = 4; c < 6; c++)
            best = Math.max(best, windowCount(b, p, r, c, 1, -1));

        return best;
    }

    private int windowCount(int[][] b, int p, int r0, int c0, int dr, int dc) {
        int opp = 3 - p;
        int mine = 0;
        for (int i = 0; i < 5; i++) {
            int v = b[r0 + i*dr][c0 + i*dc];
            if (v == opp) return 0; // blocked
            if (v == p) mine++;
        }
        return mine;
    }

    // ---------------- Candidate scoring ----------------

    private List<ScoredMove> scoreRootMoves(int[][] board, int player, int topK) {
        List<int[]> empties = empties(board);
        if (empties.isEmpty()) return new ArrayList<>();

        // pick best cells first
        List<ScoredCell> cells = new ArrayList<>();
        for (int[] cell : empties) {
            int r = cell[0], c = cell[1];
            double s = 0;

            s += centerScore(r, c);
            s += localPotential(board, player, r, c) * 1.3;
            int opp = 3 - player;
            s += localPotential(board, opp, r, c) * 1.0;

            cells.add(new ScoredCell(r, c, s));
        }
        cells.sort((a, b) -> Double.compare(b.score, a.score));
        int take = Math.min(cells.size(), Math.max(10, topK));
        List<ScoredCell> picked = cells.subList(0, take);

        // generate 8 variants for each cell
        List<ScoredMove> out = new ArrayList<>();
        for (ScoredCell cell : picked) {
            for (int q = 0; q < 4; q++) {
                for (boolean cw : new boolean[]{true, false}) {
                    Move m = new Move(cell.r, cell.c, q, cw);
                    int[][] b1 = apply(board, player, m);
                    double sc = evaluate(b1, player);
                    if (hasFive(b1, player)) sc += 1e6;
                    out.add(new ScoredMove(m, sc));
                }
            }
        }

        // sort, diversify
        out.sort((a, b) -> Double.compare(b.score, a.score));
        return diversify(out, topK);
    }

    private List<ScoredMove> diversify(List<ScoredMove> moves, int topK) {
        Map<String, Integer> perCell = new HashMap<>();
        List<ScoredMove> out = new ArrayList<>();
        for (ScoredMove sm : moves) {
            Move m = sm.move;
            String key = m.r + "," + m.c;
            int cnt = perCell.getOrDefault(key, 0);
            if (cnt >= 3) continue;
            perCell.put(key, cnt + 1);
            out.add(sm);
            if (out.size() >= topK) break;
        }
        return out;
    }

    // ---------------- Apply / rotate ----------------

    private int[][] apply(int[][] board, int player, Move m) {
        int[][] b = copy(board);
        b[m.r][m.c] = player;
        rotate(b, m.quadrant, m.clockwise);
        return b;
    }

    private void rotate(int[][] b, int q, boolean clockwise) {
        int r0 = (q < 2 ? 0 : 3);
        int c0 = (q % 2 == 0 ? 0 : 3);

        int[][] sub = new int[3][3];
        for (int r = 0; r < 3; r++) System.arraycopy(b[r0 + r], c0, sub[r], 0, 3);

        int[][] rotated = new int[3][3];
        if (clockwise) {
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 3; c++)
                    rotated[c][2 - r] = sub[r][c];
        } else {
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 3; c++)
                    rotated[2 - c][r] = sub[r][c];
        }

        for (int r = 0; r < 3; r++) System.arraycopy(rotated[r], 0, b[r0 + r], c0, 3);
    }

    private int[][] copy(int[][] b) {
        int[][] x = new int[6][6];
        for (int r = 0; r < 6; r++) System.arraycopy(b[r], 0, x[r], 0, 6);
        return x;
    }

    private List<int[]> empties(int[][] board) {
        List<int[]> out = new ArrayList<>();
        for (int r = 0; r < 6; r++)
            for (int c = 0; c < 6; c++)
                if (board[r][c] == 0) out.add(new int[]{r, c});
        return out;
    }

    // ---------------- Evaluation ----------------

    private double evaluate(int[][] b, int player) {
        int opp = 3 - player;
        if (hasFive(b, player)) return 9e6;
        if (hasFive(b, opp)) return -9e6;

        double my = linePotential(b, player);
        double op = linePotential(b, opp);
        double cen = centerControl(b, player) - centerControl(b, opp);

        return (my - op) + cen * 0.35;
    }

    private double centerControl(int[][] b, int p) {
        double s = 0;
        int[] centersR = {2, 3};
        int[] centersC = {2, 3};
        for (int r : centersR) for (int c : centersC) if (b[r][c] == p) s += 1.0;
        return s;
    }

    private double linePotential(int[][] b, int p) {
        double s = 0;

        for (int r = 0; r < 6; r++)
            for (int c = 0; c <= 1; c++)
                s += windowScore(b, p, r, c, 0, 1);

        for (int c = 0; c < 6; c++)
            for (int r = 0; r <= 1; r++)
                s += windowScore(b, p, r, c, 1, 0);

        for (int r = 0; r <= 1; r++)
            for (int c = 0; c <= 1; c++)
                s += windowScore(b, p, r, c, 1, 1);

        for (int r = 0; r <= 1; r++)
            for (int c = 4; c < 6; c++)
                s += windowScore(b, p, r, c, 1, -1);

        return s;
    }

    private double windowScore(int[][] b, int p, int r0, int c0, int dr, int dc) {
        int mine = 0, empty = 0, enemy = 0;
        int opp = 3 - p;

        for (int i = 0; i < 5; i++) {
            int v = b[r0 + i * dr][c0 + i * dc];
            if (v == p) mine++;
            else if (v == 0) empty++;
            else enemy++;
        }
        if (enemy > 0) return 0;

        double base = mine * mine;
        if (mine == 4 && empty == 1) base += 18;
        if (mine == 3 && empty == 2) base += 6;
        if (mine == 2 && empty == 3) base += 1.5;
        return base;
    }

    private double localPotential(int[][] board, int p, int r, int c) {
        int[][] tmp = copy(board);
        tmp[r][c] = p;
        return linePotential(tmp, p) * 0.02;
    }

    private double centerScore(int r, int c) {
        int dr = Math.min(Math.abs(r - 2), Math.abs(r - 3));
        int dc = Math.min(Math.abs(c - 2), Math.abs(c - 3));
        return 4 - (dr + dc);
    }

    // ---------------- Win check ----------------

    private boolean hasFive(int[][] b, int p) {
        for (int r = 0; r < 6; r++)
            for (int c = 0; c <= 1; c++)
                if (b[r][c] == p && b[r][c+1] == p && b[r][c+2] == p && b[r][c+3] == p && b[r][c+4] == p) return true;

        for (int c = 0; c < 6; c++)
            for (int r = 0; r <= 1; r++)
                if (b[r][c] == p && b[r+1][c] == p && b[r+2][c] == p && b[r+3][c] == p && b[r+4][c] == p) return true;

        for (int r = 0; r <= 1; r++)
            for (int c = 0; c <= 1; c++)
                if (b[r][c] == p && b[r+1][c+1] == p && b[r+2][c+2] == p && b[r+3][c+3] == p && b[r+4][c+4] == p) return true;

        for (int r = 0; r <= 1; r++)
            for (int c = 4; c < 6; c++)
                if (b[r][c] == p && b[r+1][c-1] == p && b[r+2][c-2] == p && b[r+3][c-3] == p && b[r+4][c-4] == p) return true;

        return false;
    }

    // ---------------- helpers ----------------
    private static class ScoredCell {
        int r, c; double score;
        ScoredCell(int r, int c, double score) { this.r = r; this.c = c; this.score = score; }
    }

    private static class ScoredMove {
        Move move; double score;
        ScoredMove(Move move, double score) { this.move = move; this.score = score; }
    }
}
