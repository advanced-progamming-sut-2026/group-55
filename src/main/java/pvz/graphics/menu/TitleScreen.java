package pvz.graphics.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

import pvz.graphics.BaseScreen;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class TitleScreen extends BaseScreen {

    private static final float SHOW_TIME = 3f;
    private static final float LOADING_X = 600f;
    private static final float LOADING_Y = 80f;

    private final Label loadingLabel;

    private float timer;
    private float loadingTimer;
    private int dots;

    public TitleScreen(
            Game game,
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
                "IMAGE_TITLEBACKGROUNDS_BACKDROP_B"
        );

        loadingLabel = new Label("Loading.", skin);
        loadingLabel.setColor(Color.WHITE);
        loadingLabel.setPosition(LOADING_X, LOADING_Y);

        stage.addActor(loadingLabel);
    }

    @Override
    public void show() {
        super.show();

        timer = 0f;
        loadingTimer = 0f;
        dots = 1;
        loadingLabel.setText("Loading.");
    }

    @Override
    public void render(float delta) {
        timer += delta;
        loadingTimer += delta;

        if (loadingTimer >= 0.4f) {
            loadingTimer = 0f;
            dots = dots % 3 + 1;

            StringBuilder text = new StringBuilder("Loading");

            for (int i = 0; i < dots; i++) {
                text.append(".");
            }

            loadingLabel.setText(text.toString());
        }

        renderBackground();
        renderStage(delta);

        if (timer >= SHOW_TIME) {
            goToNextScreen();
        }
    }

    private void goToNextScreen() {
        User activeUser = userManager.find(User::isStayLoggedIn);

        if (activeUser != null) {
            appState.setCurrentUser(activeUser);
            appState.setCurrentMenu(MenuName.MAIN);

            game.setScreen(
                    new MainMenuScreen(
                            game,
                            textures,
                            batch,
                            skin,
                            appState,
                            userManager
                    )
            );
        } else {
            game.setScreen(
                    new LoginScreen(
                            game,
                            textures,
                            batch,
                            skin,
                            appState,
                            userManager
                    )
            );
        }
    }
}
