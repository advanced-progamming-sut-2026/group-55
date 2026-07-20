package pvz.controller;

import pvz.data.PlantData;
import pvz.model.account.UserManager;
import pvz.model.account.PlayerPlant;
import pvz.model.command.CollectionCommand;
import pvz.model.command.Command;
import pvz.model.entity.plant.PlantSpec;
import pvz.data.PlantCsvLoader;
import pvz.data.PlantData;
import pvz.model.utils.AppState;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

public class CollectionController extends BaseController {

    private final PlantData plantData;

    public CollectionController(AppState appState, UserManager userManager, MenuView view, PlantData plantData) {
        super(appState, userManager, view);
        this.plantData = plantData;
    }

    @Override
    protected Message handleSpecificCommand(Command command) {
        if (!(command instanceof CollectionCommand cmd)) return null;

        switch (cmd.getAction()) {
            case SHOW_PLANTS -> {
                view.showSuccess(SystemMessage.COLLECTION_HEADER_YOUR_PLANTS.getMessage());
                appState.getCurrentUser().getUnlockedPlants()
                        .forEach(p -> view.showSuccess(p.getPlantName() + " (Lvl " + p.getLevel() + ")"));
            }

            case SHOW_ALL_PLANTS -> {
                view.showSuccess(SystemMessage.COLLECTION_HEADER_ALL_PLANTS.getMessage());
                plantData.byId().values().stream()
                        .sorted((p1, p2) -> p1.getId() - p2.getId())
                        .forEach(p -> view.showSuccess(p.getId() + ". " + p.getName()));
            }

            case SHOW_ZOMBIES -> {
                view.showSuccess(SystemMessage.COLLECTION_HEADER_ZOMBIES.getMessage());
                appState.getCurrentUser().getSeenZombies().forEach(view::showSuccess);
            }

            case SHOW_PLANT_DETAILS -> {
                PlantSpec spec = plantData.byName().get(cmd.getTargetName().toLowerCase());
                if (spec == null) {
                    view.showError(SystemMessage.COLLECTION_ITEM_NOT_FOUND.getMessage());
                } else {
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
            }
            case PURCHASE_PLANT -> {
                PlantSpec spec = plantData.byName().get(cmd.getTargetName().toLowerCase());
                if (spec == null) {
                    view.showError(SystemMessage.COLLECTION_ITEM_NOT_FOUND.getMessage());
                } else if (appState.getCurrentUser().getOwnedPlant(spec.getName()) != null) {
                    view.showError(SystemMessage.COLLECTION_ALREADY_OWNED.getMessage());
                } else if (!appState.getCurrentUser().spendCoins(2000)) {
                    view.showError(SystemMessage.COLLECTION_NOT_ENOUGH_COINS.getMessage());
                } else {
                    appState.getCurrentUser().addPlant(new PlayerPlant(spec.getName()));
                    userManager.save();
                    view.showSuccess(SystemMessage.COLLECTION_PLANT_PURCHASED.getMessage());
                }
            }

            case UPGRADE_PLANT -> {
                PlayerPlant p = appState.getCurrentUser().getOwnedPlant(cmd.getTargetName());
                if (p == null) {
                    view.showError(SystemMessage.COLLECTION_ITEM_NOT_FOUND.getMessage());
                } else if (!appState.getCurrentUser().spendCoins(500) || !appState.getCurrentUser().spendSeedPackets(1)) {
                    view.showError(SystemMessage.COLLECTION_NOT_ENOUGH_SEEDS.getMessage());
                } else {
                    p.upgrade();
                    userManager.save();
                    view.showSuccess(SystemMessage.COLLECTION_PLANT_UPGRADED.getMessage());
                }
            }
        }
        return null;
    }
}