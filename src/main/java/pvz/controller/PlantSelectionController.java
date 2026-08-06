package pvz.controller;

import pvz.data.PlantData;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.command.PlantSelectionCommand;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;
import pvz.model.session.GameRuntime;
import pvz.model.session.GameSessionConfig;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlantSelectionController extends BaseController {

    private final PlantData plantData;
    private final GameRuntime gameRuntime;
    private final List<String> selectedPlants;
    private final Set<String> boostedPlants;
    private int maxSlots = 8;

    public PlantSelectionController(
            AppState appState,
            UserManager userManager,
            MenuView view,
            PlantData plantData,
            GameRuntime gameRuntime
    ) {
        super(appState, userManager, view);
        this.plantData = plantData;
        this.gameRuntime = gameRuntime;
        this.selectedPlants = new ArrayList<>();
        this.boostedPlants = new HashSet<>();
    }

    @Override
    protected Message handleSpecificCommand(Command command) {
        if (!(command instanceof PlantSelectionCommand plantCommand)) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }

        User currentUser = appState.getCurrentUser();

        switch (plantCommand.getAction()) {
            case SHOW_ALL_PLANTS -> handleShowAllPlants();
            case SHOW_AVAILABLE_PLANTS ->
                    handleShowAvailablePlants(currentUser);
            case SHOW_SELECTED_PLANTS ->
                    handleShowSelectedPlants(currentUser);
            case ADD_PLANT ->
                    handleAddPlant(plantCommand, currentUser);
            case REMOVE_PLANT ->
                    handleRemovePlant(plantCommand);
            case BOOST_PLANT ->
                    handleBoostPlant(plantCommand, currentUser);
            case START_GAME ->
                    handleStartGame(currentUser);
        }

        return null;
    }

    private void handleShowAllPlants() {
        view.showSuccess(
                SystemMessage.PLANT_SELECTION_HEADER_ALL.getMessage()
        );

        plantData.byId().values().stream()
                .sorted(Comparator.comparingInt(PlantSpec::getId))
                .forEach(spec -> view.showSuccess(
                        spec.getId() + ". " + spec.getName()
                ));
    }

    private void handleShowAvailablePlants(User user) {
        List<PlantSpec> availablePlants =
                plantData.byId().values().stream()
                        .filter(spec ->
                                user.getOwnedPlant(spec.getName()) != null
                        )
                        .filter(spec ->
                                !isSelected(spec.getName())
                        )
                        .sorted(
                                Comparator.comparingInt(PlantSpec::getId)
                        )
                        .toList();

        view.showSuccess(
                SystemMessage.PLANT_SELECTION_HEADER_AVAILABLE.getMessage()
        );

        if (availablePlants.isEmpty()) {
            view.showSuccess(
                    SystemMessage.PLANT_SELECTION_NO_AVAILABLE.getMessage()
            );
            return;
        }

        availablePlants.forEach(spec ->
                view.showSuccess("- " + spec.getName())
        );
    }

    private void handleShowSelectedPlants(User user) {
        view.showSuccess(
                "--- Selected Plants ("
                        + selectedPlants.size()
                        + "/"
                        + maxSlots
                        + ") ---"
        );

        if (selectedPlants.isEmpty()) {
            view.showSuccess(
                    SystemMessage.PLANT_SELECTION_NO_PLANTS.getMessage()
            );
            return;
        }

        selectedPlants.forEach(plant ->
                showSelectedPlant(plant, user)
        );
    }

    private void showSelectedPlant(String plant, User user) {
        boolean boosted =
                boostedPlants.contains(plant)
                        || user.hasStoredBoost(plant);

        String boostStatus = boosted ? " [BOOSTED]" : "";

        view.showSuccess("- " + plant + boostStatus);
    }

    private void handleAddPlant(
            PlantSelectionCommand command,
            User user
    ) {
        String target =
                normalizeName(command.getTargetName());

        PlantSpec spec =
                plantData.byName().get(target);

        if (spec == null) {
            view.showError(
                    SystemMessage.PLANT_SELECTION_INVALID_NAME.getMessage()
            );
            return;
        }

        PlayerPlant playerPlant =
                user.getOwnedPlant(target);

        if (playerPlant == null) {
            view.showError(
                    SystemMessage.PLANT_SELECTION_LOCKED.getMessage()
            );
            return;
        }

        if (selectedPlants.contains(target)) {
            view.showError(
                    SystemMessage.PLANT_SELECTION_ALREADY_SELECTED
                            .getMessage()
            );
            return;
        }

        if (selectedPlants.size() >= maxSlots) {
            view.showError(
                    SystemMessage.PLANT_SELECTION_SLOTS_FULL.getMessage()
            );
            return;
        }

        selectedPlants.add(target);

        view.showSuccess(
                SystemMessage.PLANT_SELECTION_ADDED.getMessage()
        );
    }

    private void handleRemovePlant(
            PlantSelectionCommand command
    ) {
        String target =
                normalizeName(command.getTargetName());

        PlantSpec spec =
                plantData.byName().get(target);

        if (spec == null) {
            view.showError(
                    SystemMessage.PLANT_SELECTION_INVALID_NAME.getMessage()
            );
            return;
        }

        if (!selectedPlants.contains(target)) {
            view.showError(
                    SystemMessage.PLANT_SELECTION_NOT_IN_SELECTION
                            .getMessage()
            );
            return;
        }

        selectedPlants.remove(target);

        view.showSuccess(
                SystemMessage.PLANT_SELECTION_REMOVED.getMessage()
        );
    }

    private void handleBoostPlant(
            PlantSelectionCommand command,
            User user
    ) {
        String target =
                normalizeName(command.getTargetName());

        PlantSpec spec =
                plantData.byName().get(target);

        if (spec == null) {
            view.showError(
                    SystemMessage.PLANT_SELECTION_INVALID_NAME.getMessage()
            );
            return;
        }

        if (user.getOwnedPlant(target) == null) {
            view.showError(
                    SystemMessage.PLANT_SELECTION_NOT_OWNED.getMessage()
            );
            return;
        }

        if (!PlantFoodSupport.isImplemented(spec)) {
            view.showError(
                    "Plant food effect for "
                            + spec.getName()
                            + " is not implemented yet!"
            );
            return;
        }

        if (isAlreadyBoosted(target, user)) {
            view.showError(
                    SystemMessage.PLANT_SELECTION_ALREADY_BOOSTED
                            .getMessage()
            );
            return;
        }

        if (!user.spendDiamonds(2)) {
            view.showError(
                    SystemMessage.PLANT_SELECTION_NOT_ENOUGH_DIAMONDS
                            .getMessage()
            );
            return;
        }

        boostedPlants.add(target);

        if (userManager.save()) {
            view.showSuccess(
                    SystemMessage.PLANT_SELECTION_BOOSTED_SUCCESS
                            .getMessage()
            );
            return;
        }

        boostedPlants.remove(target);
        user.addDiamonds(2);

        view.showError("Failed to save boost state.");
    }

    private void handleStartGame(User currentUser) {
        String selectedChapter =
                appState.getSelectedChapter();

        if (!canStartGame(selectedChapter)) {
            return;
        }

        Set<String> consumedStoredBoosts =
                consumeStoredBoosts(currentUser);

        Set<String> activeBoosts =
                new HashSet<>(boostedPlants);

        activeBoosts.addAll(consumedStoredBoosts);

        GameSessionConfig config =
                createGameConfig(
                        selectedChapter,
                        activeBoosts
                );

        if (!userManager.save()) {
            restoreStoredBoosts(
                    currentUser,
                    consumedStoredBoosts
            );

            view.showError(
                    "Failed to save game state. Cannot start game."
            );
            return;
        }

        gameRuntime.start(config);
        appState.setCurrentMenu(MenuName.PLAYING);

        view.showSuccess(
                SystemMessage.PLANT_SELECTION_START_GAME.getMessage()
        );
    }

    private boolean canStartGame(String selectedChapter) {
        if (selectedPlants.isEmpty()) {
            view.showError(
                    SystemMessage.PLANT_SELECTION_EMPTY_START.getMessage()
            );
            return false;
        }

        if (selectedChapter == null
                || selectedChapter.isBlank()) {
            view.showError("No chapter selected!");
            return false;
        }

        List<String> missingBoostedPlants =
                findBoostedButNotSelectedPlants();

        if (missingBoostedPlants.isEmpty()) {
            return true;
        }

        view.showError(
                "Cannot start game. These boosted plants "
                        + "are not selected: "
                        + String.join(
                        ", ",
                        missingBoostedPlants
                )
        );

        return false;
    }

    private Set<String> consumeStoredBoosts(User user) {
        Set<String> consumedBoosts =
                new HashSet<>();

        for (String plant : selectedPlants) {
            if (!user.hasStoredBoost(plant)) {
                continue;
            }

            user.removeStoredBoost(plant);
            consumedBoosts.add(plant);
        }

        return consumedBoosts;
    }

    private void restoreStoredBoosts(
            User user,
            Set<String> consumedBoosts
    ) {
        consumedBoosts.forEach(
                user::addStoredBoost
        );
    }

    private GameSessionConfig createGameConfig(
            String selectedChapter,
            Set<String> activeBoosts
    ) {
        return new GameSessionConfig.Builder(
                selectedChapter,
                List.copyOf(selectedPlants)
        )
                .boostedPlants(
                        Set.copyOf(activeBoosts)
                )
                .build();
    }

    private List<String> findBoostedButNotSelectedPlants() {
        return boostedPlants.stream()
                .filter(plant ->
                        !selectedPlants.contains(plant)
                )
                .sorted()
                .toList();
    }

    private boolean isSelected(String plantName) {
        return selectedPlants.contains(
                normalizeName(plantName)
        );
    }

    private boolean isAlreadyBoosted(
            String plantName,
            User user
    ) {
        return boostedPlants.contains(plantName)
                || user.hasStoredBoost(plantName);
    }

    private String normalizeName(String plantName) {
        return plantName.toLowerCase();
    }

    public void resetSelection() {
        selectedPlants.clear();
        boostedPlants.clear();
    }
}
