package pvz.model.entity.zombie.behavior;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.zombie.Zombie;

public final class TurquoiseLaserBehavior implements ZombieBehavior {
    private final int detectionRadiusTiles;
    private final int laserRangeTiles;
    private final int sunPerSecond;
    private final long chargingTicks;
    private final long cooldownTicks;
    private final double sunDropRatio;

    private boolean charging;
    private long chargeEndTick;
    private long nextStealTick;
    private long nextReadyTick;
    private int stolenSun;

    public TurquoiseLaserBehavior(
            int detectionRadiusTiles,
            int laserRangeTiles,
            int sunPerSecond,
            double chargingSeconds,
            double cooldownSeconds,
            double sunDropRatio
    ) {
        this.detectionRadiusTiles = detectionRadiusTiles;
        this.laserRangeTiles = laserRangeTiles;
        this.sunPerSecond = sunPerSecond;
        this.chargingTicks = secondsToTicks(chargingSeconds);
        this.cooldownTicks = secondsToTicks(cooldownSeconds);
        this.sunDropRatio = sunDropRatio;
    }

    @Override
    public void onTick(Zombie zombie, World world, long currentTick) {
        if (!charging) {
            if (currentTick >= nextReadyTick
                    && world.hasTargetablePlantInRadius(
                            zombie.getTileX(),
                            zombie.getRow(),
                            detectionRadiusTiles
                    )) {
                charging = true;
                chargeEndTick = currentTick + chargingTicks;
                nextStealTick = currentTick + Game.TICKS_PER_SECOND;
                GameEvents.publish("Turquoise started charging its laser.");
            }
            return;
        }

        while (currentTick >= nextStealTick && nextStealTick <= chargeEndTick) {
            if (world.sunBank().spend(sunPerSecond)) {
                stolenSun += sunPerSecond;
            } else {
                int remaining = world.sunBank().getBalance();
                if (remaining > 0) {
                    world.sunBank().spend(remaining);
                    stolenSun += remaining;
                }
            }
            nextStealTick += Game.TICKS_PER_SECOND;
        }

        if (currentTick >= chargeEndTick) {
            fire(zombie, world);
            charging = false;
            nextReadyTick = currentTick + cooldownTicks;
        }
    }

    @Override
    public void onHardStopTick(
            Zombie zombie,
            World world,
            long currentTick
    ) {
        if (charging) {
            chargeEndTick++;
            nextStealTick++;
        }
        if (nextReadyTick > currentTick) {
            nextReadyTick++;
        }
    }

    private void fire(Zombie zombie, World world) {
        int minimumColumn = Math.max(
                1,
                zombie.getTileX() - laserRangeTiles
        );
        for (Plant plant : world.getPlants()) {
            if (plant.getTileY() == zombie.getRow()
                    && plant.getTileX() < zombie.getTileX()
                    && plant.getTileX() >= minimumColumn) {
                plant.tryRemove(PlantThreat.INSTANT_DESTROY);
            }
        }
        GameEvents.publish(
                "Turquoise fired its " + laserRangeTiles + "-tile laser."
        );
    }

    @Override
    public void onDeath(Zombie zombie, World world, long currentTick) {
        int dropped = (int) Math.floor(stolenSun * sunDropRatio);
        world.dropRecoveredSun(dropped, zombie.getX(), zombie.getY());
        if (dropped > 0) {
            GameEvents.publish("Turquoise dropped " + dropped + " stolen sun.");
        }
    }

    private long secondsToTicks(double seconds) {
        return Math.max(1, (long) Math.ceil(seconds * Game.TICKS_PER_SECOND));
    }
}
