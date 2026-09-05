package pvz.graphics.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import java.util.Comparator;
import java.util.Locale;
import pvz.controller.PlantSelectionController;
import pvz.graphics.BaseScreen;
import pvz.graphics.GraphicalMenuView;
import pvz.graphics.PvzGame;
import pvz.graphics.actor.PlantCardActor;
import pvz.graphics.asset.PamAnimationService;
import pvz.graphics.asset.PlantVisualResolver;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.ChapterSpec;
import pvz.model.adventure.LevelSpec;
import pvz.model.command.PlantSelectionCommand;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;

public final class PlantSelectionScreen extends BaseScreen {
    private static final int CARD_COLUMNS = 4;
    private static final float TEXT_Y_OFFSET = 17f;
    private static final float COIN_WIDTH = 150f;
    private final LevelSpec level;
    private final PlantSelectionController controller;
    private final PamAnimationService animationService;
    private final PlantVisualResolver plantVisuals;
    private final Table plantGrid = new Table();
    private final Table selectedSlots = new Table();
    private ScrollPane plantScroll;
    private Label statusLabel;
    private Label premiumLabel;
    private Label coinLabel;
    private Label selectionCountLabel;

    public PlantSelectionScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager,
            LevelSpec level
    ) {
        super(game, textures, batch, skin, appState, userManager,
                "IMAGE_MAINMENU_BACKGROUND");
        this.level = level;
        this.animationService = game.getAnimationService();
        this.plantVisuals = new PlantVisualResolver(
                textures,
                Gdx.files.internal("assets")
        );
        this.controller = new PlantSelectionController(
                appState, userManager,
                new GraphicalMenuView(this::showStatus),
                game.getGameData().plantData(),
                game.getGameRuntime(),
                game.getGameSessionConfigFactory()
        );
        buildUi();
        refresh();
        resetPlantScroll();
    }

    private void buildUi() {
        Label title = new Label(
                "CHOOSE YOUR PLANTS - " + level.name().toUpperCase(Locale.ROOT),
                skin
        );
        title.setFontScale(1.35f);
        title.setAlignment(Align.center);
        title.setBounds(180f, HEIGHT - 70f, 920f, 50f);
        stage.addActor(title);

        buildTopBar();
        buildCurrencies();

        selectionCountLabel = new Label("", skin);
        selectionCountLabel.setAlignment(Align.center);
        selectionCountLabel.setBounds(20f, 585f, 250f, 40f);
        stage.addActor(selectionCountLabel);

        selectedSlots.defaults().pad(3f);
        ScrollPane selectedScroll = new ScrollPane(selectedSlots, skin);
        selectedScroll.setFadeScrollBars(false);
        selectedScroll.setBounds(20f, 125f, 250f, 455f);
        stage.addActor(selectedScroll);

        plantGrid.top().left();
        plantGrid.defaults().pad(6f);
        plantScroll = new ScrollPane(plantGrid, skin);
        plantScroll.setFadeScrollBars(false);
        plantScroll.setScrollingDisabled(true, false);
        plantScroll.setBounds(285f, 125f, 970f, 455f);
        stage.addActor(plantScroll);

        statusLabel = new Label("Select at least one plant.", skin);
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(250f, 70f, 780f, 40f);
        stage.addActor(statusLabel);

        TextButton start = new TextButton("LET'S ROCK", skin, "green");
        start.setBounds(1035f, 62f, 220f, 55f);
        start.addListener(click(this::startGame));
        stage.addActor(start);
    }

    private void buildTopBar() {
        TextureRegion normal = textures.region(
                "IMAGE_UI_MAINMENU_BACK_BTN_NORMAL"
        );
        TextureRegion pressed = textures.region(
                "IMAGE_UI_MAINMENU_BACK_BTN_PRESSED"
        );

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = normal != null ? new TextureRegionDrawable(normal) : null;
        style.down = pressed != null
                ? new TextureRegionDrawable(pressed)
                : style.up;

        ImageButton back = new ImageButton(style);
        back.setBounds(25f, HEIGHT - 80f, 55f, 55f);
        back.addListener(click(this::goBack));
        stage.addActor(back);
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
        premiumGroup.setTouchable(Touchable.enabled);

        coinLabel = new Label(getCoinCount(), skin);
        coinLabel.setColor(Color.WHITE);
        Group coinGroup = currencyGroup(
                coinRegion,
                coinLabel,
                COIN_WIDTH,
                65f
        );
        coinGroup.setTouchable(Touchable.enabled);

        premiumGroup.addListener(click(() -> {
            if (isDebugModeEnabled()) {
                appState.getCurrentUser().addDiamonds(100);
                refreshCurrencies();
                userManager.save();
            }
        }));

        coinGroup.addListener(click(() -> {
            if (isDebugModeEnabled()) {
                appState.getCurrentUser().addCoins(100);
                refreshCurrencies();
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

    private boolean isDebugModeEnabled() {
        return appState.getCurrentUser() != null
                && appState.getCurrentUser().isDebugMode();
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

    private void refresh() {
        refreshCurrencies();
        rebuildSelectedSlots();
        rebuildPlantGrid();
    }

    private void resetPlantScroll() {
        plantGrid.invalidateHierarchy();
        plantScroll.validate();
        plantScroll.setScrollY(0f);
    }

    private void refreshCurrencies() {
        User user = appState.getCurrentUser();
        if (premiumLabel != null) {
            premiumLabel.setText(user == null
                    ? "0"
                    : String.valueOf(user.getDiamonds()));
            premiumLabel.pack();
        }
        if (coinLabel != null) {
            coinLabel.setText(user == null
                    ? "0"
                    : String.valueOf(user.getCoins()));
            coinLabel.pack();
        }
    }

    private void rebuildSelectedSlots() {
        selectedSlots.clear();
        int selected = controller.getSelectedPlants().size();
        selectionCountLabel.setText(
                "SELECTED " + selected + "/" + controller.getMaxSlots()
        );

        for (int index = 0; index < controller.getMaxSlots(); index++) {
            String text = index < selected
                    ? (index + 1) + ". " + controller.getSelectedPlants().get(index)
                    : (index + 1) + ". EMPTY SLOT";
            TextButton slot = new TextButton(text, skin, "brown");
            slot.setDisabled(index >= selected);
            if (index < selected) {
                String plantName = controller.getSelectedPlants().get(index);
                slot.addListener(click(() -> execute(
                        PlantSelectionCommand.Action.REMOVE_PLANT,
                        plantName
                )));
            }
            selectedSlots.add(slot).width(220f).height(46f).row();
        }
    }

    private void rebuildPlantGrid() {
        plantGrid.clear();
        User user = appState.getCurrentUser();
        int column = 0;

        for (PlantSpec spec : game.getGameData().plantData().byId().values()
                .stream()
                .sorted(Comparator.comparingInt(PlantSpec::getId))
                .toList()) {
            PlayerPlant playerPlant = user == null
                    ? null
                    : user.getOwnedPlant(spec.getName());
            int plantLevel = playerPlant == null ? 1 : playerPlant.getLevel();
            PlantSpec effectiveSpec = spec.withLevel(plantLevel);
            PlantCardActor.Model model = new PlantCardActor.Model(
                    spec.getName(), plantLevel, effectiveSpec.getCost(),
                    seedProgress(playerPlant), playerPlant != null,
                    controller.isPlantSelected(spec.getName()),
                    controller.isPlantBoosted(spec.getName(), user),
                    plantVisuals.preview(spec.getName()),
                    plantVisuals.animationPath(spec.getName()),
                    plantVisuals.animationClip(spec.getName())
            );

            PlantCardActor card = new PlantCardActor(
                    skin, animationService, model,
                    () -> toggleSelection(spec.getName()),
                    () -> execute(PlantSelectionCommand.Action.BOOST_PLANT,
                            spec.getName()),
                    () -> execute(PlantSelectionCommand.Action.UPGRADE_PLANT,
                            spec.getName())
            );
            plantGrid.add(card)
                    .size(
                            PlantCardActor.CARD_WIDTH,
                            PlantCardActor.CARD_HEIGHT
                    );
            column++;
            if (column == CARD_COLUMNS) {
                plantGrid.row();
                column = 0;
            }
        }
    }

    private String seedProgress(PlayerPlant playerPlant) {
        if (playerPlant == null) {
            return "-";
        }
        if (playerPlant.getLevel() >= PlantSpec.MAX_LEVEL) {
            return "MAX";
        }
        int required = game.getGameData().plantData()
                .levelCosts()
                .forTargetLevel(playerPlant.getLevel() + 1)
                .seedPackets();
        return playerPlant.getSeedPackets() + "/" + required;
    }

    private void toggleSelection(String plantName) {
        PlantSelectionCommand.Action action = controller.isPlantSelected(plantName)
                ? PlantSelectionCommand.Action.REMOVE_PLANT
                : PlantSelectionCommand.Action.ADD_PLANT;
        execute(action, plantName);
    }

    private void execute(PlantSelectionCommand.Action action, String plantName) {
        controller.handle(new PlantSelectionCommand(action, plantName));
        refresh();
    }

    private void startGame() {
        controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.START_GAME
        ));
        refresh();
        if (game.getGameRuntime().isActive()) {
            game.setScreen(new BattleScreen(
                    game, textures, batch, skin, appState, userManager
            ));
        }
    }

    private void goBack() {
        controller.resetSelection();
        ChapterSpec chapter = game.getGameData().adventureData()
                .catalog().findChapter(level.chapterId());
        game.setScreen(new LevelSelectionScreen(
                game, textures, batch, skin, appState, userManager, chapter
        ));
    }

    private void showStatus(String message, Boolean error) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.setColor(Boolean.TRUE.equals(error) ? Color.RED : Color.GREEN);
        statusLabel.setText(message == null ? "" : message);
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
        appState.setCurrentMenu(MenuName.PLANT_SELECTION);
        refreshCurrencies();
    }
}
