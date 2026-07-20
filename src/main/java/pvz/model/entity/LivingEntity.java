package pvz.model.entity;

public abstract class LivingEntity extends Entity {
    protected double health;

    public double getHealth() {
        return health;
    }

    public void takeDamage(double damage) {
        health -= damage;
    }

    public boolean isDead() {
        return health <= 0;
    }
}
