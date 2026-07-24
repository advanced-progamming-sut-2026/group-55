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
import pvz.model.entity.plant.shooterprofile.StraightShotPath;
import pvz.model.entity.plant.sunprofile.FixedSunProfile;
import pvz.model.entity.plant.sunprofile.SunProfile;
import pvz.model.entity.plant.sunprofile.SunShroomProfile;
import pvz.model.entity.projectile.Projectile;

public class Plant extends LivingEntity {

    private final PlantSpec spec;

    private World world;
    private int column;
    private int row;
    private long lastActionTick;

    //lifetime
    private final PlantLifetimeProfile lifetimeProfile;

    private long expirationTick = Long.MAX_VALUE;
    private boolean removedFromWorld;

    //shooter
    private boolean burstActive;
    private int nextBurstStep;
    private long nextBurstShotTick;

    private final ShooterProfile shooterProfile;
    //sun
    private SunProfile sunProfile;
    private int pendingSuns;

    public Plant(PlantSpec spec) {
        this.spec = spec;
        this.shooterProfile = createShooterProfile(spec);

        this.lifetimeProfile = PlantLifetimeProfiles.from(spec);

        this.health = spec.getBaseHp();
        this.name = spec.getName();
    }

    public void place(World world, int column, int row, long currentTick) {
        this.world = world;
        this.column = column;
        this.row = row;
        this.lastActionTick = currentTick;
        this.sunProfile = createSunProfile(currentTick);

        resetLifetime(currentTick);
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

    private void updateShooter(long tick, long intervalTicks) {
        if (shooterProfile == null) {
            return;
        }

        if (!shouldHandleShooterUpdate()) {
            return;
        }

        if (burstActive) {
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
        for (StraightShotPath path : shooterProfile.shotPaths()) {

            int targetRow = row + path.laneOffset();

            if (!world.board().inBounds(column, targetRow)) {
                continue;
            }

            if (world.board().hasStraightTarget(
                    targetRow,
                    getX(),
                    shooterProfile.rangeTiles(),
                    path.direction()
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldHandleShooterUpdate() {
        if (!isPeaPod()) {
            return true;
        }

        List<Plant> peaPods = getPeaPodsInOwnTile();

        return !peaPods.isEmpty()
                && peaPods.getFirst() == this;
    }

    private int getProjectileCopiesForVolley() {
        if (!isPeaPod()) {
            return 1;
        }

        return getPeaPodsInOwnTile().size();
    }

    private List<Plant> getPeaPodsInOwnTile() {
        return world.board()
                .getTile(column, row)
                .getPlants()
                .stream()
                .filter(plant -> plant.getName()
                        .equalsIgnoreCase("Pea Pod"))
                .toList();
    }

    private boolean isPeaPod() {
        return name.equalsIgnoreCase("Pea Pod");
    }

    private void fireBurstStep(int burstStep) {
        int projectileCopies = getProjectileCopiesForVolley();

        for (StraightShotPath path : shooterProfile.shotPaths()) {

            if (burstStep >= path.shotsPerVolley()) {
                continue;
            }

            int targetRow = row + path.laneOffset();

            if (!world.board().inBounds(column, targetRow)) {
                continue;
            }

            fireProjectileCopies(path, targetRow, projectileCopies);
        }
    }

    private void fireProjectileCopies(
            StraightShotPath path,
            int targetRow,
            int projectileCopies
    ) {
        for (int copy = 0; copy < projectileCopies; copy++) {
            world.game().register(
                    new Projectile(
                            world,
                            name + " projectile",
                            column,
                            targetRow,
                            shooterProfile.damagePerProjectile(),
                            shooterProfile.projectileType(),
                            shooterProfile.rangeTiles(),
                            path.direction()
                    )
            );
        }
    }

    private void startBurst(long tick) {
        lastActionTick = tick;

        fireBurstStep(0);

        if (shooterProfile.burstLength() <= 1) {
            burstActive = false;
            return;
        }

        burstActive = true;
        nextBurstStep = 1;
        nextBurstShotTick = tick + shooterProfile.ticksBetweenShots();
    }

    private void continueBurst(long tick) {
        if (tick < nextBurstShotTick) {
            return;
        }

        fireBurstStep(nextBurstStep);
        nextBurstStep++;

        if (nextBurstStep >= shooterProfile.burstLength()) {
            burstActive = false;
            return;
        }

        nextBurstShotTick = tick + shooterProfile.ticksBetweenShots();
    }

    //Lifetime
    public void resetLifetime(long currentTick) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        if (lifetimeProfile == null) {
            expirationTick = Long.MAX_VALUE;
            return;
        }

        expirationTick =
                currentTick
                        + lifetimeProfile.lifespanTicks();
    }

    private boolean expireIfNeeded(long tick) {
        if (lifetimeProfile == null) {
            return false;
        }

        if (tick < expirationTick) {
            return false;
        }

        expire();
        return true;
    }

    private void expire() {
        health = 0;

        removeFromWorld("Plant " + name + " at (" + column + ", " + row + ") expired.");
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

    private void removeFromWorld(String message) {
        if (world == null || removedFromWorld) {
            return;
        }

        removedFromWorld = true;

        world.board()
                .getTile(column, row)
                .removePlant(this);

        world.game().unregister(this);

        GameEvents.publish(message);
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

        if (expireIfNeeded(tick)) {
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
        removeFromWorld("Plant " + name + " at (" + column + ", " + row + ") is destroyed.");
    }
}