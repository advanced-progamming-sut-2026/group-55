package pvz.model.entity;

public abstract class LivingEntity extends Entity {
    protected double health;

    private boolean deathHandled;

    public double getHealth() {
        return health;
    }

    public final void takeDamage(double damage) {
        if (damage <= 0 || deathHandled) {
            return;
        }

        health = Math.max(0, health - damage);

        if (health == 0) {
            deathHandled = true;
            onDeath();
        }
    }

    public final boolean isDead() {
        return health <= 0;
    }

    protected abstract void onDeath();
}