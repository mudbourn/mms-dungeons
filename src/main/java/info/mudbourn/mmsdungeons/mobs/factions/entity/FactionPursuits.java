package info.mudbourn.mmsdungeons.mobs.factions.entity;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundEvents;
import java.util.function.Predicate;
import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import info.mudbourn.mmsdungeons.mobs.factions.FactionSupplies;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

// Idle pursuits of faction humans: hunting monsters, and farming or herding on the Overworld by day.
final class FactionPursuits {

    private static final int CHECK_TICKS = 40;
    private static final double HUNT_RANGE_OVERWORLD = 20.0;
    private static final double HUNT_RANGE_ELSEWHERE = 48.0;
    private static final double FARM_DEFENSE_RANGE = 10.0;
    private static final int HARVEST_SEEDS = 2;
    private static final int PLOT_RADIUS = 3;
    private static final int PLOT_SIZE = 8;
    private static final int FIELD_RINGS = 2;
    // How far water keeps farmland wet, across; it also reaches one block up.
    private static final int HYDRATION_RANGE = 4;
    // The most of its farmed meal a farmer keeps in its pack; past this the harvest drops on the ground.
    private static final int MEAL_STOCK = 16;
    // Herding reach, animals kept per kind, and cooked meat packed per slaughter.
    private static final double HERD_RANGE = 16.0;
    private static final int HERD_KEEP = 3;
    private static final int MEAT_PER_ANIMAL = 2;
    // The cooked meat each kind of farm animal gives.
    private static final Map<EntityType<?>, Item> MEAT = Map.of(
        EntityTypes.COW, Items.COOKED_BEEF,
        EntityTypes.PIG, Items.COOKED_PORKCHOP,
        EntityTypes.SHEEP, Items.COOKED_MUTTON,
        EntityTypes.CHICKEN, Items.COOKED_CHICKEN);
    private static final double WORK_REACH = 2.5;
    private static final int WORK_TICKS = 20;
    private static final int GIVE_UP_TICKS = 200;

    private FactionPursuits() {
    }

    // Adds one meal to a member's pack, up to MEAL_STOCK of it; false when it takes none.
    private static boolean stock(FactionMember mob, ItemStack meal) {
        if (meal.isEmpty() || held(mob, meal) >= MEAL_STOCK) {
            return false;
        }
        for (ItemStack stack : mob.supplies()) {
            if (ItemStack.isSameItemSameComponents(stack, meal) && stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                return true;
            }
        }
        mob.supplies().add(meal.copyWithCount(1));
        return true;
    }

    private static boolean has(FactionMember mob, Predicate<ItemStack> test) {
        return test.test(mob.getMainHandItem()) || mob.supplies().stream().anyMatch(test);
    }

    // Adds a stack to a member's pack, topping up a matching stack first.
    private static void stash(FactionMember mob, ItemStack added) {
        for (ItemStack stack : mob.supplies()) {
            if (ItemStack.isSameItemSameComponents(stack, added) && stack.getCount() + added.getCount() <= stack.getMaxStackSize()) {
                stack.grow(added.getCount());
                return;
            }
        }
        mob.supplies().add(added);
    }

    private static int held(FactionMember mob, ItemStack meal) {
        int held = 0;
        for (ItemStack stack : mob.supplies()) {
            if (ItemStack.isSameItemSameComponents(stack, meal)) {
                held += stack.getCount();
            }
        }
        return held;
    }

    // About half the members with a farming origin take to farming; the rest of their crew keeps watch.
    static boolean farmer(FactionMember mob) {
        FactionOrigin origin = mob.origin();
        return (origin == FactionOrigin.AVIAN || origin == FactionOrigin.FLORAN) && (mob.getUUID().getLeastSignificantBits() & 1L) == 0L;
    }

    // The other half of the avians, and about half the beastfolk, keep the crew's herd instead.
    private static boolean herder(FactionMember mob) {
        FactionOrigin origin = mob.origin();
        return (origin == FactionOrigin.AVIAN || origin == FactionOrigin.BEASTFOLK) && (mob.getUUID().getLeastSignificantBits() & 1L) == 1L;
    }

    // On the Overworld, by day, under open sky: where farming comes first.
    private static boolean farmingTime(FactionMember mob) {
        Level level = mob.level();
        return level.dimension() == Level.OVERWORLD && level.isBrightOutside() && level.canSeeSky(mob.blockPosition());
    }

    // Picks the nearest monster as quarry, close by on the Overworld and far elsewhere.
    static final class HuntMonstersGoal extends TargetGoal {

        private final FactionMember hunter;
        private int check;

        HuntMonstersGoal(FactionMember mob) {
            super(mob, true);
            this.hunter = mob;
        }

        @Override
        public boolean canUse() {
            if (--this.check > 0 || this.hunter.getTarget() != null || this.hunter.downed()) {
                return false;
            }
            this.check = CHECK_TICKS;
            double range = this.range();
            LivingEntity nearest = null;
            double best = range * range;
            for (LivingEntity other : this.hunter.level().getEntitiesOfClass(LivingEntity.class, this.hunter.getBoundingBox().inflate(range), this::quarry)) {
                double distance = this.hunter.distanceToSqr(other);
                if (distance < best && this.hunter.getSensing().hasLineOfSight(other)) {
                    nearest = other;
                    best = distance;
                }
            }
            this.targetMob = nearest;
            return nearest != null;
        }

        @Override
        public void start() {
            this.hunter.setTarget(this.targetMob);
            super.start();
        }

        @Override
        protected double getFollowDistance() {
            return this.range();
        }

        private double range() {
            if (this.hunter.level().dimension() != Level.OVERWORLD) {
                return HUNT_RANGE_ELSEWHERE;
            }
            return farmer(this.hunter) && farmingTime(this.hunter) ? FARM_DEFENSE_RANGE : HUNT_RANGE_OVERWORLD;
        }

        private boolean quarry(LivingEntity other) {
            return other instanceof Enemy
                && !(other instanceof FactionMember)
                && !(other instanceof OwnableEntity owned && owned.getOwner() != null)
                && other.isAlive()
                && !this.hunter.considersEntityAsAlly(other);
        }
    }

    // Keeps a ring of eight fields around a well where it first settled, sown with the crop behind its meal and packed as that meal.
    static final class FarmGoal extends Goal {

        private final FactionMember mob;
        private @Nullable BlockPos plot;
        private @Nullable BlockPos job;
        private @Nullable Block crop;
        private int check;
        private int work;
        private int walking;

        FarmGoal(FactionMember mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--this.check > 0 || !this.free() || !(this.mob.level() instanceof ServerLevel level)
                || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
                return false;
            }
            this.check = CHECK_TICKS;
            if (this.crop == null) {
                this.crop = this.mealCrop();
            }
            if (this.plot == null || this.plot.distSqr(this.mob.blockPosition()) > 32 * 32) {
                this.plot = this.mob.blockPosition();
            }
            this.job = this.nextJob(level);
            return this.job != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.job != null && this.free() && this.walking < GIVE_UP_TICKS;
        }

        @Override
        public void start() {
            this.work = 0;
            this.walking = 0;
        }

        @Override
        public void stop() {
            this.job = null;
            this.mob.getNavigation().stop();
            this.mob.stowTool();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos job = this.job;
            if (job == null || !(this.mob.level() instanceof ServerLevel level)) {
                return;
            }
            this.mob.getLookControl().setLookAt(job.getX() + 0.5, job.getY() + 0.5, job.getZ() + 0.5);
            if (this.mob.distanceToSqr(job.getX() + 0.5, job.getY() + 0.5, job.getZ() + 0.5) > WORK_REACH * WORK_REACH) {
                this.walking++;
                if (this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(job.getX() + 0.5, job.getY() + 1.0, job.getZ() + 0.5, 1.0);
                }
                return;
            }
            this.mob.getNavigation().stop();
            if (this.work == 0 && !this.ready(level, job)) {
                this.job = this.nextJob(level);
                return;
            }
            if (++this.work < WORK_TICKS) {
                return;
            }
            this.mob.swing(InteractionHand.MAIN_HAND, this.mob.getMainHandItem().getAttackAnimation(), true);
            this.act(level, job);
            this.job = this.nextJob(level);
            this.work = 0;
            this.walking = 0;
        }

        private @Nullable Block mealCrop() {
            for (ItemStack stack : this.mob.supplies()) {
                Block crop = FactionSupplies.meal(stack) ? FactionSupplies.crop(stack) : null;
                if (crop != null) {
                    return crop;
                }
            }
            return null;
        }

        private Block sown() {
            return this.crop != null ? this.crop : Blocks.WHEAT;
        }

        // Packs one meal from a harvest; false when it keeps none and the crop should drop.
        private boolean pack(Block crop) {
            return this.crop != null && stock(this.mob, FactionSupplies.meal(crop));
        }

        private boolean free() {
            return farmer(this.mob) && farmingTime(this.mob) && this.mob.getTarget() == null && !this.mob.downed();
        }

        // Water first, then ripe crops, then bare farmland to sow, then ground to till while the plot is short of nine.
        private @Nullable BlockPos nextJob(ServerLevel level) {
            BlockPos plot = this.plot;
            if (plot == null) {
                return null;
            }
            BlockPos well = plot.below();
            if (!watered(level, well) && canHoldWater(level, well) && has(this.mob, stack -> stack.is(Items.WATER_BUCKET))) {
                return well;
            }
            BlockPos sow = null;
            int fields = 0;
            for (BlockPos pos : BlockPos.betweenClosed(plot.offset(-PLOT_RADIUS, -2, -PLOT_RADIUS), plot.offset(PLOT_RADIUS, 1, PLOT_RADIUS))) {
                BlockState state = level.getBlockState(pos);
                BlockState above = level.getBlockState(pos.above());
                if (state.is(Blocks.FARMLAND)) {
                    fields++;
                    if (above.getBlock() instanceof CropBlock crop && crop.isMaxAge(above)) {
                        return pos.immutable();
                    }
                    if (above.isAir() && sow == null && has(this.mob, this::seed)) {
                        sow = pos.immutable();
                    }
                }
            }
            if (sow != null || fields >= PLOT_SIZE || !has(this.mob, stack -> stack.is(ItemTags.HOES))) {
                return sow;
            }
            BlockPos water = nearestWater(level, well);
            return water == null ? null : tillAround(level, water);
        }

        // The nearest grass or dirt hugging the water, ring by ring outward, so the field packs tight around its well.
        private static @Nullable BlockPos tillAround(ServerLevel level, BlockPos water) {
            for (int ring = 1; ring <= FIELD_RINGS; ring++) {
                for (BlockPos pos : BlockPos.betweenClosed(water.offset(-ring, 0, -ring), water.offset(ring, 0, ring))) {
                    if (Math.max(Math.abs(pos.getX() - water.getX()), Math.abs(pos.getZ() - water.getZ())) != ring) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if ((state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) && level.getBlockState(pos.above()).isAir() && level.canSeeSky(pos.above())) {
                        return pos.immutable();
                    }
                }
            }
            return null;
        }

        // The water source closest to the well spot on its level.
        private static @Nullable BlockPos nearestWater(ServerLevel level, BlockPos well) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(well.offset(-HYDRATION_RANGE, 0, -HYDRATION_RANGE), well.offset(HYDRATION_RANGE, 0, HYDRATION_RANGE))) {
                if (level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource() && pos.distSqr(well) < bestDistance) {
                    bestDistance = pos.distSqr(well);
                    best = pos.immutable();
                }
            }
            return best;
        }

        // Whether water lies close enough to keep farmland at this height wet.
        private static boolean watered(ServerLevel level, BlockPos ground) {
            for (BlockPos pos : BlockPos.betweenClosed(ground.offset(-HYDRATION_RANGE, 0, -HYDRATION_RANGE),
                ground.offset(HYDRATION_RANGE, 1, HYDRATION_RANGE))) {
                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    return true;
                }
            }
            return false;
        }

        // Ground open to the sky with solid ground on all four sides, so water poured there stays put.
        private static boolean canHoldWater(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (!(state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.FARMLAND))
                || !level.getBlockState(pos.above()).isAir() || !level.canSeeSky(pos.above())) {
                return false;
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (!level.getBlockState(pos.relative(side)).isFaceSturdy(level, pos.relative(side), side.getOpposite())) {
                    return false;
                }
            }
            return true;
        }

        // Takes out the item a job needs before working it: a water bucket to dig a well, seeds to sow, a hoe to till.
        private boolean ready(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            BlockState above = level.getBlockState(pos.above());
            if (!watered(level, pos) && canHoldWater(level, pos)) {
                return this.mob.drawSupply(stack -> stack.is(Items.WATER_BUCKET));
            }
            if (state.is(Blocks.FARMLAND) && above.getBlock() instanceof CropBlock crop && crop.isMaxAge(above)) {
                return true;
            }
            if (state.is(Blocks.FARMLAND)) {
                return this.mob.drawSupply(this::seed);
            }
            return this.mob.drawSupply(stack -> stack.is(ItemTags.HOES));
        }

        private void act(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            BlockState above = level.getBlockState(pos.above());
            ItemStack held = this.mob.getMainHandItem();
            if (!watered(level, pos) && canHoldWater(level, pos) && held.is(Items.WATER_BUCKET)) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, this.mob.getSoundSource(), 1.0f, 1.0f);
                this.mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BUCKET));
            } else if (state.is(Blocks.FARMLAND) && above.getBlock() instanceof CropBlock crop && crop.isMaxAge(above)) {
                level.destroyBlock(pos.above(), !this.pack(crop), this.mob);
                stash(this.mob, FactionSupplies.seed(crop).copyWithCount(HARVEST_SEEDS));
            } else if (state.is(Blocks.FARMLAND) && above.isAir() && this.seed(held)) {
                level.setBlock(pos.above(), this.sown().defaultBlockState(), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.CROP_PLANTED, this.mob.getSoundSource(), 1.0f, 1.0f);
                held.shrink(1);
            } else if ((state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) && above.isAir() && held.is(ItemTags.HOES)) {
                level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.HOE_TILL.value(), this.mob.getSoundSource(), 1.0f, 1.0f);
                held.hurtAndBreak(1, this.mob, EquipmentSlot.MAINHAND);
            }
        }

        // Whether this stack plants the plot's crop.
        private boolean seed(ItemStack stack) {
            return !stack.isEmpty() && ItemStack.isSameItem(stack, FactionSupplies.seed(this.sown()));
        }
    }

    // Breeds nearby farm animals up to HERD_KEEP of a kind and slaughters the surplus for the crew's meat eaters.
    static final class HerdGoal extends Goal {

        private final FactionMember mob;
        private @Nullable Animal target;
        private @Nullable Animal mate;
        private int check;
        private int work;
        private int walking;

        HerdGoal(FactionMember mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--this.check > 0 || !this.free() || !(this.mob.level() instanceof ServerLevel level)
                || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
                return false;
            }
            this.check = CHECK_TICKS;
            return this.nextJob(level);
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.target.isAlive() && this.free() && this.walking < GIVE_UP_TICKS;
        }

        @Override
        public void start() {
            this.work = 0;
            this.walking = 0;
        }

        @Override
        public void stop() {
            this.target = null;
            this.mate = null;
            this.mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Animal target = this.target;
            if (target == null || !(this.mob.level() instanceof ServerLevel level)) {
                return;
            }
            this.mob.getLookControl().setLookAt(target);
            if (this.mob.distanceToSqr(target) > WORK_REACH * WORK_REACH) {
                this.walking++;
                if (this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(target, 1.0);
                }
                return;
            }
            this.mob.getNavigation().stop();
            if (++this.work < WORK_TICKS) {
                return;
            }
            this.mob.swing(InteractionHand.MAIN_HAND, this.mob.getMainHandItem().getAttackAnimation(), true);
            if (this.mate != null) {
                breed(target);
                breed(this.mate);
            } else {
                slaughter(level, target);
            }
            this.target = null;
        }

        private boolean free() {
            return herder(this.mob) && farmingTime(this.mob) && this.mob.getTarget() == null && !this.mob.downed();
        }

        // Slaughter first when a kind is past HERD_KEEP and a meat eater has room, else a pair to breed.
        private boolean nextJob(ServerLevel level) {
            if (this.hungriest(level, ItemStack.EMPTY) == null) {
                return false;
            }
            Map<EntityType<?>, List<Animal>> herd = new HashMap<>();
            for (Animal animal : level.getEntitiesOfClass(Animal.class, this.mob.getBoundingBox().inflate(HERD_RANGE),
                animal -> animal.isAlive() && !animal.isBaby() && MEAT.containsKey(animal.getType()))) {
                herd.computeIfAbsent(animal.getType(), type -> new ArrayList<>()).add(animal);
            }
            for (Map.Entry<EntityType<?>, List<Animal>> kind : herd.entrySet()) {
                if (kind.getValue().size() > HERD_KEEP && this.hungriest(level, new ItemStack(MEAT.get(kind.getKey()))) != null) {
                    this.target = kind.getValue().getFirst();
                    this.mate = null;
                    return true;
                }
            }
            for (List<Animal> kind : herd.values()) {
                List<Animal> ready = kind.stream().filter(animal -> animal.canFallInLove() && !animal.isInLove()).toList();
                if (kind.size() < HERD_KEEP && ready.size() >= 2) {
                    this.target = ready.get(0);
                    this.mate = ready.get(1);
                    return true;
                }
            }
            return false;
        }

        // The nearby meat eater holding least of this meat, or of any meat for an empty stack.
        private @Nullable FactionMember hungriest(ServerLevel level, ItemStack meat) {
            FactionMember hungriest = null;
            int least = MEAL_STOCK;
            for (FactionMember member : level.getEntitiesOfClass(FactionMember.class, this.mob.getBoundingBox().inflate(HERD_RANGE),
                member -> member.isAlive() && member.crew().equals(this.mob.crew()) && eatsMeat(member))) {
                int held = meat.isEmpty() ? member.supplies().stream().filter(FactionSupplies::meat).mapToInt(ItemStack::getCount).sum()
                    : held(member, meat);
                if (held < least) {
                    hungriest = member;
                    least = held;
                }
            }
            return hungriest;
        }

        // A member whose pack holds no meal a field grows.
        private static boolean eatsMeat(FactionMember member) {
            return member.supplies().stream().noneMatch(stack -> FactionSupplies.meal(stack) && FactionSupplies.crop(stack) != null);
        }

        private static void breed(Animal animal) {
            if (animal.isAlive() && animal.canFallInLove()) {
                animal.setInLove(null);
            }
        }

        private void slaughter(ServerLevel level, Animal animal) {
            ItemStack meat = new ItemStack(MEAT.get(animal.getType()));
            animal.kill(level);
            for (int i = 0; i < MEAT_PER_ANIMAL; i++) {
                FactionMember member = this.hungriest(level, meat);
                if (member != null) {
                    stock(member, meat);
                }
            }
        }
    }
}
