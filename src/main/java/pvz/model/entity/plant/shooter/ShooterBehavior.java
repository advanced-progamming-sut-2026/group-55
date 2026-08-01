package pvz.model.entity.plant.shooter;

import java.util.Objects;
import java.util.List;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.plant.plantfood.PlantFoodVolley;
import pvz.model.entity.plant.projectile.PlantProjectileEmitter;

public final class ShooterBehavior
        extends AbstractPlantBehavior
        implements PlantFoodCapability {

    private static final double PLANT_FOOD_PROJECTILE_SPACING_TILES = 1.0 / 5.0;

    private static final String PEA_POD_NAME = "Pea Pod";

    private final PlantSpec spec;
    private final ShooterProfile profile;
    private final PlantProjectileEmitter projectileEmitter;
    private final ShooterPlantFoodProfile plantFoodProfile;

    private boolean burstActive;
    private int nextBurstStep;
    private long nextBurstShotTick;

    public ShooterBehavior(Plant owner, PlantSpec spec) {
        super(owner);

        this.spec = Objects.requireNonNull(spec, "plant spec cannot be null");

        this.profile = ShooterProfiles.from(spec);

        this.plantFoodProfile = ShooterPlantFoodProfiles.from(spec);

        this.projectileEmitter = new PlantProjectileEmitter(spec.getName());
    }

    @Override
    public boolean supportsPlantFood() {
        return plantFoodProfile != null;
    }

    @Override
    public long requestedDurationTicks() {
        if (plantFoodProfile == null) {
            throw new IllegalStateException(
                    spec.getName() + " does not have a shooter plant food profile"
            );
        }

        return plantFoodProfile.durationTicks();
    }

    @Override
    protected void afterPlaced() {
        projectileEmitter.onPlaced(world(), column());
    }

    @Override
    public boolean hasOngoingAction() {
        return burstActive;
    }

    @Override
    public void updateOngoingAction(long currentTick) {
        ensurePlaced();

        continueBurst(currentTick);
    }

    @Override
    public boolean canStartAction(long currentTick) {
        ensurePlaced();

        return hasTargetInAnyShootingLane();
    }

    @Override
    public void startAction(long currentTick) {
        ensurePlaced();

        fireBurstStep(0);

        if (profile.burstLength() <= 1) {
            burstActive = false;
            return;
        }

        burstActive = true;
        nextBurstStep = 1;
        nextBurstShotTick = currentTick + profile.ticksBetweenShots();
    }

    @Override
    public void onPlantFoodStarted(long currentTick, long durationTicks) {
        ensurePlaced();

        burstActive = false;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        if (plantFoodProfile == null) {
            throw new IllegalStateException(
                    spec.getName() + " does not have a shooter plant food profile"
            );
        }

        if (!shouldEmitPlantFoodProjectiles()) {
            return;
        }

        int totalSteps = plantFoodProfile.stepCount(durationTicks);

        PlantFoodVolley.start(
                world().game(),
                currentTick,
                totalSteps,
                plantFoodProfile.ticksBetweenSteps(),
                () -> !owner().isRemovedFromWorld(),
                step -> firePlantFoodStep(
                        plantFoodProfile,
                        step,
                        totalSteps
                )
        );
    }

    private boolean shouldEmitPlantFoodProjectiles() {
        if (!spec.getName().equalsIgnoreCase(PEA_POD_NAME)) {
            return true;
        }

        List<Plant> plants = world().board().getTile(column(), row()).getPlants();

        for (int index = plants.size() - 1; index >= 0; index--) {

            Plant plant = plants.get(index);

            if (plant.getName().equalsIgnoreCase(PEA_POD_NAME)) {
                return plant == owner();
            }
        }

        return false;
    }

    private boolean hasTargetInAnyShootingLane() {
        for (StraightShotPath path : profile.shotPaths()) {
            int targetRow = row() + path.laneOffset();

            if (!world().board().inBounds(column(), targetRow)) {
                continue;
            }

            if (world().board().hasStraightTarget(
                    targetRow,
                    owner().getX(),
                    profile.rangeTiles(),
                    path.direction()
            )) {
                return true;
            }
        }

        return false;
    }

    private void fireBurstStep(int burstStep) {
        for (StraightShotPath path : profile.shotPaths()) {
            if (burstStep >= path.shotsPerVolley()) {
                continue;
            }

            int targetRow = row() + path.laneOffset();

            if (!world().board().inBounds(column(), targetRow)) {
                continue;
            }

            projectileEmitter.emit(
                    targetRow,
                    0,
                    profile.damagePerProjectile(),
                    profile.projectileType(),
                    profile.rangeTiles(),
                    path.direction()
            );
        }
    }

    private void continueBurst(long currentTick) {
        if (currentTick < nextBurstShotTick) {
            return;
        }

        fireBurstStep(nextBurstStep);

        nextBurstStep++;

        if (nextBurstStep >= profile.burstLength()) {
            burstActive = false;
            return;
        }

        nextBurstShotTick = currentTick + profile.ticksBetweenShots();
    }

    private void firePlantFoodStep(ShooterPlantFoodProfile plantFoodProfile,
            int volleyStep,
            int totalSteps
    ) {
        for (PlantFoodShotPath path : plantFoodProfile.shotPaths()) {
            int targetRow = row() + path.laneOffset();

            if (!world().board().inBounds(column(), targetRow)) {
                continue;
            }

            int projectileCopies = plantFoodProfile.shotsAtStep(path, volleyStep, totalSteps);

            firePlantFoodProjectileCopies(
                    plantFoodProfile,
                    path,
                    targetRow,
                    projectileCopies
            );
        }
    }

    private void firePlantFoodProjectileCopies(
            ShooterPlantFoodProfile plantFoodProfile,
            PlantFoodShotPath path,
            int targetRow,
            int projectileCopies
    ) {
        for (int copy = 0; copy < projectileCopies; copy++) {
            double spawnOffsetX = copy * PLANT_FOOD_PROJECTILE_SPACING_TILES * path.direction().sign();

            projectileEmitter.emit(
                    targetRow,
                    spawnOffsetX,
                    plantFoodProfile.damagePerProjectile(),
                    plantFoodProfile.projectileType(),
                    plantFoodProfile.rangeTiles(),
                    path.direction()
            );
        }
    }
}
