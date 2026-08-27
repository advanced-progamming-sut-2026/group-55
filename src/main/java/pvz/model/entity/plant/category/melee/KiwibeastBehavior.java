package pvz.model.entity.plant.category.melee;

import pvz.model.entity.plant.Plant;

final class KiwibeastBehavior extends AreaMeleeBehavior {

    private int minimumGrowthStage = 1;

    KiwibeastBehavior(Plant owner, MeleeProfile profile) {
        super(owner, profile);
    }

    @Override
    protected double currentDamage(long currentTick) {
        return switch (getGrowthStage(currentTick)) {
            case 1 -> profile().damage();
            case 2 -> profile().stageTwoDamage();
            default -> profile().stageThreeDamage();
        };
    }

    @Override
    public int getGrowthStage(long currentTick) {
        int timedStage;
        if (currentTick - placedTick() >= profile().stageThreeTicks()) {
            timedStage = 3;
        } else if (currentTick - placedTick() >= profile().stageTwoTicks()) {
            timedStage = 2;
        } else {
            timedStage = 1;
        }
        return Math.max(minimumGrowthStage, timedStage);
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        minimumGrowthStage = Math.max(
                minimumGrowthStage,
                profile().plantFoodGrowthStage()
        );
        super.applyPlantFood(currentTick, durationTicks);
    }
}
