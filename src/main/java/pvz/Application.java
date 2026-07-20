package pvz;

import pvz.controller.*;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.service.AuthService;
import pvz.model.command.Command;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import pvz.view.commandparser.MainMenuParser;
import pvz.view.commandparser.RegisterParser;
import pvz.view.commandparser.LoginParser;
import pvz.view.commandparser.ProfileParser;
import pvz.view.commandparser.SettingsParser;
import pvz.view.commandparser.GameMenuParser;
import pvz.view.commandparser.NewsMenuParser;
import pvz.view.commandparser.CollectionMenuParser;
import pvz.view.commandparser.PlantSelectionMenuParser;
import pvz.controller.CollectionController;
import pvz.model.utils.SystemMessage;
import pvz.data.PlantCsvLoader;
import pvz.data.PlantData;
import java.io.IOException;
import java.util.Scanner;
import pvz.view.ConsoleView;
import pvz.view.MenuView;

public class Application {

    private final AppState appState = new AppState();
    private PlantData plantData;
    private final UserManager userManager = new UserManager();

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
    private final ProfileController profileController = new ProfileController(appState, userManager, view);
    private final NewsController newsController = new NewsController(appState, userManager, view);
    private CollectionController collectionController;
    private PlantSelectionController plantSelectionController;

    public void run() {
        User activeSessionUser = userManager.find(u -> u.isStayLoggedIn());

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

        try {
            this.plantData = PlantCsvLoader.load("assets/Data/plants.csv");
            this.collectionController = new CollectionController(appState, userManager, view, plantData);
            this.plantSelectionController = new PlantSelectionController(appState, userManager, view, plantData);
        } catch (IOException e) {
            view.showError(SystemMessage.LOADING_DATA_FAILED.getMessage());
            return;
        }
        Scanner scanner = new Scanner(System.in);
        while (appState.isRunning()) {
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine();

            Command command = parser.parse(input);

            if (command instanceof Command.RawTextCommand) {
                switch (appState.getCurrentMenu()) {
                    case REGISTER -> command = registerParser.parse(input);
                    case LOGIN    -> command = loginParser.parse(input);
                    case PROFILE  -> command = profileParser.parse(input);
                    case SETTINGS -> command = settingsParser.parse(input);
                    case GAME     -> command = gameMenuParser.parse(input);
                    case NEWS     -> command = newsMenuParser.parse(input);
                    case COLLECTION -> command = collectionParser.parse(input);
                    case PLANT_SELECTION -> command = plantSelectionParser.parse(input);
                }
            }

            handleCommand(command);
        }
        scanner.close();
    }

    private void handleCommand(Command command) {
        if (command == null || command instanceof Command.EmptyCommand) return;

        switch (appState.getCurrentMenu()) {
            case REGISTER -> registerController.handle(command);
            case LOGIN    -> loginController.handle(command);
            case MAIN     -> mainMenuController.handle(command);
            case GAME     -> gameMenuController.handle(command);
            case SETTINGS -> settingsController.handle(command);
            case PROFILE  -> profileController.handle(command);
            case NEWS     -> newsController.handle(command);
            case COLLECTION -> collectionController.handle(command);
            case PLANT_SELECTION -> plantSelectionController.handle(command);
        }
    }
}