package info.mudbourn.mmsdungeons.mobs.factions.entity;

import com.github.thedeathlycow.thermoo.api.core.v2.TemperatureAware;
import info.mudbourn.mmsorigins.MmsOriginsPowers;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.PowerTypeReference;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

// Holds a faction human's temperature within what its origin's climate powers allow, as MMS Origins does for players.
final class FactionTolerance {

    private static final float TOLERANCE = 0.5f;
    private static final List<PowerType<?>> COLD_IMMUNE = List.of(power("cold_blooded"));
    private static final List<PowerType<?>> HEAT_IMMUNE = List.of(power("fire_immunity"));
    private static final List<PowerType<?>> COLD_RESISTANT = List.of(power("cold_resistance"), power("predators_vigor"));
    private static final List<PowerType<?>> HEAT_RESISTANT = List.of(power("heat_adaptation"), power("nether_heat_immunity"), power("predators_vigor"));

    private FactionTolerance() {
    }

    static void clamp(LivingEntity entity) {
        TemperatureAware temperature = TemperatureAware.get(entity);
        int current = temperature.thermoo$getTemperature();
        int limit = current;
        if (current < 0) {
            if (holdsAny(entity, COLD_IMMUNE)) {
                limit = 0;
            } else if (holdsAny(entity, COLD_RESISTANT)) {
                limit = Math.max(current, Math.round(temperature.thermoo$getMinTemperature() * TOLERANCE));
            }
        } else if (current > 0) {
            if (holdsAny(entity, HEAT_IMMUNE)) {
                limit = 0;
            } else if (holdsAny(entity, HEAT_RESISTANT)) {
                limit = Math.min(current, Math.round(temperature.thermoo$getMaxTemperature() * TOLERANCE));
            }
        }
        if (limit != current) {
            temperature.thermoo$setTemperature(limit);
        }
    }

    private static boolean holdsAny(LivingEntity entity, List<PowerType<?>> powers) {
        for (PowerType<?> power : powers) {
            if (MmsOriginsPowers.holds(entity, power)) {
                return true;
            }
        }
        return false;
    }

    private static PowerType<?> power(String path) {
        return new PowerTypeReference<>(Identifier.fromNamespaceAndPath("mms_origins", path));
    }
}
