package pvz.model.entity.plant.category.explosive;

import java.util.Map;
import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.PassivePlantBehavior;
import pvz.model.entity.plant.behavior.PlantBehavior;

public final class ExplosiveBehaviorFactory {

    private ExplosiveBehaviorFactory() {
    }

    public static PlantBehavior create(Plant owner, PlantSpec spec) {
        Objects.requireNonNull(owner, "owner plant cannot be null");
        Objects.requireNonNull(spec, "plant spec cannot be null");

        ExplosiveProfile profile = ExplosiveProfiles.from(spec);

        if (profile == null) {
            return new PassivePlantBehavior(owner);
        }

        return switch (profile.kind()) {
            case MINE -> new ContactMineBehavior(owner, profile);

            case INSTANT_BLAST -> blast(
                    owner,
                    profile,
                    ExplosionPattern.AREA,
                    false,
                    false
            );

            case ROW_BLAST -> blast(
                    owner,
                    profile,
                    ExplosionPattern.ROW,
                    true,
                    false
            );

            case GLOBAL_BLAST -> blast(
                    owner,
                    profile,
                    ExplosionPattern.LAWN,
                    false,
                    true
            );

            case SQUASH -> new SquashBehavior(owner, profile);

            case GRAPESHOT -> new GrapeshotBehavior(owner, profile);

            case TANGLE_KELP -> new TangleKelpBehavior(owner, profile);

            case FREEZE_TRAP -> new IcebergLettuceBehavior(owner, profile);

            case GLOBAL_FREEZE -> new IceShroomBehavior(owner, profile);

            case TILE_MELT -> new HotPotatoBehavior(owner, profile);

            case TOMBSTONE_DESTROY ->
                    new GraveBusterBehavior(owner, profile);
        };
    }

    public static PlantBehavior createMineBehavior(
            Plant owner,
            double damage,
            Map<String, Double> params
    ) {
        Objects.requireNonNull(owner, "owner plant cannot be null");
        Objects.requireNonNull(params, "mine params cannot be null");

        return new ContactMineBehavior(
                owner,
                new ExplosiveProfile(ExplosiveKind.MINE, damage, params)
        );
    }

    private static PlantBehavior blast(
            Plant owner,
            ExplosiveProfile profile,
            ExplosionPattern pattern,
            boolean meltsIceInRow,
            boolean leavesCrater
    ) {
        return new InstantExplosionBehavior(
                owner,
                profile,
                pattern,
                meltsIceInRow,
                leavesCrater
        );
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        return ExplosiveProfiles.supportsPlantFood(spec);
    }
}
