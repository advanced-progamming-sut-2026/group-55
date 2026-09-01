package pvz.graphics.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import pvz.controller.RegisterController;
import pvz.graphics.BaseScreen;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.UserManager;
import pvz.model.command.RegisterCommand;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import pvz.view.MenuView;

public class RegisterScreen extends BaseScreen {

    private static final float FORM_WIDTH = 650f;
    private static final float FORM_HEIGHT = 620f;
    private static final float CONTENT_WIDTH = 332f;
    private static final float FIELD_HEIGHT = 38f;
    private static final float BUTTON_WIDTH = 160f;
    private static final float BUTTON_HEIGHT = 38f;
    private static final float BUTTON_GAP = 12f;

    private final RegisterController controller;
    private Table form;

    private int step = 1;
    private boolean stepOnePassed;
    private String username, password, passwordConfirm, nickname, email, gender;

    public RegisterScreen(
            Game game,
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

        controller = new RegisterController(
                appState,
                userManager,
                new MenuView() {
                    @Override
                    public void showError(String message) {
                        stepOnePassed = false;
                        RegisterScreen.this.showMessage(message);
                    }

                    @Override
                    public void showSuccess(String message) {
                        if (step == 1) {
                            stepOnePassed = true;
                        } else {
                            game.setScreen(new LoginScreen(
                                    game, textures, batch, skin, appState, userManager
                            ));
                        }
                    }

                    @Override
                    public void showRegisterWelcome() {
                    }

                    @Override
                    public void showMessage(String message) {
                        RegisterScreen.this.showMessage(message);
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
        form.defaults().pad(4);

        container.add(form).center();
        stage.addActor(container);

        buildStepOne();
    }

    private TextField createField(String hint, String value) {
        TextField field = new TextField(value == null ? "" : value, skin);
        field.setMessageText(hint);
        field.setSize(CONTENT_WIDTH, FIELD_HEIGHT);
        return field;
    }

    private void makePassword(TextField field) {
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
    }

    private Label createLabel(String text) {
        return new Label(text, skin);
    }

    private TextButton createButton(String text, String style) {
        return new TextButton(text, skin, style);
    }

    private void addTitle(String text) {
        form.add(new Label(text, skin, "big_outline"))
                .colspan(2)
                .center()
                .padBottom(12)
                .row();
    }

    private void addField(String labelText, TextField field) {
        form.add(createLabel(labelText))
                .width(CONTENT_WIDTH)
                .left()
                .padBottom(2)
                .colspan(2)
                .row();

        form.add(field)
                .width(CONTENT_WIDTH)
                .height(FIELD_HEIGHT)
                .colspan(2)
                .center()
                .padBottom(5)
                .row();
    }

    private Table createTwoButtons(TextButton left, TextButton right) {
        Table buttons = new Table();

        buttons.add(left)
                .width(BUTTON_WIDTH)
                .height(BUTTON_HEIGHT)
                .padRight(BUTTON_GAP);

        buttons.add(right)
                .width(BUTTON_WIDTH)
                .height(BUTTON_HEIGHT);

        return buttons;
    }

    private void addTwoButtons(TextButton left, TextButton right) {
        form.add(createTwoButtons(left, right))
                .width(CONTENT_WIDTH)
                .height(BUTTON_HEIGHT)
                .colspan(2)
                .center()
                .padTop(5)
                .row();
    }

    private void buildStepOne() {
        form.clear();
        addTitle("REGISTER");

        TextField usernameField = createField("Enter username", username);
        TextField passwordField = createField("Enter password", password);
        TextField passwordConfirmField = createField(
                "Confirm password", passwordConfirm
        );
        TextField nicknameField = createField("Enter nickname", nickname);
        TextField emailField = createField("Enter email", email);

        makePassword(passwordField);
        makePassword(passwordConfirmField);

        addField("Username:", usernameField);
        addField("Password:", passwordField);
        addField("Confirm:", passwordConfirmField);
        addField("Nickname:", nicknameField);
        addField("Email:", emailField);

        form.add(createLabel("Gender:"))
                .width(CONTENT_WIDTH)
                .left()
                .padBottom(2)
                .colspan(2)
                .row();

        TextButton genderButton = createButton(
                gender == null ? "Male" : gender,
                "green"
        );

        form.add(genderButton)
                .width(CONTENT_WIDTH)
                .height(FIELD_HEIGHT)
                .colspan(2)
                .center()
                .padBottom(6)
                .row();

        genderButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                genderButton.setText(
                        "Male".equals(genderButton.getText().toString())
                                ? "Female"
                                : "Male"
                );
            }
        });

        TextButton backButton = createButton("BACK", "brown");
        TextButton nextButton = createButton("NEXT", "green");

        addTwoButtons(backButton, nextButton);

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new LoginScreen(
                        game, textures, batch, skin, appState, userManager
                ));
            }
        });

        nextButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                username = usernameField.getText().trim();
                password = passwordField.getText();
                passwordConfirm = passwordConfirmField.getText();
                nickname = nicknameField.getText().trim();
                email = emailField.getText().trim();
                gender = genderButton.getText().toString();
                stepOnePassed = false;

                try {
                    controller.handle(RegisterCommand.createRegister(
                            username,
                            password,
                            passwordConfirm,
                            nickname,
                            email,
                            gender
                    ));

                    if (stepOnePassed) {
                        step = 2;
                        buildStepTwo();
                    }
                } catch (Exception e) {
                    showMessage(
                            e.getMessage() == null
                                    ? "Registration error."
                                    : e.getMessage()
                    );
                }
            }
        });
    }

    private void buildStepTwo() {
        form.clear();
        addTitle("SECURITY QUESTION");

        String[] questions = {
                "1. What is your favorite color?",
                "2. What is your pet's name?",
                "3. What city were you born in?"
        };

        TextButton questionButton = createButton(questions[0], "green");

        form.add(createLabel("Question:"))
                .width(CONTENT_WIDTH)
                .left()
                .padBottom(2)
                .colspan(2)
                .row();

        form.add(questionButton)
                .width(CONTENT_WIDTH)
                .height(FIELD_HEIGHT)
                .colspan(2)
                .center()
                .padBottom(5)
                .row();

        TextField answerField = createField("Enter answer", "");
        TextField confirmAnswerField = createField("Confirm answer", "");

        addField("Answer:", answerField);
        addField("Confirm:", confirmAnswerField);

        questionButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int current = getQuestionNumber(
                        questionButton.getText().toString()
                );
                int next = current % questions.length + 1;
                questionButton.setText(questions[next - 1]);
            }
        });

        TextButton backButton = createButton("BACK", "brown");
        TextButton registerButton = createButton("REGISTER", "green");

        addTwoButtons(backButton, registerButton);

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                step = 1;
                buildStepOne();
            }
        });

        registerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int questionNumber = getQuestionNumber(
                        questionButton.getText().toString()
                );

                String answer = answerField.getText().trim();
                String answerConfirm = confirmAnswerField.getText().trim();

                try {
                    controller.handle(
                            RegisterCommand.createPickQuestion(
                                    questionNumber,
                                    answer,
                                    answerConfirm
                            )
                    );
                } catch (Exception e) {
                    showMessage(
                            e.getMessage() == null
                                    ? "Registration error."
                                    : e.getMessage()
                    );
                }
            }
        });
    }

    private int getQuestionNumber(String question) {
        return Character.getNumericValue(question.charAt(0));
    }

    private void showMessage(String message) {
        Label label = new Label(
                message == null ? "Error" : message,
                skin
        );

        label.setColor(Color.BLACK);
        label.pack();
        label.setPosition(
                (WIDTH - label.getWidth()) / 2f,
                70f
        );

        stage.addActor(label);

        label.addAction(Actions.sequence(
                Actions.delay(3f),
                Actions.fadeOut(.3f),
                Actions.removeActor()
        ));
    }

    @Override
    public void show() {
        super.show();
        appState.setCurrentMenu(MenuName.REGISTER);
    }
}
