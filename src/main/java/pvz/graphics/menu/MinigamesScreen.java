package pvz.graphics.menu;

import pvz.graphics.ui.Typography;
import pvz.graphics.ui.HoverEffect;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import pvz.graphics.BaseScreen;
import pvz.graphics.PvzGame;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.minigame.MinigameCatalog;
import pvz.model.minigame.MinigameProgressService;
import pvz.model.minigame.MinigameSpec;
import pvz.model.minigame.MinigameStageRoute;
import pvz.model.minigame.MinigameStageState;
import pvz.model.quest.QuestCategory;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;

/** Phase-2 minigames menu shell; gameplay routing is completed in Phase 7. */
public final class MinigamesScreen extends BaseScreen {
    private static final float PANEL_X = 70f;
    private static final float PANEL_Y = 80f;
    private static final float PANEL_WIDTH = 1140f;
    private static final float PANEL_HEIGHT = 525f;
    private static final float CARD_WIDTH = 1080f;
    private static final float CARD_HEIGHT = 145f;
    private static final float STAGE_WIDTH = 145f;
    private static final float STAGE_HEIGHT = 62f;

    private final MinigameCatalog catalog;
    private final MinigameProgressService progressService;
    private final MenuName returnMenu;
    private final Table minigameTable = new Table();

    private ScrollPane minigameScroll;
    private Label coinLabel;
    private Label diamondLabel;
    private Label summaryLabel;
    private Label statusLabel;

    public MinigamesScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager
    ) {
        this(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager,
                MenuName.MAIN
        );
    }

    public MinigamesScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager,
            MenuName returnMenu
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
        this.catalog = game.getGameData().minigameCatalog();
        this.progressService = game.getGameData().minigameProgressService();
        this.returnMenu = normalizeReturnMenu(returnMenu);
        buildUi();
    }

    private void buildUi() {
        buildHeader();
        buildPanel();
        buildStatusBar();
    }

    private void buildHeader() {
        TextureRegion normal = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_NORMAL");
        TextureRegion pressed = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_PRESSED");
        if (normal != null) {
            ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
            style.up = new TextureRegionDrawable(normal);
            style.down = pressed == null ? style.up : new TextureRegionDrawable(pressed);
            ImageButton back = new ImageButton(style);
            back.setBounds(28f, HEIGHT - 80f, 56f, 56f);
            back.addListener(click(this::goBack));
            HoverEffect.addScale(back);
            stage.addActor(back);
        } else {
            TextButton back = new TextButton("BACK", skin, "brown");
            back.setBounds(20f, HEIGHT - 72f, 105f, 48f);
            back.addListener(click(this::goBack));
            stage.addActor(back);
        }
        Label title = new Label("MINIGAMES", skin);
        Typography.setReadableScale(title, 1.45f);
        title.setAlignment(Align.center);
        title.setBounds(150f, HEIGHT - 70f, 600f, 48f);
        stage.addActor(title);

        diamondLabel = new Label("", skin);
        coinLabel = new Label("", skin);
        addCurrencyBadge("IMAGE_UI_QUESTS_GEM_ICON", diamondLabel, 916f, this::refreshCurrencies);
        addCurrencyBadge("IMAGE_UI_QUESTS_COIN_ICON", coinLabel, 1084f, this::refreshCurrencies);
    }

    private void buildPanel() {
        Table frame = new Table();
        frame.setBackground(skin.newDrawable(
                "image_ui_dialog_asset_inner_bkgd_10", Color.valueOf("142e3c")
        ));
        frame.setBounds(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);
        frame.pad(14f);

        summaryLabel = new Label("", skin);
        summaryLabel.setColor(Color.WHITE);
        summaryLabel.setAlignment(Align.left);
        frame.add(summaryLabel)
                .growX()
                .height(32f)
                .left()
                .padBottom(8f)
                .row();

        minigameTable.top().left();
        minigameTable.defaults().padBottom(10f);

        minigameScroll = new ScrollPane(minigameTable, skin);
        minigameScroll.setFadeScrollBars(false);
        minigameScroll.setScrollingDisabled(true, false);
        minigameScroll.setOverscroll(false, false);

        frame.add(minigameScroll).grow();
        stage.addActor(frame);
    }

    private void buildStatusBar() {
        statusLabel = new Label(
                "Minigames are coming soon. Complete stages to unlock the next challenge.",
                skin
        );
        Typography.setReadableScale(statusLabel, 0.72f);
        statusLabel.setColor(Color.LIGHT_GRAY);
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(220f, 28f, 840f, 34f);
        stage.addActor(statusLabel);
    }

    private void refresh() {
        refreshCurrencies();
        rebuildMinigames();
    }

    private void refreshCurrencies() {
        User user = appState.getCurrentUser();
        if (user == null) {
            diamondLabel.setText("0");
            coinLabel.setText("0");
            return;
        }
        diamondLabel.setText(String.valueOf(user.getDiamonds()));
        coinLabel.setText(String.valueOf(user.getCoins()));
    }

    private void rebuildMinigames() {
        minigameTable.clearChildren();
        User user = appState.getCurrentUser();

        int totalStages = catalog.all().stream()
                .mapToInt(MinigameSpec::stageCount)
                .sum();
        int completedStages = user == null
                ? 0
                : progressService.completedStageCount(user);
        summaryLabel.setText(
                completedStages + " / " + totalStages
                        + " minigame stages cleared"
        );

        if (catalog.all().isEmpty()) {
            Label empty = new Label("No minigames are configured.", skin);
            empty.setAlignment(Align.center);
            minigameTable.add(empty)
                    .width(CARD_WIDTH)
                    .height(120f);
            return;
        }

        for (MinigameSpec spec : catalog.all()) {
            minigameTable.add(buildMinigameCard(spec, user))
                    .width(CARD_WIDTH)
                    .minHeight(CARD_HEIGHT)
                    .row();
        }

        minigameTable.invalidateHierarchy();
    }

    private Table buildMinigameCard(MinigameSpec spec, User user) {
        Table card = new Table();
        card.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        card.pad(12f);

        Table info = new Table();
        info.top().left();

        Label name = new Label(spec.name(), skin);
        Typography.setReadableScale(name, 1.02f);
        name.setColor(Color.valueOf("203945"));
        info.add(name).left().row();

        Label description = new Label(spec.description(), skin);
        Typography.applyBody(description, skin);
        description.setWrap(true);
        Typography.setReadableScale(description, 0.70f);
        description.setColor(Color.DARK_GRAY);
        info.add(description)
                .width(420f)
                .left()
                .padTop(6f)
                .row();

        int completed = user == null
                ? 0
                : progressService.completedStageCount(user, spec.id());
        Label progress = new Label(
                completed + " / " + spec.stageCount() + " stages cleared",
                skin
        );
        Typography.setReadableScale(progress, 0.68f);
        progress.setColor(Color.valueOf("395c49"));
        info.add(progress).left().padTop(7f);

        Table stages = new Table();
        stages.defaults().padLeft(8f);
        for (int stageNumber = 1;
             stageNumber <= spec.stageCount();
             stageNumber++) {
            stages.add(buildStageButton(spec, stageNumber, user))
                    .width(STAGE_WIDTH)
                    .height(STAGE_HEIGHT);
        }

        String iconKey = switch (spec.id()) {
            case MinigameCatalog.VASE_BREAKER ->
                    "IMAGE_VASEBREAKER_VASE_BROWN_VASE_BROWN_115X150";
            case MinigameCatalog.WALL_NUT_BOWLING -> "IMAGE_UI_PACKETS_WALLNUT";
            default -> "IMAGE_UI_QUESTS_QUESTICONS_ZOMBIE";
        };
        TextureRegion iconRegion = textures.region(iconKey);
        if (iconRegion != null) {
            Image icon = new Image(iconRegion);
            icon.setScaling(Scaling.fit);
            card.add(icon).size(64f).padRight(12f);
        }
        card.add(info).width(430f).growY().left();
        card.add(stages).growX().right();
        return card;
    }

    private TextButton buildStageButton(
            MinigameSpec spec,
            int stageNumber,
            User user
    ) {
        MinigameStageRoute route = MinigameStageRoute.of(
                spec,
                stageNumber
        );
        MinigameStageState state = user == null
                ? MinigameStageState.LOCKED
                : progressService.stageState(user, route);

        String style = state == MinigameStageState.COMPLETED
                ? "green"
                : "brown";
        TextButton button = new TextButton(
                stageButtonText(stageNumber, state),
                skin,
                style
        );
        Typography.setReadableScale(button.getLabel(), 0.61f);
        button.getLabel().setWrap(true);
        button.getLabel().setAlignment(Align.center);

        if (state == MinigameStageState.LOCKED) {
            button.getLabel().setColor(Color.LIGHT_GRAY);
        } else if (state == MinigameStageState.AVAILABLE) {
            button.getLabel().setColor(Color.WHITE);
        }

        // Phase 7 will enable launch for AVAILABLE/COMPLETED routes.
        // The typed route is already validated here for the Phase 7 launcher.
        button.setDisabled(true);
        button.setTouchable(Touchable.disabled);
        return button;
    }

    private String stageButtonText(
            int stageNumber,
            MinigameStageState state
    ) {
        return switch (state) {
            case COMPLETED -> "STAGE " + stageNumber + "\nCOMPLETED";
            case AVAILABLE -> "STAGE " + stageNumber + "\nCOMING SOON";
            case LOCKED -> "STAGE " + stageNumber + "\nLOCKED";
        };
    }

    private void goBack() {
        if (returnMenu == MenuName.TRAVEL_LOG) {
            game.setScreen(new TravelLogScreen(
                    game,
                    textures,
                    batch,
                    skin,
                    appState,
                    userManager,
                    QuestCategory.MINIGAME
            ));
            return;
        }

        game.setScreen(new MainMenuScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager
        ));
    }

    private MenuName normalizeReturnMenu(MenuName requested) {
        return requested == MenuName.TRAVEL_LOG
                ? MenuName.TRAVEL_LOG
                : MenuName.MAIN;
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
        appState.setCurrentMenu(MenuName.MINIGAMES);
        refresh();
    }
}
