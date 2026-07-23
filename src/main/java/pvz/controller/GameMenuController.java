package pvz.controller;

import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.command.GameMenuCommand;
import pvz.model.utils.*;
import pvz.view.MenuView;

public class GameMenuController extends BaseController {

    public GameMenuController(AppState appState, UserManager userManager, MenuView view) {
        super(appState, userManager, view);
    }

    @Override
    protected Message handleSpecificCommand(Command command) {

        if (!(command instanceof GameMenuCommand gameCmd)) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }

        User currentUser = appState.getCurrentUser();

        switch (gameCmd.getAction()) {

            case ENTER_CHAPTER -> {
                String chapterName = gameCmd.getStringArg().toLowerCase();

                if (isValidChapterName(chapterName)) {
                    if (currentUser.isChapterUnlocked(chapterName)) {
                        view.showSuccess(SystemMessage.ENTERED_CHAPTER.getMessage() + " " + chapterName);
                        appState.setCurrentMenu(MenuName.CHAPTER);
                    } else {
                        view.showError(SystemMessage.CHAPTER_LOCKED.getMessage());}
                    }else {
                    view.showError(SystemMessage.INVALID_COMMAND.getMessage());
                    }
                }

            case ENTER_COLLECTION -> {
                appState.setCurrentMenu(MenuName.COLLECTION);
                view.showSuccess(SystemMessage.ENTERED_COLLECTION.getMessage());
            }

            case GREENHOUSE -> {
                appState.setCurrentMenu(MenuName.GREENHOUSE);
                view.showSuccess(SystemMessage.ENTERED_GREENHOUSE.getMessage());
            }

            case TRAVEL_LOG -> {
                appState.setCurrentMenu(MenuName.TRAVEL_LOG);
                view.showSuccess(SystemMessage.ENTERED_TRAVEL_LOG.getMessage());
            }

            case LEADERBOARD -> {
                appState.setCurrentMenu(MenuName.LEADERBOARD);
                view.showSuccess(SystemMessage.SHOWING_LEADERBOARD.getMessage());
            }

            case COIN_WALLET -> {
                if (currentUser != null) {
                    view.showSuccess("coins: " + currentUser.getCoins());
                } else {
                    view.showError(SystemMessage.USER_NOT_LOGGED_IN.getMessage());
                }
            }

            case GEM_WALLET -> {
                if (currentUser != null) {
                    view.showSuccess("gems: " + currentUser.getDiamonds());
                } else {
                    view.showError(SystemMessage.USER_NOT_LOGGED_IN.getMessage());
                }
            }

            case CHANGE_WORLD -> {
                view.showSuccess("world changed to " + gameCmd.getStringArg());
            }

            case CHEAT_ADD -> {
                if (currentUser != null) {
                    int amount = gameCmd.getIntArg();
                    String type = gameCmd.getStringArg();

                    if ("coin".equalsIgnoreCase(type)) currentUser.addCoins(amount);
                    else if ("diamond".equalsIgnoreCase(type)) currentUser.addDiamonds(amount);

                    userManager.save();
                    view.showSuccess("added " + amount + " " + type + "s");
                } else {
                    view.showError(SystemMessage.USER_NOT_LOGGED_IN.getMessage());
                }
            }

            default -> view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        }

        return null;
    }

    private boolean isValidChapterName(String name) {
        return name.equals("ancient-egypt") || name.equals("frostbite-caves") ||
                name.equals("big-wave-beach") || name.equals("dark-ages");
    }
}