package info.mudbourn.mmsdungeons.dungeon;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

// A boss bar shown only to the players a boss has turned on: each player it targets joins the fight, and the
// fight ends, hiding the bar from all of them, once it has no target left.
public final class BossBarAggro {

    private static final double RANGE_SQUARED = 64.0 * 64.0;

    private BossBarAggro() {
    }

    public static void sync(ServerBossEvent bar, LivingEntity boss, Set<UUID> fought, Predicate<ServerPlayer> allowed) {
        LivingEntity target = boss instanceof Mob mob ? mob.getTarget() : null;
        if (target == null || !boss.isAlive()) {
            fought.clear();
        } else if (target instanceof ServerPlayer player && allowed.test(player)) {
            fought.add(player.getUUID());
        }
        for (ServerPlayer player : new ArrayList<>(bar.getPlayers())) {
            if (!sees(boss, player, fought)) {
                bar.removePlayer(player);
            }
        }
        if (boss.level() instanceof ServerLevel level) {
            for (ServerPlayer player : level.players()) {
                if (sees(boss, player, fought)) {
                    bar.addPlayer(player);
                }
            }
        }
    }

    private static boolean sees(LivingEntity boss, ServerPlayer player, Set<UUID> fought) {
        return fought.contains(player.getUUID()) && player.level() == boss.level() && !player.isRemoved()
            && player.isAlive() && player.distanceToSqr(boss) <= RANGE_SQUARED;
    }
}
