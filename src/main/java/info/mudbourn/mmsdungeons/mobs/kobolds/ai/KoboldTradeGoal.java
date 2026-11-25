package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldSounds;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldWork;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

// Pays out a loot table to the nearest player a few seconds after being handed an emerald.
public class KoboldTradeGoal extends Goal {

    private static final int HAGGLE_TICKS = 100;
    private static final int PUT_AWAY_TICKS = 20;
    private static final double PLAYER_RANGE = 7.0;

    private final AbstractKobold kobold;
    private final String table;

    public KoboldTradeGoal(AbstractKobold kobold, String table) {
        this.kobold = kobold;
        this.table = table;
    }

    @Override
    public boolean canUse() {
        return this.kobold.isHolding(Items.EMERALD);
    }

    @Override
    public void start() {
        this.kobold.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, HAGGLE_TICKS + PUT_AWAY_TICKS, 10, false, false));
        KoboldWork.queue(HAGGLE_TICKS, this::pay);
    }

    private void pay() {
        if (!this.kobold.isAlive() || !(this.kobold.level() instanceof ServerLevel level)) {
            return;
        }
        InteractionHand hand = this.kobold.handHolding(stack -> stack.is(Items.EMERALD));
        this.kobold.swingForAttack(hand == InteractionHand.OFF_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        this.kobold.playSound(KoboldSounds.TRADE, 1.0f, 1.0f);
        Vec3 destination = LandRandomPos.getPos(this.kobold, 2, 1);
        Player player = level.getNearestPlayer(this.kobold, PLAYER_RANGE);
        if (player != null) {
            destination = player.position();
        } else if (destination == null) {
            destination = this.kobold.position();
        }
        for (ItemStack stack : AbstractKobold.tradeItems(this.kobold, this.table)) {
            BehaviorUtils.throwItem(this.kobold, stack, destination);
        }
        KoboldWork.queue(PUT_AWAY_TICKS, () -> this.putAway(hand));
    }

    private void putAway(InteractionHand hand) {
        ItemStack restored = hand == InteractionHand.MAIN_HAND ? this.kobold.getPrimary() : this.kobold.getSecondary();
        ItemStack other = hand == InteractionHand.MAIN_HAND ? this.kobold.getOffhandItem() : this.kobold.getSecondary();
        this.kobold.setItemInHand(hand, other.is(Items.TRIDENT) ? ItemStack.EMPTY : restored);
    }
}
