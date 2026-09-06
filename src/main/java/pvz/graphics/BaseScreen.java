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

    /** Shared currency header; debug controls are absent for ordinary players. */
    protected void addCurrencyBadge(String key,
            com.badlogic.gdx.scenes.scene2d.ui.Label label, float x, Runnable refresh) {
        var badge = new com.badlogic.gdx.scenes.scene2d.ui.Table();
        badge.setBackground(skin.newDrawable("image_ui_dialog_asset_inner_bkgd_10",
                com.badlogic.gdx.graphics.Color.valueOf("152c37")));
        badge.setBounds(x, HEIGHT - 79f, 156f, 54f);
        var source = textures.region(key);
        if (source != null) {
            Image icon = new Image(source);
            icon.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            badge.add(icon).size(38f);
        }
        pvz.graphics.ui.Typography.applyBody(label, skin);
        pvz.graphics.ui.Typography.setReadableScale(label, 1.05f);
        label.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        label.setAlignment(com.badlogic.gdx.utils.Align.center);
        label.setEllipsis(true);
        boolean debug = appState.getCurrentUser() != null
                && appState.getCurrentUser().isDebugMode();
        badge.add(label).width(debug ? 70f : 104f).height(42f);
        if (debug) {
            TextButton plus = new TextButton("+", skin, "green");
            plus.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float px, float py) {
                    event.stop();
                    var user = appState.getCurrentUser();
                    if (user == null || !user.isDebugMode()) return;
                    boolean gems = key.contains("GEM");
                    int before = gems ? user.getDiamonds() : user.getCoins();
                    if (before > Integer.MAX_VALUE - 100) return;
                    if (gems) user.addDiamonds(100); else user.addCoins(100);
                    if (!userManager.save()) {
                        if (gems) user.spendDiamonds(100); else user.spendCoins(100);
                        new com.badlogic.gdx.scenes.scene2d.ui.Dialog("Save failed", skin)
                                .text("Could not save currency. Please try again.")
                                .button("OK").show(stage);
                    }
                    refresh.run();
                }
            });
            pvz.graphics.ui.HoverEffect.addScale(plus);
            badge.add(plus).size(36f);
        }
        stage.addActor(badge);
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
