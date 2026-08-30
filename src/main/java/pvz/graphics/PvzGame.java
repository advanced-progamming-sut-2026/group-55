package pvz.graphics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
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

    private final AppState appState = new AppState();
    private final UserManager userManager = new UserManager("save.json");

    @Override
    public void create() {

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

    @Override
    public void dispose() {
        super.dispose();

        if (textures != null) {
            textures.dispose();
        }

        if (batch != null) {
            batch.dispose();
        }
    }
}
