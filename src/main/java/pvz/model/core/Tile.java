package pvz.model.core;

import java.util.ArrayList;
import java.util.List;
import pvz.model.entity.plant.Plant;

public class Tile {
    private TileType type;
    private double health;
    private final List<Plant> plants = new ArrayList<>();

    public Tile(TileType type) {
        this.type = type;
        if (type == TileType.TOMBSTONE) health = 700;
        if (type == TileType.FROZEN) health = 600;
    }

    public TileType getType() { return type; }
    public List<Plant> getPlants() { return plants; }

    public boolean isPlantable() {
        return type == TileType.NORMAL
                || type == TileType.LOW_BEACH
                || type == TileType.NECROMANCY;
    }
}