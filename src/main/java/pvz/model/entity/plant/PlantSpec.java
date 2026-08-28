package pvz.model.entity.plant;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import pvz.model.entity.plant.level.PlantLevelResolver;
import pvz.model.entity.plant.level.PlantLevelUpgrade;
import pvz.model.entity.plant.level.PlantUpgradeType;

public class PlantSpec {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;

    private final int id;
    private final String name;
    private final PlantCategory category;
    private final Set<PlantTag> tags;
    private final PlantStackingRole stackingRole;
    private final int baseCost;
    private final int baseHp;
    private final String baseDamage;
    private final String baseAbility;
    private final String plantFoodEffect;
    private final String lvl2;
    private final String lvl3;
    private final String lvl4;
    private final double baseActionInterval;
    private final double baseRecharge;
    private final Map<String, Map<String, Double>> behaviorParams;
    private final List<PlantLevelUpgrade> levelUpgrades;
    private final int level;

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
        this(id, name, category, tags, cost, baseHp, damage, baseAbility,
                plantFoodEffect, lvl2, lvl3, lvl4, actionInterval, recharge,
                Map.of(), List.of(), MIN_LEVEL);
    }

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
                     double recharge,
                     Map<String, Map<String, Double>> behaviorParams) {
        this(id, name, category, tags, cost, baseHp, damage, baseAbility,
                plantFoodEffect, lvl2, lvl3, lvl4, actionInterval, recharge,
                behaviorParams, List.of(), MIN_LEVEL);
    }

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
                     double recharge,
                     Map<String, Map<String, Double>> behaviorParams,
                     List<PlantLevelUpgrade> levelUpgrades) {
        this(id, name, category, tags, cost, baseHp, damage, baseAbility,
                plantFoodEffect, lvl2, lvl3, lvl4, actionInterval, recharge,
                behaviorParams, levelUpgrades, MIN_LEVEL);
    }

    private PlantSpec(int id,
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
                      double recharge,
                      Map<String, Map<String, Double>> behaviorParams,
                      List<PlantLevelUpgrade> levelUpgrades,
                      int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("plant level must be between 1 and 4");
        }
        this.id = id;
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.tags = Set.copyOf(tags);
        this.stackingRole = PlantStackingRole.from(category, this.tags);
        this.baseCost = cost;
        this.baseHp = baseHp;
        this.baseDamage = damage;
        this.baseAbility = baseAbility;
        this.plantFoodEffect = plantFoodEffect;
        this.lvl2 = lvl2;
        this.lvl3 = lvl3;
        this.lvl4 = lvl4;
        this.baseActionInterval = actionInterval;
        this.baseRecharge = recharge;
        this.behaviorParams = copyParams(behaviorParams);
        this.levelUpgrades = List.copyOf(levelUpgrades);
        this.level = level;
    }

    private static Map<String, Map<String, Double>> copyParams(
            Map<String, Map<String, Double>> params
    ) {
        Objects.requireNonNull(params, "behavior params cannot be null");
        return params.entrySet().stream().collect(
                Collectors.toUnmodifiableMap(
                        entry -> normalizeBehavior(entry.getKey()),
                        entry -> Map.copyOf(entry.getValue())
                )
        );
    }

    private static String normalizeBehavior(String behavior) {
        return behavior.strip().toUpperCase(Locale.ROOT);
    }

    public PlantSpec withLevel(int requestedLevel) {
        if (requestedLevel == level) {
            return this;
        }
        return new PlantSpec(
                id, name, category, tags, baseCost, baseHp, baseDamage,
                baseAbility, plantFoodEffect, lvl2, lvl3, lvl4,
                baseActionInterval, baseRecharge, behaviorParams,
                levelUpgrades, requestedLevel
        );
    }

    private List<PlantLevelUpgrade> activeUpgrades() {
        return levelUpgrades.stream()
                .filter(upgrade -> upgrade.targetLevel() <= level)
                .toList();
    }

    public int getBaseHp() {
        return Math.max(0, PlantLevelResolver.resolveInt(
                baseHp, activeUpgrades(), PlantUpgradeType.HEALTH_ADD));
    }

    public String getDamage() {
        return PlantLevelResolver.resolveDamage(baseDamage, activeUpgrades());
    }

    public String getBaseAbility() { return baseAbility; }
    public String getPlantFoodEffect() { return plantFoodEffect; }
    public String getLvl2() { return lvl2; }
    public String getLvl3() { return lvl3; }
    public String getLvl4() { return lvl4; }

    public double getActionInterval() {
        return PlantLevelResolver.resolveActionInterval(baseActionInterval, activeUpgrades());
    }

    public double getRecharge() {
        return Math.max(0, PlantLevelResolver.resolveDouble(
                baseRecharge, activeUpgrades(), PlantUpgradeType.RECHARGE_SECONDS_ADD));
    }

    public int getCost() {
        return Math.max(0, PlantLevelResolver.resolveInt(
                baseCost, activeUpgrades(), PlantUpgradeType.COST_ADD));
    }

    public int getOriginalBaseHp() { return baseHp; }
    public String getOriginalDamage() { return baseDamage; }
    public int getOriginalCost() { return baseCost; }
    public double getOriginalActionInterval() { return baseActionInterval; }
    public double getOriginalRecharge() { return baseRecharge; }

    public int getId() { return id; }
    public String getName() { return name; }
    public PlantCategory getCategory() { return category; }
    public Set<PlantTag> getTags() { return tags; }
    public int getLevel() { return level; }

    public PlantStackingRole getStackingRole() {
        return stackingRole;
    }

    public List<PlantLevelUpgrade> getLevelUpgrades() {
        return levelUpgrades;
    }

    public double getUpgradeValue(PlantUpgradeType type) {
        return PlantLevelResolver.cumulativeValue(activeUpgrades(), type);
    }

    public double getLatestUpgradeValue(PlantUpgradeType type, double defaultValue) {
        return PlantLevelResolver.latestValue(activeUpgrades(), type, defaultValue);
    }

    public boolean hasUpgrade(PlantUpgradeType type) {
        return activeUpgrades().stream().anyMatch(upgrade -> upgrade.type() == type);
    }

    public Map<String, Map<String, Double>> getBehaviorParams() {
        return behaviorParams;
    }

    public boolean hasBehaviorParams(String behavior) {
        return behaviorParams.containsKey(normalizeBehavior(behavior));
    }

    public Map<String, Double> behaviorParams(String behavior) {
        return behaviorParams.getOrDefault(normalizeBehavior(behavior), Map.of());
    }

    public boolean hasPlantFoodEffect() {
        return plantFoodEffect != null
                && !plantFoodEffect.isBlank()
                && !plantFoodEffect.strip().toLowerCase(Locale.ROOT).startsWith("none");
    }
}
