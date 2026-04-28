import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Random;

public class TurnBasedBattleSwing extends JFrame {

    // ----- Kayıt sistemi (çoklu slot) -----
    private static final String SAVE_FILE_PREFIX = "battle_save_slot_";
    private static final String SAVE_FILE_EXT = ".dat";
    private static final int MAX_SAVE_SLOTS = 3;
 // Mage mana maliyetleri
    private static final int MAGE_MAGIC_MANA_COST = 20;
    private static final int MAGE_SPECIAL_MANA_COST = 40;


    private static String buildFileNameForSlot(int slot) {
        return SAVE_FILE_PREFIX + slot + SAVE_FILE_EXT;
    }

    private String getSaveFileName() {
        return buildFileNameForSlot(currentSaveSlot);
    }

    // Zorluk ve tema
    enum Difficulty {EASY, NORMAL, HARD}
    enum Theme {FOREST, LAVA, NIGHT}

    // Saldırı türleri (enum)
    enum AttackType {QUICK, HEAVY, MAGIC, SPECIAL}

    // --- Model Sınıfı ---
    static class Fighter implements Serializable {
        String name;
        int level;
        int maxHp;
        int hp;
        int exp;
        int expToNext;
        FighterClass fighterClass;
        MonsterForm monsterForm;   // sadece canavarlar için
        Color mainColor;
        Color secondaryColor;
        // Mana (özellikle büyücü için)
        int maxMana = 0;
        int mana = 0;

        // Yetenek / upgrade için
        double dmgMultiplier = 1.0;
        int extraCritChance = 0;
        // Warrior öfke (rage) sistemi
        int rage = 0;   // 0–60 arası gidecek


        // Savaşçı kalkan yeteneği
        int shieldTurnsRemaining = 0;

        // Durum efektleri
        int burnTurns = 0;
        int blindTurns = 0;
        int dodgeBuffTurns = 0;

        // Envanter / buff
        int hpPotions = 0;
        int powerPotions = 0;
        int powerBuffTurns = 0;

        // Özel skill cooldown (sadece oyuncu kullanıyor)
        int specialCooldown = 0;

        public Fighter(String name, int level, int maxHp,
                       FighterClass fighterClass,
                       MonsterForm monsterForm,
                       Color mainColor, Color secondaryColor,
                       boolean isPlayer) {
            this.name = name;
            this.level = level;
            this.maxHp = maxHp;
            this.hp = maxHp;
            this.fighterClass = fighterClass;
            this.monsterForm = monsterForm;
            this.mainColor = mainColor;
            this.secondaryColor = secondaryColor;

            if (isPlayer) {
                this.exp = 0;
                this.expToNext = 50;
            } else {
                this.exp = 0;
                this.expToNext = Integer.MAX_VALUE;
            }
            // Oyuncu büyücü ise başlangıç manası
            if (isPlayer && fighterClass == FighterClass.MAGE) {
                this.maxMana = 100;
                this.mana = this.maxMana;
            }
        }

        public boolean isDead() {
            return hp <= 0;
        }

        public void takeDamage(int dmg) {
            hp -= dmg;
            if (hp < 0) hp = 0;
        }
    }

    private enum FighterClass {
        WARRIOR,
        ROGUE,
        MAGE,
        MONSTER
    }

    private enum MonsterForm {
        GOBLIN,
        SKELETON,
        DEMON,
        DRAGONLING
    }

    // --- Animasyon tipi ---
    private enum AnimationType {
        NONE,
        PLAYER_QUICK,
        PLAYER_HEAVY,
        PLAYER_MAGIC,
        PLAYER_SPECIAL,
        ENEMY_QUICK,
        ENEMY_HEAVY,
        ENEMY_MAGIC
    }

    // Kayıt için
    static class SaveData implements Serializable {
    	 private static final long serialVersionUID = 1L; // <-- EKLENDİ
    	    int version = 1;                                // <-- EKLENDİ

        Fighter player;
        Fighter enemy;
        int stage;
        boolean playerTurn;
        Difficulty difficulty;
        Theme theme;
        int monstersKilled;
        int totalDamageDealt;
        int totalDamageTaken;
        int maxHit;
        int saveSlot;
    }

    // --- Alanlar ---
    private Fighter player;
    private Fighter enemy;
    private int stage = 1;

    private boolean playerTurn = true;
    private final Random rnd = new Random();

    private Difficulty difficulty = Difficulty.NORMAL;
    private Theme theme = Theme.FOREST;

    // istatistikler
    private int monstersKilled = 0;
    private int totalDamageDealt = 0;
    private int totalDamageTaken = 0;
    private int maxHit = 0;

    // aktif kayıt slotu
    private int currentSaveSlot = 1;

    // UI bileşenleri
    private GamePanel gamePanel;
    private JLabel lblPlayerHp;
    private JLabel lblEnemyHp;
    private JLabel lblTurn;
    private JButton btnQuick;
    private JButton btnHeavy;
    private JButton btnMagic;
    private JButton btnSpecial;
    private JButton btnShield;
    private JButton btnItem;
    private JTextArea txtLog;
 // Sağ tarafta detay paneli
    private JTextArea txtHudDetails;


    // Animasyon durumları
    private AnimationType currentAnimation = AnimationType.NONE;
    private int animationStep = 0;
    private int animationMaxStep = 0;
    private boolean isAnimating = false;
 // Animasyon hızı (ms)
    private int animationDelayMs = 40;   // NORMAL hız

    private boolean lastHitWasCrit = false;

    // Hasar yazıları (floating damage text)
    private int floatDmgEnemyValue = 0;
    private int floatDmgEnemyFrames = 0;
    private boolean floatDmgEnemyCrit = false;

    private int floatDmgPlayerValue = 0;
    private int floatDmgPlayerFrames = 0;
    private boolean floatDmgPlayerCrit = false;

    // --- Sprite görselleri ---
    private Image warriorImg;
    private Image rogueImg;
    private Image mageImg;

    // Canavar sprite'ları
    private Image goblinImg;
    private Image skeletonImg;
    private Image demonImg;
    private Image dragonImg;       // mini ejder
    private Image dragonBossImg;   // büyük boss ejder

    // Skill sprite
    private Image meteorImg;

    // --- Constructor ---
    public TurnBasedBattleSwing(boolean loadSaved, int saveSlot, Difficulty diff) {
        this.difficulty = diff;
        this.currentSaveSlot = saveSlot;

        // Sprite'ları yükle
        warriorImg = loadImage("assets/warrior.png");
        rogueImg = loadImage("assets/rogue.png");
        mageImg = loadImage("assets/mage.png");

        // Canavarlar
        goblinImg = loadImage("assets/goblin.png");
        skeletonImg = loadImage("assets/skeleton.png");
        demonImg = loadImage("assets/demon.png");
        dragonImg = loadImage("assets/dragon.png");        // mini ejder
        dragonBossImg = loadImage("assets/dragon_boss.png");   // boss ejder

        // Mage özel skilli için meteor
        meteorImg = loadImage("assets/meteor.png");

        if (loadSaved && loadGame()) {
            // kayıt yüklendi
        } else {
            startNewGame();
        }
        setupUI();
        updateButtonsForClass();
        updateUIState();
    }

    private Image loadImage(String path) {
        // Jar içinden de çalışsın
        java.net.URL url = getClass().getResource("/" + path);
        if (url != null) {
            return new ImageIcon(url).getImage();
        }
        // Kaynak bulunamazsa proje klasöründen dene
        return new ImageIcon(path).getImage();
    }

    // Yeni oyun başlat
    private void startNewGame() {
        stage = 1;
        selectPlayerCharacter();
        createEnemyForStage();
        playerTurn = true;
        isAnimating = false;
        lastHitWasCrit = false;
        monstersKilled = 0;
        totalDamageDealt = 0;
        totalDamageTaken = 0;
        maxHit = 0;
        player.specialCooldown = 0;
    }

    // Arayüz kurulum
    private void setupUI() {
        setTitle("Sıra Tabanlı 2D Dövüş");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        createMenuBar();

        // Üst panel (HP + seviye + sıra etiketi)
        JPanel topWrapper = new JPanel(new BorderLayout());
        JPanel hpPanel = new JPanel(new GridLayout(1, 2));
        lblPlayerHp = new JLabel(getPlayerHudText(), SwingConstants.CENTER);
        lblEnemyHp = new JLabel(getEnemyHudText(), SwingConstants.CENTER);

        lblPlayerHp.setFont(new Font("Arial", Font.BOLD, 14));
        lblEnemyHp.setFont(new Font("Arial", Font.BOLD, 14));

        hpPanel.add(lblPlayerHp);
        hpPanel.add(lblEnemyHp);

        lblTurn = new JLabel("", SwingConstants.CENTER);
        lblTurn.setFont(new Font("Arial", Font.BOLD, 13));
        lblTurn.setForeground(Color.WHITE);
        lblTurn.setOpaque(true);
        lblTurn.setBackground(new Color(30, 30, 60));

        topWrapper.add(hpPanel, BorderLayout.CENTER);
        topWrapper.add(lblTurn, BorderLayout.SOUTH);

        add(topWrapper, BorderLayout.NORTH);

        
     // Orta panel (oyun alanı + sağda HUD)
        gamePanel = new GamePanel();

        // Oyun alanı ve sağ paneli tutacak sarmalayıcı
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.add(gamePanel, BorderLayout.CENTER);

        // Sağdaki HUD paneli
        JPanel hudPanel = new JPanel(new BorderLayout());
        hudPanel.setPreferredSize(new Dimension(220, 0)); // genişliği 220 px olsun
        hudPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.DARK_GRAY));

        // Başlık
        JLabel hudTitle = new JLabel("Detay Paneli", SwingConstants.CENTER);
        hudTitle.setFont(new Font("Arial", Font.BOLD, 14));
        hudTitle.setOpaque(true);
        hudTitle.setBackground(new Color(25, 25, 40));
        hudTitle.setForeground(Color.WHITE);
        hudPanel.add(hudTitle, BorderLayout.NORTH);

        // İçerik (metin)
        txtHudDetails = new JTextArea();
        txtHudDetails.setEditable(false);
        txtHudDetails.setLineWrap(true);
        txtHudDetails.setWrapStyleWord(true);
        txtHudDetails.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtHudDetails.setBackground(new Color(18, 18, 30));
        txtHudDetails.setForeground(new Color(220, 220, 220));
        txtHudDetails.setMargin(new Insets(6, 6, 6, 6));

        // Scroll ekleyelim
        JScrollPane hudScroll = new JScrollPane(txtHudDetails);
        hudPanel.add(hudScroll, BorderLayout.CENTER);

        // Sağ paneli ekle
        centerWrapper.add(hudPanel, BorderLayout.EAST);

        // Orta bölgeye sarmalayıcıyı ekle
        add(centerWrapper, BorderLayout.CENTER);

        // Buff ikonları için tooltip (oyun panelinin üstüne gelince açıklama gözüksün)
        gamePanel.setToolTipText("Buff ikonları: K = Kalkan, D = Hasar Buff, C = Kritik Şansı");


        // Alt panel (butonlar + log)
        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel buttonPanel = new JPanel(new FlowLayout());
        btnQuick = new JButton("Hızlı Saldırı");
        btnHeavy = new JButton("Güçlü Saldırı");
        btnMagic = new JButton("Büyü / Özel");
        btnSpecial = new JButton();
        btnShield = new JButton("Kalkan");
        btnItem = new JButton("İksir Kullan");

        buttonPanel.add(btnQuick);
        buttonPanel.add(btnHeavy);
        buttonPanel.add(btnMagic);
        buttonPanel.add(btnSpecial);
        buttonPanel.add(btnShield);
        buttonPanel.add(btnItem);
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);

        txtLog = new JTextArea(6, 30);
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(txtLog);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        // Buton aksiyonları
        btnQuick.addActionListener(e -> {
            if (isGameOver() || isAnimating) return;
            if (applyStartOfTurnEffects(player, true)) return;
            playerTurnAttack(AttackType.QUICK);
        });

        btnHeavy.addActionListener(e -> {
            if (isGameOver() || isAnimating) return;
            if (applyStartOfTurnEffects(player, true)) return;
            playerTurnAttack(AttackType.HEAVY);
        });

        btnMagic.addActionListener(e -> {
            if (isGameOver() || isAnimating) return;
            if (applyStartOfTurnEffects(player, true)) return;
            playerTurnAttack(AttackType.MAGIC);
        });

        btnSpecial.addActionListener(e -> {
            if (isGameOver() || isAnimating) return;
            if (player.specialCooldown > 0) {
                JOptionPane.showMessageDialog(this,
                        "Bu yetenek beklemede. Kalan tur: " + player.specialCooldown,
                        "Cooldown",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (applyStartOfTurnEffects(player, true)) return;
            playerTurnAttack(AttackType.SPECIAL);
        });

        btnShield.addActionListener(e -> {
            if (isGameOver() || isAnimating) return;
            if (player.fighterClass != FighterClass.WARRIOR) return;
            useShield();
        });

        btnItem.addActionListener(e -> {
            if (isGameOver() || isAnimating) return;
            useItem();
        });

        updateTurnLabel();
        updateButtonsForClass();
        setVisible(true);
    }

    // Menü bar
    private void createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu gameMenu = new JMenu("Oyun");
        JMenuItem newGame = new JMenuItem("Yeni Oyun");
        JMenuItem loadGameItem = new JMenuItem("Yükle...");
        JMenuItem saveGameItem = new JMenuItem("Kaydet");
        JMenuItem stats = new JMenuItem("Karakter Bilgisi");
        JMenuItem exit = new JMenuItem("Çıkış");

        newGame.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(
                    this,
                    "Yeni oyuna başlamak istiyor musun?",
                    "Yeni Oyun",
                    JOptionPane.YES_NO_OPTION
            );
            if (opt == JOptionPane.YES_OPTION) {
                int slot = chooseSaveSlot("Yeni oyun için kayıt slotu seç:");
                if (slot == -1) return;
                currentSaveSlot = slot;
                chooseDifficulty();
                resetGame();
                txtLog.setText("");
                appendLog("Slot " + currentSaveSlot + " üzerinde yeni oyun başlatıldı.");
            }
        });

        loadGameItem.addActionListener(e -> {
            int slot = chooseSaveSlot("Yüklenecek slotu seç:");
            if (slot == -1) return;
            currentSaveSlot = slot;
            if (!loadGame()) {
                JOptionPane.showMessageDialog(this,
                        "Bu slotta kayıt bulunamadı.",
                        "Yükleme Hatası",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            txtLog.setText("");
            appendLog("Slot " + currentSaveSlot + " yüklendi.");
            isAnimating = false;
            currentAnimation = AnimationType.NONE;
            animationStep = 0;
            animationMaxStep = 0;
            updateButtonsForClass();
            updateUIState();
            updateTurnLabel();
        });
        saveGameItem.addActionListener(e -> {
            saveGame();
            JOptionPane.showMessageDialog(this,
                    "Oyun kaydedildi. (Slot " + currentSaveSlot + ")",
                    "Kayıt",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        stats.addActionListener(e -> showStatsDialog());
        exit.addActionListener(e -> System.exit(0));

        gameMenu.add(newGame);
        gameMenu.add(loadGameItem);
        gameMenu.add(saveGameItem);
        gameMenu.add(stats);
        gameMenu.addSeparator();
        gameMenu.add(exit);

        JMenu settingsMenu = new JMenu("Ayarlar");
        JMenu themeMenu = new JMenu("Tema");
        JMenuItem forest = new JMenuItem("Orman");
        JMenuItem lava = new JMenuItem("Lav");
        JMenuItem night = new JMenuItem("Gece");
        
     // Animasyon hızı menüsü
        JMenu speedMenu = new JMenu("Animasyon Hızı");
        JMenuItem speedSlow = new JMenuItem("Yavaş");
        JMenuItem speedNormal = new JMenuItem("Normal");
        JMenuItem speedFast = new JMenuItem("Hızlı");


        forest.addActionListener(e -> {
            theme = Theme.FOREST;
            gamePanel.repaint();
        });
        lava.addActionListener(e -> {
            theme = Theme.LAVA;
            gamePanel.repaint();
        });
        night.addActionListener(e -> {
            theme = Theme.NIGHT;
            gamePanel.repaint();
        });
     // --- Animasyon hızı ayarları ---
        speedSlow.addActionListener(e -> {
            animationDelayMs = 60; // daha yavaş
            appendLog("[Ayar] Animasyon hızı: Yavaş");
        });
        speedNormal.addActionListener(e -> {
            animationDelayMs = 40; // varsayılan
            appendLog("[Ayar] Animasyon hızı: Normal");
        });
        speedFast.addActionListener(e -> {
            animationDelayMs = 20; // daha hızlı
            appendLog("[Ayar] Animasyon hızı: Hızlı");
        });

        themeMenu.add(forest);
        themeMenu.add(lava);
        themeMenu.add(night);

        // Animasyon hızı alt menüsüne item’leri ekle
        speedMenu.add(speedSlow);
        speedMenu.add(speedNormal);
        speedMenu.add(speedFast);

        // Ayarlar menüsüne ekle
        settingsMenu.add(themeMenu);
        settingsMenu.add(speedMenu);

        bar.add(gameMenu);
        bar.add(settingsMenu);


        setJMenuBar(bar);
    }

    // Slot seçimi
    private int chooseSaveSlot(String title) {
        String[] options = {"Slot 1", "Slot 2", "Slot 3", "İptal"};
        int c = JOptionPane.showOptionDialog(
                this,
                "Kayıt slotu seç:",
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (c == 3 || c == JOptionPane.CLOSED_OPTION) return -1;
        return c + 1;
    }

    // Zorluk seçimi
    private void chooseDifficulty() {
        String[] options = {"Kolay", "Normal", "Zor"};
        int c = JOptionPane.showOptionDialog(
                this,
                "Zorluk seviyesi seç:",
                "Zorluk",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[1]
        );
        if (c == 0) difficulty = Difficulty.EASY;
        else if (c == 2) difficulty = Difficulty.HARD;
        else difficulty = Difficulty.NORMAL;
    }

    // In-game reset
    private void resetGame() {
        startNewGame();
        txtLog.setText("");
        currentAnimation = AnimationType.NONE;
        animationStep = 0;
        animationMaxStep = 0;
        playerTurn = true;
        isAnimating = false;
        updateButtonsForClass();
        btnQuick.setEnabled(true);
        btnHeavy.setEnabled(true);
        btnMagic.setEnabled(true);
        btnSpecial.setEnabled(true);
        btnShield.setEnabled(true);
        btnItem.setEnabled(true);
        updateUIState();
        updateTurnLabel();
    }

    // Karakter seçimi
    private void selectPlayerCharacter() {
        String[] options = {"Savaşçı", "Suikastçı", "Büyücü"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Karakterini seç:",
                "Karakter Seçimi",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice < 0) choice = 0;

        switch (choice) {
            case 0 -> player = new Fighter(
                    "Savaşçı",
                    1,
                    140,
                    FighterClass.WARRIOR,
                    null,
                    new Color(70, 140, 255),
                    new Color(180, 210, 255),
                    true
            );
            case 1 -> player = new Fighter(
                    "Suikastçı",
                    1,
                    105,
                    FighterClass.ROGUE,
                    null,
                    new Color(140, 220, 140),
                    new Color(200, 255, 200),
                    true
            );
            case 2 -> player = new Fighter(
                    "Büyücü",
                    1,
                    95,
                    FighterClass.MAGE,
                    null,
                    new Color(170, 120, 255),
                    new Color(220, 200, 255),
                    true
            );
        }
    }

    // Canavar oluştur
    private void createEnemyForStage() {
        int baseHp = 90 + (stage - 1) * 25;

        // zorluk etkisi
        double hpMul = 1.0;
        switch (difficulty) {
            case EASY -> hpMul = 0.85;
            case NORMAL -> hpMul = 1.0;
            case HARD -> hpMul = 1.25;
        }
        baseHp = (int) Math.round(baseHp * hpMul);

        boolean isBoss = (stage % 5 == 0);
        if (isBoss) {
            baseHp = (int) (baseHp * 1.8);
        }

        MonsterForm form;
        Color main, sec;

        switch (stage % 4) {
            case 1 -> {
                form = MonsterForm.GOBLIN;
                main = new Color(60, 170, 60);
                sec = new Color(200, 255, 200);
            }
            case 2 -> {
                form = MonsterForm.SKELETON;
                main = new Color(210, 210, 210);
                sec = new Color(240, 240, 240);
            }
            case 3 -> {
                form = MonsterForm.DRAGONLING;
                main = new Color(120, 180, 255);
                sec = new Color(200, 230, 255);
            }
            default -> {
                form = MonsterForm.DEMON;
                main = new Color(230, 60, 60);
                sec = Color.WHITE;
            }
        }

        String name = getMonsterName(form);
        if (isBoss) name = "BOSS: " + name;

        enemy = new Fighter(
                name,
                stage,
                baseHp,
                FighterClass.MONSTER,
                form,
                main,
                sec,
                false
        );
    }

    private String getMonsterName(MonsterForm form) {
        return switch (form) {
            case GOBLIN -> "Goblin Lv" + stage;
            case SKELETON -> "İskelet Lv" + stage;
            case DRAGONLING -> "Mini Ejder Lv" + stage;
            case DEMON -> "İblis Lv" + stage;
        };
    }
 // Bu savaşçı bir boss mu? (isim "BOSS:" ile başlıyorsa)
    private boolean isBoss(Fighter f) {
        return f != null
                && f.name != null
                && f.name.startsWith("BOSS:");
    }

    
 // Stage'e göre chapter numarası (1, 2, 3 ...)
    private int getChapterIndex(int s) {
        if (s <= 10) return 1;      // 1–10
        else if (s <= 20) return 2; // 11–20
        else return 3;              // 21+
    }

    // Stage'e göre chapter ismi
    private String getChapterName(int s) {
        int ch = getChapterIndex(s);
        return switch (ch) {
            case 1 -> "Orman Diyarı";
            case 2 -> "Lav Mağaraları";
            case 3 -> "Karanlık Gece Diyarı";
            default -> "Bilinmeyen Bölge";
        };
    }

    // Chapter geçişlerinde gösterilecek mini hikâye
    private String getChapterIntro(int chapterIndex) {
        return switch (chapterIndex) {
            case 1 -> """
                      Orman Diyarı

                      Sessiz bir ormana adım atıyorsun.
                      Ağaçların arasında goblin ve iskeletlerin saklandığı söyleniyor...
                      """;
            case 2 -> """
                      Lav Mağaraları

                      Derinlere indikçe sıcaklık artıyor.
                      Lav göllerinin yanında iblisler ve küçük ejderler dolaşıyor...
                      """;
            case 3 -> """
                      Karanlık Gece Diyarı

                      Gökyüzü tamamen kararmış.
                      En tehlikeli yaratıkların geceleri avlandığı bu diyarda hayatta kalmak zor olacak...
                      """;
            default -> "Yeni bir bölgeye geçtin.";
        };
    }


    // HUD metinleri
    private String getPlayerHudText() {
        if (player == null) return "";
        return player.name + " (Lv " + player.level + ")  HP: "
                + player.hp + "/" + player.maxHp +
                "  XP: " + player.exp + "/" + player.expToNext;
    }

    private String getEnemyHudText() {
        if (enemy == null) return "";
        return "Bölüm " + stage + " - " + enemy.name +
                "  HP: " + enemy.hp + "/" + enemy.maxHp;
    }

 // Sağdaki detay panelini güncelle
    private void updateHudPanel() {
        if (txtHudDetails == null || player == null) return;

        StringBuilder sb = new StringBuilder();

        sb.append("Sınıf: ").append(player.fighterClass).append("\n");
        sb.append("Seviye: ").append(player.level).append("\n");
        sb.append("Bölüm : ").append(stage).append("\n\n");
        sb.append("Bölge : ").append(getChapterName(stage)).append("\n\n");

        sb.append("HP   : ").append(player.hp).append(" / ").append(player.maxHp).append("\n");
        if (player.fighterClass == FighterClass.MAGE) {
            sb.append("Mana : ").append(player.mana).append(" / ").append(player.maxMana).append("\n");
        }
        sb.append("XP   : ").append(player.exp).append(" / ").append(player.expToNext).append("\n\n");

        sb.append("Hasar Çarpanı : x")
          .append(Math.round(player.dmgMultiplier * 100.0) / 100.0).append("\n");
        sb.append("Ek Kritik Şans: +")
          .append(player.extraCritChance).append("%\n\n");

        sb.append("Aktif Buff / Debuff:\n");
        if (player.shieldTurnsRemaining > 0)
            sb.append(" - Kalkan: ").append(player.shieldTurnsRemaining).append(" tur\n");
        if (player.powerBuffTurns > 0)
            sb.append(" - Güç Buff: ").append(player.powerBuffTurns).append(" tur\n");
        if (player.burnTurns > 0)
            sb.append(" - Yanma: ").append(player.burnTurns).append(" tur\n");
        if (player.blindTurns > 0)
            sb.append(" - Körlük: ").append(player.blindTurns).append(" tur\n");

        txtHudDetails.setText(sb.toString());
        txtHudDetails.setCaretPosition(0);
    }

    // Sınıfa göre butonların ayarlanması + cooldown text
    private void updateButtonsForClass() {
        if (btnSpecial == null || btnShield == null || player == null) return;

        if (player.fighterClass == FighterClass.WARRIOR) {
            btnShield.setVisible(true);
            btnShield.setEnabled(!isAnimating && playerTurn);
        } else {
            btnShield.setVisible(false);
        }

        updateCooldownTexts();
    }

    private void updateCooldownTexts() {
        if (btnSpecial == null || player == null) return;

        String baseText;
        switch (player.fighterClass) {
            case WARRIOR -> baseText = "Özel: Berserker Darbesi";
            case ROGUE -> baseText = "Özel: Gölge Bıçağı";
            case MAGE -> baseText = "Özel: Meteor";
            default -> baseText = "Özel Saldırı";
        }

        if (player.specialCooldown > 0) {
            btnSpecial.setText(baseText + " (" + player.specialCooldown + ")");
            btnSpecial.setEnabled(false);
        } else {
            btnSpecial.setText(baseText);
            if (!isAnimating && playerTurn) {
                btnSpecial.setEnabled(true);
            }
        }
    }

    private void updateTurnLabel() {
        if (lblTurn == null) return;
        String turnText = playerTurn ? "Sıra: Oyuncu" : "Sıra: Canavar";
        lblTurn.setText(turnText + "   |   Zorluk: " + difficulty);
    }

    // Oyuncu kalkan kullanır
    private void useShield() {
        if (player.fighterClass != FighterClass.WARRIOR) return;
        player.shieldTurnsRemaining = 3;
        appendLog("Savaşçı kalkanını kaldırdı! 3 tur boyunca %50 daha az hasar alacaksın.");
        updateUIState();
        playerTurn = false;
        updateTurnLabel();
        enemyTurnWithAnimation();
    }

    // Envanter kullanımı
    private void useItem() {
        String info = "HP İksiri: " + player.hpPotions + "\n" +
                "Güç İksiri: " + player.powerPotions + "\n\nBirini seç:";
        String[] options = {"HP İksiri", "Güç İksiri", "Vazgeç"};
        int c = JOptionPane.showOptionDialog(
                this,
                info,
                "İksir Kullan",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[2]
        );
        if (c == 0) {
            if (player.hpPotions <= 0) {
                JOptionPane.showMessageDialog(this, "HP iksirin yok.");
                return;
            }
            player.hpPotions--;
            int heal = (int) (player.maxHp * 0.35);
            int oldHp = player.hp;
            player.hp = Math.min(player.maxHp, player.hp + heal);
            appendLog("HP İksiri kullandın! Canın " + oldHp + " → " + player.hp);
            updateUIState();
            playerTurn = false;
            updateTurnLabel();
            enemyTurnWithAnimation();
        } else if (c == 1) {
            if (player.powerPotions <= 0) {
                JOptionPane.showMessageDialog(this, "Güç iksirin yok.");
                return;
            }
            player.powerPotions--;
            player.powerBuffTurns = 3;
            appendLog("Güç İksiri kullandın! 3 tur boyunca +%20 ek hasar.");
            updateUIState();
        }
    }

    // Tur başı durum efektleri
    // true dönerse, savaş bitmiş demektir
    private boolean applyStartOfTurnEffects(Fighter actor, boolean isPlayerActor) {
        boolean died = false;

        if (actor.burnTurns > 0) {
            int burnDmg = 5 + stage * 2;
            actor.burnTurns--;
            actor.takeDamage(burnDmg);
            appendLog(actor.name + " yanıyor ve " + burnDmg + " hasar alıyor.");
            if (actor.isDead()) died = true;
        }

        if (died) {
            updateUIState();
            if (isPlayerActor) {
                handlePlayerDefeated();
            } else {
                handleEnemyDefeated();
            }
            return true;
        }

        updateUIState();
        return false;
    }

 // Oyuncu saldırı başlat
    private void playerTurnAttack(AttackType attackType) {
        if (!playerTurn || isAnimating || isGameOver()) return;

        // --- Mage için mana kontrolü ---
        if (player.fighterClass == FighterClass.MAGE) {
            if (attackType == AttackType.MAGIC) {
                if (player.mana < MAGE_MAGIC_MANA_COST) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Yeterli mana yok! (Gerekli: " + MAGE_MAGIC_MANA_COST +
                                    ", Mevcut: " + player.mana + ")",
                            "Mana Yetersiz",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                } else {
                    int old = player.mana;
                    player.mana -= MAGE_MAGIC_MANA_COST;
                    appendLog("Büyü için " + MAGE_MAGIC_MANA_COST +
                            " mana harcadın. (" + old + " → " + player.mana + ")");
                    updateUIState();
                }
            } else if (attackType == AttackType.SPECIAL) {
                if (player.mana < MAGE_SPECIAL_MANA_COST) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Özel yetenek için yeterli mana yok!\n" +
                                    "Gerekli: " + MAGE_SPECIAL_MANA_COST +
                                    ", Mevcut: " + player.mana,
                            "Mana Yetersiz",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                } else {
                    int old = player.mana;
                    player.mana -= MAGE_SPECIAL_MANA_COST;
                    appendLog("Özel yetenek için " + MAGE_SPECIAL_MANA_COST +
                            " mana harcadın. (" + old + " → " + player.mana + ")");
                    updateUIState();
                }
            }
        }

        // Özel skill kullanıldıysa cooldown başlasın (3 tur)
        if (attackType == AttackType.SPECIAL) {
            player.specialCooldown = 3;
            updateCooldownTexts();
        }

        performAttackWithAnimation(player, enemy, attackType, true, () -> {
            if (!isGameOver()) {
                playerTurn = false;
                updateTurnLabel();
                enemyTurnWithAnimation();
            }
        });
    }


    // Canavar saldırı
    private void enemyTurnWithAnimation() {
        if (isGameOver()) {
            playerTurn = true;
            updateTurnLabel();
            return;
        }

        if (applyStartOfTurnEffects(enemy, false)) return;

     // Skeleton için kaçınma buff'ı şansı
     if (enemy != null && enemy.monsterForm == MonsterForm.SKELETON) {
         if (enemy.dodgeBuffTurns <= 0 && rnd.nextInt(100) < 15) { // %15 ihtimal
             enemy.dodgeBuffTurns = 2;
             appendLog(enemy.name + " savunma pozisyonu aldı! 2 tur boyunca vurması daha zor.");
         }
     }

     // 3 farklı saldırıdan birini seç (ve bazen boss ulti)
     AttackType enemyChoice;
     boolean enemyIsBoss = isBoss(enemy);
     if (enemyIsBoss && rnd.nextInt(100) < 20) { // %20 ulti şansı
         enemyChoice = AttackType.SPECIAL;
     } else {
         int r = rnd.nextInt(3);
         if (r == 0) enemyChoice = AttackType.QUICK;
         else if (r == 1) enemyChoice = AttackType.HEAVY;
         else enemyChoice = AttackType.MAGIC;
     }


        performAttackWithAnimation(enemy, player, enemyChoice, false, () -> {
            if (!isGameOver()) {
                startPlayerTurn();
            }
        });
    }

    /// Yeni tur başlarken yapılacaklar (oyuncu tarafı)
    private void startPlayerTurn() {
        playerTurn = true;

        // cooldown 1 azalır
        if (player.specialCooldown > 0) {
            player.specialCooldown--;
        }

        // Mage için mana yenileme
        if (player.fighterClass == FighterClass.MAGE) {
            int regen = 8; // her tur +8 mana
            int oldMana = player.mana;
            player.mana = Math.min(player.maxMana, player.mana + regen);
            if (player.mana > oldMana) {
                appendLog("Mana yenilendi: " + oldMana + " → " + player.mana);
            }
        }

        isAnimating = false;

        btnQuick.setEnabled(true);
        btnHeavy.setEnabled(true);
        btnMagic.setEnabled(true);
        btnItem.setEnabled(true);
        updateButtonsForClass();   // özel + kalkan
        updateTurnLabel();
        updateUIState();
    

        
        saveGame(); 
    }

    // Ortak animasyonlu saldırı metodu
    private void performAttackWithAnimation(Fighter attacker, Fighter defender,
                                           AttackType attackType, boolean isPlayer,
                                           Runnable afterAnimation) {

        isAnimating = true;
        btnQuick.setEnabled(false);
        btnHeavy.setEnabled(false);
        btnMagic.setEnabled(false);
        btnSpecial.setEnabled(false);
        btnShield.setEnabled(false);
        btnItem.setEnabled(false);

        if (isPlayer) {
            currentAnimation = switch (attackType) {
                case QUICK -> AnimationType.PLAYER_QUICK;
                case HEAVY -> AnimationType.PLAYER_HEAVY;
                case MAGIC -> AnimationType.PLAYER_MAGIC;
                case SPECIAL -> AnimationType.PLAYER_SPECIAL;
            };
        } else {
            currentAnimation = switch (attackType) {
                case QUICK -> AnimationType.ENEMY_QUICK;
                case HEAVY -> AnimationType.ENEMY_HEAVY;
                case MAGIC, SPECIAL -> AnimationType.ENEMY_MAGIC;
            };
        }

        animationStep = 0;
        animationMaxStep = 24;   // daha yumuşak animasyon

        Timer timer = new Timer(animationDelayMs, null);
        timer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animationStep++;
                if (animationStep >= animationMaxStep) {
                    timer.stop();
                    currentAnimation = AnimationType.NONE;
                    gamePanel.repaint();

                    // Hasar / log
                    doAttack(attacker, defender, attackType, isPlayer);
                    updateUIState();

                    // Ölüm kontrol
                    if (player.isDead()) {
                        handlePlayerDefeated();
                        isAnimating = false;
                        return;
                    }
                    if (defender == enemy && enemy.isDead()) {
                        handleEnemyDefeated();
                        isAnimating = false;
                        return;
                    }

                    isAnimating = false;
                    if (!isGameOver() && afterAnimation != null) {
                        afterAnimation.run();
                    }
                } else {
                    gamePanel.repaint();
                }
            }
        });

        timer.start();
    }
    
 // Hasar / kritik hesap + boss ulti + demon burn + goblin double + dodge
    private void doAttack(Fighter attacker, Fighter defender,
                          AttackType type, boolean isPlayer) {

        int hitChance;
        int minDmg;
        int maxDmg;

        // --- 1) SPECIAL mi? (Oyuncu özel + Boss ulti) ---
        if (type == AttackType.SPECIAL) {
            if (isPlayer) {
                // Oyuncunun sınıfa özel skill'leri
                switch (player.fighterClass) {
                    case WARRIOR -> {
                        hitChance = 70;
                        minDmg = 28;
                        maxDmg = 45;
                    }
                    case ROGUE -> {
                        hitChance = 95;
                        minDmg = 16;
                        maxDmg = 30;
                    }
                    case MAGE -> {
                        hitChance = 85;
                        minDmg = 22;
                        maxDmg = 38;
                    }
                    default -> {
                        hitChance = 80;
                        minDmg = 20;
                        maxDmg = 35;
                    }
                }
            } else {
                // DÜŞMAN SPECIAL → Boss ulti
                hitChance = 70;
                minDmg  = 26;
                maxDmg  = 40;
            }
        }
        // --- 2) Normal saldırılar ---
        else {
            switch (type) {
                case QUICK -> {
                    hitChance = 90;
                    minDmg = 8;
                    maxDmg = 15;
                }
                case HEAVY -> {
                    hitChance = 55;
                    minDmg = 18;
                    maxDmg = 30;
                }
                case MAGIC -> {
                    hitChance = 75;
                    minDmg = 12;
                    maxDmg = 22;
                }
                default -> {
                    hitChance = 80;
                    minDmg = 10;
                    maxDmg = 20;
                }
            }
        }

        // --- 3) Sınıf bonusu ---
        double classBonus = 1.0;
        if (isPlayer) {
            if (player.fighterClass == FighterClass.WARRIOR && type == AttackType.HEAVY)
                classBonus = 1.15;
            else if (player.fighterClass == FighterClass.ROGUE && type == AttackType.QUICK)
                classBonus = 1.15;
            else if (player.fighterClass == FighterClass.MAGE && type == AttackType.MAGIC)
                classBonus = 1.20;
        }

        // --- 4) Zorluk: canavar saldırıyorsa damage bonusu ---
        double diffDmgMul = 1.0;
        if (!isPlayer) {
            switch (difficulty) {
                case EASY -> diffDmgMul = 0.85;
                case NORMAL -> diffDmgMul = 1.0;
                case HARD -> diffDmgMul = 1.2;
            }
        }

        // --- 5) Körlük (blind) aktördeyse isabet düşsün ---
        if (attacker.blindTurns > 0) {
            hitChance -= 20;
            if (hitChance < 10) hitChance = 10;
            attacker.blindTurns--;
            appendLog(attacker.name + " kör durumda, isabet şansı azaldı!");
        }

        // --- 6) Defender üzerinde dodge buff varsa isabet düşsün ---
        if (defender.dodgeBuffTurns > 0) {
            hitChance -= 15;
            if (hitChance < 10) hitChance = 10;
            defender.dodgeBuffTurns--;
            appendLog(defender.name + " çevik duruşta, vurmak zorlaşıyor!");
        }

        // --- 7) Iskalama kontrolü ---
        int roll = rnd.nextInt(100) + 1;
        if (roll > hitChance) {
            lastHitWasCrit = false;
            appendLog(attacker.name + " " + getAttackNameFull(type, isPlayer) + " kaçırdı!");
            return;
        }

        // --- 8) Temel hasar + bufflar ---
        int base = minDmg + rnd.nextInt(maxDmg - minDmg + 1);
        double dmgDouble = base * classBonus * diffDmgMul;

        // kalıcı dmg buff
        if (isPlayer) {
            dmgDouble *= player.dmgMultiplier;
        }
        // güç iksiri buff
        if (isPlayer && player.powerBuffTurns > 0) {
            dmgDouble *= 1.2;
            player.powerBuffTurns--;
        }
        // Warrior öfke (rage) bonusu
        if (isPlayer && player.fighterClass == FighterClass.WARRIOR && player.rage > 0) {
            double rageBonus = 1.0 + (player.rage / 100.0); // rage 50 → +%50 dmg
            dmgDouble *= rageBonus;
            appendLog("Öfken saldırına güç katıyor! (Rage: " + player.rage + ")");
        }


        int dmg = (int) Math.round(dmgDouble);

        // --- 9) Kritik vuruş ---
        int critChance = isPlayer ? 10 : 6;
        if (isPlayer) {
            if (player.fighterClass == FighterClass.ROGUE && type == AttackType.QUICK) critChance += 12;
            if (player.fighterClass == FighterClass.WARRIOR && type == AttackType.HEAVY) critChance += 8;
            if (player.fighterClass == FighterClass.MAGE && type == AttackType.MAGIC) critChance += 10;
            if (type == AttackType.SPECIAL) critChance += 15;
            critChance += player.extraCritChance;
        } else {
            if (difficulty == Difficulty.HARD) critChance += 5;
        }

        int critRoll = rnd.nextInt(100) + 1;
        if (critRoll <= critChance) {
            lastHitWasCrit = true;
            dmg = (int) Math.round(dmg * 1.7);
            appendLog(">>> KRİTİK VURUŞ! <<<");
        } else {
            lastHitWasCrit = false;
        }

        // --- 10) Savaşçı kalkanı ---
        if (defender == player && player.shieldTurnsRemaining > 0) {
            int old = dmg;
            dmg = (int) Math.round(dmg * 0.5);
            player.shieldTurnsRemaining--;
            appendLog("[Kalkan] " + old + " hasar %50 azaltıldı → " + dmg +
                    " (Kalan kalkan turu: " + player.shieldTurnsRemaining + ")");
        }

        // --- 11) Hasarı uygula ---
        defender.takeDamage(dmg);
        // Warrior oyuncu hasar aldığında rage biriksin
        if (defender == player && player.fighterClass == FighterClass.WARRIOR) {
            int gained = Math.max(1, dmg / 2);               // hasarın yarısı kadar rage
            int oldRage = player.rage;
            player.rage = Math.min(60, player.rage + gained); // 60'da sınırla
            appendLog("Aldığın darbe öfkeni arttırdı: " + oldRage + " → " + player.rage);
        }


        // --- 12) İstatistik ve floating damage ---
        if (defender == enemy) {
            totalDamageDealt += dmg;
            if (dmg > maxHit) maxHit = dmg;
            floatDmgEnemyValue = dmg;
            floatDmgEnemyFrames = 25;
            floatDmgEnemyCrit = lastHitWasCrit;
        } else {
            totalDamageTaken += dmg;
            floatDmgPlayerValue = dmg;
            floatDmgPlayerFrames = 25;
            floatDmgPlayerCrit = lastHitWasCrit;
        }

        // --- 13) Demon'un ek burn yeteneği (düşman MAGIC) ---
        if (!isPlayer && attacker.monsterForm == MonsterForm.DEMON && type == AttackType.MAGIC) {
            if (rnd.nextInt(100) < 30) { // %30 ihtimal
                defender.burnTurns += 2;
                appendLog(attacker.name + " cehennem alevleri saçtı! " +
                        defender.name + " 2 tur boyunca yanacak.");
            }
        }

        // --- 14) Oyuncu SPECIAL → Mage burn, Rogue blind ---
        if (isPlayer && type == AttackType.SPECIAL) {
            if (player.fighterClass == FighterClass.MAGE) {
                defender.burnTurns += 2;
                appendLog(defender.name + " meteor ile yanıyor! (2 tur)");
            } else if (player.fighterClass == FighterClass.ROGUE) {
                defender.blindTurns += 2;
                appendLog(defender.name + " gölge bıçağı ile kör oldu! (2 tur)");
            }
        }

        // --- 15) Goblin'in çifte vuruşu (düşman QUICK) ---
        if (!isPlayer && attacker.monsterForm == MonsterForm.GOBLIN && type == AttackType.QUICK) {
            if (rnd.nextInt(100) < 20) { // %20 ihtimal
                int extra = Math.max(1, (int) Math.round(dmg * 0.6)); // ana hasarın ~%60'ı
                defender.takeDamage(extra);
                totalDamageTaken += extra;
                appendLog(attacker.name + " hızlı bir ikinci vuruş yaptı! +" +
                        extra + " ek hasar.");

                // İkinci vuruş için küçük floating damage
                if (defender == player) {
                    floatDmgPlayerValue = extra;
                    floatDmgPlayerFrames = 20;
                    floatDmgPlayerCrit = false;
                } else {
                    floatDmgEnemyValue = extra;
                    floatDmgEnemyFrames = 20;
                    floatDmgEnemyCrit = false;
                }
            }
        }

        // --- 16) Genel log ---
        appendLog(attacker.name + " " + getAttackNameFull(type, isPlayer) +
                " kullandı! " + defender.name + " " + dmg + " hasar aldı.");
        // Warrior SPECIAL sonrası rage'i boşalt
        if (isPlayer && player.fighterClass == FighterClass.WARRIOR && type == AttackType.SPECIAL) {
            if (player.rage > 0) {
                appendLog("Berserker Darbesi ile tüm öfkeni boşalttın! (Rage 0landı)");
                player.rage = 0;
            }
        }

    }



    private String getAttackNameShort(AttackType type) {
        return switch (type) {
            case QUICK -> "Hızlı Saldırı";
            case HEAVY -> "Güçlü Saldırı";
            case MAGIC -> "Büyü";
            case SPECIAL -> "Özel Saldırı";
        };
    }

    private String getAttackNameFull(AttackType type, boolean isPlayerAttack) {
        if (type == AttackType.SPECIAL && isPlayerAttack) {
            return switch (player.fighterClass) {
                case WARRIOR -> "Berserker Darbesi";
                case ROGUE -> "Gölge Bıçağı";
                case MAGE -> "Meteor";
                default -> "Özel Saldırı";
            };
        }
        return getAttackNameShort(type);
    }

    // UI güncelleme + kayıt
    private void updateUIState() {
        if (lblPlayerHp != null) lblPlayerHp.setText(getPlayerHudText());
        if (lblEnemyHp != null) lblEnemyHp.setText(getEnemyHudText());
        if (gamePanel != null) gamePanel.repaint();
        
        // Detay paneli de güncellensin
        updateDetailPanel();
        updateHudPanel();  // sağ panel de güncellensin
    }

    private void handlePlayerDefeated() {
        appendLog("Kaybettin... " + enemy.name + " seni yendi.");

        // Oyuncuya seçenek sor
        int opt = JOptionPane.showConfirmDialog(
                this,
                "Kaybettin!\nBölüm " + stage + " tekrar denensin mi?",
                "Oyun Bitti",
                JOptionPane.YES_NO_OPTION
        );

        if (opt == JOptionPane.YES_OPTION) {
            // Aynı bölümden devam
            retryCurrentStage();
            return;
        }

        // Retry istemediyse klasik game over
        JOptionPane.showMessageDialog(
                this,
                "Oyun sona erdi.\nSon seviyen: " + player.level,
                "Oyun Bitti",
                JOptionPane.INFORMATION_MESSAGE
        );

        btnQuick.setEnabled(false);
        btnHeavy.setEnabled(false);
        btnMagic.setEnabled(false);
        btnSpecial.setEnabled(false);
        btnShield.setEnabled(false);
        btnItem.setEnabled(false);

        saveGame();
    }


    private void retryCurrentStage() {
		// TODO Auto-generated method stub
		
	}

	private void handleEnemyDefeated() {
        appendLog(enemy.name + " yenildi! Bölüm " + stage + " tamamlandı.");
        monstersKilled++;

        int gainedExp = 30 + stage * 10;
        appendLog("Tecrübe kazandın: " + gainedExp + " XP.");
        player.exp += gainedExp;

        // Level up
        while (player.exp >= player.expToNext) {
            player.exp -= player.expToNext;
            player.level++;
            player.expToNext += 25;
            int oldMax = player.maxHp;
            player.maxHp += 15;
            player.hp = player.maxHp;
            appendLog("Seviye atladın! Yeni seviye: " + player.level +
                    ". Max HP: " + oldMax + " → " + player.maxHp);
            // Eğer oyuncu Mage ise mana havuzunu da büyüt
            if (player.fighterClass == FighterClass.MAGE) {
                int oldMaxMana = player.maxMana;
                player.maxMana += 10;
                player.mana = player.maxMana;
                appendLog("Mana havuzun genişledi: " + oldMaxMana +
                        " → " + player.maxMana + " (mana fullendi)");
            }

        }
        

        // Rastgele iksir dropları
        int dropRoll = rnd.nextInt(100);
        if (dropRoll < 40) {
            player.hpPotions++;
            appendLog("[Loot] Bir HP İksiri buldun! (Toplam: " + player.hpPotions + ")");
        } else if (dropRoll < 70) {
            player.powerPotions++;
            appendLog("[Loot] Bir Güç İksiri buldun! (Toplam: " + player.powerPotions + ")");
        }

        // Mağaza / skill upgrade
        showUpgradeDialog();

        JOptionPane.showMessageDialog(
                this,
                "Bölüm " + stage + " tamamlandı!\n" + gainedExp + " XP kazandın.",
                "Bölüm Bitti",
                JOptionPane.INFORMATION_MESSAGE
        );

        // Eski chapter'ı not et
        int oldChapter = getChapterIndex(stage);

        // Bir sonraki bölüme geç
        stage++;
        player.specialCooldown = 0; // yeni bölüme geçince skill reset

        // Eğer chapter değiştiyse mini hikâye + tema değişimi
        int newChapter = getChapterIndex(stage);
        if (newChapter != oldChapter) {
            // Tema chapter'a göre otomatik ayarlansın
            switch (newChapter) {
                case 1 -> theme = Theme.FOREST;
                case 2 -> theme = Theme.LAVA;
                case 3 -> theme = Theme.NIGHT;
            }

            String chapterName = getChapterName(stage);
            String introText = getChapterIntro(newChapter);

            appendLog("[Yeni Bölge] " + chapterName + " bölgesine giriş yaptın.");
            JOptionPane.showMessageDialog(
                    this,
                    introText,
                    chapterName,
                    JOptionPane.INFORMATION_MESSAGE
            );
        }

        // Yeni düşmanı oluştur ve UI'yi güncelle
        createEnemyForStage();
        updateUIState();

        startPlayerTurn();

    }

    // Mağaza / skill upgrade
    private void showUpgradeDialog() {
        String[] options = {
                "+20 Maks HP (ve full can)",
                "%10 kalıcı daha fazla hasar",
                "%10 kalıcı kritik şansı"
        };
        int choice = JOptionPane.showOptionDialog(
                this,
                "Mağaza / Yetenek Yükseltme\nBir yükseltme seç:",
                "Yetenek Ağacı",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice < 0) return;

        switch (choice) {
            case 0 -> {
                player.maxHp += 20;
                player.hp = player.maxHp;
                appendLog("[Upgrade] +20 Maks HP, canın fullendi.");
            }
            case 1 -> {
                player.dmgMultiplier *= 1.10;
                appendLog("[Upgrade] Kalıcı olarak hasarın %10 arttı.");
            }
            case 2 -> {
                player.extraCritChance += 10;
                appendLog("[Upgrade] Kritik şansın kalıcı olarak %10 arttı.");
            }
        }
    }

    private boolean isGameOver() {
        return player.isDead();
    }

    private void appendLog(String text) {
        txtLog.append(text + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    // İstatistik ekranı
    private void showStatsDialog() {
        String msg =
                "Sınıf: " + player.fighterClass + "\n" +
                        "Seviye: " + player.level + "\n" +
                        "Bulunduğun bölüm: " + stage + "\n\n" +
                        "Kesilen canavar sayısı: " + monstersKilled + "\n" +
                        "Verilen toplam hasar: " + totalDamageDealt + "\n" +
                        "Alınan toplam hasar: " + totalDamageTaken + "\n" +
                        "Tek vuruşta en yüksek hasar: " + maxHit + "\n\n" +
                        "Öfke (Rage): " + player.rage + "\n\n" +
                        "HP İksiri: " + player.hpPotions + "\n" +
                        "Güç İksiri: " + player.powerPotions + "\n";
        JOptionPane.showMessageDialog(this, msg, "Karakter Bilgisi", JOptionPane.INFORMATION_MESSAGE);
            }
 // Sağdaki detay panelini güncelle
    private void updateDetailPanel() {
        // Burada kendi değişken adını kullan:
        // Örn: txtDetail, txtStats, detailArea vs.
        if (player == null || txtHudDetails == null) return;

        StringBuilder sb = new StringBuilder();

        sb.append("Sınıf : ").append(player.fighterClass).append("\n");
        sb.append("Seviye : ").append(player.level).append("\n");
        sb.append("Bölüm : ").append(stage).append("\n\n");

        // HP – XP
        sb.append("HP : ").append(player.hp).append(" / ")
          .append(player.maxHp).append("\n");
        sb.append("XP : ").append(player.exp).append(" / ")
          .append(player.expToNext).append("\n\n");

        // Mage ise Mana göster
        if (player.fighterClass == FighterClass.MAGE) {
            sb.append("Mana : ")
              .append(player.mana).append(" / ").append(player.maxMana)
              .append("\n");
        }

        // Warrior ise Rage göster
        if (player.fighterClass == FighterClass.WARRIOR) {
            sb.append("Rage : ")
              .append(player.rage).append(" / 60\n");
        }

        // İksirler
        sb.append("\nHP İksiri : ").append(player.hpPotions);
        sb.append("\nGüç İksiri : ").append(player.powerPotions);

        // Genel istatistikler
        sb.append("\n\nKesilen canavar : ").append(monstersKilled);
        sb.append("\nToplam verilen hasar : ").append(totalDamageDealt);
        sb.append("\nToplam alınan hasar : ").append(totalDamageTaken);
        sb.append("\nEn yüksek vuruş : ").append(maxHit);

        txtHudDetails.setText(sb.toString());
    }

    

    // --- Kayıt / Yükleme ---
    private void saveGame() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getSaveFileName()))) {
            SaveData data = new SaveData();
            data.version = 1;   
            data.player = player;
            data.enemy = enemy;
            data.stage = stage;
            data.playerTurn = playerTurn;
            data.difficulty = difficulty;
            data.theme = theme;
            data.monstersKilled = monstersKilled;
            data.totalDamageDealt = totalDamageDealt;
            data.totalDamageTaken = totalDamageTaken;
            data.maxHit = maxHit;
            data.saveSlot = currentSaveSlot;
            oos.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean loadGame() {
        File file = new File(getSaveFileName());
        if (!file.exists()) return false;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            SaveData data = (SaveData) ois.readObject();

            // Versiyon kontrolü (ileride farklı versiyonlar için kullanabilirsin)
            if (data.version != 1) {
                throw new IOException("Save version mismatch");
            }

            this.player = data.player;
            this.enemy = data.enemy;
            this.stage = data.stage;
            this.playerTurn = data.playerTurn;
            this.difficulty = (data.difficulty != null) ? data.difficulty : Difficulty.NORMAL;
            this.theme = (data.theme != null) ? data.theme : Theme.FOREST;
            this.monstersKilled = data.monstersKilled;
            this.totalDamageDealt = data.totalDamageDealt;
            this.totalDamageTaken = data.totalDamageTaken;
            this.maxHit = data.maxHit;
            if (data.saveSlot >= 1 && data.saveSlot <= MAX_SAVE_SLOTS) {
                this.currentSaveSlot = data.saveSlot;
            }

            this.isAnimating = false;
            this.currentAnimation = AnimationType.NONE;
            this.animationStep = 0;
            this.animationMaxStep = 0;
            this.lastHitWasCrit = false;
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            int opt = JOptionPane.showConfirmDialog(
                    this,
                    "Kayıt yüklenirken hata oluştu.\n" +
                            "Kayıt dosyasını silip bu slottan yeni oyun başlatmak ister misin?",
                    "Bozuk Kayıt",
                    JOptionPane.YES_NO_OPTION
            );
            if (opt == JOptionPane.YES_OPTION) {
                file.delete();
                return false; // çağıran tarafta yeni oyun başlatılır
            }
            return false;
        }
    }


    // --- Çizim paneli ---
    class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Arka plan (tema)
            Graphics2D g2d = (Graphics2D) g;
            Color topColor = null;
            Color bottomColor = null;
            switch (theme) {
                case LAVA -> {
                    topColor = new Color(70, 0, 0);
                    bottomColor = new Color(180, 60, 0);
                }
                case NIGHT -> {
                    topColor = new Color(5, 5, 40);
                    bottomColor = new Color(0, 0, 0);
                }
                case FOREST -> {
                    topColor = new Color(15, 15, 40);
                    bottomColor = new Color(20, 70, 50);
                }
            }
            GradientPaint gp = new GradientPaint(
                    0, 0, topColor,
                    0, getHeight(), bottomColor
            );
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Bölüm barı
            drawStageBar(g2d);

            // Yer
            g.setColor(new Color(80, 80, 80));
            int groundY = getHeight() - 110;
            g.fillRect(0, groundY, getWidth(), 5);

            // KARAKTER KONUMU ORTALANDI
            int centerX = getWidth() / 2;
            int playerBaseX = centerX - 220;
            int enemyBaseX = centerX + 120;
            int baseY = groundY - 20;

            double phase = (animationMaxStep == 0) ? 0.0
                    : (double) animationStep / (double) animationMaxStep;
            double forwardPhase =
                    (phase <= 0.5) ? (phase / 0.5) : ((1.0 - phase) / 0.5);

            int maxOffset;
            if (currentAnimation == AnimationType.PLAYER_HEAVY ||
                    currentAnimation == AnimationType.PLAYER_SPECIAL) {
                maxOffset = 80;
            } else {
                maxOffset = 50;
            }
            int offset = (int) (forwardPhase * maxOffset);

            int playerOffsetX = 0;
            int enemyOffsetX = 0;

            boolean playerAttacking =
                    currentAnimation == AnimationType.PLAYER_QUICK ||
                            currentAnimation == AnimationType.PLAYER_HEAVY ||
                            currentAnimation == AnimationType.PLAYER_MAGIC ||
                            currentAnimation == AnimationType.PLAYER_SPECIAL;

            boolean enemyAttacking =
                    currentAnimation == AnimationType.ENEMY_QUICK ||
                            currentAnimation == AnimationType.ENEMY_HEAVY ||
                            currentAnimation == AnimationType.ENEMY_MAGIC;

            if (playerAttacking) playerOffsetX = offset;
            if (enemyAttacking) enemyOffsetX = -offset;

            int playerX = playerBaseX + playerOffsetX;
            int enemyX = enemyBaseX + enemyOffsetX;

            // Karakterler
            drawHero(g, playerX, baseY, phase, playerAttacking);
            drawMonster(g, enemyX, baseY, phase, enemyAttacking);

            // HP bar
            if (player != null)
                drawHpBar(g, playerX - 40, baseY - 100,
                        player.hp, player.maxHp, new Color(80, 220, 80));
            if (enemy != null)
                drawHpBar(g, enemyX - 40, baseY - 100,
                        enemy.hp, enemy.maxHp, new Color(220, 80, 80));

            // İsimler
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            if (player != null)
                g.drawString(player.name + " (Lv " + player.level + ")",
                        playerX - 20, baseY - 112);
            if (enemy != null)
                g.drawString(enemy.name,
                        enemyX - 10, baseY - 112);

            // Buff ikonları (player HUD altında)
            if (player != null)
                drawBuffIcons(g, playerX, baseY);

            // Efektler
            if (player != null && enemy != null)
                drawAttackEffects(g, playerX, enemyX, baseY, phase);

            // Floating damage
            if (player != null && enemy != null)
                drawFloatingDamage(g, playerX, enemyX, baseY);
        }

        // Bölüm barı (mini harita)
        private void drawStageBar(Graphics2D g2) {
            int barWidth = 260;
            int barHeight = 20;
            int x = (getWidth() - barWidth) / 2;
            int y = 10;

            int baseStage = Math.max(1, stage - 1);

            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(x - 10, y - 5, barWidth + 20, barHeight + 10, 15, 15);

            for (int i = 0; i < 3; i++) {
                int s = baseStage + i;
                int boxX = x + i * (barWidth / 3);
                int boxW = barWidth / 3 - 4;

                if (s == stage) {
                    g2.setColor(new Color(255, 210, 80));
                } else {
                    g2.setColor(new Color(200, 200, 200));
                }
                g2.fillRoundRect(boxX + 2, y, boxW, barHeight, 10, 10);

                g2.setColor(Color.BLACK);
                g2.drawRoundRect(boxX + 2, y, boxW, barHeight, 10, 10);

                String text = "Bölüm " + s;
                FontMetrics fm = g2.getFontMetrics();
                int tx = boxX + 2 + (boxW - fm.stringWidth(text)) / 2;
                int ty = y + (barHeight + fm.getAscent()) / 2 - 3;
                g2.drawString(text, tx, ty);
            }
        }

        // Oyuncu sprite
        private void drawHero(Graphics g, int x, int baseY,
                              double phase, boolean attacking) {

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            Image img;
            switch (player.fighterClass) {
                case WARRIOR -> img = warriorImg;
                case ROGUE -> img = rogueImg;
                case MAGE -> img = mageImg;
                default -> img = warriorImg;
            }

            if (img == null) return;

            double wave = Math.sin(phase * Math.PI);
            int bob = attacking ? (int) (5 * wave) : 0;

            int spriteWidth = 96;
            int spriteHeight = 96;

            // gölge
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(x - 10, baseY + 40, 70, 18);

            int drawX = x;
            int drawY = baseY - spriteHeight + bob;

            g2.drawImage(img, drawX, drawY, spriteWidth, spriteHeight, null);
        }

        // Canavar sprite
        private void drawMonster(Graphics g, int x, int baseY,
                                 double phase, boolean attacking) {

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            if (enemy == null) return;

            boolean isBoss = enemy.name != null && enemy.name.startsWith("BOSS:");

            // Hangi forma hangi sprite?
            Image img;
            switch (enemy.monsterForm) {
                case GOBLIN -> img = goblinImg;
                case SKELETON -> img = skeletonImg;
                case DEMON -> img = isBoss ? dragonBossImg : demonImg;       // boss demon = büyük ejder
                case DRAGONLING -> img = isBoss ? dragonBossImg : dragonImg; // mini ejder / boss ejder
                default -> img = dragonImg;
            }

            if (img == null) return;

            double wave = Math.sin(phase * Math.PI);
            int bob = attacking ? (int) (4 * wave) : 0;

            // Boss biraz daha büyük olsun
            int spriteWidth;
            int spriteHeight;
            if (isBoss) {
                spriteWidth = 128;
                spriteHeight = 128;
            } else {
                spriteWidth = 96;
                spriteHeight = 96;
            }

            // gölge
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(x - 10, baseY + 40, 70, 18);

            int drawX = x;
            int drawY = baseY - spriteHeight + bob;

            g2.drawImage(img, drawX, drawY, spriteWidth, spriteHeight, null);
        }

        private void drawHpBar(Graphics g, int x, int y, int hp, int maxHp, Color color) {
            int width = 120;
            int height = 12;

            g.setColor(new Color(30, 30, 30, 200));
            g.fillRoundRect(x - 2, y - 2, width + 4, height + 4, 8, 8);

            g.setColor(Color.DARK_GRAY);
            g.fillRect(x, y, width, height);

            double ratio = (double) hp / maxHp;
            int filled = (int) (width * ratio);

            g.setColor(color);
            g.fillRect(x, y, filled, height);

            g.setColor(Color.BLACK);
            g.drawRect(x, y, width, height);
        }

        // Buff ikonları
        private void drawBuffIcons(Graphics g, int playerX, int baseY) {
            int startX = playerX - 40;
            int y = baseY - 80;
            int size = 14;
            int pad = 4;
            int x = startX;

            Graphics2D g2 = (Graphics2D) g;
            g2.setFont(new Font("Arial", Font.BOLD, 10));

            if (player.shieldTurnsRemaining > 0) {
                g2.setColor(new Color(120, 200, 255));
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.BLACK);
                g2.drawOval(x, y, size, size);
                g2.drawString("K", x + 4, y + 11);
                x += size + pad;
            }

            if (player.dmgMultiplier > 1.0 || player.powerBuffTurns > 0) {
                g2.setColor(new Color(255, 200, 80));
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.BLACK);
                g2.drawOval(x, y, size, size);
                g2.drawString("D", x + 4, y + 11);
                x += size + pad;
            }

            if (player.extraCritChance > 0) {
                g2.setColor(new Color(255, 120, 180));
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.BLACK);
                g2.drawOval(x, y, size, size);
                g2.drawString("C", x + 4, y + 11);
            }
        }

        private void drawAttackEffects(Graphics g, int playerX, int enemyX,
                                       int baseY, double phase) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(lastHitWasCrit ? 4 : 3));

            int heroSwordHandX = playerX + 45;
            int heroSwordHandY = baseY;
            int enemyCenterX = enemyX + 20;
            int enemyCenterY = baseY - 20;

            // Hızlı saldırı
            if (currentAnimation == AnimationType.PLAYER_QUICK ||
                    currentAnimation == AnimationType.ENEMY_QUICK) {

                if (phase > 0.2 && phase < 0.8) {
                    int cx = (currentAnimation == AnimationType.PLAYER_QUICK)
                            ? enemyCenterX : playerX + 15;
                    g2.setColor(Color.YELLOW);
                    int size = 40;
                    g2.drawArc(cx - size / 2, enemyCenterY - size / 2,
                            size, size,
                            (currentAnimation == AnimationType.PLAYER_QUICK) ? -30 : 150,
                            60);
                }
            }

            // Güçlü saldırı
            if (currentAnimation == AnimationType.PLAYER_HEAVY ||
                    currentAnimation == AnimationType.ENEMY_HEAVY) {

                if (phase > 0.2 && phase < 0.9) {
                    double t = (phase - 0.2) / 0.7;
                    if (t < 0) t = 0;
                    if (t > 1) t = 1;

                    int startX, startY, endX, endY;
                    if (currentAnimation == AnimationType.PLAYER_HEAVY) {
                        startX = heroSwordHandX;
                        startY = heroSwordHandY;
                        endX = enemyCenterX + 18;
                        endY = enemyCenterY - 12;
                    } else {
                        startX = enemyCenterX - 10;
                        startY = enemyCenterY;
                        endX = playerX + 20;
                        endY = baseY - 10;
                    }

                    int currEndX = (int) (startX + (endX - startX) * t);
                    int currEndY = (int) (startY + (endY - startY) * t);

                    g2.setColor(lastHitWasCrit ? new Color(255, 220, 80) : new Color(255, 80, 40));
                    g2.drawLine(startX, startY, currEndX, currEndY);

                    if (currentAnimation == AnimationType.PLAYER_HEAVY) {
                        g2.setColor(new Color(255, 100, 100, 180));
                        g2.fillOval(enemyCenterX - 10, enemyCenterY - 10, 20, 20);
                    }
                }
            }

            // Büyü / enerji küresi
            if (currentAnimation == AnimationType.PLAYER_MAGIC ||
                    currentAnimation == AnimationType.ENEMY_MAGIC) {

                double t = (animationMaxStep == 0) ? 0.0
                        : (double) animationStep / (double) animationMaxStep;

                int startX, endX;
                int y = baseY - 40;

                if (currentAnimation == AnimationType.PLAYER_MAGIC) {
                    startX = playerX + 40;
                    endX = enemyX + 10;
                } else {
                    startX = enemyX;
                    endX = playerX + 20;
                }

                int projX = (int) (startX + (endX - startX) * t);

                g2.setColor(new Color(120, 200, 255));
                g2.fillOval(projX - 8, y - 8, 16, 16);
                g2.setColor(Color.WHITE);
                g2.drawOval(projX - 10, y - 10, 20, 20);

                if (lastHitWasCrit) {
                    g2.setColor(new Color(200, 240, 255, 120));
                    g2.fillOval(projX - 14, y - 14, 28, 28);
                }
            }

            // Özel saldırı efektleri
            if (currentAnimation == AnimationType.PLAYER_SPECIAL) {
                double t = (animationMaxStep == 0) ? 0.0
                        : (double) animationStep / (double) animationMaxStep;

                int enemyCenterX1 = enemyX + 20;
                int enemyCenterY1 = baseY - 20;
                int heroSwordHandX1 = playerX + 45;
                int heroSwordHandY1 = baseY;

                switch (player.fighterClass) {
                    case WARRIOR -> {
                        if (t > 0.2 && t < 0.95) {
                            int startX = heroSwordHandX1;
                            int startY = heroSwordHandY1;
                            int endX = enemyCenterX1 + 25;
                            int endY = enemyCenterY1 - 15;

                            int currEndX = (int) (startX + (endX - startX) * t);
                            int currEndY = (int) (startY + (endY - startY) * t);

                            g2.setStroke(new BasicStroke(5));
                            g2.setColor(new Color(255, 200, 80));
                            g2.drawLine(startX, startY, currEndX, currEndY);

                            g2.setStroke(new BasicStroke(3));
                            g2.setColor(new Color(255, 120, 120, 180));
                            g2.fillOval(enemyCenterX1 - 12, enemyCenterY1 - 12, 24, 24);
                        }
                    }
                    case ROGUE -> {
                        if (t > 0.2 && t < 0.9) {
                            g2.setColor(new Color(180, 120, 255));
                            int size = 30;
                            g2.drawLine(enemyCenterX1 - size, enemyCenterY1 - size,
                                    enemyCenterX1 + size, enemyCenterY1 + size);
                            g2.drawLine(enemyCenterX1 + size, enemyCenterY1 - size,
                                    enemyCenterX1 - size, enemyCenterY1 + size);
                        }
                    }
                    case MAGE -> {
                        // Meteor yukarıdan aşağı düşsün
                        if (meteorImg != null) {
                            int startY = enemyCenterY1 - 150; // ekranın üstünden gelsin
                            int endY = enemyCenterY1 - 10;
                            int meteorY = (int) (startY + (endY - startY) * t);
                            int meteorX = enemyCenterX1 - 32; // ortala

                            int w = 64;
                            int h = 64;
                            g2.drawImage(meteorImg, meteorX, meteorY, w, h, null);

                            // Çarpma anında küçük patlama
                            if (t > 0.8) {
                                int size = 36;
                                g2.setColor(new Color(255, 200, 80, 180));
                                g2.fillOval(enemyCenterX1 - size / 2, enemyCenterY1 - size / 2,
                                        size, size);
                            }
                        } else {
                            // Sprite yoksa eski efekt kalsın
                            int maxSize = 60;
                            int size = (int) (maxSize * t);
                            if (size < 10) size = 10;
                            g2.setColor(new Color(255, 160, 80, 180));
                            g2.fillOval(enemyCenterX1 - size / 2, enemyCenterY1 - size / 2,
                                    size, size);
                            g2.setColor(new Color(255, 230, 200, 180));
                            g2.drawOval(enemyCenterX1 - size / 2 - 4, enemyCenterY1 - size / 2 - 4,
                                    size + 8, size + 8);
                        }
                    }

                    default -> {
                    }
                }
            }
        }

        // Hasar yazıları
        private void drawFloatingDamage(Graphics g, int playerX, int enemyX, int baseY) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setFont(new Font("Arial", Font.BOLD, 14));

            if (floatDmgEnemyFrames > 0) {
                floatDmgEnemyFrames--;
                int yOffset = (25 - floatDmgEnemyFrames);
                int x = enemyX + 5;
                int y = baseY - 40 - yOffset;

                if (floatDmgEnemyCrit) {
                    g2.setColor(new Color(255, 220, 80));
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.drawString("-" + floatDmgEnemyValue, x, y);
            }

            if (floatDmgPlayerFrames > 0) {
                floatDmgPlayerFrames--;
                int yOffset = (25 - floatDmgPlayerFrames);
                int x = playerX + 5;
                int y = baseY - 40 - yOffset;

                if (floatDmgPlayerCrit) {
                    g2.setColor(new Color(255, 120, 120));
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.drawString("-" + floatDmgPlayerValue, x, y);
            }
        }
    }

    // --- main: Ana menü ---
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            String[] options = {"Yeni Oyun", "Devam", "Çıkış"};
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Ne yapmak istiyorsun?",
                    "Ana Menü",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                System.exit(0);
            }

            // Slot seç
            String[] slotOptions = {"Slot 1", "Slot 2", "Slot 3", "İptal"};
            int s = JOptionPane.showOptionDialog(
                    null,
                    "Kayıt slotu seç:",
                    "Kayıt Slotu",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    slotOptions,
                    slotOptions[0]
            );
            if (s == 3 || s == JOptionPane.CLOSED_OPTION) {
                System.exit(0);
            }
            int slot = s + 1;

            boolean loadSaved = (choice == 1);
            Difficulty diff = Difficulty.NORMAL;

            if (loadSaved) {
                File f = new File(buildFileNameForSlot(slot));
                if (!f.exists()) {
                    JOptionPane.showMessageDialog(null,
                            "Bu slotta kayıt yok, yeni oyun başlatılacak.",
                            "Kayıt Yok",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadSaved = false;
                }
            }

            if (!loadSaved) {
                String[] diffOptions = {"Kolay", "Normal", "Zor"};
                int c = JOptionPane.showOptionDialog(
                        null,
                        "Zorluk seviyesi seç:",
                        "Zorluk",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        diffOptions,
                        diffOptions[1]
                );
                if (c == 0) diff = Difficulty.EASY;
                else if (c == 2) diff = Difficulty.HARD;
                else diff = Difficulty.NORMAL;
            }

            new TurnBasedBattleSwing(loadSaved, slot, diff);
        });
    }
}
