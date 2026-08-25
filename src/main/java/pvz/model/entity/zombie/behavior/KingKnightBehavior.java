package pvz.model.entity.zombie.behavior;

import java.util.List;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.zombie.ArmorSpec;
import pvz.model.entity.zombie.Zombie;

public final class KingKnightBehavior implements ZombieBehavior {
    private final String eligibleZombieId;
    private final int columnRange;
    private final int rowRange;
    private final long castIntervalTicks;
    private final String crownArmorId;
    private final String crownArmorName;
    private final double crownHealth;
    private final boolean crownMetallic;
    private final String shoulderArmorId;
    private final String shoulderArmorName;
    private final double shoulderHealth;
    private final boolean shoulderMetallic;
    private long nextCastTick;

    public KingKnightBehavior(
            String eligibleZombieId,
            int columnRange,
            int rowRange,
            double castIntervalSeconds,
            String crownArmorId,
            String crownArmorName,
            double crownHealth,
            boolean crownMetallic,
            String shoulderArmorId,
            String shoulderArmorName,
            double shoulderHealth,
            boolean shoulderMetallic
    ) {
        this.eligibleZombieId = eligibleZombieId;
        this.columnRange = columnRange;
        this.rowRange = rowRange;
        this.castIntervalTicks = Math.max(
                1,
                (long) Math.ceil(castIntervalSeconds * Game.TICKS_PER_SECOND)
        );
        this.crownArmorId = crownArmorId;
        this.crownArmorName = crownArmorName;
        this.crownHealth = crownHealth;
        this.crownMetallic = crownMetallic;
        this.shoulderArmorId = shoulderArmorId;
        this.shoulderArmorName = shoulderArmorName;
        this.shoulderHealth = shoulderHealth;
        this.shoulderMetallic = shoulderMetallic;
    }

    @Override
    public void onSpawn(Zombie zombie, World world, long currentTick) {
        nextCastTick = currentTick + castIntervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, World world, long currentTick) {
        if (currentTick < nextCastTick) {
            return;
        }
        nextCastTick = currentTick + castIntervalTicks;
        List<Zombie> candidates = world.getZombies().stream()
                .filter(candidate -> candidate != zombie)
                .filter(candidate -> eligibleZombieId.equals(
                        candidate.getSpec().getId()
                ))
                .filter(candidate -> !candidate.getArmorSet().hasArmor(
                        crownArmorId
                ))
                .filter(candidate -> Math.abs(candidate.getTileX() - zombie.getTileX())
                        <= columnRange)
                .filter(candidate -> Math.abs(candidate.getRow() - zombie.getRow())
                        <= rowRange)
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        Zombie target = candidates.get(world.randomInt(candidates.size()));
        double scale = target.getMaximumHealth()
                / target.getSpec().getHitpoints();
        target.addArmor(new ArmorSpec(
                crownArmorId,
                crownArmorName,
                crownHealth * scale,
                crownMetallic
        ));
        target.addArmor(new ArmorSpec(
                shoulderArmorId,
                shoulderArmorName,
                shoulderHealth * scale,
                shoulderMetallic
        ));
        GameEvents.publish("King promoted " + target.getName() + " to Knight.");
    }

    @Override
    public double modifyMovementMultiplier(
            Zombie zombie,
            World world,
            long currentTick,
            double multiplier
    ) {
        return 0;
    }
}
