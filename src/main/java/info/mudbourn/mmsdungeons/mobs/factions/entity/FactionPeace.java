package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// A sign of peace: crouch two or more times in quick succession near Overworld humans. Those scared of you crouch back and are friendly at once; any other neutral human answers only while you hold out something valuable, then waits for one or two gifts of value before it crouches again and is friendly. Friendly humans stay so for five minutes: they keep near you and fight whatever hurts you, until you hit their crew.
public final class FactionPeace {

    private static final int MIN_PRESSES = 2;
    private static final int PRESS_GAP_TICKS = 12;
    private static final int SETTLE_TICKS = 8;
    private static final double RANGE = 16.0;
    private static final int HALF_BOB_MIN_TICKS = 2;
    private static final int HALF_BOB_SPREAD_TICKS = 2;
    private static final int PAUSE_CHANCE = 4;
    private static final int PAUSE_MAX_TICKS = 3;
    private static final int ANSWER_DELAY_MIN_TICKS = 5;
    private static final int ANSWER_DELAY_SPREAD_TICKS = 10;
    // How many times a human crouches back, weighted towards two and three: 2, 3, 4 or 5.
    private static final int[] BOB_COUNT_WEIGHTS = {0, 0, 3, 4, 2, 1};
    private static final int FRIENDLY_TICKS = 6000;
    private static final int OFFERING_WAIT_TICKS = 600;
    private static final int MAX_GIFTS = 2;
    // Things of value a human takes as an offering.
    private static final TagKey<Item> OFFERINGS = TagKey.create(Registries.ITEM, MmsDungeons.id("faction_offerings"));
    private static final double FOLLOW_FAR = 8.0;
    private static final double FOLLOW_NEAR = 4.0;
    private static final double GUARD_RANGE = 16.0;
    private static final int GUARD_MEMORY_TICKS = 100;

    private static final Map<UUID, Presses> PRESSES_BY_PLAYER = new HashMap<>();

    private static final class Presses {
        boolean wasCrouching;
        int count;
        int lastTick;
    }

    FactionPeace() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PRESSES_BY_PLAYER.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                watch(player);
            }
        });
    }

    // Counts a run of quick crouch presses, and signals peace once a run of two or more has settled.
    private static void watch(ServerPlayer player) {
        Presses presses = PRESSES_BY_PLAYER.computeIfAbsent(player.getUUID(), id -> new Presses());
        boolean crouching = player.isShiftKeyDown();
        boolean pressed = crouching && !presses.wasCrouching;
        presses.wasCrouching = crouching;
        int now = player.level().getServer().getTickCount();
        if (pressed) {
            presses.count = now - presses.lastTick <= PRESS_GAP_TICKS ? presses.count + 1 : 1;
            presses.lastTick = now;
            return;
        }
        if (presses.count == 0 || crouching || now - presses.lastTick < SETTLE_TICKS) {
            return;
        }
        int count = presses.count;
        presses.count = 0;
        if (count < MIN_PRESSES) {
            return;
        }
        ServerLevel level = player.level();
        for (FactionMember member : level.getEntitiesOfClass(FactionMember.class, player.getBoundingBox().inflate(RANGE), member -> answers(member, player))) {
            member.peace().answer(player, !member.cowedBy(player));
        }
    }

    private static boolean answers(FactionMember member, Player player) {
        return member.isAlive()
            && member.isNeutral()
            && !member.downed()
            && member.getTarget() == null
            && !member.provokedBy(player)
            && (member.cowedBy(player) || offering(player.getMainHandItem()) || offering(player.getOffhandItem()))
            && member.hasLineOfSight(player);
    }

    static boolean offering(ItemStack stack) {
        return !stack.isEmpty() && stack.is(OFFERINGS);
    }

    private @Nullable Player partner;
    private int answerTicks;
    private int[] halves = new int[0];
    private @Nullable UUID suitor;
    private long suitorUntil;
    private int giftsWanted;
    private @Nullable UUID friend;
    private long friendUntil;
    private boolean hired;

    // Answers a peace sign: befriends the player at once, or, when it wants an offering first, waits a while for one or two gifts.
    void answer(Player player, boolean wantsOffering) {
        this.crouchBack(player);
        if (wantsOffering) {
            if (!player.getUUID().equals(this.suitor) || player.level().getGameTime() >= this.suitorUntil) {
                this.giftsWanted = 1 + player.getRandom().nextInt(MAX_GIFTS);
            }
            this.suitor = player.getUUID();
            this.suitorUntil = player.level().getGameTime() + OFFERING_WAIT_TICKS;
            return;
        }
        this.befriend(player);
    }

    // Takes one valuable item from a player it is waiting on; the last gift wanted earns a crouch back and its friendship. True when it took the item.
    boolean takeOffering(Player player, ItemStack stack) {
        if (!player.getUUID().equals(this.suitor) || player.level().getGameTime() >= this.suitorUntil || !offering(stack)) {
            return false;
        }
        stack.consume(1, player);
        if (--this.giftsWanted <= 0) {
            this.suitor = null;
            this.crouchBack(player);
            this.befriend(player);
            this.hired = true;
        }
        return true;
    }

    // Thanks a player who got it back on its feet: a crouch back and the same friendship a peace sign earns. A follower its payer revives keeps following.
    void thank(Player player) {
        boolean paid = this.hired && player.getUUID().equals(this.friend);
        this.crouchBack(player);
        this.befriend(player);
        this.hired = paid;
    }

    private void befriend(Player player) {
        this.hired = false;
        this.friend = player.getUUID();
        this.friendUntil = player.level().getGameTime() + FRIENDLY_TICKS;
    }

    // Plans a crouch back: a short pause, then two to five crouches, each down and up at its own slightly uneven pace.
    private void crouchBack(Player player) {
        RandomSource random = player.getRandom();
        int bobs = pickBobs(random.nextInt(totalWeight()));
        this.halves = new int[bobs * 2];
        int total = ANSWER_DELAY_MIN_TICKS + random.nextInt(ANSWER_DELAY_SPREAD_TICKS);
        for (int i = 0; i < this.halves.length; i++) {
            this.halves[i] = HALF_BOB_MIN_TICKS + random.nextInt(HALF_BOB_SPREAD_TICKS)
                + (i % 2 == 1 && random.nextInt(PAUSE_CHANCE) == 0 ? random.nextInt(PAUSE_MAX_TICKS) : 0);
            total += this.halves[i];
        }
        this.partner = player;
        this.answerTicks = total;
    }

    private static int totalWeight() {
        int total = 0;
        for (int weight : BOB_COUNT_WEIGHTS) {
            total += weight;
        }
        return total;
    }

    private static int pickBobs(int roll) {
        for (int count = 0; count < BOB_COUNT_WEIGHTS.length; count++) {
            roll -= BOB_COUNT_WEIGHTS[count];
            if (roll < 0) {
                return count;
            }
        }
        return MIN_PRESSES;
    }

    // Whether the peace with this player still holds.
    boolean friendlyTo(Player player, long gameTime) {
        return player.getUUID().equals(this.friend) && gameTime < this.friendUntil;
    }

    // An attack on the crew ends the peace.
    void breakWith(UUID attacker) {
        if (attacker.equals(this.friend)) {
            this.friend = null;
        }
        if (attacker.equals(this.suitor)) {
            this.suitor = null;
        }
    }

    private @Nullable Player friend(FactionMember mob) {
        if (this.friend == null || mob.level().getGameTime() >= this.friendUntil) {
            this.friend = null;
            return null;
        }
        Player player = mob.level().getPlayerByUUID(this.friend);
        return player != null && player.isAlive() && !player.isSpectator() ? player : null;
    }

    // Sets a friendly human on whatever has just hurt its friend. Only a human the friend has paid with offerings also follows them around.
    static final class FriendGoal extends Goal {

        private final FactionMember mob;
        private @Nullable Player friend;

        FriendGoal(FactionMember mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.mob.downed() || this.mob.getTarget() != null) {
                return false;
            }
            FactionPeace peace = this.mob.peace();
            this.friend = peace.friend(this.mob);
            return this.friend != null && (peace.hired || this.attacker(this.friend) != null);
        }

        private @Nullable LivingEntity attacker(Player friend) {
            LivingEntity attacker = friend.getLastHurtByMob();
            if (attacker != null
                && attacker.isAlive()
                && friend.tickCount - friend.getLastHurtByMobTimestamp() < GUARD_MEMORY_TICKS
                && !(attacker instanceof FactionMember member && member.faction() == this.mob.faction())
                && this.mob.distanceTo(attacker) < GUARD_RANGE) {
                return attacker;
            }
            return null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void stop() {
            this.friend = null;
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player friend = this.friend;
            if (friend == null) {
                return;
            }
            LivingEntity attacker = this.attacker(friend);
            if (attacker != null) {
                this.mob.setTarget(attacker);
                return;
            }
            this.mob.getLookControl().setLookAt(friend);
            double distance = this.mob.distanceTo(friend);
            if (distance > FOLLOW_FAR) {
                this.mob.getNavigation().moveTo(friend, 1.0);
            } else if (distance < FOLLOW_NEAR) {
                this.mob.getNavigation().stop();
            }
        }
    }

    // Stops to face the player and crouches back a few times, after a short pause as if thinking it over.
    static final class PeaceGoal extends Goal {

        private final FactionMember mob;

        PeaceGoal(FactionMember mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            FactionPeace peace = this.mob.peace();
            return peace.partner != null && peace.answerTicks > 0 && !this.mob.downed() && this.mob.getTarget() == null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.mob.getNavigation().stop();
        }

        @Override
        public void stop() {
            FactionPeace peace = this.mob.peace();
            peace.partner = null;
            peace.answerTicks = 0;
            if (this.mob.getPose() == Pose.CROUCHING) {
                this.mob.setPose(Pose.STANDING);
            }
        }

        @Override
        public void tick() {
            FactionPeace peace = this.mob.peace();
            if (peace.partner != null) {
                this.mob.getLookControl().setLookAt(peace.partner);
            }
            // Counts back from the end of the plan to find which half of which crouch this tick falls in; before them is the pause.
            int left = --peace.answerTicks;
            boolean down = false;
            for (int i = peace.halves.length - 1; i >= 0; i--) {
                if (left < peace.halves[i]) {
                    down = i % 2 == 0;
                    break;
                }
                left -= peace.halves[i];
            }
            this.mob.setPose(down && peace.answerTicks > 0 ? Pose.CROUCHING : Pose.STANDING);
        }
    }
}
