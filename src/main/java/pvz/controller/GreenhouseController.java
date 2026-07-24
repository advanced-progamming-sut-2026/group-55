package pvz.controller;

import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.command.GreenhouseCommand;
import pvz.model.greenhouse.Greenhouse;
import pvz.model.greenhouse.Pot;
import pvz.data.PlantData;
import pvz.model.service.GreenhouseService;
import pvz.model.utils.AppState;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

public class GreenhouseController extends BaseController {

    private final GreenhouseService greenhouseService;

    public GreenhouseController(AppState appState, UserManager userManager, MenuView view, PlantData plantData) {
        super(appState, userManager, view);
        this.greenhouseService = new GreenhouseService(plantData);
    }

    @Override
    protected Message handleSpecificCommand (Command command) {
        if (!(command instanceof GreenhouseCommand cmd)) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }

        User currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            view.showError(SystemMessage.USER_NOT_LOGGED_IN.getMessage());
            return null;
        }

        switch (cmd.getAction()) {
            case SHOW -> handleShow(currentUser);

            case PLANT -> {
                try {
                    greenhouseService.plant(currentUser, cmd.getX(), cmd.getY());
                    userManager.save();
                    view.showSuccess(SystemMessage.GREENHOUSE_PLANTED_SUCCESS.getMessage());
                } catch (Exception e) {
                    view.showError(e.getMessage());
                }
            }

            case COLLECT -> {
                try {
                    greenhouseService.collect(currentUser, cmd.getX(), cmd.getY());
                    userManager.save();
                    view.showSuccess(SystemMessage.GREENHOUSE_COLLECTED_SUCCESS.getMessage());
                } catch (Exception e) {
                    view.showError(e.getMessage());
                }
            }

            case GROW -> {
                try {
                    greenhouseService.forceGrow(currentUser, cmd.getX(), cmd.getY());
                    userManager.save();
                    view.showSuccess(SystemMessage.GREENHOUSE_GROWN_SUCCESS.getMessage());
                } catch (Exception e) {
                    view.showError(e.getMessage());
                }
            }

            case UNLOCK -> {
                try {
                    greenhouseService.unlockPot(currentUser, cmd.getX(), cmd.getY());
                    userManager.save();
                    view.showSuccess(SystemMessage.GREENHOUSE_UNLOCKED_SUCCESS.getMessage());
                } catch (Exception e) {
                    view.showError(e.getMessage());
                }
            }
        }
        return null;
    }

    private void handleShow(User user) {
        Greenhouse greenhouse = user.getGreenhouse();
        greenhouse.updateAllPots();

        view.showSuccess("--- Greenhouse Status ---");
        for (int y = 1; y <= 4; y++) {
            StringBuilder rowStr = new StringBuilder();

            for (int x = 1; x <= 5; x++) {
                Pot pot = greenhouse.getPot(x, y);
                String potStatus = String.format("(%d,%d):", x, y);

                switch (pot.getState()) {
                    case LOCKED -> potStatus += "LOCKED";
                    case EMPTY -> potStatus += "EMPTY";
                    case READY -> potStatus += pot.getPlant().getPlantName() + "[READY]";
                    case GROWING -> potStatus += String.format("%s[%dh]",
                            pot.getPlant().getPlantName(),
                            pot.getPlant().getRemainingHours());
                }
                rowStr.append(String.format("%-25s", potStatus));
            }
            view.showSuccess(rowStr.toString());
        }
    }

}
