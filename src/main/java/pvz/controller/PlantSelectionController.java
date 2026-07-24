package pvz.controller;

import pvz.data.PlantData;
import pvz.model.account.PlayerPlant;
import pvz.model.account.UserManager;
import pvz.model.account.User;
import pvz.model.command.Command;
import pvz.model.command.PlantSelectionCommand;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.utils.AppState;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlantSelectionController extends BaseController {

    private final PlantData plantData;
    private final List<String> selectedPlants;
    private final Set<String> boostedPlants;
    private int maxSlots = 8;

    public PlantSelectionController(AppState appState, UserManager userManager, MenuView view, PlantData plantData) {
        super(appState, userManager, view);
        this.plantData = plantData;
        this.selectedPlants = new ArrayList<>();
        this.boostedPlants = new HashSet<>();
    }

    @Override
    protected Message handleSpecificCommand(Command command) {
        if (!(command instanceof PlantSelectionCommand cmd)) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }

        User currentUser = appState.getCurrentUser();

        switch (cmd.getAction()) {
            case SHOW_ALL_PLANTS -> handleShowAllPlants(currentUser);
            case SHOW_AVAILABLE_PLANTS -> handleShowAvailablePlants();
            case ADD_PLANT -> handleAddPlant(cmd, currentUser);
            case REMOVE_PLANT -> handleRemovePlant(cmd);
            case BOOST_PLANT -> handleBoostPlant(cmd, currentUser);
            case START_GAME -> handleStartGame();
        }
        return null;
    }

    private void handleShowAllPlants(User user) {
        view.showSuccess(SystemMessage.PLANT_SELECTION_HEADER_UNLOCKED.getMessage());
        user.getUnlockedPlants().forEach(p -> view.showSuccess(p.getPlantName()));
    }

    private void handleShowAvailablePlants() {
        view.showSuccess("--- Selected Plants (" + selectedPlants.size() + "/" + maxSlots + ") ---");
        if (selectedPlants.isEmpty()) {
            view.showSuccess(SystemMessage.PLANT_SELECTION_NO_PLANTS.getMessage());
        } else {
            selectedPlants.forEach(p -> {
                String boostStatus = boostedPlants.contains(p) ? " [BOOSTED]" : "";
                view.showSuccess("- " + p + boostStatus);
            });
        }
    }

    private void handleAddPlant(PlantSelectionCommand cmd, User user) {
        String target = cmd.getTargetName().toLowerCase();
        PlantSpec spec = plantData.byName().get(target);
        PlayerPlant playerPlant = user.getOwnedPlant(target);

        if (spec == null) {
            view.showError(SystemMessage.PLANT_SELECTION_INVALID_NAME.getMessage());
        } else if (playerPlant == null) {
            view.showError(SystemMessage.PLANT_SELECTION_LOCKED.getMessage());
        } else if (selectedPlants.contains(target)) {
            view.showError(SystemMessage.PLANT_SELECTION_ALREADY_SELECTED.getMessage());
        } else if (selectedPlants.size() >= maxSlots) {
            view.showError(SystemMessage.PLANT_SELECTION_SLOTS_FULL.getMessage());
        } else {
            selectedPlants.add(target);
            view.showSuccess(SystemMessage.PLANT_SELECTION_ADDED.getMessage());
        }
    }

    private void handleRemovePlant(PlantSelectionCommand cmd) {
        String target = cmd.getTargetName().toLowerCase();
        PlantSpec spec = plantData.byName().get(target);

        if (spec == null) {
            view.showError(SystemMessage.PLANT_SELECTION_INVALID_NAME.getMessage());
        } else if (!selectedPlants.contains(target)) {
            view.showError(SystemMessage.PLANT_SELECTION_NOT_IN_SELECTION.getMessage());
        } else {
            selectedPlants.remove(target);
            boostedPlants.remove(target);
            view.showSuccess(SystemMessage.PLANT_SELECTION_REMOVED.getMessage());
        }
    }

    private void handleBoostPlant(PlantSelectionCommand cmd, User user) {
        String target = cmd.getTargetName().toLowerCase();
        PlayerPlant playerPlant = user.getOwnedPlant(target);

        if (playerPlant == null) {
            view.showError(SystemMessage.PLANT_SELECTION_NOT_OWNED.getMessage());
        } else if (boostedPlants.contains(target)) {
            view.showError(SystemMessage.PLANT_SELECTION_ALREADY_BOOSTED.getMessage());
        } else if (!user.spendDiamonds(2)) {
            view.showError(SystemMessage.PLANT_SELECTION_NOT_ENOUGH_DIAMONDS.getMessage());
        } else {
            boostedPlants.add(target);
            userManager.save();
            view.showSuccess(SystemMessage.PLANT_SELECTION_BOOSTED_SUCCESS.getMessage());
        }
    }

    private void handleStartGame() {
        if (selectedPlants.isEmpty()) {
            view.showError(SystemMessage.PLANT_SELECTION_EMPTY_START.getMessage());
        } else {
            view.showSuccess(SystemMessage.PLANT_SELECTION_START_GAME.getMessage());
        }
    }
}