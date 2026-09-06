package pvz.graphics.menu;

import pvz.graphics.ui.Typography;
import pvz.graphics.ui.HoverEffect;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
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
    private static final float HEADER_HEIGHT = 64f;
    private static final float ROW_HEIGHT = 72f;

    private static final float USER_WIDTH = 270f;
    private static final float ADVENTURE_WIDTH = 290f;
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

    private static final Color TEXT = Color.valueOf("eef5f2");
    private static final Color SECONDARY = Color.valueOf("bdcfd4");
    private static final Color ACCENT = Color.valueOf("a9edb8");

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
        TextureRegion normal = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_NORMAL");
        TextureRegion pressed = textures.region("IMAGE_UI_MAINMENU_BACK_BTN_PRESSED");
        if (normal != null) {
            ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
            style.up = new TextureRegionDrawable(normal);
            style.down = pressed == null ? style.up : new TextureRegionDrawable(pressed);
            ImageButton back = new ImageButton(style);
            back.setBounds(28f, HEIGHT - 80f, 56f, 56f);
            back.addListener(click(this::goBack));
            HoverEffect.addScale(back);
            stage.addActor(back);
        } else {
            TextButton back = new TextButton("BACK", skin, "brown");
            back.setBounds(20f, HEIGHT - 72f, 105f, 48f);
            back.addListener(click(this::goBack));
            stage.addActor(back);
        }
        TextureRegion crown = textures.region("IMAGE_UI_GAMECENTER_ANDROID_LEADERBOARD");
        if (crown != null) {
            Image icon = new Image(crown);
            icon.setScaling(Scaling.fit);
            icon.setBounds(135f, HEIGHT - 77f, 64f, 50f);
            stage.addActor(icon);
        }
        Label title = new Label("LEADERBOARD", skin);
        Typography.setReadableScale(title, 1.45f);
        title.setBounds(214f, HEIGHT - 76f, 430f, 48f);
        stage.addActor(title);

        TextButton refresh = new TextButton("REFRESH", skin, "brown");
        refresh.setBounds(710f, HEIGHT - 74f, 155f, 46f);
        Typography.setReadableScale(refresh.getLabel(), 0.78f);
        refresh.addListener(click(() -> refreshLeaderboard(true)));
        HoverEffect.addScale(refresh);
        stage.addActor(refresh);
        diamondLabel = new Label("", skin);
        coinLabel = new Label("", skin);
        addCurrency("IMAGE_UI_QUESTS_GEM_ICON", diamondLabel, 916f);
        addCurrency("IMAGE_UI_QUESTS_COIN_ICON", coinLabel, 1084f);
    }

    private void addCurrency(String key, Label label, float x) {
        addCurrencyBadge(key, label, x, this::refreshCurrencies);
    }

    private void buildLeaderboardPanel() {
        Table frame = new Table();
        frame.setBackground(skin.newDrawable(
                "image_ui_dialog_asset_inner_bkgd_10", Color.valueOf("142e3c")
        ));
        frame.setBounds(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);
        frame.pad(12f);

        countLabel = new Label("", skin);
        countLabel.setColor(SECONDARY);
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
        rowsTable.defaults().padBottom(7f);

        rowsScroll = new ScrollPane(rowsTable, skin);
        rowsScroll.setFadeScrollBars(false);
        rowsScroll.setScrollingDisabled(true, false);
        rowsScroll.setOverscroll(false, false);

        frame.add(rowsScroll).grow();
        stage.addActor(frame);
    }

    private Table buildColumnHeader() {
        Table header = new Table();
        header.left();
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
                "MINIGAME\nSTAGES",
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
        Typography.setReadableScale(button.getLabel(), 0.72f);
        button.getLabel().setAlignment(Align.center);
        button.addListener(click(() -> selectSort(key)));
        headerButtons.put(key, button);
        button.getLabel().setWrap(true);
        header.add(button).width(width).height(HEADER_HEIGHT);
    }

    private void buildStatusBar() {
        statusLabel = new Label("", skin);
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);
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
            Typography.setReadableScale(button.getLabel(), 0.72f);
            button.getLabel().setAlignment(Align.center);
        }
    }

    private String headerText(LeaderboardSortKey key, boolean selected) {
        String label = switch (key) {
            case USERNAME -> "PLAYER";
            case ADVENTURE_PROGRESS -> "ADVENTURE";
            case MINIGAME_COMPLETIONS -> "MINIGAME\nSTAGES";
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
                empty.setColor(TEXT);
                rowsTable.add(empty)
                        .width(USER_WIDTH + ADVENTURE_WIDTH
                                + MINIGAME_WIDTH + DAILY_WIDTH
                                + QUEST_WIDTH + SCORE_WIDTH)
                        .height(120f);
            } else {
                int rowIndex = 0;
                for (LeaderboardEntry entry : entries) {
                    rowsTable.add(buildRow(entry, rowIndex++))
                            .left()
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

    private Table buildRow(LeaderboardEntry entry, int rowIndex) {
        boolean currentUser = isCurrentUser(entry);

        Table row = new Table();
        Color background = currentUser ? Color.valueOf("245b51")
                : (rowIndex % 2 == 0 ? Color.valueOf("284351") : Color.valueOf("203945"));
        row.setBackground(skin.newDrawable("image_ui_dialog_asset_inner_bkgd_10", background));
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
        Typography.setReadableScale(username, 0.78f);
        username.setColor(currentUser ? ACCENT : TEXT);
        username.setEllipsis(true);
        username.setAlignment(Align.left);
        cell.add(username).growX().left();
        if (currentUser) {
            Label marker = new Label("YOU", skin);
            Typography.setReadableScale(marker, 0.60f);
            marker.setColor(ACCENT);
            cell.add(marker).width(48f).padLeft(4f);
        }
        cell.row();
        Table details = new Table();
        details.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        details.pad(12f);
        Label fullName = new Label(entry.username() + "\n" + entry.nickname(), skin);
        Typography.setReadableScale(fullName, 0.78f);
        fullName.setColor(Color.DARK_GRAY);
        fullName.setWrap(true);
        details.add(fullName).width(480f);
        cell.addListener(new Tooltip<Table>(details));

        String secondary = LeaderboardPresentation.secondaryPlayerText(
                entry,
                false
        );
        Label nickname = new Label(secondary, skin);
        Typography.setReadableScale(nickname, 0.60f);
        nickname.setColor(SECONDARY);
        nickname.setEllipsis(true);
        nickname.setAlignment(Align.left);
        cell.add(nickname).colspan(currentUser ? 2 : 1).growX().left();

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
        Typography.setReadableScale(label, 0.66f);
        label.setAlignment(Align.center);
        label.setWrap(true);
        label.setColor(currentUser ? ACCENT : TEXT);
        return label;
    }

    private Label numberCell(int value, boolean currentUser) {
        Label label = new Label(Integer.toString(value), skin);
        Typography.setReadableScale(label, 0.82f);
        label.setAlignment(Align.center);
        label.setColor(currentUser ? ACCENT : TEXT);
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
            diamondLabel.setText("0");
            coinLabel.setText("0");
            return;
        }
        diamondLabel.setText(String.valueOf(user.getDiamonds()));
        coinLabel.setText(String.valueOf(user.getCoins()));
    }

    private String sortStatusMessage() {
        String column = switch (sort.key()) {
            case USERNAME -> "player";
            case ADVENTURE_PROGRESS -> "adventure progress";
            case MINIGAME_COMPLETIONS -> "completed minigame stages";
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
        statusLabel.setColor(error ? Color.SALMON : SECONDARY);
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
