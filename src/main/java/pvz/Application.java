package pvz;

import pvz.controller.*;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.service.AuthService;
import pvz.model.command.Command;
import pvz.model.utils.AppState;
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

public class Application {

    private final AppState appState = new AppState();
    private PlantData plantData;
    private GameRuntime gameRuntime;

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
    private final AuthService authService = new AuthService(userManager);
    private final MenuView view = new ConsoleView();

    private final RegisterController registerController = new RegisterController(appState, userManager, view);
    private final LoginController loginController = new LoginController(appState, userManager, authService, view);
    private final MainMenuController mainMenuController = new MainMenuController(appState, userManager, view);
    private final GameMenuController gameMenuController = new GameMenuController(appState, userManager, view);
    private final SettingsController settingsController = new SettingsController(appState, userManager, view);
    private final ProfileController profileController = new ProfileController(appState, userManager, authService, view);
    private final NewsController newsController = new NewsController(appState, userManager, view);
    private final GreenhouseController greenhouseController = new GreenhouseController(appState, userManager, view);
    private final TravelLogController travelLogController = new TravelLogController(appState, userManager, view);
    private final LeaderboardController leaderboardController = new LeaderboardController(appState, userManager, view);
    private final ChapterController chapterController = new ChapterController(appState, userManager, view);

    private CollectionController collectionController;
    private PlantSelectionController plantSelectionController;

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
        } else {
            appState.setCurrentMenu(MenuName.REGISTER);
            view.showMessage("Welcome to Plants vs Zombies!");
            view.showMessage("To start playing, please register a new account.");
            view.showMessage("Already have an account? Just login!");
        }
    }

    private boolean loadGameData() {
        try {
            this.plantData = PlantCsvLoader.load("assets/Data/plants.csv");
            PlantFactory plantFactory = new PlantFactory(plantData.byName());
            GameSessionFactory sessionFactory = new GameSessionFactory(plantFactory);
            this.gameRuntime = new GameRuntime(sessionFactory);
            this.collectionController =
                    new CollectionController(
                            appState,
                            userManager,
                            view,
                            plantData
                    );
            this.plantSelectionController =
                    new PlantSelectionController(
                            appState,
                            userManager,
                            view,
                            plantData,
                            gameRuntime
                    );
            return true;
        } catch (IOException e) {
            view.showError(SystemMessage.LOADING_DATA_FAILED.getMessage());
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
        view.showMessage(
                "Game ended with status: " + gameRuntime.status()
        );

        gameRuntime.clear();
        appState.setSelectedChapter(null);
        appState.setCurrentMenu(MenuName.GAME);
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
            case CHAPTER -> chapterController.handle(command);
        }
    }

    private boolean isValidMenuTransition(MenuName currentMenu, String targetMenuName) {
        String target = targetMenuName.trim().toLowerCase();
        return switch (currentMenu) {
            case REGISTER -> target.equals("login");
            case MAIN -> target.equals("game") || target.equals("settings") ||
                         target.equals("news") || target.equals("profile");
            case GAME -> target.equals("collection");
            default -> false;
        };
    }
}