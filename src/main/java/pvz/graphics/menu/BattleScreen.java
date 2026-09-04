package pvz.graphics.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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
import pvz.graphics.actor.BattleSeedPacketActor;
import pvz.graphics.actor.BattleToolButtonActor;
import pvz.graphics.actor.BattlefieldActor;
import pvz.graphics.asset.PlantVisualResolver;
import pvz.graphics.asset.ZombieVisualResolver;
import pvz.graphics.battle.BattleTickClock;
import pvz.graphics.battle.BattleToolState;
import pvz.graphics.battle.SeedPacketState;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.ChapterSpec;
import pvz.model.adventure.LevelProgressService;
import pvz.model.adventure.LevelSpec;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.sun.SunCollectionOutcome;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.session.BattleOutcomeSettlement;
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
    private final BattleOutcomeSettlement outcomeSettlement;
    private final PlantVisualResolver plantVisuals;
    private final ZombieVisualResolver zombieVisuals;
    private final TextureRegion backgroundLeft;
    private final TextureRegion backgroundRight;
    private final Map<String, BattleSeedPacketActor> seedPackets =
            new LinkedHashMap<>();

    private GameSession session;
    private BattlefieldActor battlefield;
    private Label hudLabel;
    private Label waveLabel;
    private Label statusLabel;
    private Label toolLabel;
    private Label resultTitle;
    private Label resultDetails;
    private TextButton pauseButton;
    private TextButton resultRetryButton;
    private BattleToolButtonActor shovelButton;
    private BattleToolButtonActor plantFoodButton;
    private Actor pauseInputBlocker;
    private Table pausePanel;
    private Table resultPanel;

    private String selectedPlant;
    private BattlefieldActor.ToolMode toolMode =
            BattlefieldActor.ToolMode.PLANT;
    private boolean paused;
    private boolean resultHandled;
    private boolean rewardsSettled;
    private boolean rewardsSaved;
    private BattleOutcomeSettlement.Result settlementResult;
    private boolean disposed;

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
        outcomeSettlement = new BattleOutcomeSettlement(
                new LevelProgressService(
                        game.getGameData().adventureData().catalog()
                )
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
        addHudBackdrop(8f, 666f, 1264f, 48f, 0.82f);

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

        addHudBackdrop(105f, 576f, 1162f, 88f, 0.76f);
        buildSeedBank();
        addHudBackdrop(8f, 5f, 1264f, 70f, 0.82f);
        buildBottomControls();
        buildPauseInputBlocker();
        buildPausePanel();
        buildResultPanel();
    }

    private void addHudBackdrop(
            float x,
            float y,
            float width,
            float height,
            float alpha
    ) {
        Table backdrop = new Table();
        backdrop.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        backdrop.setColor(1f, 1f, 1f, alpha);
        backdrop.setBounds(x, y, width, height);
        backdrop.setTouchable(
                com.badlogic.gdx.scenes.scene2d.Touchable.disabled
        );
        stage.addActor(backdrop);
    }

    private void buildSeedBank() {
        Table seeds = new Table();
        seeds.left();
        seeds.defaults().pad(3f);

        for (String plantName : restartConfig.selectedPlants()) {
            BattleSeedPacketActor packet = new BattleSeedPacketActor(
                    skin,
                    plantName,
                    plantVisuals.preview(plantName),
                    () -> selectPlant(plantName)
            );
            seedPackets.put(plantName, packet);
            seeds.add(packet).size(
                    BattleSeedPacketActor.PACKET_WIDTH,
                    BattleSeedPacketActor.PACKET_HEIGHT
            );
        }

        ScrollPane seedScroll = new ScrollPane(seeds, skin);
        seedScroll.setFadeScrollBars(false);
        seedScroll.setScrollingDisabled(false, true);
        seedScroll.setBounds(112f, 584f, 1148f, 78f);
        stage.addActor(seedScroll);
    }

    private void buildBottomControls() {
        shovelButton = new BattleToolButtonActor(
                skin,
                BattleToolButtonActor.IconType.SHOVEL,
                "SHOVEL",
                () -> selectTool(BattlefieldActor.ToolMode.SHOVEL)
        );
        shovelButton.setBounds(15f, 12f, 128f, 58f);
        stage.addActor(shovelButton);

        plantFoodButton = new BattleToolButtonActor(
                skin,
                BattleToolButtonActor.IconType.PLANT_FOOD,
                "PLANT FOOD",
                () -> selectTool(BattlefieldActor.ToolMode.PLANT_FOOD)
        );
        plantFoodButton.setBounds(151f, 12f, 158f, 58f);
        stage.addActor(plantFoodButton);

        toolLabel = new Label("", skin);
        toolLabel.setAlignment(Align.left);
        toolLabel.setFontScale(0.78f);
        toolLabel.setBounds(320f, 43f, 360f, 24f);
        stage.addActor(toolLabel);

        statusLabel = new Label("Mission started. Select a seed packet.", skin);
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);
        statusLabel.setBounds(320f, 5f, 585f, 38f);
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

    /** Blocks every gameplay control while leaving the top pause button free. */
    private void buildPauseInputBlocker() {
        pauseInputBlocker = new Actor();
        pauseInputBlocker.setBounds(0f, 0f, WIDTH, 666f);
        pauseInputBlocker.setTouchable(Touchable.enabled);
        pauseInputBlocker.addListener(new InputListener() {
            @Override
            public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
            ) {
                event.stop();
                return true;
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                event.stop();
                return true;
            }
        });
        pauseInputBlocker.setVisible(false);
        stage.addActor(pauseInputBlocker);
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
        resultPanel.setBounds(380f, 135f, 520f, 450f);
        resultPanel.defaults().pad(7f);

        resultTitle = new Label("", skin);
        resultTitle.setFontScale(1.55f);
        resultTitle.setAlignment(Align.center);
        resultPanel.add(resultTitle).width(460f).height(70f).row();

        resultDetails = new Label("", skin);
        resultDetails.setAlignment(Align.center);
        resultDetails.setWrap(true);
        resultPanel.add(resultDetails).width(455f).height(190f).row();

        resultRetryButton = new TextButton("RETRY", skin, "green");
        resultRetryButton.addListener(click(this::restartBattle));
        resultPanel.add(resultRetryButton).size(300f, 58f).row();

        TextButton exit = new TextButton("EXIT", skin, "brown");
        exit.addListener(click(this::exitFinishedBattle));
        resultPanel.add(exit).size(300f, 58f);
        resultPanel.setVisible(false);
        stage.addActor(resultPanel);
    }

    private void bindBattlefield() {
        disposeBattlefield();
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
                    public SunCollectionOutcome collectSun(
                            Sun sun,
                            int collectionColumn,
                            int collectionRow
                    ) {
                        return collectSunFromBattlefield(
                                sun,
                                collectionColumn,
                                collectionRow
                        );
                    }
                }
        );
        battlefield.setBounds(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
        battlefield.setSelectedPlant(selectedPlant);
        battlefield.setToolMode(toolMode);
        battlefield.setShowGrid(showGrid());
        battlefield.setPaused(paused);
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

    private SunCollectionOutcome collectSunFromBattlefield(
            Sun sun,
            int collectionColumn,
            int collectionRow
    ) {
        if (paused || !runtime.isActive() || sun == null || sun.isRemoved()) {
            return null;
        }

        int value = sun.getValue();
        SunCollectionOutcome outcome;
        try {
            outcome = session.world().collectSun(
                    sun,
                    collectionColumn,
                    collectionRow
            );
        } catch (IllegalStateException | IllegalArgumentException exception) {
            return null;
        }

        showSunCollectionStatus(
                outcome,
                value,
                collectionColumn,
                collectionRow
        );
        handleFinishedBattleIfNeeded();
        updateUi();
        return outcome;
    }

    private void showSunCollectionStatus(
            SunCollectionOutcome outcome,
            int value,
            int column,
            int row
    ) {
        if (outcome == SunCollectionOutcome.EXPLODED) {
            showStatus(
                    "Radioactive sun exploded at (" + column + ", " + row
                            + "); no sun was added.",
                    false
            );
            return;
        }
        showStatus(
                "Collected " + value + " sun; you now have "
                        + session.resources().sunBank().getBalance()
                        + " sun.",
                false
        );
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
        if (mode == BattlefieldActor.ToolMode.PLANT_FOOD
                && session.resources().getPlantFoodCount() <= 0) {
            showStatus("You do not have any Plant Food.", true);
            updateUi();
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
        handleFinishedBattleIfNeeded();
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
        if (resultHandled) {
            return;
        }
        if (handleFinishedBattleIfNeeded() || !runtime.isActive()) {
            return;
        }

        int ticks = tickClock.consume(delta, gameSpeed(), paused);
        if (ticks <= 0) {
            return;
        }

        try {
            session.advance(ticks);
        } catch (RuntimeException exception) {
            setBattlePaused(true);
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
        handleFinishedBattleIfNeeded();
    }

    private boolean handleFinishedBattleIfNeeded() {
        if (resultHandled || !runtime.isFinished()) {
            return resultHandled;
        }

        GameSessionStatus status = runtime.status();
        if (status != GameSessionStatus.WON
                && status != GameSessionStatus.LOST) {
            return false;
        }

        resultHandled = true;
        setBattlePaused(true);

        boolean saved = settleRewardsAndProgress(
                status == GameSessionStatus.WON
        );

        configureResultPanel(status);
        pausePanel.setVisible(false);
        resultPanel.setVisible(true);
        resultPanel.toFront();
        pauseButton.setText("ENDED");
        pauseButton.setDisabled(true);

        if (status == GameSessionStatus.WON) {
            showStatus(
                    saved
                            ? "Level complete. Rewards and progress saved."
                            : "Level complete, but saving needs attention.",
                    !saved
            );
        } else {
            showStatus(
                    saved
                            ? "Battle lost. Collected rewards saved."
                            : "Battle lost, but saving needs attention.",
                    true
            );
        }
        return true;
    }

    private void configureResultPanel(GameSessionStatus status) {
        boolean won = status == GameSessionStatus.WON;
        resultTitle.setText(won
                ? "LEVEL COMPLETE"
                : "ZOMBIES ATE YOUR BRAINS");
        resultTitle.setColor(won ? Color.GREEN : Color.RED);

        StringBuilder details = new StringBuilder();
        details.append(won
                ? "All waves were cleared."
                : "A zombie broke through the final defense.");
        details.append("\nLevel: ").append(restartConfig.levelId());
        details.append("\nTime: ").append(formatBattleTime());
        details.append("   Wave: ")
                .append(session.waveManager().getCurrentWaveNumber())
                .append('/')
                .append(session.waveManager().getTotalWaves());
        details.append("\nCoins: +")
                .append(session.battleWallet().getCollectedCoins());
        details.append("   Diamonds: +")
                .append(session.battleWallet().getCollectedDiamonds());

        if (won) {
            appendWinSettlementDetails(details);
        } else if (rewardsSettled) {
            details.append(rewardsSaved
                    ? "\nCoins and Diamonds saved for this attempt."
                    : "\nRewards are settled; saving is still pending.");
            details.append(
                    "\nRETRY starts a fresh attempt. EXIT returns "
                            + "remaining Plant Food before leaving."
            );
        } else {
            details.append("\nRewards were not settled; retry is disabled.");
            details.append("\nUse EXIT to retry settlement and saving.");
        }

        resultDetails.setText(details.toString());
        boolean retryAllowed = !won && rewardsSettled;
        resultRetryButton.setVisible(retryAllowed);
        resultRetryButton.setDisabled(!retryAllowed);
    }

    private void appendWinSettlementDetails(StringBuilder details) {
        if (!rewardsSettled || settlementResult == null) {
            details.append("\nRewards were not settled.");
            return;
        }

        details.append(settlementResult.newlyCompleted()
                ? "\nProgress: first clear recorded."
                : "\nProgress: level was already completed.");

        if (settlementResult.unlockedChapterId() != null) {
            details.append(" Unlocked chapter: ")
                    .append(settlementResult.unlockedChapterId())
                    .append('.');
        } else if (settlementResult.unlockedLevelId() != null) {
            details.append(" Unlocked level: ")
                    .append(settlementResult.unlockedLevelId())
                    .append('.');
        }

        details.append(rewardsSaved
                ? "\nRewards and progress saved."
                : "\nSave pending; use EXIT to retry saving.");
    }

    private String formatBattleTime() {
        return String.format(
                Locale.ROOT,
                "%.1fs",
                session.game().getElapsedSeconds()
        );
    }

    private boolean settleRewardsAndProgress(boolean returnPlantFood) {
        User user = appState.getCurrentUser();
        if (user == null) {
            showStatus("No logged-in user; rewards were not saved.", true);
            return false;
        }

        if (!rewardsSettled) {
            try {
                settlementResult = outcomeSettlement.settle(
                        runtime.status(),
                        restartConfig.levelId(),
                        session.resources(),
                        user,
                        returnPlantFood
                );
                rewardsSettled = true;
            } catch (ArithmeticException
                    | IllegalArgumentException
                    | IllegalStateException exception) {
                showStatus(
                        "Could not settle battle: " + exception.getMessage(),
                        true
                );
                return false;
            }
        } else if (returnPlantFood
                && !session.resources().isPlantFoodReturned()) {
            try {
                int returned = outcomeSettlement.returnRemainingPlantFood(
                        session.resources(),
                        user
                );
                if (returned > 0) {
                    rewardsSaved = false;
                }
            } catch (IllegalStateException exception) {
                showStatus(
                        "Could not return Plant Food: "
                                + exception.getMessage(),
                        true
                );
                return false;
            }
        }

        return saveSettledRewards();
    }

    private boolean saveSettledRewards() {
        if (!rewardsSettled) {
            return false;
        }
        if (rewardsSaved) {
            return true;
        }

        rewardsSaved = userManager.save();
        if (!rewardsSaved) {
            showStatus("Could not save battle rewards.", true);
        }
        return rewardsSaved;
    }

    private void togglePause() {
        if (resultHandled || !runtime.isActive()) {
            return;
        }
        setBattlePaused(!paused);
    }

    private void restartBattle() {
        if (rewardsSettled) {
            if (session.resources().isPlantFoodReturned()) {
                showStatus(
                        "This attempt has already returned persistent "
                                + "resources; exit instead of retrying.",
                        true
                );
                return;
            }
            if (!saveSettledRewards()) {
                showStatus(
                        "Retry is blocked until battle rewards are saved.",
                        true
                );
                return;
            }
        }

        stage.cancelTouchFocus();
        if (battlefield != null) {
            battlefield.setPaused(true);
        }

        try {
            restartRuntime();
        } catch (RuntimeException exception) {
            setBattlePaused(true);
            showStatus(
                    "Could not restart level: " + exception.getMessage(),
                    true
            );
            return;
        }
        session = runtime.session();
        selectedPlant = null;
        toolMode = BattlefieldActor.ToolMode.PLANT;
        resultHandled = false;
        rewardsSettled = false;
        rewardsSaved = false;
        settlementResult = null;
        tickClock.reset();
        GameEvents.drain();
        pausePanel.setVisible(false);
        resultPanel.setVisible(false);
        resultRetryButton.setVisible(true);
        resultRetryButton.setDisabled(false);
        pauseButton.setDisabled(false);
        pauseButton.setText("PAUSE");
        bindBattlefield();
        setBattlePaused(false);
        showStatus("Level restarted.", false);
        updateUi();
    }

    private void exitBattle() {
        if (runtime.isActive()) {
            runtime.abort();
        }
        if (!runtime.isFinished()) {
            return;
        }
        if (!settleRewardsAndProgress(true)) {
            return;
        }

        runtime.clear();
        appState.setSelectedLevelId(null);
        game.setScreen(new GameMenuScreen(
                game, textures, batch, skin, appState, userManager
        ));
    }

    private void exitFinishedBattle() {
        if (!resultHandled || !runtime.isFinished()) {
            return;
        }
        if (!settleRewardsAndProgress(true)) {
            configureResultPanel(runtime.status());
            return;
        }

        runtime.clear();

        LevelSpec level = game.getGameData()
                .adventureData()
                .catalog()
                .requireLevel(restartConfig.levelId());
        ChapterSpec chapter = game.getGameData()
                .adventureData()
                .catalog()
                .findChapter(level.chapterId());
        if (chapter == null) {
            throw new IllegalStateException(
                    "missing chapter for level: " + level.id()
            );
        }

        appState.setSelectedChapter(chapter.id());
        appState.setSelectedLevelId(null);
        game.setScreen(new LevelSelectionScreen(
                game, textures, batch, skin, appState, userManager, chapter
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
        updateToolControls();
        battlefield.setShowGrid(showGrid());
        battlefield.setSelectedPlant(selectedPlant);
        updateSeedPackets();
    }

    private void updateToolControls() {
        BattleToolState.View view = BattleToolState.resolve(
                currentToolSelection(),
                selectedPlant,
                session.resources().getPlantFoodCount()
        );
        toolLabel.setText(view.selectionText());
        shovelButton.update(view.shovel());
        plantFoodButton.update(view.plantFood());
    }

    private BattleToolState.Selection currentToolSelection() {
        if (toolMode == BattlefieldActor.ToolMode.PLANT) {
            return selectedPlant == null
                    ? BattleToolState.Selection.NONE
                    : BattleToolState.Selection.PLANT;
        }
        return toolMode == BattlefieldActor.ToolMode.SHOVEL
                ? BattleToolState.Selection.SHOVEL
                : BattleToolState.Selection.PLANT_FOOD;
    }

    private void updateSeedPackets() {
        for (Map.Entry<String, BattleSeedPacketActor> entry
                : seedPackets.entrySet()) {
            String plantName = entry.getKey();
            BattleSeedPacketActor packet = entry.getValue();
            PlantSpec spec = effectiveSpec(plantName);
            if (spec == null) {
                packet.update(
                        0,
                        new SeedPacketState.View(
                                SeedPacketState.Availability.UNAVAILABLE,
                                "DATA MISSING"
                        )
                );
                continue;
            }

            long rechargeTicks = (long) Math.ceil(
                    spec.getRecharge() * Game.TICKS_PER_SECOND
            );
            long remaining = session.getRemainingRechargeTicks(
                    plantName,
                    rechargeTicks
            );
            SeedPacketState.View state = SeedPacketState.resolve(
                    plantName.equals(selectedPlant)
                            && toolMode == BattlefieldActor.ToolMode.PLANT,
                    spec.getCost(),
                    session.resources().sunBank().getBalance(),
                    remaining,
                    Game.TICKS_PER_SECOND
            );
            packet.update(spec.getCost(), state);
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

    private void restartRuntime() {
        User user = appState.getCurrentUser();
        runtime.restart(
                restartConfig,
                zombieSpec -> {
                    if (user != null
                            && user.addSeenZombie(zombieSpec.getId())) {
                        userManager.save();
                    }
                }
        );
    }

    private void setBattlePaused(boolean shouldPause) {
        paused = shouldPause;
        if (shouldPause) {
            stage.cancelTouchFocus();
            stage.setScrollFocus(null);
            stage.setKeyboardFocus(null);
        } else {
            tickClock.reset();
        }
        if (pauseInputBlocker != null) {
            pauseInputBlocker.setVisible(shouldPause);
        }
        if (battlefield != null) {
            battlefield.setPaused(shouldPause);
        }
        pausePanel.setVisible(shouldPause && !resultHandled);
        pauseButton.setText(shouldPause ? "RESUME" : "PAUSE");
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
        handleFinishedBattleIfNeeded();
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
    public void pause() {
        if (!resultHandled && runtime.isActive()) {
            setBattlePaused(true);
        }
    }

    @Override
    public void resume() {
        tickClock.reset();
    }

    private void returnPlantFoodBeforeTerminalDispose() {
        if (!resultHandled
                || !session.isFinished()
                || !rewardsSettled
                || session.resources().isPlantFoodReturned()) {
            return;
        }

        User user = appState.getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            int returned = outcomeSettlement.returnRemainingPlantFood(
                    session.resources(),
                    user
            );
            if (returned > 0) {
                rewardsSaved = false;
            }
        } catch (IllegalStateException ignored) {
            // A final save retry below is still safe for already-settled data.
        }
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;

        returnPlantFoodBeforeTerminalDispose();
        if (rewardsSettled && !rewardsSaved) {
            rewardsSaved = userManager.save();
        }

        stage.cancelTouchFocus();
        stage.setScrollFocus(null);
        stage.setKeyboardFocus(null);

        disposeBattlefield();
        disposeSeedPackets();
        disposeToolButtons();
        clearActorCallbacks(stage.getRoot());
        clearActorReferences();
        super.dispose();
    }

    private void disposeBattlefield() {
        if (battlefield == null) {
            return;
        }
        BattlefieldActor oldBattlefield = battlefield;
        battlefield = null;
        oldBattlefield.dispose();
    }

    private void disposeSeedPackets() {
        for (BattleSeedPacketActor packet : seedPackets.values()) {
            packet.dispose();
        }
        seedPackets.clear();
    }

    private void disposeToolButtons() {
        if (shovelButton != null) {
            shovelButton.dispose();
            shovelButton = null;
        }
        if (plantFoodButton != null) {
            plantFoodButton.dispose();
            plantFoodButton = null;
        }
    }

    /**
     * Severs screen-capturing callbacks from every remaining stage actor.
     * Drawables and TextureRegions are deliberately left alone because they
     * are owned by the shared Skin/TextureBank rather than this battle.
     */
    private static void clearActorCallbacks(Actor actor) {
        actor.clearActions();
        actor.clearListeners();
        if (actor instanceof Group group) {
            for (Actor child : group.getChildren()) {
                clearActorCallbacks(child);
            }
        }
    }

    private void clearActorReferences() {
        pauseInputBlocker = null;
        pausePanel = null;
        resultPanel = null;
        pauseButton = null;
        resultRetryButton = null;
        hudLabel = null;
        waveLabel = null;
        statusLabel = null;
        toolLabel = null;
        resultTitle = null;
        resultDetails = null;
        selectedPlant = null;
    }
}
