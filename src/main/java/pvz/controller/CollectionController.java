package pvz.controller;

import pvz.data.PlantData;
import pvz.model.account.UserManager;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.command.CollectionCommand;
import pvz.model.command.Command;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.utils.AppState;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;
import java.util.Comparator;

public class CollectionController extends BaseController {

    private final PlantData plantData;

    public CollectionController(AppState appState, UserManager userManager, MenuView view, PlantData plantData) {
        super(appState, userManager, view);
        this.plantData = plantData;
    }

    @Override
    protected Message handleSpecificCommand(Command command) {
        if (!(command instanceof CollectionCommand cmd)) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }

        User currentUser = appState.getCurrentUser();

        switch (cmd.getAction()) {
            case SHOW_PLANTS -> handleShowPlants(currentUser);
            case SHOW_ALL_PLANTS -> handleShowAllPlants();
            case SHOW_PLANT_DETAILS -> handleShowPlantDetails(cmd);
            case PURCHASE_PLANT -> handlePurchasePlant(cmd, currentUser);
            case UPGRADE_PLANT -> handleUpgradePlant(cmd, currentUser);
            case SHOW_ALL_ZOMBIES, SHOW_ZOMBIES, SHOW_ZOMBIE_DETAILS -> view.showSuccess("Not implemented yet");
            default -> view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        }
        return null;
    }

    private void handleShowPlants(User user) {
        view.showSuccess(SystemMessage.COLLECTION_HEADER_YOUR_PLANTS.getMessage());
        user.getUnlockedPlants()
                .forEach(p -> view.showSuccess(p.getPlantName() + " (Lvl " + p.getLevel() + ")"));
    }

    private void handleShowAllPlants() {
        view.showSuccess(SystemMessage.COLLECTION_HEADER_ALL_PLANTS.getMessage());
        plantData.byId().values().stream()
                .sorted(Comparator.comparingInt(PlantSpec::getId))
                .forEach(p -> view.showSuccess(p.getId() + ". " + p.getName()));
    }

    private void handleShowPlantDetails(CollectionCommand cmd) {
        PlantSpec spec = plantData.byName().get(cmd.getTargetName().toLowerCase());
        if (spec == null) {
            view.showError(SystemMessage.COLLECTION_ITEM_NOT_FOUND.getMessage());
            return;
        }
        view.showSuccess("ID: " + spec.getId());
        view.showSuccess("Name: " + spec.getName());
        view.showSuccess("Category: " + spec.getCategory());
        view.showSuccess("Tags: " + spec.getTags().toString());
        view.showSuccess("Cost: " + spec.getCost() + " Sun");
        view.showSuccess("Base HP: " + spec.getBaseHp());
        view.showSuccess("Damage: " + spec.getDamage());
        view.showSuccess("Base Ability: " + spec.getBaseAbility());
        view.showSuccess("Plant Food Effect: " + spec.getPlantFoodEffect());
        view.showSuccess("Lvl 2 Upgrade: " + spec.getLvl2());
        view.showSuccess("Lvl 3 Upgrade: " + spec.getLvl3());
        view.showSuccess("Lvl 4 Upgrade: " + spec.getLvl4());
        view.showSuccess("Action Interval: " + spec.getActionInterval() + "s");
        view.showSuccess("Recharge: " + spec.getRecharge() + "s");
    }

    private void handlePurchasePlant(CollectionCommand cmd, User user) {
        PlantSpec spec = plantData.byName().get(cmd.getTargetName().toLowerCase());
        if (spec == null) {
            view.showError(SystemMessage.COLLECTION_ITEM_NOT_FOUND.getMessage());
        } else if (user.getOwnedPlant(spec.getName()) != null) {
            view.showError(SystemMessage.COLLECTION_ALREADY_OWNED.getMessage());
        } else if (!user.spendCoins(2000)) {
            view.showError(SystemMessage.COLLECTION_NOT_ENOUGH_COINS.getMessage());
        } else {
            user.addPlant(new PlayerPlant(spec.getName()));
            userManager.save();
            view.showSuccess(SystemMessage.COLLECTION_PLANT_PURCHASED.getMessage());
        }
    }

    private void handleUpgradePlant(CollectionCommand cmd, User user) {
        PlayerPlant p = user.getOwnedPlant(cmd.getTargetName());
        if (p == null) {
            view.showError(SystemMessage.COLLECTION_ITEM_NOT_FOUND.getMessage());
            return;
        }

        int currentLevel = p.getLevel();
        if (currentLevel >= 4) {
            view.showError(SystemMessage.COLLECTION_MAX_LEVEL_REACHED.getMessage());
            return;
        }
        //همینجوری فعلا نوشتم
        int costCoins = 500 * currentLevel;
        int costPackets = currentLevel;

        if (user.getCoins() >= costCoins && p.getSeedPackets() >= costPackets) {
            user.spendCoins(costCoins);
            p.spendSeedPackets(costPackets);
            p.upgrade();
            userManager.save();
            view.showSuccess(SystemMessage.COLLECTION_PLANT_UPGRADED.getMessage());
        } else {
            view.showError(SystemMessage.COLLECTION_NOT_ENOUGH_SEEDS.getMessage());
        }
    }
}