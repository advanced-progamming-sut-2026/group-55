package pvz.model.entity.plant.projectile;

import java.util.Objects;

import pvz.model.core.board.HorizontalDirection;
import pvz.model.core.World;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.projectile.DirectionalProjectile;
import pvz.model.entity.projectile.Projectile;
import pvz.model.entity.projectile.ProjectileHitLimit;
import pvz.model.entity.projectile.ProjectileType;

public final class PlantProjectileEmitter {

    private final String projectileName;

    private World world;
    private int column;

    public PlantProjectileEmitter(String plantName) {
        String normalizedPlantName = Objects.requireNonNull(
                        plantName,
                        "plant name cannot be null"
                ).strip();

        if (normalizedPlantName.isEmpty()) {
            throw new IllegalArgumentException("plant name cannot be blank");
        }

        projectileName = normalizedPlantName + " projectile";
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
        emit(
                targetRow,
                spawnOffsetX,
                damage,
                projectileType,
                rangeTiles,
                direction,
                hitLimit,
                false
        );
    }

    public void emit(
            int targetRow,
            double spawnOffsetX,
            double damage,
            ProjectileType projectileType,
            int rangeTiles,
            HorizontalDirection direction,
            ProjectileHitLimit hitLimit,
            boolean piercesBlockingTerrain
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
                        piercesBlockingTerrain
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
                ProjectileHitLimit.singleHit(),
                false
        );
    }

    public void emitDirectional(
            int startRow,
            double spawnOffset,
            double damage,
            ProjectileType projectileType,
            int rangeTiles,
            ShotVector vector,
            ProjectileHitLimit hitLimit,
            boolean piercesBlockingTerrain
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
                        piercesBlockingTerrain
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
