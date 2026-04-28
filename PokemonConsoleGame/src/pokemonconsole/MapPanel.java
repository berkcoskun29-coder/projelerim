package pokemonconsole;

import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Random;

public class MapPanel extends JPanel {

    private static final int TILE_SIZE = 40;

    private Player player;
    private WorldMap worldMap;
    private BattleSystem battleSystem;
    private Random random;

    public MapPanel(Player player, WorldMap worldMap, BattleSystem battleSystem) {
        this.player = player;
        this.worldMap = worldMap;
        this.battleSystem = battleSystem;
        this.random = new Random();

        int w = worldMap.getWidth() * TILE_SIZE;
        int h = worldMap.getHeight() * TILE_SIZE;
        setPreferredSize(new Dimension(w, h));

        setupKeyBindings();
        setFocusable(true);
    }

    private void setupKeyBindings() {
        // W / A / S / D ve ok tuşları
        bindKey("W", "moveUp",   0, -1);
        bindKey("UP", "moveUpArrow", 0, -1);

        bindKey("S", "moveDown", 0, 1);
        bindKey("DOWN", "moveDownArrow", 0, 1);

        bindKey("A", "moveLeft", -1, 0);
        bindKey("LEFT", "moveLeftArrow", -1, 0);

        bindKey("D", "moveRight", 1, 0);
        bindKey("RIGHT", "moveRightArrow", 1, 0);
    }

    private void bindKey(String key, String actionName, int dx, int dy) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), actionName);
        getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                movePlayer(dx, dy);
            }
        });
    }

    private void movePlayer(int dx, int dy) {
        int newX = player.getX() + dx;
        int newY = player.getY() + dy;

        if (!worldMap.isInsideMap(newX, newY)) {
            // Harita dışı
            JOptionPane.showMessageDialog(this, "Haritanın dışına çıkamazsın!");
            return;
        }

        // Konumu güncelle
        // Player sınıfında doğrudan setX/setY yok, hareket fonksiyonlarını kullanacağız.
        if (dx == 1) {
            player.moveRight();
        } else if (dx == -1) {
            player.moveLeft();
        }
        if (dy == 1) {
            player.moveDown();
        } else if (dy == -1) {
            player.moveUp();
        }

        // Yeni kare
        Tile tile = worldMap.getTile(player.getX(), player.getY());

        // Çimlik ise rastgele savaş
        if (tile != null && tile.isGrass()) {
            int chance = random.nextInt(100); // 0-99
            if (chance < 30) { // %30 ihtimal
                // Rastgele vahşi canavar
                Pokemon wild = PokemonFactory.createRandomWildPokemon();
                JOptionPane.showMessageDialog(this,
                        "Çimlerden bir yaratık fırladı: " + wild.getName() + " (Lv." + wild.getLevel() + ")!\n" +
                        "Savaş konsolda devam edecek.");

                // Konsol tabanlı savaş
                new Thread(() -> {
                    battleSystem.startBattle(player, wild);
                }).start();
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Haritayı çiz
        for (int x = 0; x < worldMap.getWidth(); x++) {
            for (int y = 0; y < worldMap.getHeight(); y++) {
                Tile tile = worldMap.getTile(x, y);
                if (tile != null && tile.isGrass()) {
                    g.setColor(new Color(80, 180, 80)); // çimlik
                } else {
                    g.setColor(new Color(180, 180, 180)); // normal zemin
                }
                int px = x * TILE_SIZE;
                int py = y * TILE_SIZE;
                g.fillRect(px, py, TILE_SIZE, TILE_SIZE);

                // kare sınırı
                g.setColor(Color.DARK_GRAY);
                g.drawRect(px, py, TILE_SIZE, TILE_SIZE);
            }
        }

        // Oyuncu
        int playerX = player.getX() * TILE_SIZE;
        int playerY = player.getY() * TILE_SIZE;
        g.setColor(Color.BLUE);
        g.fillOval(playerX + 5, playerY + 5, TILE_SIZE - 10, TILE_SIZE - 10);
    }
}
