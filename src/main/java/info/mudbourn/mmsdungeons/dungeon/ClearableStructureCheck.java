package info.mudbourn.mmsdungeons.dungeon;

// A structure check whose cached answers can be dropped when the Deep Region rebuilds.
public interface ClearableStructureCheck {

    void mmsDungeons$clear();
}
