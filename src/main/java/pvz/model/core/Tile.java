package pvz.model.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantTag;

public final class Tile {
    private TileType type;
    private double health;
    private final List<Plant> plants = new ArrayList<>();

    public Tile(TileType type) {
        setType(type);
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

    /** @return true when a destructible terrain object has just been destroyed. */
    public boolean takeDamage(double damage) {
        if (damage <= 0 || !type.isDestructible()) {
            return false;
        }
        health = Math.max(0, health - damage);
        if (health == 0) {
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

    private boolean hasLilyPad() {
        return plants.stream().anyMatch(plant -> plant.getName().equalsIgnoreCase("Lily Pad"));
    }
}
