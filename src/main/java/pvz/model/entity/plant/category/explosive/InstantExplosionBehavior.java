package pvz.model.entity.plant.category.explosive;

import java.util.Objects;

import pvz.model.core.board.TileOverlayType;
import pvz.model.entity.plant.Plant;

final class InstantExplosionBehavior extends AbstractExplosiveBehavior {

    private final ExplosionPattern pattern;

    private final boolean meltsIceInRow;

    private final boolean leavesCrater;

    InstantExplosionBehavior(
            Plant owner,
            ExplosiveProfile profile,
            ExplosionPattern pattern,
            boolean meltsIceInRow,
            boolean leavesCrater
    ) {
        super(owner, profile);

        this.pattern = Objects.requireNonNull(
                pattern,
                "explosion pattern cannot be null"
        );

        this.meltsIceInRow = meltsIceInRow;
        this.leavesCrater = leavesCrater;
    }

    @Override
    protected void afterPlaced() {
        triggerEffect(placedTick());
    }

    @Override
    protected void applyEffect(long currentTick) {
        pattern.damageEnemyContents(
                world(),
                column(),
                row(),
                profile().explosionRadius(),
                profile().damage()
        );

        if (meltsIceInRow) {
            meltRow(currentTick);
        }

        if (leavesCrater) {
            world().board().placeCrater(
                    column(),
                    row(),
                    currentTick,
                    profile().craterDurationTicks()
            );
        }

        publishEffect("exploded.");
    }

    private void meltRow(long currentTick) {
        world().clearZombieColdEffectsInRow(row(), currentTick);

        world().board().destroyOverlaysInRow(
                row(),
                TileOverlayType.FROZEN
        );

        world().destroyFireVulnerableObstaclesInRow(row());
    }
}
