package pvz.model.entity.zombie;

import java.util.List;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.LivingEntity;
import pvz.model.entity.plant.Plant;

public abstract class Zombie extends LivingEntity {
    protected double x;
    protected double y;

    private final double tilesPerSecond;
    private final double damagePerSecond;
    private World world;
    private boolean reachedHouse;

    protected Zombie(String name, double health, double tilesPerSecond, double damagePerSecond) {
        this.name = name;
        this.health = health;
        this.tilesPerSecond = tilesPerSecond;
        this.damagePerSecond = damagePerSecond;
    }

    public void spawn(World world, int column, int row) {
        this.world = world;
        this.x = tileCenter(column);
        this.y = tileCenter(row);
        world.board().addZombie(this);
        world.game().register(this);
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    public int getRow() {
        return getTileY();
    }

    @Override
    public void takeDamage(double damage) {
        super.takeDamage(damage);
        if (isDead()) {
            die();
        }
    }

    @Override
    public void update(long tick) {
        if (reachedHouse) {
            return;
        }
        Plant target = frontPlant();
        if (target != null) {
            if (tick % Game.TICKS_PER_SECOND == 0) {
                bite(target);
            }
            return;
        }
        x -= tilesPerSecond / Game.TICKS_PER_SECOND;
        if (x <= 0) {
            x = 0;
            reachedHouse = true;
            GameEvents.publish("a zombie reached the end of lane " + getTileY()
                    + "! (lawn mower is not implemented yet)");
        }
    }

    private Plant frontPlant() {
        int column = getTileX();
        int row = getTileY();
        if (!world.board().inBounds(column, row)) {
            return null;
        }
        List<Plant> plants = world.board().getTile(column, row).getPlants();
        return plants.isEmpty() ? null : plants.get(plants.size() - 1);
    }

    private void bite(Plant plant) {
        plant.takeDamage(damagePerSecond);
        if (plant.isDead()) {
            int column = getTileX();
            int row = getTileY();
            world.board().getTile(column, row).removePlant(plant);
            world.game().unregister(plant);
            GameEvents.publish("Plant " + plant.getName() + " at ("
                    + column + ", " + row + ") is destroyed.");
        }
    }

    private void die() {
        GameEvents.publish("Zombie of type " + name + " is dead at ("
                + getTileX() + ", " + getTileY() + ")");
        world.board().removeZombie(this);
        world.game().unregister(this);
    }
}
