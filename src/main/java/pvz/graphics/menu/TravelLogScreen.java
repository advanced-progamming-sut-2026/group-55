package pvz.graphics.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import pvz.graphics.BaseScreen;
import pvz.graphics.PvzGame;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.ChapterSpec;
import pvz.model.adventure.LevelCatalog;
import pvz.model.adventure.LevelSpec;
import pvz.model.minigame.MinigameCatalog;
import pvz.model.minigame.MinigameSpec;
import pvz.model.minigame.MinigameProgressService;
import pvz.model.minigame.MinigameStageRoute;
import pvz.model.minigame.MinigameStageState;
import pvz.model.quest.QuestCatalog;
import pvz.model.quest.QuestCategory;
import pvz.model.quest.QuestProgress;
import pvz.model.quest.QuestResetPolicy;
import pvz.model.quest.QuestReward;
import pvz.model.quest.QuestRewardType;
import pvz.model.quest.QuestSpec;
import pvz.model.quest.QuestState;
import pvz.model.service.QuestService;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;

/** Graphical Travel Log backed by the persistent quest system. */
public final class TravelLogScreen extends BaseScreen {
    private static final float PANEL_X = 80f;
    private static final float PANEL_Y = 82f;
    private static final float PANEL_WIDTH = 1120f;
    private static final float PANEL_HEIGHT = 465f;
    private static final float CARD_WIDTH = 1045f;
    private static final float CARD_HEIGHT = 142f;
    private static final float PROGRESS_WIDTH = 225f;
    private static final float PROGRESS_HEIGHT = 12f;
    private static final float TAB_Y = 565f;
    private static final float TAB_WIDTH = 235f;
    private static final float TAB_HEIGHT = 46f;
    private static final float TAB_GAP = 10f;
    private static final QuestCategory[] TAB_ORDER = {
            QuestCategory.ADVENTURE,
            QuestCategory.DAILY,
            QuestCategory.CHALLENGE,
            QuestCategory.MINIGAME
    };

    private static final Color TRACK_COLOR =
            new Color(0.16f, 0.18f, 0.16f, 0.88f);
    private static final Color FILL_COLOR =
            new Color(0.20f, 0.72f, 0.12f, 1f);

    private final QuestCatalog questCatalog;
    private final MinigameCatalog minigameCatalog;
    private final MinigameProgressService minigameProgressService;
    private final QuestService questService;
    private final Map<QuestCategory, TextButton> categoryButtons =
            new EnumMap<>(QuestCategory.class);
    private final Table questTable = new Table();

    private ScrollPane questScroll;
    private Label coinLabel;
    private Label diamondLabel;
    private Label statusLabel;

    private final Texture progressTrackTexture;
    private final Texture progressFillTexture;
    private QuestCategory selectedCategory = QuestCategory.ADVENTURE;

    public TravelLogScreen(
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
                QuestCategory.ADVENTURE
        );
    }

    public TravelLogScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager,
            QuestCategory initialCategory
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
        this.questCatalog = game.getGameData().questCatalog();
        this.minigameCatalog = game.getGameData().minigameCatalog();
        this.minigameProgressService =
                game.getGameData().minigameProgressService();
        this.questService = game.getQuestService();
        this.selectedCategory = initialCategory == null
                ? QuestCategory.ADVENTURE
                : initialCategory;
        this.progressTrackTexture = createSolidTexture(TRACK_COLOR);
        this.progressFillTexture = createSolidTexture(FILL_COLOR);

        buildUi();
    }

    private void buildUi() {
        buildHeader();
        buildCategoryTabs();
        buildQuestPanel();
        buildStatusBar();
    }

    private void buildHeader() {
        TextButton back = new TextButton("BACK", skin, "brown");
        back.setBounds(25f, HEIGHT - 72f, 125f, 48f);
        back.addListener(click(this::goBack));
        stage.addActor(back);

        Label title = new Label("TRAVEL LOG", skin);
        title.setFontScale(1.45f);
        title.setAlignment(Align.center);
        title.setBounds(300f, HEIGHT - 70f, 600f, 48f);
        stage.addActor(title);

        diamondLabel = new Label("", skin);
        diamondLabel.setAlignment(Align.right);
        diamondLabel.setBounds(930f, HEIGHT - 69f, 150f, 44f);
        stage.addActor(diamondLabel);

        coinLabel = new Label("", skin);
        coinLabel.setAlignment(Align.right);
        coinLabel.setBounds(1080f, HEIGHT - 69f, 175f, 44f);
        stage.addActor(coinLabel);
    }

    private void buildCategoryTabs() {
        float totalWidth = TAB_ORDER.length * TAB_WIDTH
                + (TAB_ORDER.length - 1) * TAB_GAP;
        float startX = (WIDTH - totalWidth) / 2f;

        for (int index = 0; index < TAB_ORDER.length; index++) {
            QuestCategory category = TAB_ORDER[index];
            TextButton button = new TextButton(
                    categoryLabel(category),
                    skin,
                    category == selectedCategory ? "green" : "brown"
            );
            button.setBounds(
                    startX + index * (TAB_WIDTH + TAB_GAP),
                    TAB_Y,
                    TAB_WIDTH,
                    TAB_HEIGHT
            );
            button.getLabel().setFontScale(0.72f);
            button.addListener(click(() -> selectCategory(category)));
            categoryButtons.put(category, button);
            stage.addActor(button);
        }
    }

    private void selectCategory(QuestCategory category) {
        if (category == null || category == selectedCategory) {
            return;
        }
        selectedCategory = category;
        refreshCategoryButtons();
        rebuildQuestList(true);
        showStatus(categoryStatusMessage(), false);
    }

    private void refreshCategoryButtons() {
        for (Map.Entry<QuestCategory, TextButton> entry
                : categoryButtons.entrySet()) {
            String styleName = entry.getKey() == selectedCategory
                    ? "green"
                    : "brown";
            entry.getValue().setStyle(
                    skin.get(styleName, TextButton.TextButtonStyle.class)
            );
        }
    }

    private void buildQuestPanel() {
        questTable.top().left();
        questTable.defaults().padBottom(8f);

        questScroll = new ScrollPane(questTable, skin);
        questScroll.setFadeScrollBars(false);
        questScroll.setScrollingDisabled(true, false);

        Table frame = new Table();
        frame.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        frame.setBounds(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);
        frame.add(questScroll).grow().pad(16f);
        stage.addActor(frame);
    }

    private void buildStatusBar() {
        statusLabel = new Label("", skin);
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(150f, 22f, 980f, 38f);
        stage.addActor(statusLabel);
    }

    private void refreshFromModel(boolean resetScroll) {
        User user = appState.getCurrentUser();
        if (user == null) {
            refreshCurrencies();
            rebuildQuestList(resetScroll);
            showStatus("No active user is available.", true);
            return;
        }

        QuestService.SyncResult sync = questService.synchronizeAndSave(
                user,
                questCatalog.all()
        );
        if (sync.user() != null && sync.user() != user) {
            appState.setCurrentUser(sync.user());
        }

        refreshCurrencies();
        rebuildQuestList(resetScroll);

        if (!sync.saved()) {
            showStatus(
                    "Quest progress could not be saved; persisted data was reloaded.",
                    true
            );
        } else {
            showStatus(
                    categoryStatusMessage(),
                    false
            );
        }
    }

    private void refreshCurrencies() {
        User user = appState.getCurrentUser();
        if (user == null) {
            diamondLabel.setText("Gems: 0");
            coinLabel.setText("Coins: 0");
            return;
        }
        diamondLabel.setText("Gems: " + user.getDiamonds());
        coinLabel.setText("Coins: " + user.getCoins());
    }

    private void rebuildQuestList(boolean resetScroll) {
        float previousScroll = questScroll == null ? 0f : questScroll.getScrollY();
        questTable.clearChildren();

        if (selectedCategory == QuestCategory.ADVENTURE) {
            questTable.add(buildAdventureAccessCard())
                    .width(CARD_WIDTH)
                    .height(112f)
                    .padBottom(12f)
                    .row();
        }

        if (selectedCategory == QuestCategory.MINIGAME) {
            questTable.add(buildMinigameAccessHeader())
                    .width(CARD_WIDTH)
                    .height(48f)
                    .row();
            for (MinigameSpec minigame : minigameCatalog.all()) {
                questTable.add(buildMinigameAccessCard(minigame))
                        .width(CARD_WIDTH)
                        .height(126f)
                        .row();
            }
            questTable.add(sectionLabel("MINIGAME QUESTS"))
                    .width(CARD_WIDTH)
                    .height(42f)
                    .padTop(6f)
                    .left()
                    .row();
        }

        List<QuestSpec> quests = questCatalog.byCategory(selectedCategory);
        if (quests.isEmpty()) {
            Label empty = new Label(
                    "No quests are configured for this category.",
                    skin
            );
            empty.setAlignment(Align.center);
            questTable.add(empty).width(CARD_WIDTH).height(120f);
        } else {
            for (QuestSpec spec : quests) {
                questTable.add(buildQuestCard(spec))
                        .width(CARD_WIDTH)
                        .height(CARD_HEIGHT)
                        .row();
            }
        }

        questTable.invalidateHierarchy();
        if (resetScroll) {
            questScroll.setScrollY(0f);
        } else {
            questScroll.setScrollY(previousScroll);
        }
    }

    private Table buildAdventureAccessCard() {
        LevelCatalog levelCatalog = game.getGameData()
                .adventureData()
                .catalog();
        User user = appState.getCurrentUser();

        int configuredLevels = 0;
        int completedLevels = 0;
        int unlockedChapters = 0;
        for (ChapterSpec chapter : levelCatalog.chapters()) {
            List<LevelSpec> levels = levelCatalog.levelsInChapter(chapter.id());
            configuredLevels += levels.size();
            if (user != null && user.isChapterUnlocked(chapter.id())) {
                unlockedChapters++;
            }
            if (user != null) {
                completedLevels += (int) levels.stream()
                        .filter(level -> user.getAdventureProgress()
                                .isLevelCompleted(level.id()))
                        .count();
            }
        }

        Table card = new Table();
        card.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        card.pad(12f);

        Table info = new Table();
        info.left();
        Label title = new Label("ADVENTURE HUB", skin);
        title.setFontScale(1.0f);
        title.setColor(Color.YELLOW);
        info.add(title).left().row();

        Label summary = new Label(
                "Adventure progress: " + completedLevels + "/"
                        + configuredLevels
                        + "  |  Unlocked chapters: "
                        + unlockedChapters
                        + "/"
                        + levelCatalog.chapters().size(),
                skin
        );
        summary.setFontScale(0.74f);
        summary.setColor(Color.DARK_GRAY);
        info.add(summary).left().padTop(6f).row();

        Label note = new Label(
                "Phase 6 chapter and level progress will feed Adventure quests through the shared quest system.",
                skin
        );
        note.setWrap(true);
        note.setFontScale(0.68f);
        info.add(note).left().width(760f).padTop(5f);

        TextButton open = new TextButton("OPEN ADVENTURE", skin, "green");
        open.getLabel().setFontScale(0.72f);
        open.addListener(click(this::openAdventure));

        card.add(info).growX().left();
        card.add(open).width(190f).height(44f).right().padLeft(16f);
        return card;
    }

    private Table buildMinigameAccessHeader() {
        Table header = new Table();

        Label accessTitle = sectionLabel("MINIGAME ACCESS");
        TextButton open = new TextButton("OPEN MINIGAMES", skin, "green");
        open.getLabel().setFontScale(0.70f);
        open.addListener(click(this::openMinigames));

        header.add(accessTitle).growX().left();
        header.add(open).width(185f).height(40f).right();
        return header;
    }

    private Table buildMinigameAccessCard(MinigameSpec spec) {
        Table card = new Table();
        card.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        card.pad(10f);

        User user = appState.getCurrentUser();
        int completed = user == null
                ? 0
                : minigameProgressService.completedStageCount(
                        user,
                        spec.id()
                );

        Table info = new Table();
        info.left().top();
        Label name = new Label(spec.name(), skin);
        name.setFontScale(0.95f);
        name.setColor(Color.YELLOW);
        info.add(name).left().row();

        Label description = new Label(spec.description(), skin);
        description.setWrap(true);
        description.setFontScale(0.68f);
        info.add(description).left().width(555f).padTop(5f).row();

        Label phase = new Label(
                completed + " / " + spec.stageCount()
                        + " stages cleared - gameplay opens in Phase 7.",
                skin
        );
        phase.setFontScale(0.64f);
        phase.setColor(Color.GRAY);
        info.add(phase).left().padTop(5f);

        Table stages = new Table();
        stages.defaults().padLeft(5f);
        for (int stageNumber = 1;
             stageNumber <= spec.stageCount();
             stageNumber++) {
            MinigameStageRoute route = MinigameStageRoute.of(
                    spec,
                    stageNumber
            );
            MinigameStageState state = user == null
                    ? MinigameStageState.LOCKED
                    : minigameProgressService.stageState(user, route);
            TextButton stageButton = new TextButton(
                    travelLogStageText(stageNumber, state),
                    skin,
                    state == MinigameStageState.COMPLETED
                            ? "green"
                            : "brown"
            );
            stageButton.getLabel().setFontScale(0.60f);
            stageButton.getLabel().setAlignment(Align.center);
            stageButton.setDisabled(true);
            stages.add(stageButton).width(128f).height(58f);
        }

        card.add(info).width(585f).growY().left();
        card.add(stages).growX().right();
        return card;
    }

    private String travelLogStageText(
            int stageNumber,
            MinigameStageState state
    ) {
        return switch (state) {
            case COMPLETED -> "STAGE " + stageNumber + "\nCOMPLETED";
            case AVAILABLE -> "STAGE " + stageNumber + "\nAVAILABLE";
            case LOCKED -> "STAGE " + stageNumber + "\nLOCKED";
        };
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text, skin);
        label.setFontScale(0.90f);
        label.setColor(Color.WHITE);
        label.setAlignment(Align.left);
        return label;
    }

    private void openMinigames() {
        game.setScreen(new MinigamesScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager,
                MenuName.TRAVEL_LOG
        ));
    }

    private void openAdventure() {
        game.setScreen(new GameMenuScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager
        ));
    }

    private Table buildQuestCard(QuestSpec spec) {
        User user = appState.getCurrentUser();
        QuestProgress progress = user == null
                ? null
                : user.getQuestLog().find(spec.id());
        QuestState state = displayState(spec, progress);
        int value = progress == null ? 0 : progress.getValue();
        int target = spec.objective().target();

        Table card = new Table();
        card.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        card.pad(10f);

        Table info = new Table();
        info.top().left();

        Label name = new Label(spec.name(), skin);
        name.setFontScale(1.02f);
        name.setColor(priorityColor(spec.priority().name()));
        info.add(name).left().growX();

        Label meta = new Label(
                pretty(spec.category().name())
                        + "  |  "
                        + pretty(spec.priority().name()),
                skin
        );
        meta.setFontScale(0.72f);
        meta.setAlignment(Align.right);
        info.add(meta).right().width(245f).row();

        Label description = new Label(spec.description(), skin);
        description.setWrap(true);
        description.setFontScale(0.78f);
        info.add(description)
                .colspan(2)
                .left()
                .width(655f)
                .padTop(4f)
                .row();

        String rewardText = "Reward: " + formatRewards(spec.rewards());
        if (spec.resetPolicy() == QuestResetPolicy.DAILY) {
            rewardText += "  |  Resets daily";
        }
        Label rewards = new Label(rewardText, skin);
        rewards.setWrap(true);
        rewards.setFontScale(0.70f);
        rewards.setColor(Color.DARK_GRAY);
        info.add(rewards)
                .colspan(2)
                .left()
                .width(655f)
                .padTop(4f);

        Table progressArea = new Table();
        progressArea.top();

        Label stateLabel = new Label(stateText(state), skin);
        stateLabel.setAlignment(Align.center);
        stateLabel.setColor(stateColor(state));
        stateLabel.setFontScale(0.82f);
        progressArea.add(stateLabel).width(245f).height(26f).row();

        Label progressLabel = new Label(
                progressText(state, value, target),
                skin
        );
        progressLabel.setAlignment(Align.center);
        progressLabel.setFontScale(0.75f);
        progressArea.add(progressLabel)
                .width(245f)
                .height(24f)
                .padTop(2f)
                .row();

        progressArea.add(progressBar(state, value, target))
                .width(PROGRESS_WIDTH)
                .height(PROGRESS_HEIGHT)
                .padTop(3f)
                .padBottom(9f)
                .row();

        TextButton action = buildActionButton(spec, state);
        progressArea.add(action).width(155f).height(38f);

        card.add(info).width(710f).growY().left();
        card.add(progressArea).width(275f).growY().right();
        return card;
    }

    private TextButton buildActionButton(QuestSpec spec, QuestState state) {
        boolean claimable = state == QuestState.COMPLETED;
        String text = switch (state) {
            case COMPLETED -> "CLAIM";
            case CLAIMED -> "CLAIMED";
            case UNAVAILABLE -> "FUTURE CONTENT";
            case AVAILABLE -> "IN PROGRESS";
        };

        TextButton button = new TextButton(
                text,
                skin,
                claimable ? "green" : "brown"
        );
        button.getLabel().setFontScale(
                state == QuestState.UNAVAILABLE ? 0.62f : 0.72f
        );
        button.setDisabled(!claimable);
        if (claimable) {
            button.addListener(click(() -> claimQuest(spec)));
        }
        return button;
    }

    private Stack progressBar(QuestState state, int value, int target) {
        Stack stack = new Stack();

        Image track = new Image(progressTrackTexture);
        track.setScaling(Scaling.stretch);
        stack.add(track);

        float ratio = state == QuestState.UNAVAILABLE
                ? 0f
                : Math.min(1f, Math.max(0f, value / (float) target));

        Table fillLayer = new Table();
        fillLayer.left();
        Image fill = new Image(progressFillTexture);
        fill.setScaling(Scaling.stretch);
        fillLayer.add(fill)
                .width(PROGRESS_WIDTH * ratio)
                .height(PROGRESS_HEIGHT);
        stack.add(fillLayer);

        return stack;
    }

    private void claimQuest(QuestSpec spec) {
        User user = appState.getCurrentUser();
        if (user == null) {
            showStatus("No active user is available.", true);
            return;
        }

        QuestService.ClaimResult result = questService.claim(user, spec);
        if (result.user() != null && result.user() != user) {
            appState.setCurrentUser(result.user());
        }

        refreshCurrencies();
        rebuildQuestList(false);

        switch (result.status()) {
            case SUCCESS -> showStatus(
                    "Claimed " + spec.name() + ": "
                            + formatRewards(spec.rewards()),
                    false
            );
            case NOT_COMPLETED -> showStatus(
                    "This quest is not completed yet.",
                    true
            );
            case ALREADY_CLAIMED -> showStatus(
                    "This quest reward was already claimed.",
                    true
            );
            case UNAVAILABLE -> showStatus(
                    "This quest belongs to content that is not available yet.",
                    true
            );
            case REWARD_BLOCKED -> showStatus(
                    result.message() == null
                            ? "The quest reward cannot be granted yet."
                            : result.message(),
                    true
            );
            case SAVE_FAILED -> showStatus(
                    "Claim could not be saved; persisted data was reloaded.",
                    true
            );
        }
    }

    private QuestState displayState(
            QuestSpec spec,
            QuestProgress progress
    ) {
        if (progress != null) {
            return progress.getState();
        }
        return spec.initiallyAvailable()
                ? QuestState.AVAILABLE
                : QuestState.UNAVAILABLE;
    }

    private String progressText(QuestState state, int value, int target) {
        if (state == QuestState.UNAVAILABLE) {
            return "Waiting for related gameplay content";
        }
        return Math.min(value, target) + " / " + target;
    }

    private String stateText(QuestState state) {
        return switch (state) {
            case AVAILABLE -> "IN PROGRESS";
            case COMPLETED -> "COMPLETED - REWARD READY";
            case CLAIMED -> "CLAIMED";
            case UNAVAILABLE -> "UNAVAILABLE";
        };
    }

    private Color stateColor(QuestState state) {
        return switch (state) {
            case AVAILABLE -> Color.DARK_GRAY;
            case COMPLETED -> new Color(0.10f, 0.55f, 0.08f, 1f);
            case CLAIMED -> new Color(0.16f, 0.42f, 0.16f, 1f);
            case UNAVAILABLE -> Color.GRAY;
        };
    }

    private Color priorityColor(String priority) {
        return switch (priority) {
            case "CRITICAL" -> Color.SCARLET;
            case "HIGH" -> Color.ORANGE;
            case "MEDIUM" -> Color.YELLOW;
            default -> Color.WHITE;
        };
    }

    private String formatRewards(List<QuestReward> rewards) {
        return rewards.stream()
                .filter(Objects::nonNull)
                .map(this::formatReward)
                .collect(Collectors.joining(" + "));
    }

    private String formatReward(QuestReward reward) {
        QuestRewardType type = reward.type();
        return switch (type) {
            case COINS -> reward.amount() + " Coins";
            case DIAMONDS -> reward.amount() + " Gems";
            case PLANT_UNLOCK -> "Unlock " + reward.targetId();
            case LEVEL_UNLOCK -> "Unlock level " + reward.targetId();
            case SEED_PACKETS -> reward.amount()
                    + " "
                    + reward.targetId()
                    + " Seed Packets";
        };
    }

    private String pretty(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ');
        StringBuilder builder = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (capitalize && Character.isLetter(current)) {
                builder.append(Character.toUpperCase(current));
                capitalize = false;
            } else {
                builder.append(current);
            }
            if (current == ' ') {
                capitalize = true;
            }
        }
        return builder.toString();
    }

    private String categoryLabel(QuestCategory category) {
        return switch (category) {
            case ADVENTURE -> "ADVENTURE";
            case DAILY -> "DAILY";
            case CHALLENGE -> "CHALLENGES";
            case MINIGAME -> "MINIGAMES";
        };
    }

    private String categoryStatusMessage() {
        return switch (selectedCategory) {
            case ADVENTURE ->
                    "Adventure quests are connected to persistent chapter and level progress.";
            case DAILY ->
                    "Daily quests use the current daily cycle and reset automatically.";
            case CHALLENGE ->
                    "Challenges are sorted by priority; future battle metrics stay unavailable until their hooks exist.";
            case MINIGAME ->
                    "Minigame progress is shared with the Minigames menu; gameplay launches in Phase 7.";
        };
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message == null ? "" : message);
        statusLabel.setColor(error ? Color.SCARLET : Color.GREEN);
    }

    private void goBack() {
        game.setScreen(new GameMenuScreen(
                game,
                textures,
                batch,
                skin,
                appState,
                userManager
        ));
    }

    @Override
    public void show() {
        super.show();
        appState.setCurrentMenu(MenuName.TRAVEL_LOG);
        refreshFromModel(true);
    }

    @Override
    public void dispose() {
        progressTrackTexture.dispose();
        progressFillTexture.dispose();
        super.dispose();
    }

    private Texture createSolidTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
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
