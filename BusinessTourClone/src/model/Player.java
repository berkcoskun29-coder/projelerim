package model;

public class Player {
    private final int id;
    private final String name;

    private int money = 1_000_000; // 1,000,000 başlangıç
    private int position = 0;

    private boolean inIsland = false;
    private int islandTurnsLeft = 0;

    private int doublesStreak = 0;
    private boolean bankrupt = false;

    public Player(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int id() { return id; }
    public String name() { return name; }

    public int money() { return money; }
    public void addMoney(int delta) { money += delta; if (money < 0) bankrupt = true; }

    public int position() { return position; }
    public void setPosition(int pos) { this.position = pos; }

    public boolean inIsland() { return inIsland; }
    public int islandTurnsLeft() { return islandTurnsLeft; }

    public void sendToIsland(int islandIndex, int turns) {
        inIsland = true;
        islandTurnsLeft = turns;
        position = islandIndex;
        doublesStreak = 0;
    }

    public void tickIsland() {
        if (!inIsland) return;
        islandTurnsLeft--;
        if (islandTurnsLeft <= 0) {
            inIsland = false;
            islandTurnsLeft = 0;
        }
    }

    public int doublesStreak() { return doublesStreak; }
    public void setDoublesStreak(int v) { doublesStreak = v; }

    public boolean bankrupt() { return bankrupt; }
}
