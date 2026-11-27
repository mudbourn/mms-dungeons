package info.mudbourn.mmsdungeons.mobs.factions.entity;

import com.github.thedeathlycow.thermoo.api.core.v2.TemperatureRecord;
import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

// Heats or chills a faction human from the climate where it stands, as Scorchful and Frostiful do for players, then holds it within its origin's tolerance.
final class FactionAmbient {

    private FactionAmbient() {
    }

    static void tick(LivingEntity entity, ServerLevel level, boolean heat, boolean cold, int cadence) {
        TemperatureRecord environment = FactionTemperature.environment(entity, level);
        int points = 0;
        if (heat) {
            points += FactionHeat.change(entity, environment, cadence);
        }
        if (cold) {
            points += FactionCold.change(entity, level, environment, cadence);
        }
        if (points != 0) {
            FactionTemperature.addEnvironment(entity, level, points);
        }
        if (FactionOrigin.POWERS) {
            FactionTolerance.clamp(entity);
        }
    }
}
