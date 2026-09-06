package pvz.graphics.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import pvz.graphics.ui.HoverEffect;
import pvz.graphics.ui.Typography;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.ChapterSpec;
import pvz.model.adventure.LevelEntryRoute;
import pvz.model.adventure.LevelEntryRouter;
import pvz.model.adventure.LevelProgressService;
import pvz.model.adventure.LevelSpec;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;

public final class LevelSelectionScreen extends BaseScreen {
    private final ChapterSpec chapter;
    private final List<LevelSpec> levels;
    private final LevelProgressService levelProgressService;
    private final LevelEntryRouter levelEntryRouter = new LevelEntryRouter();
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
        HoverEffect.addScale(back);
        stage.addActor(back);
    }

    private void buildTitle() {
        Label title = new Label(
                chapter.name().toUpperCase(Locale.ROOT),
                skin
        );
        Typography.applyBody(title, skin);
        title.setColor(Color.WHITE);
        title.setFontScale(1.6f);
        title.setAlignment(Align.center);
        title.setBounds(190f, HEIGHT - 105f, 690f, 60f);
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
        Typography.applyBody(button, skin);
        button.getLabel().setAlignment(Align.center);
        button.getLabel().setWrap(true);
        button.setDisabled(
                state == LevelProgressService.LevelState.LOCKED
        );
        HoverEffect.addScale(button, () -> !button.isDisabled());
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

        LevelEntryRoute route = levelEntryRouter.route(level);
        if (route != LevelEntryRoute.PLANT_SELECTION) {
            statusLabel.setColor(Color.YELLOW);
            statusLabel.setText(
                    "This level needs a setup flow that is not ready yet."
            );
            return;
        }

        appState.setSelectedChapter(chapter.id());
        appState.setSelectedLevelId(level.id());
        game.setScreen(new PlantSelectionScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager,
                level,
                levelEntryRouter.plantSelectionRules(level)
        ));
    }

    private void buildStatusLabel() {
        statusLabel = new Label("", skin);
        Typography.applyBody(statusLabel, skin);
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(250f, 65f, WIDTH - 500f, 40f);
        stage.addActor(statusLabel);
    }

    private void buildCurrencies() {
        premiumLabel = new Label(getPremiumCount(), skin);
        coinLabel = new Label(getCoinCount(), skin);

        addCurrencyBadge(
                "IMAGE_UI_QUESTS_GEM_ICON",
                premiumLabel,
                WIDTH - 352f,
                this::updateCurrencyLabels
        );
        addCurrencyBadge(
                "IMAGE_UI_QUESTS_COIN_ICON",
                coinLabel,
                WIDTH - 184f,
                this::updateCurrencyLabels
        );
    }

    private void updateCurrencyLabels() {
        premiumLabel.setText(getPremiumCount());
        coinLabel.setText(getCoinCount());
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
