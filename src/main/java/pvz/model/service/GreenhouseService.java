package pvz.model.service;

import pvz.data.PlantData;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.greenhouse.Greenhouse;
import pvz.model.greenhouse.GreenhousePlant;
import pvz.model.greenhouse.Pot;
import pvz.model.greenhouse.PotState;
import pvz.model.utils.SystemMessage;
import java.util.List;
import java.util.Random;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;

public class GreenhouseService {
    private static final long MARIGOLD_GROW_TIME = 7200000L; // 2 hours
    private static final long NORMAL_PLANT_GROW_TIME = 28800000L; // 8 hours
    private static final int UNLOCK_COST_DIAMONDS = 20;
    private final Random random = new Random();
    private final PlantData plantData;

    public GreenhouseService(PlantData plantData) {
        this.plantData = plantData;
    }

    public void plant(User user, int x, int y) throws Exception {
        Greenhouse greenhouse = user.getGreenhouse();
        greenhouse.updateAllPots();
        Pot pot = greenhouse.getPot(x, y);

        validatePotForPlanting(pot);

        if (random.nextBoolean()) {
            pot.setPlant(new GreenhousePlant("marigold", true, MARIGOLD_GROW_TIME));
        } else {
            plantRandomBoostPlant(user, pot);
        }
    }

    public void collect(User user, int x, int y) throws Exception {
        Greenhouse greenhouse = user.getGreenhouse();
        greenhouse.updateAllPots();
        Pot pot = greenhouse.getPot(x, y);

        if (pot == null)
            throw new Exception(SystemMessage.GREENHOUSE_INVALID_COORDINATES.getMessage());
        if (pot.isLocked())
            throw new Exception(SystemMessage.GREENHOUSE_LOCKED.getMessage());
        if (pot.isEmpty())
            throw new Exception(SystemMessage.GREENHOUSE_EMPTY.getMessage());
        if (pot.getState() == PotState.GROWING)
            throw new Exception(SystemMessage.GREENHOUSE_NOT_READY.getMessage());

        GreenhousePlant plant = pot.getPlant();

        if (plant.isMarigold()) {
            user.addCoins(500);
        } else {
            if (!user.hasStoredBoost(plant.getPlantName())) {
                user.addStoredBoost(plant.getPlantName());
            }
        }

        pot.clear();
    }

    public void forceGrow(User user, int x, int y) throws Exception {
        Pot pot = user.getGreenhouse().getPot(x, y);

        if (pot == null)
            throw new Exception(SystemMessage.GREENHOUSE_INVALID_COORDINATES.getMessage());
        if (pot.isLocked())
            throw new Exception(SystemMessage.GREENHOUSE_LOCKED.getMessage());
        if (pot.isEmpty() || pot.getPlant() == null)
            throw new Exception(SystemMessage.GREENHOUSE_NOT_GROWING.getMessage());

        int hours = pot.getPlant().getRemainingHours();
        if (hours <= 0) {
            throw new Exception(SystemMessage.GREENHOUSE_ALREADY_READY.getMessage());
        }

        if (!user.spendDiamonds(hours)) {
            throw new Exception(SystemMessage.GREENHOUSE_NOT_ENOUGH_DIAMONDS.getMessage());
        }

        pot.getPlant().forceReady();
        user.getGreenhouse().updateAllPots();
    }

    public void unlockPot(User user, int x, int y) throws Exception {
        Greenhouse greenhouse = user.getGreenhouse();
        greenhouse.updateAllPots();
        Pot pot = greenhouse.getPot(x, y);

        if (pot == null)
            throw new Exception(SystemMessage.GREENHOUSE_INVALID_COORDINATES.getMessage());
        if (!pot.isLocked())
            throw new Exception("Pot is already unlocked.");

        if (!user.spendDiamonds(UNLOCK_COST_DIAMONDS)) {
            throw new Exception(SystemMessage.GREENHOUSE_NOT_ENOUGH_DIAMONDS.getMessage());
        }

        pot.unlock();
        greenhouse.updateAllPots();
    }

    private void validatePotForPlanting(Pot pot) throws Exception {
        if (pot == null) throw new Exception(SystemMessage.GREENHOUSE_INVALID_COORDINATES.getMessage());
        if (pot.isLocked()) throw new Exception(SystemMessage.GREENHOUSE_LOCKED.getMessage());
        if (!pot.isEmpty()) throw new Exception(SystemMessage.GREENHOUSE_NOT_EMPTY.getMessage());
    }

    private void plantRandomBoostPlant(User user, Pot pot) throws Exception {
        List<PlayerPlant> validPlants = user.getUnlockedPlants().stream()
                .filter(p -> {
                    PlantSpec spec = plantData.byName().get(p.getPlantName().toLowerCase());
                    return spec != null && PlantFoodSupport.isImplemented(spec);
                }).toList();

        if (validPlants.isEmpty()) {
            pot.setPlant(new GreenhousePlant("marigold", true, MARIGOLD_GROW_TIME));
            return;
        }

        PlayerPlant p = validPlants.get(random.nextInt(validPlants.size()));
        pot.setPlant(new GreenhousePlant(p.getPlantName(), false, NORMAL_PLANT_GROW_TIME));
    }
}
