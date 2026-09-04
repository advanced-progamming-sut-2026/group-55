package pvz.graphics.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import pvz.controller.LoginController;
import pvz.graphics.BaseScreen;
import pvz.graphics.PvzGame;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.command.LoginCommand;
import pvz.model.service.AuthService;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

public class LoginScreen extends BaseScreen {

    private static final float FORM_WIDTH = 650f;
    private static final float FORM_HEIGHT = 540f;
    private static final float CONTENT_WIDTH = 332f;
    private static final float FIELD_HEIGHT = 50f;
    private static final float BUTTON_WIDTH = 160f;
    private static final float BUTTON_HEIGHT = 55f;
    private static final float BUTTON_GAP = 12f;

    private final LoginController controller;
    private Table form;

    public LoginScreen(
            PvzGame game,
            TextureBank textures,
            com.badlogic.gdx.graphics.g2d.SpriteBatch batch,
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

        controller = new LoginController(
                appState,
                userManager,
                new AuthService(userManager),
                new MenuView() {
                    @Override
                    public void showError(String message) {
                        LoginScreen.this.showMessage(message);
                    }

                    @Override
                    public void showSuccess(String message) {
                        if (appState.getCurrentUser() != null) {
                            game.setScreen(new MainMenuScreen(
                                    game,
                                    textures,
                                    batch,
                                    skin,
                                    appState,
                                    userManager
                            ));
                        } else {
                            LoginScreen.this.showMessage(message);
                        }
                    }

                    @Override
                    public void showRegisterWelcome() {}

                    @Override
                    public void showMessage(String message) {
                        LoginScreen.this.showMessage(message);
                    }
                }
        );

        setupUI();
    }

    private void setupUI() {
        Table container = new Table();
        container.setBackground(
                skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10")
        );
        container.setSize(FORM_WIDTH, FORM_HEIGHT);
        container.setPosition(
                (WIDTH - FORM_WIDTH) / 2f,
                (HEIGHT - FORM_HEIGHT) / 2f
        );

        form = new Table();
        form.defaults().pad(8);
        container.add(form).center();

        stage.addActor(container);
        buildLoginForm();
    }

    private TextField createField(String hint) {
        TextField field = new TextField("", skin);
        field.setMessageText(hint);
        field.setSize(CONTENT_WIDTH, FIELD_HEIGHT);
        return field;
    }

    private void makePassword(TextField field) {
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
    }

    private void addTitle(String text) {
        form.add(new Label(text, skin, "big_outline"))
                .colspan(2)
                .center()
                .padBottom(20)
                .row();
    }

    private void addField(String labelText, TextField field) {
        form.add(new Label(labelText, skin))
                .width(CONTENT_WIDTH)
                .left()
                .padBottom(3)
                .colspan(2)
                .row();

        form.add(field)
                .width(CONTENT_WIDTH)
                .height(FIELD_HEIGHT)
                .colspan(2)
                .center()
                .padBottom(8)
                .row();
    }

    private void addTwoButtons(TextButton left, TextButton right) {
        Table buttons = new Table();

        buttons.add(left)
                .width(BUTTON_WIDTH)
                .height(BUTTON_HEIGHT)
                .padRight(BUTTON_GAP);

        buttons.add(right)
                .width(BUTTON_WIDTH)
                .height(BUTTON_HEIGHT);

        form.add(buttons)
                .width(CONTENT_WIDTH)
                .height(BUTTON_HEIGHT)
                .colspan(2)
                .center()
                .row();
    }

    private void addFullButton(TextButton button) {
        form.add(button)
                .width(CONTENT_WIDTH)
                .height(BUTTON_HEIGHT)
                .colspan(2)
                .center()
                .row();
    }

    private void buildLoginForm() {
        controller.cancelRecovery();
        form.clear();
        addTitle("LOGIN");

        TextField usernameField = createField("Enter username");
        TextField passwordField = createField("Enter password");
        makePassword(passwordField);

        addField("Username:", usernameField);
        addField("Password:", passwordField);

        CheckBox stayLoggedIn = new CheckBox(" Stay logged in", skin);

        form.add(stayLoggedIn)
                .colspan(2)
                .center()
                .padTop(5)
                .padBottom(12)
                .row();

        TextButton loginButton = new TextButton("LOGIN", skin, "green");
        TextButton registerButton = new TextButton("REGISTER", skin, "brown");
        addTwoButtons(loginButton, registerButton);

        TextButton forgotButton =
                new TextButton("FORGOT PASSWORD", skin, "green");
        addFullButton(forgotButton);

        loginButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String username = usernameField.getText().trim();
                String password = passwordField.getText();

                if (username.isEmpty() || password.isEmpty()) {
                    showMessage("Username and password cannot be empty.");
                    return;
                }

                try {
                    controller.handle(
                            LoginCommand.createLogin(
                                    username,
                                    password,
                                    stayLoggedIn.isChecked()
                            )
                    );
                } catch (Exception e) {
                    showMessage(
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Login failed."
                    );
                }
            }
        });

        registerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new RegisterScreen(
                        game,
                        textures,
                        batch,
                        skin,
                        appState,
                        userManager
                ));
            }
        });

        forgotButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                buildForgotPasswordForm();
            }
        });
    }

    private void buildForgotPasswordForm() {
        form.clear();
        addTitle("FORGOT PASSWORD");

        TextField usernameField = createField("Enter username");
        TextField emailField = createField("Enter email");

        addField("Username:", usernameField);
        addField("Email:", emailField);

        TextButton backButton = new TextButton("BACK", skin, "brown");
        TextButton continueButton =
                new TextButton("CONTINUE", skin, "green");

        addTwoButtons(backButton, continueButton);

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                buildLoginForm();
            }
        });

        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String username = usernameField.getText().trim();
                String email = emailField.getText().trim();

                if (username.isEmpty() || email.isEmpty()) {
                    showMessage("Username and email cannot be empty.");
                    return;
                }

                try {
                    controller.handle(
                            LoginCommand.createForgetPassword(
                                    username,
                                    email
                            )
                    );

                    if (controller.isRecoveryUserFound()) {
                        buildAnswerForm();
                    }
                } catch (Exception e) {
                    showMessage(
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Password recovery failed."
                    );
                }
            }
        });
    }

    private void buildAnswerForm() {
        form.clear();
        addTitle("SECURITY QUESTION");

        form.add(new Label(
                        "Enter the answer to your security question:",
                        skin
                ))
                .width(CONTENT_WIDTH)
                .colspan(2)
                .center()
                .padBottom(12)
                .row();

        TextField answerField = createField("Enter answer");
        addField("Answer:", answerField);

        TextButton backButton = new TextButton("BACK", skin, "brown");
        TextButton continueButton =
                new TextButton("CONTINUE", skin, "green");

        addTwoButtons(backButton, continueButton);

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                buildLoginForm();
            }
        });

        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String answer = answerField.getText().trim();

                if (answer.isEmpty()) {
                    showMessage("Answer cannot be empty.");
                    return;
                }

                try {
                    controller.handle(
                            LoginCommand.createAnswer(answer)
                    );

                    if (controller.isWaitingForNewPassword()) {
                        buildNewPasswordForm();
                    }
                } catch (Exception e) {
                    showMessage(
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Incorrect answer."
                    );
                }
            }
        });
    }

    private void buildNewPasswordForm() {
        form.clear();
        addTitle("NEW PASSWORD");

        TextField passwordField =
                createField("Enter new password");
        TextField confirmField =
                createField("Confirm new password");

        makePassword(passwordField);
        makePassword(confirmField);

        addField("Password:", passwordField);
        addField("Confirm:", confirmField);

        TextButton backButton =
                new TextButton("BACK", skin, "brown");
        TextButton changeButton =
                new TextButton("CHANGE PASSWORD", skin, "green");

        addTwoButtons(backButton, changeButton);

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                buildLoginForm();
            }
        });

        changeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String password = passwordField.getText();
                String confirm = confirmField.getText();

                if (password.isEmpty() || confirm.isEmpty()) {
                    showMessage("Password fields cannot be empty.");
                    return;
                }

                if (!password.equals(confirm)) {
                    showMessage("Passwords do not match.");
                    return;
                }

                try {
                    controller.handle(
                            new Command.RawTextCommand(password)
                    );

                    if (!controller.isWaitingForNewPassword()) {
                        buildLoginForm();
                    }
                } catch (Exception e) {
                    showMessage(
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Password change failed."
                    );
                }
            }
        });
    }

    private void showMessage(String message) {
        Label label = new Label(
                message == null
                        ? SystemMessage.INVALID_COMMAND.getMessage()
                        : message,
                skin
        );

        label.setColor(Color.BLACK);
        label.pack();
        label.setPosition(
                (WIDTH - label.getWidth()) / 2f,
                70f
        );

        stage.addActor(label);

        label.addAction(
                Actions.sequence(
                        Actions.delay(3f),
                        Actions.fadeOut(0.3f),
                        Actions.removeActor()
                )
        );
    }

    @Override
    public void show() {
        super.show();
        appState.setCurrentMenu(MenuName.LOGIN);
    }
}
