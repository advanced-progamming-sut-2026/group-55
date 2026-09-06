package pvz.graphics.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import java.util.Objects;
import pvz.graphics.ui.HoverEffect;

/** Small icon-first debug action used by the battle HUD. */
public final class BattleDebugActionActor extends Table implements Disposable {
    private static final Color BACKGROUND_TINT = Color.valueOf("f6efcf");
    private static final Color TEXT_COLOR = Color.valueOf("3c3323");

    private final Texture ownedTexture;
    private Runnable action;
    private final ClickListener clickListener;
    private boolean disposed;

    public BattleDebugActionActor(
            Skin skin,
            TextureRegion iconRegion,
            String caption,
            Runnable action
    ) {
        this(skin, iconRegion, null, caption, action);
    }

    public static BattleDebugActionActor fromInternalTexture(
            Skin skin,
            String path,
            String caption,
            Runnable action
    ) {
        Objects.requireNonNull(path, "icon path cannot be null");
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
        );
        return new BattleDebugActionActor(
                skin,
                new TextureRegion(texture),
                texture,
                caption,
                action
        );
    }

    private BattleDebugActionActor(
            Skin skin,
            TextureRegion iconRegion,
            Texture ownedTexture,
            String caption,
            Runnable action
    ) {
        Objects.requireNonNull(skin, "skin cannot be null");
        this.ownedTexture = ownedTexture;
        this.action = Objects.requireNonNull(action, "action cannot be null");
        setBackground(skin.newDrawable(
                "image_ui_dialog_asset_inner_bkgd_10",
                BACKGROUND_TINT
        ));
        pad(3f);

        Image icon = iconRegion == null ? new Image() : new Image(iconRegion);
        icon.setScaling(Scaling.fit);
        add(icon).size(31f, 31f).row();

        Label label = new Label(
                Objects.requireNonNull(caption, "caption cannot be null"),
                skin
        );
        label.setAlignment(Align.center);
        label.setFontScale(0.44f);
        label.setColor(TEXT_COLOR);
        add(label).height(15f).growX();

        clickListener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                if (!disposed && BattleDebugActionActor.this.action != null) {
                    BattleDebugActionActor.this.action.run();
                }
            }
        };
        addListener(clickListener);
        HoverEffect.addScale(this);
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        action = null;
        setTouchable(Touchable.disabled);
        removeListener(clickListener);
        clearActions();
        clearListeners();
        remove();
        if (ownedTexture != null) {
            ownedTexture.dispose();
        }
    }
}
