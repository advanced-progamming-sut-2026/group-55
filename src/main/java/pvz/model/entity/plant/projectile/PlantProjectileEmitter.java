package pvz.model.entity.plant.projectile;

import java.util.Objects;

import pvz.model.core.board.HorizontalDirection;
import pvz.model.core.World;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.projectile.DirectionalProjectile;
import pvz.model.entity.projectile.Projectile;
import pvz.model.entity.projectile.ProjectileFamily;
import pvz.model.entity.projectile.ProjectileEffectProfile;
import pvz.model.entity.plant.level.PlantUpgradeType;
import pvz.model.core.Game;
import pvz.model.entity.projectile.ProjectileHitLimit;
import pvz.model.entity.projectile.ProjectileType;

public final class PlantProjectileEmitter {

    private final String projectileName;
    private final ProjectileFamily projectileFamily;
    private final ProjectileEffectProfile effectProfile;

    private World world;
    private int column;

    public PlantProjectileEmitter(String plantName) {
        this(plantName, ProjectileFamily.GENERIC, ProjectileEffectProfile.DEFAULT);
    }

    public PlantProjectileEmitter(PlantSpec spec) {
        this(
                Objects.requireNonNull(spec, "plant spec cannot be null").getName(),
                spec.getTags().contains(PlantTag.PEA)
                        ? ProjectileFamily.PEA
                        : ProjectileFamily.GENERIC,
                effectProfileFor(spec)
        );
    }

    private PlantProjectileEmitter(
            String plantName,
            ProjectileFamily projectileFamily,
            ProjectileEffectProfile effectProfile
    ) {
        String normalizedPlantName = Objects.requireNonNull(
                        plantName,
                        "plant name cannot be null"
                ).strip();

        if (normalizedPlantName.isEmpty()) {
            throw new IllegalArgumentException("plant name cannot be blank");
        }

        projectileName = normalizedPlantName + " projectile";
        this.projectileFamily = Objects.requireNonNull(
                projectileFamily,
                "projectile family cannot be null"
        );
        this.effectProfile = Objects.requireNonNull(
                effectProfile,
                "projectile effect profile cannot be null"
        );
    }

    private static ProjectileEffectProfile effectProfileFor(PlantSpec spec) {
        long chillTicks = ProjectileEffectProfile.DEFAULT.chillDurationTicks()
                + Math.round(spec.getUpgradeValue(PlantUpgradeType.CHILL_SECONDS_ADD)
                * Game.TICKS_PER_SECOND);
        double poisonDamage = ProjectileEffectProfile.DEFAULT.poisonDamagePerSecond()
                + spec.getUpgradeValue(PlantUpgradeType.POISON_DAMAGE_PER_TICK_ADD);
        return new ProjectileEffectProfile(
                Math.max(0, chillTicks),
                ProjectileEffectProfile.DEFAULT.poisonDurationTicks(),
                Math.max(0, poisonDamage),
                ProjectileEffectProfile.DEFAULT.maximumPoisonStacks()
        );
    }

    public void onPlaced(World world, int column) {
        this.world = Objects.requireNonNull(world, "world cannot be null");

        if (column <= 0) {
            throw new IllegalArgumentException("plant column must be positive");
        }

        this.column = column;
    }

    public void emit(
            int targetRow,
            double spawnOffsetX,
            double damage,
            ProjectileType projectileType,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        emit(
                targetRow,
                spawnOffsetX,
                damage,
                projectileType,
                rangeTiles,
                direction,
                ProjectileHitLimit.singleHit()
        );
    }

    public void emit(
            int targetRow,
            double spawnOffsetX,
            double damage,
            ProjectileType projectileType,
            int rangeTiles,
            HorizontalDirection direction,
            ProjectileHitLimit hitLimit
    ) {
        ensurePlaced();

        world.game().register(
                new Projectile(
                        world,
                        projectileName,
                        column,
                        targetRow,
                        spawnOffsetX,
                        damage,
                        projectileType,
                        rangeTiles,
                        direction,
                        hitLimit,
                        projectileFamily,
                        effectProfile
                )
        );
    }

    public void emitDirectional(
            int startRow,
            double damage,
            ProjectileType projectileType,
            int rangeTiles,
            ShotVector vector
    ) {
        emitDirectional(
                startRow,
                0,
                damage,
                projectileType,
                rangeTiles,
                vector
        );
    }

    public void emitDirectional(
            int startRow,
            double spawnOffset,
            double damage,
            ProjectileType projectileType,
            int rangeTiles,
            ShotVector vector
    ) {
        emitDirectional(
                startRow,
                spawnOffset,
                damage,
                projectileType,
                rangeTiles,
                vector,
                ProjectileHitLimit.singleHit()
        );
    }

    public void emitDirectional(
            int startRow,
            double spawnOffset,
            double damage,
            ProjectileType projectileType,
            int rangeTiles,
            ShotVector vector,
            ProjectileHitLimit hitLimit
    ) {
        ensurePlaced();

        world.game().register(
                new DirectionalProjectile(
                        world,
                        projectileName,
                        column,
                        startRow,
                        spawnOffset,
                        damage,
                        projectileType,
                        rangeTiles,
                        vector,
                        hitLimit,
                        projectileFamily,
                        effectProfile
                )
        );
    }

    private void ensurePlaced() {
        if (world == null) {
            throw new IllegalStateException(
                    "projectile emitter must be placed before use"
            );
        }
    }
}
