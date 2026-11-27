package info.mudbourn.mmsdungeons.mobs.factions.entity;

import com.github.thedeathlycow.scorchful.config.ScorchfulConfig;
import com.github.thedeathlycow.scorchful.config.section.TemperatureConfig;
import com.github.thedeathlycow.thermoo.api.core.v2.TemperatureRecord;
import com.github.thedeathlycow.thermoo.api.core.v2.TemperatureUnit;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

// Scorchful's heat from a hot environment, which it applies to players only, worked out for a faction human.
final class FactionHeat {

    private FactionHeat() {
    }

    // Points of heat for one check, scaled up by how many ticks the check stands for.
    static int change(LivingEntity entity, TemperatureRecord environment, int cadence) {
        TemperatureConfig config = ScorchfulConfig.getTemperatureConfig();
        int interval = config.getEnvironmentHeatingTickInterval();
        if (!config.isEnableEnvironmentHeating() || interval > cadence && entity.tickCount % interval != 0) {
            return 0;
        }
        double celsius = environment.valueInUnit(TemperatureUnit.CELSIUS);
        double minimum = config.getMinTemperatureForHeatC();
        if (celsius < minimum || FactionTemperature.scale(entity) >= config.getMaxEnvironmentHeatingScale()) {
            return 0;
        }
        double degrees = config.getDegreesCPerTemperatureIncrease();
        int points = Mth.floor(config.getEnvironmentTemperatureMultiplier() * ((celsius - minimum + degrees) / degrees));
        return points * Math.max(1, cadence / Math.max(1, interval));
    }
}
