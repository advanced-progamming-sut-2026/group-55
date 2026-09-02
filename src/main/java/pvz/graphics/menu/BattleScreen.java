package pvz.graphics.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import pvz.graphics.BaseScreen;
import pvz.graphics.PvzGame;
import pvz.graphics.actor.BattlefieldActor;
import pvz.graphics.asset.PlantVisualResolver;
import pvz.graphics.asset.ZombieVisualResolver;
import pvz.graphics.battle.BattleTickClock;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.LevelProgressService;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.entity.collectible.Collectible;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.session.BattleRewardSettlement;
import pvz.model.session.GameRuntime;
import pvz.model.session.GameSession;
import pvz.model.session.GameSessionConfig;
import pvz.model.session.GameSessionStatus;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;

/** First playable graphical battle screen. */
public final class BattleScreen extends BaseScreen {
    /*
     * Calibrated against the nine-by-five stone lawn in the Egypt background.
     * The decorative sand and machinery on either side are not plantable cells.
     */
    private static final float BOARD_X = 207f;
    private static final float BOARD_Y = 80f;
    private static final float BOARD_WIDTH = 701f;
    private static final float BOARD_HEIGHT = 452f;

    private final GameRuntime runtime;
    private final GameSessionConfig restartConfig;
    private final BattleTickClock tickClock = new BattleTickClock();
    private final BattleRewardSettlement rewardSettlement =
            new BattleRewardSettlement();
    private final LevelProgressService progressService;
    private final PlantVisualResolver plantVisuals;
    private final ZombieVisualResolver zombieVisuals;
    private final TextureRegion backgroundLeft;
    private final TextureRegion backgroundRight;
    private final Map<String, TextButton> seedButtons = new LinkedHashMap<>();

    private GameSession session;
    private BattlefieldActor battlefield;
    private Label hudLabel;
    private Label waveLabel;
    private Label statusLabel;
    private Label toolLabel;
    private Label resultTitle;
    private Label resultDetails;
    private TextButton pauseButton;
    private Table pausePanel;
    private Table resultPanel;

    private String selectedPlant;
    private BattlefieldActor.ToolMode toolMode =
            BattlefieldActor.ToolMode.PLANT;
    private boolean paused;
    private boolean resultHandled;
    private boolean rewardsSettled;
    private boolean rewardsSaved;

    public BattleScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager
    ) {
        super(
                game, textures, batch, skin, appState, userManager,
                "IMAGE_BACKGROUNDS_EGYPT_TEXTURE"
        );
        runtime = game.getGameRuntime();
        session = runtime.session();
        restartConfig = session.config();
        progressService = new LevelProgressService(
                game.getGameData().adventureData().catalog()
        );
        plantVisuals = new PlantVisualResolver(
                textures,
                Gdx.files.internal("assets")
        );
        zombieVisuals = new ZombieVisualResolver(
                Gdx.files.internal("assets")
        );
        backgroundLeft = textures.region(
                "IMAGE_BACKGROUNDS_EGYPT_TEXTURE_LEFT"
        );
        backgroundRight = textures.region(
                "IMAGE_BACKGROUNDS_EGYPT_TEXTURE_RIGHT"
        );
        GameEvents.drain();
        buildUi();
        bindBattlefield();
        updateUi();
    }

    private void buildUi() {
        pauseButton = new TextButton("PAUSE", skin, "brown");
        pauseButton.setBounds(15f, 670f, 95f, 40f);
        pauseButton.addListener(click(this::togglePause));
        stage.addActor(pauseButton);

        hudLabel = new Label("", skin);
        hudLabel.setAlignment(Align.left);
        hudLabel.setBounds(125f, 670f, 560f, 42f);
        stage.addActor(hudLabel);

        waveLabel = new Label("", skin);
        waveLabel.setAlignment(Align.right);
        waveLabel.setBounds(690f, 670f, 570f, 42f);
        stage.addActor(waveLabel);

        buildSeedBank();
        buildBottomControls();
        buildPausePanel();
        buildResultPanel();
    }

    private void buildSeedBank() {
        Table seeds = new Table();
        seeds.left();
        seeds.defaults().pad(3f);

        for (String plantName : restartConfig.selectedPlants()) {
            TextButton seed = new TextButton("", skin, "brown");
            seed.getLabel().setWrap(true);
            seed.getLabel().setAlignment(Align.center);
            seed.addListener(click(() -> selectPlant(plantName)));
            seedButtons.put(plantName, seed);
            seeds.add(seed).size(136f, 66f);
        }

        ScrollPane seedScroll = new ScrollPane(seeds, skin);
        seedScroll.setFadeScrollBars(false);
        seedScroll.setScrollingDisabled(false, true);
        seedScroll.setBounds(112f, 588f, 1148f, 74f);
        stage.addActor(seedScroll);
    }

    private void buildBottomControls() {
        TextButton shovel = new TextButton("SHOVEL", skin, "brown");
        shovel.setBounds(15f, 18f, 115f, 48f);
        shovel.addListener(click(() -> selectTool(
                BattlefieldActor.ToolMode.SHOVEL
        )));
        stage.addActor(shovel);

        TextButton plantFood = new TextButton("PLANT FOOD", skin, "brown");
        plantFood.setBounds(138f, 18f, 145f, 48f);
        plantFood.addListener(click(() -> selectTool(
                BattlefieldActor.ToolMode.PLANT_FOOD
        )));
        stage.addActor(plantFood);

        toolLabel = new Label("", skin);
        toolLabel.setAlignment(Align.center);
        toolLabel.setBounds(290f, 42f, 190f, 24f);
        stage.addActor(toolLabel);

        statusLabel = new Label("Mission started. Select a seed packet.", skin);
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);
        statusLabel.setBounds(290f, 5f, 615f, 40f);
        stage.addActor(statusLabel);

        User user = appState.getCurrentUser();
        if (user != null && user.isDebugMode()) {
            buildDebugControls();
        }
    }

    private void buildDebugControls() {
        Table debug = new Table();
        debug.defaults().pad(2f);
        debug.setBounds(910f, 10f, 355f, 58f);

        TextButton sun = new TextButton("+250 SUN", skin, "green");
        sun.addListener(click(() -> execute("cheat add -n 250 suns")));
        debug.add(sun).size(112f, 45f);

        TextButton food = new TextButton("+ FOOD", skin, "green");
        food.addListener(click(() -> execute("cheat add-plant-food")));
        debug.add(food).size(105f, 45f);

        TextButton cooldown = new TextButton("NO CD", skin, "green");
        cooldown.addListener(click(() -> execute("cheat remove-cooldown")));
        debug.add(cooldown).size(105f, 45f);
        stage.addActor(debug);
    }

    private void buildPausePanel() {
        pausePanel = new Table();
        pausePanel.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        pausePanel.setBounds(425f, 190f, 430f, 330f);
        pausePanel.defaults().pad(8f);

        Label title = new Label("GAME PAUSED", skin);
        title.setFontScale(1.45f);
        pausePanel.add(title).height(65f).row();

        TextButton resume = new TextButton("RESUME", skin, "green");
        resume.addListener(click(this::togglePause));
        pausePanel.add(resume).size(280f, 58f).row();

        TextButton restart = new TextButton("RESTART", skin, "brown");
        restart.addListener(click(this::restartBattle));
        pausePanel.add(restart).size(280f, 58f).row();

        TextButton exit = new TextButton("SAVE & EXIT", skin, "brown");
        exit.addListener(click(this::exitBattle));
        pausePanel.add(exit).size(280f, 58f);
        pausePanel.setVisible(false);
        stage.addActor(pausePanel);
    }

    private void buildResultPanel() {
        resultPanel = new Table();
        resultPanel.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        resultPanel.setBounds(380f, 170f, 520f, 370f);
        resultPanel.defaults().pad(7f);

        resultTitle = new Label("", skin);
        resultTitle.setFontScale(1.55f);
        resultTitle.setAlignment(Align.center);
        resultPanel.add(resultTitle).width(460f).height(70f).row();

        resultDetails = new Label("", skin);
        resultDetails.setAlignment(Align.center);
        resultDetails.setWrap(true);
        resultPanel.add(resultDetails).width(455f).height(110f).row();

        TextButton retry = new TextButton("RETRY", skin, "green");
        retry.addListener(click(this::restartBattle));
        resultPanel.add(retry).size(300f, 58f).row();

        TextButton exit = new TextButton("EXIT", skin, "brown");
        exit.addListener(click(this::exitBattle));
        resultPanel.add(exit).size(300f, 58f);
        resultPanel.setVisible(false);
        stage.addActor(resultPanel);
    }

    private void bindBattlefield() {
        if (battlefield != null) {
            battlefield.remove();
            battlefield.dispose();
        }
        battlefield = new BattlefieldActor(
                session,
                textures,
                skin.get(Label.LabelStyle.class).font,
                plantVisuals,
                zombieVisuals,
                game.getAnimationService(),
                new BattlefieldActor.CellListener() {
                    @Override
                    public void clicked(int column, int row) {
                        handleCellClick(column, row);
                    }

                    @Override
                    public void hovered(int column, int row) {
                        collectSunAt(column, row);
                    }
                }
        );
        battlefield.setBounds(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
        battlefield.setSelectedPlant(selectedPlant);
        battlefield.setToolMode(toolMode);
        battlefield.setShowGrid(showGrid());
        stage.getRoot().addActorAt(0, battlefield);
    }

    private void handleCellClick(int column, int row) {
        if (paused || !runtime.isActive()) {
            return;
        }

        switch (toolMode) {
            case PLANT -> {
                if (selectedPlant == null) {
                    showStatus("Select a seed packet first.", true);
                    return;
                }
                String response = execute("plant plant -t " + selectedPlant
                        + " -l (" + column + "," + row + ")");
                if (!isErrorResponse(response)) {
                    clearToolSelection();
                }
            }
            case SHOVEL -> {
                String response = execute(
                        "pluck plant -l (" + column + "," + row + ")"
                );
                if (!isErrorResponse(response)) {
                    clearToolSelection();
                }
            }
            case PLANT_FOOD -> {
                String response = execute(
                        "feed plant -l (" + column + "," + row + ")"
                );
                if (!isErrorResponse(response)) {
                    clearToolSelection();
                }
            }
        }
    }

    private void collectSunAt(int column, int row) {
        if (paused || !runtime.isActive()
                || !session.board().inBounds(column, row)) {
            return;
        }
        for (Collectible collectible : session.world().getCollectibles()) {
            if (collectible instanceof Sun sun
                    && !sun.isRemoved()
                    && sun.getTileX() == column
                    && sun.getTileY() == row) {
                execute("collect sun -l (" + column + "," + row + ")");
                return;
            }
        }
    }

    private void selectPlant(String plantName) {
        if (paused || !runtime.isActive()) {
            return;
        }
        if (toolMode == BattlefieldActor.ToolMode.PLANT
                && plantName.equals(selectedPlant)) {
            clearToolSelection();
            showStatus("Seed packet selection cancelled.", false);
            return;
        }
        selectedPlant = plantName;
        toolMode = BattlefieldActor.ToolMode.PLANT;
        battlefield.setSelectedPlant(plantName);
        battlefield.setToolMode(toolMode);
        showStatus(plantName + " selected.", false);
        updateUi();
    }

    private void selectTool(BattlefieldActor.ToolMode mode) {
        if (paused || !runtime.isActive()) {
            return;
        }
        if (toolMode == mode && selectedPlant == null) {
            clearToolSelection();
            showStatus("Tool selection cancelled.", false);
            return;
        }
        selectedPlant = null;
        toolMode = mode;
        battlefield.setSelectedPlant(null);
        battlefield.setToolMode(mode);
        showStatus(switch (mode) {
            case PLANT -> "Planting mode selected.";
            case SHOVEL -> "Click a planted tile to remove its top plant.";
            case PLANT_FOOD -> "Click a plant to use Plant Food.";
        }, false);
        updateUi();
    }

    private String execute(String command) {
        if (!runtime.isActive()) {
            return "Battle is not active.";
        }
        String response = runtime.handle(command);
        showStatus(response, isErrorResponse(response));
        updateUi();
        return response;
    }

    private void clearToolSelection() {
        selectedPlant = null;
        toolMode = BattlefieldActor.ToolMode.PLANT;
        if (battlefield != null) {
            battlefield.setSelectedPlant(null);
            battlefield.setToolMode(toolMode);
        }
        updateUi();
    }

    private void advanceBattle(float delta) {
        if (!runtime.isActive() || resultHandled) {
            return;
        }
        int ticks = tickClock.consume(delta, gameSpeed(), paused);
        if (ticks <= 0) {
            return;
        }

        try {
            session.advance(ticks);
        } catch (RuntimeException exception) {
            paused = true;
            pausePanel.setVisible(true);
            pauseButton.setText("RESUME");
            showStatus(
                    "Battle paused after an error: " + exception.getMessage(),
                    true
            );
            return;
        }

        List<String> events = GameEvents.drain();
        if (!events.isEmpty()) {
            showStatus(events.get(events.size() - 1), false);
        }
        if (runtime.isFinished()) {
            handleFinishedBattle();
        }
    }

    private void handleFinishedBattle() {
        if (resultHandled) {
            return;
        }
        resultHandled = true;
        paused = true;
        settleRewardsAndProgress();

        GameSessionStatus status = runtime.status();
        resultTitle.setText(status == GameSessionStatus.WON
                ? "LEVEL COMPLETE"
                : "ZOMBIES ATE YOUR BRAINS");
        resultTitle.setColor(status == GameSessionStatus.WON
                ? Color.GREEN
                : Color.RED);
        resultDetails.setText(
                "Level: " + restartConfig.levelId()
                        + "\nTick: " + session.game().getCurrentTick()
                        + "\nCoins: "
                        + session.battleWallet().getCollectedCoins()
                        + "   Diamonds: "
                        + session.battleWallet().getCollectedDiamonds()
                        + (rewardsSaved
                        ? "\nProgress and battle rewards were saved."
                        : "\nBattle ended, but saving needs attention.")
        );
        pausePanel.setVisible(false);
        resultPanel.setVisible(true);
        pauseButton.setDisabled(true);
    }

    private void settleRewardsAndProgress() {
        if (rewardsSettled) {
            return;
        }
        User user = appState.getCurrentUser();
        if (user == null) {
            showStatus("No logged-in user; rewards were not saved.", true);
            return;
        }

        try {
            if (runtime.status() == GameSessionStatus.WON) {
                progressService.completeLevel(user, restartConfig.levelId());
            }
            rewardSettlement.settle(session.resources(), user);
            rewardsSettled = true;
            if (!userManager.save()) {
                showStatus("Could not save battle rewards.", true);
                return;
            }
            rewardsSaved = true;
        } catch (ArithmeticException | IllegalStateException exception) {
            showStatus("Could not settle battle: " + exception.getMessage(), true);
        }
    }

    private void togglePause() {
        if (resultHandled || !runtime.isActive()) {
            return;
        }
        paused = !paused;
        pausePanel.setVisible(paused);
        pauseButton.setText(paused ? "RESUME" : "PAUSE");
        if (!paused) {
            tickClock.reset();
        }
    }

    private void restartBattle() {
        if (runtime.isActive()) {
            runtime.abort();
        }
        if (runtime.isFinished()) {
            runtime.clear();
        }

        if (rewardsSettled) {
            User user = appState.getCurrentUser();
            if (user != null) {
                user.clearPlantFood();
                userManager.save();
            }
        }

        startRuntime();
        session = runtime.session();
        selectedPlant = null;
        toolMode = BattlefieldActor.ToolMode.PLANT;
        paused = false;
        resultHandled = false;
        rewardsSettled = false;
        rewardsSaved = false;
        tickClock.reset();
        GameEvents.drain();
        pausePanel.setVisible(false);
        resultPanel.setVisible(false);
        pauseButton.setDisabled(false);
        pauseButton.setText("PAUSE");
        bindBattlefield();
        showStatus("Level restarted.", false);
        updateUi();
    }

    private void exitBattle() {
        if (runtime.isActive()) {
            runtime.abort();
        }
        if (runtime.isFinished() && !rewardsSettled) {
            settleRewardsAndProgress();
        }
        if (rewardsSettled && !rewardsSaved) {
            rewardsSaved = userManager.save();
        }
        if (runtime.isFinished()) {
            runtime.clear();
        }
        appState.setSelectedLevelId(null);
        game.setScreen(new GameMenuScreen(
                game, textures, batch, skin, appState, userManager
        ));
    }

    private void updateUi() {
        hudLabel.setText(
                restartConfig.levelId().toUpperCase(Locale.ROOT)
                        + "   SUN " + session.resources().sunBank().getBalance()
                        + "   PLANT FOOD "
                        + session.resources().getPlantFoodCount()
        );
        waveLabel.setText(
                "WAVE " + session.waveManager().getCurrentWaveNumber()
                        + "/" + session.waveManager().getTotalWaves()
                        + "   " + session.waveManager().getState()
                        + "   TICK " + session.game().getCurrentTick()
                        + "   SPEED x" + gameSpeed()
        );
        if (toolMode == BattlefieldActor.ToolMode.PLANT
                && selectedPlant == null) {
            toolLabel.setText("MODE: NONE");
        } else if (toolMode == BattlefieldActor.ToolMode.PLANT) {
            toolLabel.setText("PLANT: " + selectedPlant.toUpperCase(Locale.ROOT));
        } else {
            toolLabel.setText("MODE: " + toolMode.name().replace('_', ' '));
        }
        battlefield.setShowGrid(showGrid());
        battlefield.setSelectedPlant(selectedPlant);
        updateSeedButtons();
    }

    private void updateSeedButtons() {
        for (Map.Entry<String, TextButton> entry : seedButtons.entrySet()) {
            String plantName = entry.getKey();
            TextButton button = entry.getValue();
            PlantSpec spec = effectiveSpec(plantName);
            if (spec == null) {
                button.setText(plantName + "\nDATA MISSING");
                continue;
            }

            long rechargeTicks = (long) Math.ceil(
                    spec.getRecharge() * Game.TICKS_PER_SECOND
            );
            long remaining = session.getRemainingRechargeTicks(
                    plantName,
                    rechargeTicks
            );
            String readiness = remaining == 0
                    ? "READY"
                    : "CD " + String.format(
                            Locale.ROOT,
                            "%.1fs",
                            remaining / (double) Game.TICKS_PER_SECOND
                    );
            button.setText(
                    plantName.toUpperCase(Locale.ROOT)
                            + "\n" + spec.getCost() + " SUN   " + readiness
            );
            button.setColor(plantName.equals(selectedPlant)
                    && toolMode == BattlefieldActor.ToolMode.PLANT
                    ? Color.GOLD
                    : Color.WHITE);
        }
    }

    private PlantSpec effectiveSpec(String plantName) {
        PlantSpec base = game.getGameData().plantData().byName().get(
                plantName.toLowerCase(Locale.ROOT)
        );
        if (base == null) {
            return null;
        }
        return base.withLevel(
                restartConfig.plantLevels().getOrDefault(
                        plantName.toLowerCase(Locale.ROOT),
                        1
                )
        );
    }

    private void startRuntime() {
        User user = appState.getCurrentUser();
        runtime.start(
                restartConfig,
                zombieSpec -> {
                    if (user != null
                            && user.addSeenZombie(zombieSpec.getId())) {
                        userManager.save();
                    }
                }
        );
    }

    private int gameSpeed() {
        User user = appState.getCurrentUser();
        return user == null ? 1 : Math.max(1, Math.min(3, user.getGameSpeed()));
    }

    private boolean showGrid() {
        User user = appState.getCurrentUser();
        return user == null || user.isShowGrid();
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message == null ? "" : message);
        statusLabel.setColor(error ? Color.RED : Color.WHITE);
    }

    private static boolean isErrorResponse(String response) {
        String value = response == null
                ? ""
                : response.toLowerCase(Locale.ROOT);
        return value.contains("not enough")
                || value.contains("not selected")
                || value.contains("unknown")
                || value.contains("invalid")
                || value.contains("out of bounds")
                || value.contains("there is no")
                || value.contains("cannot")
                || value.contains("recharging")
                || value.contains("occupied")
                || value.contains("not implemented")
                || value.contains("storage is full");
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
    protected void renderBackground() {
        if (backgroundLeft == null || backgroundRight == null) {
            super.renderBackground();
            return;
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        float sourceHeight = background.getRegionHeight();
        float scale = HEIGHT / sourceHeight;
        float leftWidth = backgroundLeft.getRegionWidth() * scale;
        float centerWidth = background.getRegionWidth() * scale;
        float rightWidth = backgroundRight.getRegionWidth() * scale;
        float startX = (WIDTH - leftWidth - centerWidth - rightWidth) / 2f;

        batch.begin();
        batch.draw(backgroundLeft, startX, 0f, leftWidth, HEIGHT);
        batch.draw(background, startX + leftWidth, 0f, centerWidth, HEIGHT);
        batch.draw(backgroundRight, startX + leftWidth + centerWidth,
                0f, rightWidth, HEIGHT);
        batch.end();
    }

    @Override
    public void render(float delta) {
        if (!resultHandled
                && (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
            togglePause();
        }

        advanceBattle(delta);
        updateUi();
        renderBackground();
        renderStage(paused ? 0f : delta);
    }

    @Override
    public void show() {
        super.show();
        appState.setCurrentMenu(MenuName.PLAYING);
    }

    @Override
    public void dispose() {
        if (battlefield != null) {
            battlefield.dispose();
            battlefield = null;
        }
        super.dispose();
    }
}
