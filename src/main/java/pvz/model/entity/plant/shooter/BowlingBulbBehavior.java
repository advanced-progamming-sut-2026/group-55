package pvz.model.entity.plant.shooter;

import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.projectile.BowlingBulbProjectile;
import pvz.model.entity.projectile.ProjectileType;

public final class BowlingBulbBehavior
        extends AbstractPlantBehavior {

    private final PlantSpec spec;
    private final BowlingBulbProfile profile;
    private final long[] availableAtTick;

    private int nextBulbIndex;

    public BowlingBulbBehavior(
            Plant owner,
            PlantSpec spec
    ) {
        super(owner);

        this.spec = Objects.requireNonNull(
                spec,
                "plant spec cannot be null"
        );

        this.profile = BowlingBulbProfile.from(spec);

        this.availableAtTick =
                new long[profile.bulbs().size()];
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

        return findAvailableBulb(currentTick) >= 0
                && world().board().hasStraightTargetAhead(
                        row(),
                        owner().getX()
                );
    }

    @Override
    public void startAction(long currentTick) {
        ensurePlaced();

        int bulbIndex =
                findAvailableBulb(currentTick);

        if (bulbIndex < 0) {
            return;
        }

        BowlingBulbProfile.Bulb bulb =
                profile.bulbs().get(bulbIndex);

        world().game().register(
                new BowlingBulbProjectile(
                        world(),
                        spec.getName()
                                + " "
                                + bulb.name()
                                + " projectile",
                        column(),
                        row(),
                        bulb.damage(),
                        ProjectileType.NORMAL
                )
        );

        availableAtTick[bulbIndex] =
                currentTick + bulb.rechargeTicks();

        nextBulbIndex =
                (bulbIndex + 1)
                        % profile.bulbs().size();
    }

    private int findAvailableBulb(
            long currentTick
    ) {
        for (int offset = 0;
             offset < profile.bulbs().size();
             offset++) {

            int index =
                    (nextBulbIndex + offset)
                            % profile.bulbs().size();

            if (currentTick >= availableAtTick[index]) {
                return index;
            }
        }

        return -1;
    }
}
