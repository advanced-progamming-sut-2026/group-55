package pvz.graphics.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import pvz.controller.PlantSelectionController;
import pvz.graphics.BaseScreen;
import pvz.graphics.GraphicalMenuView;
import pvz.graphics.PvzGame;
import pvz.graphics.actor.PlantCardActor;
import pvz.graphics.actor.PlantLoadoutSlotActor;
import pvz.graphics.asset.PamAnimationService;
import pvz.graphics.asset.PlantVisualResolver;
import pvz.graphics.ui.HoverEffect;
import pvz.graphics.ui.Typography;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.ChapterSpec;
import pvz.model.adventure.LevelSpec;
import pvz.model.command.PlantSelectionCommand;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.level.PlantLevelCost;
import pvz.model.selection.PlantSelectionRules;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;

public final class PlantSelectionScreen extends BaseScreen {
    private enum OwnershipFilter {
        ALL,
        OWNED,
        LOCKED
    }

    private static final int CARD_COLUMNS = 4;
    private static final float GRID_X = 285f;
    private static final float GRID_Y = 125f;
    private static final float GRID_WIDTH = 970f;
    private static final float GRID_HEIGHT = 415f;

    private final LevelSpec level;
    private final PlantSelectionController controller;
    private final PamAnimationService animationService;
    private final PlantVisualResolver plantVisuals;
    private final Table plantGrid = new Table();
    private final Table selectedSlots = new Table();
    private final List<PlantCategory> familyOptions = new ArrayList<>();

    private ScrollPane plantScroll;
    private Label statusLabel;
    private Label premiumLabel;
    private Label coinLabel;
    private Label selectionCountLabel;
    private TextButton familyFilterButton;
    private TextButton ownershipFilterButton;
    private TextButton upgradeFilterButton;
    private int familyFilterIndex = -1;
    private OwnershipFilter ownershipFilter = OwnershipFilter.ALL;
    private boolean upgradableOnly;

    public PlantSelectionScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager,
            LevelSpec level
    ) {
        this(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager,
                level,
                PlantSelectionRules.normal()
        );
    }

    public PlantSelectionScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager,
            LevelSpec level,
            PlantSelectionRules rules
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
        this.level = level;
        this.animationService = game.getAnimationService();
        this.plantVisuals = new PlantVisualResolver(
                textures,
                Gdx.files.internal("assets")
        );
        this.controller = new PlantSelectionController(
                appState,
                userManager,
                new GraphicalMenuView(this::showStatus),
                game.getGameData().plantData(),
                game.getGameRuntime(),
                game.getGameSessionConfigFactory(),
                rules
        );
        initializeFamilyOptions();
        buildUi();
        refresh();
        resetPlantScroll();
    }

    private void initializeFamilyOptions() {
        familyOptions.addAll(
                game.getGameData().plantData().byId().values().stream()
                        .map(PlantSpec::getCategory)
                        .distinct()
                        .sorted(Comparator.comparing(Enum::name))
                        .toList()
        );
    }

    private void buildUi() {
        Label title = new Label(
                "CHOOSE YOUR PLANTS - " + level.name().toUpperCase(Locale.ROOT),
                skin
        );
        Typography.applyBody(title, skin);
        Typography.setReadableScale(title, 1.35f);
        title.setColor(Color.WHITE);
        title.setAlignment(Align.center);
        title.setBounds(180f, HEIGHT - 70f, 720f, 50f);
        stage.addActor(title);

        buildTopBar();
        buildCurrencies();
        buildFilters();

        selectionCountLabel = new Label("", skin);
        Typography.applyBody(selectionCountLabel, skin);
        Typography.setReadableScale(selectionCountLabel, 0.82f);
        selectionCountLabel.setAlignment(Align.center);
        selectionCountLabel.setBounds(20f, 565f, 250f, 62f);
        stage.addActor(selectionCountLabel);

        selectedSlots.defaults().pad(3f);
        ScrollPane selectedScroll = new ScrollPane(selectedSlots, skin);
        selectedScroll.setFadeScrollBars(false);
        selectedScroll.setBounds(20f, 125f, 250f, 435f);
        stage.addActor(selectedScroll);

        plantGrid.top().left();
        plantGrid.defaults().pad(6f);
        plantScroll = new ScrollPane(plantGrid, skin);
        plantScroll.setFadeScrollBars(false);
        plantScroll.setScrollingDisabled(true, false);
        plantScroll.setBounds(GRID_X, GRID_Y, GRID_WIDTH, GRID_HEIGHT);
        stage.addActor(plantScroll);

        statusLabel = new Label("Select at least one plant.", skin);
        Typography.applyBody(statusLabel, skin);
        Typography.setReadableScale(statusLabel, 0.92f);
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(250f, 70f, 780f, 40f);
        stage.addActor(statusLabel);

        TextButton start = new TextButton("LET'S ROCK", skin, "green");
        Typography.applyBody(start, skin);
        start.setBounds(1035f, 62f, 220f, 55f);
        HoverEffect.addScale(start);
        start.addListener(click(this::startGame));
        stage.addActor(start);
    }

    private void buildTopBar() {
        var normal = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_NORMAL");
        var pressed = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_PRESSED");

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = normal != null ? new TextureRegionDrawable(normal) : null;
        style.down = pressed != null ? new TextureRegionDrawable(pressed) : style.up;

        ImageButton back = new ImageButton(style);
        back.setBounds(25f, HEIGHT - 80f, 55f, 55f);
        HoverEffect.addScale(back);
        back.addListener(click(this::goBack));
        stage.addActor(back);
    }

    private void buildCurrencies() {
        premiumLabel = new Label(getPremiumCount(), skin);
        coinLabel = new Label(getCoinCount(), skin);
        addCurrencyBadge(
                "IMAGE_UI_QUESTS_GEM_ICON",
                premiumLabel,
                WIDTH - 352f,
                this::refreshCurrencies
        );
        addCurrencyBadge(
                "IMAGE_UI_QUESTS_COIN_ICON",
                coinLabel,
                WIDTH - 184f,
                this::refreshCurrencies
        );
    }

    private void buildFilters() {
        Table filters = new Table();
        filters.setBounds(GRID_X, 548f, GRID_WIDTH, 48f);
        filters.defaults().height(42f).padRight(6f);

        familyFilterButton = new TextButton("", skin, "brown");
        familyFilterButton.addListener(click(this::cycleFamilyFilter));
        filters.add(familyFilterButton).width(230f);

        ownershipFilterButton = new TextButton("", skin, "brown");
        ownershipFilterButton.addListener(click(this::cycleOwnershipFilter));
        filters.add(ownershipFilterButton).width(185f);

        upgradeFilterButton = new TextButton("", skin, "brown");
        upgradeFilterButton.addListener(click(this::toggleUpgradeFilter));
        filters.add(upgradeFilterButton).width(190f);

        TextButton reset = new TextButton("RESET", skin, "brown");
        reset.addListener(click(this::resetFilters));
        filters.add(reset).width(120f);

        for (var cell : filters.getCells()) {
            if (cell.getActor() instanceof TextButton button) {
                Typography.setReadableScale(button.getLabel(), 0.72f);
                HoverEffect.addScale(button);
            }
        }
        stage.addActor(filters);
        updateFilterLabels();
    }

    private void cycleFamilyFilter() {
        if (familyOptions.isEmpty()) {
            return;
        }
        familyFilterIndex++;
        if (familyFilterIndex >= familyOptions.size()) {
            familyFilterIndex = -1;
        }
        updateFilterLabels();
        rebuildPlantGrid();
        resetPlantScroll();
    }

    private void cycleOwnershipFilter() {
        ownershipFilter = switch (ownershipFilter) {
            case ALL -> OwnershipFilter.OWNED;
            case OWNED -> OwnershipFilter.LOCKED;
            case LOCKED -> OwnershipFilter.ALL;
        };
        updateFilterLabels();
        rebuildPlantGrid();
        resetPlantScroll();
    }

    private void toggleUpgradeFilter() {
        upgradableOnly = !upgradableOnly;
        updateFilterLabels();
        rebuildPlantGrid();
        resetPlantScroll();
    }

    private void resetFilters() {
        familyFilterIndex = -1;
        ownershipFilter = OwnershipFilter.ALL;
        upgradableOnly = false;
        updateFilterLabels();
        rebuildPlantGrid();
        resetPlantScroll();
    }

    private void updateFilterLabels() {
        if (familyFilterButton == null) {
            return;
        }
        familyFilterButton.setText(
                "FAMILY: " + (familyFilterIndex < 0
                        ? "ALL"
                        : pretty(familyOptions.get(familyFilterIndex)))
        );
        ownershipFilterButton.setText("OWNERSHIP: " + ownershipFilter.name());
        upgradeFilterButton.setText(
                upgradableOnly ? "UPGRADE: READY" : "UPGRADE: ANY"
        );
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
            premiumLabel.setText(user == null ? "0" : String.valueOf(user.getDiamonds()));
        }
        if (coinLabel != null) {
            coinLabel.setText(user == null ? "0" : String.valueOf(user.getCoins()));
        }
    }

    private void rebuildSelectedSlots() {
        selectedSlots.clear();
        User user = appState.getCurrentUser();
        List<String> selectedPlants = controller.getSelectedPlants();
        int selected = selectedPlants.size();
        int pendingCost = controller.getPendingManualBoostCost();
        selectionCountLabel.setText(
                "SELECTED " + selected + "/" + controller.getSelectableCapacity()
                        + (pendingCost > 0 ? "\nPENDING BOOST: " + pendingCost + " GEMS" : "")
        );

        for (int index = 0; index < controller.getMaxSlots(); index++) {
            if (index < selected) {
                addSelectedPlantSlot(index, selectedPlants.get(index), user);
                continue;
            }
            boolean locked = index >= controller.getSelectableCapacity();
            selectedSlots.add(
                    PlantLoadoutSlotActor.empty(skin, index + 1, locked)
            ).width(PlantLoadoutSlotActor.WIDTH)
                    .height(PlantLoadoutSlotActor.HEIGHT)
                    .row();
        }
    }

    private void addSelectedPlantSlot(int index, String plantName, User user) {
        PlantSpec spec = effectiveSpec(plantName, user);
        int sunCost = spec == null ? 0 : spec.getCost();
        PlantSelectionController.BoostSource boostSource = controller.getBoostSource(
                plantName,
                user
        );
        String status = switch (boostSource) {
            case MANUAL -> "BOOST: MANUAL";
            case GREENHOUSE -> "BOOST: GREENHOUSE";
            case NONE -> controller.isForcedPlant(plantName) ? "REQUIRED" : "CLICK TO REMOVE";
        };
        boolean removable = !controller.isForcedPlant(plantName);
        PlantLoadoutSlotActor slot = new PlantLoadoutSlotActor(
                skin,
                plantVisuals.preview(plantName),
                (index + 1) + ". " + plantName,
                sunCost,
                status,
                removable,
                removable
                        ? () -> execute(PlantSelectionCommand.Action.REMOVE_PLANT, plantName)
                        : null
        );
        selectedSlots.add(slot)
                .width(PlantLoadoutSlotActor.WIDTH)
                .height(PlantLoadoutSlotActor.HEIGHT)
                .row();
    }

    private void rebuildPlantGrid() {
        plantGrid.clear();
        User user = appState.getCurrentUser();
        int column = 0;

        List<PlantSpec> plants = game.getGameData().plantData().byId().values()
                .stream()
                .sorted(Comparator.comparingInt(PlantSpec::getId))
                .filter(spec -> matchesFilters(spec, user))
                .toList();

        if (plants.isEmpty()) {
            Label empty = new Label("NO PLANTS MATCH THESE FILTERS", skin);
            Typography.applyBody(empty, skin);
            empty.setAlignment(Align.center);
            plantGrid.add(empty).width(GRID_WIDTH - 30f).height(70f);
            return;
        }

        for (PlantSpec spec : plants) {
            PlayerPlant playerPlant = user == null ? null : user.getOwnedPlant(spec.getName());
            int plantLevel = playerPlant == null ? PlantSpec.MIN_LEVEL : playerPlant.getLevel();
            PlantSpec effectiveSpec = spec.withLevel(plantLevel);
            PlantCardActor.Model model = new PlantCardActor.Model(
                    spec.getName(),
                    plantLevel,
                    effectiveSpec.getCost(),
                    seedProgress(playerPlant),
                    pretty(spec.getCategory()),
                    playerPlant != null,
                    controller.isPlantAllowed(spec.getName()),
                    controller.isPlantSelected(spec.getName()),
                    !controller.isForcedPlant(spec.getName()),
                    controller.isPlantBoosted(spec.getName(), user),
                    plantVisuals.preview(spec.getName()),
                    plantVisuals.animationPath(spec.getName()),
                    plantVisuals.animationClip(spec.getName())
            );

            PlantCardActor card = new PlantCardActor(
                    skin,
                    animationService,
                    model,
                    () -> toggleSelection(spec.getName()),
                    () -> execute(PlantSelectionCommand.Action.BOOST_PLANT, spec.getName()),
                    () -> execute(PlantSelectionCommand.Action.UPGRADE_PLANT, spec.getName())
            );
            plantGrid.add(card).size(PlantCardActor.CARD_WIDTH, PlantCardActor.CARD_HEIGHT);
            column++;
            if (column == CARD_COLUMNS) {
                plantGrid.row();
                column = 0;
            }
        }
    }

    private boolean matchesFilters(PlantSpec spec, User user) {
        if (familyFilterIndex >= 0
                && spec.getCategory() != familyOptions.get(familyFilterIndex)) {
            return false;
        }
        PlayerPlant owned = user == null ? null : user.getOwnedPlant(spec.getName());
        if (ownershipFilter == OwnershipFilter.OWNED && owned == null) {
            return false;
        }
        if (ownershipFilter == OwnershipFilter.LOCKED && owned != null) {
            return false;
        }
        return !upgradableOnly || canUpgradeNow(user, owned);
    }

    private boolean canUpgradeNow(User user, PlayerPlant owned) {
        if (user == null || owned == null || owned.getLevel() >= PlantSpec.MAX_LEVEL) {
            return false;
        }
        PlantLevelCost next = game.getGameData().plantData()
                .levelCosts()
                .forTargetLevel(owned.getLevel() + 1);
        return user.getCoins() >= next.coins()
                && owned.getSeedPackets() >= next.seedPackets();
    }

    private PlantSpec effectiveSpec(String plantName, User user) {
        PlantSpec base = game.getGameData().plantData().byName().get(
                plantName.toLowerCase(Locale.ROOT)
        );
        if (base == null) {
            return null;
        }
        PlayerPlant owned = user == null ? null : user.getOwnedPlant(plantName);
        int levelValue = owned == null ? PlantSpec.MIN_LEVEL : owned.getLevel();
        return base.withLevel(levelValue);
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
                    game,
                    textures,
                    batch,
                    skin,
                    appState,
                    userManager
            ));
        }
    }

    private void goBack() {
        controller.resetSelection();
        ChapterSpec chapter = game.getGameData().adventureData()
                .catalog()
                .findChapter(level.chapterId());
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

    private void showStatus(String message, Boolean error) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.setColor(Boolean.TRUE.equals(error) ? Color.RED : Color.GREEN);
        statusLabel.setText(message == null ? "" : message);
    }

    private String getPremiumCount() {
        User user = appState.getCurrentUser();
        return user == null ? "0" : String.valueOf(user.getDiamonds());
    }

    private String getCoinCount() {
        User user = appState.getCurrentUser();
        return user == null ? "0" : String.valueOf(user.getCoins());
    }

    private String pretty(Enum<?> value) {
        String text = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.isEmpty()
                ? ""
                : Character.toUpperCase(text.charAt(0)) + text.substring(1);
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
        refresh();
    }
}
