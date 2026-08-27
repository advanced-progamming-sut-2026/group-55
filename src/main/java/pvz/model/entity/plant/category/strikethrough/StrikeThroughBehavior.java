package pvz.model.entity.plant.category.strikethrough;

import java.util.List;
import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.attack.ProjectileAttackController;
import pvz.model.entity.plant.attack.ShotPath;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.plant.projectile.PlantProjectileEmitter;
import pvz.model.entity.projectile.ProjectileHitLimit;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;

public final class StrikeThroughBehavior
        extends AbstractPlantBehavior
        implements PlantFoodCapability, StrikeThroughStateCapability {

    private final StrikeThroughProfile profile;
    private final PlantProjectileEmitter projectileEmitter;
    private final ProjectileAttackController attackController;

    private CactusStage cactusStage = CactusStage.NORMAL;

    public StrikeThroughBehavior(
            Plant owner,
            PlantSpec spec
    ) {
        super(owner);
        Objects.requireNonNull(spec, "plant spec cannot be null");

        profile = StrikeThroughProfiles.from(spec);
        projectileEmitter = new PlantProjectileEmitter(spec.getName());
        attackController = new ProjectileAttackController(
                profile,
                targetRow -> world().board().inBounds(column(), targetRow),
                this::hasTargetOnPath,
                this::fireProjectile
        );
    }

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
        attackController.updateOngoingAction(currentTick, row());
    }

    @Override
    public boolean canStartAction(long currentTick) {
        ensurePlaced();
        return attackController.canStartAction(row());
    }

    @Override
    public void startAction(long currentTick) {
        ensurePlaced();
        attackController.startAction(currentTick, row());
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    @Override
    public boolean canReceivePlantFood(long currentTick) {
        return profile.kind() != StrikeThroughKind.CACTUS
                || cactusStage == CactusStage.NORMAL;
    }

    @Override
    public void onPlantFoodStarted(long currentTick, long durationTicks) {
        attackController.cancelOngoingAction();
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        switch (profile.kind()) {
            case CACTUS -> cactusStage = CactusStage.ELECTRIFIED;
            case FUME_SHROOM -> applyFumeShroomPlantFood(currentTick);
        }
    }

    @Override
    public CactusStage getCactusStage() {
        return cactusStage;
    }

    private boolean hasTargetOnPath(
            ShotPath path,
            int targetRow
    ) {
        return world().hasStraightTarget(
                targetRow,
                owner().getX(),
                profile.rangeTiles(),
                path.vector().horizontalDirection()
        );
    }

    private void fireProjectile(
            ShotPath path,
            int targetRow
    ) {
        projectileEmitter.emit(
                targetRow,
                0,
                currentProjectileDamage(),
                currentProjectileType(),
                profile.rangeTiles(),
                path.vector().horizontalDirection(),
                currentHitLimit()
        );
    }

    private double currentProjectileDamage() {
        if (profile.kind() == StrikeThroughKind.CACTUS
                && cactusStage == CactusStage.ELECTRIFIED) {
            return profile.damagePerProjectile()
                    * profile.plantFoodDamageMultiplier();
        }
        return profile.damagePerProjectile();
    }

    private ProjectileType currentProjectileType() {
        if (profile.kind() == StrikeThroughKind.CACTUS
                && cactusStage == CactusStage.ELECTRIFIED) {
            return ProjectileType.ELECTRIC;
        }
        return profile.projectileType();
    }

    private ProjectileHitLimit currentHitLimit() {
        if (profile.kind() == StrikeThroughKind.CACTUS
                && cactusStage == CactusStage.ELECTRIFIED) {
            return ProjectileHitLimit.unlimited();
        }
        return profile.hitLimit();
    }

    private void applyFumeShroomPlantFood(long currentTick) {
        List<Zombie> targets = List.copyOf(world().getHostileZombies());
        for (Zombie zombie : targets) {
            if (!isFumePlantFoodTarget(zombie)) {
                continue;
            }

            boolean accepted = zombie.takeAbilityDamage(
                    profile.plantFoodDamage(),
                    DamageContext.AttackDelivery.STRAIGHT,
                    DamageContext.ImpactMode.AREA
            );

            if (accepted && !zombie.isDead()) {
                zombie.knockBackTowardSpawn(
                        profile.plantFoodKnockbackTiles()
                );
            }
        }
    }

    private boolean isFumePlantFoodTarget(Zombie zombie) {
        return !zombie.isDead()
                && zombie.getRow() == row()
                && zombie.getX() >= owner().getX();
    }
}
