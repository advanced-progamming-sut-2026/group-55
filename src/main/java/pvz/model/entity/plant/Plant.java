package pvz.model.entity.plant;

import java.util.Objects;

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

public class Plant extends LivingEntity {
    private final PlantSpec spec;

    private final PlantFoodController plantFoodController;
    private final PlantLifecycle lifecycle = new PlantLifecycle();

    private World world;
    private int column;
    private int row;

    private long lastActionTick;
    private final long actionIntervalTicks;

    private final PlantBehavior behavior;

    private final SunProductionCapability sunProductionCapability;

    private final PlantLifetimeController lifetimeController;

    private boolean removedFromWorld;

    public Plant(PlantSpec spec) {
        this.spec = Objects.requireNonNull(spec, "plant spec cannot be null");

        this.health = spec.getBaseHp();
        this.name = spec.getName();

        this.actionIntervalTicks = (long) (spec.getActionInterval() * Game.TICKS_PER_SECOND);

        this.lifetimeController = PlantLifetimeController.from(spec);

        this.behavior = PlantBehaviorFactory.create(this, spec);

        PlantFoodCapability plantFoodCapability = resolvePlantFoodCapability(behavior);

        this.sunProductionCapability = resolveSunProductionCapability(behavior);

        this.plantFoodController =
                new PlantFoodController(
                        name,
                        lifecycle,
                        plantFoodCapability,
                        () -> world != null && !removedFromWorld,
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

    private static SunProductionCapability
    resolveSunProductionCapability(PlantBehavior behavior) {
        if (behavior instanceof SunProductionCapability capability) {
            return capability;
        }

        return null;
    }

    //plantfood
    private static PlantFoodCapability
    resolvePlantFoodCapability(PlantBehavior behavior) {
        if (!(behavior
                instanceof PlantFoodCapability capability)) {
            return null;
        }

        if (!capability.supportsPlantFood()) {
            return null;
        }

        return capability;
    }

    public boolean supportsPlantFood() {
        return plantFoodController.supportsPlantFood();
    }

    public boolean canBeAffectedBy(PlantThreat threat) {
        Objects.requireNonNull(threat, "plant threat cannot be null");

        if (removedFromWorld) {
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

    public boolean tryApplyPlantFood(long currentTick) {
        return plantFoodController.tryActivate(currentTick);
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

    public boolean hasTag(PlantTag tag) {
        if (spec.getTags().contains(tag)) {
            return true;
        }
        return false;
    }
    // remove damage death
    public boolean isRemovedFromWorld() {
        return removedFromWorld;
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
            return PlantRemovalResult.BLOCKED_BY_PLANT_FOOD;
        }

        if (threat.setsHealthToZeroOnRemoval()) {
            health = 0;
        }

        finishRemoval(eventMessage);

        return PlantRemovalResult.REMOVED;
    }

    private void finishRemoval(String eventMessage) {
        removedFromWorld = true;

        world.board().detachPlant(column, row, this);

        world.game().unregister(this);

        if (eventMessage != null && !eventMessage.isBlank()) {
            GameEvents.publish(eventMessage);
        }
    }

    @Override
    protected boolean canTakeDamage() {
        return canBeAffectedBy(PlantThreat.DAMAGE);
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
        if (world == null) {
            return;
        }

        if (isPlantFoodActive(tick)) {
            return;
        }

        if (removeIfExpired(tick)) {
            return;
        }

        if (behavior.hasOngoingAction()) {
            behavior.updateOngoingAction(tick);
            return;
        }

        if (actionIntervalTicks <= 0) {
            return;
        }

        if (tick - lastActionTick < actionIntervalTicks) {
            return;
        }

        if (!behavior.canStartAction(tick)) {
            return;
        }

        lastActionTick = tick;

        behavior.startAction(tick);
    }
}
