package pvz.model.entity.plant;

import pvz.model.entity.Entity;

import java.util.Set;

public class Plant extends Entity {
    private final PlantSpec spec;
    public Plant(PlantSpec spec) {
        this.spec = spec;
        this.health = spec.getBaseHp();
        this.name = spec.getName();
    }
    public void takeDamage(double damage){
        health -= damage;
    }
    public boolean isDead(){
        return health <= 0;
    }
    @Override
    public void update(long tick) {
        // baadan por mishe
    }

    public boolean hasTag(PlantTag plantTag) {
        Set<PlantTag> tags = this.spec.getTags();
        return tags.contains(plantTag);
    }
}