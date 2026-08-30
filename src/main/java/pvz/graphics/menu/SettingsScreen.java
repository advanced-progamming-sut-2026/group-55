package pvz.graphics.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import pvz.controller.SettingsController;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.SettingsCommand;
import pvz.model.utils.AppState;
import pvz.view.MenuView;

public class SettingsScreen extends Group {
    private static final float PANEL_WIDTH = 430f, PANEL_HEIGHT = 400f;
    private static final float CONTENT_WIDTH = 380f, PEPPER_SIZE = 42f;
    private static final float OVERLAY_ALPHA = 0.55f;

    private final SettingsController controller;
    private final TextureBank textures;
    private final Skin skin;
    private final AppState appState;
    private final UserManager userManager;
    private final Table contentTable;

    private Image overlay;
    private Label saveMessage;

    public SettingsScreen(TextureBank textures, Skin skin, AppState appState, UserManager userManager) {
        this.textures = textures;
        this.skin = skin;
        this.appState = appState;
        this.userManager = userManager;
        contentTable = new Table();

        controller = new SettingsController(appState, userManager, new MenuView() {
            @Override public void showSuccess(String message) {}
            @Override public void showError(String message) {}
            @Override public void showMessage(String message) {}
            @Override public void showRegisterWelcome() {}
        });

        buildUI();
        setVisible(false);
    }

    private void buildUI() {
        setSize(1280f, 720f);

        overlay = new Image(createOverlayDrawable());
        overlay.setSize(getWidth(), getHeight());
        overlay.getColor().a = OVERLAY_ALPHA;
        overlay.addListener(new ClickListener());
        addActor(overlay);

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        panel.setSize(PANEL_WIDTH, PANEL_HEIGHT);
        panel.setPosition((getWidth() - PANEL_WIDTH) / 2f, (getHeight() - PANEL_HEIGHT) / 2f);
        addActor(panel);

        Table header = new Table();
        Image back = new Image(textures.region("IMAGE_UI_MAINMENU_BACK_BTN_NORMAL"));
        back.setSize(35f, 35f);
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                hide();
            }
        });

        header.add(back).size(45f).left().padLeft(5f);

        Label title = new Label("SETTINGS", skin);
        title.setAlignment(Align.center);
        title.setFontScale(1.15f);
        header.add(title).expandX().fillX().center();
        header.add().size(45f);

        panel.add(header).width(PANEL_WIDTH).height(50f).top().row();

        contentTable.defaults().pad(3f);
        ScrollPane scrollPane = new ScrollPane(contentTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        panel.add(scrollPane).width(PANEL_WIDTH - 20f).height(PANEL_HEIGHT - 60f)
                .expand().fill().padBottom(8f).row();

        saveMessage = new Label("", skin);
        saveMessage.setAlignment(Align.center);
        saveMessage.setColor(Color.WHITE);
        saveMessage.setVisible(false);
        saveMessage.setSize(300f, 30f);
        saveMessage.setPosition((getWidth() - 300f) / 2f, 20f);
        addActor(saveMessage);

        showSettings();
    }

    private Drawable createOverlayDrawable() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }

    private void showSettings() {
        contentTable.clear();
        User user = appState.getCurrentUser();

        if (user == null) {
            contentTable.add(new Label("No user is logged in.", skin)).center();
            return;
        }

        addDifficultySection(user);
        addGameSpeedSection(user);
        addToggleOptions(user);
        addSaveButton();
    }

    private void addDifficultySection(User user) {
        Label title = new Label("Difficulty", skin);
        title.setColor(Color.WHITE);
        title.setFontScale(1.05f);

        contentTable.add(title).width(CONTENT_WIDTH).height(30f).left().padTop(5f).row();

        Table pepperTable = new Table();
        pepperTable.defaults().pad(3f);
        Image[] peppers = new Image[5];

        for (int i = 0; i < peppers.length; i++) {
            final int level = i + 1;
            peppers[i] = createPepper(level <= user.getDifficultyLevel());

            peppers[i].addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    controller.handle(SettingsCommand.createChangeDifficulty(level));
                    updatePeppers(peppers, level);
                }
            });

            pepperTable.add(peppers[i]).size(PEPPER_SIZE);
        }

        contentTable.add(pepperTable).width(CONTENT_WIDTH).height(52f).center().row();
    }

    private Image createPepper(boolean filled) {
        return new Image(textures.region(filled
                ? "IMAGE_UI_PENNY_PURSUITS_COMMON_EASY_ICON_SMALL"
                : "IMAGE_UI_PENNY_PURSUITS_COMMON_EASY_HOLLOW_ICON_SMALL"));
    }

    private void updatePeppers(Image[] peppers, int level) {
        for (int i = 0; i < peppers.length; i++) {
            peppers[i].setDrawable(new Image(textures.region(i < level
                    ? "IMAGE_UI_PENNY_PURSUITS_COMMON_EASY_ICON_SMALL"
                    : "IMAGE_UI_PENNY_PURSUITS_COMMON_EASY_HOLLOW_ICON_SMALL")).getDrawable());
        }
    }

    private void addGameSpeedSection(User user) {
        Label title = new Label("Game Speed: " + user.getGameSpeed(), skin);
        title.setColor(Color.WHITE);
        title.setFontScale(1.05f);
        title.setAlignment(Align.center);

        TextButton decrease = createButton("-");
        TextButton increase = createButton("+");

        Table speedTable = new Table();
        speedTable.add(decrease).size(55f, 42f);
        speedTable.add(title).width(180f).height(42f).center();
        speedTable.add(increase).size(55f, 42f);
        contentTable.add(speedTable).width(CONTENT_WIDTH).height(52f).center().row();

        decrease.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                changeSpeed(user, -1, title);
            }
        });

        increase.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                changeSpeed(user, 1, title);
            }
        });
    }

    private void changeSpeed(User user, int amount, Label title) {
        int speed = user.getGameSpeed() + amount;
        if (speed >= 1 && speed <= 3) {
            controller.handle(SettingsCommand.createChangeGameSpeed(speed));
            title.setText("Game Speed: " + user.getGameSpeed());
        }
    }

    private void addToggleOptions(User user) {
        CheckBox grid = new CheckBox(" Show Grid", skin);
        grid.setChecked(user.isShowGrid());
        contentTable.add(grid).width(CONTENT_WIDTH).height(40f).left().row();
        grid.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                controller.handle(SettingsCommand.createToggleGrid(grid.isChecked()));
            }
        });

        CheckBox debug = new CheckBox(" Debug Mode", skin);
        debug.setChecked(user.isDebugMode());
        contentTable.add(debug).width(CONTENT_WIDTH).height(40f).left().row();
        debug.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                controller.handle(SettingsCommand.createToggleDebug(debug.isChecked()));
            }
        });
    }

    private void addSaveButton() {
        TextButton save = createButton("SAVE");
        contentTable.add(save).width(220f).height(45f).padTop(10f).center().row();

        save.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                userManager.save();
                showSaveMessage();
            }
        });
    }

    private TextButton createButton(String text) {
        return new TextButton(text, skin);
    }

    private void showSaveMessage() {
        saveMessage.clearActions();
        saveMessage.setText("Changes saved.");
        saveMessage.setVisible(true);
        saveMessage.getColor().a = 1f;

        saveMessage.addAction(Actions.sequence(
                Actions.delay(2f),
                Actions.fadeOut(0.3f),
                Actions.run(() -> {
                    saveMessage.setVisible(false);
                    saveMessage.getColor().a = 1f;
                })
        ));
    }

    public void show() {
        if (appState.getCurrentUser() != null) showSettings();

        overlay.setSize(getWidth(), getHeight());
        overlay.getColor().a = OVERLAY_ALPHA;
        setVisible(true);
        toFront();
    }

    public void hide() {
        setVisible(false);
    }
}
