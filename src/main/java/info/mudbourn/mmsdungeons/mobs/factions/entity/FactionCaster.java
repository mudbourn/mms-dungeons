package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.Faction;
import info.mudbourn.mmsdungeons.mobs.factions.FactionClass;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.SpellParameters;
import net.spell_engine.internals.delivery.SpellDelivery;
import net.spell_engine.internals.target.SpellIntents;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_power.api.SpellPower;
import org.jspecify.annotations.Nullable;

// Casts Spell Engine spells for a faction mage or priest: picks a spell and target, charges for the cast time, then releases or channels it.
public final class FactionCaster {

    private static final double ALLY_RANGE = 16.0;
    private static final float HURT_FRACTION = 0.7f;
    private static final int MIN_GAP_TICKS = 30;
    private static final int GAP_SPREAD_TICKS = 40;
    private static final float DEFAULT_RANGE = 16.0f;

    private final FactionMember mob;
    private @Nullable Holder<Spell> spell;
    private @Nullable LivingEntity aim;
    private int ticks;
    private int castTicks;
    private boolean channel;
    private int channelInterval;
    private int cooldown;

    FactionCaster(FactionMember mob) {
        this.mob = mob;
    }

    public boolean busy() {
        return this.spell != null;
    }

    // How far the caster likes to stand from its enemy.
    public double preferredRange() {
        return this.mob.factionClass() == FactionClass.PRIEST ? 8.0 : 11.0;
    }

    public void stop() {
        this.spell = null;
        this.aim = null;
        this.mob.setAction(FactionMember.ACTION_NONE);
    }

    public void tick(ServerLevel level, LivingEntity enemy) {
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.spell == null) {
            if (this.cooldown > 0 || !this.begin(level, enemy)) {
                return;
            }
        }
        if (this.aim == null || !this.aim.isAlive()) {
            this.stop();
            return;
        }
        this.face(this.aim);
        this.ticks++;
        if (this.channel) {
            if (this.ticks % this.channelInterval == 0) {
                this.release(level);
            }
            if (this.ticks >= this.castTicks) {
                this.finish();
            }
        } else if (this.ticks >= this.castTicks) {
            this.release(level);
            this.finish();
        }
    }

    private boolean begin(ServerLevel level, LivingEntity enemy) {
        LivingEntity hurtAlly = this.mob.factionClass() == FactionClass.PRIEST ? this.hurtAlly(level) : null;
        LivingEntity target = hurtAlly != null ? hurtAlly : enemy;
        List<Holder<Spell>> spells = this.available(level, hurtAlly != null);
        if (spells.isEmpty()) {
            return false;
        }
        Holder<Spell> chosen = spells.get(this.mob.getRandom().nextInt(spells.size()));
        float range = range(chosen);
        if (target != this.mob && this.mob.distanceTo(target) > range) {
            return false;
        }
        if (target != this.mob && !this.mob.getSensing().hasLineOfSight(target)) {
            return false;
        }
        Spell value = chosen.value();
        this.spell = chosen;
        this.aim = target;
        this.ticks = 0;
        this.castTicks = Math.max(1, Math.round(SpellParameters.getCastDuration(this.mob, value) * 20.0f));
        this.channel = value.active != null && value.active.cast != null && value.active.cast.type == Spell.Active.Cast.Type.CHANNEL;
        this.channelInterval = this.channel ? Math.max(1, value.active.cast.channelTicks()) : 0;
        this.mob.setAction(FactionMember.ACTION_CAST);
        return true;
    }

    private float range(Holder<Spell> spell) {
        try {
            float range = SpellParameters.getRange(this.mob, spell);
            return range > 0.0f ? range : DEFAULT_RANGE;
        } catch (RuntimeException e) {
            return DEFAULT_RANGE;
        }
    }

    private void finish() {
        this.cooldown = MIN_GAP_TICKS + this.mob.getRandom().nextInt(GAP_SPREAD_TICKS);
        this.stop();
    }

    private void release(ServerLevel level) {
        Holder<Spell> holder = this.spell;
        if (holder == null || this.aim == null) {
            return;
        }
        Spell value = holder.value();
        SpellExecution.ImpactContext context = new SpellExecution.ImpactContext()
            .power(SpellPower.getSpellPower(value.school, this.mob))
            .target(SpellIntents.focusMode(value));
        SpellTarget.SearchResult found = SpellTarget.findTargets(this.mob, holder, SpellTarget.SearchResult.empty(), false);
        SpellTarget.SearchResult targets = found.entities().isEmpty() ? SpellTarget.SearchResult.of(this.aim) : found;
        SpellDelivery.resolveAndDeliver(level, this.mob, holder, targets, context, completion -> {
        });
    }

    // Turns body, head and pitch to the target so look-based spell targeting finds it.
    private void face(LivingEntity target) {
        if (target == this.mob) {
            return;
        }
        Vec3 delta = target.getEyePosition().subtract(this.mob.getEyePosition());
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0f;
        float pitch = (float) -(Mth.atan2(delta.y, delta.horizontalDistance()) * Mth.RAD_TO_DEG);
        this.mob.setYRot(yaw);
        this.mob.setYHeadRot(yaw);
        this.mob.setYBodyRot(yaw);
        this.mob.setXRot(pitch);
        this.mob.getLookControl().setLookAt(target);
    }

    // The most hurt faction ally in range, the caster included.
    private @Nullable LivingEntity hurtAlly(ServerLevel level) {
        AABB area = this.mob.getBoundingBox().inflate(ALLY_RANGE);
        Faction faction = this.mob.faction();
        return level.getEntitiesOfClass(
                FactionMember.class,
                area,
                member -> member.faction() == faction && member.isAlive() && member.getHealth() < member.getMaxHealth() * HURT_FRACTION
            )
            .stream()
            .min(Comparator.comparingDouble(member -> member.getHealth() / member.getMaxHealth()))
            .orElse(null);
    }

    private List<Holder<Spell>> available(ServerLevel level, boolean healing) {
        List<Holder<Spell>> spells = new ArrayList<>();
        for (String id : this.spellIds(healing)) {
            SpellRegistry.from(level).get(Identifier.parse(id)).ifPresent(spells::add);
        }
        return spells;
    }

    // The spells for this caster's class, school and faction: stronger factions know more.
    private List<String> spellIds(boolean healing) {
        int rank = this.mob.faction().ordinal();
        List<String> ids = new ArrayList<>();
        if (this.mob.factionClass() == FactionClass.PRIEST) {
            if (healing) {
                ids.add("paladins:heal");
                ids.add("paladins:flash_heal");
                if (rank >= 2) {
                    ids.add("paladins:circle_of_healing");
                }
            } else {
                ids.add("paladins:holy_shock");
                if (rank >= 2) {
                    ids.add("paladins:holy_beam");
                }
            }
            return ids;
        }
        String held = BuiltInRegistries.ITEM.getKey(this.mob.getMainHandItem().getItem()).getPath();
        if (held.contains("fire")) {
            ids.add("wizards:fireball");
            ids.add("wizards:fire_scorch");
            if (rank >= 1) {
                ids.add("wizards:fire_blast");
            }
            if (rank >= 2) {
                ids.add("wizards:fire_meteor");
            }
        } else if (held.contains("frost")) {
            ids.add("wizards:frostbolt");
            ids.add("wizards:frost_shard");
            if (rank >= 1) {
                ids.add("wizards:frost_nova");
            }
            if (rank >= 2) {
                ids.add("wizards:frost_blizzard");
            }
        } else {
            ids.add("wizards:arcane_bolt");
            ids.add("wizards:arcane_blast");
            if (rank >= 1) {
                ids.add("wizards:arcane_missile");
            }
            if (rank >= 2) {
                ids.add("wizards:arcane_beam");
            }
        }
        return ids;
    }
}
