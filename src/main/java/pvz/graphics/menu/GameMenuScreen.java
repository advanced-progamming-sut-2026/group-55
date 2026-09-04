package pvz.graphics.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import pvz.graphics.BaseScreen;
import pvz.graphics.PvzGame;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.ChapterSpec;
import pvz.model.adventure.LevelCatalog;
import pvz.model.adventure.LevelSpec;
import pvz.model.service.GreenhouseService;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GameMenuScreen extends BaseScreen {

    private static final float WORLD_WIDTH = 200f, WORLD_HEIGHT = 380f;
    private static final float SELECTED_SCALE = 1.12f, NORMAL_SCALE = 0.82f;
    private static final float SIDE_DISTANCE = 245f, WORLD_Y = 25f, CENTER_X_OFFSET = 0f;
    private static final float COIN_WIDTH = 150f;
    private static final float TEXT_Y_OFFSET = 17f;
    private static final Color LOCKED_COLOR = new Color(0.45f, 0.45f, 0.45f, 1f);
    private static final Map<String, String> WORLD_TEXTURES = Map.of(
            "ancient-egypt", "IMAGE_UI_UNIVERSE_WORLDS_EGYPT",
            "frostbite-caves", "IMAGE_UI_UNIVERSE_WORLDS_ICEAGE",
            "big-wave-beach", "IMAGE_UI_UNIVERSE_WORLDS_BEACH",
            "dark-ages", "IMAGE_UI_UNIVERSE_WORLDS_DARK"
    );

    private final Group worldContainer = new Group();
    private final List<ChapterSpec> chapters;
    private final LevelCatalog levelCatalog;
    private final Image[] worlds;
    private final Label[] worldLabels;
    private final GreenhouseService greenhouseService;

    private SettingsScreen settingsScreen;
    private int currentPage;

    private Label premiumLabel;
    private Label coinLabel;
    private TextButton enterChapterButton;
    private Label statusLabel;

    public GameMenuScreen(
            PvzGame game,
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

        this.levelCatalog = game.getGameData()
                .adventureData()
                .catalog();
        this.chapters = levelCatalog.chapters();
        if (chapters.isEmpty()) {
            throw new IllegalStateException("No chapters are configured.");
        }
        this.worlds = new Image[chapters.size()];
        this.worldLabels = new Label[chapters.size()];
        this.greenhouseService = game.getGameData()
                .greenhouseService();
        this.currentPage = selectedChapterIndex();

        buildUI();
        buildSettingsOverlay();
        updateWorlds();
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

        collection.addListener(click(() -> game.setScreen(new CollectionScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager,
                MenuName.GAME
        ))));

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
                userManager,
                MenuName.GAME
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

        for (int i = 0; i < chapters.size(); i++) {
            createWorld(i);
        }

        enterChapterButton = new TextButton(
                "VIEW LEVELS",
                skin,
                "green"
        );
        enterChapterButton.setBounds(
                (WIDTH - 220f) / 2f,
                50f,
                220f,
                55f
        );
        enterChapterButton.addListener(click(this::enterCurrentChapter));
        stage.addActor(enterChapterButton);

        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.YELLOW);
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(240f, 15f, WIDTH - 480f, 30f);
        stage.addActor(statusLabel);
    }

    private void createWorld(int index) {
        ChapterSpec chapter = chapters.get(index);
        String textureName = textureFor(chapter);
        TextureRegion region = textures.region(textureName);

        if (region == null) {
            throw new IllegalStateException(
                    "Texture not found: " + textureName
            );
        }

        Image world = new Image(region);
        world.setSize(WORLD_WIDTH, WORLD_HEIGHT);
        world.setOrigin(Align.center);

        final int worldIndex = index;

        world.addListener(click(() -> {
            currentPage = worldIndex;
            statusLabel.setText("");
            updateWorlds();
        }));

        worlds[index] = world;
        worldContainer.addActor(world);

        Label name = new Label(worldLabelText(index), skin);
        name.setAlignment(Align.center);
        name.setSize(WORLD_WIDTH, 60f);
        name.setOrigin(Align.center);

        worldLabels[index] = name;
        worldContainer.addActor(name);
    }

    private void updateWorlds() {
        float centerX = WIDTH / 2f + CENTER_X_OFFSET;

        for (int i = 0; i < worlds.length; i++) {
            boolean selected = i == currentPage;

            worlds[i].setScale(
                    selected ? SELECTED_SCALE : NORMAL_SCALE
            );

            worlds[i].setColor(
                    isWorldUnlocked(i)
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
                worldLabels[i].setText(worldLabelText(i));
                worldLabels[i].setColor(
                        isWorldUnlocked(i)
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

        updateEnterChapterButton();
    }

    private boolean isWorldUnlocked(int index) {
        User user = appState.getCurrentUser();
        return user != null
                && user.isChapterUnlocked(chapters.get(index).id());
    }

    private int selectedChapterIndex() {
        String selectedChapter = appState.getSelectedChapter();
        if (selectedChapter == null) {
            return 0;
        }
        for (int index = 0; index < chapters.size(); index++) {
            if (chapters.get(index).id().equalsIgnoreCase(selectedChapter)) {
                return index;
            }
        }
        return 0;
    }

    private String textureFor(ChapterSpec chapter) {
        String textureName = WORLD_TEXTURES.get(chapter.id());
        if (textureName == null) {
            throw new IllegalStateException(
                    "No world texture configured for chapter: "
                            + chapter.id()
            );
        }
        return textureName;
    }

    private String worldLabelText(int index) {
        ChapterSpec chapter = chapters.get(index);
        List<LevelSpec> levels = levelCatalog.levelsInChapter(chapter.id());
        User user = appState.getCurrentUser();
        long completed = user == null
                ? 0
                : levels.stream()
                        .filter(level -> user.getAdventureProgress()
                                .isLevelCompleted(level.id()))
                        .count();
        return chapter.name().toUpperCase(Locale.ROOT)
                + "\n"
                + completed
                + "/"
                + levels.size();
    }

    private void updateEnterChapterButton() {
        boolean unlocked = isWorldUnlocked(currentPage);
        boolean hasLevels = !levelCatalog.levelsInChapter(
                chapters.get(currentPage).id()
        ).isEmpty();

        enterChapterButton.setDisabled(!unlocked || !hasLevels);
        if (!unlocked) {
            enterChapterButton.setText("LOCKED");
        } else if (!hasLevels) {
            enterChapterButton.setText("NO LEVELS");
        } else {
            enterChapterButton.setText("VIEW LEVELS");
        }
    }

    private void enterCurrentChapter() {
        ChapterSpec chapter = chapters.get(currentPage);
        if (!isWorldUnlocked(currentPage)) {
            statusLabel.setText("Complete the previous chapter first.");
            return;
        }
        if (levelCatalog.levelsInChapter(chapter.id()).isEmpty()) {
            statusLabel.setText("No levels are configured for this chapter.");
            return;
        }

        appState.setSelectedChapter(chapter.id());
        appState.setSelectedLevelId(null);
        game.setScreen(new LevelSelectionScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager,
                chapter
        ));
    }

    @Override
    public void show() {
        super.show();
        appState.setCurrentMenu(MenuName.GAME);
        updateWorlds();
    }

    @Override
    public void dispose() {
        if (settingsScreen != null) {
            settingsScreen.dispose();
        }
        super.dispose();
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
