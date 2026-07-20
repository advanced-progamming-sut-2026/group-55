package pvz.model.entity.plant;

import java.util.Set;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.LivingEntity;
import pvz.model.entity.projectile.Projectile;

public class Plant extends LivingEntity {
    private static final int PRODUCED_SUN_VALUE = 50;
    private static final double DEFAULT_SHOT_DAMAGE = 20;

    private final PlantSpec spec;
    private World world;
    private int column;
    private int row;
    private long lastActionTick;
    private boolean uncollectedSun;

    public Plant(PlantSpec spec) {
        this.spec = spec;
        this.health = spec.getBaseHp();
        this.name = spec.getName();
    }

    public void place(World world, int x, int y, long currentTick) {
        this.world = world;
        this.column = x;
        this.row = y;
        this.lastActionTick = currentTick;
    }

    @Override
    public double getX() {
        return tileCenter(column);
    }

    @Override
    public double getY() {
        return tileCenter(row);
    }

    public PlantSpec getSpec() {
        return spec;
    }

    public boolean hasTag(PlantTag plantTag) {
        Set<PlantTag> tags = this.spec.getTags();
        return tags.contains(plantTag);
    }

    public boolean hasUncollectedSun() {
        return uncollectedSun;
    }

    public int collectSun() {
        uncollectedSun = false;
        return PRODUCED_SUN_VALUE;
    }

    @Override
    public void update(long tick) {
        if (world == null) {
            return;
        }
        long intervalTicks = (long) (spec.getActionInterval() * Game.TICKS_PER_SECOND);
        if (intervalTicks <= 0) {
            return;
        }
        if (spec.getCategory() == PlantCategory.SUN_PRODUCER) {
            updateSunProducer(tick, intervalTicks);
        } else if (spec.getCategory() == PlantCategory.SHOOTER) {
            updateShooter(tick, intervalTicks);
        }
    }

    private void updateSunProducer(long tick, long intervalTicks) {
        if (!uncollectedSun && tick - lastActionTick >= intervalTicks) {
            uncollectedSun = true;
            lastActionTick = tick;
            GameEvents.publish("plant " + name + " produced a sun at (" + column + ", " + row + ")");
        }
    }

    private void updateShooter(long tick, long intervalTicks) {
        if (tick - lastActionTick >= intervalTicks && (world.board().hasZombieAhead(row, getX())
                || world.board().hasTileObstacleAhead(row, column))) {
            lastActionTick = tick;
            world.game().register(new Projectile(world, name + " projectile", column, row, getShotDamage()));
        }
    }

    private double getShotDamage() {
        try {
            return Double.parseDouble(spec.getDamage());
        } catch (NumberFormatException exception) {
            return DEFAULT_SHOT_DAMAGE;
        }
    }
}
