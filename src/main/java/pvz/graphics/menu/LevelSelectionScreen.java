package pvz.graphics.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import pvz.graphics.BaseScreen;
import pvz.graphics.PvzGame;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.ChapterSpec;
import pvz.model.adventure.LevelProgressService;
import pvz.model.adventure.LevelSpec;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;

public final class LevelSelectionScreen extends BaseScreen {
    private static final float COIN_WIDTH = 150f;
    private static final float TEXT_Y_OFFSET = 17f;

    private final ChapterSpec chapter;
    private final List<LevelSpec> levels;
    private final LevelProgressService levelProgressService;
    private final Table levelTable = new Table();

    private Label premiumLabel;
    private Label coinLabel;
    private Label statusLabel;

    public LevelSelectionScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager,
            ChapterSpec chapter
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
        this.chapter = Objects.requireNonNull(
                chapter,
                "chapter cannot be null"
        );
        this.levels = game.getGameData()
                .adventureData()
                .catalog()
                .levelsInChapter(chapter.id());
        this.levelProgressService = game.getGameData()
                .levelProgressService();

        buildUI();
    }

    private void buildUI() {
        buildTopBar();
        buildCurrencies();
        buildTitle();
        buildLevelList();
        buildStatusLabel();
    }

    private void buildTopBar() {
        Image back = image("IMAGE_UI_MAINMENU_BACK_BTN_NORMAL");
        back.setBounds(25f, HEIGHT - 80f, 55f, 55f);
        back.addListener(click(() -> {
            appState.setSelectedLevelId(null);
            game.setScreen(new GameMenuScreen(
                    game,
                    textures,
                    batch,
                    skin,
                    appState,
                    userManager
            ));
        }));
        stage.addActor(back);
    }

    private void buildTitle() {
        Label title = new Label(
                chapter.name().toUpperCase(Locale.ROOT),
                skin
        );
        title.setColor(Color.WHITE);
        title.setFontScale(1.6f);
        title.setAlignment(Align.center);
        title.setBounds(240f, HEIGHT - 105f, WIDTH - 480f, 60f);
        stage.addActor(title);
    }

    private void buildLevelList() {
        levelTable.defaults().pad(8f);

        ScrollPane scrollPane = new ScrollPane(levelTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setBounds(290f, 125f, 700f, 470f);
        stage.addActor(scrollPane);

        rebuildLevelList();
    }

    private void rebuildLevelList() {
        levelTable.clear();
        User user = appState.getCurrentUser();

        if (user == null) {
            levelTable.add(new Label("No user is logged in.", skin));
            return;
        }
        if (levels.isEmpty()) {
            levelTable.add(new Label(
                    "No levels are configured for this chapter.",
                    skin
            ));
            return;
        }

        for (LevelSpec level : levels) {
            LevelProgressService.LevelState state =
                    levelProgressService.state(user, level);
            levelTable.add(createLevelButton(level, state))
                    .width(650f)
                    .height(82f)
                    .row();
        }
    }

    private TextButton createLevelButton(
            LevelSpec level,
            LevelProgressService.LevelState state
    ) {
        String text = "LEVEL "
                + level.number()
                + " - "
                + level.name()
                + "\n"
                + state;
        String style = state == LevelProgressService.LevelState.AVAILABLE
                ? "green"
                : "brown";
        TextButton button = new TextButton(text, skin, style);
        button.getLabel().setAlignment(Align.center);
        button.getLabel().setWrap(true);
        button.setDisabled(
                state == LevelProgressService.LevelState.LOCKED
        );
        button.addListener(click(() -> selectLevel(level, state)));
        return button;
    }

    private void selectLevel(
            LevelSpec level,
            LevelProgressService.LevelState state
    ) {
        if (state == LevelProgressService.LevelState.LOCKED) {
            statusLabel.setColor(Color.RED);
            statusLabel.setText(
                    "Complete the previous level first."
            );
            return;
        }

        appState.setSelectedChapter(chapter.id());
        appState.setSelectedLevelId(level.id());
        statusLabel.setColor(Color.GREEN);
        statusLabel.setText("Selected: " + level.name());
    }

    private void buildStatusLabel() {
        statusLabel = new Label("", skin);
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(250f, 65f, WIDTH - 500f, 40f);
        stage.addActor(statusLabel);
    }

    private void buildCurrencies() {
        TextureRegion premiumRegion = textures.region(
                "IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL"
        );
        TextureRegion coinRegion = textures.region(
                "IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL"
        );
        if (premiumRegion == null || coinRegion == null) {
            throw new IllegalStateException("Currency textures not found.");
        }

        premiumLabel = new Label(getPremiumCount(), skin);
        premiumLabel.setColor(Color.WHITE);
        Group premiumGroup = currencyGroup(
                premiumRegion,
                premiumLabel,
                premiumRegion.getRegionWidth(),
                70f
        );

        coinLabel = new Label(getCoinCount(), skin);
        coinLabel.setColor(Color.WHITE);
        Group coinGroup = currencyGroup(
                coinRegion,
                coinLabel,
                COIN_WIDTH,
                65f
        );

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
        currencies.add(premiumGroup)
                .width(premiumRegion.getRegionWidth())
                .height(premiumRegion.getRegionHeight())
                .padRight(10f);
        currencies.add(coinGroup)
                .width(COIN_WIDTH)
                .height(coinRegion.getRegionHeight());
        currencies.pack();
        currencies.setPosition(
                WIDTH - currencies.getWidth() - 20f,
                HEIGHT - currencies.getHeight() - 20f
        );
        stage.addActor(currencies);
    }

    private Group currencyGroup(
            TextureRegion region,
            Label label,
            float width,
            float textX
    ) {
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
        premiumLabel.setText(getPremiumCount());
        premiumLabel.pack();
        coinLabel.setText(getCoinCount());
        coinLabel.pack();
    }

    private boolean isDebugModeEnabled() {
        User user = appState.getCurrentUser();
        return user != null && user.isDebugMode();
    }

    private String getPremiumCount() {
        User user = appState.getCurrentUser();
        return user == null ? "0" : String.valueOf(user.getDiamonds());
    }

    private String getCoinCount() {
        User user = appState.getCurrentUser();
        return user == null ? "0" : String.valueOf(user.getCoins());
    }

    @Override
    public void show() {
        super.show();
        appState.setSelectedChapter(chapter.id());
        appState.setCurrentMenu(MenuName.CHAPTER);
        updateCurrencyLabels();
        rebuildLevelList();
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
