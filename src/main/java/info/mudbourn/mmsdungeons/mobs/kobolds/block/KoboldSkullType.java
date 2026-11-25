package info.mudbourn.mmsdungeons.mobs.kobolds.block;

import net.minecraft.world.level.block.SkullBlock;

public enum KoboldSkullType implements SkullBlock.Type {
    SKELEBOLD("skelebold"),
    WITHERBOLD("witherbold");

    private final String name;

    KoboldSkullType(String name) {
        this.name = name;
        TYPES.put(name, this);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
