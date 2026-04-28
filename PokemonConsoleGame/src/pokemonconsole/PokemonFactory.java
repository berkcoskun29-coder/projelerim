package pokemonconsole;

import java.util.Random;

public class PokemonFactory {
    private static Random random = new Random();

    public static Pokemon createRandomWildPokemon() {
        int r = random.nextInt(3); // 0,1,2
        switch (r) {
            case 0:
                return createFireType("Flameling", 3);
            case 1:
                return createWaterType("Aquabug", 3);
            default:
                return createGrassType("Leafy", 3);
        }
    }

    public static Pokemon createFireType(String name, int level) {
        int maxHp = 30 + level * 3;
        int atk = 12 + level * 2;
        int def = 8 + level;
        Pokemon p = new Pokemon(name, PokemonType.FIRE, level, maxHp, atk, def);
        p.addMove(new Move("Ember", 35, 95));
        p.addMove(new Move("Bite", 30, 100));
        return p;
    }

    public static Pokemon createWaterType(String name, int level) {
        int maxHp = 32 + level * 3;
        int atk = 10 + level * 2;
        int def = 10 + level;
        Pokemon p = new Pokemon(name, PokemonType.WATER, level, maxHp, atk, def);
        p.addMove(new Move("Splash Hit", 30, 100));
        p.addMove(new Move("Water Shot", 35, 90));
        return p;
    }

    public static Pokemon createGrassType(String name, int level) {
        int maxHp = 34 + level * 3;
        int atk = 9 + level * 2;
        int def = 11 + level;
        Pokemon p = new Pokemon(name, PokemonType.GRASS, level, maxHp, atk, def);
        p.addMove(new Move("Leaf Cut", 30, 100));
        p.addMove(new Move("Vine Hit", 35, 95));
        return p;
    }
}
