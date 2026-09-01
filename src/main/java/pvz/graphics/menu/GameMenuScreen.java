package pvz.graphics.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import pvz.data.PlantCsvLoader;
import pvz.data.PlantData;
import pvz.graphics.BaseScreen;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.service.GreenhouseService;
import pvz.model.utils.AppState;

import java.io.IOException;

public class GameMenuScreen extends BaseScreen {

    private static final float WORLD_WIDTH = 200f, WORLD_HEIGHT = 380f;
    private static final float SELECTED_SCALE = 1.12f, NORMAL_SCALE = 0.82f;
    private static final float SIDE_DISTANCE = 245f, WORLD_Y = 25f, CENTER_X_OFFSET = 0f;
    private static final float COIN_WIDTH = 150f;
    private static final float TEXT_Y_OFFSET = 17f;
    private static final Color LOCKED_COLOR = new Color(0.45f, 0.45f, 0.45f, 1f);

    private final Group worldContainer = new Group();
    private final Image[] worlds = new Image[5];
    private final Label[] worldLabels = new Label[5];

    private final GreenhouseService greenhouseService;

    private SettingsScreen settingsScreen;
    private int currentPage = 0;

    private Label premiumLabel;
    private Label coinLabel;

    private final String[] worldNames = {
            "EGYPT", "BIG WAVE BEACH", "DARK AGES", "FROSTBITE CAVES", "INVASION"
    };

    private final String[] worldTextures = {
            "IMAGE_UI_UNIVERSE_WORLDS_EGYPT",
            "IMAGE_UI_UNIVERSE_WORLDS_BEACH",
            "IMAGE_UI_UNIVERSE_WORLDS_DARK",
            "IMAGE_UI_UNIVERSE_WORLDS_ICEAGE",
            "IMAGE_UI_UNIVERSE_INVASION_UNIVERSE_PORTAL_INVASION_UNIVERSE_PORTAL_367X839"
    };

    private final boolean[] unlockedWorlds = {
            true, false, false, false, false
    };

    public GameMenuScreen(
            Game game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager
    ) {
        super(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager,
                "IMAGE_MAINMENU_BACKGROUND"
        );

        this.greenhouseService = createGreenhouseService();

        buildUI();
        buildSettingsOverlay();
        updateWorlds();
    }

    private GreenhouseService createGreenhouseService() {
        try {
            PlantData plantData =
                    PlantCsvLoader.load("assets/data/plants.csv");

            return new GreenhouseService(plantData);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load plant data from assets/data/plants.csv", e
            );
        }
    }

    private void buildUI() {
        buildTopBar();
        buildCurrencies();
        buildWorldArea();
    }

    private void buildTopBar() {
        Image back = image("IMAGE_UI_MAINMENU_BACK_BTN_NORMAL");
        Image greenhouse = image("IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_NORMAL");
        Image collection = image("IMAGE_UI_HUD_ALMANACBUTTON_BUTTONS_HUD_ALMANAC_NORMAL");
        Image settings = image("IMAGE_UI_HUD_SETTINGSBUTTON_BUTTONS_HUD_SETTINGS_NORMAL");

        float size = 55f, gap = 10f, y = HEIGHT - 80f;

        back.setSize(size, size);
        greenhouse.setSize(size, size);
        collection.setSize(size, size);
        settings.setSize(size, size);

        back.setPosition(25f, y);
        greenhouse.setPosition(25f + size + gap, y);
        collection.setPosition(25f + (size + gap) * 2f, y);
        settings.setPosition(25f + (size + gap) * 3f, y);

        back.addListener(click(() -> game.setScreen(new MainMenuScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager
        ))));

        greenhouse.addListener(click(() -> game.setScreen(new GreenhouseScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager,
                greenhouseService
        ))));

        collection.addListener(click(() -> {
            // بعداً CollectionScreen
        }));

        settings.addListener(click(() -> settingsScreen.show()));

        stage.addActor(back);
        stage.addActor(greenhouse);
        stage.addActor(collection);
        stage.addActor(settings);
    }

    private void buildSettingsOverlay() {
        settingsScreen = new SettingsScreen(
                textures,
                skin,
                appState,
                userManager
        );

        settingsScreen.setSize(WIDTH, HEIGHT);
        settingsScreen.setPosition(0f, 0f);
        settingsScreen.setVisible(false);

        stage.addActor(settingsScreen);
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
                appState.getCurrentUser().addDiamonds(1000);
                updateCurrencyLabels();
                userManager.save();
            }
        }));

        coinGroup.addListener(click(() -> {
            if (isDebugModeEnabled()) {
                appState.getCurrentUser().addCoins(1000);
                updateCurrencyLabels();
                userManager.save();
            }
        }));

        Table currencies = new Table();
        currencies.add(premiumGroup).width(premiumRegion.getRegionWidth()).
                height(premiumRegion.getRegionHeight()).padRight(10f);
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

    private boolean isDebugModeEnabled() {
        return appState.getCurrentUser() != null && appState.getCurrentUser().isDebugMode();
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

    private void buildWorldArea() {
        worldContainer.setSize(WIDTH, 430f);
        worldContainer.setPosition(0f, 150f);
        stage.addActor(worldContainer);

        for (int i = 0; i < worldTextures.length; i++) {
            createWorld(i);
        }
    }

    private void createWorld(int index) {
        TextureRegion region = textures.region(worldTextures[index]);

        if (region == null) {
            throw new IllegalStateException(
                    "Texture not found: " + worldTextures[index]
            );
        }

        Image world = new Image(region);
        world.setSize(WORLD_WIDTH, WORLD_HEIGHT);
        world.setOrigin(Align.center);

        final int worldIndex = index;

        world.addListener(click(() -> {
            currentPage = worldIndex;
            updateWorlds();

            appState.setSelectedChapter(
                    worldNames[worldIndex].toLowerCase()
            );
        }));

        worlds[index] = world;
        worldContainer.addActor(world);

        if (index < 4) {
            Label name = new Label(worldNames[index], skin);
            name.setAlignment(Align.center);
            name.setSize(WORLD_WIDTH, 40f);
            name.setOrigin(Align.center);

            worldLabels[index] = name;
            worldContainer.addActor(name);
        }
    }

    private void updateWorlds() {
        float centerX = WIDTH / 2f + CENTER_X_OFFSET;

        for (int i = 0; i < worlds.length; i++) {
            boolean selected = i == currentPage;

            worlds[i].setScale(
                    selected ? SELECTED_SCALE : NORMAL_SCALE
            );

            worlds[i].setColor(
                    unlockedWorlds[i]
                            ? Color.WHITE
                            : LOCKED_COLOR
            );

            float x = i == currentPage
                    ? centerX - WORLD_WIDTH / 2f
                    : i == currentPage - 1
                      ? centerX - SIDE_DISTANCE - WORLD_WIDTH / 2f
                      : i == currentPage + 1
                        ? centerX + SIDE_DISTANCE - WORLD_WIDTH / 2f
                        : WIDTH + 1000f;

            worlds[i].setPosition(x, WORLD_Y);

            if (worldLabels[i] != null) {
                worldLabels[i].setColor(
                        unlockedWorlds[i]
                                ? Color.WHITE
                                : Color.DARK_GRAY
                );

                worldLabels[i].setFontScale(
                        selected ? 1.2f : 0.95f
                );

                worldLabels[i].setPosition(
                        x,
                        WORLD_Y - 60f
                );
            }
        }
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
