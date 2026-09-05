package pvz;

import pvz.controller.*;
import pvz.data.ZombieCsvLoader;
import pvz.data.ZombieData;
import pvz.data.AdventureCsvLoader;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.entity.zombie.ZombieFactory;
import pvz.model.service.AuthService;
import pvz.model.command.Command;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuHelp;
import pvz.model.utils.MenuName;
import pvz.view.commandparser.*;
import pvz.model.utils.SystemMessage;
import pvz.data.PlantCsvLoader;
import pvz.data.PlantData;
import java.io.IOException;
import java.util.Scanner;
import pvz.view.ConsoleView;
import pvz.view.MenuView;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.session.GameRuntime;
import pvz.model.session.GameSessionFactory;
import pvz.model.session.GameSessionConfigFactory;
import pvz.model.session.GameSessionStatus;
import pvz.model.adventure.AdventureData;
import pvz.model.adventure.LevelProgressService;
import pvz.model.core.BattleWallet;
import pvz.model.core.BattleResources;
import pvz.model.session.BattleRewardSettlement;

public class Application {

    private final AppState appState = new AppState();
    private PlantData plantData;
    private ZombieData zombieData;
    private GameRuntime gameRuntime;
    private LevelProgressService levelProgressService;
    private final BattleRewardSettlement battleRewardSettlement =
            new BattleRewardSettlement();

    private final UserManager userManager = new UserManager("save.json");

    private final MainMenuParser parser = new MainMenuParser();
    private final RegisterParser registerParser = new RegisterParser();
    private final LoginParser loginParser = new LoginParser();
    private final ProfileParser profileParser = new ProfileParser();
    private final SettingsParser settingsParser = new SettingsParser();
    private final GameMenuParser gameMenuParser = new GameMenuParser();
    private final NewsMenuParser newsMenuParser = new NewsMenuParser();
    private final CollectionMenuParser collectionParser = new CollectionMenuParser();
    private final PlantSelectionMenuParser plantSelectionParser = new PlantSelectionMenuParser();
    private final ChapterMenuParser chapterParser = new ChapterMenuParser();
    private final GreenhouseParser greenhouseParser = new GreenhouseParser();
    private final ShopParser shopParser = new ShopParser();
    private final AuthService authService = new AuthService(userManager);
    private final MenuView view = new ConsoleView();

    private final RegisterController registerController = new RegisterController(appState, userManager, view);
    private final LoginController loginController = new LoginController(appState, userManager, authService, view);
    private final MainMenuController mainMenuController = new MainMenuController(appState, userManager, view);
    private GameMenuController gameMenuController;
    private final SettingsController settingsController = new SettingsController(appState, userManager, view);
    private final ProfileController profileController = new ProfileController(appState, userManager, authService, view);
    private final NewsController newsController = new NewsController(appState, userManager, view);
    private final TravelLogController travelLogController = new TravelLogController(appState, userManager, view);
    private final LeaderboardController leaderboardController = new LeaderboardController(appState, userManager, view);
    private final MinigamesController minigamesController = new MinigamesController(appState, userManager, view);
    private ChapterController chapterController;
    private GreenhouseController greenhouseController;
    private CollectionController collectionController;
    private PlantSelectionController plantSelectionController;
    private ShopController shopController;

    public void run() {
        initSession();
        if (!loadGameData()) return;

        try (Scanner scanner = new Scanner(System.in)) {
            while (appState.isRunning() && scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();

                if (appState.getCurrentMenu() == MenuName.PLAYING) {
                    handlePlayingInput(input);
                    continue;
                }

                Command command = parseCommand(input);
                handleCommand(command);
            }
        }
    }

    private void initSession() {
        User activeSessionUser = userManager.find(User::isStayLoggedIn);
        if (activeSessionUser != null) {
            appState.setCurrentUser(activeSessionUser);
            appState.setCurrentMenu(MenuName.MAIN);
            view.showMessage("Welcome back, " + activeSessionUser.getUsername() + "!");
            view.showMessage("--- MAIN MENU ---");
            for (String command : MenuHelp.MAIN) {
                view.showMessage(command);}
        } else {
            appState.setCurrentMenu(MenuName.REGISTER);
            view.showRegisterWelcome();
        }
    }

    private boolean loadGameData() {
        try {
            this.plantData = PlantCsvLoader.load("assets/Data/plants.csv");
            this.zombieData = ZombieCsvLoader.load("assets/Data/zombies.csv");
            AdventureData adventureData = AdventureCsvLoader.load(
                    "assets/Data/chapters.csv",
                    "assets/Data/levels.csv",
                    "assets/Data/level_zombies.csv",
                    "assets/Data/waves.csv",
                    zombieData
            );
            PlantFactory plantFactory = new PlantFactory(plantData.byName());
            ZombieFactory zombieFactory = new ZombieFactory(zombieData);
            GameSessionFactory sessionFactory = new GameSessionFactory(plantFactory, zombieFactory);
            GameSessionConfigFactory configFactory =
                    new GameSessionConfigFactory(adventureData);
            this.levelProgressService = new LevelProgressService(
                    adventureData.catalog()
            );
            this.gameRuntime = new GameRuntime(sessionFactory);
            this.gameMenuController = new GameMenuController(
                    appState,
                    userManager,
                    view,
                    adventureData.catalog()
            );
            this.chapterController = new ChapterController(
                    appState,
                    userManager,
                    view,
                    adventureData.catalog(),
                    levelProgressService
            );
            this.collectionController =
                    new CollectionController(
                            appState,
                            userManager,
                            view,
                            plantData,
                            zombieData
                    );
            this.plantSelectionController =
                    new PlantSelectionController(
                            appState,
                            userManager,
                            view,
                            plantData,
                            gameRuntime,
                            configFactory
                    );
            this.shopController =
                    new ShopController(
                            appState,
                            userManager,
                            view
                    );
            this.greenhouseController =
                    new GreenhouseController(
                            appState,
                            userManager,
                            view,
                            plantData
                    );

            return true;
        } catch (IOException | IllegalArgumentException e) {
            view.showError(SystemMessage.LOADING_DATA_FAILED.getMessage());
            view.showError(e.getMessage());
            return false;
        }
    }

    private void handlePlayingInput(String input) {
        if (input.equals("menu exit")) {
            gameRuntime.abort();
            finishGameSession();
            return;
        }

        String result = gameRuntime.handle(input);
        view.showMessage(result);

        if (gameRuntime.isFinished()) {
            finishGameSession();
        }
    }

    private void finishGameSession() {
        LevelProgressService.CompletionResult completion =
                recordLevelProgress();

        view.showMessage(
                "Game ended with status: " + gameRuntime.status()
        );

        boolean saved = transferBattleRewards(
                gameRuntime.session().resources()
        );
        if (saved) {
            showProgressResult(completion);
        }

        gameRuntime.clear();
        clearStagePreparation();
        appState.setCurrentMenu(MenuName.GAME);
    }

    private LevelProgressService.CompletionResult recordLevelProgress() {
        if (gameRuntime.status() != GameSessionStatus.WON) {
            return new LevelProgressService.CompletionResult(
                    false,
                    null,
                    null
            );
        }

        User currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            return new LevelProgressService.CompletionResult(
                    false,
                    null,
                    null
            );
        }

        return levelProgressService.completeLevel(
                currentUser,
                gameRuntime.session().config().levelId()
        );
    }

    private void showProgressResult(
            LevelProgressService.CompletionResult completion
    ) {
        if (!completion.newlyCompleted()) {
            return;
        }
        view.showSuccess("Level completion saved.");
        if (completion.unlockedChapterId() != null) {
            view.showSuccess(
                    "Chapter unlocked: "
                            + completion.unlockedChapterId()
            );
        }
        if (completion.unlockedLevelId() != null) {
            view.showSuccess(
                    "Next level available: "
                            + completion.unlockedLevelId()
            );
        }
    }

    private boolean transferBattleRewards(BattleResources resources) {
        User currentUser = appState.getCurrentUser();

        if (currentUser == null) {
            view.showError(
                    "Cannot transfer battle currency without a logged-in user."
            );
            return false;
        }

        BattleRewardSettlement.Result settlement;
        try {
            settlement = battleRewardSettlement.settle(
                    resources,
                    currentUser
            );
        } catch (ArithmeticException | IllegalStateException exception) {
            view.showError(
                    "Failed to settle stage rewards: "
                            + exception.getMessage()
            );
            return false;
        }

        if (!userManager.save()) {
            view.showError("Failed to save collected battle currency.");
            return false;
        }

        if (resources.battleWallet().hasCollectedCurrency()) {
            showBattleCurrencySummary(
                    resources.battleWallet(),
                    currentUser
            );
        }
        showBattleItemSummary(settlement, currentUser);
        return true;
    }

    private void showBattleItemSummary(
            BattleRewardSettlement.Result settlement,
            User currentUser
    ) {
        if (settlement.collectedPots() > 0) {
            view.showMessage(
                    "Collected this stage: "
                            + settlement.collectedPots()
                            + " pot(s); "
                            + settlement.unlockedPots()
                            + " greenhouse slot(s) unlocked."
            );
        }

        if (settlement.returnedPlantFood() > 0) {
            view.showMessage(
                    "Returned "
                            + settlement.returnedPlantFood()
                            + " unused plant food(s); total stored: "
                            + currentUser.getPlantFoodCount()
                            + "."
            );
        }
    }

    private void showBattleCurrencySummary(
            BattleWallet battleWallet,
            User currentUser
    ) {
        view.showMessage(
                "Collected this stage: "
                        + battleWallet.getCollectedCoins()
                        + " coins, "
                        + battleWallet.getCollectedDiamonds()
                        + " diamonds. Total wallet: "
                        + currentUser.getCoins()
                        + " coins, "
                        + currentUser.getDiamonds()
                        + " diamonds."
        );
    }

    private void clearStagePreparation(){
        plantSelectionController.resetSelection();
        appState.setSelectedChapter(null);
        appState.setSelectedLevelId(null);
    }

    private Command parseCommand(String input) {
        Command command = null;
        if (appState.getCurrentMenu() == MenuName.GAME) {
            command = gameMenuParser.parse(input);
        }

        if (command == null || command instanceof Command.RawTextCommand) {
            command = parser.parse(input);
            if (command instanceof Command.RawTextCommand) {
                return parseMenuSpecificCommand(input);
            }
        }
        return command;
    }

    private Command parseMenuSpecificCommand(String input) {
        return switch (appState.getCurrentMenu()) {
            case REGISTER -> registerParser.parse(input);
            case LOGIN -> loginParser.parse(input);
            case PROFILE -> profileParser.parse(input);
            case SETTINGS -> settingsParser.parse(input);
            case GAME -> gameMenuParser.parse(input);
            case NEWS -> newsMenuParser.parse(input);
            case COLLECTION -> collectionParser.parse(input);
            case PLANT_SELECTION -> plantSelectionParser.parse(input);
            case GREENHOUSE -> greenhouseParser.parse(input);
            case SHOP -> shopParser.parse(input);
            case CHAPTER -> chapterParser.parse(input);
            default -> null;
        };
    }

    private void handleCommand(Command command) {
        if (command == null) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return;
        }
        if (command instanceof Command.EmptyCommand) {
            return;
        }
        if (command instanceof Command.MenuEnterCommand menuEnterCmd) {
            if (!isValidMenuTransition(appState.getCurrentMenu(), menuEnterCmd.getMenuName())) {
                view.showError(SystemMessage.MENU_NAVIGATION_NOT_ALLOWED.getMessage());
                return;
            }
        }
        dispatchToController(command);
    }

    private void dispatchToController(Command command) {
        User userBeforeCommand = appState.getCurrentUser();

        switch (appState.getCurrentMenu()) {
            case REGISTER -> registerController.handle(command);
            case LOGIN -> loginController.handle(command);
            case MAIN -> mainMenuController.handle(command);
            case GAME -> gameMenuController.handle(command);
            case SETTINGS -> settingsController.handle(command);
            case PROFILE -> profileController.handle(command);
            case NEWS -> newsController.handle(command);
            case COLLECTION -> collectionController.handle(command);
            case PLANT_SELECTION -> plantSelectionController.handle(command);
            case GREENHOUSE -> greenhouseController.handle(command);
            case TRAVEL_LOG -> travelLogController.handle(command);
            case LEADERBOARD -> leaderboardController.handle(command);
            case MINIGAMES -> minigamesController.handle(command);
            case CHAPTER -> chapterController.handle(command);
            case SHOP -> shopController.handle(command);
        }

        if (userBeforeCommand != null && appState.getCurrentUser() == null) {
            clearStagePreparation();
        }
    }


    private boolean isValidMenuTransition(MenuName currentMenu, String targetMenuName) {
        String target = targetMenuName.trim().toLowerCase();
        return switch (currentMenu) {
            case REGISTER -> target.equals("login");

            case MAIN -> target.equals("game") || target.equals("settings") ||
                    target.equals("news") || target.equals("profile");

            case GAME ->
                    target.equals("collection") ||
                            target.equals("leaderboard") ||
                            target.equals("chapter") ||
                            target.equals("greenhouse") ||
                            target.equals("travel-log");

            case GREENHOUSE -> target.equals("shop");
            case SHOP -> target.equals("greenhouse");
            default -> false;
        };
    }
}
