package pvz.model.entity.zombie;

public enum ArmorType {
    NONE(0, false),
    CONE(370, false),
    BUCKET(1100, true),
    BRICK(2200, false),
    SHOULDER_ARMOR(1600, true),
    CROWN(1600, true),
    NEWSPAPER(800, false);

    private final int hitpoints;
    private final boolean metallic;

    ArmorType(int hitpoints, boolean metallic) {
        this.hitpoints = hitpoints;
        this.metallic = metallic;
    }

    public int getHitpoints() {
        return hitpoints;
    }

    public boolean isMetallic() {
        return metallic;
    }
}
