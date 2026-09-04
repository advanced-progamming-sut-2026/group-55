package pvz.controller;

import pvz.data.PlantData;
import pvz.data.ZombieData;
import pvz.model.account.UserManager;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.command.CollectionCommand;
import pvz.model.command.Command;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.level.PlantLevelCost;
import pvz.model.service.PlantUpgradeService;
import pvz.model.entity.zombie.ZombieSpec;
import pvz.model.utils.AppState;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;
import java.util.Comparator;
import java.util.Locale;

public class CollectionController extends BaseController {

    private final PlantData plantData;
    private final ZombieData zombieData;
    private final PlantUpgradeService plantUpgradeService;

    public CollectionController(
            AppState appState,
            UserManager userManager,
            MenuView view,
            PlantData plantData,
            ZombieData zombieData
    ) {
        super(appState, userManager, view);
        this.plantData = plantData;
        this.zombieData = zombieData;
        this.plantUpgradeService = new PlantUpgradeService(plantData.levelCosts());
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
            case SHOW_PLANT_DETAILS -> handleShowPlantDetails(cmd, currentUser);
            case PURCHASE_PLANT -> handlePurchasePlant(cmd, currentUser);
            case UPGRADE_PLANT -> handleUpgradePlant(cmd, currentUser);
            case SHOW_ZOMBIES -> handleShowZombies(currentUser);
            case SHOW_ALL_ZOMBIES -> handleShowAllZombies();
            case SHOW_ZOMBIE_DETAILS -> handleShowZombieDetails(cmd);
            default -> view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        }
        return null;
    }

    private void handleShowPlants(User user) {
        view.showSuccess(SystemMessage.COLLECTION_HEADER_YOUR_PLANTS.getMessage());
        user.getUnlockedPlants().forEach(playerPlant -> {
            String progress;
            if (playerPlant.getLevel() >= PlantSpec.MAX_LEVEL) {
                progress = "MAX";
            } else {
                PlantLevelCost next = plantData.levelCosts()
                        .forTargetLevel(playerPlant.getLevel() + 1);
                progress = playerPlant.getSeedPackets() + "/" + next.seedPackets() + " seeds";
            }
            view.showSuccess(playerPlant.getPlantName()
                    + " (Lvl " + playerPlant.getLevel() + ", " + progress + ")");
        });
    }

    private void handleShowAllPlants() {
        view.showSuccess(SystemMessage.COLLECTION_HEADER_ALL_PLANTS.getMessage());
        plantData.byId().values().stream()
                .sorted(Comparator.comparingInt(PlantSpec::getId))
                .forEach(p -> view.showSuccess(p.getId() + ". " + p.getName()));
    }

    private void handleShowPlantDetails(CollectionCommand cmd, User user) {
        PlantSpec spec = plantData.byName().get(cmd.getTargetName().toLowerCase(Locale.ROOT));
        if (spec == null) {
            view.showError(SystemMessage.COLLECTION_ITEM_NOT_FOUND.getMessage());
            return;
        }
        PlayerPlant owned = user.getOwnedPlant(spec.getName());
        int currentLevel = owned == null ? PlantSpec.MIN_LEVEL : owned.getLevel();
        PlantSpec effectiveSpec = spec.withLevel(currentLevel);

        view.showSuccess("ID: " + spec.getId());
        view.showSuccess("Name: " + spec.getName());
        view.showSuccess("Category: " + spec.getCategory());
        view.showSuccess("Tags: " + spec.getTags());
        view.showSuccess("Base Ability: " + spec.getBaseAbility());
        view.showSuccess("Plant Food Effect: " + spec.getPlantFoodEffect());
        view.showSuccess("Lvl 2 Upgrade: " + spec.getLvl2());
        view.showSuccess("Lvl 3 Upgrade: " + spec.getLvl3());
        view.showSuccess("Lvl 4 Upgrade: " + spec.getLvl4());

        if (owned == null) {
            view.showSuccess("Owned: no");
            view.showSuccess("Base Cost: " + spec.getCost() + " Sun");
            view.showSuccess("Base HP: " + spec.getBaseHp());
            view.showSuccess("Base Damage: " + spec.getDamage());
            view.showSuccess("Base Action Interval: " + spec.getActionInterval() + "s");
            view.showSuccess("Base Recharge: " + spec.getRecharge() + "s");
            return;
        }

        view.showSuccess("Current Level: " + currentLevel);
        view.showSuccess("Seed Packets: " + owned.getSeedPackets());
        view.showSuccess("Effective Cost: " + effectiveSpec.getCost() + " Sun");
        view.showSuccess("Effective HP: " + effectiveSpec.getBaseHp());
        view.showSuccess("Effective Damage: " + effectiveSpec.getDamage());
        view.showSuccess("Effective Action Interval: " + effectiveSpec.getActionInterval() + "s");
        view.showSuccess("Effective Recharge: " + effectiveSpec.getRecharge() + "s");
        if (currentLevel < PlantSpec.MAX_LEVEL) {
            PlantLevelCost next = plantData.levelCosts().forTargetLevel(currentLevel + 1);
            view.showSuccess("Next Upgrade: " + next.coins() + " coins + "
                    + next.seedPackets() + " seed packets");
        } else {
            view.showSuccess("Next Upgrade: MAX LEVEL");
        }
    }

    private void handlePurchasePlant(CollectionCommand cmd, User user) {
        PlantSpec spec = plantData.byName().get(cmd.getTargetName().toLowerCase(Locale.ROOT));
        if (spec == null) {
            view.showError(SystemMessage.COLLECTION_ITEM_NOT_FOUND.getMessage());
        } else if (user.getOwnedPlant(spec.getName()) != null) {
            view.showError(SystemMessage.COLLECTION_ALREADY_OWNED.getMessage());
        } else if (!user.spendCoins(2000)) {
            view.showError(SystemMessage.COLLECTION_NOT_ENOUGH_COINS.getMessage());
        } else {
            user.addPlant(new PlayerPlant(spec.getName()));
            user.addNews("Plant Unlocked", spec.getName() + " has been unlocked!");
            userManager.save();
            view.showSuccess(SystemMessage.COLLECTION_PLANT_PURCHASED.getMessage());
        }
    }

    private void handleUpgradePlant(CollectionCommand cmd, User user) {
        PlantUpgradeService.Result result = plantUpgradeService.upgrade(
                user,
                cmd.getTargetName()
        );

        switch (result) {
            case NOT_OWNED -> view.showError(
                    SystemMessage.COLLECTION_ITEM_NOT_FOUND.getMessage());
            case MAX_LEVEL -> view.showError(
                    SystemMessage.COLLECTION_MAX_LEVEL_REACHED.getMessage());
            case NOT_ENOUGH_COINS -> view.showError(
                    SystemMessage.COLLECTION_NOT_ENOUGH_COINS.getMessage());
            case NOT_ENOUGH_SEEDS -> view.showError(
                    SystemMessage.COLLECTION_NOT_ENOUGH_SEEDS.getMessage());
            case SUCCESS -> {
                String username = user.getUsername();
                if (userManager.save()) {
                    view.showSuccess(SystemMessage.COLLECTION_PLANT_UPGRADED.getMessage());
                } else {
                    userManager.reload();
                    appState.setCurrentUser(userManager.find(
                            candidate -> candidate.getUsername().equals(username)
                    ));
                    view.showError("Failed to save game data. Plant upgrade reverted.");
                }
            }
        }
    }

    private void handleShowZombies(User user) {
        view.showSuccess("Seen Zombies:");

        if (user.getSeenZombies().isEmpty()) {
            view.showSuccess("No zombies discovered yet.");
            return;
        }

        user.getSeenZombies()
                .stream()
                .map(this::resolveSeenZombieName)
                .forEach(view::showSuccess);
    }

    private String resolveSeenZombieName(String storedValue) {
        String normalized = storedValue.strip().toLowerCase(Locale.ROOT);
        ZombieSpec spec = zombieData.byId().get(normalized);
        if (spec == null) {
            spec = zombieData.byName().get(normalized);
        }
        return spec == null ? storedValue : spec.getName();
    }

    private void handleShowAllZombies() {
        view.showSuccess("All Zombies:");

        zombieData.byId()
                .values()
                .forEach(z ->
                        view.showSuccess(z.getName()));
    }

    private void handleShowZombieDetails(CollectionCommand cmd) {

        ZombieSpec spec =
                zombieData.byName().get(cmd.getTargetName().toLowerCase(Locale.ROOT));

        if (spec == null) {
            view.showError(SystemMessage.COLLECTION_ITEM_NOT_FOUND.getMessage());
            return;
        }

        view.showSuccess("Name: " + spec.getName());
        view.showSuccess("Hitpoints: " + spec.getHitpoints());
        view.showSuccess("Eat DPS: " + spec.getEatDps());
        view.showSuccess("Speed: " + spec.getSpeed());
        view.showSuccess("Wave Cost: " + spec.getWaveCost());
        view.showSuccess("Armor: " + spec.getArmor());
    }
}
