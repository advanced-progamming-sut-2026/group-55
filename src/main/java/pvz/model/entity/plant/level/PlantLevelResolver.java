package pvz.model.entity.plant.level;

import java.util.List;

public final class PlantLevelResolver {
    private PlantLevelResolver() {}

    public static int resolveInt(int baseValue, List<PlantLevelUpgrade> upgrades,
                                 PlantUpgradeType type) {
        return (int) Math.round(baseValue + cumulativeValue(upgrades, type));
    }

    public static double resolveDouble(double baseValue, List<PlantLevelUpgrade> upgrades,
                                       PlantUpgradeType type) {
        return baseValue + cumulativeValue(upgrades, type);
    }

    public static double resolveActionInterval(double baseValue, List<PlantLevelUpgrade> upgrades) {
        double seconds = baseValue + cumulativeValue(upgrades, PlantUpgradeType.ACTION_INTERVAL_SECONDS_ADD);
        double speedPercent = cumulativeValue(upgrades, PlantUpgradeType.ATTACK_SPEED_PERCENT_ADD);
        if (speedPercent != 0 && seconds > 0) {
            seconds /= (1.0 + speedPercent / 100.0);
        }
        return Math.max(0, seconds);
    }

    public static String resolveDamage(String baseDamage, List<PlantLevelUpgrade> upgrades) {
        return PlantDamageExpression.addToDamageValues(
                baseDamage,
                cumulativeValue(upgrades, PlantUpgradeType.DAMAGE_ADD)
        );
    }

    public static double cumulativeValue(List<PlantLevelUpgrade> upgrades, PlantUpgradeType type) {
        return upgrades.stream()
                .filter(upgrade -> upgrade.type() == type)
                .mapToDouble(PlantLevelUpgrade::value)
                .sum();
    }

    public static double latestValue(List<PlantLevelUpgrade> upgrades, PlantUpgradeType type,
                                     double defaultValue) {
        double result = defaultValue;
        int latestLevel = -1;
        for (PlantLevelUpgrade upgrade : upgrades) {
            if (upgrade.type() == type && upgrade.targetLevel() > latestLevel) {
                latestLevel = upgrade.targetLevel();
                result = upgrade.value();
            }
        }
        return result;
    }
}
