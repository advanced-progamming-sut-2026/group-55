package pvz.model.entity.plant;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.LivingEntity;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.sun.SunValue;
import pvz.model.entity.plant.shooterprofile.ShooterProfile;
import pvz.model.entity.plant.shooterprofile.ShooterProfiles;
import pvz.model.entity.plant.sunprofile.FixedSunProfile;
import pvz.model.entity.plant.sunprofile.SunProfile;
import pvz.model.entity.plant.sunprofile.SunShroomProfile;
import pvz.model.entity.projectile.Projectile;
import pvz.model.entity.projectile.ProjectileType;

public class Plant extends LivingEntity {

    private final PlantSpec spec;

    private World world;
    private int column;
    private int row;
    private long lastActionTick;

    private int remainingShotsInBurst;
    private long nextBurstShotTick;

    private final ShooterProfile shooterProfile;

    private SunProfile sunProfile;
    private int pendingSuns;

    public Plant(PlantSpec spec) {
        this.spec = spec;
        this.shooterProfile = createShooterProfile(spec);
        this.health = spec.getBaseHp();
        this.name = spec.getName();
    }

    public void place(World world, int column, int row, long currentTick) {
        this.world = world;
        this.column = column;
        this.row = row;
        this.lastActionTick = currentTick;
        this.sunProfile = createSunProfile(currentTick);
    }
    // SunProducer
    private SunProfile createSunProfile(long plantedTick) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "sunflower" ->
                    new FixedSunProfile(SunValue.NORMALSUN.getValue(), 1,
                            SunValue.FNSUN.getValue());

            case "twin sunflower" ->
                    new FixedSunProfile(SunValue.NORMALSUN.getValue(), 2,
                            SunValue.FTSUN.getValue());

            case "primal sunflower" ->
                    new FixedSunProfile(SunValue.BIGSUN.getValue(), 1,
                            SunValue.FBSUN.getValue());

            case "sun-shroom" ->
                    new SunShroomProfile(plantedTick);

            default -> null;
        };
    }

    public boolean hasPendingSuns() {
        return pendingSuns > 0;
    }

    public void onProducedSunRemoved() {
        if (pendingSuns > 0) {
            pendingSuns--;
        }
    }

    private void updateSunProducer(long tick, long intervalTicks) {
        if (sunProfile == null) {
            return;
        }

        if (pendingSuns > 0) {
            return;
        }

        if (tick - lastActionTick < intervalTicks) {
            return;
        }

        lastActionTick = tick;

        List<Integer> drops = sunProfile.getCycleDrops(tick);

        for (int value : drops) {
            Sun sun = Sun.fromPlant(
                    world,
                    this,
                    getX(),
                    getY(),
                    value
            );

            world.addCollectible(sun);
            world.game().register(sun);
            pendingSuns++;
        }

        GameEvents.publish(
                "plant " + name + " produced "
                        + drops.size() + " sun(value: " + drops.getLast() + ") at ("
                        + column + ", " + row + ")"
        );
    }

    //Shooters
    private ShooterProfile createShooterProfile(
            PlantSpec plantSpec
    ) {
        if (plantSpec.getCategory() != PlantCategory.SHOOTER) {
            return null;
        }

        return ShooterProfiles.from(plantSpec);
    }

    private void updateShooter(
            long tick,
            long intervalTicks
    ) {
        if (shooterProfile == null) {
            return;
        }

        if (remainingShotsInBurst > 0) {
            continueBurst(tick);
            return;
        }

        if (tick - lastActionTick < intervalTicks) {
            return;
        }

        if (!hasTargetInAnyShootingLane()) {
            return;
        }

        startBurst(tick);
    }

    private boolean hasTargetInAnyShootingLane() {
        for (int laneOffset : shooterProfile.laneOffsets()) {
            int targetRow = row + laneOffset;

            if (!world.board().inBounds(column, targetRow)) {
                continue;
            }

            if (world.board().hasStraightTargetAhead(
                    targetRow,
                    getX(),
                    shooterProfile.rangeTiles()
            )) {
                return true;
            }
        }

        return false;
    }

    private void fireOneShotInAllLanes() {
        for (int laneOffset : shooterProfile.laneOffsets()) {
            int targetRow = row + laneOffset;

            if (!world.board().inBounds(column, targetRow)) {
                continue;
            }

            world.game().register(
                    new Projectile(
                            world,
                            name + " projectile",
                            column,
                            targetRow,
                            shooterProfile.damagePerProjectile(),
                            shooterProfile.projectileType(),
                            shooterProfile.rangeTiles()
                    )
            );
        }
    }

    private void startBurst(long tick) {
        lastActionTick = tick;

        fireOneShotInAllLanes();

        remainingShotsInBurst =
                shooterProfile.shotsPerLane() - 1;

        if (remainingShotsInBurst > 0) {
            nextBurstShotTick =
                    tick + shooterProfile.ticksBetweenShots();
        }
    }

    private void continueBurst(long tick) {
        if (tick < nextBurstShotTick) {
            return;
        }

        fireOneShotInAllLanes();
        remainingShotsInBurst--;

        if (remainingShotsInBurst > 0) {
            nextBurstShotTick =
                    tick + shooterProfile.ticksBetweenShots();
        }
    }

    // General
    @Override
    public double getX() {
        return tileCenter(column);
    }

    @Override
    public double getY() {
        return tileCenter(row);
    }

    public PlantSpec getSpec() {
        return spec;
    }

    public boolean hasTag(PlantTag plantTag) {
        Set<PlantTag> tags = spec.getTags();
        return tags.contains(plantTag);
    }

    @Override
    public void update(long tick) {
        if (world == null) {
            return;
        }

        long intervalTicks =
                (long) (spec.getActionInterval() * Game.TICKS_PER_SECOND);

        if (intervalTicks <= 0) {
            return;
        }

        if (spec.getCategory() == PlantCategory.SUN_PRODUCER) {
            updateSunProducer(tick, intervalTicks);
        } else if (spec.getCategory() == PlantCategory.SHOOTER) {
            updateShooter(tick, intervalTicks);
        }
    }

    @Override
    protected void onDeath() {
        if (world == null) {
            return;
        }

        world.board()
                .getTile(column, row)
                .removePlant(this);

        world.game().unregister(this);

        GameEvents.publish(
                "Plant " + name + " at ("
                        + column + ", " + row
                        + ") is destroyed."
        );
    }
}