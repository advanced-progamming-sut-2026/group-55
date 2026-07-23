package pvz.controller;

import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.utils.AppState;
import pvz.model.utils.Message;
import pvz.view.MenuView;
import pvz.model.utils.SystemMessage;

public class TravelLogController extends BaseController {

    public TravelLogController(AppState appState, UserManager userManager, MenuView view) {
        super(appState, userManager, view);
    }

    @Override
    protected Message handleSpecificCommand(Command command) {
        view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        return null;
    }
}