package info.mudbourn.mmsdungeons.dungeon;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

// Shows a boss bar to nearby players for every living dungeon entity in the c:bosses tag that has no bar of its own.
public final class DungeonBosses {

    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"));
    private static final double RANGE_SQUARED = 64.0 * 64.0;
    private static final int INTERVAL = 10;
    private static final Map<LivingEntity, ServerBossEvent> BARS = new ConcurrentHashMap<>();
    private static final ClassValue<Boolean> HAS_OWN_BAR = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            if (EnderDragon.class.isAssignableFrom(type)) {
                return true;
            }
            for (Class<?> current = type; current != null && current != Entity.class; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && BossEvent.class.isAssignableFrom(field.getType())) {
                        return true;
                    }
                }
            }
            return false;
        }
    };

    private static int counter;

    private DungeonBosses() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(DungeonBosses::onLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register(DungeonBosses::onUnload);
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BARS.clear());
    }

    private static void onLoad(Entity entity, ServerLevel level) {
        if (!(entity instanceof LivingEntity living)
            || !DungeonDimension.isDungeon(level)
            || !entity.getType().builtInRegistryHolder().is(BOSSES)
            || HAS_OWN_BAR.get(entity.getClass())) {
            return;
        }
        ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), entity.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        BARS.put(living, bar);
    }

    private static void onUnload(Entity entity, ServerLevel level) {
        if (entity instanceof LivingEntity living) {
            ServerBossEvent bar = BARS.remove(living);
            if (bar != null) {
                bar.removeAllPlayers();
            }
        }
    }

    private static void tick() {
        if (BARS.isEmpty() || ++counter % INTERVAL != 0) {
            return;
        }
        for (Map.Entry<LivingEntity, ServerBossEvent> entry : BARS.entrySet()) {
            LivingEntity boss = entry.getKey();
            ServerBossEvent bar = entry.getValue();
            if (!(boss.level() instanceof ServerLevel level) || boss.isRemoved() || !boss.isAlive()) {
                bar.removeAllPlayers();
                BARS.remove(boss);
                continue;
            }
            bar.setName(boss.getDisplayName());
            bar.setProgress(Math.clamp(boss.getHealth() / boss.getMaxHealth(), 0.0f, 1.0f));
            for (ServerPlayer player : new ArrayList<>(bar.getPlayers())) {
                if (player.level() != level || player.isRemoved() || player.distanceToSqr(boss) > RANGE_SQUARED) {
                    bar.removePlayer(player);
                }
            }
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(boss) <= RANGE_SQUARED && player.isAlive()) {
                    bar.addPlayer(player);
                }
            }
        }
    }
}
