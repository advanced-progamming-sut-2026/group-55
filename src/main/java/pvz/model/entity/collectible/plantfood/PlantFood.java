package pvz.model.entity.collectible.plantfood;

import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.entity.collectible.Collectible;

public final class PlantFood extends Collectible {

    private static final long LIFETIME_TICKS =
            8L * Game.TICKS_PER_SECOND;

    private final World world;

    private final double x;
    private final double y;

    private long lifetimeTicksLeft;

    private PlantFood(
            World world,
            double x,
            double y
    ) {
        this.world = Objects.requireNonNull(world);
        this.name = "plant_food";

        this.x = x;
        this.y = y;

        this.lifetimeTicksLeft = LIFETIME_TICKS;
    }

    public static PlantFood fromZombie(
            World world,
            double x,
            double y
    ) {
        return new PlantFood(world, x, y);
    }

    @Override
    public void update(long tick) {
        lifetimeTicksLeft--;

        if (lifetimeTicksLeft <= 0) {
            remove();
        }
    }

    public void remove() {
        world.removeCollectible(this);
        world.game().unregister(this);
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }
}
