package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

// Doubles health, raises damage and adds a second loot roll to the hostile Mutant Monsters mobs that first load in the dungeon.
public final class MutantBuffs {

    private static final String NAMESPACE = "mutantmonsters";
    private static final Set<String> MUTANTS = Set.of("mutant_creeper", "mutant_enderman", "mutant_skeleton", "mutant_zombie");
    private static final String BUFFED_TAG = "mms_dungeons.mutant_buffed";
    private static final double HEALTH_BONUS = 1.0;
    private static final double DAMAGE_BONUS = 0.5;
    private static final Identifier ID = MmsDungeons.id("mutant_buff");

    private MutantBuffs() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(MutantBuffs::onLoad);
        ServerLivingEntityEvents.AFTER_DEATH.register(MutantBuffs::onDeath);
    }

    private static boolean isMutant(Entity entity) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key.getNamespace().equals(NAMESPACE) && MUTANTS.contains(key.getPath());
    }

    private static void onLoad(Entity entity, ServerLevel level) {
        if (!(entity instanceof Mob mob) || !isMutant(entity) || !DungeonDimension.isDungeon(level) || !entity.addTag(BUFFED_TAG)) {
            return;
        }
        scale(mob, Attributes.MAX_HEALTH, HEALTH_BONUS);
        scale(mob, Attributes.ATTACK_DAMAGE, DAMAGE_BONUS);
        mob.setHealth(mob.getMaxHealth());
    }

    private static void scale(Mob mob, Holder<Attribute> attribute, double amount) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null || instance.getBaseValue() <= 0.0 || instance.hasModifier(ID)) {
            return;
        }
        instance.addPermanentModifier(new AttributeModifier(ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Mob mob) || !(entity.level() instanceof ServerLevel level) || !entity.entityTags().contains(BUFFED_TAG)) {
            return;
        }
        mob.getLootTable().ifPresent(key -> {
            LootParams.Builder params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
            if (source.getEntity() instanceof ServerPlayer player) {
                params.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
            }
            level.getServer()
                .reloadableRegistries()
                .getLootTable(key)
                .getRandomItems(params.create(LootContextParamSets.ENTITY), stack -> entity.spawnAtLocation(level, stack));
        });
    }
}
