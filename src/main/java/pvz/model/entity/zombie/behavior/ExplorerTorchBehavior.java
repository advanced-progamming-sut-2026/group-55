package pvz.model.entity.zombie.behavior;

import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;

public final class ExplorerTorchBehavior implements ZombieBehavior {
    private boolean lit = true;

    @Override
    public void onAcceptedHit(Zombie zombie, DamageContext context) {
        if (context.projectileType() == ProjectileType.ICE) {
            setLit(false);
        } else if (context.projectileType() == ProjectileType.FIRE) {
            setLit(true);
        }
    }

    @Override
    public boolean onPlantEncounter(
            Zombie zombie,
            Plant plant,
            World world,
            long currentTick
    ) {
        if (plant.hasTag(PlantTag.ICE)) {
            setLit(false);
        } else if (plant.hasTag(PlantTag.FIRE)) {
            setLit(true);
        }
        if (!lit) {
            return false;
        }
        plant.tryRemove(PlantThreat.INSTANT_DESTROY);
        return true;
    }

    private void setLit(boolean newValue) {
        if (lit == newValue) {
            return;
        }
        lit = newValue;
        GameEvents.publish("Explorer torch is now " + (lit ? "lit." : "extinguished."));
    }
}
