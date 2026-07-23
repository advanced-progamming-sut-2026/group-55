package pvz.model.entity.plant;

import java.util.Set;

public class PlantSpec {
    private final int id;
    private final String name;
    private final PlantCategory category;
    private final Set<PlantTag> tags;
    private final int cost;
    private final int baseHp;
    private final String damage;
    private final String baseAbility;
    private final String plantFoodEffect;
    private final String lvl2;
    private final String lvl3;
    private final String lvl4;
    private final double actionInterval;
    private final double recharge;

    public PlantSpec(int id,
                     String name,
                     PlantCategory category,
                     Set<PlantTag> tags,
                     int cost,
                     int baseHp,
                     String damage,
                     String baseAbility,
                     String plantFoodEffect,
                     String lvl2,
                     String lvl3,
                     String lvl4,
                     double actionInterval,
                     double recharge) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.tags = Set.copyOf(tags);
        this.cost = cost;
        this.baseHp = baseHp;
        this.damage = damage;
        this.baseAbility = baseAbility;
        this.plantFoodEffect = plantFoodEffect;
        this.lvl2 = lvl2;
        this.lvl3 = lvl3;
        this.lvl4 = lvl4;
        this.actionInterval = actionInterval;
        this.recharge = recharge;
    }

    public int getBaseHp() { return baseHp; }
    public String getDamage() { return damage; }
    public String getBaseAbility() { return baseAbility; }
    public String getPlantFoodEffect() { return plantFoodEffect; }
    public String getLvl2() { return lvl2; }
    public String getLvl3() { return lvl3; }
    public String getLvl4() { return lvl4; }
    public double getActionInterval() { return actionInterval; }
    public double getRecharge() { return recharge; }
    public int getCost() { return cost; }
    public int getId() { return id; }
    public String getName() { return name; }
    public PlantCategory getCategory() { return category; }
    public Set<PlantTag> getTags() { return tags; }
}