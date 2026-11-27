package info.mudbourn.mmsdungeons.relations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.mixin.features.MobAccessor;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldRascal;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Reads the creature kinds and the relations between them, and answers who hates whom.
public final class MobRelations {

    public static final String PLAYER = "player";
    public static final TagKey<EntityType<?>> LIGHT_WARY = TagKey.create(Registries.ENTITY_TYPE, MmsDungeons.id("light_wary"));

    private static final Logger LOG = LoggerFactory.getLogger(MmsDungeons.MOD_ID);
    private static final Identifier FILE = MmsDungeons.id("mob_relations/relations.json");
    private static final String ANY = "*";
    private static final Map<EntityType<?>, List<String>> KINDS_OF = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, Boolean> FEUDS = new ConcurrentHashMap<>();

    private static volatile Data data = new Data(List.of(), Set.of(), Map.of());

    private MobRelations() {
    }

    public static void register() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            MmsDungeons.id("mob_relations"),
            (ResourceManagerReloadListener) MobRelations::load
        );
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            KINDS_OF.clear();
            FEUDS.clear();
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> addGoals(entity));
    }

    private static void addGoals(Entity entity) {
        if (!(entity instanceof Mob mob) || mob instanceof FeudAware) {
            return;
        }
        if (mob.typeHolder().is(LIGHT_WARY)) {
            ((MobAccessor) mob).mmsDungeons$getTargetSelector().removeAllGoals(goal -> goal.getClass().getName().endsWith("MoleclawUntamedTargetGoal"));
            ((MobAccessor) mob).mmsDungeons$getTargetSelector().addGoal(3, new MoleclawTargetGoal(mob));
        } else if (mob instanceof Enemy || !kindsOf(mob.getType()).isEmpty()) {
            ((MobAccessor) mob).mmsDungeons$getTargetSelector().addGoal(4, new FeudTargetGoal(mob));
        }
    }

    // The kinds an entity type belongs to; players have the kind "player".
    public static List<String> kindsOf(EntityType<?> type) {
        if (type == EntityTypes.PLAYER) {
            return List.of(PLAYER);
        }
        return KINDS_OF.computeIfAbsent(type, MobRelations::lookupKinds);
    }

    private static List<String> lookupKinds(EntityType<?> type) {
        return data.kinds().stream()
            .filter(kind -> type.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, MmsDungeons.id("kind/" + kind))))
            .toList();
    }

    // Whether the mob has any hostile relation at all, so the rest can skip scanning for targets.
    public static boolean mayFeud(Mob mob) {
        return FEUDS.computeIfAbsent(mob.getType(), type -> hasHostileCell(kindsOf(type)) || mob instanceof Enemy && !data.treatedAsPlayer().isEmpty());
    }

    private static boolean hasHostileCell(List<String> kinds) {
        for (String kind : kinds) {
            Map<String, Relation> row = data.rows().get(kind);
            if (row != null && row.containsValue(Relation.HOSTILE)) {
                return true;
            }
        }
        return false;
    }

    // How the viewer sees the target, from their kinds. A kind-specific entry beats the wildcard.
    public static Relation between(LivingEntity viewer, LivingEntity target) {
        List<String> from = kindsOf(viewer.getType());
        List<String> to = kindsOf(target.getType());
        Tally specific = new Tally();
        Tally wild = new Tally();
        for (String a : from) {
            Map<String, Relation> row = data.rows().get(a);
            if (row == null) {
                continue;
            }
            for (String b : to) {
                specific.add(row.get(b));
            }
            wild.add(row.get(ANY));
        }
        if (specific.found) {
            return specific.result();
        }
        if (wild.found && !to.isEmpty()) {
            return wild.result();
        }
        if (viewer instanceof Enemy && to.stream().anyMatch(data.treatedAsPlayer()::contains)) {
            return Relation.HOSTILE;
        }
        return Relation.NEUTRAL;
    }

    // Whether the mob should pick this creature as a target on its own.
    public static boolean shouldTarget(Mob mob, LivingEntity target) {
        if (!target.isAlive() || target == mob || target.isSpectator()) {
            return false;
        }
        if (target instanceof Player player && player.isCreative()) {
            return false;
        }
        if (mob instanceof OwnableEntity owned && owned.getOwner() != null) {
            return false;
        }
        if (mob instanceof FeudAware aware && !aware.mayStartFeud(target)) {
            return false;
        }
        if (target instanceof KoboldRascal && mob.getLastHurtByMob() != target) {
            return false;
        }
        return between(mob, target) == Relation.HOSTILE;
    }

    // Whether a monster that was just hit by another mob should let it go as an accident.
    public static boolean forgives(Mob victim, LivingEntity attacker) {
        if (!DungeonDimension.isDungeon(victim.level()) || !(attacker instanceof Mob) || attacker instanceof OwnableEntity owned && owned.getOwner() != null) {
            return false;
        }
        if (!monsterLike(victim) || !monsterLike(attacker)) {
            return false;
        }
        if (between(victim, attacker) == Relation.ALLIED || between(attacker, victim) == Relation.ALLIED) {
            return true;
        }
        if (((Mob) attacker).getTarget() == victim) {
            return false;
        }
        return between(victim, attacker) != Relation.HOSTILE && between(attacker, victim) != Relation.HOSTILE;
    }

    private static boolean monsterLike(LivingEntity entity) {
        return entity instanceof Enemy || !kindsOf(entity.getType()).isEmpty();
    }

    private static void load(ResourceManager manager) {
        List<String> kinds = new ArrayList<>();
        Set<String> treated = new HashSet<>();
        Map<String, Map<String, Relation>> rows = new HashMap<>();
        for (Resource resource : manager.getResourceStack(FILE)) {
            try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                merge(JsonParser.parseReader(reader).getAsJsonObject(), kinds, treated, rows);
            } catch (Exception e) {
                LOG.error("Could not read {} from {}", FILE, resource.sourcePackId(), e);
            }
        }
        data = new Data(List.copyOf(kinds), Set.copyOf(treated), rows);
        KINDS_OF.clear();
        FEUDS.clear();
    }

    private static void merge(JsonObject file, List<String> kinds, Set<String> treated, Map<String, Map<String, Relation>> rows) {
        if (file.has("kinds")) {
            file.getAsJsonArray("kinds").forEach(kind -> addOnce(kinds, kind.getAsString()));
        }
        if (file.has("treated_as_player")) {
            file.getAsJsonArray("treated_as_player").forEach(kind -> treated.add(kind.getAsString()));
        }
        if (!file.has("relations")) {
            return;
        }
        for (Map.Entry<String, JsonElement> row : file.getAsJsonObject("relations").entrySet()) {
            Map<String, Relation> cells = rows.computeIfAbsent(row.getKey(), key -> new HashMap<>());
            for (Map.Entry<String, JsonElement> cell : row.getValue().getAsJsonObject().entrySet()) {
                Relation relation = Relation.parse(cell.getValue().getAsString());
                if (relation == null) {
                    LOG.warn("Unknown relation {} for {} to {}", cell.getValue(), row.getKey(), cell.getKey());
                } else {
                    cells.put(cell.getKey(), relation);
                }
            }
        }
    }

    private static void addOnce(List<String> list, String value) {
        if (!list.contains(value)) {
            list.add(value);
        }
    }

    private record Data(List<String> kinds, Set<String> treatedAsPlayer, Map<String, Map<String, Relation>> rows) {
    }

    private static final class Tally {

        private boolean found;
        private boolean hostile;
        private boolean allied;

        void add(Relation relation) {
            if (relation != null) {
                this.found = true;
                this.hostile |= relation == Relation.HOSTILE;
                this.allied |= relation == Relation.ALLIED;
            }
        }

        Relation result() {
            return this.hostile ? Relation.HOSTILE : this.allied ? Relation.ALLIED : Relation.NEUTRAL;
        }
    }
}
