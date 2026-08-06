package pvz.controller;

import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.command.ShopCommand;
import pvz.model.service.ShopService;
import pvz.model.utils.AppState;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

public class ShopController extends BaseController {

    private final ShopService shopService;

    public ShopController(AppState appState, UserManager userManager, MenuView view) {
        super(appState, userManager, view);
        this.shopService = new ShopService();
    }

    @Override
    protected Message handleSpecificCommand(Command command) {
        if (!(command instanceof ShopCommand cmd)) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }

        User user = appState.getCurrentUser();
        if (user == null) {
            view.showError(SystemMessage.USER_NOT_LOGGED_IN.getMessage());
            return null;
        }

        switch (cmd.getAction()) {
            case SHOW_LIST -> handleShowList();
            case SHOW_DAILY -> handleShowDaily(user);
            case BUY -> handleBuy(user, cmd);
        }
        return null;
    }

    private void handleShowList() {
        view.showSuccess("--- Shop Items ---");
        pvz.model.shop.ShopData.getAllItems().forEach(i -> {
            String priceStr = i.getCoinPrice() > 0 ? i.getCoinPrice() + " coins" : i.getDiamondPrice() + " diamonds";
            view.showSuccess(i.getId() + ". " + i.getName() + " (" + i.getUnit() + "x) - Price: " + priceStr);
        });
    }

    private void handleShowDaily(User user) {
        try {
            var result = shopService.getOrGenerateDailyOffer(user);
            String status = result.offer().isPurchased() ? " [Purchased]" : "";

            view.showSuccess("Daily Offer (ID: 6): " + result.offer().getPlantName() +
                    " for " + result.offer().getPrice() + " coins" + status);

            if (result.newlyCreated()) {
                if (!userManager.save()) {
                    view.showError("Failed to save the new daily offer.");
                }
            }
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    private void handleBuy(User user, ShopCommand cmd) {
        try {
            if (cmd.getItemId() == 6) {
                var result = shopService.getOrGenerateDailyOffer(user);
                if (result.newlyCreated()) userManager.save();
            }

            shopService.buy(user, cmd.getItemId(), cmd.getCount(), cmd.getPlantType());


            if (userManager.save()) {
                view.showSuccess("Purchase successful!");
            } else {
                userManager.reload();
                appState.setCurrentUser(userManager.find(u -> u.getUsername().equals(user.getUsername())));
                view.showError("Failed to save game data. Purchase reverted.");
            }
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }
}
