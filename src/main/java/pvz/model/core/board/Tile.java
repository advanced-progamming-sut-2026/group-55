package pvz.model.core.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pvz.model.core.GameEvents;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantStackingRole;
import pvz.model.entity.plant.PlantTag;

public final class Tile {
    private static final int MAX_SELF_STACKING_PLANTS = 5;

    private TileType type;
    private final int x;
    private final int y;
    private double health;
    private final List<Plant> plants = new ArrayList<>();

    public Tile(TileType type, int x, int y) {
        setType(type);
        this.x = x;
        this.y = y;
    }

    public TileType getType() {
        return type;
    }

    public double getHealth() {
        return health;
    }

    public List<Plant> getPlants() {
        return List.copyOf(plants);
    }

    void setType(TileType newType) {
        type = Objects.requireNonNull(
                newType,
                "tile type cannot be null"
        );
        health = type.getInitialHealth();
    }

    boolean isPlantableFor(Plant plant) {
        Objects.requireNonNull(plant, "plant cannot be null");

        if (type == TileType.WATER) {
            return plant.hasTag(PlantTag.WATER)
                    || hasWaterPlatform();
        }

        return type.isNormallyPlantable()
                && !plant.hasTag(PlantTag.WATER);
    }

    boolean canStack(Plant newPlant) {
        Objects.requireNonNull(newPlant, "plant cannot be null");

        if (plants.isEmpty()) {
            return true;
        }

        return switch (newPlant.getStackingRole()) {
            case WATER_PLATFORM -> false;
            case PROTECTIVE_COVER -> !hasProtectiveCover();
            case SELF_STACKING -> canAddSelfStackingPlant(newPlant);
            case NONE -> !hasMainPlant();
        };
    }

    void addPlant(Plant plant) {
        Plant checkedPlant = Objects.requireNonNull(
                plant,
                "plant cannot be null"
        );

        int insertionIndex = findInsertionIndex(
                checkedPlant.getStackingRole()
        );

        plants.add(insertionIndex, checkedPlant);
    }

    boolean removePlant(Plant plant) {
        return plants.remove(plant);
    }

    int countPlantsWithTag(PlantTag tag) {
        Objects.requireNonNull(tag, "plant tag cannot be null");

        return (int) plants.stream()
                .filter(plant -> plant.hasTag(tag))
                .count();
    }

    public boolean blocksStraightProjectiles() {
        return type.blocksStraightProjectiles();
    }

    boolean takeDamage(double damage) {
        if (damage <= 0 || !type.isDestructible()) {
            return false;
        }

        health = Math.max(0, health - damage);

        if (health == 0) {
            publishDestroyedMessage();
            setType(TileType.NORMAL);
            return true;
        }

        return false;
    }

    boolean applyFireDamage(double damage) {
        if (type != TileType.FROZEN) {
            return false;
        }

        return takeDamage(damage);
    }

    private boolean canAddSelfStackingPlant(Plant newPlant) {
        List<Plant> mainPlants = plants.stream()
                .filter(this::occupiesMainLayer)
                .toList();

        if (mainPlants.isEmpty()) {
            return true;
        }

        if (mainPlants.size() >= MAX_SELF_STACKING_PLANTS) {
            return false;
        }

        int newPlantId = newPlant.getSpec().getId();

        return mainPlants.stream().allMatch(
                plant -> plant.getStackingRole()
                        == PlantStackingRole.SELF_STACKING
                        && plant.getSpec().getId() == newPlantId
        );
    }

    private boolean hasMainPlant() {
        return plants.stream().anyMatch(this::occupiesMainLayer);
    }

    private boolean occupiesMainLayer(Plant plant) {
        PlantStackingRole role = plant.getStackingRole();

        return role == PlantStackingRole.NONE
                || role == PlantStackingRole.SELF_STACKING;
    }

    private boolean hasWaterPlatform() {
        return hasRole(PlantStackingRole.WATER_PLATFORM);
    }

    private boolean hasProtectiveCover() {
        return hasRole(PlantStackingRole.PROTECTIVE_COVER);
    }

    private boolean hasRole(PlantStackingRole role) {
        return plants.stream().anyMatch(
                plant -> plant.getStackingRole() == role
        );
    }

    private int findInsertionIndex(PlantStackingRole role) {
        if (role == PlantStackingRole.WATER_PLATFORM) {
            return 0;
        }

        if (role == PlantStackingRole.PROTECTIVE_COVER) {
            return plants.size();
        }

        for (int index = 0; index < plants.size(); index++) {
            if (plants.get(index).getStackingRole()
                    == PlantStackingRole.PROTECTIVE_COVER) {
                return index;
            }
        }

        return plants.size();
    }

    private void publishDestroyedMessage() {
        String message = switch (type) {
            case TOMBSTONE -> "the tombstone at ("
                    + x + ", " + y + ") is destroyed";
            case FROZEN -> "the frozen tile at ("
                    + x + ", " + y + ") melted";
            default -> "tile at ("
                    + x + ", " + y + ") is destroyed";
        };

        GameEvents.publish(message);
    }
}
