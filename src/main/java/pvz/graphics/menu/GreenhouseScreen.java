package pvz.graphics.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import pvz.graphics.BaseScreen;
import pvz.graphics.asset.PlantVisualResolver;
import pvz.graphics.PvzGame;
import pvz.graphics.actor.PlantActor;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.greenhouse.*;
import pvz.model.service.GreenhouseService;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;


public class GreenhouseScreen extends BaseScreen {

    private static final int ROWS = 3, COLS = 4;
    private static final float POT_SIZE = 68f;
    private static final float CELL_W = 128f, CELL_H = 144f;
    private static final float GRID_TOP = 202f;

    private static final float POT_SCALE = 0.45f;
    private static final float PLANT_ORIGIN_Y = 45f;
    private static final float GROWING_DIRT_SIZE = 45f;
    private static final float GEM_BADGE_SIZE = 15f;
    private static final float REFRESH_INTERVAL = 0.5f;
    private static final float TIMER_SCALE = 0.9f;

    private static final String POT_TEXTURE = "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161_2";
    private static final String GROWING_TEXTURE = "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_122X161";
    private static final String BEE_PATH = "768/INITIAL/ZEN_GARDEN/BEE/BEE.PAM";

    private final GreenhouseService greenhouseService;
    private final PamPlayer pamPlayer;
    private final PlantVisualResolver plantVisuals;

    private final Group greenhouseGroup = new Group();
    private Label sproutLabel;
    private Label diamondLabel;
    private Label coinLabel;

    private final PotState[][] lastStates = new PotState[ROWS][COLS];
    private final Label[][] timerLabels = new Label[ROWS][COLS];
    private final Label[][] costLabels = new Label[ROWS][COLS];

    private float refreshTimer;
    private boolean refreshing;

    public GreenhouseScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager,
            GreenhouseService greenhouseService
    ) {
        super(game, textures, batch, skin, appState, userManager, "IMAGE_BACKGROUNDS_ZEN_GARDEN");
        this.greenhouseService = greenhouseService;
        this.pamPlayer = game.getAnimationService().player();
        this.plantVisuals = new PlantVisualResolver(
                textures,
                Gdx.files.internal("assets")
        );
        buildUI();
    }

    private void buildUI() {
        greenhouseGroup.setSize(WIDTH, HEIGHT);
        stage.addActor(greenhouseGroup);

        rebuildPots(true);
        buildTopBar();
        buildCurrencies();
        buildBee();
    }

    private void buildBee() {
        if (!assetExists(BEE_PATH)) return;

        Group group = new Group();
        group.setBounds(WIDTH - 200f, HEIGHT - 250f, 100f, 100f);

        PlantActor bee = new PlantActor(pamPlayer, BEE_PATH);
        bee.setSize(100f, 100f);
        group.addActor(bee);

        stage.addActor(group);
        group.toFront();
    }

    private void buildTopBar() {
        float size = 55f;
        float gap = 10f;
        float y = HEIGHT - 80f;

        TextureRegion normal = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_NORMAL");
        TextureRegion pressed = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_PRESSED");

        if (normal == null) throw new IllegalStateException("Back button texture not found.");

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = new TextureRegionDrawable(normal);
        style.down = pressed == null ? style.up : new TextureRegionDrawable(pressed);

        ImageButton back = new ImageButton(style);
        back.setBounds(25f, y, size, size);

        back.addListener(click(() -> game.setScreen(new GameMenuScreen(game, textures, batch, skin, appState, userManager))));

        Image collection = image("IMAGE_UI_HUD_ALMANACBUTTON_BUTTONS_HUD_ALMANAC_NORMAL");
        collection.setBounds(25f + size + gap, y, size, size);
        collection.addListener(click(() -> game.setScreen(new CollectionScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager,
                MenuName.GREENHOUSE
        ))));

        TextButton store = new TextButton("", skin, "brown");
        TextureRegion storeRegion = textures.region("IMAGE_UI_ALMANAC_FINDMORE_STORE");

        if (storeRegion != null) {
            Image storeImage = new Image(storeRegion);
            storeImage.setBounds(5f, 5f, 45f, 45f);
            store.addActor(storeImage);
        }

        store.setBounds(25f + 2f * (size + gap), y, size, size);

        stage.addActor(back);
        stage.addActor(collection);

        store.addListener(click(() -> game.setScreen(new ShopScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager,
                greenhouseService
        ))));
        stage.addActor(store);
    }

    private void buildCurrencies() {
        TextureRegion diamond = textures.region("IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL");
        TextureRegion coin = textures.region("IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL");
        TextureRegion sprout = textures.region("IMAGE_UI_HUD_INGAME_SPROUT_ICON");

        if (diamond == null || coin == null) throw new IllegalStateException("Currency texture not found.");

        Group currencies = new Group();

        sproutLabel = createCurrencyLabel(getStoredBoostsCount(), 1.2f);
        Group sproutGroup = currencyGroup(sprout, sproutLabel, 100f);

        diamondLabel = createCurrencyLabel(getDiamondCount(), 1f);
        Group diamondGroup = currencyGroup(diamond, diamondLabel, diamond.getRegionWidth());

        coinLabel = createCurrencyLabel(getCoinCount(), 1f);
        Group coinGroup = currencyGroup(coin, coinLabel, 150f);

        diamondGroup.setTouchable(Touchable.enabled);
        diamondGroup.addListener(click(() -> {
            if (isDebugModeEnabled()) {
                appState.getCurrentUser().addDiamonds(100);
                rebuildCurrencies();
                userManager.save();
            }
        }));

        coinGroup.setTouchable(Touchable.enabled);
        coinGroup.addListener(click(() -> {
            if (isDebugModeEnabled()) {
                appState.getCurrentUser().addCoins(100);
                rebuildCurrencies();
                userManager.save();
            }
        }));

        sproutGroup.setPosition(0f, 0f);
        diamondGroup.setPosition(110f, 0f);
        coinGroup.setPosition(120f + diamond.getRegionWidth(), 0f);

        currencies.addActor(sproutGroup);
        currencies.addActor(diamondGroup);
        currencies.addActor(coinGroup);

        currencies.setSize(130f + diamond.getRegionWidth() + 150f, Math.max(diamond.getRegionHeight(), coin.getRegionHeight()));
        currencies.setPosition(WIDTH - currencies.getWidth() - 20f, HEIGHT - currencies.getHeight() - 20f);

        stage.addActor(currencies);
        currencies.toFront();
    }

    private boolean isDebugModeEnabled() {
        return appState.getCurrentUser() != null && appState.getCurrentUser().isDebugMode();
    }

    private Label createCurrencyLabel(String text, float scale) {
        Label label = new Label(text, skin);
        label.setColor(Color.WHITE);
        label.setFontScale(scale);
        return label;
    }

    private Group currencyGroup(TextureRegion region, Label label, float width) {
        Group group = new Group();
        float height = region == null ? 55f : region.getRegionHeight();
        group.setSize(width, height);

        if (region != null) {
            Image image = new Image(region);
            image.setSize(width, height);
            group.addActor(image);
        }

        label.pack();
        label.setPosition(width * 0.5f, 17f);
        group.addActor(label);

        return group;
    }

    private String getDiamondCount() {
        User user = appState.getCurrentUser();
        return user == null ? "0" : String.valueOf(user.getDiamonds());
    }

    private String getCoinCount() {
        User user = appState.getCurrentUser();
        return user == null ? "0" : String.valueOf(user.getCoins());
    }

    private String getStoredBoostsCount() {
        User user = appState.getCurrentUser();
        return user == null || user.getStoredBoosts() == null ? "0" : String.valueOf(user.getStoredBoosts().size());
    }

    private void rebuildPots(boolean force) {
        if (refreshing) return;

        User user = appState.getCurrentUser();
        if (user == null || user.getGreenhouse() == null) return;

        Greenhouse greenhouse = user.getGreenhouse();
        greenhouse.updateAllPots();

        if (!force && !statesChanged(greenhouse)) {
            updateTimers(greenhouse);
            return;
        }

        refreshing = true;
        try {
            greenhouseGroup.clearChildren();
            clearLabels();

            float startX = (WIDTH - COLS * CELL_W) / 2f;
            float topY = HEIGHT - GRID_TOP;

            for (int y = 1; y <= ROWS; y++) {
                for (int x = 1; x <= COLS; x++) {
                    Pot pot = greenhouse.getPot(x, y);
                    if (pot != null) {
                        Group potGroup = createPot(pot, x, y, startX, topY);
                        greenhouseGroup.addActor(potGroup);
                    }
                }
            }

            saveStates(greenhouse);
        } finally {
            refreshing = false;
        }
    }

    private void clearLabels() {
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                timerLabels[y][x] = null;
                costLabels[y][x] = null;
            }
        }
    }

    private void updateTimers(Greenhouse greenhouse) {
        for (int y = 1; y <= ROWS; y++) {
            for (int x = 1; x <= COLS; x++) {
                Pot pot = greenhouse.getPot(x, y);
                if (pot == null || pot.getState() != PotState.GROWING) continue;

                GreenhousePlant plant = pot.getPlant();
                if (plant == null) continue;

                Label timer = timerLabels[y - 1][x - 1];
                Label cost = costLabels[y - 1][x - 1];

                if (timer != null) {
                    timer.setText(plant.getExactRemainingTime());
                    timer.pack();
                }

                if (cost != null) {
                    cost.setText(String.valueOf(plant.getRemainingHours()));
                    cost.pack();
                }
            }
        }
    }

    private boolean statesChanged(Greenhouse greenhouse) {
        for (int y = 1; y <= ROWS; y++) {
            for (int x = 1; x <= COLS; x++) {
                Pot pot = greenhouse.getPot(x, y);
                PotState state = pot == null ? null : pot.getState();
                if (lastStates[y - 1][x - 1] != state) return true;
            }
        }
        return false;
    }

    private void saveStates(Greenhouse greenhouse) {
        for (int y = 1; y <= ROWS; y++) {
            for (int x = 1; x <= COLS; x++) {
                Pot pot = greenhouse.getPot(x, y);
                lastStates[y - 1][x - 1] = pot == null ? null : pot.getState();
            }
        }
    }

    private Group createPot(Pot pot, int x, int y, float startX, float topY) {
        float px = startX + (x - 1) * CELL_W + (CELL_W - POT_SIZE) / 2f;
        float py = topY - y * CELL_H + (CELL_H - POT_SIZE) / 2f;

        switch (pot.getState()) {
            case LOCKED: return createLockedPot(px, py);
            case EMPTY: return createEmptyPot(px, py, x, y);
            case GROWING: return createGrowingPot(pot, px, py, x, y);
            case READY: return createReadyPot(pot, px, py, x, y);
            default: return potGroup(px, py);
        }
    }

    private Group potGroup(float px, float py) {
        Group group = new Group();
        group.setBounds(px, py, POT_SIZE, POT_SIZE);
        return group;
    }

    private Image potImage() {
        TextureRegion region = textures.region(POT_TEXTURE);
        if (region == null) return null;
        Image image = new Image(region);
        image.setSize(POT_SIZE, POT_SIZE);
        return image;
    }

    private Group createLockedPot(float px, float py) {
        Group group = potGroup(px, py);
        TextureRegion region = textures.region("IMAGE_ZEN_GARDEN_LOCKED_POT_ICON");

        if (region != null) {
            Image lockImage = new Image(region);
            float lockSize = 40f;
            lockImage.setSize(lockSize, lockSize);
            lockImage.setPosition(
                    (POT_SIZE - lockSize) / 2f,
                    (POT_SIZE - lockSize) / 2f
            );
            group.addActor(lockImage);
        }

        group.setTouchable(Touchable.disabled);
        return group;
    }

    private Group createEmptyPot(float px, float py, int potX, int potY) {
        Group group = potGroup(px, py);
        Image pot = potImage();
        if (pot == null) return group;

        pot.addListener(click(() -> plant(potX, potY)));
        group.addActor(pot);
        return group;
    }

    private Group createGrowingPot(Pot pot, float px, float py, int potX, int potY) {
        Group group = potGroup(px, py);
        Image potImage = potImage();
        if (potImage != null) group.addActor(potImage);

        TextureRegion region = textures.region(GROWING_TEXTURE);
        if (region != null) {
            Image growing = new Image(region);
            growing.setSize(GROWING_DIRT_SIZE, GROWING_DIRT_SIZE);
            growing.setPosition((POT_SIZE - GROWING_DIRT_SIZE) / 2f, (POT_SIZE - GROWING_DIRT_SIZE) / 2f + 16f);
            group.addActor(growing);
        }

        GreenhousePlant plant = pot.getPlant();
        if (plant != null) {
            addTimer(group, plant, potX, potY);
            addFastGrowButton(group, plant, potX, potY);
        }

        group.setTouchable(Touchable.enabled);
        return group;
    }

    private void addTimer(Group group, GreenhousePlant plant, int x, int y) {
        TextureRegion region = textures.region("IMAGE_ZEN_GARDEN_FINISH_TIMER_BACKGROUND");
        if (region != null) {
            Image background = new Image(region);
            background.setBounds(-2f, -22f, 55f, 25f);
            group.addActor(background);
        }

        Label label = new Label(plant.getExactRemainingTime(), skin);
        label.setColor(Color.WHITE);
        label.setFontScale(TIMER_SCALE);
        label.pack();
        label.setPosition(3f, -19f);

        timerLabels[y - 1][x - 1] = label;
        group.addActor(label);
    }

    private void addFastGrowButton(Group group, GreenhousePlant plant, int potX, int potY) {
        Group buttonGroup = new Group();
        buttonGroup.setBounds(38f, -25f, 44f, 35f);

        TextButton button = new TextButton("", skin, "purple");
        button.setSize(44f, 35f);
        buttonGroup.addActor(button);

        Label cost = new Label(String.valueOf(plant.getRemainingHours()), skin);
        cost.setColor(Color.WHITE);
        cost.setFontScale(0.9f);
        cost.setPosition(18f, 8f);
        costLabels[potY - 1][potX - 1] = cost;
        buttonGroup.addActor(cost);

        TextureRegion badge = textures.region("IMAGE_ZEN_GARDEN_GEM_LARGE");
        if (badge != null) {
            Image image = new Image(badge);
            image.setBounds(-5f, 19f, GEM_BADGE_SIZE, GEM_BADGE_SIZE);
            buttonGroup.addActor(image);
        }

        buttonGroup.addListener(click(() -> grow(potX, potY)));
        group.addActor(buttonGroup);
    }

    private Group createReadyPot(Pot pot, float px, float py, int potX, int potY) {
        Group group = potGroup(px, py);
        Image image = potImage();
        if (image != null) group.addActor(image);

        GreenhousePlant plant = pot.getPlant();
        if (plant != null) {
            addPlantAnimation(group, plant.getPlantName());
        }

        group.addListener(click(() -> collect(potX, potY)));
        group.setTouchable(Touchable.enabled);
        return group;
    }

    private void addPlantAnimation(Group group, String plantName) {
        String path = plantVisuals.animationPath(plantName);
        if (path == null) {
            return;
        }

        String clip = plantVisuals.animationClip(plantName);

        Group scaler = new Group();
        scaler.setSize(POT_SIZE, POT_SIZE);
        scaler.setTransform(true);
        scaler.setOrigin(POT_SIZE / 2f, PLANT_ORIGIN_Y);
        scaler.setScale(POT_SCALE);

        PlantActor actor = new PlantActor(
                pamPlayer,
                path,
                clip
        );
        actor.setSize(POT_SIZE, POT_SIZE);

        scaler.addActor(actor);
        group.addActor(scaler);
    }

    private boolean assetExists(String path) {
        return Gdx.files.internal("assets/IMAGES/" + path).exists()
                || Gdx.files.internal("IMAGES/" + path).exists()
                || Gdx.files.internal("assets/" + path).exists()
                || Gdx.files.internal(path).exists();
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

    private void plant(int x, int y) {
        execute(() -> greenhouseService.plant(appState.getCurrentUser(), x, y));
    }

    private void collect(int x, int y) {
        User currentUser = appState.getCurrentUser();
        if (currentUser == null || currentUser.getGreenhouse() == null) {
            return;
        }

        try {
            int initialCoins = currentUser.getCoins();
            int initialBoosts = currentUser.getStoredBoosts() != null ? currentUser.getStoredBoosts().size() : 0;

            greenhouseService.collect(currentUser, x, y);

            int finalCoins = currentUser.getCoins();
            int finalBoosts = currentUser.getStoredBoosts() != null ? currentUser.getStoredBoosts().size() : 0;

            String rewardText;
            if (finalCoins > initialCoins) {
                rewardText = "+ 500 Coins!";
            } else if (finalBoosts > initialBoosts) {
                rewardText = "+ 1 Boost!";
            } else {
                rewardText = "Harvested!";
            }

            if (saveAndRefresh()) {
                showRewardNotification(rewardText);
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void grow(int x, int y) {
        execute(() -> greenhouseService.forceGrow(appState.getCurrentUser(), x, y));
    }

    private void showRewardNotification(String message) {
        Label label = new Label(message, skin);
        label.setColor(Color.YELLOW);
        label.setFontScale(1.3f);
        label.pack();

        label.setPosition((WIDTH - label.getWidth()) / 2f, HEIGHT / 2f + 50f);
        label.getColor().a = 0f;

        label.addAction(Actions.sequence(
                Actions.fadeIn(0.2f),
                Actions.moveBy(0f, 60f, 2.5f, Interpolation.sineOut),
                Actions.fadeOut(0.3f),
                Actions.removeActor()
        ));

        stage.addActor(label);
    }

    private void execute(Action action) {
        User user = appState.getCurrentUser();
        if (user == null) return;

        try {
            action.run();
            saveAndRefresh();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private boolean saveAndRefresh() {
        if (!userManager.save()) {
            User current = appState.getCurrentUser();
            String username = current == null ? null : current.getUsername();

            userManager.reload();

            if (current != null) {
                User reloaded = userManager.find(u -> u.getUsername().equals(username));
                appState.setCurrentUser(reloaded);
            }

            rebuildPots(true);
            rebuildCurrencies();
            showError("Failed to save greenhouse changes.");
            return false;
        }

        rebuildPots(true);
        rebuildCurrencies();
        return true;
    }

    private void rebuildCurrencies() {
        updateCurrencyLabel(sproutLabel, getStoredBoostsCount());
        updateCurrencyLabel(diamondLabel, getDiamondCount());
        updateCurrencyLabel(coinLabel, getCoinCount());
    }

    private void updateCurrencyLabel(Label label, String value) {
        if (label == null) {
            return;
        }

        label.setText(value);
        label.pack();
    }

    private void showError(String message) {
        String errorMessage = message == null || message.isBlank()
                ? "Greenhouse action failed."
                : message;

        Label label = new Label(errorMessage, skin);
        label.setColor(Color.RED);
        label.setFontScale(1.05f);
        label.pack();
        label.setPosition(
                (WIDTH - label.getWidth()) / 2f,
                HEIGHT / 2f - 25f
        );

        label.addAction(Actions.sequence(
                Actions.delay(2.5f),
                Actions.fadeOut(0.3f),
                Actions.removeActor()
        ));

        stage.addActor(label);
    }

    @Override
    public void render(float delta) {
        textures.update();
        refreshTimer += delta;

        if (refreshTimer >= REFRESH_INTERVAL) {
            refreshTimer = 0f;
            rebuildPots(false);
        }

        super.render(delta);
    }

    @Override
    public void show() {
        super.show();
        appState.setCurrentMenu(MenuName.GREENHOUSE);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        viewport.update(width, height, true);
    }
}
