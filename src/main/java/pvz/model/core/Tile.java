package pvz.model.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantTag;

public final class Tile {
    private TileType type;

    int x; int y;
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

    public void setType(TileType newType) {
        type = Objects.requireNonNull(newType, "tile type cannot be null");
        health = type.getInitialHealth();
    }

    public boolean isPlantableFor(Plant plant) {
        Objects.requireNonNull(plant, "plant cannot be null");
        if (type.isNormallyPlantable()) {
            return true;
        }
        if (type == TileType.WATER) {
            return plant.hasTag(PlantTag.WATER) || hasLilyPad();
        }
        return false;
    }
    public boolean canStack(Plant newPlant) {
        if (plants.isEmpty()) {
            return true;
        }
        boolean onlyLilyPadHere = type == TileType.WATER && hasLilyPad() && plants.size() == 1;
        if (onlyLilyPadHere && !isLilyPad(newPlant)) {
            return true; // planting one plant on top of a lily pad
        }
        if (isPeaPod(newPlant)) {
            long peaPodCount = plants.stream().filter(this::isPeaPod).count();
            boolean othersAreOk = plants.stream()
                    .allMatch(plant -> isPeaPod(plant) || isLilyPad(plant));
            return othersAreOk && peaPodCount < 5;
        }
        if (isPumpkin(newPlant)) {
            return !hasPumpkin(plants);
        }
        return false;
    }

    /// canStack helpier method
    private boolean isLilyPad(Plant plant) {
        return plant.getName().equalsIgnoreCase("Lily Pad");
    }

    private boolean isPeaPod(Plant plant) {
        return plant.getName().equalsIgnoreCase("Pea Pod");
    }

    private boolean hasLilyPad() {
        return plants.stream().anyMatch(plant -> plant.getName().equalsIgnoreCase("Lily Pad"));
    }

    private boolean isPumpkin(Plant plant) {
        return plant.getName().equalsIgnoreCase("Pumpkin");
    }

    private boolean hasPumpkin(List<Plant> plants) {
        return plants.stream().anyMatch(plant -> plant.getName().equalsIgnoreCase("Pumpkin"));
    }
    //////////////////////////////////////////////////////

    public void addPlant(Plant plant) {
        plants.add(Objects.requireNonNull(plant, "plant cannot be null"));
    }

    public boolean removePlant(Plant plant) {
        return plants.remove(plant);
    }

    public boolean hasFirePlant() {
        return plants.stream().anyMatch(plant -> plant.hasTag(PlantTag.FIRE));
    }

    public boolean blocksStraightProjectiles() {
        return type.blocksStraightProjectiles();
    }

    public boolean takeDamage(double damage) {
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

    public boolean applyFireDamage(double damage) {
        if (type != TileType.FROZEN) {
            return false;
        }
        return takeDamage(damage);
    }

    private void publishDestroyedMessage() {
        String message = switch (type) {
            case TOMBSTONE -> "the tombstone at (" + x + ", " + y + ") is destroyed";
            case FROZEN -> "the frozen tile at (" + x + ", " + y + ") melted";
            default -> "tile at (" + x + ", " + y + ") is destroyed";
        };
        GameEvents.publish(message);
    }
}
