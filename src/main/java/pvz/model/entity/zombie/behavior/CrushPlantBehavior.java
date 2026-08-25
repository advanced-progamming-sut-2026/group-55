package pvz.model.entity.zombie.behavior;

import java.util.Locale;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.lifecycle.PlantRemovalResult;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.zombie.Zombie;

public final class CrushPlantBehavior implements ZombieBehavior {
    private final String requiredArmorId;

    public CrushPlantBehavior(String requiredArmorId) {
        this.requiredArmorId = requiredArmorId == null
                ? null
                : requiredArmorId.strip().toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean onPlantEncounter(
            Zombie zombie,
            Plant plant,
            World world,
            long currentTick
    ) {
        if (requiredArmorId != null
                && !zombie.getArmorSet()
                .hasIntactArmor(requiredArmorId)) {
            return false;
        }

        PlantRemovalResult result = plant.tryRemove(PlantThreat.INSTANT_DESTROY);
        if (result == PlantRemovalResult.REMOVED) {
            GameEvents.publish(
                    zombie.getName() + " crushed " + plant.getName()
                            + " at (" + plant.getTileX() + ", "
                            + plant.getTileY() + ")."
            );
        }
        return true;
    }
}
