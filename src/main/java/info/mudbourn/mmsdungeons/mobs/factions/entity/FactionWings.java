package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsorigins.power.PhantomFlightPower;
import info.mudbourn.mmsorigins.power.SurgingFlight;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Power;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

// A faction member's real flight power from MMS Origins: it says when the member may fly and how fast, as it does for a player. Only loaded with Origins and MMS Origins.
final class FactionWings {

    private static final Identifier FIRE_POWER = Identifier.fromNamespaceAndPath("mms_origins", "fire_power");

    private FactionWings() {
    }

    // Whether the origin's flight power lets the member fly now; false when it has none.
    static boolean allowed(LivingEntity mob) {
        SurgingFlight flight = flight(mob);
        return flight != null && flight.allowed(mob);
    }

    // The power's flying speed in player terms, built up along its surge curve after the given ticks of surging.
    static float speed(LivingEntity mob, int surgeTicks) {
        SurgingFlight flight = flight(mob);
        if (flight == null) {
            return 0.0f;
        }
        float cruise = flight.cruiseSpeed(mob);
        return surgeTicks > 0 ? SurgingFlight.surgeSpeed(cruise, surgeTicks) : cruise;
    }

    // Whether a phantom is in phantom form, which its flight power requires.
    static boolean formed(LivingEntity mob) {
        for (Power power : PowerHolderComponent.KEY.get(mob).getPowers()) {
            if (power instanceof PhantomFlightPower) {
                return power.isActive();
            }
        }
        return false;
    }

    // The blaze's fuel as a share of its full bar, or -1 when it has none.
    static float fuel(LivingEntity mob) {
        int max = FactionOriginPowers.max(mob, FIRE_POWER);
        return max <= 0 ? -1.0f : FactionOriginPowers.resource(mob, FIRE_POWER) / (float) max;
    }

    private static @Nullable SurgingFlight flight(LivingEntity mob) {
        for (Power power : PowerHolderComponent.KEY.get(mob).getPowers()) {
            if (power instanceof SurgingFlight flight) {
                return flight;
            }
        }
        return null;
    }
}
