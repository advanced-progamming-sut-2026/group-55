package pvz.model.entity.collectible.sun;

public enum SunValue {
    SMALLSUN(25),
    NORMALSUN(50),
    BIGSUN(75),
    SPECIALSUN(100),

    FNSUN(150),  // Feed Sunflower
    FTSUN(250),  // Feed Twin Sunflower
    FBSUN(225);  // Feed Primal Sunflower

    private final int value;

    SunValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}