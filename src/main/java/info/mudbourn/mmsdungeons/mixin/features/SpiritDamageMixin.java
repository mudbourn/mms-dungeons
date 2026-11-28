package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Spirits take 40% less damage from physical attacks and 20% more from magic, and deal soul damage that bypasses armor.
@Mixin(LivingEntity.class)
public abstract class SpiritDamageMixin {

    private static final TagKey<EntityType<?>> SPIRIT = TagKey.create(Registries.ENTITY_TYPE, MmsDungeons.id("kind/spirit"));
    private static final TagKey<DamageType> IS_MAGIC = TagKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("c", "is_magic"));
    private static final ResourceKey<DamageType> SOUL = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("spell_power", "soul"));

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private DamageSource mmsDungeons$spiritSoulDamage(DamageSource source, ServerLevel level) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || !attacker.getType().builtInRegistryHolder().is(SPIRIT)
            || source.is(IS_MAGIC) || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return source;
        }
        return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(SOUL)
            .<DamageSource>map(soul -> new DamageSource(soul, source.getDirectEntity(), attacker))
            .orElse(source);
    }

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float mmsDungeons$spiritDamage(float amount, ServerLevel level, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.getType().builtInRegistryHolder().is(SPIRIT) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        if (source.is(IS_MAGIC)) {
            return amount * 1.2f;
        }
        boolean physical = source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
            || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || source.getDirectEntity() instanceof Projectile;
        return physical ? amount * 0.6f : amount;
    }
}
