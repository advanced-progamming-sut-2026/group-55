package pvz.graphics.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import pvz.controller.MainMenuController;
import pvz.graphics.BaseScreen;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.utils.AppState;
import pvz.view.MenuView;

public class MainMenuScreen extends BaseScreen {

    private static final float LOGO_WIDTH = 400f, LOGO_HEIGHT = 90f;
    private static final float CONTENT_WIDTH = 700f, CONTENT_HEIGHT = 350f;
    private static final float PLAY_WIDTH = 200f, PLAY_HEIGHT = 55f;
    private static final float MENU_ICON_SIZE = 50f;
    private static final float PREMIUM_TEXT_X = 70f, PREMIUM_TEXT_Y = 17f;
    private static final float COIN_TEXT_X = 65f, COIN_TEXT_Y = 17f;
    private static final float COIN_WIDTH = 150f;

    private final MainMenuController controller;
    private SettingsScreen settingsScreen;
    private ProfileScreen profileScreen;
    private NewsScreen newsScreen;

    public MainMenuScreen(Game game, TextureBank textures, SpriteBatch batch,
                          Skin skin, AppState appState, UserManager userManager) {
        super(game, textures, batch, skin, appState, userManager,
                "IMAGE_MAINMENU_BACKGROUND");

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

        Image content = image("IMAGE_UI_MAINMENU_MAINMENU_CONTENT_OFFLINE");
        content.setSize(CONTENT_WIDTH, CONTENT_HEIGHT);
        content.setPosition((WIDTH - CONTENT_WIDTH) / 2f, 175f);
        stage.addActor(content);

        TextButton play = new TextButton("PLAY", skin, "green");
        play.setSize(PLAY_WIDTH, PLAY_HEIGHT);
        play.setPosition((WIDTH - PLAY_WIDTH) / 2f, 105f);
        stage.addActor(play);

        play.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameMenuScreen(
                        game, textures, batch, skin, appState, userManager
                ));
            }
        });
    }

    private void buildLogoutButton() {
        Image logout = image("IMAGE_UI_DRAPER_CLOSE_BUTTON");
        logout.setSize(55f, 55f);
        logout.setPosition(25f, HEIGHT - 85f);
        stage.addActor(logout);

        logout.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    controller.handle(new Command.MenuLogoutCommand());
                } catch (Exception e) {
                    showMessage(e.getMessage() != null ? e.getMessage() : "Logout failed.");
                }
            }
        });
    }

    private void buildCurrencies() {
        TextureRegion premiumRegion = textures.region("IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL");
        TextureRegion coinRegion = textures.region("IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL");

        if (premiumRegion == null)
            throw new IllegalStateException("Texture not found: IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL");
        if (coinRegion == null)
            throw new IllegalStateException("Texture not found: IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL");

        float premiumWidth = premiumRegion.getRegionWidth();
        float premiumHeight = premiumRegion.getRegionHeight();
        float coinHeight = coinRegion.getRegionHeight();

        Image premium = new Image(premiumRegion);
        Image coin = new Image(coinRegion);

        Group premiumGroup = new Group();
        premiumGroup.setSize(premiumWidth, premiumHeight);
        premiumGroup.addActor(premium);

        Group coinGroup = new Group();
        coinGroup.setSize(COIN_WIDTH, coinHeight);
        coin.setSize(COIN_WIDTH, coinHeight);
        coinGroup.addActor(coin);

        Label premiumCount = new Label(getPremiumCount(), skin);
        Label coinCount = new Label(getCoinCount(), skin);
        premiumCount.setColor(Color.WHITE);
        coinCount.setColor(Color.WHITE);
        premiumCount.pack();
        coinCount.pack();
        premiumCount.setPosition(PREMIUM_TEXT_X, PREMIUM_TEXT_Y);
        coinCount.setPosition(COIN_TEXT_X, COIN_TEXT_Y);

        premiumGroup.addActor(premiumCount);
        coinGroup.addActor(coinCount);

        Table currencies = new Table();
        currencies.add(premiumGroup).width(premiumWidth).height(premiumHeight).padRight(10f);
        currencies.add(coinGroup).width(COIN_WIDTH).height(coinHeight);
        currencies.pack();
        currencies.setPosition(
                WIDTH - currencies.getWidth() - 20f,
                HEIGHT - currencies.getHeight() - 20f
        );
        stage.addActor(currencies);

        premiumGroup.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!isDebugModeEnabled() || appState.getCurrentUser() == null) return;

                appState.getCurrentUser().addDiamonds(100);
                premiumCount.setText(String.valueOf(appState.getCurrentUser().getDiamonds()));
                premiumCount.pack();
                premiumCount.setPosition(PREMIUM_TEXT_X, PREMIUM_TEXT_Y);
                userManager.save();
            }
        });

        coinGroup.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!isDebugModeEnabled() || appState.getCurrentUser() == null) return;

                appState.getCurrentUser().addCoins(100);
                coinCount.setText(String.valueOf(appState.getCurrentUser().getCoins()));
                coinCount.pack();
                coinCount.setPosition(COIN_TEXT_X, COIN_TEXT_Y);
                userManager.save();
            }
        });
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

        profile.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (profileScreen != null) profileScreen.show();
            }
        });

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

        unreadMark.setVisible(
                appState.getCurrentUser() != null && appState.getCurrentUser().hasUnreadNews());
        newsGroup.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (newsScreen != null) {
                    newsScreen.show();
                    unreadMark.setVisible(false);
                }
            }
        });

        settings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (settingsScreen != null) settingsScreen.show();}});

        Table bottomRight = new Table();
        bottomRight.add(newsGroup).size(MENU_ICON_SIZE).padRight(8f);
        bottomRight.add(settings).size(MENU_ICON_SIZE);
        bottomRight.pack();
        bottomRight.setPosition(WIDTH - bottomRight.getWidth() - 20f, 20f);

        stage.addActor(bottomRight);
    }

    private void buildSettingsOverlay() {
        settingsScreen = new SettingsScreen(textures, skin, appState, userManager);
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
        newsScreen = new NewsScreen(textures, skin, appState);
        newsScreen.setSize(WIDTH, HEIGHT);
        newsScreen.setPosition(0f, 0f);
        newsScreen.setVisible(false);
        stage.addActor(newsScreen);
    }

    private String getPremiumCount() {
        return appState.getCurrentUser() == null
                ? "0"
                : String.valueOf(appState.getCurrentUser().getDiamonds());
    }

    private String getCoinCount() {
        return appState.getCurrentUser() == null
                ? "0"
                : String.valueOf(appState.getCurrentUser().getCoins());
    }

    private void showMessage(String message) {
        Label label = new Label(message == null ? "Error" : message, skin);
        label.setColor(Color.WHITE);
        label.pack();
        label.setPosition((WIDTH - label.getWidth()) / 2f, 35f);
        stage.addActor(label);
    }
}
