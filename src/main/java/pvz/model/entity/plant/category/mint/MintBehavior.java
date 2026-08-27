package pvz.model.entity.plant.category.mint;

import java.util.ArrayList;
import java.util.List;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.TransientEffectCapability;
import pvz.model.entity.plant.category.explosive.TransientActionWindow;
import pvz.model.entity.plant.lifecycle.PlantThreat;

final class MintBehavior extends AbstractPlantBehavior
        implements TransientEffectCapability {

    private static final long DISPLAY_TICKS = 5;

    private final PlantCategory family;
    private final TransientActionWindow displayWindow =
            new TransientActionWindow(DISPLAY_TICKS);

    MintBehavior(Plant owner, PlantSpec spec) {
        super(owner);
        family = spec.getCategory();
    }

    @Override
    protected void afterPlaced() {
        applyFamilyPlantFood(placedTick());
        displayWindow.start(placedTick());
    }

    private void applyFamilyPlantFood(long currentTick) {
        List<Plant> snapshot = new ArrayList<>(world().getPlants());

        for (Plant plant : snapshot) {
            if (plant == owner()
                    || plant.getSpec().getCategory() != family
                    || plant.getSpec().getTags().contains(PlantTag.MINT)
                    || !plant.supportsPlantFood()) {
                continue;
            }

            plant.tryApplyPlantFood(currentTick);
        }
    }

    @Override
    public boolean isTransientEffectActive() {
        return displayWindow.isEffectActive();
    }

    @Override
    public void updateTransientEffect(long currentTick) {
        if (!displayWindow.shouldFinish(currentTick)) {
            return;
        }

        displayWindow.finish();
        owner().tryRemove(PlantThreat.TRANSIENT_EFFECT_COMPLETION);
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
        return false;
    }

    @Override
    public void startAction(long currentTick) {
    }
}
