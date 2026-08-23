package pvz.controller;

import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.command.GameMenuCommand;
import pvz.model.utils.*;
import pvz.view.MenuView;
import pvz.model.adventure.LevelCatalog;

import java.util.Objects;

public class GameMenuController extends BaseController {
    private final LevelCatalog levelCatalog;

    public GameMenuController(
            AppState appState,
            UserManager userManager,
            MenuView view,
            LevelCatalog levelCatalog
    ) {
        super(appState, userManager, view);
        this.levelCatalog = Objects.requireNonNull(
                levelCatalog,
                "level catalog cannot be null"
        );
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

                if (!isValidChapterName(chapterName)) {
                    view.showError(SystemMessage.INVALID_COMMAND.getMessage());
                    return null;
                }

                if (!currentUser.isChapterUnlocked(chapterName)) {
                    view.showError(SystemMessage.CHAPTER_LOCKED.getMessage());
                    return null;
                }

                appState.setSelectedChapter(chapterName);
                appState.setSelectedLevelId(null);
                appState.setCurrentMenu(MenuName.CHAPTER);

                view.showSuccess(
                        SystemMessage.ENTERED_CHAPTER.getMessage()
                                + " "
                                + chapterName
                );
                view.showMessage(
                        "Use 'show levels', then "
                                + "'select level -l <id-or-number>'."
                );
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
                    view.showSuccess("diamonds: " + currentUser.getDiamonds());
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
        return levelCatalog.findChapter(name) != null;
    }
}
