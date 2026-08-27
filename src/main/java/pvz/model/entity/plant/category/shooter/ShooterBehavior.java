package pvz.model.entity.plant.category.shooter;

import java.util.List;
import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.attack.ProjectileAttackController;
import pvz.model.entity.plant.attack.ShotPath;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.plant.plantfood.PlantFoodVolley;
import pvz.model.entity.plant.projectile.PlantProjectileEmitter;
import pvz.model.entity.plant.category.shooter.plantfood.PlantFoodShotPath;
import pvz.model.entity.plant.category.shooter.plantfood.ShooterPlantFoodPhase;
import pvz.model.entity.plant.category.shooter.plantfood.ShooterPlantFoodProfile;
import pvz.model.entity.plant.category.shooter.plantfood.ShooterPlantFoodProfiles;
import pvz.model.entity.plant.category.shooter.plantfood.ShooterPlantFoodStartEffect;

public class ShooterBehavior
        extends AbstractPlantBehavior
        implements PlantFoodCapability {

    private static final double PLANT_FOOD_PROJECTILE_SPACING_TILES = 1.0 / 5.0;
    private static final long SNOW_PEA_FREEZE_DURATION_TICKS =
            3L * Game.TICKS_PER_SECOND;
    private static final String PEA_POD_NAME = "Pea Pod";

    private final PlantSpec spec;
    private final ShooterProfile profile;
    private final PlantProjectileEmitter projectileEmitter;
    private final ProjectileAttackController attackController;
    private final ShooterPlantFoodProfile plantFoodProfile;

    public ShooterBehavior(Plant owner, PlantSpec spec) {
        super(owner);

        this.spec = Objects.requireNonNull(spec, "plant spec cannot be null");

        this.profile = ShooterProfiles.from(spec);

        this.plantFoodProfile = ShooterPlantFoodProfiles.from(spec);

        this.projectileEmitter = new PlantProjectileEmitter(spec);

        this.attackController = new ProjectileAttackController(
                profile,
                targetRow -> world().board().inBounds(
                        column(),
                        targetRow
                ),
                this::hasTargetOnPath,
                this::fireProjectile
        );
    }

    ///* plantBehavior
    @Override
    protected void afterPlaced() {
        projectileEmitter.onPlaced(world(), column());
    }

    @Override
    public boolean hasOngoingAction() {
        return attackController.hasOngoingAction();
    }

    @Override
    public void updateOngoingAction(long currentTick) {
        ensurePlaced();

        attackController.updateOngoingAction(
                currentTick,
                row()
        );
    }

    @Override
    public boolean canStartAction(long currentTick) {
        ensurePlaced();

        return attackController.canStartAction(row());
    }

    @Override
    public void startAction(long currentTick) {
        ensurePlaced();

        attackController.startAction(
                currentTick,
                row()
        );
    }

    ///* plantFoodBehavior

    @Override
    public boolean supportsPlantFood() {
        return plantFoodProfile != null;
    }

    @Override
    public boolean targetsMatchingPlantsOnBoard() {
        return plantFoodProfile != null
                && plantFoodProfile.targetsMatchingPlantsOnBoard();
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
    public void onPlantFoodStarted(long currentTick, long durationTicks) {
        ensurePlaced();

        attackController.cancelOngoingAction();
        applyPlantFoodStartEffect(currentTick);
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

        for (ShooterPlantFoodPhase phase
                : plantFoodProfile.phases()) {
            startPlantFoodPhase(currentTick, phase);
        }
    }

    /// //////////////////////////////////////////
    private void applyPlantFoodStartEffect(
            long currentTick
    ) {
        ShooterPlantFoodStartEffect startEffect =
                plantFoodProfile.startEffect();

        switch (startEffect) {
            case NONE -> {
            }
            case FREEZE_OWNER_LANE ->
                    world().board().freezeZombiesInRow(
                            world().getHostileZombies(),
                            row(),
                            currentTick,
                            SNOW_PEA_FREEZE_DURATION_TICKS
                    );
        }
    }

    private void startPlantFoodPhase(
            long currentTick,
            ShooterPlantFoodPhase phase
    ) {
        int totalSteps = phase.stepCount();

        PlantFoodVolley.startAfterDelay(
                world().game(),
                currentTick,
                phase.startDelayTicks(),
                totalSteps,
                phase.ticksBetweenSteps(),
                () -> !owner().isRemovedFromWorld(),
                step -> firePlantFoodStep(
                        phase,
                        step,
                        totalSteps
                )
        );
    }

    private boolean shouldEmitPlantFoodProjectiles() {
        if (!spec.getName().equalsIgnoreCase(PEA_POD_NAME)) {
            return true;
        }

        List<Plant> plants = world()
                .board()
                .getTile(column(), row())
                .getPlants();

        for (int index = plants.size() - 1; index >= 0; index--) {
            Plant plant = plants.get(index);

            if (plant.getName().equalsIgnoreCase(PEA_POD_NAME)) {
                return plant == owner();
            }
        }

        return false;
    }

    private boolean hasTargetOnPath(
            ShotPath path,
            int targetRow
    ) {
        if (path.vector().isHorizontal()) {
            return world().hasStraightTarget(
                    targetRow,
                    owner().getX(),
                    profile.rangeTiles(),
                    path.vector().horizontalDirection()
            );
        }

        return world().hasDirectionalTarget(
                column(),
                targetRow,
                profile.rangeTiles(),
                path.vector()
        );
    }

    private void fireProjectile(
            ShotPath path,
            int targetRow
    ) {
        if (path.vector().isHorizontal()) {
            projectileEmitter.emit(
                    targetRow,
                    0,
                    profile.damagePerProjectile(),
                    profile.projectileType(),
                    profile.rangeTiles(),
                    path.vector().horizontalDirection()
            );
            return;
        }

        projectileEmitter.emitDirectional(
                targetRow,
                profile.damagePerProjectile(),
                profile.projectileType(),
                profile.rangeTiles(),
                path.vector()
        );
    }

    private void firePlantFoodStep(ShooterPlantFoodPhase phase,
            int volleyStep,
            int totalSteps
    ) {
        for (PlantFoodShotPath path : phase.shotPaths()) {
            int targetRow = row() + path.laneOffset();

            if (!world().board().inBounds(column(), targetRow)) {
                continue;
            }

            int projectileCopies = phase.shotsAtStep(
                    path,
                    volleyStep,
                    totalSteps
            );

            firePlantFoodProjectileCopies(
                    phase,
                    path,
                    targetRow,
                    projectileCopies
            );
        }
    }

    private void firePlantFoodProjectileCopies(
            ShooterPlantFoodPhase phase,
            PlantFoodShotPath path,
            int targetRow,
            int projectileCopies
    ) {
        ShotVector vector = path.vector();

        for (int copy = 0; copy < projectileCopies; copy++) {
            double spawnOffset =
                    copy * PLANT_FOOD_PROJECTILE_SPACING_TILES;

            if (vector.isHorizontal()) {
                projectileEmitter.emit(
                        targetRow,
                        spawnOffset * vector.unitColumnStep(),
                        phase.damagePerProjectile(),
                        phase.projectileType(),
                        phase.rangeTiles(),
                        vector.horizontalDirection(),
                        phase.hitLimit()
                );
                continue;
            }

            projectileEmitter.emitDirectional(
                    targetRow,
                    spawnOffset,
                    phase.damagePerProjectile(),
                    phase.projectileType(),
                    phase.rangeTiles(),
                    vector,
                    phase.hitLimit()
            );
        }
    }
}
