package pvz.model.entity.projectile;

import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.entity.Entity;
import pvz.model.entity.zombie.Zombie;

public class Projectile extends Entity {
    private static final double TILES_PER_SECOND = 2;

    private final World world;
    private final double damage;
    protected double x;

    public Projectile(World world, String name, int row, double startX, double damage) {
        this.world = world;
        this.name = name;
        this.row = row;
        this.x = startX;
        this.damage = damage;
    }

    @Override
    public void update(long tick) {
        double previousX = x;
        x += TILES_PER_SECOND / Game.TICKS_PER_SECOND;
        Zombie target = world.board().findHitZombie(row, previousX, x);
        if (target != null) {
            target.takeDamage(damage);
            world.game().unregister(this);
            return;
        }
        if (x > world.board().getCols() + 1) {
            world.game().unregister(this);
        }
    }
}
