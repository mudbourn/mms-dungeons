package info.mudbourn.mmsdungeons.mobs.factions;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import info.mudbourn.mmsdungeons.mobs.factions.entity.ThrownBuff;
import info.mudbourn.mmsdungeons.mobs.factions.entity.ThrownWeapon;
import java.util.List;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.spell_engine.internals.target.EntityRelations;

// Registers the bandits, mercenaries and dark guildsmen, their thrown weapons and buff potions, and their Spell Engine alliance.
public final class HumanFactions {

    public static EntityType<FactionMember> BANDIT;
    public static EntityType<FactionMember> MERCENARY;
    public static EntityType<FactionMember> DARK_GUILDSMAN;
    public static EntityType<ThrownWeapon> THROWN_WEAPON;
    public static EntityType<ThrownBuff> THROWN_BUFF;

    private HumanFactions() {
    }

    public static void register() {
        BANDIT = member(Faction.BANDIT);
        MERCENARY = member(Faction.MERCENARY);
        DARK_GUILDSMAN = member(Faction.GUILDSMAN);
        Identifier thrownId = MmsDungeons.id("thrown_weapon");
        THROWN_WEAPON = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            thrownId,
            EntityType.Builder.<ThrownWeapon>of(ThrownWeapon::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(4)
                .updateInterval(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, thrownId))
        );
        Identifier buffId = MmsDungeons.id("thrown_buff");
        THROWN_BUFF = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            buffId,
            EntityType.Builder.<ThrownBuff>of(ThrownBuff::new, MobCategory.MISC)
                .sized(0.25f, 0.25f)
                .clientTrackingRange(4)
                .updateInterval(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, buffId))
        );

        FabricDefaultAttributeRegistry.register(BANDIT, attributes(24.0, 0.3));
        FabricDefaultAttributeRegistry.register(MERCENARY, attributes(30.0, 0.31));
        FabricDefaultAttributeRegistry.register(DARK_GUILDSMAN, attributes(40.0, 0.32));

        FactionSpawns.register();
        FearAudit.register();
        EntityRelations.registerTeamMatcher("mms_dungeons_factions", (attacker, target) -> {
            if (attacker instanceof FactionMember one && target instanceof FactionMember two && one.faction() == two.faction()) {
                return new EntityRelations.TeamRelation(true, false);
            }
            return null;
        });
    }

    public static List<EntityType<FactionMember>> types() {
        return List.of(BANDIT, MERCENARY, DARK_GUILDSMAN);
    }

    public static EntityType<FactionMember> typeOf(Faction faction) {
        return switch (faction) {
            case BANDIT -> BANDIT;
            case MERCENARY -> MERCENARY;
            case GUILDSMAN -> DARK_GUILDSMAN;
        };
    }

    private static EntityType<FactionMember> member(Faction faction) {
        Identifier id = MmsDungeons.id(faction.id());
        EntityType.Builder<FactionMember> builder = EntityType.Builder.<FactionMember>of(
                (type, level) -> new FactionMember(type, level, faction),
                MobCategory.MONSTER
            )
            .sized(0.6f, 1.8f)
            .eyeHeight(1.62f)
            .clientTrackingRange(8);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    private static AttributeSupplier attributes(double health, double speed) {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, health)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.ATTACK_SPEED, 4.0)
            .add(Attributes.MOVEMENT_SPEED, speed)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .build();
    }
}
