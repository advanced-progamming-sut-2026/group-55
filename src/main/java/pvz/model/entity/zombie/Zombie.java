package pvz.model.entity.zombie;

import java.util.List;
import java.util.Objects;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.LivingEntity;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.hit.PlantHitSource;
import pvz.model.entity.zombie.behavior.ZombieBehavior;

public final class Zombie extends LivingEntity {
    private static final double CHILLED_SPEED_MULTIPLIER = 0.5;

    protected double x;
    protected double y;

    private final double tilesPerSecond;
    private final double damagePerSecond;
    private double alliedHealthMultiplier = 1;
    private double alliedDamageMultiplier = 1;
    private final ArmorSet armorSet;
    private final ZombieSpec spec;
    private final ZombieRuntimeStats runtimeStats;
    private final List<ZombieBehavior> behaviors;

    private World world;
    private DamageContext incomingDamage;
    private boolean reachedHouse;
    private boolean exitedWorld;
    private boolean glowing;

    private Plant biteTarget;
    private long nextBiteTick = Long.MAX_VALUE;

    private ZombieAllegiance allegiance = ZombieAllegiance.HOSTILE;
    private final ZombieCombatController zombieCombat =
            new ZombieCombatController();

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
        this(
                spec,
                ZombieRuntimeStats.from(spec, 3),
                armorSet,
                behaviors
        );
    }

    public Zombie(
            ZombieSpec spec,
            ZombieRuntimeStats runtimeStats,
            ArmorSet armorSet,
            List<ZombieBehavior> behaviors
    ) {
        this.spec = Objects.requireNonNull(
                spec,
                "zombie spec cannot be null"
        );
        this.runtimeStats = Objects.requireNonNull(
                runtimeStats,
                "zombie runtime stats cannot be null"
        );
        this.name = spec.getName();
        this.health = runtimeStats.maxHealth();
        this.tilesPerSecond = runtimeStats.tilesPerSecond();
        this.damagePerSecond = runtimeStats.eatDamagePerSecond();
        this.armorSet = Objects.requireNonNull(
                armorSet,
                "armor set cannot be null"
        );
        this.behaviors = List.copyOf(behaviors);
    }

    public void spawn(World world, int column, int row) {
        spawn(world, column, row, ZombieAllegiance.HOSTILE);
    }

    public void spawn(
            World world,
            int column,
            int row,
            ZombieAllegiance initialAllegiance
    ) {
        spawnInternal(
                world,
                column,
                row,
                initialAllegiance,
                null
        );
    }

    public void spawnWithGlowingState(
            World world,
            int column,
            int row,
            ZombieAllegiance initialAllegiance,
            boolean initialGlowing
    ) {
        spawnInternal(
                world,
                column,
                row,
                initialAllegiance,
                initialGlowing
        );
    }

    private void spawnInternal(
            World world,
            int column,
            int row,
            ZombieAllegiance initialAllegiance,
            Boolean initialGlowing
    ) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(
                initialAllegiance,
                "initial allegiance cannot be null"
        );

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

        allegiance = initialAllegiance;
        glowing = initialGlowing != null
                ? initialGlowing
                : world.rollGlowingZombie();
        this.x = tileCenter(column);
        this.y = tileCenter(row);
        world.addZombie(this);
        this.world = world;
        world.game().register(this);

        if (isHostile()) {
            for (ZombieBehavior behavior : behaviors) {
                behavior.onSpawn(
                        this,
                        world,
                        world.game().getCurrentTick()
                );
            }
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

    public World getWorld() {
        return world;
    }

    public double getMaximumHealth() {
        return runtimeStats.maxHealth() * alliedHealthMultiplier;
    }

    public double getHealthRatio() {
        return health / getMaximumHealth();
    }

    public double getEffectiveEatDamagePerSecond() {
        return damagePerSecond * alliedDamageMultiplier;
    }

    public void applyAlliedCombatBuff(double healthPercentAdd, double damagePercentAdd) {
        if (!Double.isFinite(healthPercentAdd) || healthPercentAdd < 0
                || !Double.isFinite(damagePercentAdd) || damagePercentAdd < 0) {
            throw new IllegalArgumentException("allied combat buffs must be finite and non-negative");
        }
        if (!isAllied()) {
            throw new IllegalStateException("combat buffs may only be applied to allied zombies");
        }
        double oldMaximum = getMaximumHealth();
        alliedHealthMultiplier *= 1 + healthPercentAdd;
        alliedDamageMultiplier *= 1 + damagePercentAdd;
        double newMaximum = getMaximumHealth();
        health += Math.max(0, newMaximum - oldMaximum);
    }

    public boolean isEating() {
        return biteTarget != null && !biteTarget.isRemovedFromWorld();
    }

    public ZombieAllegiance getAllegiance() {
        return allegiance;
    }

    public boolean isHostile() {
        return allegiance == ZombieAllegiance.HOSTILE;
    }

    public boolean isAllied() {
        return allegiance == ZombieAllegiance.ALLIED;
    }

    /**
     * Internal side-change hook used by World/ZombieRegistry. Callers that
     * change allegiance must go through World so registry indexes stay in sync.
     */
    public void applyAllegianceFromWorld(
            ZombieAllegiance newAllegiance
    ) {
        Objects.requireNonNull(
                newAllegiance,
                "zombie allegiance cannot be null"
        );

        if (allegiance == newAllegiance) {
            return;
        }

        ZombieAllegiance oldAllegiance = allegiance;
        allegiance = newAllegiance;
        resetBiteTarget();
        zombieCombat.reset();

        if (world != null) {
            long tick = currentTick();
            for (ZombieBehavior behavior : behaviors) {
                behavior.onAllegianceChanged(
                        this,
                        world,
                        oldAllegiance,
                        newAllegiance,
                        tick
                );
            }
        }
    }

    public boolean isFightingZombie() {
        return zombieCombat.isFighting();
    }

    public ZombieSpec getSpec() {
        return spec;
    }

    public ZombieRuntimeStats getRuntimeStats() {
        return runtimeStats;
    }

    public ArmorSet getArmorSet() {
        return armorSet;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        if (world != null) {
            throw new IllegalStateException(
                    "glowing state cannot change after spawn"
            );
        }

        this.glowing = glowing;
    }

    public double getArmorHealth() {
        return armorSet.layers().stream()
                .mapToDouble(ArmorInstance::remainingHealth)
                .sum();
    }

    public void addArmor(ArmorSpec armorSpec) {
        armorSet.add(Objects.requireNonNull(armorSpec));
    }

    void detachForTransformation() {
        requireSpawned();

        World activeWorld = world;
        resetBiteTarget();
        zombieCombat.reset();
        activeWorld.removeZombie(this);
        activeWorld.game().unregister(this);
        world = null;
    }

    void restorePositionAfterTransformation(double targetX, double targetY) {
        requireSpawned();

        if (!Double.isFinite(targetX) || !Double.isFinite(targetY)) {
            throw new IllegalArgumentException(
                    "transformation position must be finite"
            );
        }

        if (targetX < 0
                || targetX >= world.board().getCols()
                || targetY < 0
                || targetY >= world.board().getRows()) {
            throw new IllegalArgumentException(
                    "transformation position is out of bounds"
            );
        }

        x = targetX;
        y = targetY;
        resetBiteTarget();
    }

    public void moveToColumn(int column) {
        requireSpawned();
        if (!world.board().inBounds(column, getRow())) {
            throw new IllegalArgumentException("zombie column is out of bounds");
        }
        x = tileCenter(column);
        resetBiteTarget();
        notifyPositionChanged();
    }

    public void moveToRow(int row) {
        requireSpawned();
        if (!world.board().inBounds(getTileX(), row)) {
            throw new IllegalArgumentException("zombie row is out of bounds");
        }
        y = tileCenter(row);
        resetBiteTarget();
        notifyPositionChanged();
    }

    public void moveByTiles(double deltaX) {
        requireSpawned();
        if (!Double.isFinite(deltaX)) {
            throw new IllegalArgumentException("movement must be finite");
        }
        double nextX = x + deltaX;
        if (nextX >= world.board().getCols()) {
            x = world.board().getCols();
            resetBiteTarget();
            notifyPositionChanged();
            handleExitedWorld();
            return;
        }

        x = Math.max(0, nextX);
        resetBiteTarget();
        notifyPositionChanged();
    }

    public void knockBackTowardSpawn(double tiles) {
        requireSpawned();
        if (!Double.isFinite(tiles) || tiles < 0) {
            throw new IllegalArgumentException(
                    "knockback distance must be finite and non-negative"
            );
        }
        if (tiles == 0 || isDead()) {
            return;
        }

        double rightmostCenter = world.board().getCols() - 0.5;
        x = Math.min(rightmostCenter, x + tiles);
        resetBiteTarget();
        zombieCombat.reset();
        notifyPositionChanged();
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

        if (reachedHouse || exitedWorld || isDead()) {
            return;
        }

        if (isFrozen(tick) || isButtered(tick)) {
            zombieCombat.reset();
            if (isHostile()) {
                for (ZombieBehavior behavior : behaviors) {
                    behavior.onHardStopTick(this, world, tick);
                }
            }
            return;
        }

        // Opposing zombies are a physical combat blocker for both sides.
        // Checking before hostile abilities prevents a zombie already locked
        // in melee from continuing to attack the player's plants remotely.
        if (updateZombieCombat(tick)) {
            resetBiteTarget();
            return;
        }

        if (isAllied()) {
            resetBiteTarget();
            advanceMovement(tick, false);
            return;
        }

        for (ZombieBehavior behavior : behaviors) {
            behavior.onTick(this, world, tick);
        }

        if (reachedHouse || exitedWorld || isDead()) {
            return;
        }

        // Some hostile abilities move the zombie during onTick. Re-check
        // contact before allowing a plant encounter or another movement step.
        if (updateZombieCombat(tick)) {
            resetBiteTarget();
            return;
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

        resetBiteTarget();
        advanceMovement(tick, true);
    }

    private void advanceMovement(
            long tick,
            boolean applyHostileBehaviorModifiers
    ) {
        double speedMultiplier =
                isChilled(tick)
                        ? CHILLED_SPEED_MULTIPLIER
                        : 1;

        if (applyHostileBehaviorModifiers) {
            for (ZombieBehavior behavior : behaviors) {
                speedMultiplier = behavior.modifyMovementMultiplier(
                        this,
                        world,
                        tick,
                        speedMultiplier
                );
            }
        }

        double nextX = x + movementSign() * tilesPerSecond
                * speedMultiplier
                / Game.TICKS_PER_SECOND;

        if (nextX >= world.board().getCols()) {
            x = world.board().getCols();
            notifyPositionChanged();
            handleExitedWorld();
            return;
        }

        x = nextX;
        notifyPositionChanged();

        if (isHostile() && x <= 0) {
            x = 0;
            handleReachedHouse();
        }
    }

    private double movementSign() {
        return isAllied() ? 1 : -1;
    }

    private boolean updateZombieCombat(long tick) {
        Zombie opponent = world.findOpposingZombieInTile(this);
        if (opponent == null) {
            zombieCombat.reset();
            return false;
        }

        if (isHostile()) {
            for (ZombieBehavior behavior : behaviors) {
                if (behavior.onOpposingZombieEncounter(
                        this,
                        opponent,
                        world,
                        tick
                )) {
                    zombieCombat.reset();
                    return true;
                }
            }
        }

        return zombieCombat.update(
                this,
                opponent,
                tick,
                getEffectiveEatDamagePerSecond(),
                attackIntervalTicks(tick)
        );
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

    private void handleExitedWorld() {
        if (exitedWorld || world == null) {
            return;
        }

        exitedWorld = true;
        resetBiteTarget();
        world.removeZombie(this);
        world.game().unregister(this);
        GameEvents.publish(
                "Zombie of type " + name
                        + " exited the lawn from the right side."
        );
    }

    public void takeProjectileDamage(double damage) {
        receiveHit(new DamageContext(
                damage,
                DamageContext.DamageSource.PROJECTILE,
                null,
                DamageContext.AttackDelivery.UNKNOWN,
                DamageContext.ImpactMode.SINGLE_TARGET,
                false,
                currentTick()
        ));
    }

    public void takeDirectDamage(double damage) {
        receiveHit(new DamageContext(
                damage,
                DamageContext.DamageSource.DIRECT,
                null,
                DamageContext.AttackDelivery.UNKNOWN,
                DamageContext.ImpactMode.SINGLE_TARGET,
                true,
                currentTick()
        ));
    }

    public boolean takeAbilityDamage(
            double damage,
            DamageContext.ImpactMode impactMode
    ) {
        return takeAbilityDamage(
                damage,
                DamageContext.AttackDelivery.UNKNOWN,
                impactMode
        );
    }

    public boolean takeAbilityDamage(
            double damage,
            DamageContext.AttackDelivery delivery,
            DamageContext.ImpactMode impactMode
    ) {
        return receiveHit(new DamageContext(
                damage,
                DamageContext.DamageSource.ABILITY,
                null,
                Objects.requireNonNull(
                        delivery,
                        "attack delivery cannot be null"
                ),
                Objects.requireNonNull(
                        impactMode,
                        "impact mode cannot be null"
                ),
                false,
                currentTick()
        ));
    }

    public boolean takeZombieCombatDamage(
            double damage,
            long currentTick
    ) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }
        return receiveHit(new DamageContext(
                damage,
                DamageContext.DamageSource.ZOMBIE,
                null,
                DamageContext.AttackDelivery.CONTACT,
                DamageContext.ImpactMode.SINGLE_TARGET,
                false,
                currentTick
        ));
    }

    public boolean receiveHit(DamageContext context) {
        Objects.requireNonNull(context, "damage context cannot be null");
        DamageContext processed = context;
        for (ZombieBehavior behavior : behaviors) {
            processed = Objects.requireNonNull(
                    behavior.onIncomingHit(this, processed),
                    "behavior cannot return a null damage context"
            );
        }
        if (processed.damage() <= 0) {
            return false;
        }
        incomingDamage = processed;
        try {
            takeDamage(processed.damage());
        } finally {
            incomingDamage = null;
        }

        if (!isDead()) {
            for (ZombieBehavior behavior : behaviors) {
                behavior.onAcceptedHit(this, processed);
            }
        }
        return true;
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
        zombieCombat.delayNextAttack(
                currentTick,
                chilledAttackIntervalTicks()
        );
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
        zombieCombat.accelerateNextAttack(
                currentTick,
                Game.TICKS_PER_SECOND
        );
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

        if (behaviors.stream().anyMatch(
                ZombieBehavior::convertsFreezeToChill
        )) {
            applyChill(currentTick, durationTicks);
            return;
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

    public void clearColdEffects(long currentTick) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        chilledUntilTick = 0;
        frozenUntilTick = 0;

        if (biteTarget != null) {
            nextBiteTick = Math.min(
                    nextBiteTick,
                    currentTick + Game.TICKS_PER_SECOND
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

    public long getRemainingChillTicks(long currentTick) {
        return remainingTicks(chilledUntilTick, currentTick);
    }

    public long getRemainingFreezeTicks(long currentTick) {
        return remainingTicks(frozenUntilTick, currentTick);
    }

    public long getRemainingButterTicks(long currentTick) {
        return remainingTicks(butteredUntilTick, currentTick);
    }

    public long getRemainingPoisonTicks(long currentTick) {
        if (!isPoisoned(currentTick)) {
            return 0;
        }

        return remainingTicks(poisonUntilTick, currentTick);
    }

    private long remainingTicks(long endTick, long currentTick) {
        return Math.max(0, endTick - currentTick);
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
                    DamageContext.AttackDelivery.UNKNOWN,
                    DamageContext.ImpactMode.SINGLE_TARGET,
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
        if (isAllied()) {
            return null;
        }

        int column = getTileX();
        int row = getTileY();
        if (!world.board().inBounds(column, row)) {
            return null;
        }
        List<Plant> plants = world.board().getTile(column, row).getPlants();
        for (int index = plants.size() - 1; index >= 0; index--) {
            Plant plant = plants.get(index);
            if (plant.isZombieTargetable()
                    && plant.canBeEatenByZombie()) {
                return plant;
            }
        }
        return null;
    }

    private void bite(Plant plant) {
        double damage = getEffectiveEatDamagePerSecond();
        long tick = currentTick();
        for (ZombieBehavior behavior : behaviors) {
            damage = behavior.modifyBiteDamage(
                    this,
                    plant,
                    tick,
                    damage
            );
        }
        plant.receiveHit(PlantHitSource.BITE, this, damage);
    }

    private void resetBiteTarget() {
        biteTarget = null;
        nextBiteTick = Long.MAX_VALUE;
    }

    private void notifyPositionChanged() {
        long tick = currentTick();
        if (isHostile()) {
            for (ZombieBehavior behavior : behaviors) {
                behavior.onPositionChanged(this, world, tick);
            }
        }

        if (world != null && !isDead() && isHostile()) {
            world.notifyHostilePresentAt(
                    getTileX(),
                    getTileY(),
                    tick
            );
        }
    }

    private void requireSpawned() {
        if (world == null) {
            throw new IllegalStateException("zombie is not spawned");
        }
    }

    @Override
    protected void onDeath() {
        if (world == null || exitedWorld) {
            return;
        }

        world.recordZombieDefeated(this);

        for (ZombieBehavior behavior : behaviors) {
            behavior.onDeath(this, world, currentTick());
        }

        GameEvents.publish(
                "Zombie of type " + name + " is dead at ("
                        + getTileX() + ", "
                        + getTileY() + ")"
        );

        world.resolveZombieDeathDrops(this);
        world.removeZombie(this);
        world.game().unregister(this);
    }
}
