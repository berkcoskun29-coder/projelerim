import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class GameMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Roguelike Survival (Swing) - Synergy + Elites + Boss Patterns");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);

            CardLayout cards = new CardLayout();
            JPanel root = new JPanel(cards);

            GamePanel game = new GamePanel(() -> cards.show(root, "menu"));
            MenuPanel menu = new MenuPanel((minutes) -> {
                game.startRun(minutes * 60);
                cards.show(root, "game");
                game.requestFocusInWindow();
            });

            root.add(menu, "menu");
            root.add(game, "game");

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            cards.show(root, "menu");
        });
    }

    // ============================ META PROGRESSION ============================
    static class MetaProgression {
        static int souls = 0;
        static int hpUp = 0;         // +10 per level
        static int xpUp = 0;         // +5% per level
        static int rareDropUp = 0;   // +2% per level

        static int hpBonus() { return hpUp * 10; }
        static double xpMult() { return 1.0 + xpUp * 0.05; }
        static double rareDropBonus() { return rareDropUp * 0.02; }

        static boolean buyHp() {
            int cost = 20 + hpUp * 15;
            if (souls < cost) return false;
            souls -= cost; hpUp++; return true;
        }
        static boolean buyXp() {
            int cost = 25 + xpUp * 18;
            if (souls < cost) return false;
            souls -= cost; xpUp++; return true;
        }
        static boolean buyRare() {
            int cost = 30 + rareDropUp * 22;
            if (souls < cost) return false;
            souls -= cost; rareDropUp++; return true;
        }
    }

    // ============================ MENU ============================
    static class MenuPanel extends JPanel {
        interface StartCallback { void startWithMinutes(int minutes); }
        private final JLabel soulsLabel = new JLabel("");

        MenuPanel(StartCallback cb) {
            setPreferredSize(new Dimension(960, 540));
            setLayout(new GridBagLayout());
            setBackground(new Color(18, 18, 24));

            JLabel title = new JLabel("Survival Roguelike (Synergy + Elites + Boss Patterns)");
            title.setForeground(Color.WHITE);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));

            JLabel sub = new JLabel("Hedef süre seç (dk):");
            sub.setForeground(new Color(210, 210, 210));
            sub.setFont(sub.getFont().deriveFont(16f));

            JButton b10 = makeBtn("10", () -> cb.startWithMinutes(10));
            JButton b20 = makeBtn("20", () -> cb.startWithMinutes(20));
            JButton b30 = makeBtn("30", () -> cb.startWithMinutes(30));

            JButton upHp = makeSmallBtn("Kalıcı +HP", () -> { MetaProgression.buyHp(); refresh(); });
            JButton upXp = makeSmallBtn("Kalıcı +XP", () -> { MetaProgression.buyXp(); refresh(); });
            JButton upRare = makeSmallBtn("Kalıcı +Rare Drop", () -> { MetaProgression.buyRare(); refresh(); });

            soulsLabel.setForeground(new Color(235,235,235));
            soulsLabel.setFont(soulsLabel.getFont().deriveFont(Font.BOLD, 16f));

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0; gc.gridy = 0;
            gc.insets = new Insets(8, 8, 8, 8);
            add(title, gc);

            gc.gridy++;
            add(sub, gc);

            gc.gridy++;
            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
            row.setOpaque(false);
            row.add(b10); row.add(b20); row.add(b30);
            add(row, gc);

            gc.gridy++;
            JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            metaRow.setOpaque(false);
            metaRow.add(upHp); metaRow.add(upXp); metaRow.add(upRare);
            add(metaRow, gc);

            gc.gridy++;
            add(soulsLabel, gc);

            gc.gridy++;
            JLabel hint = new JLabel("WASD • Otomatik skills • Level Up: 1-2-3 / tıkla • ESC menü • R restart");
            hint.setForeground(new Color(160, 160, 160));
            add(hint, gc);

            refresh();
        }

        private void refresh() {
            soulsLabel.setText("Souls: " + MetaProgression.souls +
                    "   | Kalıcı: HP+" + MetaProgression.hpBonus() +
                    " XPx" + String.format(Locale.US, "%.2f", MetaProgression.xpMult()) +
                    " Rare+" + (int)Math.round(MetaProgression.rareDropBonus()*100) + "%");
            revalidate(); repaint();
        }

        private JButton makeBtn(String text, Runnable action) {
            JButton b = new JButton(text + " DK");
            b.setFocusPainted(false);
            b.setFont(b.getFont().deriveFont(Font.BOLD, 16f));
            b.addActionListener(e -> action.run());
            b.setPreferredSize(new Dimension(140, 44));
            return b;
        }
        private JButton makeSmallBtn(String text, Runnable action) {
            JButton b = new JButton(text);
            b.setFocusPainted(false);
            b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
            b.addActionListener(e -> action.run());
            b.setPreferredSize(new Dimension(180, 34));
            return b;
        }
    }

    // ============================ GAME ============================
    static class GamePanel extends JPanel implements ActionListener {
        enum State { PLAYING, LEVEL_UP, GAME_OVER, VICTORY }
        enum MapMod { FOG, LAVA, OBSTACLES }
        enum DamageSource { BULLET, SWORD, LIGHTNING, ICE, LASER, EXPLOSION }

        private final Runnable onExitToMenu;
        private final int VIEW_W = 960;
        private final int VIEW_H = 540;

        private final javax.swing.Timer timer = new javax.swing.Timer(16, this);
        private long lastNs;

        private boolean up, down, left, right;
        private int mouseX, mouseY;
        private boolean mouseClicked;

        private Player player;
        private final List<Enemy> enemies = new ArrayList<>();
        private final List<Projectile> projectiles = new ArrayList<>();
        private final List<EnemyProjectile> enemyProjectiles = new ArrayList<>();
        private final List<XpOrb> orbs = new ArrayList<>();
        private final List<ItemPickup> items = new ArrayList<>();
        private final List<FloatingText> floatingTexts = new ArrayList<>();
        private final List<Effect> effects = new ArrayList<>();
        private final List<Obstacle> obstacles = new ArrayList<>();

        private final Random rng = new Random();

        private double enemySpawnAcc = 0;
        private int targetSeconds = 600;
        private int elapsedSeconds = 0;
        private double timeAccumulator = 0;
        private State state = State.PLAYING;
        private boolean bossSpawned = false;

        private final UpgradeSystem upgradeSystem = new UpgradeSystem();
        private final ItemDropSystem itemDropSystem = new ItemDropSystem();
        private List<Upgrade> currentChoices = List.of();

        // loot hover
        private int lootPanelHoveredIndex = -1;

        // reminders
        private String reminderText = "";
        private double reminderLeft = 0;

        // hit stop / shake (DONMA FIX)
        private double hitStopLeft = 0.0;
        private double shakeLeft = 0.0;
        private double shakePower = 0.0;

        // map modifier
        private MapMod mapMod = MapMod.FOG;

        // arena shrink phase3
        private boolean arenaShrink = false;
        private double arenaRadius = 520;
        private double arenaShrinkTarget = 340;

        GamePanel(Runnable onExitToMenu) {
            this.onExitToMenu = onExitToMenu;
            setPreferredSize(new Dimension(VIEW_W, VIEW_H));
            setFocusable(true);
            setBackground(new Color(12, 12, 16));
            setupKeyBinds();
            setupMouse();
        }

        void startRun(int targetSeconds) {
            this.targetSeconds = targetSeconds;
            this.elapsedSeconds = 0;
            this.timeAccumulator = 0;

            enemies.clear();
            projectiles.clear();
            enemyProjectiles.clear();
            orbs.clear();
            items.clear();
            floatingTexts.clear();
            effects.clear();
            obstacles.clear();

            player = new Player(0, 0);

            // apply meta
            player.maxHp += MetaProgression.hpBonus();
            player.hp = player.maxHp;

            // Skills default
            player.skills.clear();
            player.skills.add(new BasicShotSkill());

            state = State.PLAYING;
            enemySpawnAcc = 0;
            bossSpawned = false;

            player.level = 1;
            player.xp = 0;
            player.xpToNext = xpRequirement(player.level);

            player.inventory.clear();
            player.updateSets(); // init

            hitStopLeft = 0; shakeLeft = 0; shakePower = 0;
            reminderLeft = 0; reminderText = "";

            // map mod
            mapMod = MapMod.values()[rng.nextInt(MapMod.values().length)];
            if (mapMod == MapMod.OBSTACLES) spawnObstacles();

            arenaShrink = false;
            arenaRadius = 520;
            arenaShrinkTarget = 340;

            showReminder("RUN MOD: " + mapMod, 2.2);

            lastNs = System.nanoTime();
            timer.start();
        }

        private void spawnObstacles() {
            for (int i = 0; i < 10; i++) {
                double a = rng.nextDouble() * Math.PI * 2;
                double d = 120 + rng.nextDouble() * 420;
                double x = Math.cos(a) * d;
                double y = Math.sin(a) * d;
                obstacles.add(new Obstacle(x, y, 18 + rng.nextInt(18)));
            }
        }

        private int xpRequirement(int level) {
            return 10 + (level - 1) * 6;
        }

        private void setupMouse() {
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    mouseClicked = true;
                    mouseX = e.getX();
                    mouseY = e.getY();
                }
            });
        }

        private void setupKeyBinds() {
            InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = getActionMap();

            bind(im, am, "pressed W",  () -> up = true);
            bind(im, am, "released W", () -> up = false);
            bind(im, am, "pressed S",  () -> down = true);
            bind(im, am, "released S", () -> down = false);
            bind(im, am, "pressed A",  () -> left = true);
            bind(im, am, "released A", () -> left = false);
            bind(im, am, "pressed D",  () -> right = true);
            bind(im, am, "released D", () -> right = false);

            bind(im, am, "pressed ESCAPE", () -> {
                timer.stop();
                onExitToMenu.run();
            });

            bind(im, am, "pressed R", () -> {
                if (state != State.PLAYING) startRun(targetSeconds);
            });

            bind(im, am, "pressed 1", () -> pickUpgrade(0));
            bind(im, am, "pressed 2", () -> pickUpgrade(1));
            bind(im, am, "pressed 3", () -> pickUpgrade(2));
        }

        private void bind(InputMap im, ActionMap am, String keystroke, Runnable r) {
            im.put(KeyStroke.getKeyStroke(keystroke), keystroke);
            am.put(keystroke, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { r.run(); }
            });
        }

        private void pickUpgrade(int idx) {
            if (state != State.LEVEL_UP) return;
            if (idx < 0 || idx >= currentChoices.size()) return;

            currentChoices.get(idx).apply(player);
            state = State.PLAYING;
            floatingTexts.add(FloatingText.center("UPGRADE SEÇİLDİ!", 1.2));
        }

        // ===== HIT STOP / SHAKE =====
        private void triggerHitStop(double seconds) { hitStopLeft = Math.max(hitStopLeft, seconds); }
        private void triggerShake(double seconds, double powerPx) {
            shakeLeft = Math.max(shakeLeft, seconds);
            shakePower = Math.max(shakePower, powerPx);
        }

        private void showReminder(String t, double seconds) {
            reminderText = t;
            reminderLeft = Math.max(reminderLeft, seconds);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();

            double rawDt = (now - lastNs) / 1_000_000_000.0;
            lastNs = now;
            rawDt = Math.min(rawDt, 0.05);

            // ✅ hitstop ALWAYS rawDt ile azalır (donma fix)
            if (hitStopLeft > 0) {
                hitStopLeft -= rawDt;
                if (hitStopLeft < 0) hitStopLeft = 0;
            }

            // ✅ oyun dt: hitstop varken 0
            double dt = (hitStopLeft > 0) ? 0.0 : rawDt;

            if (shakeLeft > 0) {
                shakeLeft -= rawDt;
                if (shakeLeft < 0) shakeLeft = 0;
            }
            if (reminderLeft > 0) {
                reminderLeft -= rawDt;
                if (reminderLeft < 0) reminderLeft = 0;
            }

            if (state == State.PLAYING) updateGame(dt, rawDt);
            else if (state == State.LEVEL_UP) {
                if (mouseClicked) handleLevelUpClick();
                mouseClicked = false;
            }

            repaint();
        }

        private void handleLevelUpClick() {
            Rectangle[] rects = levelUpCardRects();
            for (int i = 0; i < rects.length; i++) {
                if (rects[i].contains(mouseX, mouseY)) { pickUpgrade(i); break; }
            }
        }

        private Rectangle[] levelUpCardRects() {
            int cardW = 240, cardH = 170, gap = 18;
            int totalW = cardW * 3 + gap * 2;
            int startX = (VIEW_W - totalW) / 2;
            int y = (VIEW_H - cardH) / 2 + 30;
            return new Rectangle[] {
                    new Rectangle(startX, y, cardW, cardH),
                    new Rectangle(startX + cardW + gap, y, cardW, cardH),
                    new Rectangle(startX + (cardW + gap) * 2, y, cardW, cardH)
            };
        }

        private void updateGame(double dt, double rawDt) {
            // time
            timeAccumulator += rawDt;
            while (timeAccumulator >= 1.0) {
                timeAccumulator -= 1.0;
                elapsedSeconds++;
                if (elapsedSeconds >= targetSeconds) { state = State.VICTORY; break; }
            }
            if (state != State.PLAYING) {
                if (state == State.VICTORY) endRun(true);
                return;
            }

            // boss at 3 min
            if (!bossSpawned && elapsedSeconds >= 180) {
                spawnBoss();
                bossSpawned = true;
                floatingTexts.add(FloatingText.center("BOSS GELDİ!", 2.0));
                showReminder("BOSS SPAWN!", 1.7);
            }

            // movement
            double mx = 0, my = 0;
            if (up) my -= 1;
            if (down) my += 1;
            if (left) mx -= 1;
            if (right) mx += 1;
            double len = Math.hypot(mx, my);
            if (len > 0) { mx /= len; my /= len; }

            // slippery
            if (mapMod == MapMod.FOG || mapMod == MapMod.OBSTACLES) {
                player.vx = mx * player.moveSpeed;
                player.vy = my * player.moveSpeed;
            } else {
                // lava mod: tiny inertia feel
                double targetVx = mx * player.moveSpeed;
                double targetVy = my * player.moveSpeed;
                player.vx += (targetVx - player.vx) * Math.min(1.0, dt * 8);
                player.vy += (targetVy - player.vy) * Math.min(1.0, dt * 8);
            }

            player.x += player.vx * dt;
            player.y += player.vy * dt;

            // lava DOT
            if (mapMod == MapMod.LAVA && dt > 0) {
                player.hp -= 2.2 * dt;
                if (player.hp <= 0) { player.hp = 0; state = State.GAME_OVER; endRun(false); return; }
            }

            // obstacles collision
            if (mapMod == MapMod.OBSTACLES) {
                resolveObstacleCollision(player);
            }

            // spawn enemies
            double t = elapsedSeconds;
            double spawnInterval = Math.max(0.20, 1.05 - t * 0.006);
            enemySpawnAcc += dt;
            while (enemySpawnAcc >= spawnInterval) {
                enemySpawnAcc -= spawnInterval;
                spawnEnemyAroundPlayer();
            }

            // enemies update + patterns
            for (Enemy en : enemies) {
                en.update(player, dt);

                if (!en.alive) continue;

                // boss pattern logic
                if (en.type == EnemyType.BOSS) {
                    runBossPatterns(en, dt);
                }

                // obstacle collision for enemies (optional)
                if (mapMod == MapMod.OBSTACLES) resolveObstacleCollision(en);

                // contact damage
                if (circleHit(en.x, en.y, en.radius, player.x, player.y, player.radius)) {
                    player.hp -= en.contactDps * dt;
                    if (player.hp <= 0) { player.hp = 0; state = State.GAME_OVER; endRun(false); return; }
                }

                // ranged shoot
                if (en.type == EnemyType.RANGED || en.type == EnemyType.BOSS_RANGED) {
                    en.shootCooldown -= dt;
                    if (en.shootCooldown <= 0) {
                        fireEnemyProjectile(en);
                        en.shootCooldown = en.baseShootDelay;
                    }
                }
            }

            // skills update
            for (Skill sk : player.skills) sk.update(dt, player, this);

            // FX update
            for (Effect fx : effects) fx.update(dt);
            effects.removeIf(fx -> !fx.alive);

            // player projectiles update
            for (Projectile p : projectiles) {
                p.x += p.vx * dt;
                p.y += p.vy * dt;
                p.life -= dt;
                if (p.life <= 0) p.alive = false;
            }

            // enemy projectiles update
            for (EnemyProjectile p : enemyProjectiles) {
                p.x += p.vx * dt;
                p.y += p.vy * dt;
                p.life -= dt;
                if (p.life <= 0) p.alive = false;

                if (p.alive && circleHit(p.x, p.y, p.radius, player.x, player.y, player.radius)) {
                    player.hp -= p.damage;
                    p.alive = false;
                    triggerShake(0.10, 5);
                    if (player.hp <= 0) { player.hp = 0; state = State.GAME_OVER; endRun(false); return; }
                }
            }

            // projectile hits
            for (Projectile p : projectiles) {
                if (!p.alive) continue;
                for (Enemy en : enemies) {
                    if (!en.alive) continue;
                    if (circleHit(p.x, p.y, p.radius, en.x, en.y, en.radius)) {

                        // Shielded: front from player blocks bullets
                        if (en.eliteType == EliteType.SHIELDED) {
                            if (en.blocksHitFrom(player.x, player.y)) {
                                // tiny block fx
                                addEffect(new BlockSparkEffect(en.x, en.y, 0.18));
                                p.alive = false;
                                break;
                            }
                        }

                        double dmg = p.damage;

                        // Multi-shot + crit synergy: same target quick multi hit => bonus crit chance
                        double critChance = player.critChance;
                        en.recentHitTimer = 0.12;
                        en.recentHitCount++;
                        if (en.recentHitCount >= 2) critChance = Math.min(0.85, critChance + 0.18);

                        boolean isCrit = (rng.nextDouble() < critChance);
                        if (isCrit) dmg *= player.critMultiplier;

                        damageEnemy(en, dmg, true, true, DamageSource.BULLET);

                        // damage numbers
                        addEffect(new DamageNumberEffect(en.x, en.y, dmg, isCrit, DamageSource.BULLET));

                        if (isCrit) { triggerHitStop(0.02); triggerShake(0.08, 3); }

                        p.alive = false;
                        break;
                    }
                }
            }

            // decay enemy recent-hit
            for (Enemy en : enemies) {
                if (en.recentHitTimer > 0) {
                    en.recentHitTimer -= dt;
                    if (en.recentHitTimer <= 0) { en.recentHitTimer = 0; en.recentHitCount = 0; }
                }
            }

            // XP orbs
            for (XpOrb orb : orbs) {
                orb.update(player, dt);
                if (!orb.alive) continue;
                if (circleHit(orb.x, orb.y, orb.radius, player.x, player.y, player.pickupRadius)) {
                    orb.alive = false;
                    gainXp((int)Math.round(orb.value * MetaProgression.xpMult()));
                }
            }

            // items pickup
            for (ItemPickup it : items) {
                if (!it.alive) continue;
                if (circleHit(it.x, it.y, it.radius, player.x, player.y, player.pickupRadius)) {
                    it.alive = false;
                    it.def.apply(player);
                    player.inventory.add(it.def);
                    player.updateSets();

                    floatingTexts.add(FloatingText.loot("Loot: " + it.def.rarity + " - " + it.def.name, 2.0));
                }
            }

            // arena shrink (phase3)
            if (arenaShrink) {
                arenaRadius += (arenaShrinkTarget - arenaRadius) * Math.min(1.0, dt * 0.7);
                double dist = Math.hypot(player.x, player.y);
                if (dist > arenaRadius) {
                    player.hp -= 12 * dt;
                    if (player.hp <= 0) { player.hp = 0; state = State.GAME_OVER; endRun(false); return; }
                }
            }

            // floating texts
            for (FloatingText ft : floatingTexts) ft.update(dt);
            floatingTexts.removeIf(ft -> !ft.alive);

            // cleanup
            enemies.removeIf(en -> !en.alive);
            projectiles.removeIf(p -> !p.alive);
            enemyProjectiles.removeIf(p -> !p.alive);
            orbs.removeIf(o -> !o.alive);
            items.removeIf(i -> !i.alive);

            // death/victory
            if (player.hp <= 0) { player.hp = 0; state = State.GAME_OVER; endRun(false); }
        }

        private void endRun(boolean victory) {
            timer.stop();
            int gain = victory ? (30 + player.level * 3) : (12 + player.level * 2);
            gain += elapsedSeconds / 30;
            MetaProgression.souls += gain;
            floatingTexts.add(FloatingText.center("SOULS +" + gain, 2.0));
        }

        private void runBossPatterns(Enemy boss, double dt) {
            // phase check
            double hpPct = boss.hp / boss.maxHp;
            int newPhase = (hpPct > 0.60) ? 1 : (hpPct > 0.30 ? 2 : 3);
            if (newPhase != boss.phase) {
                boss.phase = newPhase;
                showReminder("BOSS PHASE " + newPhase, 1.5);

                if (newPhase == 3) {
                    arenaShrink = true;
                    addEffect(new ArenaRingEffect(() -> player.x, () -> player.y, () -> arenaRadius, 999));
                }
            }

            boss.patternCd -= dt;
            if (boss.patternCd > 0) return;

            if (boss.phase == 1) {
                // occasional explosion warning
                spawnExplosionPattern(boss, 0.85, 90, 26);
                boss.patternCd = 1.35;
            } else if (boss.phase == 2) {
                // laser + explosions mixed
                if (rng.nextDouble() < 0.55) spawnLaserPattern(boss, 0.75);
                else spawnExplosionPattern(boss, 0.75, 110, 30);
                boss.patternCd = 1.05;
            } else {
                // phase3: more aggressive + multi
                spawnLaserPattern(boss, 0.60);
                if (rng.nextDouble() < 0.85) spawnExplosionPattern(boss, 0.60, 125, 34);
                boss.patternCd = 0.85;
            }
        }

        private void spawnLaserPattern(Enemy boss, double warnSec) {
            // telegraph line from boss to player direction
            double dx = player.x - boss.x;
            double dy = player.y - boss.y;
            double d = Math.hypot(dx, dy);
            if (d < 0.0001) return;
            dx /= d; dy /= d;

            double len = 900;
            double x1 = boss.x;
            double y1 = boss.y;
            double x2 = boss.x + dx * len;
            double y2 = boss.y + dy * len;

            effects.add(new LaserTelegraphEffect(x1, y1, x2, y2, warnSec, () -> {
                // fire laser hit for a short burst
                effects.add(new LaserBeamEffect(x1, y1, x2, y2, 0.22, 14, this));
                triggerHitStop(0.02);
                triggerShake(0.10, 6);
            }));
        }

        private void spawnExplosionPattern(Enemy boss, double warnSec, double radius, double dmg) {
            // warning circle near player
            double a = rng.nextDouble() * Math.PI * 2;
            double off = 40 + rng.nextDouble() * 110;
            double cx = player.x + Math.cos(a) * off;
            double cy = player.y + Math.sin(a) * off;

            effects.add(new ExplosionTelegraphEffect(cx, cy, radius, warnSec, () -> {
                // apply damage if player inside
                double dist = Math.hypot(player.x - cx, player.y - cy);
                if (dist <= radius) {
                    player.hp -= dmg;
                    effects.add(new DamageNumberEffect(player.x, player.y, dmg, false, DamageSource.EXPLOSION));
                    triggerHitStop(0.03);
                    triggerShake(0.12, 7);
                    if (player.hp <= 0) { player.hp = 0; state = State.GAME_OVER; endRun(false); }
                }
                effects.add(new ExplosionBurstEffect(cx, cy, radius, 0.25));
            }));
        }

        private void gainXp(int amount) {
            player.xp += amount;
            while (player.xp >= player.xpToNext) {
                player.xp -= player.xpToNext;
                player.level++;
                player.xpToNext = xpRequirement(player.level);
                state = State.LEVEL_UP;
                currentChoices = upgradeSystem.roll3(player, rng);
                showReminder("LEVEL UP AVAILABLE!", 1.0);
                break;
            }
        }

        private void killEnemy(Enemy en, DamageSource source) {
            en.alive = false;

            // toxic death
            if (en.eliteType == EliteType.TOXIC) {
                effects.add(new ToxicFieldEffect(en.x, en.y, 110, 3.6, this));
                showReminder("TOXIC FIELD!", 0.9);
            }

            // xp drop
            int base = switch (en.type) {
                case FAST -> 2;
                case TANK -> 4;
                case RANGED -> 3;
                case BOSS -> 20;
                case BOSS_RANGED -> 12;
                default -> 2;
            };
            int xp = base + rng.nextInt(2);
            orbs.add(new XpOrb(en.x, en.y, xp));

            // heal on kill base
            if (player.healOnKill > 0) player.hp = Math.min(player.maxHp, player.hp + player.healOnKill);

            // sword+heal synergy: Slash kill -> extra heal
            if (source == DamageSource.SWORD) {
                player.hp = Math.min(player.maxHp, player.hp + 2.5);
            }

            // Blood set synergy: lifesteal on kill
            if (player.setBloodActive) {
                double heal = player.maxHp * 0.10;
                player.hp = Math.min(player.maxHp, player.hp + heal);
            }

            // item drop
            boolean isBoss = (en.type == EnemyType.BOSS || en.type == EnemyType.BOSS_RANGED);
            ItemDef def = itemDropSystem.rollDrop(rng, elapsedSeconds, isBoss, MetaProgression.rareDropBonus());
            if (def != null) items.add(new ItemPickup(en.x, en.y, def));
        }

        // helpers for skills
        Enemy findNearestEnemy() {
            Enemy best = null;
            double bestD2 = Double.POSITIVE_INFINITY;
            for (Enemy en : enemies) {
                if (!en.alive) continue;
                double dx = en.x - player.x;
                double dy = en.y - player.y;
                double d2 = dx*dx + dy*dy;
                if (d2 < bestD2) { bestD2 = d2; best = en; }
            }
            return best;
        }

        List<Enemy> findEnemiesInRange(double x, double y, double range) {
            double r2 = range * range;
            List<Enemy> out = new ArrayList<>();
            for (Enemy en : enemies) {
                if (!en.alive) continue;
                double dx = en.x - x;
                double dy = en.y - y;
                if (dx*dx + dy*dy <= r2) out.add(en);
            }
            return out;
        }

        void damageEnemy(Enemy en, double dmg, boolean canCrit, boolean applySlow, DamageSource src) {
            if (!en.alive) return;

            // shielded block for non-bullet too (front)
            if (en.eliteType == EliteType.SHIELDED) {
                if (en.blocksHitFrom(player.x, player.y)) {
                    addEffect(new BlockSparkEffect(en.x, en.y, 0.18));
                    return;
                }
            }

            double real = dmg;

            // ❄️ Ice + ⚡ synergy: slowed enemies take +30% lightning
            if (src == DamageSource.LIGHTNING && en.isSlowed()) {
                real *= 1.30;
            }

            // phasing: less affected by slow
            if (en.eliteType == EliteType.PHASING && src == DamageSource.ICE) {
                real *= 0.8;
            }

            en.hp -= real;

            if (applySlow && player.bulletSlowPct > 0) en.applySlow(player.bulletSlowPct, player.bulletSlowDuration);
            if (en.hp <= 0) killEnemy(en, src);
        }

        void spawnPlayerProjectile(double x, double y, double vx, double vy, double dmg) {
            Projectile p = new Projectile(x, y);
            p.vx = vx; p.vy = vy; p.damage = dmg;
            projectiles.add(p);
        }

        void addEffect(Effect fx) { effects.add(fx); }

        private void fireEnemyProjectile(Enemy shooter) {
            double dx = player.x - shooter.x;
            double dy = player.y - shooter.y;
            double d = Math.hypot(dx, dy);
            if (d < 0.0001) return;
            dx /= d; dy /= d;

            double speed = 260;
            double px = shooter.x + dx * (shooter.radius + 8);
            double py = shooter.y + dy * (shooter.radius + 8);

            EnemyProjectile p = new EnemyProjectile(px, py);
            p.vx = dx * speed;
            p.vy = dy * speed;
            p.damage = shooter.rangedDamage;
            enemyProjectiles.add(p);
        }

        private void spawnEnemyAroundPlayer() {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = 520 + rng.nextDouble() * 320;
            double ex = player.x + Math.cos(angle) * dist;
            double ey = player.y + Math.sin(angle) * dist;

            double hpScale = 1.0 + elapsedSeconds * 0.010;
            double baseHp = 18;

            double p = rng.nextDouble();
            EnemyType type;
            if (elapsedSeconds < 40) type = EnemyType.NORMAL;
            else if (elapsedSeconds < 90) type = (p < 0.75) ? EnemyType.NORMAL : EnemyType.FAST;
            else if (elapsedSeconds < 160) {
                if (p < 0.60) type = EnemyType.NORMAL;
                else if (p < 0.80) type = EnemyType.FAST;
                else type = EnemyType.RANGED;
            } else {
                if (p < 0.45) type = EnemyType.NORMAL;
                else if (p < 0.65) type = EnemyType.FAST;
                else if (p < 0.85) type = EnemyType.RANGED;
                else type = EnemyType.TANK;
            }

            double hp = baseHp * hpScale;
            Enemy e = Enemy.create(type, ex, ey, hp, elapsedSeconds, rng);

            // chance of elite
            double eliteChance = Math.min(0.22, 0.04 + elapsedSeconds * 0.0006);
            if (rng.nextDouble() < eliteChance && type != EnemyType.BOSS && type != EnemyType.BOSS_RANGED) {
                e.makeElite(rng);
                showReminder("ELITE APPROACHING!", 0.9);
            }

            enemies.add(e);
        }

        private void spawnBoss() {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = 680;
            double ex = player.x + Math.cos(angle) * dist;
            double ey = player.y + Math.sin(angle) * dist;

            Enemy boss = Enemy.create(EnemyType.BOSS, ex, ey, 520, elapsedSeconds, rng);
            boss.phase = 1;
            boss.patternCd = 1.0;
            enemies.add(boss);

            enemies.add(Enemy.create(EnemyType.BOSS_RANGED, ex + 80, ey + 60, 240, elapsedSeconds, rng));
        }

        private boolean circleHit(double x1, double y1, double r1, double x2, double y2, double r2) {
            double dx = x1 - x2;
            double dy = y1 - y2;
            double rr = r1 + r2;
            return (dx*dx + dy*dy) <= rr*rr;
        }

        private void resolveObstacleCollision(Entity ent) {
            for (Obstacle ob : obstacles) {
                double dx = ent.x - ob.x;
                double dy = ent.y - ob.y;
                double d = Math.hypot(dx, dy);
                double min = ent.radius + ob.r;
                if (d > 0.0001 && d < min) {
                    double push = (min - d);
                    ent.x += (dx / d) * push;
                    ent.y += (dy / d) * push;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // ✅ screen shake
            if (shakeLeft > 0) {
                double mag = shakePower * Math.min(1.0, shakeLeft / 0.18);
                int ox = (int)Math.round((rng.nextDouble()*2 - 1) * mag);
                int oy = (int)Math.round((rng.nextDouble()*2 - 1) * mag);
                g2.translate(ox, oy);
            }

            double camX = player != null ? player.x - VIEW_W / 2.0 : 0;
            double camY = player != null ? player.y - VIEW_H / 2.0 : 0;

            drawGrid(g2, camX, camY);

            if (player != null) {
                // obstacles
                if (mapMod == MapMod.OBSTACLES) {
                    for (Obstacle ob : obstacles) {
                        g2.setColor(new Color(70, 70, 80));
                        drawCircle(g2, ob.x - camX, ob.y - camY, ob.r, new Color(70,70,80));
                        g2.setColor(new Color(30,30,35,180));
                        g2.drawOval((int)Math.round(ob.x-camX-ob.r), (int)Math.round(ob.y-camY-ob.r),
                                (int)Math.round(ob.r*2), (int)Math.round(ob.r*2));
                    }
                }

                // items
                for (ItemPickup it : items) {
                    Color c = rarityColor(it.def.rarity);
                    drawDiamond(g2, it.x - camX, it.y - camY, it.radius, c);
                }

                // orbs
                for (XpOrb o : orbs) drawCircle(g2, o.x - camX, o.y - camY, o.radius, new Color(120, 220, 120));

                // enemies
                for (Enemy en : enemies) {
                    Color c = switch (en.type) {
                        case FAST -> new Color(170, 70, 70);
                        case TANK -> new Color(90, 45, 45);
                        case RANGED -> new Color(170, 110, 50);
                        case BOSS -> new Color(130, 20, 90);
                        case BOSS_RANGED -> new Color(160, 60, 120);
                        default -> new Color(120, 30, 30);
                    };

                    if (en.eliteType != EliteType.NONE) {
                        // aura
                        drawCircle(g2, en.x - camX, en.y - camY, en.radius + 10, en.eliteAuraColor());
                    }

                    drawCircle(g2, en.x - camX, en.y - camY, en.radius, c);

                    if (en.eliteType == EliteType.SHIELDED) {
                        // draw shield arc
                        int r = (int)Math.round(en.radius + 8);
                        int sx = (int)Math.round(en.x - camX - r);
                        int sy = (int)Math.round(en.y - camY - r);
                        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.setColor(new Color(200, 210, 230, 190));
                        g2.drawArc(sx, sy, r*2, r*2, (int)Math.toDegrees(en.shieldAngle - Math.PI/3), 120);
                    }

                    drawEnemyHpBar(g2, en, camX, camY);
                }

                // projectiles
                for (Projectile p : projectiles) drawCircle(g2, p.x - camX, p.y - camY, p.radius, new Color(230, 230, 245));
                for (EnemyProjectile p : enemyProjectiles) drawCircle(g2, p.x - camX, p.y - camY, p.radius, new Color(240, 170, 70));

                // FX world
                for (Effect fx : effects) fx.drawWorld(g2, camX, camY);

                // pickup ring
                g2.setColor(new Color(80, 160, 220, 50));
                int pr = (int)Math.round(player.pickupRadius);
                g2.drawOval((int)Math.round(VIEW_W/2.0 - pr), (int)Math.round(VIEW_H/2.0 - pr), pr*2, pr*2);

                // player
                drawCircle(g2, player.x - camX, player.y - camY, player.radius, new Color(40, 160, 220));
            }

            // HUD
            drawHUD(g2);

            // floating texts
            for (FloatingText ft : floatingTexts) ft.draw(g2, VIEW_W, VIEW_H);

            // FX screen
            for (Effect fx : effects) fx.drawScreen(g2, VIEW_W, VIEW_H);

            // fog
            if (mapMod == MapMod.FOG && player != null) drawFogVignette(g2);

            if (state == State.LEVEL_UP) drawLevelUpOverlay(g2);
            if (state == State.GAME_OVER || state == State.VICTORY) drawEndOverlay(g2);

            mouseClicked = false;
            g2.dispose();
        }

        private void drawFogVignette(Graphics2D g2) {
            // simple vignette: dark outside circle around center
            int cx = VIEW_W/2;
            int cy = VIEW_H/2;
            int r = 220;

            Shape oldClip = g2.getClip();
            Area a = new Area(new Rectangle2D.Double(0,0,VIEW_W,VIEW_H));
            a.subtract(new Area(new Ellipse2D.Double(cx-r, cy-r, r*2, r*2)));
            g2.setClip(a);
            g2.setColor(new Color(0,0,0,165));
            g2.fillRect(0,0,VIEW_W,VIEW_H);
            g2.setClip(oldClip);
        }

        private Color rarityColor(Rarity r) {
            return switch (r) {
                case COMMON -> new Color(190, 190, 190);
                case RARE -> new Color(100, 170, 255);
                case EPIC -> new Color(200, 90, 255);
            };
        }

        private void drawGrid(Graphics2D g2, double camX, double camY) {
            g2.setColor(new Color(16, 16, 22));
            g2.fillRect(0, 0, VIEW_W, VIEW_H);

            int grid = 60;
            g2.setColor(new Color(30, 30, 40));

            int x0 = (int) Math.floor(camX / grid) * grid;
            int y0 = (int) Math.floor(camY / grid) * grid;

            for (int x = x0; x < camX + VIEW_W + grid; x += grid) {
                int sx = (int) Math.round(x - camX);
                g2.drawLine(sx, 0, sx, VIEW_H);
            }
            for (int y = y0; y < camY + VIEW_H + grid; y += grid) {
                int sy = (int) Math.round(y - camY);
                g2.drawLine(0, sy, VIEW_W, sy);
            }
        }

        private void drawCircle(Graphics2D g2, double sx, double sy, double r, Color c) {
            g2.setColor(c);
            int d = (int) Math.round(r * 2);
            int x = (int) Math.round(sx - r);
            int y = (int) Math.round(sy - r);
            g2.fillOval(x, y, d, d);
        }

        private void drawDiamond(Graphics2D g2, double sx, double sy, double r, Color c) {
            int cx = (int)Math.round(sx);
            int cy = (int)Math.round(sy);
            int rr = (int)Math.round(r);
            int[] xs = {cx, cx+rr, cx, cx-rr};
            int[] ys = {cy-rr, cy, cy+rr, cy};
            g2.setColor(c);
            g2.fillPolygon(xs, ys, 4);
            g2.setColor(new Color(20,20,25,140));
            g2.drawPolygon(xs, ys, 4);
        }

        private void drawEnemyHpBar(Graphics2D g2, Enemy en, double camX, double camY) {
            int w = 34, h = 5;
            int sx = (int)Math.round(en.x - camX);
            int sy = (int)Math.round(en.y - camY);
            int x = sx - w/2;
            int y = sy - (int)Math.round(en.radius) - 12;

            double pct = Math.max(0, en.hp / en.maxHp);
            g2.setColor(new Color(0,0,0,120));
            g2.fillRoundRect(x, y, w, h, 6, 6);

            Color fill = (en.type == EnemyType.BOSS) ? new Color(210, 90, 220) : new Color(200, 80, 80);
            g2.setColor(fill);
            g2.fillRoundRect(x, y, (int)Math.round(w * pct), h, 6, 6);
        }

        private void drawHUD(Graphics2D g2) {
            lootPanelHoveredIndex = -1;

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
            g2.setColor(new Color(240, 240, 240));

            int remain = Math.max(0, targetSeconds - elapsedSeconds);
            g2.drawString("Kalan: " + formatTime(remain), 16, 26);

            // HP
            double hpPct = player.hp / player.maxHp;
            int barW = 260, barH = 14;
            int bx = 16, by = 40;

            g2.setColor(new Color(50, 50, 60));
            g2.fillRoundRect(bx, by, barW, barH, 8, 8);
            g2.setColor(new Color(180, 40, 60));
            g2.fillRoundRect(bx, by, (int) Math.round(barW * hpPct), barH, 8, 8);
            g2.setColor(new Color(220, 220, 220));
            g2.drawRoundRect(bx, by, barW, barH, 8, 8);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            g2.drawString(String.format(Locale.US, "HP: %.0f/%.0f", player.hp, player.maxHp), bx, by + 28);

            // XP
            double xpPct = (player.xpToNext <= 0) ? 0 : (player.xp / (double) player.xpToNext);
            int xb = 16, yb = 78;
            g2.setColor(new Color(50, 50, 60));
            g2.fillRoundRect(xb, yb, barW, barH, 8, 8);
            g2.setColor(new Color(120, 180, 240));
            g2.fillRoundRect(xb, yb, (int) Math.round(barW * xpPct), barH, 8, 8);
            g2.setColor(new Color(220, 220, 220));
            g2.drawRoundRect(xb, yb, barW, barH, 8, 8);
            g2.drawString("LVL: " + player.level + "  XP: " + player.xp + "/" + player.xpToNext, xb, yb + 28);

            // Skills list
            g2.setColor(new Color(170, 170, 180));
            g2.drawString("Skills:", 16, 150);
            int yy = 168;
            for (Skill sk : player.skills) {
                g2.drawString("- " + sk.getName() + " (Lv " + sk.getLevel() + ")", 16, yy);
                yy += 16;
            }

            // Set status
            g2.setColor(new Color(160, 200, 255));
            g2.drawString("Set: Frost=" + (player.setFrostActive ? "ON" : "OFF") + "   Blood=" + (player.setBloodActive ? "ON" : "OFF"), 16, yy + 10);

            // Loot panel
            drawLootPanel(g2);

            // Reminder
            if (reminderLeft > 0) {
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
                String t = reminderText;
                int tw = g2.getFontMetrics().stringWidth(t);
                int x = (VIEW_W - tw)/2;
                int y = 80;
                g2.setColor(new Color(0,0,0,140));
                g2.fillRoundRect(x-14, y-26, tw+28, 34, 14, 14);
                g2.setColor(new Color(245,245,245));
                g2.drawString(t, x, y);
            }

            g2.setColor(new Color(160, 160, 170));
            g2.drawString("ESC: Menü | Level Up: 1-2-3 / tıkla | R: Yeniden", 16, VIEW_H - 16);
        }

        private void drawLootPanel(Graphics2D g2) {
            int panelW = 300;
            int panelH = 220;
            int x = VIEW_W - panelW - 16;
            int y = 16;

            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(x, y, panelW, panelH, 14, 14);

            g2.setColor(new Color(220, 220, 230));
            g2.drawRoundRect(x, y, panelW, panelH, 14, 14);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString("LOOT (son kazançlar)", x + 12, y + 22);

            int listY = y + 40;
            int rowH = 22;
            int maxShow = 7;

            int invSize = player.inventory.size();
            int show = Math.min(maxShow, invSize);

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));

            for (int i = 0; i < show; i++) {
                int idx = invSize - 1 - i;
                ItemDef it = player.inventory.get(idx);

                int ry = listY + i * rowH;
                Rectangle rowRect = new Rectangle(x + 10, ry - 14, panelW - 20, rowH);

                boolean hover = rowRect.contains(mouseX, mouseY);
                if (hover) lootPanelHoveredIndex = idx;

                if (hover) {
                    g2.setColor(new Color(255, 255, 255, 35));
                    g2.fillRoundRect(rowRect.x, rowRect.y, rowRect.width, rowRect.height, 10, 10);
                }

                g2.setColor(rarityColor(it.rarity));
                g2.fillOval(x + 14, ry - 10, 10, 10);

                g2.setColor(new Color(220, 220, 230));
                String line = it.rarity + " - " + it.name;
                g2.drawString(trimToWidth(g2, line, panelW - 55), x + 30, ry);
            }

            // details
            int dx = x + 10;
            int dy = y + panelH - 62;
            int dw = panelW - 20;
            int dh = 52;

            g2.setColor(new Color(0, 0, 0, 110));
            g2.fillRoundRect(dx, dy, dw, dh, 10, 10);
            g2.setColor(new Color(200, 200, 210, 140));
            g2.drawRoundRect(dx, dy, dw, dh, 10, 10);

            g2.setColor(new Color(200, 200, 210));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));

            if (lootPanelHoveredIndex >= 0 && lootPanelHoveredIndex < player.inventory.size()) {
                ItemDef it = player.inventory.get(lootPanelHoveredIndex);
                g2.setColor(rarityColor(it.rarity));
                g2.drawString(it.rarity + "  " + it.name, dx + 10, dy + 18);

                g2.setColor(new Color(220, 220, 230));
                drawWrapped(g2, it.desc, dx + 10, dy + 36, dw - 20, 14);
            } else {
                g2.drawString("Üzerine gel: detay gör", dx + 10, dy + 20);
                g2.drawString("Loot sayısı: " + player.inventory.size(), dx + 10, dy + 40);
            }
        }

        private String trimToWidth(Graphics2D g2, String s, int maxW) {
            if (g2.getFontMetrics().stringWidth(s) <= maxW) return s;
            String ell = "...";
            int target = maxW - g2.getFontMetrics().stringWidth(ell);
            if (target <= 0) return ell;
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                String t = out.toString() + s.charAt(i);
                if (g2.getFontMetrics().stringWidth(t) > target) break;
                out.append(s.charAt(i));
            }
            return out + ell;
        }

        private void drawLevelUpOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, VIEW_W, VIEW_H);

            g2.setColor(new Color(245, 245, 245));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 34f));
            String title = "LEVEL UP! (LVL " + player.level + ")";
            int tw = g2.getFontMetrics().stringWidth(title);
            g2.drawString(title, (VIEW_W - tw) / 2, VIEW_H / 2 - 110);

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            String sub = "Birini seç: 1-2-3 veya karta tıkla";
            int sw = g2.getFontMetrics().stringWidth(sub);
            g2.setColor(new Color(210, 210, 210));
            g2.drawString(sub, (VIEW_W - sw) / 2, VIEW_H / 2 - 86);

            Rectangle[] rects = levelUpCardRects();
            for (int i = 0; i < rects.length; i++) {
                Rectangle r = rects[i];
                boolean hover = r.contains(mouseX, mouseY);

                g2.setColor(new Color(30, 30, 40, hover ? 240 : 220));
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 18, 18);
                g2.setColor(new Color(220, 220, 220));
                g2.drawRoundRect(r.x, r.y, r.width, r.height, 18, 18);

                if (i < currentChoices.size()) {
                    Upgrade u = currentChoices.get(i);

                    g2.setColor(new Color(245, 245, 245));
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
                    g2.drawString((i + 1) + ") " + u.getName(), r.x + 14, r.y + 28);

                    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 13f));
                    g2.setColor(new Color(200, 200, 210));
                    drawWrapped(g2, u.getDesc(), r.x + 14, r.y + 52, r.width - 20, 16);

                    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                    g2.setColor(new Color(160, 160, 170));
                    g2.drawString(u.getTag(), r.x + 14, r.y + r.height - 14);
                }
            }
        }

        private void drawEndOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, VIEW_W, VIEW_H);

            String title = (state == State.VICTORY) ? "VICTORY!" : "GAME OVER";
            String sub = (state == State.VICTORY) ? "Süre doldu. Tebrikler!" : "Canın bitti.";
            String stats = "Süre: " + formatTime(elapsedSeconds) + "   Level: " + player.level + "   Loot: " + player.inventory.size();

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 56f));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (VIEW_W - fm.stringWidth(title)) / 2;
            int ty = VIEW_H / 2 - 70;
            g2.setColor(new Color(245, 245, 245));
            g2.drawString(title, tx, ty);

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(210, 210, 210));
            g2.drawString(sub, (VIEW_W - fm.stringWidth(sub)) / 2, ty + 36);

            g2.setColor(new Color(170, 170, 180));
            g2.drawString(stats, (VIEW_W - fm.stringWidth(stats)) / 2, ty + 64);

            g2.setColor(new Color(160, 160, 170));
            g2.drawString("R: tekrar   |   ESC: menü", (VIEW_W - fm.stringWidth("R: tekrar   |   ESC: menü")) / 2, ty + 94);
        }

        private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxWidth, int lineH) {
            String[] words = text.split("\\s+");
            StringBuilder line = new StringBuilder();
            int cy = y;
            for (String w : words) {
                String test = line.isEmpty() ? w : line + " " + w;
                if (g2.getFontMetrics().stringWidth(test) > maxWidth) {
                    g2.drawString(line.toString(), x, cy);
                    line = new StringBuilder(w);
                    cy += lineH;
                } else line = new StringBuilder(test);
            }
            if (!line.isEmpty()) g2.drawString(line.toString(), x, cy);
        }

        private String formatTime(int sec) {
            int m = sec / 60;
            int s = sec % 60;
            return String.format("%02d:%02d", m, s);
        }
    }

    // ============================ FX ============================
    static class Effect {
        boolean alive = true;
        double life;
        void update(double dt) { life -= dt; if (life <= 0) alive = false; }
        void drawWorld(Graphics2D g2, double camX, double camY) {}
        void drawScreen(Graphics2D g2, int W, int H) {}
    }

    static class DamageNumberEffect extends Effect {
        final double x, y;
        final String text;
        final Color color;
        double vy = -32;

        DamageNumberEffect(double x, double y, double dmg, boolean crit, GamePanel.DamageSource src) {
            this.x = x; this.y = y;
            this.life = 0.65;
            this.text = String.valueOf((int)Math.round(dmg));

            this.color = switch (src) {
                case ICE -> new Color(140, 220, 255);
                case LIGHTNING -> new Color(210, 170, 255);
                case LASER, EXPLOSION -> new Color(255, 120, 120);
                default -> crit ? new Color(255, 230, 120) : new Color(245,245,245);
            };
        }

        @Override
        void drawWorld(Graphics2D g2, double camX, double camY) {
            float a = (float)Math.max(0, Math.min(1, life / 0.65));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f + 0.80f * a));

            int sx = (int)Math.round(x - camX);
            int sy = (int)Math.round(y - camY - (0.65-life)*28);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.setColor(new Color(0,0,0,140));
            int tw = g2.getFontMetrics().stringWidth(text);
            g2.fillRoundRect(sx - tw/2 - 8, sy - 16, tw + 16, 20, 10, 10);

            g2.setColor(color);
            g2.drawString(text, sx - tw/2, sy);

            g2.setComposite(old);
        }
    }

    static class SwordArcEffect extends Effect {
        final double x, y;
        final double radius;
        final double startAngle, extent;
        SwordArcEffect(double x, double y, double radius, double seconds) {
            this.x = x; this.y = y;
            this.radius = radius;
            this.life = seconds;
            double a = Math.random() * Math.PI * 2;
            this.startAngle = Math.toDegrees(a);
            this.extent = 120;
        }
        @Override
        void drawWorld(Graphics2D g2, double camX, double camY) {
            float alpha = (float)Math.max(0, Math.min(1, life / 0.25));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f + 0.35f * alpha));
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(240, 240, 255));
            int r = (int)Math.round(radius);
            int sx = (int)Math.round(x - camX - r);
            int sy = (int)Math.round(y - camY - r);
            g2.drawArc(sx, sy, r*2, r*2, (int)startAngle, (int)extent);
            g2.setComposite(old);
        }
    }

    static class IceFieldEffect extends Effect {
        final Player p;
        final double radius;
        IceFieldEffect(Player p, double radius, double seconds) {
            this.p = p;
            this.radius = radius;
            this.life = seconds;
        }
        @Override
        void drawWorld(Graphics2D g2, double camX, double camY) {
            float alpha = (float)Math.max(0, Math.min(1, life / 1.0));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f + 0.12f * alpha));

            int r = (int)Math.round(radius);
            int sx = (int)Math.round(p.x - camX - r);
            int sy = (int)Math.round(p.y - camY - r);

            g2.setColor(new Color(120, 200, 255));
            g2.fillOval(sx, sy, r*2, r*2);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(200, 240, 255));
            g2.drawOval(sx, sy, r*2, r*2);

            g2.setComposite(old);
        }
    }

    static class LightningEffect extends Effect {
        final List<double[]> pts;
        LightningEffect(List<double[]> pts, double seconds) { this.pts = pts; this.life = seconds; }
        @Override
        void drawWorld(Graphics2D g2, double camX, double camY) {
            if (pts.size() < 2) return;
            float a = (float)Math.max(0, Math.min(1, life / 0.18));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f + 0.45f * a));
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(220, 210, 255));

            for (int i = 0; i < pts.size()-1; i++) {
                double[] p1 = pts.get(i);
                double[] p2 = pts.get(i+1);

                int x1 = (int)Math.round(p1[0] - camX);
                int y1 = (int)Math.round(p1[1] - camY);
                int x2 = (int)Math.round(p2[0] - camX);
                int y2 = (int)Math.round(p2[1] - camY);

                int mx = (x1 + x2) / 2;
                int my = (y1 + y2) / 2;
                int off = 10;
                int jx = mx + (i%2==0 ? off : -off);
                int jy = my + (i%2==0 ? -off : off);

                g2.drawLine(x1, y1, jx, jy);
                g2.drawLine(jx, jy, x2, y2);
            }
            g2.setComposite(old);
        }
    }

    static class LaserTelegraphEffect extends Effect {
        final double x1,y1,x2,y2;
        final Runnable onFire;
        LaserTelegraphEffect(double x1,double y1,double x2,double y2,double warnSec,Runnable onFire) {
            this.x1=x1; this.y1=y1; this.x2=x2; this.y2=y2;
            this.life = warnSec;
            this.onFire = onFire;
        }
        @Override
        void update(double dt) {
            life -= dt;
            if (life <= 0) {
                alive = false;
                onFire.run();
            }
        }
        @Override
        void drawWorld(Graphics2D g2, double camX, double camY) {
            float a = (float)Math.max(0, Math.min(1, life / 0.8));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f + 0.32f * a));
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 120, 120));
            g2.drawLine((int)Math.round(x1-camX),(int)Math.round(y1-camY),(int)Math.round(x2-camX),(int)Math.round(y2-camY));
            g2.setComposite(old);
        }
    }

    static class LaserBeamEffect extends Effect {
        final double x1,y1,x2,y2;
        final double width;
        final GamePanel world;
        LaserBeamEffect(double x1,double y1,double x2,double y2,double sec,double width, GamePanel world) {
            this.x1=x1; this.y1=y1; this.x2=x2; this.y2=y2;
            this.life = sec;
            this.width = width;
            this.world = world;
        }
        @Override
        void update(double dt) {
            super.update(dt);
            // damage player if close to line
            if (!alive || dt <= 0) return;
            double dist = distPointToSegment(world.player.x, world.player.y, x1,y1,x2,y2);
            if (dist <= width) {
                double dmg = 12 * dt * 6.5; // bursty
                world.player.hp -= dmg;
                world.effects.add(new DamageNumberEffect(world.player.x, world.player.y, dmg, false, GamePanel.DamageSource.LASER));
                if (world.player.hp <= 0) {
                    world.player.hp = 0;
                    world.state = GamePanel.State.GAME_OVER;
                    world.endRun(false);
                }
            }
        }
        @Override
        void drawWorld(Graphics2D g2, double camX, double camY) {
            float a = (float)Math.max(0, Math.min(1, life / 0.22));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f + 0.55f * a));
            g2.setStroke(new BasicStroke((float)width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 160, 160));
            g2.drawLine((int)Math.round(x1-camX),(int)Math.round(y1-camY),(int)Math.round(x2-camX),(int)Math.round(y2-camY));
            g2.setComposite(old);
        }
        static double distPointToSegment(double px,double py,double x1,double y1,double x2,double y2) {
            double vx = x2-x1, vy = y2-y1;
            double wx = px-x1, wy = py-y1;
            double c1 = wx*vx + wy*vy;
            if (c1 <= 0) return Math.hypot(px-x1, py-y1);
            double c2 = vx*vx + vy*vy;
            if (c2 <= c1) return Math.hypot(px-x2, py-y2);
            double t = c1 / c2;
            double bx = x1 + t*vx;
            double by = y1 + t*vy;
            return Math.hypot(px-bx, py-by);
        }
    }

    static class ExplosionTelegraphEffect extends Effect {
        final double cx, cy, r;
        final Runnable onBoom;
        ExplosionTelegraphEffect(double cx,double cy,double r,double sec,Runnable onBoom) {
            this.cx=cx; this.cy=cy; this.r=r;
            this.life = sec;
            this.onBoom = onBoom;
        }
        @Override
        void update(double dt) {
            life -= dt;
            if (life <= 0) {
                alive = false;
                onBoom.run();
            }
        }
        @Override
        void drawWorld(Graphics2D g2, double camX, double camY) {
            float a = (float)Math.max(0, Math.min(1, life / 0.75));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f + 0.25f * a));
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 140, 120));
            int rr = (int)Math.round(r);
            int sx = (int)Math.round(cx - camX - rr);
            int sy = (int)Math.round(cy - camY - rr);
            g2.drawOval(sx, sy, rr*2, rr*2);
            g2.setComposite(old);
        }
    }

    static class ExplosionBurstEffect extends Effect {
        final double cx,cy,r;
        ExplosionBurstEffect(double cx,double cy,double r,double sec) {
            this.cx=cx; this.cy=cy; this.r=r; this.life=sec;
        }
        @Override
        void drawWorld(Graphics2D g2, double camX, double camY) {
            float a = (float)Math.max(0, Math.min(1, life / 0.25));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f + 0.55f * a));
            g2.setColor(new Color(255, 180, 150));
            int rr = (int)Math.round(r);
            int sx = (int)Math.round(cx - camX - rr);
            int sy = (int)Math.round(cy - camY - rr);
            g2.fillOval(sx, sy, rr*2, rr*2);
            g2.setComposite(old);
        }
    }

    static class ToxicFieldEffect extends Effect {
        final double cx,cy,r;
        final GamePanel world;
        ToxicFieldEffect(double cx,double cy,double r,double sec, GamePanel world) {
            this.cx=cx; this.cy=cy; this.r=r; this.life=sec; this.world = world;
        }
        @Override
        void update(double dt) {
            super.update(dt);
            if (!alive || dt<=0) return;
            double dist = Math.hypot(world.player.x - cx, world.player.y - cy);
            if (dist <= r) {
                double dmg = 6.5 * dt;
                world.player.hp -= dmg;
                world.effects.add(new DamageNumberEffect(world.player.x, world.player.y, dmg, false, GamePanel.DamageSource.EXPLOSION));
                if (world.player.hp <= 0) {
                    world.player.hp = 0;
                    world.state = GamePanel.State.GAME_OVER;
                    world.endRun(false);
                }
            }
        }
        @Override
        void drawWorld(Graphics2D g2, double camX, double camY) {
            float a = (float)Math.max(0, Math.min(1, life / 3.6));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f + 0.22f * a));
            g2.setColor(new Color(120, 255, 150));
            int rr = (int)Math.round(r);
            int sx = (int)Math.round(cx - camX - rr);
            int sy = (int)Math.round(cy - camY - rr);
            g2.fillOval(sx, sy, rr*2, rr*2);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(200, 255, 220));
            g2.drawOval(sx, sy, rr*2, rr*2);

            g2.setComposite(old);
        }
    }

    static class BlockSparkEffect extends Effect {
        final double x,y;
        BlockSparkEffect(double x,double y,double sec){ this.x=x; this.y=y; this.life=sec; }
        @Override
        void drawWorld(Graphics2D g2, double camX, double camY) {
            float a = (float)Math.max(0, Math.min(1, life / 0.18));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f + 0.65f * a));
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(230, 240, 255));
            int sx = (int)Math.round(x - camX);
            int sy = (int)Math.round(y - camY);
            g2.drawLine(sx-10, sy, sx+10, sy);
            g2.drawLine(sx, sy-10, sx, sy+10);
            g2.setComposite(old);
        }
    }

    static class ArenaRingEffect extends Effect {
        interface D { double get(); }
        final D px, py, rad;
        ArenaRingEffect(D px, D py, D rad, double sec) { this.px=px; this.py=py; this.rad=rad; this.life=sec; }
        @Override
        void update(double dt) { /* infinite by life */ }
        @Override
        void drawWorld(Graphics2D g2, double camX, double camY) {
            double r = rad.get();
            int rr = (int)Math.round(r);
            int sx = (int)Math.round(px.get() - camX - rr);
            int sy = (int)Math.round(py.get() - camY - rr);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 180, 220, 120));
            g2.drawOval(sx, sy, rr*2, rr*2);
        }
    }

    // ============================ ENTITIES ============================
    static class Entity {
        double x, y;
        double vx, vy;
        double radius;
        boolean alive = true;
    }

    static class Obstacle {
        final double x,y,r;
        Obstacle(double x,double y,double r){ this.x=x; this.y=y; this.r=r; }
    }

    static class Player extends Entity {
        double maxHp = 100;
        double hp = maxHp;

        double moveSpeed = 260;

        double fireDelay = 0.30;
        double bulletDamage = 10;
        double bulletSpeed = 520;

        int multiShot = 1;
        double multiShotSpreadRad = Math.toRadians(8);

        double critChance = 0.05;
        double critMultiplier = 2.0;

        double bulletSlowPct = 0.0;
        double bulletSlowDuration = 1.2;

        int level = 1;
        int xp = 0;
        int xpToNext = 10;

        double pickupRadius = 42;
        double magnetRadius = 140;

        double healOnKill = 0;

        final List<Skill> skills = new ArrayList<>();
        final List<ItemDef> inventory = new ArrayList<>();

        // set flags
        boolean setFrostActive = false;
        boolean setBloodActive = false;

        Player(double x, double y) {
            this.x = x; this.y = y;
            this.radius = 16;
        }

        boolean hasSkill(Class<? extends Skill> cls) {
            for (Skill s : skills) if (cls.isInstance(s)) return true;
            return false;
        }

        Skill getSkill(Class<? extends Skill> cls) {
            for (Skill s : skills) if (cls.isInstance(s)) return s;
            return null;
        }

        void updateSets() {
            boolean hasFrostRune = false, hasGlacialHeart = false;
            boolean hasBloodSigil = false, hasExecMark = false;

            for (ItemDef it : inventory) {
                if (it.name.equals("Frost Rune")) hasFrostRune = true;
                if (it.name.equals("Glacial Heart")) hasGlacialHeart = true;
                if (it.name.equals("Blood Sigil")) hasBloodSigil = true;
                if (it.name.equals("Executioner Mark")) hasExecMark = true;
            }
            setFrostActive = hasFrostRune && hasGlacialHeart;
            setBloodActive = hasBloodSigil && hasExecMark;
        }
    }

    enum EnemyType { NORMAL, FAST, TANK, RANGED, BOSS, BOSS_RANGED }
    enum EliteType { NONE, BERSERKER, SHIELDED, TOXIC, PHASING }

    static class Enemy extends Entity {
        EnemyType type = EnemyType.NORMAL;

        double maxHp;
        double hp;

        double baseSpeed;
        double contactDps;

        double slowMult = 1.0;
        double slowTimer = 0;

        double baseShootDelay = 1.6;
        double shootCooldown = 0.6;
        double rangedDamage = 7;

        // elites
        EliteType eliteType = EliteType.NONE;
        double shieldAngle = 0;

        // boss patterns
        int phase = 1;
        double patternCd = 1.0;

        // multi-hit tracking
        int recentHitCount = 0;
        double recentHitTimer = 0;

        static Enemy create(EnemyType type, double x, double y, double hp, int elapsedSeconds, Random rng) {
            Enemy e = new Enemy();
            e.type = type;
            e.x = x; e.y = y;

            switch (type) {
                case FAST -> {
                    e.radius = 12;
                    e.baseSpeed = 165;
                    e.contactDps = 16;
                    e.maxHp = hp * 0.75;
                }
                case TANK -> {
                    e.radius = 18;
                    e.baseSpeed = 85;
                    e.contactDps = 22;
                    e.maxHp = hp * 2.2;
                }
                case RANGED -> {
                    e.radius = 14;
                    e.baseSpeed = 105;
                    e.contactDps = 10;
                    e.maxHp = hp * 1.2;
                    e.baseShootDelay = 1.8;
                    e.shootCooldown = 0.8;
                    e.rangedDamage = 7 + elapsedSeconds * 0.01;
                }
                case BOSS -> {
                    e.radius = 28;
                    e.baseSpeed = 95;
                    e.contactDps = 36;
                    e.maxHp = Math.max(420, hp);
                }
                case BOSS_RANGED -> {
                    e.radius = 20;
                    e.baseSpeed = 110;
                    e.contactDps = 14;
                    e.maxHp = Math.max(220, hp);
                    e.baseShootDelay = 1.15;
                    e.shootCooldown = 0.6;
                    e.rangedDamage = 10;
                }
                default -> {
                    e.radius = 14;
                    e.baseSpeed = 120;
                    e.contactDps = 18;
                    e.maxHp = hp;
                }
            }

            e.hp = e.maxHp;
            return e;
        }

        void makeElite(Random rng) {
            EliteType[] types = { EliteType.BERSERKER, EliteType.SHIELDED, EliteType.TOXIC, EliteType.PHASING };
            eliteType = types[rng.nextInt(types.length)];

            switch (eliteType) {
                case BERSERKER -> {
                    baseSpeed *= 1.5;
                    maxHp *= 0.75;
                    hp = Math.min(hp, maxHp);
                    contactDps *= 1.25;
                }
                case SHIELDED -> {
                    maxHp *= 1.25;
                    hp = maxHp;
                }
                case TOXIC -> {
                    maxHp *= 1.15;
                    hp = maxHp;
                }
                case PHASING -> {
                    baseSpeed *= 1.18;
                    maxHp *= 0.90;
                    hp = Math.min(hp, maxHp);
                }
                default -> {}
            }
        }

        Color eliteAuraColor() {
            return switch (eliteType) {
                case BERSERKER -> new Color(255, 80, 80, 70);
                case SHIELDED -> new Color(200, 220, 255, 70);
                case TOXIC -> new Color(120, 255, 170, 70);
                case PHASING -> new Color(200, 200, 220, 70);
                default -> new Color(0,0,0,0);
            };
        }

        boolean isSlowed() {
            return slowTimer > 0 && slowMult < 0.999;
        }

        void applySlow(double slowPct, double duration) {
            // phasing: resistant
            if (eliteType == EliteType.PHASING) slowPct *= 0.55;

            double mult = Math.max(0.45, 1.0 - slowPct);
            if (mult < slowMult) slowMult = mult;
            slowTimer = Math.max(slowTimer, duration);
        }

        boolean blocksHitFrom(double ax, double ay) {
            // shield "front" faces attacker direction; blocks within ~120° cone
            double dx = ax - x, dy = ay - y;
            double angToAtt = Math.atan2(dy, dx);
            double diff = wrapAngle(angToAtt - shieldAngle);
            return Math.abs(diff) < Math.toRadians(60);
        }

        static double wrapAngle(double a) {
            while (a > Math.PI) a -= Math.PI*2;
            while (a < -Math.PI) a += Math.PI*2;
            return a;
        }

        void update(Player player, double dt) {
            if (!alive) return;

            // shield faces player
            shieldAngle = Math.atan2(player.y - y, player.x - x);

            if (slowTimer > 0) {
                slowTimer -= dt;
                if (slowTimer <= 0) { slowTimer = 0; slowMult = 1.0; }
            }

            double dx = player.x - x;
            double dy = player.y - y;
            double dist = Math.hypot(dx, dy);
            if (dist > 0.0001) { dx /= dist; dy /= dist; }

            double speed = baseSpeed * slowMult;

            if (type == EnemyType.RANGED || type == EnemyType.BOSS_RANGED) {
                double desired = (type == EnemyType.BOSS_RANGED) ? 260 : 220;
                if (dist < desired - 40) { vx = -dx * speed; vy = -dy * speed; }
                else if (dist > desired + 40) { vx = dx * speed; vy = dy * speed; }
                else { vx = 0; vy = 0; }
            } else {
                vx = dx * speed;
                vy = dy * speed;
            }

            x += vx * dt;
            y += vy * dt;

            if (hp <= 0) alive = false;
        }
    }

    static class Projectile extends Entity {
        double damage = 10;
        double life = 1.6;
        Projectile(double x, double y) { this.x = x; this.y = y; this.radius = 5; }
    }

    static class EnemyProjectile extends Entity {
        double damage = 8;
        double life = 2.2;
        EnemyProjectile(double x, double y) { this.x = x; this.y = y; this.radius = 5; }
    }

    static class XpOrb extends Entity {
        int value;
        XpOrb(double x, double y, int value) { this.x = x; this.y = y; this.value = value; this.radius = 6; }
        void update(Player player, double dt) {
            if (!alive) return;
            double dx = player.x - x;
            double dy = player.y - y;
            double d = Math.hypot(dx, dy);
            if (d < player.magnetRadius && d > 0.0001) {
                double pull = 220 + (player.magnetRadius - d) * 2;
                x += (dx / d) * pull * dt;
                y += (dy / d) * pull * dt;
            }
        }
    }

    // ============================ LOOT ============================
    enum Rarity { COMMON, RARE, EPIC }

    static class ItemDef {
        final Rarity rarity;
        final String name;
        final String desc;
        final Consumer<Player> apply;

        ItemDef(Rarity rarity, String name, String desc, Consumer<Player> apply) {
            this.rarity = rarity;
            this.name = name;
            this.desc = desc;
            this.apply = apply;
        }
        void apply(Player p) { apply.accept(p); }
    }

    static class ItemPickup extends Entity {
        final ItemDef def;
        ItemPickup(double x, double y, ItemDef def) { this.x = x; this.y = y; this.def = def; this.radius = 10; }
    }

    static class ItemDropSystem {
        final List<ItemDef> common = new ArrayList<>();
        final List<ItemDef> rare = new ArrayList<>();
        final List<ItemDef> epic = new ArrayList<>();

        ItemDropSystem() {
            // COMMON
            common.add(new ItemDef(Rarity.COMMON, "Sharpened Tip", "+%12 mermi hasarı", p -> p.bulletDamage *= 1.12));
            common.add(new ItemDef(Rarity.COMMON, "Quick Hands", "Ateş aralığı -%8", p -> p.fireDelay = Math.max(0.10, p.fireDelay * 0.92)));
            common.add(new ItemDef(Rarity.COMMON, "Light Boots", "+%8 hareket hızı", p -> p.moveSpeed *= 1.08));
            common.add(new ItemDef(Rarity.COMMON, "Magnet Shard", "Toplama +%12, çekim +%8", p -> { p.pickupRadius *= 1.12; p.magnetRadius *= 1.08; }));
            common.add(new ItemDef(Rarity.COMMON, "Minor Vitality", "+10 max HP (doldurur)", p -> { p.maxHp += 10; p.hp = Math.min(p.maxHp, p.hp + 10); }));

            // RARE
            rare.add(new ItemDef(Rarity.RARE, "Frost Rune", "Mermiler %20 yavaşlatır", p -> { p.bulletSlowPct = Math.min(0.55, p.bulletSlowPct + 0.20); p.bulletSlowDuration = 1.2; }));
            rare.add(new ItemDef(Rarity.RARE, "Critical Eye", "+%8 kritik şansı", p -> p.critChance = Math.min(0.60, p.critChance + 0.08)));
            rare.add(new ItemDef(Rarity.RARE, "Double Barrel", "+1 Multi-shot", p -> p.multiShot = Math.min(7, p.multiShot + 1)));
            rare.add(new ItemDef(Rarity.RARE, "Blood Sigil", "Kill başına +2 HP", p -> p.healOnKill += 2));

            // EPIC
            epic.add(new ItemDef(Rarity.EPIC, "Executioner Mark", "Kritik çarpanı +1.0x", p -> p.critMultiplier = Math.min(5.0, p.critMultiplier + 1.0)));
            epic.add(new ItemDef(Rarity.EPIC, "Overclock Core", "Ateş aralığı -%18", p -> p.fireDelay = Math.max(0.09, p.fireDelay * 0.82)));
            epic.add(new ItemDef(Rarity.EPIC, "Arc Catalyst", "Chain Lightning açar / güçlendirir", p -> {
                if (!p.hasSkill(ChainLightningSkill.class)) p.skills.add(new ChainLightningSkill());
                else ((ChainLightningSkill)p.getSkill(ChainLightningSkill.class)).levelUp();
            }));
            epic.add(new ItemDef(Rarity.EPIC, "Glacial Heart", "Ice Field açar / güçlendirir", p -> {
                if (!p.hasSkill(IceFieldSkill.class)) p.skills.add(new IceFieldSkill());
                else ((IceFieldSkill)p.getSkill(IceFieldSkill.class)).levelUp();
            }));
        }

        ItemDef rollDrop(Random rng, int elapsedSeconds, boolean isBoss, double metaRareBonus) {
            if (isBoss) {
                return rng.nextDouble() < (0.35 + metaRareBonus) ? epic.get(rng.nextInt(epic.size())) : rare.get(rng.nextInt(rare.size()));
            }

            double baseChance = 0.08 + Math.min(0.07, elapsedSeconds * 0.0003);
            if (rng.nextDouble() > baseChance) return null;

            double r = rng.nextDouble();
            double rareBoost = metaRareBonus;

            if (r < 0.72 - rareBoost*0.25) return common.get(rng.nextInt(common.size()));
            if (r < 0.95) return rare.get(rng.nextInt(rare.size()));
            return epic.get(rng.nextInt(epic.size()));
        }
    }

    // ============================ FLOATING TEXT ============================
    static class FloatingText {
        String text;
        double timeLeft;
        boolean alive = true;
        int x, y;
        Color color;

        static FloatingText center(String t, double seconds) {
            FloatingText ft = new FloatingText();
            ft.text = t;
            ft.timeLeft = seconds;
            ft.x = -1; ft.y = -1;
            ft.color = new Color(245, 245, 245);
            return ft;
        }

        static FloatingText loot(String t, double seconds) {
            FloatingText ft = new FloatingText();
            ft.text = t;
            ft.timeLeft = seconds;
            ft.x = 16; ft.y = 230;
            ft.color = new Color(220, 220, 210);
            return ft;
        }

        void update(double dt) {
            timeLeft -= dt;
            if (timeLeft <= 0) alive = false;
        }

        void draw(Graphics2D g2, int W, int H) {
            if (!alive) return;
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
            g2.setColor(new Color(0,0,0,120));
            int xx = (x < 0) ? W/2 : x;
            int yy = (y < 0) ? H/2 - 140 : y;
            int tw = g2.getFontMetrics().stringWidth(text);
            int px = (x < 0) ? (W - tw)/2 : xx;
            g2.fillRoundRect(px-10, yy-18, tw+20, 26, 10, 10);

            g2.setColor(color);
            g2.drawString(text, px, yy);
        }
    }

    // ============================ SKILLS ============================
    interface Skill {
        String getName();
        int getLevel();
        void levelUp();
        void update(double dt, Player p, GamePanel world);
    }

    static class BasicShotSkill implements Skill {
        int level = 1;
        double cd = 0;
        @Override public String getName() { return "Basic Shot"; }
        @Override public int getLevel() { return level; }
        @Override public void levelUp() { level = Math.min(10, level+1); }
        @Override
        public void update(double dt, Player p, GamePanel world) {
            cd -= dt;
            double delay = p.fireDelay * Math.max(0.70, 1.0 - (level-1) * 0.03);
            if (cd > 0) return;

            Enemy target = world.findNearestEnemy();
            if (target == null) return;

            double dx = target.x - p.x;
            double dy = target.y - p.y;
            double d = Math.hypot(dx, dy);
            if (d < 0.0001) return;
            dx /= d; dy /= d;

            int shots = Math.max(1, p.multiShot);
            double spread = p.multiShotSpreadRad;
            double baseAngle = Math.atan2(dy, dx);

            double speed = p.bulletSpeed;
            double dmg = p.bulletDamage * (1.0 + (level-1) * 0.06);

            for (int i = 0; i < shots; i++) {
                double angle = (shots == 1) ? baseAngle : baseAngle + (i - (shots - 1) / 2.0) * spread;
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed;

                double px = p.x + Math.cos(angle) * (p.radius + 7);
                double py = p.y + Math.sin(angle) * (p.radius + 7);

                world.spawnPlayerProjectile(px, py, vx, vy, dmg);
            }

            cd = delay;
        }
    }

    static class SwordSlashSkill implements Skill {
        int level = 1;
        double cd = 0;
        @Override public String getName() { return "Sword Slash"; }
        @Override public int getLevel() { return level; }
        @Override public void levelUp() { level = Math.min(10, level+1); }
        @Override
        public void update(double dt, Player p, GamePanel world) {
            cd -= dt;
            double interval = Math.max(0.55, 1.25 - (level-1) * 0.07);
            if (cd > 0) return;

            double radius = 70 + level * 10;
            double dmg = 16 + level * 8;

            List<Enemy> hit = world.findEnemiesInRange(p.x, p.y, radius);
            for (Enemy en : hit) {
                world.damageEnemy(en, dmg, true, true, GamePanel.DamageSource.SWORD);
                world.addEffect(new DamageNumberEffect(en.x, en.y, dmg, false, GamePanel.DamageSource.SWORD));
            }

            world.addEffect(new SwordArcEffect(p.x, p.y, radius, 0.25));
            cd = interval;
        }
    }

    static class ChainLightningSkill implements Skill {
        int level = 1;
        double cd = 0;
        @Override public String getName() { return "Chain Lightning"; }
        @Override public int getLevel() { return level; }
        @Override public void levelUp() { level = Math.min(10, level+1); }
        @Override
        public void update(double dt, Player p, GamePanel world) {
            cd -= dt;
            double interval = Math.max(0.9, 2.2 - (level-1) * 0.12);
            if (cd > 0) return;

            Enemy start = world.findNearestEnemy();
            if (start == null) return;

            int jumps = 2 + level / 2;
            double range = 160 + level * 12;
            double dmg = 18 + level * 7;

            Set<Enemy> used = new HashSet<>();
            Enemy cur = start;

            List<double[]> pts = new ArrayList<>();
            pts.add(new double[]{p.x, p.y});

            for (int i = 0; i < jumps; i++) {
                if (cur == null || !cur.alive) break;
                used.add(cur);

                pts.add(new double[]{cur.x, cur.y});
                // ⚡ synergy inside damageEnemy: slowed => +30%
                world.damageEnemy(cur, dmg, true, false, GamePanel.DamageSource.LIGHTNING);
                world.addEffect(new DamageNumberEffect(cur.x, cur.y, dmg, false, GamePanel.DamageSource.LIGHTNING));

                Enemy next = null;
                double bestD2 = Double.POSITIVE_INFINITY;
                for (Enemy en : world.enemies) {
                    if (!en.alive) continue;
                    if (used.contains(en)) continue;
                    double dx = en.x - cur.x;
                    double dy = en.y - cur.y;
                    double d2 = dx*dx + dy*dy;
                    if (d2 <= range*range && d2 < bestD2) { bestD2 = d2; next = en; }
                }
                cur = next;
            }

            if (pts.size() >= 2) world.addEffect(new LightningEffect(pts, 0.18));
            cd = interval;
        }
    }

    static class IceFieldSkill implements Skill {
        int level = 1;
        double cd = 0;
        double tickAcc = 0;
        double activeTime = 0;

        @Override public String getName() { return "Ice Field"; }
        @Override public int getLevel() { return level; }
        @Override public void levelUp() { level = Math.min(10, level+1); }

        @Override
        public void update(double dt, Player p, GamePanel world) {
            cd -= dt;

            double interval = Math.max(2.0, 6.0 - (level-1) * 0.35);
            double duration = 1.4 + level * 0.15;

            double radiusBase = 120 + level * 10;
            // set synergy frost => +40% radius
            double radius = p.setFrostActive ? radiusBase * 1.40 : radiusBase;

            double slowPct = Math.min(0.65, 0.20 + level * 0.04);

            if (cd <= 0 && activeTime <= 0) {
                activeTime = duration;
                tickAcc = 0;
                cd = interval;
                world.addEffect(new IceFieldEffect(p, radius, duration));
            }

            if (activeTime > 0) {
                activeTime -= dt;
                tickAcc += dt;

                List<Enemy> list = world.findEnemiesInRange(p.x, p.y, radius);
                for (Enemy en : list) en.applySlow(slowPct, 0.25);

                while (tickAcc >= 0.25) {
                    tickAcc -= 0.25;
                    double dmg = 6 + level * 2.2;
                    for (Enemy en : list) {
                        world.damageEnemy(en, dmg, false, false, GamePanel.DamageSource.ICE);
                        world.addEffect(new DamageNumberEffect(en.x, en.y, dmg, false, GamePanel.DamageSource.ICE));
                    }
                }
            }
        }
    }

    // ============================ UPGRADES ============================
    interface Upgrade {
        String getName();
        String getDesc();
        String getTag();
        void apply(Player p);
    }

    static class UpgradeImpl implements Upgrade {
        final String name, desc, tag;
        final Consumer<Player> apply;

        UpgradeImpl(String name, String desc, String tag, Consumer<Player> apply) {
            this.name = name; this.desc = desc; this.tag = tag; this.apply = apply;
        }
        @Override public String getName() { return name; }
        @Override public String getDesc() { return desc; }
        @Override public String getTag() { return tag; }
        @Override public void apply(Player p) { apply.accept(p); }
    }

    static class UpgradeSystem {
        List<Upgrade> roll3(Player p, Random rng) {
            List<Upgrade> pool = buildPool(p);

            List<Upgrade> choices = new ArrayList<>();
            Set<Integer> used = new HashSet<>();
            while (choices.size() < 3) {
                int idx = rng.nextInt(pool.size());
                if (used.add(idx)) choices.add(pool.get(idx));
            }
            return choices;
        }

        private List<Upgrade> buildPool(Player p) {
            List<Upgrade> pool = new ArrayList<>();

            pool.add(new UpgradeImpl("Keskin Darbe", "+%20 mermi hasarı.", "OFFENSE",
                    pl -> pl.bulletDamage *= 1.20));
            pool.add(new UpgradeImpl("Hızlı Tetik", "Ateş aralığı -%12.", "OFFENSE",
                    pl -> pl.fireDelay = Math.max(0.10, pl.fireDelay * 0.88)));
            pool.add(new UpgradeImpl("Çifte Atış", "+1 Multi-shot.", "OFFENSE",
                    pl -> pl.multiShot = Math.min(7, pl.multiShot + 1)));
            pool.add(new UpgradeImpl("Hafif Adım", "+%10 hareket hızı.", "UTILITY",
                    pl -> pl.moveSpeed *= 1.10));
            pool.add(new UpgradeImpl("Dayanıklılık", "+20 Max HP (doldurur).", "DEFENSE",
                    pl -> { pl.maxHp += 20; pl.hp = Math.min(pl.maxHp, pl.hp + 20); }));
            pool.add(new UpgradeImpl("Kanla Beslenme", "Kill başına +2 HP.", "DEFENSE",
                    pl -> pl.healOnKill += 2));
            pool.add(new UpgradeImpl("Buz Mermi", "Mermiler %20 slow (1.2sn).", "CONTROL",
                    pl -> { pl.bulletSlowPct = Math.min(0.55, pl.bulletSlowPct + 0.20); pl.bulletSlowDuration = 1.2; }));
            pool.add(new UpgradeImpl("Kritik Sezgi", "+%6 kritik şansı.", "OFFENSE",
                    pl -> pl.critChance = Math.min(0.60, pl.critChance + 0.06)));
            pool.add(new UpgradeImpl("Ölüm Vuruşu", "Kritik çarpanı +0.5x.", "OFFENSE",
                    pl -> pl.critMultiplier = Math.min(5.0, pl.critMultiplier + 0.5)));
            pool.add(new UpgradeImpl("Mıknatıs", "Toplama +%25, çekim +%15.", "UTILITY",
                    pl -> { pl.pickupRadius *= 1.25; pl.magnetRadius *= 1.15; }));

            if (!p.hasSkill(SwordSlashSkill.class)) {
                pool.add(new UpgradeImpl("Skill Aç: Sword Slash", "Yakın AOE kılıç darbesi.", "SKILL",
                        pl -> pl.skills.add(new SwordSlashSkill())));
            } else {
                pool.add(new UpgradeImpl("Sword Slash +1", "Sword Slash level +1.", "SKILL",
                        pl -> ((SwordSlashSkill)pl.getSkill(SwordSlashSkill.class)).levelUp()));
            }

            if (!p.hasSkill(ChainLightningSkill.class)) {
                pool.add(new UpgradeImpl("Skill Aç: Chain Lightning", "Zincir şimşek (çoklu hedef).", "SKILL",
                        pl -> pl.skills.add(new ChainLightningSkill())));
            } else {
                pool.add(new UpgradeImpl("Chain Lightning +1", "Chain Lightning level +1.", "SKILL",
                        pl -> ((ChainLightningSkill)pl.getSkill(ChainLightningSkill.class)).levelUp()));
            }

            if (!p.hasSkill(IceFieldSkill.class)) {
                pool.add(new UpgradeImpl("Skill Aç: Ice Field", "Alan slow + hasar tick.", "SKILL",
                        pl -> pl.skills.add(new IceFieldSkill())));
            } else {
                pool.add(new UpgradeImpl("Ice Field +1", "Ice Field level +1.", "SKILL",
                        pl -> ((IceFieldSkill)pl.getSkill(IceFieldSkill.class)).levelUp()));
            }

            pool.add(new UpgradeImpl("Basic Shot +1", "Ana otomatik atış güçlenir.", "SKILL",
                    pl -> ((BasicShotSkill)pl.getSkill(BasicShotSkill.class)).levelUp()));

            return pool;
        }
    }
}
