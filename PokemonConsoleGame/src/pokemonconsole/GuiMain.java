package pokemonconsole;

public class GuiMain {

    public static void main(String[] args) {
        Player player = new Player("Berk");
        BattleSystem battleSystem = new BattleSystem();
        WorldMap worldMap = new WorldMap(10, 10);

        // Başlangıç pokemonu (ateş tipi)
        Pokemon starter = new Pokemon("Flamemon", PokemonType.FIRE, 5, 35, 12, 8);
        starter.addMove(new Move("Tackle", 35, 100));
        starter.addMove(new Move("Quick Hit", 25, 95));
        starter.addMove(new Move("Fire Bite", 40, 90));
        player.addPokemon(starter);

        // Başlangıç Pokéball
        player.addPokeballs(5);

        GameFrame.launch(player, worldMap, battleSystem);
    }
}
