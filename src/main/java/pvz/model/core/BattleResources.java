package pvz.model.core;

public final class BattleResources {
    private static final int MAX_PLANT_FOOD = 3;

    private final SunBank sunBank;
    private final BattleWallet battleWallet;
    private int plantFoodCount;
    private boolean cooldownCheatEnabled;

    public BattleResources(int startingSun) {
        this.sunBank = new SunBank(startingSun);
        this.battleWallet = new BattleWallet();
        this.plantFoodCount = 0;
        this.cooldownCheatEnabled = false;
    }

    public SunBank sunBank() {
        return sunBank;
    }

    public BattleWallet battleWallet() {
        return battleWallet;
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public boolean tryAddPlantFood() {
        if (plantFoodCount < MAX_PLANT_FOOD) {
            plantFoodCount++;
            return true;
        }

        return false;

    }

    public boolean tryConsumePlantFood() {
        if (plantFoodCount > 0) {
            plantFoodCount--;
            return true;
        }

        return false;
    }

    public void enableCooldownCheat() {
        cooldownCheatEnabled = true;
    }

    public boolean isCooldownCheatEnabled() {
        return cooldownCheatEnabled;
    }
}
