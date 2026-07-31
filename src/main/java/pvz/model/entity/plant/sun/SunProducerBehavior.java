package pvz.model.entity.plant.sun;

import java.util.List;
import java.util.Objects;

import pvz.model.core.GameEvents;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.plantfood.PlantFoodVolley;

public final class SunProducerBehavior
        extends AbstractPlantBehavior {

    private final PlantSpec spec;

    private SunProfile profile;
    private int pendingSuns;

    public SunProducerBehavior(Plant owner, PlantSpec spec) {
        super(owner);

        this.spec = Objects.requireNonNull(spec, "plant spec cannot be null");
    }

    @Override
    protected void afterPlaced() {
        profile = SunProfiles.from(spec, placedTick());
    }

    @Override
    public boolean hasOngoingAction() {
        return false;
    }

    @Override
    public void updateOngoingAction(long currentTick) {
    }

    @Override
    public boolean canStartAction(long currentTick) {
        ensurePlaced();

        return pendingSuns == 0;
    }

    @Override
    public void startAction(long currentTick) {
        ensurePlaced();

        List<Integer> drops = profile.getCycleDrops(currentTick);

        spawnProducedSuns(drops);

        GameEvents.publish(
                "plant "
                        + owner().getName()
                        + " produced "
                        + drops.size()
                        + " sun(value: "
                        + drops.getLast()
                        + ") at ("
                        + column()
                        + ", "
                        + row()
                        + ")"
        );
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        List<Integer> drops = profile.getPlantFoodDrops(currentTick);

        schedulePlantFoodSuns(drops, currentTick, durationTicks);
    }

    @Override
    public boolean hasPendingSuns() {
        return pendingSuns > 0;
    }

    @Override
    public void onProducedSunRemoved() {
        if (pendingSuns > 0) {
            pendingSuns--;
        }
    }

    private void schedulePlantFoodSuns(
            List<Integer> drops,
            long currentTick,
            long durationTicks
    ) {
        if (drops.isEmpty()) {
            return;
        }

        int totalSteps = Math.toIntExact(durationTicks);

        PlantFoodVolley.start(
                world().game(),
                currentTick,
                totalSteps,
                1,
                () -> !owner().isRemovedFromWorld(),
                step -> spawnPlantFoodSunsForStep(drops, step, totalSteps)
        );
    }

    private void spawnPlantFoodSunsForStep(
            List<Integer> drops,
            int currentStep,
            int totalSteps
    ) {
        for (int dropIndex = 0; dropIndex < drops.size(); dropIndex++) {
            int scheduledStep = scheduledStepForDrop(dropIndex, drops.size(), totalSteps);

            if (scheduledStep != currentStep) {
                continue;
            }

            spawnProducedSun(drops.get(dropIndex));
        }
    }

    private int scheduledStepForDrop(
            int dropIndex,
            int dropCount,
            int totalSteps
    ) {
        if (dropCount == 1) {
            return 0;
        }

        return (int) ((long) dropIndex * (totalSteps - 1) / (dropCount - 1));
    }

    private void spawnProducedSuns(List<Integer> drops) {
        for (int value : drops) {
            spawnProducedSun(value);
        }
    }

    private void spawnProducedSun(int value) {
        Sun sun = Sun.fromPlant(
                world(),
                owner(),
                owner().getX(),
                owner().getY(),
                value
        );

        world().addCollectible(sun);
        world().game().register(sun);

        pendingSuns++;
    }
}
