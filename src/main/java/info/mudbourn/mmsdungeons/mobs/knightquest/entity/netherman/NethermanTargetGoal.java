package info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman;

import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Netherman;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

// Picks the nearest player, only while the boss is searching.
public class NethermanTargetGoal extends NearestAttackableTargetGoal<Player> {

    private final Netherman netherman;

    public NethermanTargetGoal(Netherman netherman) {
        super(netherman, Player.class, true);
        this.netherman = netherman;
    }

    @Override
    public boolean canUse() {
        return this.netherman.isSearching() && super.canUse();
    }
}
