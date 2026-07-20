package pvz.model.entity.plant;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.LivingEntity;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.sun.SunValue;
import pvz.model.entity.projectile.Projectile;

public class Plant extends LivingEntity {
    private static final double DEFAULT_SHOT_DAMAGE = 20;

    private final PlantSpec spec;

    private World world;
    private int column;
    private int row;
    private long lastActionTick;

    private SunProfile sunProfile;
    private int pendingSuns;

    public Plant(PlantSpec spec) {
        this.spec = spec;
        this.health = spec.getBaseHp();
        this.name = spec.getName();
    }

    public void place(World world, int column, int row, long currentTick) {
        this.world = world;
        this.column = column;
        this.row = row;
        this.lastActionTick = currentTick;
        this.sunProfile = createSunProfile(currentTick);
    }

    private SunProfile createSunProfile(long plantedTick) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "sunflower" ->
                    new FixedSunProfile(SunValue.NORMALSUN.getValue(), 1,
                            SunValue.FNSUN.getValue());

            case "twin sunflower" ->
                    new FixedSunProfile(SunValue.NORMALSUN.getValue(), 2,
                            SunValue.FTSUN.getValue());

            case "primal sunflower" ->
                    new FixedSunProfile(SunValue.BIGSUN.getValue(), 1,
                            SunValue.FBSUN.getValue());

            case "sun-shroom" ->
                    new SunShroomProfile(plantedTick);

            default -> null;
        };
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
        Set<PlantTag> tags = spec.getTags();
        return tags.contains(plantTag);
    }

    public boolean hasPendingSuns() {
        return pendingSuns > 0;
    }

    public void onProducedSunRemoved() {
        if (pendingSuns > 0) {
            pendingSuns--;
        }
    }

    @Override
    public void update(long tick) {
        if (world == null) {
            return;
        }

        long intervalTicks =
                (long) (spec.getActionInterval() * Game.TICKS_PER_SECOND);

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
        if (sunProfile == null) {
            return;
        }

        if (pendingSuns > 0) {
            return;
        }

        if (tick - lastActionTick < intervalTicks) {
            return;
        }

        lastActionTick = tick;

        List<Integer> drops = sunProfile.getCycleDrops(tick);

        for (int value : drops) {
            Sun sun = Sun.fromPlant(
                    world,
                    this,
                    getX(),
                    getY(),
                    value
            );

            world.addCollectible(sun);
            world.game().register(sun);
            pendingSuns++;
        }

        GameEvents.publish(
                "plant " + name + " produced "
                        + drops.size() + " sun(value: " + drops.getLast() + ") at ("
                        + column + ", " + row + ")"
        );
    }

    private void updateShooter(long tick, long intervalTicks) {
        if (tick - lastActionTick >= intervalTicks
                && (world.board().hasZombieAhead(row, getX())
                || world.board().hasTileObstacleAhead(row, column))) {

            lastActionTick = tick;

            world.game().register(
                    new Projectile(
                            world,
                            name + " projectile",
                            column,
                            row,
                            getShotDamage()
                    )
            );
        }
    }

    private double getShotDamage() {
        try {
            return Double.parseDouble(spec.getDamage());
        } catch (NumberFormatException exception) {
            return DEFAULT_SHOT_DAMAGE;
        }
    }

    @Override
    protected void onDeath() {
        if (world == null) {
            return;
        }

        world.board()
                .getTile(column, row)
                .removePlant(this);

        world.game().unregister(this);

        GameEvents.publish(
                "Plant " + name + " at ("
                        + column + ", " + row
                        + ") is destroyed."
        );
    }
}