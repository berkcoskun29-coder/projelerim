package pokemonconsole;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class GameFrame extends JFrame {

    public GameFrame(Player player, WorldMap worldMap, BattleSystem battleSystem) {
        setTitle("Mini Pokemon Harita");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MapPanel mapPanel = new MapPanel(player, worldMap, battleSystem);
        add(mapPanel);

        pack();
        setLocationRelativeTo(null); // ekranın ortası
        setResizable(false);
    }

    public static void launch(Player player, WorldMap worldMap, BattleSystem battleSystem) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame(player, worldMap, battleSystem);
            frame.setVisible(true);
        });
    }
}
