package pvz.controller;

import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.utils.*;
import pvz.view.MenuView;

public abstract class BaseController implements Controller {
    protected final AppState appState;
    protected final UserManager userManager;
    protected final MenuView view;

    public BaseController(AppState appState, UserManager userManager, MenuView view) {
        this.appState = appState;
        this.userManager = userManager;
        this.view = view;
    }

    @Override
    public final Message handle(Command command) {
        if (command == null || command instanceof Command.EmptyCommand) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }
        if (handleGlobalCommand(command)) return null;

        return handleSpecificCommand(command);
    }

    protected abstract Message handleSpecificCommand(Command command);

    protected boolean handleGlobalCommand(Command command) {

        if (command instanceof Command.MenuShowCurrentCommand) {
            String currentMenuStr = appState.getCurrentMenu().name().toLowerCase();
            view.showMessage(currentMenuStr);
            return true;
        }

        if (command instanceof Command.MenuExitCommand) {
            handleMenuExit();
            return true;
        }

        if (command instanceof Command.MenuEnterCommand) {
            String inputName = (
                    (Command.MenuEnterCommand) command).getMenuName().toUpperCase().replace("-", "_");

            if (inputName.equals("PLAY")) {
                inputName = "GAME";
            }

            MenuName targetMenu = null;
            for (MenuName menu : MenuName.values()) {
                if (menu.name().equals(inputName)) {
                    targetMenu = menu;
                    break;
                }
            }

            if (targetMenu != null) {
                handleMenuEnter(targetMenu);
                return true;
            }
            return false;
        }

        if (command instanceof Command.MenuLogoutCommand) {
            if (appState.getCurrentMenu() == MenuName.MAIN) {
                if (appState.getCurrentUser() != null) {
                    appState.getCurrentUser().setStayLoggedIn(false);
                    userManager.save();
                }

                appState.setCurrentUser(null);
                appState.setCurrentMenu(MenuName.REGISTER);
                view.showSuccess(SystemMessage.LOGOUT_SUCCESS.getMessage());
            } else {
                view.showError(SystemMessage.LOGOUT_NOT_ALLOWED.getMessage());
            }
            return true;
        }

        return false;
    }

    protected void handleMenuExit() {
        switch (appState.getCurrentMenu()) {
            case REGISTER -> {
                appState.setRunning(false);
            }
            case LOGIN -> {
                appState.setCurrentMenu(MenuName.REGISTER);
                view.showSuccess(SystemMessage.MENU_ENTERED_REGISTER.getMessage());
            }
            case MAIN -> {
                view.showError(SystemMessage.LOGOUT_REQUIRED_MAIN.getMessage());
            }
            case CHAPTER, COLLECTION, GREENHOUSE, TRAVEL_LOG, LEADERBOARD -> {
                appState.setCurrentMenu(MenuName.GAME);
                view.showSuccess(SystemMessage.MENU_ENTERED_GAME.getMessage());
            }
            case GAME, SETTINGS, NEWS, PROFILE -> {
                appState.setCurrentMenu(MenuName.MAIN);
                view.showSuccess(SystemMessage.MENU_ENTERED_MAIN.getMessage());
            }
            default -> {
                appState.setCurrentMenu(MenuName.MAIN);
                view.showSuccess(SystemMessage.MENU_ENTERED_MAIN.getMessage());
            }
        }
    }

    protected void handleMenuEnter(MenuName targetMenu) {
        boolean isLoggedIn = (appState.getCurrentUser() != null);
        if (!isLoggedIn) {
            if (targetMenu != MenuName.REGISTER && targetMenu != MenuName.LOGIN) {
                view.showError(SystemMessage.MENU_REQUIRES_LOGIN.getMessage());
                return;
            }
        }
        else {
            if (targetMenu == MenuName.REGISTER || targetMenu == MenuName.LOGIN) {
                view.showError(SystemMessage.MENU_ALREADY_LOGGED_IN.getMessage());
                return;
            }
        }
        appState.setCurrentMenu(targetMenu);
        if (targetMenu == MenuName.GAME) {
            view.showSuccess(SystemMessage.MENU_ENTERED_GAME.getMessage());
        } else {
            view.showSuccess("menu entered " + targetMenu.name().toLowerCase());
        }
    }
}