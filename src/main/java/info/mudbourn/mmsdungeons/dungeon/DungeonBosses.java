package info.mudbourn.mmsdungeons.dungeon;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
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

// Gives every living dungeon entity in the c:bosses tag without a bar of its own one, shown once it turns on a player.
// Outside the dungeon a Useless Reptile dragon has one too, for a player only after they struck it.
public final class DungeonBosses {

    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"));
    private static final int INTERVAL = 10;
    private static final Map<LivingEntity, ServerBossEvent> BARS = new ConcurrentHashMap<>();
    private static final Map<LivingEntity, Set<UUID>> ATTACKERS = new ConcurrentHashMap<>();
    private static final Map<LivingEntity, Set<UUID>> FOUGHT = new ConcurrentHashMap<>();
    private static final String DRAGONS = "uselessreptile";
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
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (source.getEntity() instanceof ServerPlayer player && BARS.containsKey(entity) && !DungeonDimension.isDungeon(entity.level())) {
                ATTACKERS.computeIfAbsent(entity, boss -> ConcurrentHashMap.newKeySet()).add(player.getUUID());
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            BARS.clear();
            ATTACKERS.clear();
            FOUGHT.clear();
        });
    }

    private static void onLoad(Entity entity, ServerLevel level) {
        if (!(entity instanceof LivingEntity living)
            || !DungeonDimension.isDungeon(level) && !dragon(entity)
            || !entity.getType().builtInRegistryHolder().is(BOSSES)
            || HAS_OWN_BAR.get(entity.getClass())) {
            return;
        }
        ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), entity.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        BARS.put(living, bar);
    }

    private static void onUnload(Entity entity, ServerLevel level) {
        if (entity instanceof LivingEntity living) {
            ATTACKERS.remove(living);
            FOUGHT.remove(living);
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
                ATTACKERS.remove(boss);
                FOUGHT.remove(boss);
                continue;
            }
            bar.setName(boss.getDisplayName());
            bar.setProgress(Math.clamp(boss.getHealth() / boss.getMaxHealth(), 0.0f, 1.0f));
            boolean dungeon = DungeonDimension.isDungeon(level);
            Set<UUID> fought = FOUGHT.computeIfAbsent(boss, key -> ConcurrentHashMap.newKeySet());
            BossBarAggro.sync(bar, boss, fought, player -> dungeon || struck(boss, player));
        }
    }

    private static boolean dragon(Entity entity) {
        return DRAGONS.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
    }

    private static boolean struck(LivingEntity boss, ServerPlayer player) {
        Set<UUID> attackers = ATTACKERS.get(boss);
        return attackers != null && attackers.contains(player.getUUID());
    }
}
