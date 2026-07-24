package pvz.controller;

import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.command.ShopCommand;
import pvz.model.shop.DailyOffer;
import pvz.model.shop.ShopData;
import pvz.model.shop.ShopItem;
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
            view.showError(SystemMessage.SHOP_INVALID_COMMAND.getMessage());
            return null;
        }

        User currentUser = appState.getCurrentUser();
        if (currentUser == null) {
            view.showError(SystemMessage.SHOP_NOT_LOGGED_IN.getMessage());
            return null;
        }

        switch (cmd.getAction()) {
            case SHOW_LIST -> handleShowList();
            case SHOW_DAILY -> handleShowDaily(currentUser);
            case BUY -> handleBuy(currentUser, cmd);
        }

        return null;
    }

    private void handleShowList() {
        view.showSuccess("--- Daily Offer ---");
        view.showSuccess("ID: 6 | Daily Offer | Type 'shop daily' to view details");

        view.showSuccess("\n--- Standard Items ---");
        for (ShopItem item : ShopData.getAllItems()) {
            if (item.getId() == 6) continue;

            String price = item.getCoinPrice() > 0 ? item.getCoinPrice() + " Coins" : item.getDiamondPrice() + " Diamonds";
            view.showSuccess(String.format("ID: %d | %s | Price: %s | Quantity: %d",
                    item.getId(), item.getName(), price, item.getUnit()));
        }
    }

    private void handleShowDaily(User user) {
        try {
            DailyOffer offer = shopService.getOrGenerateDailyOffer(user);
            view.showSuccess(SystemMessage.SHOP_HEADER_DAILY.getMessage());
            view.showSuccess("Plant: " + offer.getPlantName());
            view.showSuccess("Contains: 10 Seed Packets");
            view.showSuccess("Price: " + offer.getPrice() + " Coins (-20% Off)");
            view.showSuccess("Status: " + (offer.isPurchased() ? "Already Purchased" : "Available"));
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    private void handleBuy(User user, ShopCommand cmd) {
        try {
            shopService.buy(user, cmd.getItemId(), cmd.getCount(), cmd.getPlantType());
            userManager.save();
            view.showSuccess(SystemMessage.SHOP_PURCHASE_SUCCESS.getMessage());
        } catch (Exception e) {
            view.showError(SystemMessage.SHOP_PURCHASE_FAILED.getMessage() + e.getMessage());
        }
    }
}