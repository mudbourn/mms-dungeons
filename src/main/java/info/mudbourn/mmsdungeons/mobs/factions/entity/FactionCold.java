package info.mudbourn.mmsdungeons.mobs.factions.entity;

import com.github.thedeathlycow.frostiful.compat.TrinketsIntegration;
import com.github.thedeathlycow.frostiful.config.FrostifulConfigYACL;
import com.github.thedeathlycow.frostiful.config.section.EnvironmentSettings;
import com.github.thedeathlycow.frostiful.registry.FGameRules;
import com.github.thedeathlycow.frostiful.survival.ServerPlayerEnvironmentTickListeners;
import com.github.thedeathlycow.thermoo.api.core.v2.TemperatureRecord;
import com.github.thedeathlycow.thermoo.api.core.v2.TemperatureUnit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

// Frostiful's cold from a cold environment, which it applies to players only, worked out for a faction human.
final class FactionCold {

    private static final double WARMING_CELSIUS = 15.0;

    private FactionCold() {
    }

    // Points of temperature for one check, negative for cold, scaled up by how many ticks the check stands for.
    static int change(LivingEntity entity, ServerLevel level, TemperatureRecord environment, int cadence) {
        EnvironmentSettings settings = FrostifulConfigYACL.environmentSettings();
        int points = ServerPlayerEnvironmentTickListeners.envTemperatureToTemperaturePoint(environment, settings);
        if (points < 0 && FactionTemperature.wet(entity)) {
            points = (int) (points * settings.environmentFreezingSoakedMultiplier());
        }
        if (points == 0 && FactionTemperature.cold(entity) && environment.valueInUnit(TemperatureUnit.CELSIUS) >= WARMING_CELSIUS) {
            return 1;
        }
        if (points >= 0) {
            return points;
        }
        int interval = settings.environmentFreezingTickInterval();
        if (interval > cadence && entity.tickCount % interval != 0 || FactionTemperature.scale(entity) < settings.minEnvironmentalFreezingTemperatureScale()) {
            return 0;
        }
        boolean enabled = settings.enableEnvironmentFreezing() && level.getGameRules().get(FGameRules.ENABLE_ENVIRONMENT_FREEZING);
        if (!enabled && !TrinketsIntegration.wearingFrostologyCloak(entity)) {
            return 0;
        }
        return points * Math.max(1, cadence / Math.max(1, interval));
    }
}
