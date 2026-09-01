package pvz.graphics;

import java.io.IOException;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import pvz.graphics.menu.TitleScreen;
import pvz.libpvz.textures.TextureBank;
import pvz.model.account.UserManager;
import pvz.model.utils.AppState;
import pvz.skin.PvzSkin;

public class PvzGame extends Game {

    private SpriteBatch batch;
    private TextureBank textures;
    private Skin skin;
    private GameDataContext gameData;

    private final AppState appState = new AppState();
    private final UserManager userManager = new UserManager("save.json");

    @Override
    public void create() {

        try {
            gameData = GameDataContext.loadDefault();
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Failed to load game data.",
                    exception
            );
        }

        batch = new SpriteBatch();
        skin = PvzSkin.get();

        FileHandle assetsFolder=Gdx.files.internal("assets");

        textures = new TextureBank(
                "768",
                assetsFolder
        );

        textures.loadSync(
                "ATLASIMAGE_ATLAS_TITLESCREEN2_768_00"
        );
        setScreen(
                new TitleScreen(
                        this,
                        textures,
                        batch,
                        skin,
                        appState,
                        userManager
                )
        );
    }

    public TextureBank getTextures() {
        return textures;
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public Skin getSkin() {
        return skin;
    }

    public AppState getAppState() {
        return appState;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GameDataContext getGameData() {
        if (gameData == null) {
            throw new IllegalStateException("Game data is not loaded.");
        }
        return gameData;
    }

    @Override
    public void setScreen(Screen screen) {
        Screen previousScreen = getScreen();
        super.setScreen(screen);

        if (previousScreen != null && previousScreen != screen) {
            previousScreen.dispose();
        }
    }

    @Override
    public void dispose() {
        Screen currentScreen = getScreen();
        super.dispose();

        if (currentScreen != null) {
            currentScreen.dispose();
        }

        if (textures != null) {
            textures.dispose();
        }

        if (batch != null) {
            batch.dispose();
        }

        if (skin != null) {
            skin.dispose();
        }
    }
}
