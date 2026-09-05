package pvz.graphics.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
import com.badlogic.gdx.utils.Scaling;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import pvz.controller.CollectionController;
import pvz.data.PlantData;
import pvz.data.ZombieData;
import pvz.graphics.BaseScreen;
import pvz.graphics.GraphicalMenuView;
import pvz.graphics.PvzGame;
import pvz.graphics.actor.PlantActor;
import pvz.graphics.asset.PamAnimationRenderer;
import pvz.graphics.asset.PamAnimationService;
import pvz.graphics.asset.PlantVisualResolver;
import pvz.graphics.asset.ZombieVisualResolver;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.CollectionCommand;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.level.PlantLevelCost;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;
import pvz.model.entity.zombie.ZombieSpec;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;

/** Graphical Collection/Almanac screen for plants and discovered zombies. */
public final class CollectionScreen extends BaseScreen {
    private static final int PLANT_PURCHASE_PRICE = 2_000;
    private static final int GRID_COLUMNS = 4;
    private static final float TEXT_Y_OFFSET = 17f;

    private static final float GRID_X = 22f;
    private static final float GRID_Y = 72f;
    private static final float GRID_WIDTH = 805f;
    private static final float GRID_HEIGHT = 462f;
    private static final float CARD_WIDTH = 188f;
    private static final float PLANT_CARD_HEIGHT = 226f;
    private static final float ZOMBIE_CARD_HEIGHT = 180f;
    private static final float DETAIL_X = 850f;
    private static final float DETAIL_Y = 72f;
    private static final float DETAIL_WIDTH = 408f;
    private static final float DETAIL_HEIGHT = 565f;
    private static final float DETAIL_CONTENT_WIDTH = 355f;

    private static final Color LOCKED_COLOR =
            new Color(0.58f, 0.58f, 0.58f, 1f);
    private static final Color READY_COLOR =
            new Color(0.12f, 0.52f, 0.12f, 1f);

    private final MenuName returnMenu;
    private final PlantData plantData;
    private final ZombieData zombieData;
    private final CollectionController controller;
    private final PamAnimationService animationService;
    private final PlantVisualResolver plantVisuals;
    private final ZombieVisualResolver zombieVisuals;
    private final ZombieFactory zombieFactory;
    private final PamAnimationRenderer zombieAnimationRenderer;
    private final List<PlantCategory> familyOptions;
    private final Map<String, ZombieAnimationSpec> zombieAnimationCache =
            new HashMap<>();
    private final List<LazyPamPreview> activePreviews = new ArrayList<>();

    private final Table entityGrid = new Table();
    private final Table detailContent = new Table();

    private ScrollPane entityScroll;
    private Label premiumLabel;
    private Label coinLabel;
    private Label statusLabel;
    private TextButton plantsTabButton;
    private TextButton zombiesTabButton;
    private TextButton familyFilterButton;
    private TextButton ownershipFilterButton;
    private TextButton upgradeFilterButton;
    private Table filters;

    private Tab activeTab = Tab.PLANTS;
    private int familyFilterIndex = -1;
    private OwnershipFilter ownershipFilter = OwnershipFilter.ALL;
    private boolean upgradableOnly;
    private PlantSpec selectedPlant;
    private ZombieSpec selectedZombie;

    public CollectionScreen(
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
        this.returnMenu = returnMenu == null ? MenuName.GAME : returnMenu;
        this.plantData = game.getGameData().plantData();
        this.zombieData = game.getGameData().zombieData();
        this.animationService = game.getAnimationService();
        this.plantVisuals = new PlantVisualResolver(
                textures,
                Gdx.files.internal("assets")
        );
        this.zombieVisuals = new ZombieVisualResolver(
                Gdx.files.internal("assets")
        );
        this.zombieFactory = new ZombieFactory(zombieData);
        this.zombieAnimationRenderer = new PamAnimationRenderer(
                animationService
        );
        this.familyOptions = plantData.byId().values().stream()
                .map(PlantSpec::getCategory)
                .distinct()
                .sorted(Comparator.comparingInt(PlantCategory::ordinal))
                .toList();
        this.controller = new CollectionController(
                appState,
                userManager,
                new GraphicalMenuView(this::showStatus),
                plantData,
                zombieData
        );

        buildUi();
        refreshAll(true);
    }

    private void buildUi() {
        buildHeader();
        buildTabs();
        buildFilters();
        buildEntityGrid();
        buildDetailPanel();
        buildStatusBar();
    }

    private void buildHeader() {
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

        Label title = new Label("COLLECTION", skin);
        title.setFontScale(1.35f);
        title.setAlignment(Align.center);
        title.setBounds(255f, HEIGHT - 70f, 650f, 48f);
        stage.addActor(title);

        buildCurrencies();
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
                150f,
                65f
        );
        coinGroup.setTouchable(Touchable.enabled);

        premiumGroup.addListener(click(() -> {
            if (isDebugModeEnabled()) {
                appState.getCurrentUser().addDiamonds(100);
                refreshCurrency();
                userManager.save();
            }
        }));

        coinGroup.addListener(click(() -> {
            if (isDebugModeEnabled()) {
                appState.getCurrentUser().addCoins(100);
                refreshCurrency();
                userManager.save();
            }
        }));

        Table currencies = new Table();
        currencies.add(premiumGroup)
                .width(premiumRegion.getRegionWidth())
                .height(premiumRegion.getRegionHeight())
                .padRight(10f);
        currencies.add(coinGroup)
                .width(150f)
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

    private void buildTabs() {
        plantsTabButton = new TextButton("PLANTS", skin, "green");
        plantsTabButton.setBounds(GRID_X, 588f, 185f, 46f);
        plantsTabButton.addListener(click(() -> switchTab(Tab.PLANTS)));
        stage.addActor(plantsTabButton);

        zombiesTabButton = new TextButton("ZOMBIES", skin, "brown");
        zombiesTabButton.setBounds(GRID_X + 195f, 588f, 185f, 46f);
        zombiesTabButton.addListener(click(() -> switchTab(Tab.ZOMBIES)));
        stage.addActor(zombiesTabButton);
    }

    private void buildFilters() {
        filters = new Table();
        filters.setBounds(GRID_X, 540f, GRID_WIDTH, 42f);
        filters.defaults().height(40f).padRight(6f);

        familyFilterButton = new TextButton("", skin, "brown");
        familyFilterButton.getLabel().setFontScale(0.75f);
        familyFilterButton.addListener(click(this::cycleFamilyFilter));
        filters.add(familyFilterButton).width(230f);

        ownershipFilterButton = new TextButton("", skin, "brown");
        ownershipFilterButton.getLabel().setFontScale(0.75f);
        ownershipFilterButton.addListener(click(this::cycleOwnershipFilter));
        filters.add(ownershipFilterButton).width(185f);

        upgradeFilterButton = new TextButton("", skin, "brown");
        upgradeFilterButton.getLabel().setFontScale(0.75f);
        upgradeFilterButton.addListener(click(this::toggleUpgradeFilter));
        filters.add(upgradeFilterButton).width(190f);

        TextButton reset = new TextButton("RESET", skin, "brown");
        reset.getLabel().setFontScale(0.75f);
        reset.addListener(click(this::resetFilters));
        filters.add(reset).width(120f);

        stage.addActor(filters);
    }

    private void buildEntityGrid() {
        entityGrid.top().left();
        entityGrid.defaults().pad(5f);

        entityScroll = new ScrollPane(entityGrid, skin);
        entityScroll.setFadeScrollBars(false);
        entityScroll.setScrollingDisabled(true, false);
        entityScroll.setBounds(GRID_X, GRID_Y, GRID_WIDTH, GRID_HEIGHT);
        stage.addActor(entityScroll);
    }

    private void buildDetailPanel() {
        detailContent.top().left();
        detailContent.defaults().pad(3f);

        ScrollPane detailScroll = new ScrollPane(detailContent, skin);
        detailScroll.setFadeScrollBars(false);
        detailScroll.setScrollingDisabled(true, false);

        Table frame = new Table();
        frame.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        frame.setBounds(DETAIL_X, DETAIL_Y, DETAIL_WIDTH, DETAIL_HEIGHT);
        frame.add(detailScroll).grow().pad(10f);
        stage.addActor(frame);
    }

    private void buildStatusBar() {
        statusLabel = new Label(
                "Select a plant or a discovered zombie to view details.",
                skin
        );
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(160f, 18f, 960f, 38f);
        stage.addActor(statusLabel);
    }

    private void switchTab(Tab tab) {
        if (activeTab == tab) {
            return;
        }
        activeTab = tab;
        updateTabButtons();
        filters.setVisible(activeTab == Tab.PLANTS);
        showStatus(
                activeTab == Tab.PLANTS
                        ? "Plant collection."
                        : "Only discovered zombies reveal their details.",
                false
        );
        refreshAll(true);
    }

    private void updateTabButtons() {
        plantsTabButton.setStyle(skin.get(
                activeTab == Tab.PLANTS ? "green" : "brown",
                TextButton.TextButtonStyle.class
        ));
        zombiesTabButton.setStyle(skin.get(
                activeTab == Tab.ZOMBIES ? "green" : "brown",
                TextButton.TextButtonStyle.class
        ));
    }

    private void refreshAll(boolean resetScroll) {
        cancelPreviews();
        refreshCurrency();
        updateFilterLabels();
        filters.setVisible(activeTab == Tab.PLANTS);
        rebuildGrid(resetScroll);
        rebuildDetailPanel();
    }

    private void refreshCurrency() {
        User user = appState.getCurrentUser();
        if (premiumLabel != null) {
            premiumLabel.setText(
                    user == null ? "0" : String.valueOf(user.getDiamonds())
            );
            premiumLabel.pack();
        }
        if (coinLabel != null) {
            coinLabel.setText(
                    user == null ? "0" : String.valueOf(user.getCoins())
            );
            coinLabel.pack();
        }
    }

    private void updateFilterLabels() {
        familyFilterButton.setText(
                "FAMILY: " + (familyFilterIndex < 0
                        ? "ALL"
                        : pretty(familyOptions.get(familyFilterIndex)))
        );
        ownershipFilterButton.setText(
                "OWNERSHIP: " + ownershipFilter.name()
        );
        upgradeFilterButton.setText(
                upgradableOnly
                        ? "UPGRADE: READY ONLY"
                        : "UPGRADE: ANY"
        );
    }

    private void cycleFamilyFilter() {
        if (familyOptions.isEmpty()) {
            return;
        }
        familyFilterIndex++;
        if (familyFilterIndex >= familyOptions.size()) {
            familyFilterIndex = -1;
        }
        refreshAll(true);
    }

    private void cycleOwnershipFilter() {
        ownershipFilter = switch (ownershipFilter) {
            case ALL -> OwnershipFilter.OWNED;
            case OWNED -> OwnershipFilter.LOCKED;
            case LOCKED -> OwnershipFilter.ALL;
        };
        refreshAll(true);
    }

    private void toggleUpgradeFilter() {
        upgradableOnly = !upgradableOnly;
        refreshAll(true);
    }

    private void resetFilters() {
        familyFilterIndex = -1;
        ownershipFilter = OwnershipFilter.ALL;
        upgradableOnly = false;
        refreshAll(true);
    }

    private void rebuildGrid(boolean resetScroll) {
        float previousScroll = entityScroll.getScrollY();
        entityGrid.clearChildren();

        if (activeTab == Tab.PLANTS) {
            rebuildPlantGrid();
        } else {
            rebuildZombieGrid();
        }

        entityGrid.invalidateHierarchy();
        entityScroll.validate();
        entityScroll.setScrollY(resetScroll ? 0f : previousScroll);
    }

    private void rebuildPlantGrid() {
        User user = appState.getCurrentUser();
        List<PlantSpec> plants = plantData.byId().values().stream()
                .sorted(Comparator.comparingInt(PlantSpec::getId))
                .filter(spec -> matchesPlantFilters(spec, user))
                .toList();

        if (plants.isEmpty()) {
            Label empty = new Label("NO PLANTS MATCH THESE FILTERS", skin);
            empty.setAlignment(Align.center);
            entityGrid.add(empty).width(GRID_WIDTH - 30f).height(70f);
            return;
        }

        int column = 0;
        for (PlantSpec spec : plants) {
            entityGrid.add(createPlantCard(spec, user))
                    .size(CARD_WIDTH, PLANT_CARD_HEIGHT);
            column++;
            if (column == GRID_COLUMNS) {
                entityGrid.row();
                column = 0;
            }
        }
    }

    private boolean matchesPlantFilters(PlantSpec spec, User user) {
        if (familyFilterIndex >= 0
                && spec.getCategory() != familyOptions.get(familyFilterIndex)) {
            return false;
        }

        PlayerPlant owned = user == null
                ? null
                : user.getOwnedPlant(spec.getName());
        if (ownershipFilter == OwnershipFilter.OWNED && owned == null) {
            return false;
        }
        if (ownershipFilter == OwnershipFilter.LOCKED && owned != null) {
            return false;
        }
        return !upgradableOnly || canUpgradeNow(user, owned);
    }

    private Table createPlantCard(PlantSpec spec, User user) {
        PlayerPlant owned = user == null
                ? null
                : user.getOwnedPlant(spec.getName());
        boolean upgradeReady = canUpgradeNow(user, owned);

        Table card = cardBase();
        card.setTouchable(Touchable.enabled);
        if (owned == null) {
            card.setColor(LOCKED_COLOR);
        } else if (upgradeReady) {
            card.setColor(new Color(0.90f, 1f, 0.82f, 1f));
        }

        card.add(createPlantImage(spec))
                .size(74f, 74f)
                .padTop(5f)
                .row();

        Label name = cardLabel(spec.getName(), 0.82f);
        card.add(name).width(CARD_WIDTH - 16f).height(30f).row();

        Label family = cardLabel(pretty(spec.getCategory()), 0.62f);
        family.setColor(Color.GRAY);
        card.add(family).width(CARD_WIDTH - 16f).height(20f).row();

        String levelText = owned == null
                ? "LOCKED"
                : "LEVEL " + owned.getLevel();
        Label level = cardLabel(levelText, 0.72f);
        level.setColor(owned == null ? Color.GRAY : Color.DARK_GRAY);
        card.add(level).width(CARD_WIDTH - 16f).height(22f).row();

        Label seeds = cardLabel(seedProgress(owned), 0.68f);
        seeds.setColor(owned == null ? Color.GRAY : Color.DARK_GRAY);
        card.add(seeds).width(CARD_WIDTH - 16f).height(22f).row();

        if (owned == null) {
            TextButton buy = new TextButton(
                    "BUY " + PLANT_PURCHASE_PRICE,
                    skin,
                    "green"
            );
            buy.getLabel().setFontScale(0.72f);
            buy.addListener(click(() -> purchasePlant(spec)));
            card.add(buy).width(145f).height(36f).padBottom(5f);
        } else {
            Label state = cardLabel(
                    upgradeReady ? "UPGRADE READY" : "OWNED",
                    0.68f
            );
            state.setColor(upgradeReady ? READY_COLOR : Color.DARK_GRAY);
            card.add(state).width(150f).height(36f).padBottom(5f);
        }

        card.addListener(click(() -> selectPlant(spec)));
        return card;
    }

    private Actor createPlantImage(PlantSpec spec) {
        AnimationSpec animation = plantAnimation(spec);
        if (animation == null) {
            return emptyPreview(74f, 74f);
        }
        return createPamPreview(
                animation,
                null,
                74f,
                74f
        );
    }

    private String seedProgress(PlayerPlant owned) {
        if (owned == null) {
            return "SEEDS -/-";
        }
        if (owned.getLevel() >= PlantSpec.MAX_LEVEL) {
            return "SEEDS MAX";
        }
        PlantLevelCost next = plantData.levelCosts()
                .forTargetLevel(owned.getLevel() + 1);
        return "SEEDS " + owned.getSeedPackets()
                + "/" + next.seedPackets();
    }

    private boolean canUpgradeNow(User user, PlayerPlant owned) {
        if (user == null || owned == null
                || owned.getLevel() >= PlantSpec.MAX_LEVEL) {
            return false;
        }
        PlantLevelCost next = plantData.levelCosts()
                .forTargetLevel(owned.getLevel() + 1);
        return user.getCoins() >= next.coins()
                && owned.getSeedPackets() >= next.seedPackets();
    }

    private void rebuildZombieGrid() {
        User user = appState.getCurrentUser();
        List<ZombieSpec> zombies = zombieData.byId().values().stream()
                .sorted(Comparator.comparing(
                        ZombieSpec::getName,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();

        int column = 0;
        for (ZombieSpec spec : zombies) {
            entityGrid.add(createZombieCard(spec, user))
                    .size(CARD_WIDTH, ZOMBIE_CARD_HEIGHT);
            column++;
            if (column == GRID_COLUMNS) {
                entityGrid.row();
                column = 0;
            }
        }
    }

    private Table createZombieCard(ZombieSpec spec, User user) {
        boolean seen = isZombieSeen(user, spec);
        Table card = cardBase();
        card.setTouchable(Touchable.enabled);

        if (!seen) {
            card.setColor(LOCKED_COLOR);
            Actor unknown = placeholder("?");
            card.add(unknown).size(92f, 92f).padTop(8f).row();
            Label undiscovered = cardLabel("UNDISCOVERED", 0.72f);
            undiscovered.setColor(Color.GRAY);
            card.add(undiscovered)
                    .width(CARD_WIDTH - 16f)
                    .height(42f)
                    .padBottom(8f);
            card.addListener(click(() -> showStatus(
                    "This zombie has not been discovered yet.",
                    true
            )));
            return card;
        }

        ZombieAnimationSpec animation = zombieAnimation(spec, user);
        Actor preview = animation == null
                ? emptyPreview(100f, 100f)
                : createZombiePreview(animation, 100f, 100f);
        card.add(preview).size(100f, 100f).padTop(4f).row();

        Label name = cardLabel(spec.getName(), 0.78f);
        card.add(name)
                .width(CARD_WIDTH - 16f)
                .height(42f)
                .padBottom(4f);
        card.addListener(click(() -> selectZombie(spec)));
        return card;
    }

    private boolean isZombieSeen(User user, ZombieSpec spec) {
        if (user == null) {
            return false;
        }
        return user.getSeenZombies().stream().anyMatch(value -> {
            if (value == null) {
                return false;
            }
            String normalized = value.strip();
            return normalized.equalsIgnoreCase(spec.getId())
                    || normalized.equalsIgnoreCase(spec.getName());
        });
    }

    private ZombieAnimationSpec zombieAnimation(
            ZombieSpec spec,
            User user
    ) {
        String key = spec.getId().toLowerCase(Locale.ROOT);
        if (zombieAnimationCache.containsKey(key)) {
            return zombieAnimationCache.get(key);
        }

        ZombieAnimationSpec animation = null;
        try {
            int difficulty = user == null ? 3 : user.getDifficultyLevel();
            Zombie zombie = zombieFactory.create(spec.getId(), difficulty);
            if (zombie != null) {
                ZombieVisualResolver.Visual visual = zombieVisuals.visual(zombie);
                if (visual != null) {
                    String clip = zombieVisuals.clip(
                            zombie,
                            ZombieVisualResolver.Motion.IDLE
                    );
                    String referenceClip =
                            zombieVisuals.referenceClip(zombie);
                    if (clip != null && !clip.isBlank()
                            && referenceClip != null
                            && !referenceClip.isBlank()) {
                        animation = new ZombieAnimationSpec(
                                visual.path(),
                                clip,
                                referenceClip,
                                Map.copyOf(
                                        zombieVisuals.partsVisibility(
                                                zombie,
                                                0L
                                        )
                                )
                        );
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Missing or unimplemented visuals stay intentionally blank.
        }

        zombieAnimationCache.put(key, animation);
        return animation;
    }

    private void selectPlant(PlantSpec spec) {
        selectedPlant = spec;
        showStatus("Showing " + spec.getName() + ".", false);
        refreshDetailOnly();
    }

    private void selectZombie(ZombieSpec spec) {
        User user = appState.getCurrentUser();
        if (!isZombieSeen(user, spec)) {
            showStatus("This zombie has not been discovered yet.", true);
            return;
        }
        selectedZombie = spec;
        showStatus("Showing " + spec.getName() + ".", false);
        refreshDetailOnly();
    }

    private void refreshDetailOnly() {
        cancelPreviews();
        rebuildGrid(false);
        rebuildDetailPanel();
    }

    private void rebuildDetailPanel() {
        detailContent.clearChildren();
        if (activeTab == Tab.PLANTS) {
            if (selectedPlant == null) {
                addDetailPrompt("SELECT A PLANT", "Click a plant card to view its details.");
            } else {
                buildPlantDetails(selectedPlant);
            }
        } else if (selectedZombie == null) {
            addDetailPrompt(
                    "ZOMBIE ALMANAC",
                    "Only zombies encountered during gameplay can be inspected."
            );
        } else if (isZombieSeen(appState.getCurrentUser(), selectedZombie)) {
            buildZombieDetails(selectedZombie);
        } else {
            selectedZombie = null;
            addDetailPrompt(
                    "ZOMBIE ALMANAC",
                    "Only zombies encountered during gameplay can be inspected."
            );
        }
    }

    private void addDetailPrompt(String title, String body) {
        Label header = detailTitle(title);
        detailContent.add(header)
                .width(DETAIL_CONTENT_WIDTH)
                .height(55f)
                .colspan(2)
                .row();
        Label message = detailText(body);
        message.setAlignment(Align.center);
        detailContent.add(message)
                .width(DETAIL_CONTENT_WIDTH)
                .height(100f)
                .colspan(2)
                .padTop(30f);
    }

    private void buildPlantDetails(PlantSpec spec) {
        User user = appState.getCurrentUser();
        PlayerPlant owned = user == null
                ? null
                : user.getOwnedPlant(spec.getName());
        int level = owned == null ? PlantSpec.MIN_LEVEL : owned.getLevel();
        PlantSpec effective = spec.withLevel(level);

        detailContent.add(detailTitle(spec.getName()))
                .width(DETAIL_CONTENT_WIDTH)
                .height(44f)
                .colspan(2)
                .row();

        AnimationSpec animation = plantAnimation(spec);
        TextureRegion fallback = plantVisuals.preview(spec.getName());
        Actor preview = animation == null
                ? fallbackActor(fallback, "?")
                : createPamPreview(animation, fallback, 180f, 165f);
        detailContent.add(preview)
                .size(180f, 165f)
                .colspan(2)
                .padBottom(5f)
                .row();

        addDetailRow("Status", owned == null ? "LOCKED" : "OWNED");
        addDetailRow("Level", owned == null ? "-" : String.valueOf(level));
        addDetailRow("Health", String.valueOf(effective.getBaseHp()));
        addDetailRow("Sun Cost", String.valueOf(effective.getCost()));
        addDetailRow("Damage", effective.getDamage());
        addDetailRow("Family", pretty(spec.getCategory()));
        addDetailRow("Tags", formatTags(spec));
        addDetailRow("Action Interval", formatSeconds(effective.getActionInterval()));
        addDetailRow("Recharge", formatSeconds(effective.getRecharge()));

        addDetailParagraph("ABILITY", spec.getBaseAbility());
        addDetailParagraph("PLANT FOOD", spec.getPlantFoodEffect());
        addDetailParagraph("LEVEL 2", spec.getLvl2());
        addDetailParagraph("LEVEL 3", spec.getLvl3());
        addDetailParagraph("LEVEL 4", spec.getLvl4());

        if (owned == null) {
            TextButton buy = new TextButton(
                    "BUY FOR " + PLANT_PURCHASE_PRICE + " COINS",
                    skin,
                    "green"
            );
            buy.addListener(click(() -> purchasePlant(spec)));
            detailContent.add(buy)
                    .width(285f)
                    .height(48f)
                    .colspan(2)
                    .padTop(8f)
                    .padBottom(8f)
                    .row();
            return;
        }

        if (owned.getLevel() >= PlantSpec.MAX_LEVEL) {
            addDetailRow("Upgrade", "MAX LEVEL");
            return;
        }

        PlantLevelCost next = plantData.levelCosts()
                .forTargetLevel(owned.getLevel() + 1);
        addDetailRow(
                "Seed Packets",
                owned.getSeedPackets() + "/" + next.seedPackets()
        );
        addDetailRow(
                "Next Upgrade",
                next.coins() + " coins + "
                        + next.seedPackets() + " seeds"
        );

        TextButton upgrade = new TextButton("UPGRADE", skin, "green");
        upgrade.addListener(click(() -> upgradePlant(spec)));
        detailContent.add(upgrade)
                .width(285f)
                .height(48f)
                .colspan(2)
                .padTop(8f)
                .padBottom(8f)
                .row();
    }

    private AnimationSpec plantAnimation(PlantSpec spec) {
        String path = plantVisuals.animationPath(spec.getName());
        if (path == null) {
            return null;
        }
        return new AnimationSpec(
                path,
                List.of(plantVisuals.animationClip(spec.getName()))
        );
    }

    private void buildZombieDetails(ZombieSpec spec) {
        detailContent.add(detailTitle(spec.getName()))
                .width(DETAIL_CONTENT_WIDTH)
                .height(44f)
                .colspan(2)
                .row();

        ZombieAnimationSpec animation = zombieAnimation(
                spec,
                appState.getCurrentUser()
        );
        Actor preview = animation == null
                ? emptyPreview(190f, 190f)
                : createZombiePreview(animation, 190f, 190f);
        detailContent.add(preview)
                .size(190f, 190f)
                .colspan(2)
                .padBottom(8f)
                .row();

        addDetailRow("Health", String.valueOf(spec.getHitpoints()));
        addDetailRow("Speed", formatNumber(spec.getSpeed()));
        addDetailRow("Eat DPS", String.valueOf(spec.getEatDps()));
        addDetailRow("Wave Cost", String.valueOf(spec.getWaveCost()));
        addDetailRow("Wave Weight", String.valueOf(spec.getWaveWeight()));
        addDetailRow("Armor", spec.getArmor());
        addDetailParagraph(
                "BEHAVIORS",
                spec.getBehaviorTypes().isEmpty()
                        ? "NONE"
                        : String.join(", ", spec.getBehaviorTypes())
        );
    }

    private void addDetailRow(String name, String value) {
        Label key = new Label(name + ":", skin);
        key.setColor(Color.DARK_GRAY);
        key.setFontScale(0.82f);
        key.setAlignment(Align.left);

        Label detail = new Label(value == null ? "-" : value, skin);
        detail.setColor(Color.DARK_GRAY);
        detail.setFontScale(0.82f);
        detail.setWrap(true);
        detail.setAlignment(Align.left);

        detailContent.add(key)
                .width(112f)
                .height(30f)
                .left();
        detailContent.add(detail)
                .width(DETAIL_CONTENT_WIDTH - 122f)
                .minHeight(30f)
                .left()
                .row();
    }

    private void addDetailParagraph(String heading, String body) {
        Label title = new Label(heading, skin);
        title.setColor(Color.DARK_GRAY);
        title.setFontScale(0.78f);
        title.setAlignment(Align.left);
        detailContent.add(title)
                .width(DETAIL_CONTENT_WIDTH)
                .height(26f)
                .colspan(2)
                .left()
                .padTop(5f)
                .row();

        Label text = detailText(body == null || body.isBlank() ? "-" : body);
        detailContent.add(text)
                .width(DETAIL_CONTENT_WIDTH)
                .minHeight(42f)
                .colspan(2)
                .left()
                .row();
    }

    private Label detailTitle(String text) {
        Label label = new Label(text, skin);
        label.setColor(Color.DARK_GRAY);
        label.setFontScale(1.12f);
        label.setAlignment(Align.center);
        label.setWrap(true);
        return label;
    }

    private Label detailText(String text) {
        Label label = new Label(text == null ? "-" : text, skin);
        label.setColor(Color.DARK_GRAY);
        label.setFontScale(0.78f);
        label.setWrap(true);
        label.setAlignment(Align.left);
        return label;
    }

    private String formatTags(PlantSpec spec) {
        if (spec.getTags().isEmpty()) {
            return "NONE";
        }
        return spec.getTags().stream()
                .map(this::pretty)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
    }

    private String formatSeconds(double value) {
        return formatNumber(value) + " s";
    }

    private String formatNumber(double value) {
        if (Math.rint(value) == value) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private void purchasePlant(PlantSpec spec) {
        if (appState.getCurrentUser() == null) {
            showStatus("No active user.", true);
            return;
        }
        controller.handle(new CollectionCommand(
                CollectionCommand.Action.PURCHASE_PLANT,
                spec.getName()
        ));
        selectedPlant = spec;
        refreshAll(false);
    }

    private void upgradePlant(PlantSpec spec) {
        if (appState.getCurrentUser() == null) {
            showStatus("No active user.", true);
            return;
        }
        controller.handle(new CollectionCommand(
                CollectionCommand.Action.UPGRADE_PLANT,
                spec.getName()
        ));
        selectedPlant = spec;
        refreshAll(false);
    }

    private Table cardBase() {
        Table card = new Table();
        card.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        card.defaults().pad(1f);
        return card;
    }

    private Label cardLabel(String text, float fontScale) {
        Label label = new Label(text, skin);
        label.setColor(Color.DARK_GRAY);
        label.setFontScale(fontScale);
        label.setAlignment(Align.center);
        label.setWrap(true);
        return label;
    }

    private Actor placeholder(String text) {
        Label label = new Label(text, skin);
        label.setColor(Color.GRAY);
        label.setFontScale(2f);
        label.setAlignment(Align.center);
        return label;
    }

    private Actor fallbackActor(TextureRegion region, String fallbackText) {
        if (region == null) {
            return placeholder(fallbackText);
        }
        Image image = new Image(region);
        image.setScaling(Scaling.fit);
        return image;
    }

    private Actor emptyPreview(float width, float height) {
        Group empty = new Group();
        empty.setSize(width, height);
        return empty;
    }

    private Actor createZombiePreview(
            ZombieAnimationSpec animation,
            float width,
            float height
    ) {
        return new ZombiePamPreview(
                zombieAnimationRenderer,
                animation,
                width,
                height
        );
    }

    private LazyPamPreview createPamPreview(
            AnimationSpec animation,
            TextureRegion fallback,
            float width,
            float height
    ) {
        LazyPamPreview preview = new LazyPamPreview(
                animationService,
                animation,
                fallback,
                width,
                height
        );
        activePreviews.add(preview);
        return preview;
    }

    private void cancelPreviews() {
        for (LazyPamPreview preview : activePreviews) {
            preview.cancel();
        }
        activePreviews.clear();
    }

    private void goBack() {
        if (returnMenu == MenuName.GREENHOUSE) {
            game.setScreen(new GreenhouseScreen(
                    game,
                    textures,
                    batch,
                    skin,
                    appState,
                    userManager,
                    game.getGameData().greenhouseService()
            ));
            return;
        }

        game.setScreen(new GameMenuScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager
        ));
    }

    private void showStatus(String message, Boolean error) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.setColor(Boolean.TRUE.equals(error) ? Color.RED : Color.GREEN);
        statusLabel.setText(message == null ? "" : message);
    }

    private String pretty(Enum<?> value) {
        return pretty(value == null ? "" : value.name());
    }

    private String pretty(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String normalized = value.strip()
                .replace('_', ' ')
                .toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (int index = 0; index < normalized.length(); index++) {
            char ch = normalized.charAt(index);
            if (capitalize && Character.isLetter(ch)) {
                result.append(Character.toUpperCase(ch));
                capitalize = false;
            } else {
                result.append(ch);
            }
            if (Character.isWhitespace(ch)) {
                capitalize = true;
            }
        }
        return result.toString();
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
        appState.setCurrentMenu(MenuName.COLLECTION);
        refreshCurrency();
    }

    @Override
    public void dispose() {
        cancelPreviews();
        zombieAnimationRenderer.dispose();
        super.dispose();
    }

    private enum Tab {
        PLANTS,
        ZOMBIES
    }

    private enum OwnershipFilter {
        ALL,
        OWNED,
        LOCKED
    }

    private record ZombieAnimationSpec(
            String path,
            String clip,
            String referenceClip,
            Map<String, Boolean> partsVisibility
    ) {
        private ZombieAnimationSpec {
            Objects.requireNonNull(path, "animation path cannot be null");
            Objects.requireNonNull(clip, "animation clip cannot be null");
            Objects.requireNonNull(
                    referenceClip,
                    "reference clip cannot be null"
            );
            partsVisibility = Map.copyOf(partsVisibility);
        }
    }

    private record AnimationSpec(String path, List<String> clips) {
        private AnimationSpec {
            Objects.requireNonNull(path, "animation path cannot be null");
            clips = List.copyOf(clips);
        }
    }

    private static final class ZombiePamPreview extends Actor {
        private final PamAnimationRenderer renderer;
        private final ZombieAnimationSpec animation;
        private float stateTime;

        private ZombiePamPreview(
                PamAnimationRenderer renderer,
                ZombieAnimationSpec animation,
                float width,
                float height
        ) {
            this.renderer = Objects.requireNonNull(
                    renderer,
                    "animation renderer cannot be null"
            );
            this.animation = Objects.requireNonNull(
                    animation,
                    "animation cannot be null"
            );
            setSize(width, height);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            stateTime += Math.max(0f, delta);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            renderer.draw(
                    batch,
                    animation.path(),
                    animation.clip(),
                    animation.referenceClip(),
                    stateTime,
                    getX(),
                    getY(),
                    getWidth(),
                    getHeight(),
                    parentAlpha,
                    false,
                    true,
                    Color.WHITE,
                    animation.partsVisibility()
            );
        }
    }

    private final class LazyPamPreview extends Group {
        private final PamAnimationService service;
        private final AnimationSpec animation;
        private PamAnimationService.AnimationRequest clipRequest;
        private PamAnimationService.AnimationRequest boundsRequest;
        private boolean requested;
        private boolean initialized;
        private boolean cancelled;

        private LazyPamPreview(
                PamAnimationService service,
                AnimationSpec animation,
                TextureRegion fallback,
                float width,
                float height
        ) {
            this.service = service;
            this.animation = animation;
            setSize(width, height);
            if (fallback != null) {
                addActor(fallbackActor(fallback, "?"));
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (!requested && !initialized && !cancelled) {
                requested = true;
                clipRequest = service.prepareFirstAvailable(
                        animation.path(),
                        animation.clips(),
                        this::prepareBounds
                );
            }
            super.draw(batch, parentAlpha);
        }

        private void prepareBounds(String clipName) {
            clipRequest = null;
            if (cancelled || clipName == null) {
                initialized = true;
                return;
            }
            boundsRequest = service.prepare(
                    animation.path(),
                    clipName,
                    bounds -> initializeAnimation(clipName, bounds)
            );
        }

        private void initializeAnimation(String clipName, Rectangle bounds) {
            boundsRequest = null;
            if (cancelled || initialized || getStage() == null) {
                return;
            }
            if (bounds == null || bounds.width <= 0f || bounds.height <= 0f) {
                initialized = true;
                return;
            }

            float usableWidth = getWidth() * 0.82f;
            float usableHeight = getHeight() * 0.82f;
            float scale = Math.min(
                    usableWidth / bounds.width,
                    usableHeight / bounds.height
            );
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;

            Group scaler = new Group();
            scaler.setTransform(true);
            scaler.setSize(getWidth(), getHeight());
            scaler.setOrigin(centerX, centerY);
            scaler.setScale(scale);

            PlantActor actor = new PlantActor(
                    service.player(),
                    animation.path(),
                    clipName
            );
            actor.setSize(getWidth(), getHeight());
            actor.setPosition(
                    -bounds.x - bounds.width / 2f,
                    centerY - 45f + bounds.y + bounds.height / 2f
            );

            clearChildren();
            scaler.addActor(actor);
            addActor(scaler);
            initialized = true;
        }

        private void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            if (clipRequest != null) {
                clipRequest.cancel();
                clipRequest = null;
            }
            if (boundsRequest != null) {
                boundsRequest.cancel();
                boundsRequest = null;
            }
        }
    }
}
