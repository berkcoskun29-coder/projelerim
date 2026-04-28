package pokemonconsole;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class BattleSystem {
    private Random random = new Random();
    private Scanner scanner = new Scanner(System.in);

    public void startBattle(Player player, Pokemon enemy) {
        Pokemon ally = player.getFirstHealthyPokemon();
        if (ally == null) {
            System.out.println("Hiç sağlıklı Pokémonun yok! Savaşamazsın.");
            return;
        }

        System.out.println("\n⚔ Vahşi " + enemy.getName() + " (Lv." + enemy.getLevel() + ") ortaya çıktı!");
        System.out.println("Sen: " + ally.toString());

        while (!ally.isFainted() && !enemy.isFainted()) {
            System.out.println("\n--- TUR BAŞI ---");
            System.out.println("Senin Pokémonun: " + ally.getName() + " HP: " +
                               ally.getCurrentHp() + "/" + ally.getMaxHp());
            System.out.println("Vahşi Pokémon: " + enemy.getName() + " HP: " +
                               enemy.getCurrentHp() + "/" + enemy.getMaxHp());
            System.out.println("Pokéball: " + player.getPokeballs());

            System.out.println("\nNe yapmak istiyorsun?");
            System.out.println("1) Saldır");
            System.out.println("2) Pokéball kullan (yakala)");
            System.out.println("3) Kaç");

            int choice = readIntSafe();

            if (choice == 1) {
                playerAttack(ally, enemy);
                if (!enemy.isFainted()) {
                    enemyAttack(enemy, ally);
                }
            } else if (choice == 2) {
                boolean caught = tryCatch(player, enemy);
                if (caught) {
                    // Pokémon yakalandı, savaş biter
                    return;
                } else {
                    // Yakalanamazsa, düşman hala bayılmadıysa saldırı yapabilir
                    if (!enemy.isFainted()) {
                        enemyAttack(enemy, ally);
                    }
                }
            } else if (choice == 3) {
                System.out.println("Kaçtın! Savaş bitti.");
                return;
            } else {
                System.out.println("Geçersiz seçim, tur boşa gitti!");
            }

            if (ally.isFainted()) {
                System.out.println(ally.getName() + " bayıldı!");
            } else if (enemy.isFainted()) {
                System.out.println("🎉 Vahşi " + enemy.getName() + " yenildi!");
                int gainedExp = enemy.getLevel() * 30;
                ally.gainExp(gainedExp);
            }
        }
    }

    private void playerAttack(Pokemon attacker, Pokemon defender) {
        List<Move> moves = attacker.getMoves();
        if (moves.isEmpty()) {
            System.out.println(attacker.getName() + " hiç hareket bilmiyor!");
            return;
        }

        System.out.println("\nHareket seç:");
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            System.out.println((i + 1) + ") " + m.getName() +
                               " (Güç: " + m.getPower() +
                               ", İsabet: " + m.getAccuracy() + ")");
        }

        int choice = readIntSafe() - 1;

        if (choice < 0 || choice >= moves.size()) {
            System.out.println("Geçersiz seçim, saldırı boşa gitti!");
            return;
        }

        Move move = moves.get(choice);
        if (!didHit(move)) {
            System.out.println(attacker.getName() + " ıska geçti!");
            return;
        }

        int damage = calculateDamage(attacker, defender, move);
        defender.takeDamage(damage);
        System.out.println(attacker.getName() + " " + move.getName() +
                           " kullandı! " + damage + " hasar verdi.");
    }

    private void enemyAttack(Pokemon attacker, Pokemon defender) {
        List<Move> moves = attacker.getMoves();
        if (moves.isEmpty()) return;

        Move move = moves.get(random.nextInt(moves.size()));
        System.out.println("\nVahşi " + attacker.getName() +
                           " " + move.getName() + " kullanıyor!");

        if (!didHit(move)) {
            System.out.println("Vahşi " + attacker.getName() + " ıska geçti!");
            return;
        }

        int damage = calculateDamage(attacker, defender, move);
        defender.takeDamage(damage);
        System.out.println("Vahşi " + attacker.getName() + " " + damage + " hasar verdi.");
    }

    private boolean didHit(Move move) {
        int roll = random.nextInt(100); // 0–99
        return roll < move.getAccuracy();
    }

    private int calculateDamage(Pokemon attacker, Pokemon defender, Move move) {
        int base = move.getPower() + attacker.getAttack() - defender.getDefense() / 2;
        if (base < 1) base = 1;

        int variation = random.nextInt(5); // 0–4

        double multiplier = getTypeMultiplier(attacker.getType(), defender.getType());
        int damage = (int) ((base + variation) * multiplier);

        if (multiplier > 1.01) {
            System.out.println("⚡ Saldırı çok etkili oldu!");
        } else if (multiplier < 0.99) {
            System.out.println("...Pek etkili olmadı.");
        }

        if (damage < 1) damage = 1;
        return damage;
    }
    private double getTypeMultiplier(PokemonType atkType, PokemonType defType) {
        if (atkType == null || defType == null) {
            return 1.0;
        }

        if (atkType == defType) {
            return 1.0;
        }

        switch (atkType) {
            case FIRE:
                if (defType == PokemonType.GRASS) return 1.5;  // Fire > Grass
                if (defType == PokemonType.WATER) return 0.5;  // Fire < Water
                break;
            case WATER:
                if (defType == PokemonType.FIRE) return 1.5;   // Water > Fire
                if (defType == PokemonType.GRASS) return 0.5;  // Water < Grass
                break;
            case GRASS:
                if (defType == PokemonType.WATER) return 1.5;  // Grass > Water
                if (defType == PokemonType.FIRE) return 0.5;   // Grass < Fire
                break;
        }

        return 1.0; // nötr
    }


    /**
     * Yakalama mantığı:
     * - Önce Pokéball var mı kontrol
     * - HP yüzdesi ne kadar düşükse, yakalama şansı o kadar yüksek
     */
    private boolean tryCatch(Player player, Pokemon enemy) {
        if (!player.usePokeball()) {
            // Pokéball yoksa false
            return false;
        }

        int hpPercent = (enemy.getCurrentHp() * 100) / enemy.getMaxHp(); // 0–100
        int baseChance = 60 - hpPercent; // HP düşükse bu artar
        if (baseChance < 10) baseChance = 10; // minimum %10
        if (baseChance > 80) baseChance = 80; // maksimum %80

        int roll = random.nextInt(100); // 0–99

        System.out.println("Top sallanıyor... (Şans: %" + baseChance + ")");

        if (roll < baseChance) {
            System.out.println("🎊 Tebrikler! " + enemy.getName() + " yakalandı!");
            enemy.healToFull();
            player.addPokemon(enemy);
            return true;
        } else {
            System.out.println(enemy.getName() + " Pokéball'dan kurtuldu!");
            return false;
        }
    }

    // Güvenli int okuma (yanlış girişte çökmemesi için)
    private int readIntSafe() {
        while (true) {
            try {
                String line = scanner.nextLine();
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Lütfen sayı gir.");
            }
        }
    }
}
