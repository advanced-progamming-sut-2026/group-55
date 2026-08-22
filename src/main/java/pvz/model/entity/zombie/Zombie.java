package pvz.model.entity.zombie;

import java.util.List;
import java.util.Objects;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.LivingEntity;
import pvz.model.entity.collectible.plantfood.PlantFood;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.behavior.ZombieBehavior;

public final class Zombie extends LivingEntity {
    private static final double CHILLED_SPEED_MULTIPLIER = 0.5;
    private static final double PLANT_FOOD_DROP_CHANCE = 0.05;

    protected double x;
    protected double y;

    private final double tilesPerSecond;
    private final double damagePerSecond;
    private final ArmorSet armorSet;
    private final ZombieSpec spec;
    private final List<ZombieBehavior> behaviors;

    private World world;
    private DamageContext incomingDamage;
    private boolean reachedHouse;

    private Plant biteTarget;
    private long nextBiteTick = Long.MAX_VALUE;

    private long chilledUntilTick;
    private long frozenUntilTick;
    private long butteredUntilTick;

    private int poisonStacks;
    private double poisonDamagePerStack;
    private long poisonUntilTick;
    private long nextPoisonDamageTick = Long.MAX_VALUE;

    public Zombie(
            ZombieSpec spec,
            ArmorSet armorSet,
            List<ZombieBehavior> behaviors
    ) {
        this.spec = Objects.requireNonNull(
                spec,
                "zombie spec cannot be null"
        );
        this.name = spec.getName();
        this.health = spec.getHitpoints();
        this.tilesPerSecond = spec.getSpeed();
        this.damagePerSecond = spec.getEatDps();
        this.armorSet = Objects.requireNonNull(
                armorSet,
                "armor set cannot be null"
        );
        this.behaviors = List.copyOf(behaviors);
    }

    public void spawn(World world, int column, int row) {
        Objects.requireNonNull(world, "world cannot be null");

        if (this.world != null) {
            throw new IllegalStateException(
                    "zombie is already spawned"
            );
        }

        if (!world.board().inBounds(column, row)) {
            throw new IllegalArgumentException(
                    "zombie spawn location is out of bounds"
            );
        }

        world.addZombie(this);
        this.world = world;
        this.x = tileCenter(column);
        this.y = tileCenter(row);
        world.game().register(this);
        for (ZombieBehavior behavior : behaviors) {
            behavior.onSpawn(this, world, world.game().getCurrentTick());
        }
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

    public ArmorSet getArmorSet() {
        return armorSet;
    }

    public double getArmorHealth() {
        return armorSet.layers().stream()
                .mapToDouble(ArmorInstance::remainingHealth)
                .sum();
    }

    @Override
    protected double modifyIncomingDamage(double damage) {
        if (incomingDamage == null
                || incomingDamage.bypassArmor()
                || !armorSet.hasIntactArmor()) {
            return damage;
        }
        ArmorSet.ArmorDamageResult result = armorSet.absorb(damage);
        long currentTick = incomingDamage.tick();
        for (ArmorInstance brokenArmor : result.brokenLayers()) {
            for (ZombieBehavior behavior : behaviors) {
                behavior.onArmorBroken(this, brokenArmor, currentTick);
            }
        }
        return result.overflowDamage();
    }

    @Override
    public void update(long tick) {
        updatePoison(tick);

        if (reachedHouse
                || isDead()
                || isFrozen(tick)
                || isButtered(tick)) {
            return;
        }

        for (ZombieBehavior behavior : behaviors) {
            behavior.onTick(this, world, tick);
        }

        Plant target = frontPlant();

        if (target != null) {
            for (ZombieBehavior behavior : behaviors) {
                if (behavior.onPlantEncounter(this, target, world, tick)) {
                    return;
                }
            }
            updateBiting(target, tick);
            return;
        }

        biteTarget = null;
        nextBiteTick = Long.MAX_VALUE;

        double speedMultiplier =
                isChilled(tick)
                        ? CHILLED_SPEED_MULTIPLIER
                        : 1;

        x -= tilesPerSecond
                * speedMultiplier
                / Game.TICKS_PER_SECOND;

        if (x <= 0) {
            x = 0;
            handleReachedHouse();
        }
    }

    private void handleReachedHouse() {
        reachedHouse = true;

        int row = getTileY();

        if (world.isLawnMowerAvailable(row)) {
            world.activateLawnMower(row);
            return;
        }

        world.game()
                .getStateManager()
                .lose();
    }

    public void takeProjectileDamage(double damage) {
        receiveHit(new DamageContext(
                damage,
                DamageContext.DamageSource.PROJECTILE,
                null,
                DamageContext.AttackPath.UNKNOWN,
                false,
                currentTick()
        ));
    }

    public void takeDirectDamage(double damage) {
        receiveHit(new DamageContext(
                damage,
                DamageContext.DamageSource.DIRECT,
                null,
                DamageContext.AttackPath.UNKNOWN,
                true,
                currentTick()
        ));
    }

    public void receiveHit(DamageContext context) {
        Objects.requireNonNull(context, "damage context cannot be null");
        DamageContext processed = context;
        for (ZombieBehavior behavior : behaviors) {
            processed = Objects.requireNonNull(
                    behavior.onIncomingHit(this, processed),
                    "behavior cannot return a null damage context"
            );
        }
        incomingDamage = processed;
        try {
            takeDamage(processed.damage());
        } finally {
            incomingDamage = null;
        }
    }

    private long currentTick() {
        return world == null ? 0 : world.game().getCurrentTick();
    }

    public void applyChill(
            long currentTick,
            long durationTicks
    ) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        if (durationTicks <= 0) {
            throw new IllegalArgumentException(
                    "chill duration must be positive"
            );
        }

        chilledUntilTick = Math.max(
                chilledUntilTick,
                currentTick + durationTicks
        );

        if (biteTarget != null) {
            nextBiteTick = Math.max(
                    nextBiteTick,
                    currentTick
                            + chilledAttackIntervalTicks()
            );
        }
    }

    public void removeChill(long currentTick) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        chilledUntilTick = 0;

        if (biteTarget != null) {
            nextBiteTick = Math.min(
                    nextBiteTick,
                    currentTick + Game.TICKS_PER_SECOND
            );
        }
    }

    public void applyFreeze(
            long currentTick,
            long durationTicks
    ) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        if (durationTicks <= 0) {
            throw new IllegalArgumentException(
                    "freeze duration must be positive"
            );
        }

        frozenUntilTick = Math.max(
                frozenUntilTick,
                currentTick + durationTicks
        );

        delayBiteUntilAfterHardStop(currentTick);
    }

    public void removeFreeze(long currentTick) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        frozenUntilTick = 0;

        if (biteTarget != null) {
            nextBiteTick = Math.min(
                    nextBiteTick,
                    currentTick
                            + attackIntervalTicks(currentTick)
            );
        }
    }


    public void applyButterStun(
            long currentTick,
            long durationTicks
    ) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        if (durationTicks <= 0) {
            throw new IllegalArgumentException(
                    "butter stun duration must be positive"
            );
        }

        butteredUntilTick = Math.max(
                butteredUntilTick,
                currentTick + durationTicks
        );

        delayBiteUntilAfterHardStop(currentTick);
    }

    public void applyPoison(
            long currentTick,
            long durationTicks,
            double damagePerStack,
            int maximumStacks
    ) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        if (durationTicks <= 0) {
            throw new IllegalArgumentException(
                    "poison duration must be positive"
            );
        }

        if (damagePerStack < 0) {
            throw new IllegalArgumentException(
                    "poison damage cannot be negative"
            );
        }

        if (maximumStacks <= 0) {
            throw new IllegalArgumentException(
                    "maximum poison stacks must be positive"
            );
        }

        if (currentTick > poisonUntilTick) {
            clearPoison();
        }

        poisonStacks = Math.min(
                maximumStacks,
                poisonStacks + 1
        );

        poisonDamagePerStack = Math.max(
                poisonDamagePerStack,
                damagePerStack
        );

        poisonUntilTick = currentTick + durationTicks;

        if (nextPoisonDamageTick == Long.MAX_VALUE) {
            nextPoisonDamageTick =
                    currentTick + Game.TICKS_PER_SECOND;
        }
    }

    public boolean isChilled(long currentTick) {
        return currentTick < chilledUntilTick;
    }

    public boolean isFrozen(long currentTick) {
        return currentTick < frozenUntilTick;
    }

    public boolean isButtered(long currentTick) {
        return currentTick < butteredUntilTick;
    }

    public boolean isPoisoned(long currentTick) {
        return poisonStacks > 0 && currentTick <= poisonUntilTick;
    }

    private void updateBiting(
            Plant target,
            long currentTick
    ) {
        if (target != biteTarget) {
            biteTarget = target;
            nextBiteTick = currentTick
                    + attackIntervalTicks(currentTick);
        }

        if (currentTick < nextBiteTick) {
            return;
        }

        bite(target);

        nextBiteTick = currentTick
                + attackIntervalTicks(currentTick);
    }

    private void delayBiteUntilAfterHardStop(
            long currentTick
    ) {
        if (biteTarget == null) {
            return;
        }

        long hardStopEndTick = Math.max(
                frozenUntilTick,
                butteredUntilTick
        );

        if (hardStopEndTick <= currentTick) {
            return;
        }

        nextBiteTick = Math.max(
                nextBiteTick,
                hardStopEndTick
                        + attackIntervalTicks(hardStopEndTick)
        );
    }

    private long attackIntervalTicks(long currentTick) {
        if (isChilled(currentTick)) {
            return chilledAttackIntervalTicks();
        }

        return Game.TICKS_PER_SECOND;
    }

    private long chilledAttackIntervalTicks() {
        return 2L * Game.TICKS_PER_SECOND;
    }

    private void updatePoison(long currentTick) {
        if (poisonStacks == 0) {
            return;
        }

        while (currentTick >= nextPoisonDamageTick
                && nextPoisonDamageTick <= poisonUntilTick
                && !isDead()) {

            receiveHit(new DamageContext(
                    poisonStacks * poisonDamagePerStack,
                    DamageContext.DamageSource.POISON,
                    null,
                    DamageContext.AttackPath.UNKNOWN,
                    true,
                    currentTick
            ));

            nextPoisonDamageTick +=
                    Game.TICKS_PER_SECOND;
        }

        if (currentTick >= poisonUntilTick) {
            clearPoison();
        }
    }

    private void clearPoison() {
        poisonStacks = 0;
        poisonDamagePerStack = 0;
        poisonUntilTick = 0;
        nextPoisonDamageTick = Long.MAX_VALUE;
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
    }

    private void tryDropPlantFood() {
        if (Math.random() > PLANT_FOOD_DROP_CHANCE) {
            return;
        }

        PlantFood plantFood = PlantFood.fromZombie(
                world,
                getX(),
                getY()
        );

        world.addCollectible(plantFood);
        world.game().register(plantFood);
    }

    @Override
    protected void onDeath() {
        if (world == null) {
            return;
        }

        for (ZombieBehavior behavior : behaviors) {
            behavior.onDeath(this, world, currentTick());
        }

        GameEvents.publish(
                "Zombie of type " + name + " is dead at ("
                        + getTileX() + ", "
                        + getTileY() + ")"
        );

        tryDropPlantFood();
        world.removeZombie(this);
        world.game().unregister(this);
    }
}
