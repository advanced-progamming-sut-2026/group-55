package pvz.model.entity.plant;

import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.LivingEntity;
import pvz.model.entity.plant.behavior.PlantBehavior;
import pvz.model.entity.plant.lifetime.PlantLifetimeProfile;
import pvz.model.entity.plant.lifetime.PlantLifetimeProfiles;
import pvz.model.entity.plant.plantfood.PlantFoodEffect;
import pvz.model.entity.plant.plantfood.PlantFoodEffects;
import pvz.model.entity.plant.lifecycle.PlantLifecycle;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.plant.lifecycle.PlantRemovalResult;
import pvz.model.entity.plant.behavior.PlantBehaviorFactory;
import pvz.model.entity.plant.behavior.PlantPlacementContext;

public class Plant extends LivingEntity {
    private final PlantSpec spec;

    private final PlantFoodEffect plantFoodEffect;
    private final PlantLifecycle lifecycle = new PlantLifecycle();

    private World world;
    private int column;
    private int row;

    private long lastActionTick;
    private final long actionIntervalTicks;

    private final PlantBehavior behavior;

    //lifetime
    private final PlantLifetimeProfile lifetimeProfile;

    private long expirationTick = Long.MAX_VALUE;
    private boolean removedFromWorld;

    public Plant(PlantSpec spec) {
        this.spec = Objects.requireNonNull(spec, "plant spec cannot be null");

        this.plantFoodEffect = PlantFoodEffects.from(spec);

        this.health = spec.getBaseHp();
        this.name = spec.getName();

        this.actionIntervalTicks = (long) (spec.getActionInterval() * Game.TICKS_PER_SECOND);

        this.lifetimeProfile = PlantLifetimeProfiles.from(spec);

        this.behavior = PlantBehaviorFactory.create(this, spec);
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

        resetLifetime(placementContext.placedTick());
    }
    // sun
    public boolean hasPendingSuns() {
        return behavior.hasPendingSuns();
    }

    public void onProducedSunRemoved() {
        behavior.onProducedSunRemoved();
    }

    //plantfood
    public boolean supportsPlantFood() {
        return plantFoodEffect != null;
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

    public void applyBehaviorPlantFood(long currentTick, long durationTicks) {
        behavior.applyPlantFood(currentTick, durationTicks);
    }

    public boolean isPlantFoodActive(long currentTick) {
        return lifecycle.isPlantFoodActive(currentTick);
    }

    public boolean tryApplyPlantFood(long currentTick) {
        if (!supportsPlantFood()) {
            throw new IllegalStateException(
                    name + " does not have a plant food effect"
            );
        }

        if (world == null) {
            throw new IllegalStateException(
                    name + " must be placed before applying plant food"
            );
        }

        long durationTicks = PlantFoodEffects.durationTicks(spec);

        boolean activated = lifecycle.tryActivatePlantFood(currentTick, durationTicks);

        if (!activated) {
            return false;
        }

        prepareForPlantFood(currentTick, durationTicks);

        plantFoodEffect.apply(this, currentTick, durationTicks);

        return true;
    }

    private void prepareForPlantFood(long currentTick, long durationTicks) {
        long effectEndTick = currentTick + durationTicks;

        health = spec.getBaseHp();

        behavior.onPlantFoodStarted(currentTick, durationTicks);

        lastActionTick = effectEndTick;

        resetLifetime(effectEndTick);
    }

    //Lifetime
    private void resetLifetime(long currentTick) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        if (lifetimeProfile == null) {
            expirationTick = Long.MAX_VALUE;
            return;
        }

        expirationTick = currentTick + lifetimeProfile.lifespanTicks();
    }

    private boolean expireIfNeeded(long tick) {
        if (lifetimeProfile == null) {
            return false;
        }

        if (tick < expirationTick) {
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

        if (lifecycle.isPlantFoodActive(tick)) {
            return;
        }

        if (expireIfNeeded(tick)) {
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
