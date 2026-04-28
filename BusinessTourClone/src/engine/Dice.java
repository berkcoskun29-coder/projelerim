package engine;

import java.util.concurrent.ThreadLocalRandom;

public final class Dice {
    public record Roll(int d1, int d2) {
        public int sum() { return d1 + d2; }
        public boolean isDouble() { return d1 == d2; }
        @Override public String toString() { return d1 + " + " + d2 + " = " + sum() + (isDouble() ? " (ÇİFT)" : ""); }
    }

    public Roll roll() {
        int a = ThreadLocalRandom.current().nextInt(1, 7);
        int b = ThreadLocalRandom.current().nextInt(1, 7);
        return new Roll(a, b);
    }
}
