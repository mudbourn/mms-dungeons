package info.mudbourn.mmsdungeons.mobs.factions.entity;

import com.github.thedeathlycow.thermoo.api.core.v2.Soakable;
import com.github.thedeathlycow.thermoo.api.core.v2.TemperatureAware;
import com.github.thedeathlycow.thermoo.api.core.v2.TemperatureRecord;
import com.github.thedeathlycow.thermoo.api.core.v2.ThermooLevel;
import com.github.thedeathlycow.thermoo.api.entity.v1.ThermooAttributes;
import com.github.thedeathlycow.thermoo.api.environment.v2.EnvironmentLookup;
import com.github.thedeathlycow.thermoo.api.environment.v2.component.EnvironmentComponentTypes;
import com.github.thedeathlycow.thermoo.api.environment.v2.component.TemperatureRecordComponent;
import com.github.thedeathlycow.thermoo.impl.LivingEntityTickUtil;
import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

// Gives faction humans a player's temperature resistance and the cooling a player gets from a waterskin, through Thermoo.
public final class FactionTemperature {

    private static final double PLAYER_RESISTANCE = 5.0;
    private static final float MIN_COOLING = 0.45f;
    private static final float MAX_COOLING = 0.60f;
    private static final double WARD_RESISTANCE = 4.0;
    private static final Identifier WARD_ID = MmsDungeons.id("waterskin_ward");

    private FactionTemperature() {
    }

    public static void register() {
        ThermooAttributes.baseValueEvent(ThermooAttributes.HEAT_RESISTANCE)
            .register((entity, base) -> entity instanceof FactionMember ? base + PLAYER_RESISTANCE : base);
        ThermooAttributes.baseValueEvent(ThermooAttributes.FROST_RESISTANCE)
            .register((entity, base) -> entity instanceof FactionMember ? base + PLAYER_RESISTANCE : base);
    }

    // Where the temperature sits on its scale: 0 at neutral, 1 at the hottest and -1 at the coldest.
    public static float scale(LivingEntity entity) {
        return TemperatureAware.get(entity).thermoo$getTemperatureScale();
    }

    // The outdoor temperature at the entity, read the way Thermoo reads it for a player.
    public static TemperatureRecord environment(LivingEntity entity, ServerLevel level) {
        return EnvironmentLookup.getInstance()
            .findEnvironmentComponents(level, LivingEntityTickUtil.getTemperatureTickPos(entity))
            .getOrDefault(EnvironmentComponentTypes.TEMPERATURE, TemperatureRecordComponent.DEFAULT);
    }

    public static boolean wet(LivingEntity entity) {
        return entity instanceof Soakable soakable && soakable.thermoo$isWet();
    }

    public static boolean cold(LivingEntity entity) {
        return TemperatureAware.get(entity).thermoo$isCold();
    }

    // Adds temperature from the environment, which a resistance attribute reduces as it does for a player.
    public static void addEnvironment(LivingEntity entity, ServerLevel level, int points) {
        TemperatureAware.get(entity).thermoo$addTemperature(points, ((ThermooLevel) level).thermoo$temperatureSources().environment());
    }

    // Sheds 45 to 60 percent of built-up heat and adds the heat ward, as a player's waterskin sip does.
    public static void coolFromWaterskin(LivingEntity entity) {
        ward(entity, true);
        TemperatureAware temperature = TemperatureAware.get(entity);
        int current = temperature.thermoo$getTemperature();
        if (current <= 0) {
            return;
        }
        float cooling = MIN_COOLING + entity.getRandom().nextFloat() * (MAX_COOLING - MIN_COOLING);
        temperature.thermoo$setTemperature(Math.round(current * (1.0f - cooling)));
    }

    // Adds or removes the extra heat resistance a waterskin sip leaves for a while.
    public static void ward(LivingEntity entity, boolean active) {
        AttributeInstance resistance = entity.getAttribute(ThermooAttributes.HEAT_RESISTANCE);
        if (resistance == null) {
            return;
        }
        if (!active) {
            resistance.removeModifier(WARD_ID);
        } else if (!resistance.hasModifier(WARD_ID)) {
            resistance.addTransientModifier(new AttributeModifier(WARD_ID, WARD_RESISTANCE, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
