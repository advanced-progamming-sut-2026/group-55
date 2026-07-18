package pvz.model.entity.zombie;

public final class BasicZombie extends Zombie {
    private static final double HEALTH = 200;
    private static final double TILES_PER_SECOND = 0.2;
    private static final double DAMAGE_PER_SECOND = 100;

    public BasicZombie() {
        super("normal", HEALTH, TILES_PER_SECOND, DAMAGE_PER_SECOND);
    }
}
