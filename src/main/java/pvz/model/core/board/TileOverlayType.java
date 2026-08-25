package pvz.model.core.board;

public enum TileOverlayType {
    FROZEN(600, true, true),
    OCTOPUS(600, true, true);

    private final double initialHealth;
    private final boolean blocksStraightProjectiles;
    private final boolean blocksPlantActions;

    TileOverlayType(
            double initialHealth,
            boolean blocksStraightProjectiles,
            boolean blocksPlantActions
    ) {
        this.initialHealth = initialHealth;
        this.blocksStraightProjectiles = blocksStraightProjectiles;
        this.blocksPlantActions = blocksPlantActions;
    }

    public double getInitialHealth() {
        return initialHealth;
    }

    public boolean blocksStraightProjectiles() {
        return blocksStraightProjectiles;
    }

    public boolean blocksPlantActions() {
        return blocksPlantActions;
    }
}
