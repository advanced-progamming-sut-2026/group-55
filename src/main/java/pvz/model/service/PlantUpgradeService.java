package pvz.model.service;

import java.util.Objects;

import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.level.PlantLevelCost;
import pvz.model.entity.plant.level.PlantLevelCostTable;

public final class PlantUpgradeService {

    public enum Result {
        SUCCESS,
        NOT_OWNED,
        MAX_LEVEL,
        NOT_ENOUGH_COINS,
        NOT_ENOUGH_SEEDS
    }

    private final PlantLevelCostTable costTable;

    public PlantUpgradeService(PlantLevelCostTable costTable) {
        this.costTable = Objects.requireNonNull(
                costTable,
                "plant level cost table cannot be null"
        );
    }

    public PlantLevelCost nextCost(PlayerPlant playerPlant) {
        Objects.requireNonNull(playerPlant, "player plant cannot be null");
        if (playerPlant.getLevel() >= PlantSpec.MAX_LEVEL) {
            throw new IllegalStateException("plant is already at maximum level");
        }
        return costTable.forTargetLevel(playerPlant.getLevel() + 1);
    }

    public Result upgrade(User user, String plantName) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(plantName, "plant name cannot be null");

        PlayerPlant playerPlant = user.getOwnedPlant(plantName);
        if (playerPlant == null) {
            return Result.NOT_OWNED;
        }
        if (playerPlant.getLevel() >= PlantSpec.MAX_LEVEL) {
            return Result.MAX_LEVEL;
        }

        PlantLevelCost cost = nextCost(playerPlant);
        if (user.getCoins() < cost.coins()) {
            return Result.NOT_ENOUGH_COINS;
        }
        if (playerPlant.getSeedPackets() < cost.seedPackets()) {
            return Result.NOT_ENOUGH_SEEDS;
        }

        if (!user.spendCoins(cost.coins())) {
            throw new IllegalStateException("coin balance changed during plant upgrade");
        }
        if (!playerPlant.spendSeedPackets(cost.seedPackets())) {
            throw new IllegalStateException("seed packet balance changed during plant upgrade");
        }
        playerPlant.upgrade();
        return Result.SUCCESS;
    }
}
