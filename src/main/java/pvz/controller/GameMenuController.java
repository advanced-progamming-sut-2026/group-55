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
                String chapterName = gameCmd.getStringArg();
                if (currentUser.isChapterUnlocked(chapterName)) {
                    appState.setCurrentMenu(MenuName.GAME);
                    view.showSuccess("Entered chapter: " + chapterName);
                } else {
                    view.showError(SystemMessage.CHAPTER_LOCKED.getMessage());
                }
            }

            case CHANGE_WORLD -> {
                String worldName = gameCmd.getStringArg();
                view.showSuccess("Switched to world: " + worldName);
            }

            case ENTER_COLLECTION -> handleMenuEnter(MenuName.COLLECTION);
            case GREENHOUSE       -> handleMenuEnter(MenuName.GREENHOUSE);
            case TRAVEL_LOG       -> handleMenuEnter(MenuName.TRAVEL_LOG);
            case LEADERBOARD      -> handleMenuEnter(MenuName.LEADERBOARD);

            case COIN_WALLET -> {
                view.showMessage("Coins: " + currentUser.getCoins());
            }

            case GEM_WALLET -> {
                view.showMessage("Diamonds: " + currentUser.getDiamonds());
            }

            case CHEAT_ADD -> {
                int amount = gameCmd.getIntArg();
                String resource = gameCmd.getStringArg().toLowerCase();

                if (resource.equals("coin")) {
                    currentUser.addCoins(amount);
                } else if (resource.equals("diamond")) {
                    currentUser.addDiamonds(amount);
                }
                userManager.save();
                view.showSuccess("Added " + amount + " " + resource + "(s).");
            }
        }

        return null;
    }
}