package pvz.model.entity.plant;

import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.LivingEntity;
import pvz.model.entity.plant.behavior.PlantBehavior;
import pvz.model.entity.plant.lifetime.PlantLifetimeController;
import pvz.model.entity.plant.plantfood.PlantFoodController;
import pvz.model.entity.plant.lifecycle.PlantLifecycle;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.plant.lifecycle.PlantRemovalResult;
import pvz.model.entity.plant.behavior.PlantBehaviorFactory;
import pvz.model.entity.plant.behavior.PlantPlacementContext;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.plant.behavior.capability.SunProductionCapability;
import pvz.model.entity.plant.hit.PlantHitContext;
import pvz.model.entity.plant.hit.PlantHitSource;
import pvz.model.entity.plant.placement.PlantPlacementTarget;
import pvz.model.entity.zombie.Zombie;

public class Plant extends LivingEntity {
    public static final int FULL_FREEZE_LEVEL = 3;

    private final PlantSpec spec;

    private final PlantFoodController plantFoodController;
    private final PlantLifecycle lifecycle = new PlantLifecycle();

    private World world;
    private int column;
    private int row;

    private long lastActionTick;
    private long lastActionStartedTick = Long.MIN_VALUE;
    private final long actionIntervalTicks;

    private final PlantBehavior behavior;

    private final PlantFoodCapability plantFoodCapability;

    private final SunProductionCapability sunProductionCapability;

    private final PlantLifetimeController lifetimeController;

    private boolean removedFromWorld;
    private final Set<Object> actionBlockers = new HashSet<>();
    private int freezeLevel;

    private final PlantHitPipeline hitPipeline;

    public Plant(PlantSpec spec) {
        this.spec = Objects.requireNonNull(spec, "plant spec cannot be null");

        this.health = spec.getBaseHp();
        this.name = spec.getName();

        this.actionIntervalTicks = (long) (spec.getActionInterval() * Game.TICKS_PER_SECOND);

        this.lifetimeController = PlantLifetimeController.from(spec);

        this.behavior = PlantBehaviorFactory.create(this, spec);

        this.plantFoodCapability = PlantCapabilities.plantFood(behavior);

        this.sunProductionCapability =
                PlantCapabilities.sunProduction(behavior);

        this.hitPipeline = new PlantHitPipeline(this, behavior);

        this.plantFoodController =
                new PlantFoodController(
                        name,
                        lifecycle,
                        plantFoodCapability,
                        () -> world != null && !removedFromWorld,
                        this::isPlantFoodActivationAllowed,
                        this::prepareForPlantFood
                );
    }

    public void place(
            World world,
            int column,
            int row,
            long currentTick
    ) {
        if (this.world != null) {
            throw new IllegalStateException(name + " is already placed");
        }

        PlantPlacementContext placementContext =
                new PlantPlacementContext(
                        this,
                        world,
                        column,
                        row,
                        currentTick
                );

        this.world = placementContext.world();
        this.column = placementContext.column();
        this.row = placementContext.row();
        this.lastActionTick = placementContext.placedTick();

        behavior.onPlaced(placementContext);

        lifetimeController.resetAt(placementContext.placedTick());
    }
    // sun
    public boolean hasPendingSuns() {
        return (sunProductionCapability != null) &&
                (sunProductionCapability.hasPendingSuns());
    }

    public void onProducedSunRemoved() {
        if (sunProductionCapability == null) {
            throw new IllegalStateException(
                    name + " does not have a sun production capability"
            );
        }

        sunProductionCapability.onProducedSunRemoved();
    }

    //plantfood
    public boolean supportsPlantFood() {
        return plantFoodController.supportsPlantFood();
    }

    public boolean canBeAffectedBy(PlantThreat threat) {
        Objects.requireNonNull(threat, "plant threat cannot be null");

        if (removedFromWorld) {
            return false;
        }

        if (!actionBlockers.isEmpty()
                && (threat == PlantThreat.DAMAGE
                || threat == PlantThreat.INSTANT_DESTROY)) {
            return false;
        }

        if (PlantCapabilities.blocksThreatDuringActivation(
                behavior,
                threat
        )) {
            return false;
        }

        if (world == null) {
            return true;
        }

        return lifecycle.allows(threat, world.game().getCurrentTick());
    }

    public boolean isPlantFoodActive(long currentTick) {
        return plantFoodController.isActive(currentTick);
    }

    public boolean canApplyPlantFood(long currentTick) {
        return plantFoodController.canActivate(currentTick);
    }

    public boolean tryApplyPlantFood(long currentTick) {
        boolean activated = plantFoodController.tryActivate(currentTick);

        if (!activated
                || plantFoodCapability == null
                || !plantFoodCapability
                        .targetsMatchingPlantsOnBoard()) {
            return activated;
        }

        activatePlantFoodForMatchingPlants(currentTick);

        return true;
    }

    private boolean isPlantFoodActivationAllowed() {
        return freezeLevel == 0
                && world != null
                && !world.board().isPlantCovered(this)
                && !PlantCapabilities.blocksPlantFoodDuringActivation(
                        behavior
                );
    }

    private void activatePlantFoodForMatchingPlants(
            long currentTick
    ) {
        for (Plant plant : world.getPlants()) {
            if (plant == this
                    || plant.spec.getId() != spec.getId()) {
                continue;
            }

            plant.plantFoodController.tryActivate(currentTick);
        }
    }

    private void prepareForPlantFood(long currentTick, long durationTicks) {
        long effectEndTick = currentTick + durationTicks;

        health = spec.getBaseHp();

        lastActionTick = effectEndTick;

        lifetimeController.resetAt(effectEndTick);
    }

    //Lifetime
    private boolean removeIfExpired(long tick) {
        if (!lifetimeController.isExpired(tick)) {
            return false;
        }

        tryRemove(
                PlantThreat.EXPIRATION,
                "Plant "
                        + name
                        + " at ("
                        + column
                        + ", "
                        + row
                        + ") expired."
        );

        return true;
    }

    // getters
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

    /** Read-only action state used by battle presentation code. */
    public boolean hasOngoingAction() {
        return behavior.hasOngoingAction();
    }

    /** Tick of the last real action start; placement does not count. */
    public long getLastActionStartedTick() {
        return lastActionStartedTick;
    }

    public boolean hasTag(PlantTag tag) {
        return spec.getTags().contains(tag);
    }

    public PlantStackingRole getStackingRole() {
        return spec.getStackingRole();
    }

    public boolean canBeEatenByZombie() {
        return PlantCapabilities.canBeEatenByZombie(behavior);
    }

    public boolean tryTriggerOnHostileContact(long currentTick) {
        if (removedFromWorld) {
            return false;
        }

        return PlantCapabilities.tryTriggerOnHostileContact(
                behavior,
                currentTick
        );
    }

    public boolean requiresTargetTile() {
        return PlantCapabilities.requiresTargetTile(behavior);
    }

    public boolean canTargetTile(PlantPlacementTarget target) {
        return PlantCapabilities.canTargetTile(behavior, target);
    }

    public <T> T behaviorCapability(Class<T> capabilityType) {
        Objects.requireNonNull(
                capabilityType,
                "capability type cannot be null"
        );

        return capabilityType.isInstance(behavior)
                ? capabilityType.cast(behavior)
                : null;
    }

    public boolean blocksVaulting() {
        return PlantCapabilities.blocksVaulting(behavior);
    }
    // remove damage death
    public boolean isRemovedFromWorld() {
        return removedFromWorld;
    }

    public boolean isZombieTargetable() {
        return !removedFromWorld && actionBlockers.isEmpty();
    }

    public boolean isActionBlocked() {
        return !actionBlockers.isEmpty();
    }

    public boolean addActionBlocker(Object source) {
        Objects.requireNonNull(source, "action blocker source cannot be null");
        if (!canBeAffectedBy(PlantThreat.ACTION_BLOCK)) {
            return false;
        }
        return actionBlockers.add(source);
    }

    public void removeActionBlocker(Object source) {
        actionBlockers.remove(Objects.requireNonNull(source));
    }

    public boolean addFreezeLevel(int fullFreezeLevel) {
        if (fullFreezeLevel <= 0) {
            throw new IllegalArgumentException(
                    "full freeze level must be positive"
            );
        }

        if (removedFromWorld
                || hasTag(PlantTag.FIRE)
                || freezeLevel >= fullFreezeLevel
                || !canBeAffectedBy(PlantThreat.FREEZE)) {
            return false;
        }

        freezeLevel++;
        return true;
    }

    public int getFreezeLevel() {
        return freezeLevel;
    }

    public void clearFreezeLevels() {
        freezeLevel = 0;
    }

    public boolean tryRelocate(int newColumn, int newRow) {
        if (world == null
                || removedFromWorld
                || !canBeAffectedBy(PlantThreat.FORCED_RELOCATION)) {
            return false;
        }
        if (!world.board().movePlant(
                column,
                row,
                newColumn,
                newRow,
                this
        )) {
            return false;
        }
        column = newColumn;
        row = newRow;
        return true;
    }

    public PlantRemovalResult tryRemove(PlantThreat threat) {
        return tryRemove(threat, null);
    }

    private PlantRemovalResult tryRemove(PlantThreat threat, String eventMessage) {
        Objects.requireNonNull(threat, "plant threat cannot be null");

        if (world == null || removedFromWorld) {
            return PlantRemovalResult.ALREADY_REMOVED;
        }

        if (!canBeAffectedBy(threat)) {
            if (PlantCapabilities.blocksThreatDuringActivation(
                    behavior,
                    threat
            )) {
                return PlantRemovalResult.BLOCKED_BY_ACTIVATION;
            }

            return PlantRemovalResult.BLOCKED_BY_PLANT_FOOD;
        }

        if (threat.setsHealthToZeroOnRemoval()) {
            health = 0;
        }

        finishRemoval(threat, eventMessage);

        return PlantRemovalResult.REMOVED;
    }

    private void finishRemoval(
            PlantThreat threat,
            String eventMessage
    ) {
        removedFromWorld = true;

        boolean removedWaterPlatform = getStackingRole()
                == PlantStackingRole.WATER_PLATFORM;

        world.board().detachPlant(column, row, this);

        world.game().unregister(this);

        behavior.onRemoved(threat);

        if (removedWaterPlatform) {
            world.removePlantsUnsupportedByWaterPlatform(column, row);
        }

        if (eventMessage != null && !eventMessage.isBlank()) {
            GameEvents.publish(eventMessage);
        }
    }

    @Override
    protected boolean canTakeDamage() {
        return canBeAffectedBy(PlantThreat.DAMAGE);
    }

    @Override
    protected double modifyIncomingDamage(double damage) {
        return hitPipeline.modifyIncomingDamage(damage);
    }

    public boolean receiveHit(
            PlantHitSource source,
            Zombie attacker,
            double damage
    ) {
        return hitPipeline.receiveHit(
                source,
                attacker,
                damage,
                currentTick()
        );
    }

    public double getArmorHealth() {
        return PlantCapabilities.armorHealth(behavior);
    }

    public double getArmorCapacity() {
        return PlantCapabilities.armorCapacity(behavior);
    }

    public boolean hasIntactArmor() {
        return getArmorHealth() > 0;
    }

    private long currentTick() {
        return world == null ? 0 : world.game().getCurrentTick();
    }

    @Override
    protected void onDeath() {
        tryRemove(
                PlantThreat.DAMAGE,
                "Plant "
                        + name
                        + " at ("
                        + column
                        + ", "
                        + row
                        + ") is destroyed."
        );
    }
    //update
    @Override
    public void update(long tick) {
        if (world == null || removedFromWorld) {
            return;
        }

        if (PlantCapabilities.updateTransientEffectIfActive(
                behavior,
                tick
        )) {
            return;
        }

        if (isPlantFoodActive(tick)) {
            return;
        }

        if (removeIfExpired(tick)) {
            return;
        }

        if (isActionBlocked()
                || world.board().isPlantCovered(this)) {
            return;
        }

        if (behavior.hasOngoingAction()) {
            behavior.updateOngoingAction(tick);
            return;
        }

        boolean intrinsicTiming =
                PlantCapabilities.usesIntrinsicActionTiming(behavior);

        if (!intrinsicTiming && actionIntervalTicks <= 0) {
            return;
        }

        long minimumInterval = intrinsicTiming ? 1 : actionIntervalTicks;
        if (tick - lastActionTick < minimumInterval) {
            return;
        }

        if (!behavior.canStartAction(tick)) {
            return;
        }

        lastActionTick = tick;
        behavior.startAction(tick);
        lastActionStartedTick = tick;
    }
}
