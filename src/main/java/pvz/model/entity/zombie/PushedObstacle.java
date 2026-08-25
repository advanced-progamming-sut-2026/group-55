package pvz.model.entity.zombie;

import java.util.Objects;
import java.util.function.Consumer;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.LivingEntity;
import pvz.model.entity.projectile.ProjectileType;

public final class PushedObstacle extends LivingEntity {
    private final String id;
    private final double maximumHealth;
    private final boolean blocksStraightProjectiles;
    private final boolean crushesPlants;
    private final boolean meltsOnFire;
    private final Consumer<PushedObstacle> destructionListener;

    private World world;
    private double x;
    private double y;

    public PushedObstacle(
            String id,
            String name,
            double maximumHealth,
            boolean blocksStraightProjectiles,
            boolean crushesPlants,
            boolean meltsOnFire
    ) {
        this(
                id,
                name,
                maximumHealth,
                blocksStraightProjectiles,
                crushesPlants,
                meltsOnFire,
                ignored -> { }
        );
    }

    public PushedObstacle(
            String id,
            String name,
            double maximumHealth,
            boolean blocksStraightProjectiles,
            boolean crushesPlants,
            boolean meltsOnFire,
            Consumer<PushedObstacle> destructionListener
    ) {
        this.id = requireText(id, "obstacle id");
        this.name = requireText(name, "obstacle name");
        if (!Double.isFinite(maximumHealth) || maximumHealth <= 0) {
            throw new IllegalArgumentException(
                    "obstacle health must be a positive number"
            );
        }
        this.maximumHealth = maximumHealth;
        this.health = maximumHealth;
        this.blocksStraightProjectiles = blocksStraightProjectiles;
        this.crushesPlants = crushesPlants;
        this.meltsOnFire = meltsOnFire;
        this.destructionListener = Objects.requireNonNull(
                destructionListener,
                "destruction listener cannot be null"
        );
    }

    public void spawn(World world, double x, double y) {
        if (this.world != null) {
            throw new IllegalStateException("obstacle is already spawned");
        }
        this.world = Objects.requireNonNull(world, "world cannot be null");
        moveTo(x, y);
        world.addPushedObstacle(this);
    }

    public void moveTo(double x, double y) {
        if (world == null) {
            throw new IllegalStateException("obstacle is not spawned");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException(
                    "obstacle position must be finite"
            );
        }
        this.x = Math.max(0, Math.min(world.board().getCols(), x));
        this.y = Math.max(0, Math.min(world.board().getRows(), y));
    }

    public void takeProjectileDamage(
            ProjectileType projectileType,
            double baseDamage
    ) {
        Objects.requireNonNull(
                projectileType,
                "projectile type cannot be null"
        );
        if (meltsOnFire && projectileType == ProjectileType.FIRE) {
            takeDamage(health);
            return;
        }
        takeDamage(projectileType.calculateDamage(baseDamage));
    }

    public void takeDirectDamage(double damage) {
        takeDamage(damage);
    }

    public String getId() {
        return id;
    }

    public double getMaximumHealth() {
        return maximumHealth;
    }

    public boolean blocksStraightProjectiles() {
        return blocksStraightProjectiles;
    }

    public boolean crushesPlants() {
        return crushesPlants;
    }

    public boolean meltsOnFire() {
        return meltsOnFire;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public void update(long tick) {
        // Movement belongs to the zombie behavior that pushes this object.
    }

    @Override
    protected void onDeath() {
        if (world == null) {
            return;
        }
        world.removePushedObstacle(this);
        destructionListener.accept(this);
        GameEvents.publish(
                name + " was destroyed at (" + getTileX()
                        + ", " + getTileY() + ")."
        );
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " cannot be null");
        String result = value.strip();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return result;
    }
}
