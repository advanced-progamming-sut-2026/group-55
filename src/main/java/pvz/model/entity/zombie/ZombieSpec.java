package pvz.model.entity.zombie;

public class ZombieSpec {

    private final String id;
    private final String name;

    private final int hitpoints;
    private final int eatDps;
    private final double speed;
    private final int waveCost;
    private final ArmorType armor;


    public ZombieSpec(
            String id,
            String name,
            int hitpoints,
            int eatDps,
            double speed,
            int waveCost,
            ArmorType armor
    ) {
        this.id = id;
        this.name = name;
        this.hitpoints = hitpoints;
        this.eatDps = eatDps;
        this.speed = speed;
        this.waveCost = waveCost;
        this.armor = armor;
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getHitpoints() {
        return hitpoints;
    }

    public int getEatDps() {
        return eatDps;
    }

    public double getSpeed() {
        return speed;
    }

    public int getWaveCost() {
        return waveCost;
    }

    public ArmorType getArmor() {
        return armor;
    }
}
