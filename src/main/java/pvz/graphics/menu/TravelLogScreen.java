package pvz.graphics.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import pvz.graphics.ui.Typography;
import pvz.graphics.ui.HoverEffect;
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
import java.time.LocalDate;
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
    private static final float CARD_HEIGHT = 154f;
    private static final float PROGRESS_WIDTH = 225f;
    private static final float PROGRESS_HEIGHT = 18f;
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

    private static final Color INK = new Color(0.16f, 0.22f, 0.25f, 1f);
    private static final Color MUTED = new Color(0.31f, 0.36f, 0.38f, 1f);
    private LocalDate lastRefreshDate;
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
                "IMAGE_UI_QUESTS_TRAVEL_LOG_FINAL"
        );
        this.questCatalog = game.getGameData().questCatalog();
        this.minigameCatalog = game.getGameData().minigameCatalog();
        this.minigameProgressService =
                game.getGameData().minigameProgressService();
        this.questService = game.getQuestService();
        this.selectedCategory = initialCategory == null
                ? QuestCategory.ADVENTURE
                : initialCategory;

        buildUi();
        refreshCategoryButtons();
    }

    private void buildUi() {
        buildHeader();
        buildCategoryTabs();
        buildQuestPanel();
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
            back.setBounds(28f, HEIGHT - 82f, 56f, 56f);
            back.addListener(click(this::goBack));
            HoverEffect.addScale(back);
            stage.addActor(back);
        } else {
            TextButton back = new TextButton("BACK", skin, "brown");
            back.setBounds(25f, HEIGHT - 72f, 125f, 48f);
            back.addListener(click(this::goBack));
            stage.addActor(back);
        }
        TextureRegion mascot = textures.region("IMAGE_UI_QUESTS_QUEST_ICON_BROWN");
        if (mascot != null) {
            Image icon = new Image(mascot);
            icon.setScaling(Scaling.fit);
            icon.setBounds(103f, HEIGHT - 94f, 64f, 70f);
            stage.addActor(icon);
        }
        Label title = new Label("TRAVEL LOG", skin);
        Typography.setReadableScale(title, 1.55f);
        title.setBounds(180f, HEIGHT - 72f, 540f, 44f);
        stage.addActor(title);
        Label subtitle = body("YOUR JOURNEY  /  QUESTS & REWARDS", 0.72f);
        subtitle.setColor(new Color(0.70f, 0.85f, 0.88f, 1f));
        subtitle.setBounds(180f, HEIGHT - 96f, 540f, 26f);
        stage.addActor(subtitle);
        diamondLabel = body("", 1.05f);
        coinLabel = body("", 1.05f);
        addCurrency("IMAGE_UI_QUESTS_GEM_ICON", diamondLabel, 916f);
        addCurrency("IMAGE_UI_QUESTS_COIN_ICON", coinLabel, 1084f);
    }

    private void addCurrency(String key, Label label, float x) {
        Table badge = new Table();
        badge.setBackground(skin.newDrawable("image_ui_dialog_asset_inner_bkgd_10",
                new Color(0.08f, 0.16f, 0.21f, 1f)));
        badge.setBounds(x, HEIGHT - 79f, 156f, 54f);
        TextureRegion source = textures.region(key);
        if (source != null) {
            Image image = new Image(source);
            image.setScaling(Scaling.fit);
            badge.add(image).width(52f).height(52f);
        }
        label.setColor(Color.WHITE);
        label.setAlignment(Align.center);
        label.setEllipsis(true);
        badge.add(label).width(92f).height(42f);
        stage.addActor(badge);
    }

    /** Nine-patch stretching preserves the authored corners of quest assets. */
    private Drawable questPatch(String id, int border) {
        TextureRegion region = textures.region(id);
        if (region == null) {
            return skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10");
        }
        return new NinePatchDrawable(new NinePatch(region, border, border, border, border));
    }

    private Label body(String text, float scale) {
        Label label = new Label(text, skin);
        Typography.applyBody(label, skin);
        Typography.setReadableScale(label, scale);
        label.setColor(INK);
        return label;
    }

    private Image categoryArt(QuestCategory category) {
        String key = switch (category) {
            case ADVENTURE -> "IMAGE_UI_QUESTS_QUESTICONS_EGYPT";
            case DAILY -> "IMAGE_UI_QUESTS_DAILY_QUEST_CLOCK_ICON_DAILY_QUEST_CLOCK_ICON_77X78";
            case CHALLENGE -> "IMAGE_UI_QUESTS_ICON_EPIC";
            case MINIGAME -> "IMAGE_UI_QUESTS_QUESTICONS_ZOMBIE";
        };
        TextureRegion region = textures.region(key);
        if (region == null) {
            region = textures.region("IMAGE_UI_QUESTS_QUESTICONS_PREMIUMSEEDS");
        }
        Image image = region == null ? new Image() : new Image(region);
        image.setScaling(Scaling.fit);
        return image;
    }

    private Table questArt(QuestSpec spec) {
        String key = switch (spec.objective().metric()) {
            case OWNED_PLANTS, PLANT_PLACED -> "IMAGE_UI_QUESTS_QUESTICONS_PLANT";
            case UPGRADED_PLANTS, PLANT_UPGRADED -> "IMAGE_UI_QUESTS_QUESTICONS_LEVELUP";
            case SEEN_ZOMBIES -> "IMAGE_UI_QUESTS_QUESTICONS_ZOMBIE";
            case ZOMBIE_KILLED -> "IMAGE_UI_QUESTS_QUESTICONS_ASH";
            case SUN_SPENT -> "IMAGE_UI_QUESTS_QUESTICONS_PLANT";
            case COINS_EARNED -> "IMAGE_UI_QUESTS_EPIC_REWARD_COINS";
            case DIAMONDS_EARNED -> "IMAGE_UI_QUESTS_EPIC_REWARD_GEMS";
            case SEED_PACKETS_COLLECTED -> "IMAGE_UI_QUESTS_QUESTICONS_PREMIUMSEEDS";
            default -> null;
        };
        TextureRegion region = key == null ? null : textures.region(key);
        Image icon = region == null ? categoryArt(spec.category()) : new Image(region);
        icon.setScaling(Scaling.fit);
        // Daily's clock is a small atlas sprite: do not enlarge it to card height.
        float size = spec.category() == QuestCategory.DAILY ? 40f : 72f;
        Table slot = new Table();
        slot.add(icon).size(size);
        return slot;
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
            Typography.setReadableScale(button.getLabel(), 0.82f);
            HoverEffect.addScale(button, () -> !button.isDisabled());
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
        refreshFromModel(true);
    }

    private void refreshCategoryButtons() {
        for (Map.Entry<QuestCategory, TextButton> entry
                : categoryButtons.entrySet()) {
            boolean selected = entry.getKey() == selectedCategory;
            String family = switch (entry.getKey()) {
                case ADVENTURE, MINIGAME -> "EPIC";
                case DAILY -> "DAILY";
                case CHALLENGE -> "ACHIEVEMENTS";
            };
            TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(
                    skin.get("brown", TextButton.TextButtonStyle.class));
            style.up = questPatch("IMAGE_UI_QUESTS_" + family
                    + (selected ? "_ACTIVE" : "_INACTIVE"), 10);
            style.down = questPatch("IMAGE_UI_QUESTS_" + family + "_ACTIVE", 10);
            style.over = style.down;
            entry.getValue().setStyle(style);
        }
    }

    private void buildQuestPanel() {
        questTable.top().left();
        questTable.defaults().padBottom(8f);

        questScroll = new ScrollPane(questTable, skin);
        questScroll.setFadeScrollBars(false);
        questScroll.setScrollingDisabled(true, false);

        Table frame = new Table();
        frame.setBackground(skin.newDrawable(
                "image_ui_dialog_asset_inner_bkgd_10",
                new Color(0.10f, 0.20f, 0.25f, 0.96f)
        ));
        frame.setBounds(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);
        frame.add(questScroll).grow().pad(16f);
        stage.addActor(frame);
    }

    private void buildStatusBar() {
        statusLabel = body("", 0.82f);
        statusLabel.setWrap(true);
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(150f, 22f, 980f, 38f);
        stage.addActor(statusLabel);
    }

    private void refreshFromModel(boolean resetScroll) {
        lastRefreshDate = questService.currentDate();
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
            diamondLabel.setText("0");
            coinLabel.setText("0");
            return;
        }
        diamondLabel.setText(String.valueOf(user.getDiamonds()));
        coinLabel.setText(String.valueOf(user.getCoins()));
    }

    private void rebuildQuestList(boolean resetScroll) {
        float previousScroll = questScroll == null ? 0f : questScroll.getScrollY();
        questTable.clearChildren();

        if (selectedCategory == QuestCategory.ADVENTURE) {
            questTable.add(buildAdventureAccessCard())
                    .width(CARD_WIDTH)
                    .minHeight(145f)
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
                        .minHeight(155f)
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
                        .minHeight(CARD_HEIGHT)
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
        card.setBackground(questPatch("IMAGE_UI_QUESTS_TRAVEL_LOG_PANEL_DEFAULT", 12));
        card.pad(12f);

        Table info = new Table();
        info.left();
        Label title = new Label("ADVENTURE HUB", skin);
        Typography.setReadableScale(title, 1.0f);
        title.setColor(INK);
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
        Typography.setReadableScale(summary, 0.74f);
        Typography.applyBody(summary, skin);
        summary.setColor(MUTED);
        info.add(summary).left().padTop(6f).row();

        Label note = new Label(
                "Complete available Adventure stages to advance your quests. More chapters are coming soon.",
                skin
        );
        Typography.applyBody(note, skin);
        note.setColor(MUTED);
        note.setWrap(true);
        Typography.setReadableScale(note, 0.68f);
        info.add(note).left().width(760f).padTop(5f);

        TextButton open = new TextButton("OPEN ADVENTURE", skin, "green");
        Typography.setReadableScale(open.getLabel(), 0.72f);
        open.addListener(click(this::openAdventure));
        HoverEffect.addScale(open);

        card.add(info).growX().left();
        card.add(open).width(190f).height(44f).right().padLeft(16f);
        return card;
    }

    private Table buildMinigameAccessHeader() {
        Table header = new Table();

        Label accessTitle = sectionLabel("MINIGAME ACCESS");
        TextButton open = new TextButton("OPEN MINIGAMES", skin, "green");
        Typography.setReadableScale(open.getLabel(), 0.70f);
        open.addListener(click(this::openMinigames));
        HoverEffect.addScale(open);

        header.add(accessTitle).growX().left();
        header.add(open).width(185f).height(40f).right();
        return header;
    }

    private Table buildMinigameAccessCard(MinigameSpec spec) {
        Table card = new Table();
        card.setBackground(questPatch("IMAGE_UI_QUESTS_TRAVEL_LOG_PANEL_DEFAULT", 12));
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
        Typography.setReadableScale(name, 0.95f);
        name.setColor(INK);
        info.add(name).left().row();

        Label description = new Label(spec.description(), skin);
        Typography.applyBody(description, skin);
        description.setColor(INK);
        description.setWrap(true);
        Typography.setReadableScale(description, 0.68f);
        info.add(description).left().width(555f).padTop(5f).row();

        Label phase = new Label(
                completed + " / " + spec.stageCount()
                        + " stages cleared - coming soon.",
                skin
        );
        Typography.setReadableScale(phase, 0.64f);
        Typography.applyBody(phase, skin);
        phase.setColor(MUTED);
        phase.setWrap(true);
        info.add(phase).width(555f).left().padTop(5f);

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
            Typography.setReadableScale(stageButton.getLabel(), 0.60f);
            stageButton.getLabel().setWrap(true);
            stageButton.getLabel().setAlignment(Align.center);
            stageButton.setDisabled(true);
            stages.add(stageButton).width(128f).height(76f);
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
            case AVAILABLE -> "STAGE " + stageNumber + "\nCOMING SOON";
            case LOCKED -> "STAGE " + stageNumber + "\nLOCKED";
        };
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text, skin);
        Typography.setReadableScale(label, 0.90f);
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
        card.setBackground(questPatch("IMAGE_UI_QUESTS_TRAVEL_LOG_PANEL_DEFAULT", 12));
        card.pad(10f);

        Table info = new Table();
        info.top().left();

        Label name = new Label(spec.name(), skin);
        Typography.setReadableScale(name, 1.02f);
        name.setColor(INK);
        name.setWrap(true);
        info.add(name).left().width(565f).colspan(2).row();

        Label meta = new Label(
                pretty(spec.category().name())
                        + "  |  "
                        + pretty(spec.priority().name()),
                skin
        );
        Typography.applyBody(meta, skin);
        meta.setColor(MUTED);
        Typography.setReadableScale(meta, 0.68f);
        meta.setAlignment(Align.right);
        meta.setWrap(true);
        info.add(meta).left().width(565f).colspan(2).padTop(4f).row();

        Label description = new Label(spec.description(), skin);
        Typography.applyBody(description, skin);
        description.setColor(INK);
        description.setWrap(true);
        Typography.setReadableScale(description, 0.78f);
        info.add(description)
                .colspan(2)
                .left()
                .width(565f)
                .padTop(4f)
                .row();

        String rewardText = "Reward: " + formatRewards(spec.rewards());
        if (spec.resetPolicy() == QuestResetPolicy.DAILY) {
            rewardText += "  |  Resets daily";
        }
        Label rewards = new Label(rewardText, skin);
        Typography.applyBody(rewards, skin);
        rewards.setWrap(true);
        Typography.setReadableScale(rewards, 0.70f);
        rewards.setColor(new Color(0.24f, 0.34f, 0.16f, 1f));
        info.add(rewards)
                .colspan(2)
                .left()
                .width(565f)
                .padTop(4f);

        Table progressArea = new Table();
        progressArea.top();

        Label stateLabel = new Label(stateText(state), skin);
        Typography.applyBody(stateLabel, skin);
        stateLabel.setWrap(true);
        stateLabel.setAlignment(Align.center);
        stateLabel.setColor(stateColor(state));
        Typography.setReadableScale(stateLabel, 0.82f);
        progressArea.add(stateLabel).width(245f).minHeight(44f).row();

        Label progressLabel = new Label(
                progressText(state, value, target),
                skin
        );
        Typography.applyBody(progressLabel, skin);
        progressLabel.setColor(MUTED);
        progressLabel.setWrap(true);
        progressLabel.setAlignment(Align.center);
        Typography.setReadableScale(progressLabel, 0.75f);
        progressArea.add(progressLabel)
                .width(245f)
                .minHeight(28f)
                .padTop(2f)
                .row();

        progressArea.add(progressBar(state, value, target))
                .width(PROGRESS_WIDTH)
                .height(PROGRESS_HEIGHT)
                .padTop(3f)
                .padBottom(9f)
                .row();

        TextButton action = buildActionButton(spec, state);
        action.getLabel().setWrap(true);
        progressArea.add(action).width(225f).height(48f);

        card.add(questArt(spec)).width(90f).height(110f).padRight(20f);
        card.add(info).width(600f).growY().left();
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
        Typography.setReadableScale(button.getLabel(), 
                state == QuestState.UNAVAILABLE ? 0.62f : 0.72f
        );
        button.setDisabled(!claimable);
        HoverEffect.addScale(button, () -> !button.isDisabled());
        if (claimable) {
            button.addListener(click(() -> claimQuest(spec)));
        }
        return button;
    }

    private Stack progressBar(QuestState state, int value, int target) {
        Stack stack = new Stack();

        Image track = new Image(questPatch("IMAGE_UI_QUESTS_QUEST_POINTS_FILLBAR_BG", 5));
        track.setScaling(Scaling.stretch);
        stack.add(track);

        float ratio = state == QuestState.UNAVAILABLE || target <= 0
                ? 0f
                : Math.min(1f, Math.max(0f, value / (float) target));
        if (ratio <= 0f) {
            return stack;
        }

        TextureRegion source = textures.region(
                "IMAGE_UI_QUESTS_QUEST_POINTS_FILLBAR_FILL_GREEN");
        if (source != null) {
            // A NinePatch has fixed end caps and can overflow at small widths.
            // Crop the sprite instead, and allow the cell to shrink below its
            // native width so even very small positive progress stays accurate.
            TextureRegion portion = new TextureRegion(source, 0, 0,
                    Math.max(1, (int) Math.ceil(source.getRegionWidth() * ratio)),
                    source.getRegionHeight());
            Image fill = new Image(portion);
            fill.setScaling(Scaling.stretch);
            Table fillLayer = new Table();
            fillLayer.left();
            fillLayer.add(fill).minWidth(0f)
                    .prefWidth(PROGRESS_WIDTH * ratio)
                    .maxWidth(PROGRESS_WIDTH * ratio)
                    .height(PROGRESS_HEIGHT);
            stack.add(fillLayer);
        }

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

        if (result.status() == QuestService.ClaimStatus.SUCCESS) {
            // Plant rewards can complete other quests, such as Growing Collection.
            QuestService.SyncResult sync = questService.synchronizeAndSave(
                    appState.getCurrentUser(), questCatalog.all());
            if (sync.user() != null && sync.user() != appState.getCurrentUser()) {
                appState.setCurrentUser(sync.user());
            }
            refreshCurrencies();
            rebuildQuestList(false);
            if (!sync.saved()) {
                showStatus("Reward claimed, but other quest progress could not be saved. Reopen Travel Log to retry.", true);
                return;
            }
        } else {
            refreshCurrencies();
            rebuildQuestList(false);
        }

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
            return "Coming in a future update";
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
                    "Complete Adventure stages to earn rewards. More adventures are coming soon.";
            case DAILY ->
                    "Daily quests reset each day. Complete them and claim your rewards.";
            case CHALLENGE ->
                    "Grow your collection and complete battle challenges to earn rewards.";
            case MINIGAME ->
                    "Browse minigames and their stages. Minigames are coming soon.";
        };
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message == null ? "" : message);
        statusLabel.setColor(error ? new Color(1f, 0.55f, 0.45f, 1f) : new Color(0.75f, 0.87f, 0.90f, 1f));
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
    public void render(float delta) {
        if (!questService.currentDate().equals(lastRefreshDate)) {
            refreshFromModel(false);
        }
        super.render(delta);
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
