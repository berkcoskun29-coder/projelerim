package pokemonconsole;

import java.util.ArrayList;
import java.util.List;

public class Pokemon {
    private String name;
    private PokemonType type;   // 🔥💧🌿
    private int level;
    private int maxHp;
    private int currentHp;
    private int attack;
    private int defense;
    private int exp;
    private int expToNextLevel;
    private List<Move> moves;

    public Pokemon(String name, PokemonType type, int level, int maxHp, int attack, int defense) {
        this.name = name;
        this.type = type;
        this.level = level;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.attack = attack;
        this.defense = defense;
        this.exp = 0;
        this.expToNextLevel = level * 50;
        this.moves = new ArrayList<>();
    }

    public void addMove(Move move) {
        if (moves.size() < 4) {
            moves.add(move);
        }
    }

    public void takeDamage(int damage) {
        currentHp -= damage;
        if (currentHp < 0) {
            currentHp = 0;
        }
    }

    public boolean isFainted() {
        return currentHp <= 0;
    }

    public void healToFull() {
        currentHp = maxHp;
    }

    public void gainExp(int amount) {
        System.out.println(name + " " + amount + " XP kazandı!");
        exp += amount;
        while (exp >= expToNextLevel) {
            levelUp();
        }
    }

    private void levelUp() {
        exp -= expToNextLevel;
        level++;
        expToNextLevel = level * 50;

        maxHp += 5;
        attack += 2;
        defense += 2;
        currentHp = maxHp;

        System.out.println("⚡ " + name + " seviye atladı! Yeni seviye: " + level);
        System.out.println("Yeni istatistikler: HP=" + maxHp + " ATK=" + attack + " DEF=" + defense);
    }

    // --- Getter'lar ---
    public String getName() {
        return name;
    }

    public PokemonType getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public List<Move> getMoves() {
        return moves;
    }

    @Override
    public String toString() {
        String typeText = (type != null) ? type.toString() : "NONE";
        return name + " (Lv." + level + ", " + typeText + ") HP: " + currentHp + "/" + maxHp +
               " ATK:" + attack + " DEF:" + defense;
    }
}
