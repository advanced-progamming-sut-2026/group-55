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

    private final ArmorType armor;
    private double armorHealth;

    protected World world;
    private boolean reachedHouse;

    protected final ZombieSpec spec;


    protected Zombie(ZombieSpec spec) {
        this.spec = spec;

        this.name = spec.getName();
        this.health = spec.getHitpoints();

        this.tilesPerSecond = spec.getSpeed();
        this.damagePerSecond = spec.getEatDps();

        this.armor = spec.getArmor();
        this.armorHealth = armor.getHitpoints();
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


    public ZombieSpec getSpec() {
        return spec;
    }


    public ArmorType getArmor() {
        return armor;
    }


    public double getArmorHealth() {
        return armorHealth;
    }


    @Override
    protected double modifyIncomingDamage(double damage) {

        if (armorHealth <= 0) {
            return damage;
        }


        double remainingDamage = damage - armorHealth;

        armorHealth -= damage;


        if (armorHealth < 0) {
            armorHealth = 0;
        }


        return Math.max(0, remainingDamage);
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

            GameEvents.publish(
                    "A zombie reached the end of lane "
                            + getTileY()
                            + "!"
            );
        }
    }


    private Plant frontPlant() {

        int column = getTileX();
        int row = getTileY();


        if (!world.board().inBounds(column, row)) {
            return null;
        }


        List<Plant> plants =
                world.board()
                        .getTile(column, row)
                        .getPlants();


        return plants.isEmpty()
                ? null
                : plants.get(plants.size() - 1);
    }


    private void bite(Plant plant) {

        plant.takeDamage(damagePerSecond);
    }


    @Override
    protected void onDeath() {

        if (world == null) {
            return;
        }


        GameEvents.publish(
                "Zombie " + name
                        + " died at ("
                        + getTileX()
                        + ", "
                        + getTileY()
                        + ")"
        );


        world.board().removeZombie(this);
        world.game().unregister(this);
    }
}
