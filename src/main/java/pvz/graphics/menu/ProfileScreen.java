package pvz.graphics.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import pvz.controller.ProfileController;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.ProfileCommand;
import pvz.model.service.AuthService;
import pvz.model.utils.AppState;
import pvz.view.MenuView;

public class ProfileScreen extends Group {

    private static final float PANEL_WIDTH = 430f, PANEL_HEIGHT = 400f;
    private static final float CONTENT_WIDTH = 380f, FIELD_WIDTH = 275f, SAVE_WIDTH = 78f;
    private static final float OVERLAY_ALPHA = 0.55f;

    private final ProfileController controller;
    private final TextureBank textures;
    private final Skin skin;
    private final AppState appState;
    private final UserManager userManager;
    private final Table contentTable;
    private final Label messageLabel;
    private Image overlay;

    public ProfileScreen(TextureBank textures, Skin skin, AppState appState, UserManager userManager) {
        this.textures = textures;
        this.skin = skin;
        this.appState = appState;
        this.userManager = userManager;
        this.contentTable = new Table();
        this.messageLabel = new Label("", skin);

        controller = new ProfileController(
                appState,
                userManager,
                new AuthService(userManager),
                new MenuView() {
                    @Override
                    public void showSuccess(String message) {
                        ProfileScreen.this.showSuccess(message);
                        buildProfileContent();
                    }

                    @Override
                    public void showError(String message) {
                        ProfileScreen.this.showError(message);
                    }

                    @Override
                    public void showMessage(String message) {
                        ProfileScreen.this.showInfo(message);
                    }

                    @Override
                    public void showRegisterWelcome() {
                    }
                }
        );

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
            @Override public void clicked(InputEvent event, float x, float y) { hide(); }
        });

        header.add(back).size(45f).left().padLeft(5f);
        Label title = new Label("PROFILE", skin);
        title.setAlignment(Align.center);
        title.setFontScale(1.15f);
        header.add(title).expandX().fillX().center();
        header.add().size(45f);

        panel.add(header).width(PANEL_WIDTH).height(50f).top().row();

        contentTable.defaults().pad(3f);
        ScrollPane scroll = new ScrollPane(contentTable, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        panel.add(scroll).width(PANEL_WIDTH - 20f).height(PANEL_HEIGHT - 60f)
                .expand().fill().padBottom(8f).row();

        messageLabel.setAlignment(Align.center);
        messageLabel.setColor(Color.WHITE);
        messageLabel.setPosition((getWidth() - 700f) / 2f, 20f);
        messageLabel.setSize(700f, 30f);
        addActor(messageLabel);

        buildProfileContent();
    }

    private Drawable createOverlayDrawable() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private void buildProfileContent() {
        contentTable.clear();
        User user = appState.getCurrentUser();

        if (user == null) {
            contentTable.add(new Label("No user is logged in.", skin)).center();
            return;
        }

        Table profileTitle = new Table();
        Image profileIcon = new Image(textures.region("IMAGE_UI_MAINMENU_MM_PLAYERICON"));
        profileIcon.setSize(28f, 28f);

        Label title = new Label("Profile", skin);
        title.setColor(Color.BROWN);
        title.setFontScale(1.2f);

        profileTitle.add(profileIcon).size(28f).padRight(6f);
        profileTitle.add(title).left();

        contentTable.add(profileTitle).left().row();

        addInfoLine("Username:", user.getUsername());
        addInfoLine("Nickname:", user.getNickname());
        addInfoLine("Games Played:", String.valueOf(user.getGamesPlayed()));
        addInfoLine("Coins:", String.valueOf(user.getCoins()));
        addInfoLine("Diamonds:", String.valueOf(user.getDiamonds()));
        addInfoLine("Passed Levels:", String.valueOf(user.getClearedStages()));
        addInfoLine("MewPoint:", String.valueOf(user.getMaxMewPoint()));

        addSeparator();

        addChangeField("Change Username", "New username", "Username cannot be empty.",
                value -> controller.handle(ProfileCommand.createChangeUsername(value)));
        addChangeField("Change Nickname", "New nickname", "Nickname cannot be empty.",
                value -> controller.handle(ProfileCommand.createChangeNickname(value)));
        addChangeField("Change Email", "New email", "Email cannot be empty.",
                value -> controller.handle(ProfileCommand.createChangeEmail(value)));

        addPasswordSection();
    }

    private void addInfoLine(String name, String value) {
        Label label = new Label(name + " " + value, skin);
        label.setColor(Color.BLACK);
        label.setFontScale(1.06f);
        contentTable.add(label).width(CONTENT_WIDTH).height(12f).left().row();
    }

    private void addSeparator() {
        Label separator = new Label("________________________________________", skin);
        separator.setColor(Color.BLACK);
        separator.setFontScale(0.75f);
        contentTable.add(separator).width(CONTENT_WIDTH).height(25f).left().row();
    }

    private void addChangeField(String titleText, String hint, String emptyMessage,
                                java.util.function.Consumer<String> action) {
        Label title = new Label(titleText, skin);
        title.setColor(Color.BROWN);
        title.setFontScale(1.05f);
        contentTable.add(title).width(CONTENT_WIDTH).height(25f).left().row();

        TextField field = new TextField("", skin);
        field.setMessageText(hint);

        TextButton save = new TextButton("Save", skin, "green");
        save.getLabel().setFontScale(0.85f);

        Table row = new Table();
        row.add(field).width(FIELD_WIDTH).height(34f).left();
        row.add(save).width(SAVE_WIDTH).height(34f).padLeft(8f);
        contentTable.add(row).width(CONTENT_WIDTH).height(38f).left().row();

        save.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                String value = field.getText().trim();
                if (value.isEmpty()) showError(emptyMessage);
                else action.accept(value);
            }
        });
    }

    private void addPasswordSection() {
        Label title = new Label("Change Password", skin);
        title.setColor(Color.BROWN);
        title.setFontScale(1.05f);
        contentTable.add(title).width(CONTENT_WIDTH).height(25f).left().row();

        TextField oldPassword = new TextField("", skin);
        oldPassword.setMessageText("Current password");
        oldPassword.setPasswordMode(true);
        oldPassword.setPasswordCharacter('*');
        contentTable.add(oldPassword).width(CONTENT_WIDTH).height(34f).left().row();

        TextField newPassword = new TextField("", skin);
        newPassword.setMessageText("New password");
        newPassword.setPasswordMode(true);
        newPassword.setPasswordCharacter('*');

        TextButton save = new TextButton("Save", skin, "green");
        save.getLabel().setFontScale(0.85f);

        Table row = new Table();
        row.add(newPassword).width(FIELD_WIDTH).height(34f).left();
        row.add(save).width(SAVE_WIDTH).height(34f).padLeft(8f);
        contentTable.add(row).width(CONTENT_WIDTH).height(38f).left().row();

        save.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                String oldValue = oldPassword.getText();
                String newValue = newPassword.getText();

                if (oldValue.isEmpty()) {
                    showError("Current password cannot be empty.");
                    return;
                }
                if (newValue.isEmpty()) {
                    showError("New password cannot be empty.");
                    return;
                }

                controller.handle(ProfileCommand.createChangePassword(newValue, oldValue));
            }
        });
    }

    private void showMessage(String message, Color color) {
        messageLabel.clearActions();
        messageLabel.setColor(color);
        messageLabel.setText(message == null ? "" : message);
        messageLabel.getColor().a = 1f;
        messageLabel.addAction(Actions.sequence(
                Actions.delay(2f),
                Actions.fadeOut(0.3f),
                Actions.run(() -> {
                    messageLabel.setText("");
                    messageLabel.getColor().a = 1f;
                })
        ));
    }

    private void showSuccess(String message) {
        showMessage(message == null ? "Success." : message, Color.GREEN);
    }

    private void showError(String message) {
        showMessage(message == null ? "An error occurred." : message, Color.RED);
    }

    private void showInfo(String message) {
        showMessage(message == null ? "" : message, Color.WHITE);
    }

    public void show() {
        if (appState.getCurrentUser() != null) buildProfileContent();

        if (overlay != null) {
            overlay.setSize(getWidth(), getHeight());
            overlay.getColor().a = OVERLAY_ALPHA;
        }

        setVisible(true);
        toFront();
    }

    public void hide() {
        setVisible(false);
    }
}
