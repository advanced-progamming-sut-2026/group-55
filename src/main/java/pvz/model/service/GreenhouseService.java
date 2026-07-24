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

public class GreenhouseService {

    private final Random random = new Random();

    private static final long MARIGOLD_GROW_TIME = 2 * 60 * 60 * 1000L;
    private static final long NORMAL_PLANT_GROW_TIME = 8 * 60 * 60 * 1000L;
    private static final int UNLOCK_COST = 1000; // هزینه باز کردن قفل گلدان (همینجوری)

    private final PlantData plantData;

    public GreenhouseService(PlantData plantData) {
        this.plantData = plantData;
    }

    public void plant(User user, int x, int y) throws Exception {
        Greenhouse greenhouse = user.getGreenhouse();
        greenhouse.updateAllPots();

        Pot pot = greenhouse.getPot(x, y);
        if (pot == null) throw new Exception(SystemMessage.GREENHOUSE_INVALID_COORDINATES.getMessage());
        if (pot.isLocked()) throw new Exception(SystemMessage.GREENHOUSE_LOCKED.getMessage());
        if (!pot.isEmpty()) throw new Exception(SystemMessage.GREENHOUSE_NOT_EMPTY.getMessage());

        // شانس ۵۰-۵۰
        boolean isMarigold = random.nextBoolean();
        String plantName;
        long growTime;

        if (isMarigold) {
            plantName = "marigold";
            growTime = MARIGOLD_GROW_TIME;
        } else {
            List<PlayerPlant> validPlants = user.getUnlockedPlants().stream()
                    .filter(p -> {
                        PlantSpec spec = plantData.byName().get(p.getPlantName().toLowerCase());
                        return spec != null && spec.getPlantFoodEffect() != null
                                && !spec.getPlantFoodEffect().trim().isEmpty()
                                && !spec.getPlantFoodEffect().equalsIgnoreCase("none");
                    })
                    .toList();

            if (validPlants.isEmpty()) throw new Exception(SystemMessage.PLANT_SELECTION_NO_PLANTS.getMessage());

            PlayerPlant randomPlant = validPlants.get(random.nextInt(validPlants.size()));
            plantName = randomPlant.getPlantName();
            growTime = NORMAL_PLANT_GROW_TIME;
        }

        GreenhousePlant newPlant = new GreenhousePlant(plantName, isMarigold, growTime);
        pot.setPlant(newPlant);
    }

    public void collect(User user, int x, int y) throws Exception {
        Greenhouse greenhouse = user.getGreenhouse();
        greenhouse.updateAllPots();

        Pot pot = greenhouse.getPot(x, y);
        if (pot == null) throw new Exception(SystemMessage.GREENHOUSE_INVALID_COORDINATES.getMessage());
        if (pot.getState() == PotState.EMPTY || pot.getState() == PotState.LOCKED) {
            throw new Exception(SystemMessage.GREENHOUSE_EMPTY.getMessage());
        }
        if (pot.getState() == PotState.GROWING) {
            throw new Exception(SystemMessage.GREENHOUSE_NOT_READY.getMessage());
        }

        GreenhousePlant plant = pot.getPlant();
        if (plant.isMarigold()) {
            user.addCoins(500);
        } else {
            user.addBoost(plant.getPlantName());
        }

        pot.clear();
    }

    public void forceGrow(User user, int x, int y) throws Exception {
        Greenhouse greenhouse = user.getGreenhouse();
        greenhouse.updateAllPots();

        Pot pot = greenhouse.getPot(x, y);
        if (pot == null) throw new Exception(SystemMessage.GREENHOUSE_INVALID_COORDINATES.getMessage());
        if (pot.getState() != PotState.GROWING) {
            throw new Exception(SystemMessage.GREENHOUSE_NOT_GROWING.getMessage());
        }

        GreenhousePlant plant = pot.getPlant();
        int remainingHours = plant.getRemainingHours();

        if (remainingHours <= 0) {
            throw new Exception(SystemMessage.GREENHOUSE_ALREADY_READY.getMessage());
        }

        if (!user.spendDiamonds(remainingHours)) {
            throw new Exception(SystemMessage.GREENHOUSE_NOT_ENOUGH_DIAMONDS.getMessage());
        }

        plant.forceReady();
        pot.updateState();
    }

    public void unlockPot(User user, int x, int y) throws Exception {
        Greenhouse greenhouse = user.getGreenhouse();
        Pot pot = greenhouse.getPot(x, y);

        if (pot == null) throw new Exception(SystemMessage.GREENHOUSE_INVALID_COORDINATES.getMessage());
        if (!pot.isLocked()) throw new Exception(SystemMessage.GREENHOUSE_ALREADY_UNLOCKED.getMessage());

        if (!user.spendCoins(UNLOCK_COST)) {
            throw new Exception(SystemMessage.GREENHOUSE_NOT_ENOUGH_COINS.getMessage());
        }

        pot.unlock();
    }
}