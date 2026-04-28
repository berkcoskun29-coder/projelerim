import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

public class Main extends Application {

    // ================= WORLD SETTINGS =================
    static final int MAP_W = 90;
    static final int MAP_H = 65;
    static final int TILE_SIZE = 28;

    enum Tile { GRASS, WATER, FOREST }

    Tile[][] map = new Tile[MAP_H][MAP_W];
    int[][] woodAmount = new int[MAP_H][MAP_W]; // FOREST tiles store wood

    // ================= VIEW =================
    Canvas canvas;
    GraphicsContext g;

    double camX = 0;
    double camY = 0;
    double zoom = 1.0;
    double mouseSX = 0; // screen X
    double mouseSY = 0; // screen Y

    // ================= INPUT =================
    Set<KeyCode> keys = new HashSet<>();

    // Selection drag box
    boolean dragging = false;
    double dragStartX, dragStartY; // screen
    double dragEndX, dragEndY;     // screen

    // ================= RESOURCES =================
    int wood = 0;

    // ================= ENTITIES =================
    enum Team { PLAYER, ENEMY }
    enum UnitType { VILLAGER, SOLDIER, ENEMY }
    enum UnitState { IDLE, MOVING, GATHERING, ATTACKING }

    class Unit {
        int id;
        Team team;
        UnitType type;

        double x, y; // world px
        int tileX, tileY;

        double hp, hpMax;
        double speed;
        double atk;
        double range;
        double atkCooldown;
        double atkTimer = 0;

        boolean selected = false;

        // Path
        List<Node> path = null;
        int pathIndex = 0;

        // State
        UnitState state = UnitState.IDLE;

        // Targets
        int targetTileX, targetTileY;
        Unit targetUnit = null;
        Building targetBuilding = null;

        // Gathering
        double gatherTimer = 0;
        double gatherInterval = 0.6; // seconds

        Unit(int id, Team team, UnitType type, int startTileX, int startTileY) {
            this.id = id;
            this.team = team;
            this.type = type;

            this.tileX = startTileX;
            this.tileY = startTileY;
            this.x = startTileX * TILE_SIZE + TILE_SIZE / 2.0;
            this.y = startTileY * TILE_SIZE + TILE_SIZE / 2.0;

            if (type == UnitType.VILLAGER) {
                hpMax = 50; hp = hpMax;
                speed = 175;
                atk = 6;
                range = 28;
                atkCooldown = 0.7;
            } else if (type == UnitType.SOLDIER) {
                hpMax = 90; hp = hpMax;
                speed = 195;
                atk = 14;
                range = 32;
                atkCooldown = 0.55;
            } else { // ENEMY
                hpMax = 70; hp = hpMax;
                speed = 170;
                atk = 10;
                range = 28;
                atkCooldown = 0.7;
            }

            targetTileX = tileX;
            targetTileY = tileY;
        }

        boolean isAlive() { return hp > 0; }
    }

    enum BuildingType { TOWNCENTER, HOUSE, BARRACKS, ENEMY_BASE }

    class Building {
        int id;
        Team team;
        BuildingType type;

        int tx, ty; // top-left tile
        int w, h;   // tile size footprint

        double hp, hpMax;

        boolean selected = false;

        // Production
        Deque<UnitType> queue = new ArrayDeque<>();
        double prodTimer = 0;
        double prodTimePerUnit = 3.0;

        Building(int id, Team team, BuildingType type, int tx, int ty) {
            this.id = id;
            this.team = team;
            this.type = type;
            this.tx = tx; this.ty = ty;

            if (type == BuildingType.TOWNCENTER) { w = 3; h = 3; hpMax = 400; }
            else if (type == BuildingType.HOUSE) { w = 2; h = 2; hpMax = 220; }
            else if (type == BuildingType.BARRACKS) { w = 3; h = 3; hpMax = 320; }
            else { w = 3; h = 3; hpMax = 500; }

            hp = hpMax;
        }

        boolean isAlive() { return hp > 0; }
    }

    int nextUnitId = 1;
    int nextBuildingId = 1;

    List<Unit> units = new ArrayList<>();
    List<Building> buildings = new ArrayList<>();

    // ================= BUILD MODE =================
    boolean buildMode = false;
    BuildingType buildType = BuildingType.HOUSE;

    // ================= FOG OF WAR =================
    boolean[][] discovered = new boolean[MAP_H][MAP_W];
    float[][] visible = new float[MAP_H][MAP_W]; // 0..1
    int visionRadiusTiles = 7;

    // ================= ENEMY AI =================
    double enemySpawnTimer = 0;
    double enemySpawnInterval = 6.0;

    // ================= PATHFINDING =================
    class Node {
        int x, y;
        double g, h;
        Node parent;
        Node(int x, int y) { this.x = x; this.y = y; }
        double f() { return g + h; }
    }

    boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < MAP_W && y < MAP_H;
    }

    boolean isWalkable(int x, int y) {
        if (!inBounds(x, y)) return false;
        return map[y][x] != Tile.WATER;
    }

    List<Node> findPath(int sx, int sy, int ex, int ey) {
        if (!inBounds(ex, ey)) return null;
        if (!isWalkable(ex, ey)) return null;

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        boolean[][] closed = new boolean[MAP_H][MAP_W];
        double[][] bestG = new double[MAP_H][MAP_W];
        for (int y=0;y<MAP_H;y++) Arrays.fill(bestG[y], Double.POSITIVE_INFINITY);

        Node start = new Node(sx, sy);
        start.g = 0;
        start.h = Math.abs(ex - sx) + Math.abs(ey - sy);
        open.add(start);
        bestG[sy][sx] = 0;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (closed[cur.y][cur.x]) continue;
            closed[cur.y][cur.x] = true;

            if (cur.x == ex && cur.y == ey) {
                List<Node> path = new ArrayList<>();
                Node n = cur;
                while (n != null) { path.add(n); n = n.parent; }
                Collections.reverse(path);
                return path;
            }

            for (int[] d : dirs) {
                int nx = cur.x + d[0];
                int ny = cur.y + d[1];
                if (!isWalkable(nx, ny) || closed[ny][nx]) continue;

                double ng = cur.g + 1;
                if (ng >= bestG[ny][nx]) continue;
                bestG[ny][nx] = ng;

                Node n = new Node(nx, ny);
                n.g = ng;
                n.h = Math.abs(ex - nx) + Math.abs(ey - ny);
                n.parent = cur;
                open.add(n);
            }
        }

        return null;
    }

    // Buildings block tiles for movement
    boolean isTileBlockedByBuilding(int tx, int ty) {
        for (Building b : buildings) {
            if (!b.isAlive()) continue;
            if (tx >= b.tx && tx < b.tx + b.w && ty >= b.ty && ty < b.ty + b.h) {
                return true;
            }
        }
        return false;
    }

    List<Node> findPathWithBuildings(int sx, int sy, int ex, int ey) {
        if (!inBounds(ex, ey)) return null;
        if (!isWalkable(ex, ey)) return null;
        if (isTileBlockedByBuilding(ex, ey)) return null;

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        boolean[][] closed = new boolean[MAP_H][MAP_W];
        double[][] bestG = new double[MAP_H][MAP_W];
        for (int y=0;y<MAP_H;y++) Arrays.fill(bestG[y], Double.POSITIVE_INFINITY);

        Node start = new Node(sx, sy);
        start.g = 0;
        start.h = Math.abs(ex - sx) + Math.abs(ey - sy);
        open.add(start);
        bestG[sy][sx] = 0;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (closed[cur.y][cur.x]) continue;
            closed[cur.y][cur.x] = true;

            if (cur.x == ex && cur.y == ey) {
                List<Node> path = new ArrayList<>();
                Node n = cur;
                while (n != null) { path.add(n); n = n.parent; }
                Collections.reverse(path);
                return path;
            }

            for (int[] d : dirs) {
                int nx = cur.x + d[0];
                int ny = cur.y + d[1];
                if (!inBounds(nx, ny)) continue;
                if (!isWalkable(nx, ny)) continue;
                if (isTileBlockedByBuilding(nx, ny)) continue;
                if (closed[ny][nx]) continue;

                double ng = cur.g + 1;
                if (ng >= bestG[ny][nx]) continue;
                bestG[ny][nx] = ng;

                Node n = new Node(nx, ny);
                n.g = ng;
                n.h = Math.abs(ex - nx) + Math.abs(ey - ny);
                n.parent = cur;
                open.add(n);
            }
        }
        return null;
    }

    // ================= UTIL =================
    static double clamp(double v, double a, double b) { return Math.max(a, Math.min(b, v)); }
    static int clampInt(int v, int a, int b) { return Math.max(a, Math.min(b, v)); }

    int toTile(double worldPx) { return (int) Math.floor(worldPx / TILE_SIZE); }
    double tileCenterX(int tx) { return tx * TILE_SIZE + TILE_SIZE / 2.0; }
    double tileCenterY(int ty) { return ty * TILE_SIZE + TILE_SIZE / 2.0; }

    // ================= GAME START =================
    @Override
    public void start(Stage stage) {
        generateMap();

        canvas = new Canvas(1200, 760);
        g = canvas.getGraphicsContext2D();

        // Initial base
        Building town = new Building(nextBuildingId++, Team.PLAYER, BuildingType.TOWNCENTER, 6, 6);
        buildings.add(town);
        units.add(new Unit(nextUnitId++, Team.PLAYER, UnitType.VILLAGER, 9, 9));
        units.add(new Unit(nextUnitId++, Team.PLAYER, UnitType.VILLAGER, 10, 9));

        // Enemy base
        Building enemyBase = new Building(nextBuildingId++, Team.ENEMY, BuildingType.ENEMY_BASE, MAP_W - 10, MAP_H - 10);
        buildings.add(enemyBase);

        Scene scene = new Scene(new StackPane(canvas));
        scene.setOnMouseMoved(e -> { mouseSX = e.getX(); mouseSY = e.getY(); });
        scene.setOnMouseDragged(e -> { mouseSX = e.getX(); mouseSY = e.getY(); });

        // Keys
        scene.setOnKeyPressed(e -> keys.add(e.getCode()));
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));

        // Zoom
        scene.addEventFilter(ScrollEvent.SCROLL, e -> {
            double oldZoom = zoom;
            zoom *= (e.getDeltaY() > 0) ? 1.1 : 0.9;
            zoom = clamp(zoom, 0.55, 2.6);

            // zoom towards cursor
            double mx = e.getX();
            double my = e.getY();
            double worldXBefore = (mx / oldZoom) + camX;
            double worldYBefore = (my / oldZoom) + camY;
            double worldXAfter  = (mx / zoom) + camX;
            double worldYAfter  = (my / zoom) + camY;
            camX += (worldXBefore - worldXAfter);
            camY += (worldYBefore - worldYAfter);
        });

        // Mouse
        scene.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragging = true;
                dragStartX = e.getX();
                dragStartY = e.getY();
                dragEndX = dragStartX;
                dragEndY = dragStartY;
            }

            if (e.getButton() == MouseButton.SECONDARY) {
                double wx = (e.getX() / zoom) + camX;
                double wy = (e.getY() / zoom) + camY;
                handleRightClick(wx, wy);
            }
        });

        scene.setOnMouseDragged(e -> {
            if (dragging) {
                dragEndX = e.getX();
                dragEndY = e.getY();
            }
        });

        scene.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (buildMode) {
                    // place building
                    double wx = (e.getX() / zoom) + camX;
                    double wy = (e.getY() / zoom) + camY;
                    placeBuildingAt(wx, wy);
                } else {
                    // selection
                    finalizeSelection();
                }
                dragging = false;
            }
        });

        // Loop
        new AnimationTimer() {
            long last = 0;
            @Override public void handle(long now) {
                if (last == 0) last = now;
                double dt = (now - last) / 1e9;
                last = now;

                handleHotkeys(dt);
                update(dt);
                render();
            }
        }.start();

        stage.setTitle("Mini AoE RTS (JavaFX) - Multi Units + Drag Select + A* + Gather + Build + Barracks + Fog+Minimap");
        stage.setScene(scene);
        stage.show();
        canvas.requestFocus();
    }

    // ================= HOTKEYS =================
    void handleHotkeys(double dt) {
        // Camera
        double camSpeed = 650 * dt / zoom;
        if (keys.contains(KeyCode.W)) camY -= camSpeed;
        if (keys.contains(KeyCode.S)) camY += camSpeed;
        if (keys.contains(KeyCode.A)) camX -= camSpeed;
        if (keys.contains(KeyCode.D)) camX += camSpeed;

        // clamp camera a bit
        double worldW = MAP_W * TILE_SIZE;
        double worldH = MAP_H * TILE_SIZE;
        double viewW = canvas.getWidth() / zoom;
        double viewH = canvas.getHeight() / zoom;
        camX = clamp(camX, -200, worldW - viewW + 200);
        camY = clamp(camY, -200, worldH - viewH + 200);

        // Spawn villager for testing
        if (consumeKey(KeyCode.V)) {
            buildings.stream().filter(b -> b.team == Team.PLAYER && b.type == BuildingType.TOWNCENTER).findFirst().ifPresent(tc -> {
                int sx = tc.tx + tc.w + 1;
                int sy = tc.ty + tc.h - 1;
                sx = clampInt(sx, 0, MAP_W-1);
                sy = clampInt(sy, 0, MAP_H-1);
                if (isWalkable(sx, sy) && !isTileBlockedByBuilding(sx, sy)) {
                    units.add(new Unit(nextUnitId++, Team.PLAYER, UnitType.VILLAGER, sx, sy));
                }
            });
        }

        // Build mode toggle
        if (consumeKey(KeyCode.B)) buildMode = !buildMode;

        if (buildMode) {
            if (consumeKey(KeyCode.H)) buildType = BuildingType.HOUSE;
            if (consumeKey(KeyCode.K)) buildType = BuildingType.BARRACKS;
        }

        if (consumeKey(KeyCode.ESCAPE)) buildMode = false;

        // Produce soldier at selected barracks
        if (consumeKey(KeyCode.P)) {
            Building b = getSelectedPlayerBuilding();
            if (b != null && b.type == BuildingType.BARRACKS) {
                if (wood >= 30) {
                    wood -= 30;
                    b.queue.add(UnitType.SOLDIER);
                }
            }
        }
    }

    boolean consumeKey(KeyCode code) {
        if (keys.contains(code)) {
            keys.remove(code);
            return true;
        }
        return false;
    }

    // ================= INPUT ACTIONS =================
    void clearSelections() {
        for (Unit u : units) u.selected = false;
        for (Building b : buildings) b.selected = false;
    }

    List<Unit> getSelectedPlayerUnits() {
        return units.stream().filter(u -> u.team == Team.PLAYER && u.selected && u.isAlive()).collect(Collectors.toList());
    }

    Building getSelectedPlayerBuilding() {
        return buildings.stream().filter(b -> b.team == Team.PLAYER && b.selected && b.isAlive()).findFirst().orElse(null);
    }

    void handleRightClick(double wx, double wy) {
        if (buildMode) return; // ignore commands while building

        int tx = toTile(wx);
        int ty = toTile(wy);
        if (!inBounds(tx, ty)) return;

        // Hit test: enemy unit/building?
        Unit enemyU = findUnitAtWorld(wx, wy, Team.ENEMY);
        Building enemyB = findBuildingAtTile(tx, ty, Team.ENEMY);

        // Resource?
        boolean isForest = (map[ty][tx] == Tile.FOREST && woodAmount[ty][tx] > 0);

        List<Unit> selectedUnits = getSelectedPlayerUnits();
        if (selectedUnits.isEmpty()) return;

        for (Unit u : selectedUnits) {
            u.targetUnit = null;
            u.targetBuilding = null;

            if (enemyU != null) {
                issueAttackUnit(u, enemyU);
            } else if (enemyB != null) {
                issueAttackBuilding(u, enemyB);
            } else if (isForest && u.type == UnitType.VILLAGER) {
                issueGather(u, tx, ty);
            } else {
                issueMove(u, tx, ty);
            }
        }
    }

    Unit findUnitAtWorld(double wx, double wy, Team team) {
        for (Unit u : units) {
            if (!u.isAlive() || u.team != team) continue;
            double dx = wx - u.x;
            double dy = wy - u.y;
            if (dx*dx + dy*dy <= 14*14) return u;
        }
        return null;
    }

    Building findBuildingAtTile(int tx, int ty, Team team) {
        for (Building b : buildings) {
            if (!b.isAlive() || b.team != team) continue;
            if (tx >= b.tx && tx < b.tx + b.w && ty >= b.ty && ty < b.ty + b.h) return b;
        }
        return null;
    }

    void issueMove(Unit u, int tx, int ty) {
        // find nearest reachable tile if blocked
        if (!isWalkable(tx, ty) || isTileBlockedByBuilding(tx, ty)) {
            // simple: try neighbors
            int[][] dirs = {{0,0},{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}};
            boolean found = false;
            for (int[] d : dirs) {
                int nx = tx + d[0], ny = ty + d[1];
                if (inBounds(nx, ny) && isWalkable(nx, ny) && !isTileBlockedByBuilding(nx, ny)) {
                    tx = nx; ty = ny; found = true; break;
                }
            }
            if (!found) return;
        }

        u.state = UnitState.MOVING;
        u.targetTileX = tx;
        u.targetTileY = ty;
        u.path = findPathWithBuildings(u.tileX, u.tileY, tx, ty);
        u.pathIndex = 0;
    }

    void issueGather(Unit u, int tx, int ty) {
        u.state = UnitState.GATHERING;
        u.targetTileX = tx;
        u.targetTileY = ty;
        u.path = findPathWithBuildings(u.tileX, u.tileY, tx, ty);
        u.pathIndex = 0;
        u.gatherTimer = 0;
    }

    void issueAttackUnit(Unit u, Unit enemy) {
        u.state = UnitState.ATTACKING;
        u.targetUnit = enemy;
        u.targetBuilding = null;
        // path will be recomputed as enemy moves
    }

    void issueAttackBuilding(Unit u, Building b) {
        u.state = UnitState.ATTACKING;
        u.targetBuilding = b;
        u.targetUnit = null;
    }

    void placeBuildingAt(double wx, double wy) {
        int tx = toTile(wx);
        int ty = toTile(wy);
        tx = clampInt(tx, 0, MAP_W-1);
        ty = clampInt(ty, 0, MAP_H-1);

        // cost
        int cost = (buildType == BuildingType.HOUSE) ? 25 : 60;
        if (wood < cost) return;

        // footprint check
        Building tmp = new Building(-1, Team.PLAYER, buildType, tx, ty);
        if (!canPlaceBuilding(tmp)) return;

        wood -= cost;
        buildings.add(new Building(nextBuildingId++, Team.PLAYER, buildType, tx, ty));
    }

    boolean canPlaceBuilding(Building b) {
        // inside bounds
        if (b.tx < 0 || b.ty < 0 || b.tx + b.w > MAP_W || b.ty + b.h > MAP_H) return false;

        // tiles walkable and not water/forest; and no overlap with other buildings
        for (int y = b.ty; y < b.ty + b.h; y++) {
            for (int x = b.tx; x < b.tx + b.w; x++) {
                if (!isWalkable(x, y)) return false;
                if (map[y][x] == Tile.FOREST) return false; // keep simple
                if (isTileBlockedByBuilding(x, y)) return false;
            }
        }
        return true;
    }

    // ================= SELECTION =================
    void finalizeSelection() {
        double dx = Math.abs(dragEndX - dragStartX);
        double dy = Math.abs(dragEndY - dragStartY);

        double x0 = Math.min(dragStartX, dragEndX);
        double y0 = Math.min(dragStartY, dragEndY);
        double x1 = Math.max(dragStartX, dragEndX);
        double y1 = Math.max(dragStartY, dragEndY);

        double wx0 = (x0 / zoom) + camX;
        double wy0 = (y0 / zoom) + camY;
        double wx1 = (x1 / zoom) + camX;
        double wy1 = (y1 / zoom) + camY;

        clearSelections();

        // Click select (small drag)
        if (dx < 6 && dy < 6) {
            double wx = (dragEndX / zoom) + camX;
            double wy = (dragEndY / zoom) + camY;

            // unit first
            Unit hitU = null;
            for (Unit u : units) {
                if (!u.isAlive() || u.team != Team.PLAYER) continue;
                double ddx = wx - u.x, ddy = wy - u.y;
                if (ddx*ddx + ddy*ddy <= 16*16) { hitU = u; break; }
            }
            if (hitU != null) {
                hitU.selected = true;
                return;
            }

            // building select
            int tx = toTile(wx), ty = toTile(wy);
            for (Building b : buildings) {
                if (!b.isAlive() || b.team != Team.PLAYER) continue;
                if (tx >= b.tx && tx < b.tx + b.w && ty >= b.ty && ty < b.ty + b.h) {
                    b.selected = true;
                    return;
                }
            }
            return;
        }

        // Box select units
        for (Unit u : units) {
            if (!u.isAlive() || u.team != Team.PLAYER) continue;
            if (u.x >= wx0 && u.x <= wx1 && u.y >= wy0 && u.y <= wy1) {
                u.selected = true;
            }
        }
    }

    // ================= UPDATE LOOP =================
    void update(double dt) {
        // Remove dead
        units.removeIf(u -> !u.isAlive());
        buildings.removeIf(b -> !b.isAlive());

        // Enemy AI spawn
        enemySpawnTimer += dt;
        if (enemySpawnTimer >= enemySpawnInterval) {
            enemySpawnTimer = 0;
            Building eb = buildings.stream().filter(b -> b.team == Team.ENEMY && b.type == BuildingType.ENEMY_BASE).findFirst().orElse(null);
            if (eb != null) {
                int sx = eb.tx - 1;
                int sy = eb.ty + eb.h - 1;
                sx = clampInt(sx, 0, MAP_W-1);
                sy = clampInt(sy, 0, MAP_H-1);
                if (isWalkable(sx, sy) && !isTileBlockedByBuilding(sx, sy)) {
                    units.add(new Unit(nextUnitId++, Team.ENEMY, UnitType.ENEMY, sx, sy));
                }
            }
        }

        // Update buildings production
        for (Building b : buildings) {
            if (!b.isAlive()) continue;
            if (b.team == Team.PLAYER && b.type == BuildingType.BARRACKS && !b.queue.isEmpty()) {
                b.prodTimer += dt;
                if (b.prodTimer >= b.prodTimePerUnit) {
                    b.prodTimer = 0;
                    UnitType t = b.queue.pollFirst();
                    // spawn near barracks
                    int sx = b.tx + b.w;
                    int sy = b.ty + b.h - 1;
                    sx = clampInt(sx, 0, MAP_W-1);
                    sy = clampInt(sy, 0, MAP_H-1);
                    if (isWalkable(sx, sy) && !isTileBlockedByBuilding(sx, sy)) {
                        units.add(new Unit(nextUnitId++, Team.PLAYER, (t == UnitType.SOLDIER ? UnitType.SOLDIER : UnitType.VILLAGER), sx, sy));
                    }
                }
            }
        }

        // Update units
        for (Unit u : units) {
            if (!u.isAlive()) continue;

            // forest slow
            double tileSpeedFactor = 1.0;
            if (inBounds(u.tileX, u.tileY) && map[u.tileY][u.tileX] == Tile.FOREST) tileSpeedFactor = 0.75;

            // Enemy behavior: move to nearest player unit/building
            if (u.team == Team.ENEMY) {
                if (u.state != UnitState.ATTACKING) {
                    Unit pu = nearestUnit(u.x, u.y, Team.PLAYER);
                    Building pb = nearestBuilding(u.x, u.y, Team.PLAYER);
                    if (pu != null) issueAttackUnit(u, pu);
                    else if (pb != null) issueAttackBuilding(u, pb);
                    else u.state = UnitState.IDLE;
                }
            }

            // Attack state
            if (u.state == UnitState.ATTACKING) {
                u.atkTimer += dt;

                // pick current target position
                double tx, ty;
                boolean hasTarget = false;

                if (u.targetUnit != null && u.targetUnit.isAlive()) {
                    tx = u.targetUnit.x; ty = u.targetUnit.y; hasTarget = true;

                    double dist = Math.hypot(tx - u.x, ty - u.y);
                    if (dist <= u.range) {
                        if (u.atkTimer >= u.atkCooldown) {
                            u.atkTimer = 0;
                            u.targetUnit.hp -= u.atk;
                        }
                        u.path = null;
                    } else {
                        // chase: recompute occasionally
                        if (u.path == null || u.pathIndex >= (u.path == null ? 0 : u.path.size()) || (u.atkTimer > 0.4)) {
                            u.atkTimer = 0; // reuse timer as a cheap throttle
                            int ex = u.targetUnit.tileX;
                            int ey = u.targetUnit.tileY;
                            u.path = findPathWithBuildings(u.tileX, u.tileY, ex, ey);
                            u.pathIndex = 0;
                        }
                        stepAlongPath(u, dt, tileSpeedFactor);
                    }
                } else if (u.targetBuilding != null && u.targetBuilding.isAlive()) {
                    // nearest tile near building footprint
                    Building b = u.targetBuilding;
                    int ex = clampInt(b.tx - 1, 0, MAP_W-1);
                    int ey = clampInt(b.ty, 0, MAP_H-1);
                    tx = tileCenterX(ex);
                    ty = tileCenterY(ey);
                    hasTarget = true;

                    double dist = Math.hypot(tx - u.x, ty - u.y);
                    if (dist <= u.range + 8) {
                        if (u.atkTimer >= u.atkCooldown) {
                            u.atkTimer = 0;
                            b.hp -= u.atk;
                        }
                        u.path = null;
                    } else {
                        if (u.path == null || u.pathIndex >= u.path.size()) {
                            u.path = findPathWithBuildings(u.tileX, u.tileY, ex, ey);
                            u.pathIndex = 0;
                        }
                        stepAlongPath(u, dt, tileSpeedFactor);
                    }
                } else {
                    u.state = UnitState.IDLE;
                    u.targetUnit = null;
                    u.targetBuilding = null;
                }

                if (!hasTarget) u.state = UnitState.IDLE;
                continue;
            }

            // Moving or Gathering uses path
            if (u.state == UnitState.MOVING || u.state == UnitState.GATHERING) {
                stepAlongPath(u, dt, tileSpeedFactor);

                // if reached end
                if (u.path != null && u.pathIndex >= u.path.size()) {
                    u.path = null;
                    u.state = (u.state == UnitState.GATHERING) ? UnitState.GATHERING : UnitState.IDLE;
                }

                // Gathering tick if on target tile
                if (u.state == UnitState.GATHERING) {
                    if (u.tileX == u.targetTileX && u.tileY == u.targetTileY) {
                        if (map[u.tileY][u.tileX] == Tile.FOREST && woodAmount[u.tileY][u.tileX] > 0) {
                            u.gatherTimer += dt;
                            if (u.gatherTimer >= u.gatherInterval) {
                                u.gatherTimer = 0;
                                woodAmount[u.tileY][u.tileX] -= 1;
                                wood += 1;
                                if (woodAmount[u.tileY][u.tileX] <= 0) {
                                    // forest depleted -> becomes grass
                                    map[u.tileY][u.tileX] = Tile.GRASS;
                                    u.state = UnitState.IDLE;
                                }
                            }
                        } else {
                            u.state = UnitState.IDLE;
                        }
                    }
                }
            }
        }

        // Fog update
        updateFog(dt);
    }

    void stepAlongPath(Unit u, double dt, double speedFactor) {
        if (u.path == null || u.pathIndex >= u.path.size()) return;

        Node n = u.path.get(u.pathIndex);
        double tx = tileCenterX(n.x);
        double ty = tileCenterY(n.y);

        double dx = tx - u.x;
        double dy = ty - u.y;
        double dist = Math.sqrt(dx*dx + dy*dy);

        if (dist < 2) {
            u.tileX = n.x;
            u.tileY = n.y;
            u.pathIndex++;

            // update exact px to center for stability
            u.x = tileCenterX(u.tileX);
            u.y = tileCenterY(u.tileY);
        } else {
            double step = u.speed * speedFactor * dt;
            u.x += (dx / dist) * step;
            u.y += (dy / dist) * step;
            u.tileX = clampInt(toTile(u.x), 0, MAP_W-1);
            u.tileY = clampInt(toTile(u.y), 0, MAP_H-1);
        }
    }

    Unit nearestUnit(double x, double y, Team team) {
        Unit best = null;
        double bestD = Double.POSITIVE_INFINITY;
        for (Unit u : units) {
            if (!u.isAlive() || u.team != team) continue;
            double d = Math.hypot(u.x - x, u.y - y);
            if (d < bestD) { bestD = d; best = u; }
        }
        return best;
    }

    Building nearestBuilding(double x, double y, Team team) {
        Building best = null;
        double bestD = Double.POSITIVE_INFINITY;
        for (Building b : buildings) {
            if (!b.isAlive() || b.team != team) continue;
            double cx = (b.tx + b.w/2.0) * TILE_SIZE;
            double cy = (b.ty + b.h/2.0) * TILE_SIZE;
            double d = Math.hypot(cx - x, cy - y);
            if (d < bestD) { bestD = d; best = b; }
        }
        return best;
    }

    // ================= FOG =================
    void updateFog(double dt) {
        // fade old visibility
        for (int y=0;y<MAP_H;y++) {
            for (int x=0;x<MAP_W;x++) {
                visible[y][x] *= 0.86f;
                if (visible[y][x] < 0.02f) visible[y][x] = 0f;
            }
        }

        // mark visible from player units + towncenter
        List<Unit> playerUnits = units.stream().filter(u -> u.team == Team.PLAYER && u.isAlive()).toList();
        List<Building> playerBuildings = buildings.stream().filter(b -> b.team == Team.PLAYER && b.isAlive()).toList();

        for (Unit u : playerUnits) markVision(u.tileX, u.tileY, visionRadiusTiles);
        for (Building b : playerBuildings) markVision(b.tx + b.w/2, b.ty + b.h/2, visionRadiusTiles);

        // discovered
        for (int y=0;y<MAP_H;y++) {
            for (int x=0;x<MAP_W;x++) {
                if (visible[y][x] > 0) discovered[y][x] = true;
            }
        }
    }

    void markVision(int cx, int cy, int r) {
        int r2 = r*r;
        for (int y = cy - r; y <= cy + r; y++) {
            for (int x = cx - r; x <= cx + r; x++) {
                if (!inBounds(x, y)) continue;
                int dx = x - cx, dy = y - cy;
                if (dx*dx + dy*dy <= r2) visible[y][x] = 1.0f;
            }
        }
    }

    // ================= RENDER =================
    void render() {
        // background
        g.setFill(Color.rgb(14, 14, 18));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // world transform
        g.save();
        g.scale(zoom, zoom);
        g.translate(-camX, -camY);

        // draw tiles (only visible screen range for perf)
        double viewW = canvas.getWidth() / zoom;
        double viewH = canvas.getHeight() / zoom;

        int x0 = clampInt((int)Math.floor(camX / TILE_SIZE) - 2, 0, MAP_W-1);
        int y0 = clampInt((int)Math.floor(camY / TILE_SIZE) - 2, 0, MAP_H-1);
        int x1 = clampInt((int)Math.ceil((camX + viewW) / TILE_SIZE) + 2, 0, MAP_W-1);
        int y1 = clampInt((int)Math.ceil((camY + viewH) / TILE_SIZE) + 2, 0, MAP_H-1);

        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                Color c = switch (map[y][x]) {
                    case GRASS -> Color.rgb(70, 155, 85);
                    case WATER -> Color.rgb(55, 120, 205);
                    case FOREST -> Color.rgb(38, 105, 62);
                };
                g.setFill(c);
                g.fillRect(x*TILE_SIZE, y*TILE_SIZE, TILE_SIZE, TILE_SIZE);

                // subtle grid
                g.setStroke(Color.rgb(0,0,0,0.12));
                g.strokeRect(x*TILE_SIZE, y*TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // draw buildings
        for (Building b : buildings) {
            if (!b.isAlive()) continue;

            double px = b.tx * TILE_SIZE;
            double py = b.ty * TILE_SIZE;
            double pw = b.w * TILE_SIZE;
            double ph = b.h * TILE_SIZE;

            Color c;
            if (b.team == Team.PLAYER) {
                c = switch (b.type) {
                    case TOWNCENTER -> Color.rgb(190, 160, 120);
                    case HOUSE -> Color.rgb(170, 130, 95);
                    case BARRACKS -> Color.rgb(150, 120, 90);
                    default -> Color.GRAY;
                };
            } else {
                c = Color.rgb(140, 60, 60);
            }

            g.setFill(c);
            g.fillRoundRect(px, py, pw, ph, 12, 12);

            // selection outline
            if (b.selected) {
                g.setStroke(Color.YELLOW);
                g.setLineWidth(2);
                g.strokeRoundRect(px-2, py-2, pw+4, ph+4, 14, 14);
            }

            // hp bar
            drawHPBar(px, py - 8, pw, 6, b.hp, b.hpMax, b.team == Team.ENEMY);

            // barracks queue indicator
            if (b.team == Team.PLAYER && b.type == BuildingType.BARRACKS && (!b.queue.isEmpty() || b.prodTimer > 0)) {
                g.setFill(Color.WHITE);
                g.fillText("Q:" + b.queue.size(), px + 6, py + 16);
            }
        }

        // draw units
        for (Unit u : units) {
            if (!u.isAlive()) continue;

            // fog: hide enemy if not visible
            if (u.team == Team.ENEMY) {
                if (!inBounds(u.tileX, u.tileY) || visible[u.tileY][u.tileX] <= 0) continue;
            }

            Color c;
            if (u.team == Team.PLAYER) {
                c = (u.type == UnitType.VILLAGER) ? Color.BEIGE : Color.rgb(220, 220, 235);
            } else {
                c = Color.rgb(220, 80, 80);
            }

            double r = (u.type == UnitType.VILLAGER) ? 10 : 11;
            g.setFill(c);
            g.fillOval(u.x - r, u.y - r, r*2, r*2);

            if (u.selected) {
                g.setStroke(Color.YELLOW);
                g.setLineWidth(2);
                g.strokeOval(u.x - r - 4, u.y - r - 4, r*2 + 8, r*2 + 8);
            }

            // hp bar
            drawHPBar(u.x - 14, u.y - r - 10, 28, 5, u.hp, u.hpMax, u.team == Team.ENEMY);
        }

        // build ghost
        if (buildMode) {
        	double mx = mouseSX;
        	double my = mouseSY;

            double wx = (mx / zoom) + camX;
            double wy = (my / zoom) + camY;

            int tx = clampInt(toTile(wx), 0, MAP_W-1);
            int ty = clampInt(toTile(wy), 0, MAP_H-1);

            Building ghost = new Building(-1, Team.PLAYER, buildType, tx, ty);
            boolean ok = canPlaceBuilding(ghost);

            double px = tx * TILE_SIZE;
            double py = ty * TILE_SIZE;
            double pw = ghost.w * TILE_SIZE;
            double ph = ghost.h * TILE_SIZE;

            g.setFill(ok ? Color.rgb(80, 220, 120, 0.25) : Color.rgb(255, 70, 70, 0.25));
            g.fillRoundRect(px, py, pw, ph, 12, 12);
            g.setStroke(ok ? Color.rgb(80, 220, 120, 0.65) : Color.rgb(255, 70, 70, 0.65));
            g.setLineWidth(2);
            g.strokeRoundRect(px, py, pw, ph, 12, 12);
        }

        // fog overlay (world)
        renderFogOverlay(x0, y0, x1, y1);

        g.restore();

        // selection box (screen overlay)
        if (dragging && !buildMode) {
            double x = Math.min(dragStartX, dragEndX);
            double y = Math.min(dragStartY, dragEndY);
            double w = Math.abs(dragEndX - dragStartX);
            double h = Math.abs(dragEndY - dragStartY);

            g.setFill(Color.rgb(255, 255, 255, 0.08));
            g.fillRect(x, y, w, h);
            g.setStroke(Color.rgb(255, 255, 255, 0.35));
            g.strokeRect(x, y, w, h);
        }

        // UI overlay
        drawUI();

        // minimap
        drawMinimap();
    }

    void drawHPBar(double x, double y, double w, double h, double hp, double hpMax, boolean enemy) {
        double p = clamp(hp / hpMax, 0, 1);
        g.setFill(Color.rgb(0,0,0,0.45));
        g.fillRect(x, y, w, h);
        g.setFill(enemy ? Color.rgb(230,80,80,0.9) : Color.rgb(80,230,120,0.9));
        g.fillRect(x, y, w * p, h);
        g.setStroke(Color.rgb(255,255,255,0.25));
        g.strokeRect(x, y, w, h);
    }

    void renderFogOverlay(int x0, int y0, int x1, int y1) {
        // draw a semi-transparent black rect per tile (simple)
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float vis = visible[y][x];
                boolean disc = discovered[y][x];

                if (!disc) {
                    g.setFill(Color.rgb(0,0,0,0.92));
                    g.fillRect(x*TILE_SIZE, y*TILE_SIZE, TILE_SIZE, TILE_SIZE);
                } else if (vis <= 0) {
                    g.setFill(Color.rgb(0,0,0,0.55));
                    g.fillRect(x*TILE_SIZE, y*TILE_SIZE, TILE_SIZE, TILE_SIZE);
                } else {
                    // slight dim
                    g.setFill(Color.rgb(0,0,0,0.12));
                    g.fillRect(x*TILE_SIZE, y*TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    void drawUI() {
        g.setFill(Color.rgb(0,0,0,0.55));
        g.fillRoundRect(10, 10, 420, 84, 16, 16);

        g.setFill(Color.WHITE);
        g.fillText("Wood: " + wood, 24, 34);
        g.fillText("Controls: Drag-select | RightClick: move/attack/gather | V: spawn villager", 24, 54);
        g.fillText("B: build mode  H: House(25)  K: Barracks(60)  Place: LeftClick  |  P: train soldier(30)", 24, 74);

        if (buildMode) {
            g.setFill(Color.rgb(255,255,255,0.85));
            g.fillText("BUILD MODE: " + buildType + " (ESC to cancel)", 24, 92);
        }
    }

    void drawMinimap() {
        int mw = 220, mh = 160;
        int x = (int)canvas.getWidth() - mw - 14;
        int y = 14;

        g.setFill(Color.rgb(0,0,0,0.55));
        g.fillRoundRect(x, y, mw, mh, 14, 14);

        // map to minimap
        double sx = (mw - 16) / (double)MAP_W;
        double sy = (mh - 16) / (double)MAP_H;

        int ox = x + 8;
        int oy = y + 8;

        // tiles (very cheap: sample every 2 tiles)
        for (int ty=0; ty<MAP_H; ty+=2) {
            for (int tx=0; tx<MAP_W; tx+=2) {
                boolean disc = discovered[ty][tx];
                float vis = visible[ty][tx];

                if (!disc) {
                    g.setFill(Color.rgb(0,0,0,0.9));
                } else {
                    Color c = switch (map[ty][tx]) {
                        case GRASS -> Color.rgb(70,155,85);
                        case WATER -> Color.rgb(55,120,205);
                        case FOREST -> Color.rgb(38,105,62);
                    };
                    if (vis <= 0) {
                        // darken
                        g.setFill(Color.rgb((int)(c.getRed()*255*0.45), (int)(c.getGreen()*255*0.45), (int)(c.getBlue()*255*0.45), 0.95));
                    } else {
                        g.setFill(Color.rgb((int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), 0.95));
                    }
                }

                double px = ox + tx * sx;
                double py = oy + ty * sy;
                g.fillRect(px, py, sx*2, sy*2);
            }
        }

        // units dots
        for (Unit u : units) {
            if (!u.isAlive()) continue;
            if (u.team == Team.ENEMY && (u.tileY<0||u.tileY>=MAP_H||u.tileX<0||u.tileX>=MAP_W || visible[u.tileY][u.tileX] <= 0)) continue;

            g.setFill(u.team == Team.PLAYER ? Color.WHITE : Color.rgb(230,80,80));
            double px = ox + u.tileX * sx;
            double py = oy + u.tileY * sy;
            g.fillOval(px-1.7, py-1.7, 3.4, 3.4);
        }

        // camera rect
        double viewW = canvas.getWidth() / zoom;
        double viewH = canvas.getHeight() / zoom;
        double tx0 = camX / TILE_SIZE;
        double ty0 = camY / TILE_SIZE;
        double tx1 = (camX + viewW) / TILE_SIZE;
        double ty1 = (camY + viewH) / TILE_SIZE;

        g.setStroke(Color.rgb(255,255,255,0.6));
        g.strokeRect(ox + tx0*sx, oy + ty0*sy, (tx1-tx0)*sx, (ty1-ty0)*sy);
    }

    // ================= MAP GEN =================
    void generateMap() {
        Random r = new Random(42);

        // base noise
        for (int y=0; y<MAP_H; y++) {
            for (int x=0; x<MAP_W; x++) {
                double n = r.nextDouble();
                if (n < 0.12) map[y][x] = Tile.WATER;
                else if (n < 0.26) map[y][x] = Tile.FOREST;
                else map[y][x] = Tile.GRASS;

                if (map[y][x] == Tile.FOREST) woodAmount[y][x] = 60 + r.nextInt(60);
            }
        }

        // carve a safer starting area
        for (int y=0; y<18; y++) {
            for (int x=0; x<18; x++) {
                map[y][x] = Tile.GRASS;
                woodAmount[y][x] = 0;
            }
        }

        // ensure enemy corner grass
        for (int y=MAP_H-18; y<MAP_H; y++) {
            for (int x=MAP_W-18; x<MAP_W; x++) {
                map[y][x] = Tile.GRASS;
                woodAmount[y][x] = 0;
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
