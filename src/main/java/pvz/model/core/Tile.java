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

    public List<Plant> getPlants() { return List.copyOf(plants); }

    public void addPlant(Plant plant){
        plants.add(plant);
    }

    public boolean removePlant(Plant plant) {
        return plants.remove(plant);
    }

    public boolean isPlantable() {
        return type == TileType.NORMAL
                || type == TileType.LOW_BEACH
                || type == TileType.NECROMANCY;
    }
    public void takeDamage(double damage) {
        if (type == TileType.FROZEN ||  type == TileType.TOMBSTONE) {
            this.health -= damage;
            if (this.health <= 0) {
                this.type = TileType.NORMAL;
            }
        }
    }
}