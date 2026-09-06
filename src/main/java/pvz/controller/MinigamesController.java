package pvz.controller;

import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.utils.AppState;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

/** Console-side shell for the graphical minigames menu. */
public final class MinigamesController extends BaseController {
    public MinigamesController(
            AppState appState,
            UserManager userManager,
            MenuView view
    ) {
        super(appState, userManager, view);
    }

    @Override
    protected Message handleSpecificCommand(Command command) {
        view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        return null;
    }
}
