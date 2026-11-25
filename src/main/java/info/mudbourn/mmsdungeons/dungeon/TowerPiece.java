package info.mudbourn.mmsdungeons.dungeon;

// A surface tower piece that is only placed up to a top height.
public interface TowerPiece {

    int UNSET = Integer.MIN_VALUE;

    int mmsDungeons$towerTop();

    void mmsDungeons$setTowerTop(int top);
}
