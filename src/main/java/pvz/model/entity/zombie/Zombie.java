package pvz.model.entity.zombie;

import java.util.List;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.Entity;
import pvz.model.entity.plant.Plant;

public abstract class Zombie extends Entity {
    protected double x;

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
        this.x = column;
        this.row = row;
        world.board().addZombie(this);
        world.game().register(this);
    }

    public double getX() {
        return x;
    }

    public int getRow() {
        return row;
    }

    public void takeDamage(double damage) {
        health -= damage;
        if (health <= 0) {
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
        if (x <= 1) {
            x = 1;
            reachedHouse = true;
            GameEvents.publish("a zombie reached the end of lane " + row
                    + "! (lawn mower is not implemented yet)");
        }
    }

    private Plant frontPlant() {
        int column = (int) Math.floor(x);
        if (!world.board().inBounds(column, row)) {
            return null;
        }
        List<Plant> plants = world.board().getTile(column, row).getPlants();
        return plants.isEmpty() ? null : plants.get(plants.size() - 1);
    }

    private void bite(Plant plant) {
        plant.takeDamage(damagePerSecond);
        if (plant.isDead()) {
            int column = (int) Math.floor(x);
            world.board().getTile(column, row).removePlant(plant);
            world.game().unregister(plant);
            GameEvents.publish("Plant " + plant.getName() + " at (" + column + ", " + row + ") is destroyed.");
        }
    }

    private void die() {
        GameEvents.publish("Zombie of type " + name + " is dead at ("
                + (int) Math.round(x) + ", " + row + ")");
        world.board().removeZombie(this);
        world.game().unregister(this);
    }
}
