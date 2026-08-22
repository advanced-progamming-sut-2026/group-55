package pvz.model.entity.plant.category.lobber;

import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.projectile.LobbedProjectile;

final class LobberBehavior
        extends AbstractPlantBehavior
        implements PlantFoodCapability {
    private final PlantSpec spec;
    private final LobberProfile profile;
    private final DoubleSupplier randomValueSupplier;

    private LobberTargetResolver targetResolver;

    LobberBehavior(
            Plant owner,
            PlantSpec spec,
            DoubleSupplier randomValueSupplier
    ) {
        super(owner);
        this.spec = Objects.requireNonNull(
                spec,
                "plant spec cannot be null"
        );
        this.profile = LobberProfiles.from(spec);
        this.randomValueSupplier = Objects.requireNonNull(
                randomValueSupplier,
                "random value supplier cannot be null"
        );
    }

    @Override
    protected void afterPlaced() {
        targetResolver = new LobberTargetResolver(
                world(),
                column(),
                row()
        );
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
        ensurePlaced();
        return targetResolver.findTarget() != null;
    }

    @Override
    public void startAction(long currentTick) {
        ensurePlaced();

        LobberTarget target = targetResolver.findTarget();

        if (target == null) {
            return;
        }

        LobberShot shot = profile.selectShot(
                randomValueSupplier.getAsDouble()
        );

        world().game().register(
                new LobbedProjectile(
                        world(),
                        spec.getName() + " lobbed projectile",
                        column(),
                        row(),
                        target,
                        shot,
                        currentTick
                )
        );
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    @Override
    public void applyPlantFood(
            long currentTick,
            long durationTicks
    ) {
        ensurePlaced();

        List<LobberTarget> targets =
                targetResolver.findPlantFoodTargets();
        LobberShot plantFoodShot = profile.plantFoodShot();

        for (LobberTarget target : targets) {
            launchPlantFoodProjectile(
                    target,
                    plantFoodShot,
                    currentTick
            );
        }
    }

    private void launchPlantFoodProjectile(
            LobberTarget target,
            LobberShot shot,
            long currentTick
    ) {
        world().game().register(
                new LobbedProjectile(
                        world(),
                        spec.getName()
                                + " plant food lobbed projectile",
                        1,
                        target.currentRow(),
                        target,
                        shot,
                        currentTick
                )
        );
    }
}
