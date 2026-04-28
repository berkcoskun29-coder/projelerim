package engine;

import java.util.ArrayList;
import java.util.List;

import model.Board;
import model.Player;
import model.Tile;
import model.TileType;

public final class GameEngine {

    public enum Phase { WAIT_ROLL, WAIT_END_TURN }

    private final Board board;
    private final Dice dice = new Dice();

    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex = 0;

    private Phase phase = Phase.WAIT_ROLL;

    // Board'da island index neresi?
    private final int islandIndex = 10; // Board.createDefaultBoard() içinde 10 yapmıştık

    public GameEngine(Board board) {
        this.board = board;
        // default 2 oyuncu
        setPlayerCount(2);
    }

    public Board board() { return board; }
    public List<Player> players() { return players; }
    public Player currentPlayer() { return players.get(currentPlayerIndex); }
    public int currentPlayerIndex() { return currentPlayerIndex; }
    public Phase phase() { return phase; }

    public void setPlayerCount(int count) {
        if (count < 2 || count > 4) throw new IllegalArgumentException("2-4 olmalı");
        players.clear();
        for (int i = 0; i < count; i++) {
            players.add(new Player(i, "P" + (i + 1)));
        }
        currentPlayerIndex = 0;
        phase = Phase.WAIT_ROLL;
    }

    public String rollAndMove() {
        if (phase != Phase.WAIT_ROLL) return "Şu an zar atılamaz.";

        Player p = currentPlayer();

        // Ada kontrolü: adadaysa, tur geçir
        if (p.inIsland()) {
            p.tickIsland();
            phase = Phase.WAIT_END_TURN;
            return p.name() + " adada. Kalan tur: " + p.islandTurnsLeft();
        }

        Dice.Roll r = dice.roll();

        // 3 kere çift zar → adaya sürgün
        if (r.isDouble()) {
            p.setDoublesStreak(p.doublesStreak() + 1);
        } else {
            p.setDoublesStreak(0);
        }

        if (p.doublesStreak() >= 3) {
            p.sendToIsland(islandIndex, 2); // 2 tur beklesin (istersen 3 yaparız)
            phase = Phase.WAIT_END_TURN;
            return p.name() + " 3 kere ÇİFT attı! KAÇIŞ ADASI'na sürüldü (2 tur).";
        }

        // hareket
        int newPos = (p.position() + r.sum()) % board.size();
        p.setPosition(newPos);

        Tile landed = board.get(newPos);

        String baseMsg = p.name() + " zar: " + r + " → " + landed.name() + " (" + landed.type() + ")";
        String rentMsg = handleRentIfNeeded(p);

        phase = Phase.WAIT_END_TURN;

        if (rentMsg != null) {
            return baseMsg + "\n" + rentMsg;
        }
        return baseMsg;

    }

    public String endTurn() {
        if (phase != Phase.WAIT_END_TURN) return "Önce zar atmalısın.";

        // sıradaki oyuncu
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        phase = Phase.WAIT_ROLL;
        return "Sıra: " + currentPlayer().name();
    }

    // Şimdilik tile etkilerini sadece log'luyoruz
    public String describeTileEffect(Player p) {
        Tile tile = board.get(p.position());
        if (tile.type() == TileType.TAX) {
            return "Vergi dairesi: (MVP) ileride mülk değerine göre vergi kesilecek.";
        }
        if (tile.type() == TileType.CHANCE) {
            return "Şans: (MVP) ileride kart çekilecek.";
        }
        if (tile.type() == TileType.RESORT) {
            return "Tatil Köyü: (MVP) ileride sahiplik + 3 köy + havalimanı win.";
        }
        if (tile.type() == TileType.AIRPORT) {
            return "Havalimanı: (MVP) tatil köyü kazanma kontrol noktası.";
        }
        if (tile.type() == TileType.ISLAND) {
            return "Kaçış Adası: Buraya gelince (MVP) ileride ceza/ödeme seçenekleri.";
        }
        return "Şehir/Arsa: (MVP) ileride satın alma/kira.";
    }
    public String buyCurrentTile() {
        Player p = currentPlayer();
        Tile tile = board.get(p.position());

        if (!(tile instanceof model.PropertyTile prop)) {
            return "Burası satın alınabilir bir yer değil.";
        }
        if (prop.isOwned()) {
            return "Bu yer zaten sahipli.";
        }
        if (p.money() < prop.price()) {
            return "Yetersiz para! Fiyat: " + prop.price();
        }

        p.addMoney(-prop.price());
        prop.setOwnerId(p.id());
        return p.name() + " satın aldı: " + prop.name() + " (-" + prop.price() + ")";
    }

    private String handleRentIfNeeded(Player p) {
        Tile tile = board.get(p.position());
        if (!(tile instanceof model.PropertyTile prop)) return null;

        if (!prop.isOwned()) return null;

        // kendi malıysa kira yok
        if (prop.ownerId() == p.id()) return "Kendi mülkün: kira ödemezsin.";

        int rent = prop.rent();
        p.addMoney(-rent);

        Player owner = players.get(prop.ownerId());
        owner.addMoney(rent);

        return p.name() + " kira ödedi: " + rent + " → " + owner.name();
    }

}
