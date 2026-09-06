package pvz.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import pvz.data.PlantData;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.command.PlantSelectionCommand;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;
import pvz.model.selection.PlantSelectionRules;
import pvz.model.service.PlantUpgradeService;
import pvz.model.session.GameRuntime;
import pvz.model.session.GameSessionConfig;
import pvz.model.session.GameSessionConfigFactory;
import pvz.model.session.StoredPlantBoostAccess;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

public class PlantSelectionController extends BaseController {
    public static final int MANUAL_BOOST_COST = 2;

    public enum BoostSource {
        NONE,
        MANUAL,
        GREENHOUSE
    }

    private final PlantData plantData;
    private final GameRuntime gameRuntime;
    private final GameSessionConfigFactory configFactory;
    private final PlantUpgradeService plantUpgradeService;
    private final PlantSelectionRules rules;
    private final List<String> selectedPlants;
    private final Set<String> boostedPlants;

    public PlantSelectionController(
            AppState appState,
            UserManager userManager,
            MenuView view,
            PlantData plantData,
            GameRuntime gameRuntime,
            GameSessionConfigFactory configFactory
    ) {
        this(
                appState,
                userManager,
                view,
                plantData,
                gameRuntime,
                configFactory,
                PlantSelectionRules.normal()
        );
    }

    public PlantSelectionController(
            AppState appState,
            UserManager userManager,
            MenuView view,
            PlantData plantData,
            GameRuntime gameRuntime,
            GameSessionConfigFactory configFactory,
            PlantSelectionRules rules
    ) {
        super(appState, userManager, view);
        this.plantData = Objects.requireNonNull(plantData, "plant data cannot be null");
        this.gameRuntime = Objects.requireNonNull(gameRuntime, "game runtime cannot be null");
        this.configFactory = Objects.requireNonNull(
                configFactory,
                "game session config factory cannot be null"
        );
        this.rules = Objects.requireNonNull(rules, "plant selection rules cannot be null");
        this.plantUpgradeService = new PlantUpgradeService(plantData.levelCosts());
        this.selectedPlants = new ArrayList<>();
        this.boostedPlants = new HashSet<>();
        initializeForcedPlants(appState.getCurrentUser());
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
            case SHOW_AVAILABLE_PLANTS -> handleShowAvailablePlants(currentUser);
            case SHOW_SELECTED_PLANTS -> handleShowSelectedPlants(currentUser);
            case ADD_PLANT -> handleAddPlant(plantCommand, currentUser);
            case REMOVE_PLANT -> handleRemovePlant(plantCommand);
            case BOOST_PLANT -> handleBoostPlant(plantCommand, currentUser);
            case UPGRADE_PLANT -> handleUpgradePlant(plantCommand, currentUser);
            case START_GAME -> handleStartGame(currentUser);
        }

        return null;
    }

    private void initializeForcedPlants(User user) {
        if (user == null) {
            return;
        }
        for (String forcedPlant : rules.forcedPlants()) {
            PlantSpec spec = plantData.byName().get(forcedPlant);
            if (spec != null
                    && rules.isSelectable(spec)
                    && user.getOwnedPlant(forcedPlant) != null
                    && selectedPlants.size() < rules.selectableCapacity()) {
                selectedPlants.add(forcedPlant);
            }
        }
    }

    private void handleShowAllPlants() {
        view.showSuccess(SystemMessage.PLANT_SELECTION_HEADER_ALL.getMessage());

        plantData.byId().values().stream()
                .sorted(Comparator.comparingInt(PlantSpec::getId))
                .forEach(spec -> view.showSuccess(
                        spec.getId() + ". " + spec.getName()
                ));
    }

    private void handleShowAvailablePlants(User user) {
        if (user == null) {
            view.showError("No user is logged in.");
            return;
        }
        List<PlantSpec> availablePlants = plantData.byId().values().stream()
                .filter(rules::isSelectable)
                .filter(spec -> user.getOwnedPlant(spec.getName()) != null)
                .filter(spec -> !isSelected(spec.getName()))
                .sorted(Comparator.comparingInt(PlantSpec::getId))
                .toList();

        view.showSuccess(SystemMessage.PLANT_SELECTION_HEADER_AVAILABLE.getMessage());

        if (availablePlants.isEmpty()) {
            view.showSuccess(SystemMessage.PLANT_SELECTION_NO_AVAILABLE.getMessage());
            return;
        }

        availablePlants.forEach(spec -> {
            PlayerPlant playerPlant = user.getOwnedPlant(spec.getName());
            PlantSpec effectiveSpec = spec.withLevel(playerPlant.getLevel());
            view.showSuccess("- " + spec.getName()
                    + " [Lvl " + playerPlant.getLevel()
                    + ", Cost " + effectiveSpec.getCost()
                    + ", Seeds " + seedProgress(playerPlant) + "]");
        });
    }

    private void handleShowSelectedPlants(User user) {
        view.showSuccess(
                "--- Selected Plants ("
                        + selectedPlants.size()
                        + "/"
                        + rules.selectableCapacity()
                        + ") ---"
        );

        if (selectedPlants.isEmpty()) {
            view.showSuccess(SystemMessage.PLANT_SELECTION_NO_PLANTS.getMessage());
            return;
        }

        selectedPlants.forEach(plant -> showSelectedPlant(plant, user));
    }

    private void showSelectedPlant(String plant, User user) {
        BoostSource boostSource = getBoostSource(plant, user);
        String boostStatus = boostSource == BoostSource.NONE
                ? ""
                : " [BOOSTED: " + boostSource + "]";
        PlayerPlant playerPlant = user == null ? null : user.getOwnedPlant(plant);
        int level = playerPlant == null ? PlantSpec.MIN_LEVEL : playerPlant.getLevel();
        PlantSpec baseSpec = plantData.byName().get(normalizeName(plant));
        int effectiveCost = baseSpec == null ? 0 : baseSpec.withLevel(level).getCost();

        view.showSuccess("- " + plant + " [Lvl " + level
                + ", Cost " + effectiveCost + "]" + boostStatus);
    }

    private void handleAddPlant(PlantSelectionCommand command, User user) {
        if (user == null) {
            view.showError("No user is logged in.");
            return;
        }
        String target = normalizeName(command.getTargetName());
        PlantSpec spec = plantData.byName().get(target);

        if (spec == null) {
            view.showError(SystemMessage.PLANT_SELECTION_INVALID_NAME.getMessage());
            return;
        }
        if (!rules.isSelectable(spec)) {
            view.showError("This plant is not available in the selected level.");
            return;
        }
        if (user.getOwnedPlant(target) == null) {
            view.showError(SystemMessage.PLANT_SELECTION_LOCKED.getMessage());
            return;
        }
        if (selectedPlants.contains(target)) {
            view.showError(SystemMessage.PLANT_SELECTION_ALREADY_SELECTED.getMessage());
            return;
        }
        if (selectedPlants.size() >= rules.selectableCapacity()) {
            view.showError(SystemMessage.PLANT_SELECTION_SLOTS_FULL.getMessage());
            return;
        }

        selectedPlants.add(target);
        view.showSuccess(SystemMessage.PLANT_SELECTION_ADDED.getMessage());
    }

    private void handleRemovePlant(PlantSelectionCommand command) {
        String target = normalizeName(command.getTargetName());
        PlantSpec spec = plantData.byName().get(target);

        if (spec == null) {
            view.showError(SystemMessage.PLANT_SELECTION_INVALID_NAME.getMessage());
            return;
        }
        if (!selectedPlants.contains(target)) {
            view.showError(SystemMessage.PLANT_SELECTION_NOT_IN_SELECTION.getMessage());
            return;
        }
        if (rules.isForced(target)) {
            view.showError("This plant is required by the selected level.");
            return;
        }

        selectedPlants.remove(target);
        boostedPlants.remove(target);
        view.showSuccess(SystemMessage.PLANT_SELECTION_REMOVED.getMessage());
    }

    private void handleBoostPlant(PlantSelectionCommand command, User user) {
        if (user == null) {
            view.showError("No user is logged in.");
            return;
        }
        String target = normalizeName(command.getTargetName());
        PlantSpec spec = plantData.byName().get(target);

        if (spec == null) {
            view.showError(SystemMessage.PLANT_SELECTION_INVALID_NAME.getMessage());
            return;
        }
        if (!rules.isSelectable(spec)) {
            view.showError("This plant is not available in the selected level.");
            return;
        }
        if (user.getOwnedPlant(target) == null) {
            view.showError(SystemMessage.PLANT_SELECTION_NOT_OWNED.getMessage());
            return;
        }
        if (!selectedPlants.contains(target)) {
            view.showError("Select the plant before boosting it.");
            return;
        }
        if (!PlantFoodSupport.isImplemented(spec)) {
            view.showError(
                    "Plant food effect for " + spec.getName() + " is not implemented yet!"
            );
            return;
        }
        if (isAlreadyBoosted(target, user)) {
            view.showError(SystemMessage.PLANT_SELECTION_ALREADY_BOOSTED.getMessage());
            return;
        }
        if (availableDiamondsForNewBoosts(user) < MANUAL_BOOST_COST) {
            view.showError(SystemMessage.PLANT_SELECTION_NOT_ENOUGH_DIAMONDS.getMessage());
            return;
        }

        boostedPlants.add(target);
        view.showSuccess(
                spec.getName() + " boost queued. " + MANUAL_BOOST_COST
                        + " diamonds will be charged when the level starts."
        );
    }

    private void handleUpgradePlant(PlantSelectionCommand command, User user) {
        if (user == null) {
            view.showError("No user is logged in.");
            return;
        }
        String target = normalizeName(command.getTargetName());
        PlantSpec spec = plantData.byName().get(target);
        if (spec == null) {
            view.showError(SystemMessage.PLANT_SELECTION_INVALID_NAME.getMessage());
            return;
        }

        PlantUpgradeService.Result result = plantUpgradeService.upgrade(user, spec.getName());
        switch (result) {
            case NOT_OWNED -> view.showError(SystemMessage.PLANT_SELECTION_NOT_OWNED.getMessage());
            case MAX_LEVEL -> view.showError(SystemMessage.COLLECTION_MAX_LEVEL_REACHED.getMessage());
            case NOT_ENOUGH_COINS -> view.showError(SystemMessage.COLLECTION_NOT_ENOUGH_COINS.getMessage());
            case NOT_ENOUGH_SEEDS -> view.showError(SystemMessage.COLLECTION_NOT_ENOUGH_SEEDS.getMessage());
            case SUCCESS -> saveUpgradeOrRollback(user, spec);
        }
    }

    private void saveUpgradeOrRollback(User user, PlantSpec spec) {
        String username = user.getUsername();
        if (userManager.save()) {
            PlayerPlant playerPlant = user.getOwnedPlant(spec.getName());
            view.showSuccess(spec.getName() + " upgraded to level "
                    + playerPlant.getLevel() + ".");
            return;
        }

        userManager.reload();
        appState.setCurrentUser(userManager.find(
                candidate -> candidate.getUsername().equals(username)
        ));
        view.showError("Failed to save game data. Plant upgrade reverted.");
    }

    private void handleStartGame(User currentUser) {
        String selectedLevelId = appState.getSelectedLevelId();
        if (!canStartGame(selectedLevelId, currentUser)) {
            return;
        }

        Set<String> storedBoostsForSession = selectedPlants.stream()
                .filter(plant -> hasUsableStoredBoost(plant, currentUser))
                .collect(Collectors.toUnmodifiableSet());
        GameSessionConfig config = createGameConfig(
                selectedLevelId,
                Set.copyOf(boostedPlants),
                storedBoostsForSession,
                currentUser.getPlantFoodCount(),
                currentUser.getDifficultyLevel()
        );

        int manualBoostCost = getPendingManualBoostCost();
        if (manualBoostCost > 0 && !currentUser.spendDiamonds(manualBoostCost)) {
            view.showError(SystemMessage.PLANT_SELECTION_NOT_ENOUGH_DIAMONDS.getMessage());
            return;
        }
        int transferredPlantFood = currentUser.getPlantFoodCount();
        currentUser.clearPlantFood();

        if (!userManager.save()) {
            rollbackStartResources(currentUser, manualBoostCost, transferredPlantFood);
            view.showError("Failed to save game state. Cannot start game.");
            return;
        }

        try {
            gameRuntime.start(
                    config,
                    zombieSpec -> discoverZombie(currentUser, zombieSpec.getId(), zombieSpec.getName()),
                    createStoredBoostAccess(currentUser)
            );
        } catch (RuntimeException exception) {
            rollbackAfterRuntimeStartFailure(
                    currentUser,
                    manualBoostCost,
                    transferredPlantFood
            );
            view.showError("Could not start the selected level: " + exception.getMessage());
            return;
        }

        appState.setCurrentMenu(MenuName.PLAYING);
        view.showSuccess(SystemMessage.PLANT_SELECTION_START_GAME.getMessage());
        view.showMessage("Mission: clear all waves before a zombie reaches the house.");
        view.showMessage(
                "Level " + config.levelId() + " has "
                        + gameRuntime.session().waveManager().getTotalWaves()
                        + " waves."
        );
    }

    private void discoverZombie(User user, String zombieId, String zombieName) {
        if (user.discoverZombie(zombieId, zombieName)) {
            userManager.save();
        }
    }

    private StoredPlantBoostAccess createStoredBoostAccess(User user) {
        return new StoredPlantBoostAccess() {
            @Override
            public boolean isAvailable(String plantName) {
                return user.hasStoredBoost(plantName);
            }

            @Override
            public boolean consume(String plantName) {
                if (!user.hasStoredBoost(plantName)) {
                    return false;
                }
                user.removeStoredBoost(plantName);
                if (userManager.save()) {
                    return true;
                }
                user.addStoredBoost(plantName);
                return false;
            }
        };
    }

    private void rollbackStartResources(User user, int diamondCost, int plantFood) {
        if (diamondCost > 0) {
            user.addDiamonds(diamondCost);
        }
        if (plantFood > 0) {
            user.addPlantFood(plantFood);
        }
    }

    private void rollbackAfterRuntimeStartFailure(User user, int diamondCost, int plantFood) {
        rollbackStartResources(user, diamondCost, plantFood);
        if (!userManager.save()) {
            String username = user.getUsername();
            userManager.reload();
            appState.setCurrentUser(userManager.find(
                    candidate -> candidate.getUsername().equals(username)
            ));
        }
    }

    private boolean canStartGame(String selectedLevelId, User currentUser) {
        if (currentUser == null) {
            view.showError("No user is logged in.");
            return false;
        }
        if (selectedPlants.isEmpty()) {
            view.showError(SystemMessage.PLANT_SELECTION_EMPTY_START.getMessage());
            return false;
        }
        if (selectedLevelId == null || selectedLevelId.isBlank()) {
            view.showError("No level selected!");
            return false;
        }
        if (selectedPlants.size() > rules.selectableCapacity()) {
            view.showError("Too many plants are selected for this level.");
            return false;
        }
        for (String forcedPlant : rules.forcedPlants()) {
            if (!selectedPlants.contains(forcedPlant)) {
                view.showError("Required plant is unavailable or not selected: " + forcedPlant);
                return false;
            }
        }
        for (String plantName : selectedPlants) {
            PlantSpec spec = plantData.byName().get(plantName);
            if (spec == null || !rules.isSelectable(spec)) {
                view.showError("Selected plant is not allowed in this level: " + plantName);
                return false;
            }
            if (currentUser.getOwnedPlant(plantName) == null) {
                view.showError("Selected plant is no longer owned: " + plantName);
                return false;
            }
        }
        if (!selectedPlants.containsAll(boostedPlants)) {
            view.showError("Every manual boost must belong to a selected plant.");
            return false;
        }
        if (getPendingManualBoostCost() > currentUser.getDiamonds()) {
            view.showError(SystemMessage.PLANT_SELECTION_NOT_ENOUGH_DIAMONDS.getMessage());
            return false;
        }
        return true;
    }

    private GameSessionConfig createGameConfig(
            String selectedLevelId,
            Set<String> manualBoosts,
            Set<String> storedBoosts,
            int startingPlantFood,
            int difficultyLevel
    ) {
        User user = appState.getCurrentUser();
        Map<String, Integer> plantLevels = selectedPlants.stream()
                .collect(Collectors.toUnmodifiableMap(
                        plant -> plant,
                        plant -> {
                            PlayerPlant playerPlant = user.getOwnedPlant(plant);
                            return playerPlant == null
                                    ? PlantSpec.MIN_LEVEL
                                    : playerPlant.getLevel();
                        }
                ));

        return configFactory.create(
                selectedLevelId,
                List.copyOf(selectedPlants),
                plantLevels,
                Set.copyOf(manualBoosts),
                Set.copyOf(storedBoosts),
                startingPlantFood,
                difficultyLevel
        );
    }

    private int availableDiamondsForNewBoosts(User user) {
        return Math.max(0, user.getDiamonds() - getPendingManualBoostCost());
    }

    private boolean hasUsableStoredBoost(String plantName, User user) {
        if (user == null || !user.hasStoredBoost(plantName)) {
            return false;
        }
        PlantSpec spec = plantData.byName().get(normalizeName(plantName));
        return spec != null && PlantFoodSupport.isImplemented(spec);
    }

    private boolean isSelected(String plantName) {
        return selectedPlants.contains(normalizeName(plantName));
    }

    private boolean isAlreadyBoosted(String plantName, User user) {
        return boostedPlants.contains(normalizeName(plantName))
                || hasUsableStoredBoost(plantName, user);
    }

    private String seedProgress(PlayerPlant playerPlant) {
        if (playerPlant.getLevel() >= PlantSpec.MAX_LEVEL) {
            return "MAX";
        }
        int required = plantData.levelCosts()
                .forTargetLevel(playerPlant.getLevel() + 1)
                .seedPackets();
        return playerPlant.getSeedPackets() + "/" + required;
    }

    private String normalizeName(String plantName) {
        return Objects.requireNonNull(plantName, "plant name cannot be null")
                .strip()
                .toLowerCase(Locale.ROOT);
    }

    public void resetSelection() {
        selectedPlants.clear();
        boostedPlants.clear();
        initializeForcedPlants(appState.getCurrentUser());
    }

    public List<String> getSelectedPlants() {
        return List.copyOf(selectedPlants);
    }

    public Set<String> getBoostedPlants() {
        return Set.copyOf(boostedPlants);
    }

    public int getMaxSlots() {
        return rules.maxSlots();
    }

    public int getSelectableCapacity() {
        return rules.selectableCapacity();
    }

    public int getLockedSlots() {
        return rules.lockedSlots();
    }

    public int getPendingManualBoostCost() {
        return boostedPlants.size() * MANUAL_BOOST_COST;
    }

    public PlantSelectionRules getRules() {
        return rules;
    }

    public boolean isPlantSelected(String plantName) {
        return plantName != null && isSelected(plantName);
    }

    public boolean isPlantAllowed(String plantName) {
        if (plantName == null) {
            return false;
        }
        PlantSpec spec = plantData.byName().get(normalizeName(plantName));
        return spec != null && rules.isSelectable(spec);
    }

    public boolean isForcedPlant(String plantName) {
        return plantName != null && rules.isForced(plantName);
    }

    public BoostSource getBoostSource(String plantName, User user) {
        if (plantName == null) {
            return BoostSource.NONE;
        }
        String normalizedName = normalizeName(plantName);
        if (boostedPlants.contains(normalizedName)) {
            return BoostSource.MANUAL;
        }
        if (hasUsableStoredBoost(normalizedName, user)) {
            return BoostSource.GREENHOUSE;
        }
        return BoostSource.NONE;
    }

    public boolean isPlantBoosted(String plantName, User user) {
        return getBoostSource(plantName, user) != BoostSource.NONE;
    }

    @Override
    protected void handleMenuExit() {
        resetSelection();
        appState.setSelectedLevelId(null);
        appState.setCurrentMenu(MenuName.CHAPTER);
        view.showSuccess("returned to chapter levels");
    }
}
