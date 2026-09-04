package pvz.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import pvz.libpvz.textures.TextureBank;
import pvz.model.account.UserManager;
import pvz.model.utils.AppState;

public abstract class BaseScreen extends ScreenAdapter {

    protected static final float WIDTH = 1280f;
    protected static final float HEIGHT = 720f;

    protected final PvzGame game;
    protected final TextureBank textures;
    protected final SpriteBatch batch;
    protected final Skin skin;
    protected final AppState appState;
    protected final UserManager userManager;

    protected final Viewport viewport;
    protected final Stage stage;
    protected final TextureRegion background;
    private boolean disposed;

    protected BaseScreen(
            PvzGame game,
            TextureBank textures,
            SpriteBatch batch,
            Skin skin,
            AppState appState,
            UserManager userManager,
            String backgroundName
    ) {
        this.game = game;
        this.textures = textures;
        this.batch = batch;
        this.skin = skin;
        this.appState = appState;
        this.userManager = userManager;

        viewport = new FitViewport(WIDTH, HEIGHT);
        stage = new Stage(viewport);

        background = textures.region(backgroundName);

        if (background == null) {
            throw new IllegalStateException(
                    "Texture not found: " + backgroundName
            );
        }
    }

    protected Image image(String name) {
        TextureRegion region = textures.region(name);

        if (region == null) {
            throw new IllegalStateException(
                    "Texture not found: " + name
            );
        }

        return new Image(region);
    }

    protected TextButton button(String text) {
        return new TextButton(text, skin);
    }

    protected void renderBackground() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(
                viewport.getCamera().combined
        );

        batch.begin();
        batch.draw(
                background,
                0f,
                0f,
                WIDTH,
                HEIGHT
        );
        batch.end();
    }

    protected void renderStage(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void render(float delta) {
        renderBackground();
        renderStage(delta);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        stage.cancelTouchFocus();
        stage.setScrollFocus(null);
        stage.setKeyboardFocus(null);
        stage.dispose();
    }
}
