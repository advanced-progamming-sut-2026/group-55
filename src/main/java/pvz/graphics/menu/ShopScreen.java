package pvz.graphics.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import pvz.controller.ShopController;
import pvz.graphics.BaseScreen;
import pvz.graphics.PvzGame;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.ShopCommand;
import pvz.model.service.GreenhouseService;
import pvz.model.service.ShopService;
import pvz.model.shop.DailyOffer;
import pvz.model.shop.DailyOfferResult;
import pvz.model.shop.ShopData;
import pvz.model.shop.ShopItem;
import pvz.model.utils.AppState;
import pvz.skin.BorderedTable;
import pvz.view.MenuView;

import java.util.List;

public class ShopScreen extends BaseScreen {

    private static final int SELECT_SEED_ID = 4;
    private static final int DAILY_OFFER_ID = 6;
    private static final float CARD_WIDTH = 155f;
    private static final float CARD_HEIGHT = 205f;
    private static final float PADDING = 12f;
    private static final float POPUP_WIDTH = 600f;
    private static final float POPUP_HEIGHT = 430f;

    private final ShopController shopController;
    private final ShopService readOnlyService;
    private final GreenhouseService greenhouseService;

    private Label diamondLabel;
    private Label coinLabel;
    private Table storeGrid;

    public ShopScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager,
            GreenhouseService greenhouseService
    ) {
        super(game, textures, batch, skin, appState, userManager, "IMAGE_MAINMENU_BACKGROUND");

        this.greenhouseService = greenhouseService;
        this.readOnlyService = new ShopService();

        this.shopController = new ShopController(appState, userManager, new MenuView() {
            @Override
            public void showSuccess(String message) {
                updateCurrencyLabels();
                refreshStoreGrid();
                showToast("Purchase Successful!", Color.GREEN);
            }

            @Override
            public void showError(String message) {
                showToast(message, Color.RED);
            }

            @Override
            public void showMessage(String message) {
                showToast(message, Color.WHITE);
            }

            @Override
            public void showRegisterWelcome() {}
        });

        buildUI();
    }

    private void buildUI() {
        buildTopBar();
        buildCurrencies();
        buildCentralContainer();
    }

    private void buildTopBar() {
        TextureRegion normal = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_NORMAL");
        TextureRegion pressed = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_PRESSED");

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = normal != null ? new TextureRegionDrawable(normal) : null;
        style.down = pressed != null ? new TextureRegionDrawable(pressed) : style.up;

        ImageButton back = new ImageButton(style);
        back.setBounds(25f, HEIGHT - 80f, 55f, 55f);
        back.addListener(click(() -> game.setScreen(new GreenhouseScreen(
                game, textures, batch, skin, appState, userManager, greenhouseService))));

        stage.addActor(back);
    }

    private void buildCurrencies() {
        TextureRegion diamondRegion = textures.region("IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL");
        TextureRegion coinRegion = textures.region("IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL");

        Group currencies = new Group();

        diamondLabel = new Label(getDiamondCount(), skin);
        diamondLabel.setColor(Color.WHITE);
        Group diamondGroup = currencyGroup(diamondRegion,
                diamondLabel, diamondRegion != null ? diamondRegion.getRegionWidth() : 100f);

        coinLabel = new Label(getCoinCount(), skin);
        coinLabel.setColor(Color.WHITE);
        Group coinGroup = currencyGroup(coinRegion, coinLabel, 150f);

        diamondGroup.setTouchable(Touchable.enabled);
        diamondGroup.addListener(click(() -> {
            if (isDebugModeEnabled()) {
                appState.getCurrentUser().addDiamonds(1000);
                updateCurrencyLabels();
                userManager.save();
            }
        }));

        coinGroup.setTouchable(Touchable.enabled);
        coinGroup.addListener(click(() -> {
            if (isDebugModeEnabled()) {
                appState.getCurrentUser().addCoins(1000);
                updateCurrencyLabels();
                userManager.save();
            }
        }));

        diamondGroup.setPosition(0f, 0f);
        coinGroup.setPosition((diamondRegion != null ? diamondRegion.getRegionWidth() : 100f) + 10f, 0f);

        currencies.addActor(diamondGroup);
        currencies.addActor(coinGroup);

        float curWidth = (diamondRegion != null ? diamondRegion.getRegionWidth() : 100f) + 160f;
        float curHeight = Math.max(diamondRegion != null ? diamondRegion.getRegionHeight() : 55f, coinRegion != null ? coinRegion.getRegionHeight() : 55f);

        currencies.setSize(curWidth, curHeight);
        currencies.setPosition(WIDTH - currencies.getWidth() - 20f, HEIGHT - currencies.getHeight() - 20f);

        stage.addActor(currencies);
    }

    private Group currencyGroup(TextureRegion region, Label label, float width) {
        Group group = new Group();
        float height = region == null ? 55f : region.getRegionHeight();
        group.setSize(width, height);

        if (region != null) {
            Image image = new Image(region);
            image.setSize(width, height);
            group.addActor(image);
        }

        label.pack();
        label.setPosition(width * 0.5f, 17f);
        group.addActor(label);

        return group;
    }

    private void buildCentralContainer() {
        BorderedTable centralBox = new BorderedTable();

        storeGrid = new Table();
        storeGrid.top().center();
        populateStoreGrid();

        ScrollPane scrollPane = new ScrollPane(storeGrid, skin);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);

        centralBox.add(scrollPane).width(540f).height(370f);
        centralBox.setSize(POPUP_WIDTH, POPUP_HEIGHT);
        centralBox.setPosition((WIDTH - POPUP_WIDTH) / 2f, (HEIGHT - POPUP_HEIGHT) / 2f - 15f);

        stage.addActor(centralBox);
    }

    private void populateStoreGrid() {
        int col = 0;

        try {
            User user = appState.getCurrentUser();
            if (user != null) {
                DailyOfferResult result = readOnlyService.getOrGenerateDailyOffer(user);
                if (result.newlyCreated()) {
                    userManager.save();
                }

                storeGrid.add(createDailyOfferCard(result.offer())).width(CARD_WIDTH).height(CARD_HEIGHT).pad(PADDING);
                col++;
            }
        } catch (Exception ignored) {}

        for (ShopItem item : ShopData.getAllItems()) {
            storeGrid.add(createItemCard(item)).width(CARD_WIDTH).height(CARD_HEIGHT).pad(PADDING);
            col++;
            if (col == 3) {
                storeGrid.row();
                col = 0;
            }
        }
    }

    private void refreshStoreGrid() {
        storeGrid.clearChildren();
        populateStoreGrid();
    }

    private Group createItemCard(ShopItem item) {
        return buildCardBase(
                item.getName(),
                item.getCoinPrice(),
                item.getDiamondPrice(),
                item.getId(),
                false
        );
    }

    private Group createDailyOfferCard(DailyOffer offer) {
        String title = "DAILY OFFER\n" + offer.getPlantName();
        return buildCardBase(
                title,
                offer.getPrice(),
                0,
                DAILY_OFFER_ID,
                offer.isPurchased()
        );
    }

    private Group buildCardBase(String title, int coinPrice, int diamondPrice, int itemId, boolean disabled) {
        Group card = new Group();
        card.setSize(CARD_WIDTH, CARD_HEIGHT);

        TextureRegion cardBgRegion = textures.region("IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD");
        if (cardBgRegion != null) {
            Image cardBg = new Image(cardBgRegion);
            cardBg.setSize(CARD_WIDTH, CARD_HEIGHT);
            card.addActor(cardBg);
        }

        String cleanTitle = title.replace("[YELLOW]", "").replace("[]", "").replace("\n", " ");
        Label titleLabel = new Label(cleanTitle, skin);
        titleLabel.setWrap(true);
        titleLabel.setAlignment(Align.center);
        titleLabel.setSize(CARD_WIDTH - 20f, 35f);
        titleLabel.setPosition(10f, CARD_HEIGHT - 38f);
        card.addActor(titleLabel);

        float warpX = 22f;
        float warpY = 60f;
        float warpWidth = CARD_WIDTH - 44f;
        float warpHeight = 85f;

        TextureRegion warpRegion = textures.region("IMAGE_UI_CARDS_BACKGROUNDS_CARD_PLANT_BG_WARP");
        if (warpRegion != null) {
            Image warpBg = new Image(warpRegion);
            warpBg.setSize(warpWidth, warpHeight);
            warpBg.setPosition(warpX, warpY);
            card.addActor(warpBg);
        }

        String textureName = getTextureNameForItem(itemId, cleanTitle);
        TextureRegion iconRegion = textures.region(textureName);
        if (iconRegion == null) {
            iconRegion = textures.region("IMAGE_UI_ALMANAC_SEEDPACKET");
        }

        if (iconRegion != null) {
            Image icon = new Image(iconRegion);
            float iconWidth = 54f;
            float iconHeight = 58f;
            icon.setSize(iconWidth, iconHeight);
            icon.setPosition((CARD_WIDTH - iconWidth) / 2f, warpY + (warpHeight - iconHeight) / 2f);
            card.addActor(icon);
        }

        String btnStyle = (itemId == DAILY_OFFER_ID) ? "purple" : "green";
        float btnWidth = 110f;
        float btnHeight = 42f;

        String buttonText;
        if (disabled) {
            buttonText = "SOLD OUT";
        } else if (itemId == SELECT_SEED_ID) {
            buttonText = "Select";
        } else {
            buttonText = getPriceText(coinPrice, diamondPrice);
        }

        TextButton buyBtn = new TextButton(buttonText, skin, btnStyle);
        buyBtn.setSize(btnWidth, btnHeight);
        buyBtn.setPosition((CARD_WIDTH - btnWidth) / 2f, 18f);
        buyBtn.getLabel().setFontScale(0.85f);
        buyBtn.setDisabled(disabled);

        if (!disabled) {
            buyBtn.addListener(click(() -> handleBuyClick(itemId, cleanTitle)));
        }

        card.addActor(buyBtn);
        return card;
    }

    private String getTextureNameForItem(int itemId, String title) {
        switch (itemId) {
            case 1: // Pot
                return "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161_2";
            case 2: // Plant Food
                return "IMAGE_UI_HUD_EVENTBUTTON_EVENT_ICON_POTW_UP";
            case 3: // Random Seed
                return "IMAGE_UI_STOREMULTI_SEEDPACKETICON";
            case 4: // Selected Seed
                return "IMAGE_UI_PACKETS_STARFRUIT";
            case 5: // Diamond Exchange
                return "IMAGE_EFFECTS_COIN_STACK_COIN_STACK_196X203";
            case DAILY_OFFER_ID:
                String plantClean = title.toLowerCase()
                        .replace("daily offer", "")
                        .trim()
                        .replace(" ", "")
                        .toUpperCase();
                String candidate = "IMAGE_UI_PACKETS_" + plantClean;
                if (textures.region(candidate) != null) {
                    return candidate;
                }
                break;
        }

        return "IMAGE_UI_ALMANAC_SEEDPACKET";
    }

    private String getPriceText(int coinPrice, int diamondPrice) {
        if (coinPrice > 0) return coinPrice + " Coins";
        if (diamondPrice > 0) return diamondPrice + " Gems";
        return "Free";
    }

    private void handleBuyClick(int itemId, String itemName) {
        String cleanName = itemName.replace("[YELLOW]", "").replace("[]", "").replace("\n", " ");

        if (itemId == SELECT_SEED_ID) {
            showCustomPlantSelectionPopup(itemId, cleanName);
        } else {
            showCustomConfirmPopup(itemId, cleanName, null);
        }
    }

    private Table createDimBackground() {
        Table dimBackground = new Table();
        dimBackground.setFillParent(true);
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0.6f);
        pixmap.fill();
        TextureRegionDrawable dimDrawable = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();
        dimBackground.setBackground(dimDrawable);
        dimBackground.setTouchable(Touchable.enabled);
        dimBackground.addListener(click(() -> {}));
        return dimBackground;
    }

    private BorderedTable createPopupBox() {
        BorderedTable popup = new BorderedTable();
        popup.setLayoutEnabled(false);
        popup.setSize(POPUP_WIDTH, POPUP_HEIGHT);
        popup.setPosition((WIDTH - POPUP_WIDTH) / 2f, (HEIGHT - POPUP_HEIGHT) / 2f - 15f);
        return popup;
    }

    private void showCustomPlantSelectionPopup(int itemId, String itemName) {
        User user = appState.getCurrentUser();
        if (user == null) return;

        List<PlayerPlant> unlocked = user.getUnlockedPlants();
        if (unlocked.isEmpty()) {
            showToast("No unlocked plants available!", Color.RED);
            return;
        }

        Table dimBackground = createDimBackground();
        BorderedTable popup = createPopupBox();

        TextureRegion normal = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_NORMAL");
        TextureRegion pressed = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_PRESSED");

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = normal != null ? new TextureRegionDrawable(normal) : null;
        style.down = pressed != null ? new TextureRegionDrawable(pressed) : style.up;

        ImageButton backBtn = new ImageButton(style);
        backBtn.setSize(40f, 40f);
        backBtn.setPosition(25f, POPUP_HEIGHT - 55f);
        backBtn.addListener(click(() -> {
            dimBackground.remove();
            popup.remove();
        }));
        popup.addActor(backBtn);

        Label header = new Label("Select Plant", skin);
        header.setFontScale(1.2f);
        header.setColor(Color.BLACK);
        header.setAlignment(Align.center);
        header.setSize(300f, 35f);
        header.setPosition((POPUP_WIDTH - 300f) / 2f, POPUP_HEIGHT - 60f);
        popup.addActor(header);

        Table plantTable = new Table();
        int col = 0;
        for (PlayerPlant plant : unlocked) {
            TextButton plantBtn = new TextButton(plant.getPlantName(), skin, "green");
            plantBtn.addListener(click(() -> {
                dimBackground.remove();
                popup.remove();
                showCustomConfirmPopup(itemId, itemName, plant.getPlantName());
            }));

            plantTable.add(plantBtn).width(150f).height(40f).pad(6f);
            col++;
            if (col == 3) {
                plantTable.row();
                col = 0;
            }
        }

        ScrollPane scroll = new ScrollPane(plantTable, skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);

        float scrollWidth = 540f;
        float scrollHeight = 330f;
        Table contentTable = new Table();
        contentTable.add(scroll).width(scrollWidth).height(scrollHeight);
        contentTable.setSize(scrollWidth, scrollHeight);
        contentTable.setPosition((POPUP_WIDTH - scrollWidth) / 2f, 15f);
        popup.addActor(contentTable);

        stage.addActor(dimBackground);
        stage.addActor(popup);
    }

    private void showCustomConfirmPopup(int itemId, String itemName, String plantType) {
        String message = plantType != null
                ? "Purchase " + itemName + " for " + plantType + "?"
                : "Purchase " + itemName + "?";

        Table dimBackground = createDimBackground();
        BorderedTable popup = createPopupBox();

        Label msgLabel = new Label(message, skin);
        msgLabel.setFontScale(1.1f);
        msgLabel.setAlignment(Align.center);
        msgLabel.setSize(500f, 50f);
        msgLabel.setPosition((POPUP_WIDTH - 500f) / 2f, POPUP_HEIGHT / 2f + 30f);
        popup.addActor(msgLabel);

        TextButton yesBtn = new TextButton("Yes", skin, "green");
        TextButton noBtn = new TextButton("No", skin, "green");

        yesBtn.addListener(click(() -> {
            dimBackground.remove();
            popup.remove();
            ShopCommand cmd = new ShopCommand(ShopCommand.Action.BUY, itemId, 1, plantType);
            try {
                shopController.handle(cmd);
            } catch (Exception e) {
                showToast("Transaction failed.", Color.RED);
            }
        }));

        noBtn.addListener(click(() -> {
            dimBackground.remove();
            popup.remove();
            if (itemId == SELECT_SEED_ID) {
                showCustomPlantSelectionPopup(itemId, itemName);
            }
        }));

        float btnWidth = 120f;
        float btnHeight = 45f;
        yesBtn.setSize(btnWidth, btnHeight);
        yesBtn.setPosition(POPUP_WIDTH / 2f - btnWidth - 15f, POPUP_HEIGHT / 2f - 40f);

        noBtn.setSize(btnWidth, btnHeight);
        noBtn.setPosition(POPUP_WIDTH / 2f + 15f, POPUP_HEIGHT / 2f - 40f);

        popup.addActor(yesBtn);
        popup.addActor(noBtn);

        stage.addActor(dimBackground);
        stage.addActor(popup);
    }

    private void showToast(String message, Color color) {
        Label label = new Label(message, skin);
        label.setColor(color);
        label.setFontScale(1.2f);
        label.pack();

        label.setPosition((WIDTH - label.getWidth()) / 2f, HEIGHT / 2f);
        label.getColor().a = 0f;

        label.addAction(Actions.sequence(
                Actions.fadeIn(0.2f),
                Actions.moveBy(0f, 50f, 2.5f, Interpolation.sineOut),
                Actions.fadeOut(0.3f),
                Actions.removeActor()
        ));

        stage.addActor(label);
    }

    private void updateCurrencyLabels() {
        if (diamondLabel != null) diamondLabel.setText(getDiamondCount());
        if (coinLabel != null) coinLabel.setText(getCoinCount());
    }

    private String getDiamondCount() {
        User user = appState.getCurrentUser();
        return user == null ? "0" : String.valueOf(user.getDiamonds());
    }

    private String getCoinCount() {
        User user = appState.getCurrentUser();
        return user == null ? "0" : String.valueOf(user.getCoins());
    }

    private boolean isDebugModeEnabled() {
        return appState.getCurrentUser() != null && appState.getCurrentUser().isDebugMode();
    }

    private ClickListener click(Runnable action) {
        return new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                action.run();
            }
        };
    }
}
