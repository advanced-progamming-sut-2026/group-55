package pvz.graphics.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import pvz.graphics.BaseScreen;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.greenhouse.Greenhouse;
import pvz.model.greenhouse.GreenhousePlant;
import pvz.model.greenhouse.Pot;
import pvz.model.service.GreenhouseService;
import pvz.model.utils.AppState;

import java.util.HashMap;
import java.util.Map;

public class GreenhouseScreen extends BaseScreen {

    private static final int ROWS = 3, COLS = 4;
    private static final float POT_SIZE = 68f, CELL_WIDTH = 128f, CELL_HEIGHT = 144f;
    private static final float GRID_TOP_PADDING = 202f, LOCK_SIZE = 36f;
    private static final float REFRESH_INTERVAL = 0.5f;
    private static final float NAME_SCALE = 0.8f, TIMER_SCALE = 0.65f, READY_SCALE = 0.7f;

    private static final Map<String, String> PAM_PATHS = new HashMap<>();

    static {
        PAM_PATHS.put("ALOE", "768/INITIAL/PLANT/ALOE/ALOE.PAM");
        PAM_PATHS.put("SUNFLOWER", "768/INITIAL/PLANT/SUNFLOWER/SUNFLOWER.PAM");
        PAM_PATHS.put("PEASHOOTER", "768/INITIAL/PLANT/PEASHOOTER/PEASHOOTER.PAM");
        PAM_PATHS.put("WALLNUT", "768/INITIAL/PLANT/WALLNUT/WALLNUT.PAM");
        PAM_PATHS.put("POTATO_MINE", "768/INITIAL/PLANT/POTATO_MINE/POTATO_MINE.PAM");
        PAM_PATHS.put("CHOMPER", "768/INITIAL/PLANT/CHOMPER/CHOMPER.PAM");
        PAM_PATHS.put("SNOW_PEA", "768/INITIAL/PLANT/SNOW_PEA/SNOW_PEA.PAM");
        PAM_PATHS.put("REPEATER", "768/INITIAL/PLANT/REPEATER/REPEATER.PAM");
        PAM_PATHS.put("FIRE_PEA", "768/INITIAL/PLANT/FIRE_PEASHOOTER/FIRE_PEASHOOTER.PAM");
        PAM_PATHS.put("FIRE_PEASHOOTER", "768/INITIAL/PLANT/FIRE_PEASHOOTER/FIRE_PEASHOOTER.PAM");
    }

    private final Group greenhouseGroup = new Group();
    private final GreenhouseService greenhouseService;
    private final PamPlayer pamPlayer;
    private float refreshTimer;
    private boolean refreshing;

    public GreenhouseScreen(Game game, TextureBank textures, SpriteBatch batch, Skin skin,
                            AppState appState, UserManager userManager,
                            GreenhouseService greenhouseService) {
        super(game, textures, batch, skin, appState, userManager,
                "IMAGE_BACKGROUNDS_ZEN_GARDEN");
        this.greenhouseService = greenhouseService;
        this.pamPlayer = new PamPlayer(textures, Gdx.files.internal("assets"));
        buildUI();
    }

    private void buildUI() {
        buildPots();
        buildTopBar();
        buildCurrencies();
    }

    private void buildTopBar() {
        float size = 55f, gap = 10f, y = HEIGHT - 80f;

        Image back = image("IMAGE_UI_MAINMENU_BACK_BTN_NORMAL");
        Image collection = image("IMAGE_UI_HUD_ALMANACBUTTON_BUTTONS_HUD_ALMANAC_NORMAL");
        TextButton storeButton = new TextButton("", skin, "brown");

        TextureRegion storeRegion = textures.region("IMAGE_UI_ALMANAC_FINDMORE_STORE");
        if (storeRegion != null) {
            Image storeImage = new Image(storeRegion);
            storeImage.setSize(45f, 45f);
            storeImage.setPosition(5f, 5f);
            storeButton.addActor(storeImage);
        }

        back.setSize(size, size);
        collection.setSize(size, size);
        storeButton.setSize(size, size);

        back.setPosition(25f, y);
        collection.setPosition(25f + size + gap, y);
        storeButton.setPosition(25f + (size + gap) * 2f, y);

        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameMenuScreen(
                        game, textures, batch, skin, appState, userManager));
            }
        });

        stage.addActor(back);
        stage.addActor(collection);
        stage.addActor(storeButton);

        back.toFront();
        collection.toFront();
        storeButton.toFront();
    }

    private void buildCurrencies() {
        TextureRegion diamondRegion =
                textures.region("IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL");
        TextureRegion coinRegion =
                textures.region("IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL");

        if (diamondRegion == null || coinRegion == null)
            throw new IllegalStateException("Currency texture not found.");

        float diamondWidth = diamondRegion.getRegionWidth();
        float diamondHeight = diamondRegion.getRegionHeight();
        float coinHeight = coinRegion.getRegionHeight();
        float coinWidth = 150f;

        Image diamond = new Image(diamondRegion);
        Image coin = new Image(coinRegion);
        Group diamondGroup = new Group();
        Group coinGroup = new Group();

        diamondGroup.setSize(diamondWidth, diamondHeight);
        diamondGroup.addActor(diamond);

        coin.setSize(coinWidth, coinHeight);
        coinGroup.setSize(coinWidth, coinHeight);
        coinGroup.addActor(coin);

        Label diamondCount = new Label(getDiamondCount(), skin);
        Label coinCount = new Label(getCoinCount(), skin);

        diamondCount.setColor(Color.WHITE);
        coinCount.setColor(Color.WHITE);
        diamondCount.pack();
        coinCount.pack();

        diamondCount.setPosition(70f, 17f);
        coinCount.setPosition(65f, 17f);

        diamondGroup.addActor(diamondCount);
        coinGroup.addActor(coinCount);

        Group currencies = new Group();
        coinGroup.setPosition(diamondWidth + 10f, 0f);
        currencies.addActor(diamondGroup);
        currencies.addActor(coinGroup);

        currencies.setSize(
                diamondWidth + 10f + coinWidth,
                Math.max(diamondHeight, coinHeight));

        currencies.setPosition(
                WIDTH - currencies.getWidth() - 20f,
                HEIGHT - currencies.getHeight() - 20f);

        stage.addActor(currencies);
    }

    private String getDiamondCount() {
        User user = appState.getCurrentUser();
        return user == null ? "0" : String.valueOf(user.getDiamonds());
    }

    private String getCoinCount() {
        User user = appState.getCurrentUser();
        return user == null ? "0" : String.valueOf(user.getCoins());
    }

    private void buildPots() {
        greenhouseGroup.setSize(WIDTH, HEIGHT);
        greenhouseGroup.setTouchable(Touchable.childrenOnly);
        stage.addActor(greenhouseGroup);
        rebuildPots();
    }

    private void rebuildPots() {
        if (refreshing) return;
        refreshing = true;

        try {
            greenhouseGroup.clearChildren();

            User user = appState.getCurrentUser();
            if (user == null || user.getGreenhouse() == null) return;

            Greenhouse greenhouse = user.getGreenhouse();
            greenhouse.updateAllPots();

            float gridStartX = (WIDTH - COLS * CELL_WIDTH) / 2f;
            float topY = HEIGHT - GRID_TOP_PADDING;

            for (int y = 1; y <= ROWS; y++)
                for (int x = 1; x <= COLS; x++)
                    createPot(greenhouse.getPot(x, y), x, y, gridStartX, topY);
        } finally {
            refreshing = false;
        }
    }

    private void createPot(Pot pot, int x, int y, float gridStartX, float topY) {
        if (pot == null) return;

        float cellX = gridStartX + (x - 1) * CELL_WIDTH;
        float cellTop = topY - (y - 1) * CELL_HEIGHT;
        float posX = cellX + (CELL_WIDTH - POT_SIZE) / 2f;
        float posY = cellTop - CELL_HEIGHT + (CELL_HEIGHT - POT_SIZE) / 2f;

        switch (pot.getState()) {
            case LOCKED -> createLockedPot(posX, posY);
            case EMPTY -> createEmptyPot(posX, posY, x, y);
            case GROWING -> createGrowingPot(pot, posX, posY, x, y);
            case READY -> createReadyPot(pot, posX, posY, x, y);
        }
    }

    private void createLockedPot(float x, float y) {
        TextureRegion region = textures.region("IMAGE_ZEN_GARDEN_LOCKED_POT_ICON");
        if (region == null) return;

        Image lock = new Image(region);
        lock.setSize(LOCK_SIZE, LOCK_SIZE);
        lock.setPosition(
                x + (POT_SIZE - LOCK_SIZE) / 2f,
                y + (POT_SIZE - LOCK_SIZE) / 2f);
        greenhouseGroup.addActor(lock);
    }

    private Image createPotImage() {
        TextureRegion region = textures.region(
                "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161_2");
        if (region == null) return null;

        Image image = new Image(region);
        image.setSize(POT_SIZE, POT_SIZE);
        return image;
    }

    private void createEmptyPot(float x, float y, final int potX, final int potY) {
        Image pot = createPotImage();
        if (pot == null) return;

        pot.setPosition(x, y);
        pot.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                plant(potX, potY);
            }
        });

        greenhouseGroup.addActor(pot);
    }

    private void createGrowingPot(Pot pot, float x, float y, final int potX, final int potY) {
        Image potImage = createPotImage();
        if (potImage == null) return;

        Group group = createPlantGroup(x, y);
        group.addActor(potImage);

        GreenhousePlant plant = pot.getPlant();
        if (plant != null) {
            addPlantName(group, plant.getPlantName());
            addTimer(group, plant.getExactRemainingTime());
            addPamPlant(group, plant.getPlantName());
        }

        group.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                grow(potX, potY);
            }
        });

        greenhouseGroup.addActor(group);
    }

    private void createReadyPot(Pot pot, float x, float y, final int potX, final int potY) {
        Image potImage = createPotImage();
        if (potImage == null) return;

        Group group = createPlantGroup(x, y);
        group.addActor(potImage);

        GreenhousePlant plant = pot.getPlant();
        if (plant != null) {
            addPlantName(group, plant.getPlantName());
            addPamPlant(group, plant.getPlantName());
        }

        addReadyLabel(group);

        group.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                collect(potX, potY);
            }
        });

        greenhouseGroup.addActor(group);
    }

    private Group createPlantGroup(float x, float y) {
        Group group = new Group();
        group.setSize(POT_SIZE, POT_SIZE);
        group.setPosition(x, y);
        return group;
    }

    private void addPamPlant(Group group, String plantName) {
        if (plantName == null) return;

        String key = plantName.trim().toUpperCase().replace(' ', '_');
        String pamPath = PAM_PATHS.get(key);
        if (pamPath == null) return;

        PamActor actor = new PamActor(pamPlayer, pamPath);
        actor.setSize(POT_SIZE * 1.35f, POT_SIZE * 1.35f);
        actor.setPosition(
                (POT_SIZE - actor.getWidth()) / 2f,
                2f);

        group.addActor(actor);
        actor.toFront();
    }

    private void addPlantName(Group group, String name) {
        Label label = new Label(name, skin);
        label.setColor(Color.WHITE);
        label.setFontScale(NAME_SCALE);
        label.pack();

        float width = Math.min(label.getWidth(), CELL_WIDTH - 10f);
        label.setPosition(
                (POT_SIZE - width) / 2f,
                POT_SIZE + 8f);

        group.addActor(label);
    }

    private void addTimer(Group group, String time) {
        Label label = new Label(time, skin);
        label.setColor(Color.WHITE);
        label.setFontScale(TIMER_SCALE);
        label.pack();
        label.setPosition(
                (POT_SIZE - label.getWidth()) / 2f,
                -label.getHeight() - 8f);
        group.addActor(label);
    }

    private void addReadyLabel(Group group) {
        Label label = new Label("READY", skin);
        label.setColor(Color.YELLOW);
        label.setFontScale(READY_SCALE);
        label.pack();
        label.setPosition(
                (POT_SIZE - label.getWidth()) / 2f,
                -label.getHeight() - 8f);
        group.addActor(label);
    }

    private void plant(int x, int y) {
        User user = appState.getCurrentUser();
        if (user == null) return;

        try {
            greenhouseService.plant(user, x, y);
            saveAndRefresh();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void collect(int x, int y) {
        User user = appState.getCurrentUser();
        if (user == null) return;

        try {
            greenhouseService.collect(user, x, y);
            saveAndRefresh();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void grow(int x, int y) {
        User user = appState.getCurrentUser();
        if (user == null) return;

        try {
            greenhouseService.forceGrow(user, x, y);
            saveAndRefresh();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void saveAndRefresh() {
        if (!userManager.save()) {
            userManager.reload();
            User currentUser = appState.getCurrentUser();

            if (currentUser != null) {
                User reloadedUser = userManager.find(
                        user -> user.getUsername().equals(currentUser.getUsername()));
                appState.setCurrentUser(reloadedUser);
            }
        }

        rebuildPots();
        rebuildCurrencies();
    }

    private void rebuildCurrencies() {
        for (int i = stage.getActors().size - 1; i >= 0; i--) {
            Actor actor = stage.getActors().get(i);

            if (actor instanceof Group group
                    && group != greenhouseGroup
                    && group.getWidth() > 200f
                    && group.getY() > HEIGHT - 200f) {
                group.remove();
                break;
            }
        }

        buildCurrencies();
    }

    private void showError(String message) {
        if (message == null || message.isBlank())
            message = "Greenhouse operation failed.";

        System.out.println("Greenhouse error: " + message);
    }

    @Override
    public void render(float delta) {
        refreshTimer += delta;

        if (refreshTimer >= REFRESH_INTERVAL) {
            refreshTimer = 0f;

            User user = appState.getCurrentUser();
            if (user != null && user.getGreenhouse() != null) {
                user.getGreenhouse().updateAllPots();
                rebuildPots();
            }
        }

        super.render(delta);
    }

    @Override
    public void show() {
        super.show();
        Gdx.input.setInputProcessor(stage);
    }

    private static class PamActor extends Actor {

        private final PamPlayer player;
        private final String pamPath;
        private float stateTime;

        PamActor(PamPlayer player, String pamPath) {
            this.player = player;
            this.pamPath = pamPath;
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            stateTime += delta;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float centerX = getX() + getWidth() / 2f;
            float centerY = getY() + getHeight() / 2f;
            Actor parent = getParent();

            if (parent == null) return;

            player.draw(
                    batch,
                    pamPath,
                    "idle",
                    stateTime,
                    parent.getX() + centerX,
                    parent.getY() + centerY,
                    true);
        }
    }
}
