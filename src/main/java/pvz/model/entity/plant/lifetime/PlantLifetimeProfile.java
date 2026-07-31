package pvz.model.entity.plant.lifetime;

public record PlantLifetimeProfile(
        long lifespanTicks
) {
    public PlantLifetimeProfile {
        if (lifespanTicks <= 0) {
            throw new IllegalArgumentException(
                    "plant lifespan must be positive"
            );
        }
    }
}
