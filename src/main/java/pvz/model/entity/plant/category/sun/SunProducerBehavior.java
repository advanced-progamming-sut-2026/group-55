package pvz.model.entity.plant.category.sun;

import java.util.List;
import java.util.Objects;

import pvz.model.core.GameEvents;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.category.explosive.TransientActionWindow;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.plant.level.PlantUpgradeType;
import pvz.model.entity.plant.plantfood.PlantFoodVolley;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.plant.behavior.capability.SunProductionCapability;
import pvz.model.entity.plant.behavior.capability.TransientEffectCapability;
import pvz.model.entity.plant.behavior.capability.ZombieEdibilityCapability;

public final class SunProducerBehavior
        extends AbstractPlantBehavior
        implements PlantFoodCapability,
        SunProductionCapability,
        ZombieEdibilityCapability,
        TransientEffectCapability {

    private static final String SUN_BURST_BEHAVIOR = "SUN_BURST";

    private static final long DEFAULT_EFFECT_DISPLAY_TICKS = 5;

    private final PlantSpec spec;

    private SunProfile profile;
    private int pendingSuns;
    private boolean singleUseProductionDone;
    private TransientActionWindow effectWindow;

    public SunProducerBehavior(Plant owner, PlantSpec spec) {
        super(owner);

        this.spec = Objects.requireNonNull(spec, "plant spec cannot be null");
    }

    @Override
    public boolean supportsPlantFood() {
        return SunProfiles.supportsPlantFood(spec);
    }

    @Override
    protected void afterPlaced() {
        profile = SunProfiles.from(spec, placedTick());

        if (profile.activatesImmediatelyAfterPlacement()) {
            produceCycle(placedTick());
        }
    }

    @Override
    public boolean hasOngoingAction() {
        return effectWindow != null && effectWindow.isEffectActive();
    }

    @Override
    public void updateOngoingAction(long currentTick) {
        updateTransientEffect(currentTick);
    }

    @Override
    public boolean isTransientEffectActive() {
        return effectWindow != null && effectWindow.isEffectActive();
    }

    @Override
    public void updateTransientEffect(long currentTick) {
        if (!isTransientEffectActive()) {
            return;
        }

        if (effectWindow.shouldFinish(currentTick)) {
            effectWindow.finish();
            owner().tryRemove(
                    PlantThreat.TRANSIENT_EFFECT_COMPLETION
            );
        }
    }

    @Override
    public boolean canBeEatenByZombie() {
        return effectWindow == null || !effectWindow.isEffectActive();
    }

    @Override
    public boolean canStartAction(long currentTick) {
        ensurePlaced();

        return pendingSuns == 0 && !singleUseProductionDone;
    }

    @Override
    public void startAction(long currentTick) {
        ensurePlaced();

        if (singleUseProductionDone) {
            return;
        }

        produceCycle(currentTick);
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

    private void produceCycle(long currentTick) {
        List<Integer> drops = profile.getCycleDrops(currentTick);

        double doubleChance = spec.getUpgradeValue(
                PlantUpgradeType.SUN_DOUBLE_CHANCE_ADD
        );
        if (!drops.isEmpty() && doubleChance > 0 && world().rollChance(doubleChance)) {
            java.util.ArrayList<Integer> doubled = new java.util.ArrayList<>(drops);
            doubled.addAll(drops);
            drops = List.copyOf(doubled);
        }

        if (drops.isEmpty()) {
            return;
        }

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

        if (!profile.removesProducerAfterProduction()) {
            return;
        }

        singleUseProductionDone = true;

        effectWindow = new TransientActionWindow(effectDisplayTicks());
        effectWindow.start(currentTick);
    }

    private long effectDisplayTicks() {
        long baseTicks = spec.behaviorParams(SUN_BURST_BEHAVIOR)
                .getOrDefault(
                        "effectDisplayTicks",
                        (double) DEFAULT_EFFECT_DISPLAY_TICKS
                )
                .longValue();
        return Math.max(1, baseTicks);
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
        ProducedSunSpawner.spawnAtProducer(
                world(),
                owner(),
                value
        );

        pendingSuns++;
    }
}
