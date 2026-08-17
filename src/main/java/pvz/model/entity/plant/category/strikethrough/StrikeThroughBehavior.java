package pvz.model.entity.plant.category.strikethrough;

import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.attack.ProjectileAttackController;
import pvz.model.entity.plant.attack.ShotPath;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.projectile.PlantProjectileEmitter;

public final class StrikeThroughBehavior
        extends AbstractPlantBehavior {

    private final StrikeThroughProfile profile;
    private final PlantProjectileEmitter projectileEmitter;
    private final ProjectileAttackController attackController;

    public StrikeThroughBehavior(
            Plant owner,
            PlantSpec spec
    ) {
        super(owner);

        Objects.requireNonNull(
                spec,
                "plant spec cannot be null"
        );

        this.profile = StrikeThroughProfiles.from(spec);

        this.projectileEmitter =
                new PlantProjectileEmitter(spec.getName());

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

    @Override
    protected void afterPlaced() {
        projectileEmitter.onPlaced(
                world(),
                column()
        );
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
                profile.damagePerProjectile(),
                profile.projectileType(),
                profile.rangeTiles(),
                path.vector().horizontalDirection(),
                profile.hitLimit()
        );
    }
}
