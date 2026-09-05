package pvz.graphics.menu;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
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
import pvz.model.leaderboard.AdventureStanding;
import pvz.model.leaderboard.LeaderboardEntry;
import pvz.model.leaderboard.LeaderboardService;
import pvz.model.leaderboard.LeaderboardSort;
import pvz.model.leaderboard.LeaderboardSortKey;
import pvz.model.leaderboard.SortDirection;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;

/** Graphical Phase-2 leaderboard backed by the shared leaderboard service. */
public final class LeaderboardScreen extends BaseScreen {
    private static final float PANEL_X = 32f;
    private static final float PANEL_Y = 72f;
    private static final float PANEL_WIDTH = 1216f;
    private static final float PANEL_HEIGHT = 548f;
    private static final float HEADER_HEIGHT = 44f;
    private static final float ROW_HEIGHT = 58f;

    private static final float USER_WIDTH = 230f;
    private static final float ADVENTURE_WIDTH = 330f;
    private static final float MINIGAME_WIDTH = 140f;
    private static final float DAILY_WIDTH = 125f;
    private static final float QUEST_WIDTH = 135f;
    private static final float SCORE_WIDTH = 145f;

    private static final LeaderboardSortKey[] SORTABLE_COLUMNS = {
            LeaderboardSortKey.USERNAME,
            LeaderboardSortKey.ADVENTURE_PROGRESS,
            LeaderboardSortKey.MINIGAME_COMPLETIONS,
            LeaderboardSortKey.DAILY_QUEST_COMPLETIONS,
            LeaderboardSortKey.NON_DAILY_QUEST_COMPLETIONS,
            LeaderboardSortKey.MAX_MEW_POINT
    };

    private final LeaderboardService leaderboardService;
    private final Map<LeaderboardSortKey, TextButton> headerButtons =
            new EnumMap<>(LeaderboardSortKey.class);
    private final Table rowsTable = new Table();

    private LeaderboardSort sort = LeaderboardSort.defaultSort();
    private ScrollPane rowsScroll;
    private Label diamondLabel;
    private Label coinLabel;
    private Label statusLabel;
    private Label countLabel;

    public LeaderboardScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager
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
        this.leaderboardService = game.getLeaderboardService();
        buildUi();
    }

    private void buildUi() {
        buildHeader();
        buildLeaderboardPanel();
        buildStatusBar();
    }

    private void buildHeader() {
        TextButton back = new TextButton("BACK", skin, "brown");
        back.setBounds(25f, HEIGHT - 72f, 125f, 48f);
        back.addListener(click(this::goBack));
        stage.addActor(back);

        Label title = new Label("LEADERBOARD", skin);
        title.setFontScale(1.45f);
        title.setAlignment(Align.center);
        title.setBounds(300f, HEIGHT - 70f, 600f, 48f);
        stage.addActor(title);

        TextButton refresh = new TextButton("REFRESH", skin, "brown");
        refresh.setBounds(165f, HEIGHT - 72f, 125f, 48f);
        refresh.getLabel().setFontScale(0.78f);
        refresh.addListener(click(() -> refreshLeaderboard(true)));
        stage.addActor(refresh);

        diamondLabel = new Label("", skin);
        diamondLabel.setAlignment(Align.right);
        diamondLabel.setBounds(930f, HEIGHT - 69f, 150f, 44f);
        stage.addActor(diamondLabel);

        coinLabel = new Label("", skin);
        coinLabel.setAlignment(Align.right);
        coinLabel.setBounds(1080f, HEIGHT - 69f, 175f, 44f);
        stage.addActor(coinLabel);
    }

    private void buildLeaderboardPanel() {
        Table frame = new Table();
        frame.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        frame.setBounds(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);
        frame.pad(12f);

        countLabel = new Label("", skin);
        countLabel.setColor(Color.DARK_GRAY);
        countLabel.setAlignment(Align.left);
        frame.add(countLabel)
                .growX()
                .height(28f)
                .left()
                .padBottom(6f)
                .row();

        frame.add(buildColumnHeader())
                .growX()
                .height(HEADER_HEIGHT)
                .padBottom(6f)
                .row();

        rowsTable.top().left();
        rowsTable.defaults().padBottom(4f);

        rowsScroll = new ScrollPane(rowsTable, skin);
        rowsScroll.setFadeScrollBars(false);
        rowsScroll.setScrollingDisabled(true, false);
        rowsScroll.setOverscroll(false, false);

        frame.add(rowsScroll).grow();
        stage.addActor(frame);
    }

    private Table buildColumnHeader() {
        Table header = new Table();
        header.defaults().padRight(4f);

        addHeaderButton(
                header,
                LeaderboardSortKey.USERNAME,
                "PLAYER",
                USER_WIDTH
        );
        addHeaderButton(
                header,
                LeaderboardSortKey.ADVENTURE_PROGRESS,
                "ADVENTURE",
                ADVENTURE_WIDTH
        );
        addHeaderButton(
                header,
                LeaderboardSortKey.MINIGAME_COMPLETIONS,
                "MINIGAMES",
                MINIGAME_WIDTH
        );
        addHeaderButton(
                header,
                LeaderboardSortKey.DAILY_QUEST_COMPLETIONS,
                "DAILY",
                DAILY_WIDTH
        );
        addHeaderButton(
                header,
                LeaderboardSortKey.NON_DAILY_QUEST_COMPLETIONS,
                "QUESTS",
                QUEST_WIDTH
        );
        addHeaderButton(
                header,
                LeaderboardSortKey.MAX_MEW_POINT,
                "MEW POINT",
                SCORE_WIDTH
        );

        return header;
    }

    private void addHeaderButton(
            Table header,
            LeaderboardSortKey key,
            String text,
            float width
    ) {
        TextButton button = new TextButton(text, skin, "brown");
        button.getLabel().setFontScale(0.72f);
        button.getLabel().setAlignment(Align.center);
        button.addListener(click(() -> selectSort(key)));
        headerButtons.put(key, button);
        header.add(button).width(width).height(HEADER_HEIGHT);
    }

    private void buildStatusBar() {
        statusLabel = new Label("", skin);
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(150f, 22f, 980f, 38f);
        stage.addActor(statusLabel);
    }

    private void selectSort(LeaderboardSortKey key) {
        sort = sort.select(key);
        refreshHeaderButtons();
        refreshLeaderboard(true);
    }

    private void refreshHeaderButtons() {
        for (LeaderboardSortKey key : SORTABLE_COLUMNS) {
            TextButton button = headerButtons.get(key);
            if (button == null) {
                continue;
            }

            boolean selected = key == sort.key();
            String styleName = selected ? "green" : "brown";
            button.setStyle(
                    skin.get(styleName, TextButton.TextButtonStyle.class)
            );
            button.setText(headerText(key, selected));
            button.getLabel().setFontScale(0.72f);
            button.getLabel().setAlignment(Align.center);
        }
    }

    private String headerText(LeaderboardSortKey key, boolean selected) {
        String label = switch (key) {
            case USERNAME -> "PLAYER";
            case ADVENTURE_PROGRESS -> "ADVENTURE";
            case MINIGAME_COMPLETIONS -> "MINIGAMES";
            case DAILY_QUEST_COMPLETIONS -> "DAILY";
            case NON_DAILY_QUEST_COMPLETIONS -> "QUESTS";
            case MAX_MEW_POINT -> "MEW POINT";
        };

        if (!selected) {
            return label;
        }
        return label + (sort.direction() == SortDirection.ASCENDING
                ? " ^"
                : " v");
    }

    private void refreshLeaderboard(boolean resetScroll) {
        refreshCurrencies();

        float previousScroll = rowsScroll == null ? 0f : rowsScroll.getScrollY();
        rowsTable.clearChildren();

        try {
            List<LeaderboardEntry> entries = leaderboardService.snapshot(sort);
            countLabel.setText(
                    entries.size() + (entries.size() == 1 ? " player" : " players")
            );

            if (entries.isEmpty()) {
                Label empty = new Label(
                        "No registered users are available for the leaderboard.",
                        skin
                );
                empty.setAlignment(Align.center);
                empty.setColor(Color.DARK_GRAY);
                rowsTable.add(empty)
                        .width(USER_WIDTH + ADVENTURE_WIDTH
                                + MINIGAME_WIDTH + DAILY_WIDTH
                                + QUEST_WIDTH + SCORE_WIDTH)
                        .height(120f);
            } else {
                for (LeaderboardEntry entry : entries) {
                    rowsTable.add(buildRow(entry))
                            .growX()
                            .height(ROW_HEIGHT)
                            .row();
                }
            }

            rowsTable.invalidateHierarchy();
            if (resetScroll && rowsScroll != null) {
                rowsScroll.setScrollY(0f);
            } else if (rowsScroll != null) {
                rowsScroll.setScrollY(previousScroll);
            }

            showStatus(sortStatusMessage(), false);
        } catch (RuntimeException exception) {
            countLabel.setText("Leaderboard unavailable");
            Label error = new Label(
                    "Leaderboard data could not be loaded.",
                    skin
            );
            error.setAlignment(Align.center);
            error.setColor(Color.RED);
            rowsTable.add(error)
                    .width(USER_WIDTH + ADVENTURE_WIDTH
                            + MINIGAME_WIDTH + DAILY_WIDTH
                            + QUEST_WIDTH + SCORE_WIDTH)
                    .height(120f);
            showStatus(
                    exception.getMessage() == null
                            ? "Leaderboard data could not be loaded."
                            : exception.getMessage(),
                    true
            );
        }
    }

    private Table buildRow(LeaderboardEntry entry) {
        boolean currentUser = isCurrentUser(entry);

        Table row = new Table();
        row.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        row.defaults().padRight(4f);

        row.add(playerCell(entry, currentUser))
                .width(USER_WIDTH)
                .height(ROW_HEIGHT);
        row.add(adventureCell(entry.adventure(), currentUser))
                .width(ADVENTURE_WIDTH)
                .height(ROW_HEIGHT);
        row.add(numberCell(entry.completedMinigameStages(), currentUser))
                .width(MINIGAME_WIDTH)
                .height(ROW_HEIGHT);
        row.add(numberCell(entry.completedDailyQuests(), currentUser))
                .width(DAILY_WIDTH)
                .height(ROW_HEIGHT);
        row.add(numberCell(entry.completedNonDailyQuests(), currentUser))
                .width(QUEST_WIDTH)
                .height(ROW_HEIGHT);
        row.add(numberCell(entry.maxMewPoint(), currentUser))
                .width(SCORE_WIDTH)
                .height(ROW_HEIGHT);

        return row;
    }

    private Table playerCell(LeaderboardEntry entry, boolean currentUser) {
        Table cell = new Table();
        cell.left().padLeft(10f).padRight(6f);

        Label username = new Label(
                LeaderboardPresentation.username(entry),
                skin
        );
        username.setFontScale(0.78f);
        username.setColor(currentUser ? Color.YELLOW : Color.DARK_GRAY);
        username.setAlignment(Align.left);
        cell.add(username).growX().left().row();

        String secondary = LeaderboardPresentation.secondaryPlayerText(
                entry,
                currentUser
        );
        Label nickname = new Label(secondary, skin);
        nickname.setFontScale(0.60f);
        nickname.setColor(currentUser ? Color.YELLOW : Color.GRAY);
        nickname.setAlignment(Align.left);
        cell.add(nickname).growX().left();

        return cell;
    }

    private Label adventureCell(
            AdventureStanding standing,
            boolean currentUser
    ) {
        Label label = new Label(
                LeaderboardPresentation.adventure(standing),
                skin
        );
        label.setFontScale(0.66f);
        label.setAlignment(Align.center);
        label.setWrap(true);
        label.setColor(currentUser ? Color.YELLOW : Color.DARK_GRAY);
        return label;
    }

    private Label numberCell(int value, boolean currentUser) {
        Label label = new Label(Integer.toString(value), skin);
        label.setFontScale(0.82f);
        label.setAlignment(Align.center);
        label.setColor(currentUser ? Color.YELLOW : Color.DARK_GRAY);
        return label;
    }

    private boolean isCurrentUser(LeaderboardEntry entry) {
        User current = appState.getCurrentUser();
        return current != null
                && current.getUsername() != null
                && current.getUsername().equals(entry.username());
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

    private String sortStatusMessage() {
        String column = switch (sort.key()) {
            case USERNAME -> "player";
            case ADVENTURE_PROGRESS -> "adventure progress";
            case MINIGAME_COMPLETIONS -> "minigame completions";
            case DAILY_QUEST_COMPLETIONS -> "daily quest completions";
            case NON_DAILY_QUEST_COMPLETIONS -> "quest completions";
            case MAX_MEW_POINT -> "Mew Point";
        };
        String direction = sort.direction() == SortDirection.ASCENDING
                ? "ascending"
                : "descending";
        return "Sorted by " + column + " (" + direction + ").";
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message == null ? "" : message);
        statusLabel.setColor(error ? Color.RED : Color.WHITE);
    }

    private void goBack() {
        appState.setCurrentMenu(MenuName.MAIN);
        game.setScreen(new MainMenuScreen(
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
        appState.setCurrentMenu(MenuName.LEADERBOARD);
        refreshHeaderButtons();
        refreshLeaderboard(true);
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
