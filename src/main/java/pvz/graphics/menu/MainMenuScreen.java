package pvz.graphics.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import pvz.controller.MainMenuController;
import pvz.graphics.BaseScreen;
import pvz.graphics.PvzGame;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import pvz.view.MenuView;

public class MainMenuScreen extends BaseScreen {

    private static final float LOGO_WIDTH = 400f, LOGO_HEIGHT = 90f;
    private static final float CONTENT_WIDTH = 700f, CONTENT_HEIGHT = 350f;
    private static final float PLAY_WIDTH = 200f, PLAY_HEIGHT = 55f;
    private static final float MENU_ICON_SIZE = 50f;
    private static final float COIN_WIDTH = 150f;
    private static final float TEXT_Y_OFFSET = 17f;

    private final MainMenuController controller;

    private SettingsScreen settingsScreen;
    private ProfileScreen profileScreen;
    private NewsScreen newsScreen;

    private Label premiumLabel;
    private Label coinLabel;
    private Label welcomeLabel;

    public MainMenuScreen(PvzGame game, TextureBank textures, SpriteBatch batch,
                          Skin skin, AppState appState, UserManager userManager) {
        super(game, textures, batch, skin, appState, userManager, "IMAGE_MAINMENU_BACKGROUND");

        controller = new MainMenuController(appState, userManager, new MenuView() {
            @Override
            public void showSuccess(String message) {
                game.setScreen(new LoginScreen(game, textures, batch, skin, appState, userManager));
            }

            @Override
            public void showError(String message) {
                MainMenuScreen.this.showMessage(message);
            }

            @Override
            public void showMessage(String message) {
                MainMenuScreen.this.showMessage(message);
            }

            @Override
            public void showRegisterWelcome() {
            }
        });

        buildUI();
    }

    private void buildUI() {
        buildMainContent();
        buildLogoutButton();
        buildCurrencies();
        buildBottomLeftMenu();
        buildBottomRightMenu();
        buildSettingsOverlay();
        buildProfileOverlay();
        buildNewsOverlay();
    }

    private void buildMainContent() {
        Image logo = image("IMAGE_UI_MAINMENU_PVZ2_LOGO_HORIZONTAL");
        logo.setSize(LOGO_WIDTH, LOGO_HEIGHT);
        logo.setPosition((WIDTH - LOGO_WIDTH) / 2f, HEIGHT - LOGO_HEIGHT - 35f);
        stage.addActor(logo);

        welcomeLabel = new Label("", skin);
        welcomeLabel.setColor(Color.BROWN);
        welcomeLabel.setFontScale(0.95f);
        welcomeLabel.setAlignment(Align.center);

        Table userBox = new Table();
        userBox.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        userBox.add(welcomeLabel).growX().pad(8f, 10f, 8f, 10f);
        userBox.setSize(230f, 55f);
        userBox.setPosition(90f, HEIGHT - 85f);
        stage.addActor(userBox);

        Image content = image("IMAGE_UI_MAINMENU_MAINMENU_CONTENT_OFFLINE");
        content.setSize(CONTENT_WIDTH, CONTENT_HEIGHT);
        content.setPosition((WIDTH - CONTENT_WIDTH) / 2f, 175f);
        stage.addActor(content);

        TextButton play = new TextButton("PLAY", skin, "green");
        play.setSize(PLAY_WIDTH, PLAY_HEIGHT);
        play.setPosition((WIDTH - PLAY_WIDTH) / 2f, 105f);
        stage.addActor(play);

        play.addListener(click(() -> game.setScreen(new GameMenuScreen(game, textures, batch, skin, appState, userManager))));
    }

    private void buildLogoutButton() {
        Image logout = image("IMAGE_UI_DRAPER_CLOSE_BUTTON");
        logout.setSize(55f, 55f);
        logout.setPosition(25f, HEIGHT - 85f);
        stage.addActor(logout);

        logout.addListener(click(() -> {
            try {
                controller.handle(new Command.MenuLogoutCommand());
            } catch (Exception e) {
                showMessage(e.getMessage() != null ? e.getMessage() : "Logout failed.");
            }
        }));
    }

    private void buildCurrencies() {
        TextureRegion premiumRegion = textures.region("IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL");
        TextureRegion coinRegion = textures.region("IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL");

        if (premiumRegion == null || coinRegion == null) {
            throw new IllegalStateException("Currency textures not found.");
        }

        premiumLabel = new Label(getPremiumCount(), skin);
        premiumLabel.setColor(Color.WHITE);
        Group premiumGroup = currencyGroup(premiumRegion, premiumLabel, premiumRegion.getRegionWidth(), 70f);

        coinLabel = new Label(getCoinCount(), skin);
        coinLabel.setColor(Color.WHITE);
        Group coinGroup = currencyGroup(coinRegion, coinLabel, COIN_WIDTH, 65f);

        premiumGroup.addListener(click(() -> {
            if (isDebugModeEnabled()) {
                appState.getCurrentUser().addDiamonds(100);
                updateCurrencyLabels();
                userManager.save();
            }
        }));

        coinGroup.addListener(click(() -> {
            if (isDebugModeEnabled()) {
                appState.getCurrentUser().addCoins(100);
                updateCurrencyLabels();
                userManager.save();
            }
        }));

        Table currencies = new Table();
        currencies.add(premiumGroup).width(premiumRegion.getRegionWidth()).height(premiumRegion.getRegionHeight()).padRight(10f);
        currencies.add(coinGroup).width(COIN_WIDTH).height(coinRegion.getRegionHeight());
        currencies.pack();

        currencies.setPosition(WIDTH - currencies.getWidth() - 20f, HEIGHT - currencies.getHeight() - 20f);
        stage.addActor(currencies);
    }

    private Group currencyGroup(TextureRegion region, Label label, float width, float textX) {
        Group group = new Group();
        float height = region.getRegionHeight();
        group.setSize(width, height);

        Image image = new Image(region);
        image.setSize(width, height);
        group.addActor(image);

        label.pack();
        label.setPosition(textX, TEXT_Y_OFFSET);
        group.addActor(label);

        return group;
    }

    private void updateCurrencyLabels() {
        if (premiumLabel != null) {
            premiumLabel.setText(getPremiumCount());
            premiumLabel.pack();
        }
        if (coinLabel != null) {
            coinLabel.setText(getCoinCount());
            coinLabel.pack();
        }
    }

    private void updateWelcomeLabel() {
        if (welcomeLabel == null) {
            return;
        }

        User user = appState.getCurrentUser();
        String displayName = "Guest";
        if (user != null) {
            String nickname = user.getNickname();
            displayName = nickname != null && !nickname.isBlank()
                    ? nickname
                    : user.getUsername();
        }

        welcomeLabel.setText("WELCOME!  " + truncateDisplayName(displayName));
    }

    private static String truncateDisplayName(String value) {
        if (value == null || value.isBlank()) {
            return "Guest";
        }
        int maxLength = 18;
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private boolean isDebugModeEnabled() {
        return appState.getCurrentUser() != null && appState.getCurrentUser().isDebugMode();
    }

    private void buildBottomLeftMenu() {
        TextButton profile = new TextButton("", skin, "brown");
        Image prof = new Image(textures.region("IMAGE_UI_MAINMENU_MM_PLAYERICON"));
        profile.add(prof).size(25f, 25f).center();

        Image miniGames = image("IMAGE_UI_GENERIC_BUTTON_HUD_MINIGAMES_ALT_SELECTED");
        miniGames.setSize(MENU_ICON_SIZE, MENU_ICON_SIZE);

        TextButton leaderboard = new TextButton("", skin, "brown");
        Image cup = new Image(textures.region("IMAGE_UI_GAMECENTER_ICON"));
        leaderboard.add(cup).size(35f, 35f).center();

        profile.addListener(click(() -> {
            if (profileScreen != null) profileScreen.show();
        }));

        miniGames.addListener(click(() -> game.setScreen(
                new MinigamesScreen(
                        game,
                        textures,
                        batch,
                        skin,
                        appState,
                        userManager
                )
        )));

        leaderboard.addListener(click(() -> game.setScreen(
                new LeaderboardScreen(
                        game,
                        textures,
                        batch,
                        skin,
                        appState,
                        userManager
                )
        )));

        Table bottomLeft = new Table();
        bottomLeft.add(profile).size(MENU_ICON_SIZE).padRight(6f);
        bottomLeft.add(miniGames).size(MENU_ICON_SIZE).padRight(6f);
        bottomLeft.add(leaderboard).size(MENU_ICON_SIZE).padRight(6f);
        bottomLeft.pack();
        bottomLeft.setPosition(20f, 20f);
        stage.addActor(bottomLeft);
    }

    private void buildBottomRightMenu() {
        Image news = image("IMAGE_UI_HUD_NEWSBUTTON_BUTTONS_HUD_NEWS_NORMAL");
        Image settings = image("IMAGE_UI_HUD_SETTINGSBUTTON_BUTTONS_HUD_SETTINGS_NORMAL");

        news.setSize(MENU_ICON_SIZE, MENU_ICON_SIZE);
        settings.setSize(MENU_ICON_SIZE, MENU_ICON_SIZE);

        Group newsGroup = new Group();
        newsGroup.setSize(MENU_ICON_SIZE, MENU_ICON_SIZE);
        newsGroup.addActor(news);

        Image unreadMark = image("IMAGE_UI_CLAIM_SMALL");
        unreadMark.setSize(20f, 20f);
        unreadMark.setPosition(0f, MENU_ICON_SIZE - 20f);
        newsGroup.addActor(unreadMark);

        unreadMark.setVisible(appState.getCurrentUser() != null && appState.getCurrentUser().hasUnreadNews());

        newsGroup.addListener(click(() -> {
            if (newsScreen != null) {
                newsScreen.show();
                unreadMark.setVisible(
                        appState.getCurrentUser() != null
                                && appState.getCurrentUser().hasUnreadNews()
                );
            }
        }));

        settings.addListener(click(() -> {
            if (settingsScreen != null) settingsScreen.show();
        }));

        Table bottomRight = new Table();
        bottomRight.add(newsGroup).size(MENU_ICON_SIZE).padRight(8f);
        bottomRight.add(settings).size(MENU_ICON_SIZE);
        bottomRight.pack();
        bottomRight.setPosition(WIDTH - bottomRight.getWidth() - 20f, 20f);

        stage.addActor(bottomRight);
    }

    private void buildSettingsOverlay() {
        settingsScreen = new SettingsScreen(
                textures,
                skin,
                appState,
                userManager,
                MenuName.MAIN
        );
        settingsScreen.setSize(WIDTH, HEIGHT);
        settingsScreen.setPosition(0f, 0f);
        settingsScreen.setVisible(false);
        stage.addActor(settingsScreen);
    }

    private void buildProfileOverlay() {
        profileScreen = new ProfileScreen(textures, skin, appState, userManager);
        profileScreen.setSize(WIDTH, HEIGHT);
        profileScreen.setPosition(0f, 0f);
        profileScreen.setVisible(false);
        stage.addActor(profileScreen);
    }

    private void buildNewsOverlay() {
        newsScreen = new NewsScreen(textures, skin, appState, userManager);
        newsScreen.setSize(WIDTH, HEIGHT);
        newsScreen.setPosition(0f, 0f);
        newsScreen.setVisible(false);
        stage.addActor(newsScreen);
    }

    private String getPremiumCount() {
        return appState.getCurrentUser() == null ? "0" : String.valueOf(appState.getCurrentUser().getDiamonds());
    }

    private String getCoinCount() {
        return appState.getCurrentUser() == null ? "0" : String.valueOf(appState.getCurrentUser().getCoins());
    }

    private void showMessage(String message) {
        Label label = new Label(message == null ? "Error" : message, skin);
        label.setColor(Color.YELLOW);
        label.pack();
        label.setPosition((WIDTH - label.getWidth()) / 2f, 45f);

        label.getColor().a = 0f;
        label.addAction(Actions.sequence(
                Actions.fadeIn(0.2f),
                Actions.moveBy(0f, 30f, 2f, Interpolation.sineOut),
                Actions.fadeOut(0.3f),
                Actions.removeActor()
        ));

        stage.addActor(label);
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

    @Override
    public void show() {
        super.show();
        appState.setCurrentMenu(MenuName.MAIN);
        updateCurrencyLabels();
        updateWelcomeLabel();
    }

    @Override
    public void dispose() {
        if (settingsScreen != null) {
            settingsScreen.dispose();
        }
        if (profileScreen != null) {
            profileScreen.dispose();
        }
        if (newsScreen != null) {
            newsScreen.dispose();
        }
        super.dispose();
    }
}
