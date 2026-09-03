package pvz.graphics.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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
import pvz.graphics.battle.BattleToolState;

/** Graphical battle tool button with an icon and explicit state text. */
public final class BattleToolButtonActor extends Table implements Disposable {
    private static final Color READY_COLOR = Color.WHITE;
    private static final Color SELECTED_COLOR =
            new Color(1f, 0.80f, 0.18f, 1f);
    private static final Color UNAVAILABLE_COLOR =
            new Color(0.46f, 0.46f, 0.46f, 1f);
    private static final Color ACTIVE_TEXT_COLOR =
            new Color(0.12f, 0.48f, 0.08f, 1f);

    private final Texture iconTexture;
    private final Image icon;
    private final Label titleLabel;
    private final Label statusLabel;
    private Runnable action;
    private final ClickListener clickListener;
    private boolean disposed;

    private BattleToolState.ButtonView state =
            new BattleToolState.ButtonView(false, true, "READY");

    public BattleToolButtonActor(
            Skin skin,
            IconType iconType,
            String title,
            Runnable action
    ) {
        Objects.requireNonNull(skin, "skin cannot be null");
        this.action = Objects.requireNonNull(action, "action cannot be null");

        setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        pad(4f);

        iconTexture = createIconTexture(Objects.requireNonNull(
                iconType,
                "icon type cannot be null"
        ));
        icon = new Image(iconTexture);
        icon.setScaling(Scaling.fit);
        add(icon).size(38f, 38f).padRight(5f);

        Table text = new Table();
        titleLabel = label(skin, title, 0.72f);
        text.add(titleLabel).height(21f).growX().row();
        statusLabel = label(skin, "READY", 0.60f);
        text.add(statusLabel).height(17f).growX();
        add(text).grow();

        clickListener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                if (!disposed && state.available() && action != null) {
                    action.run();
                }
            }
        };
        addListener(clickListener);
        applyState();
    }

    public void update(BattleToolState.ButtonView state) {
        if (disposed) {
            return;
        }
        this.state = Objects.requireNonNull(state, "state cannot be null");
        applyState();
    }

    private void applyState() {
        Color tint = !state.available()
                ? UNAVAILABLE_COLOR
                : state.selected() ? SELECTED_COLOR : READY_COLOR;
        setColor(tint);
        icon.setColor(state.available() ? Color.WHITE : Color.GRAY);
        titleLabel.setColor(state.available()
                ? Color.DARK_GRAY
                : Color.LIGHT_GRAY);
        statusLabel.setColor(state.selected()
                ? ACTIVE_TEXT_COLOR
                : state.available() ? Color.DARK_GRAY : Color.LIGHT_GRAY);
        statusLabel.setText(state.statusText());
        setTouchable(state.available()
                ? Touchable.enabled
                : Touchable.disabled);
    }

    private static Label label(Skin skin, String text, float fontScale) {
        Label label = new Label(text, skin);
        label.setAlignment(Align.center);
        label.setFontScale(fontScale);
        return label;
    }

    private static Texture createIconTexture(IconType iconType) {
        Pixmap pixmap = new Pixmap(48, 48, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        pixmap.setColor(Color.CLEAR);
        pixmap.fill();

        if (iconType == IconType.SHOVEL) {
            drawShovel(pixmap);
        } else {
            drawPlantFood(pixmap);
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private static void drawShovel(Pixmap pixmap) {
        pixmap.setColor(0.42f, 0.20f, 0.07f, 1f);
        for (int offset = -2; offset <= 2; offset++) {
            pixmap.drawLine(13 + offset, 8, 34 + offset, 31);
        }
        pixmap.setColor(0.90f, 0.62f, 0.18f, 1f);
        pixmap.fillCircle(12, 8, 6);
        pixmap.setColor(0.72f, 0.78f, 0.82f, 1f);
        pixmap.fillTriangle(29, 28, 45, 36, 36, 46);
        pixmap.fillTriangle(29, 28, 36, 46, 25, 39);
        pixmap.setColor(0.30f, 0.34f, 0.38f, 1f);
        pixmap.drawLine(29, 28, 45, 36);
        pixmap.drawLine(45, 36, 36, 46);
        pixmap.drawLine(36, 46, 25, 39);
    }

    private static void drawPlantFood(Pixmap pixmap) {
        pixmap.setColor(0.30f, 0.82f, 0.12f, 1f);
        pixmap.fillCircle(21, 27, 13);
        pixmap.fillCircle(29, 19, 11);
        pixmap.fillTriangle(10, 37, 39, 10, 34, 33);
        pixmap.setColor(0.08f, 0.42f, 0.06f, 1f);
        for (int offset = -1; offset <= 1; offset++) {
            pixmap.drawLine(12, 38 + offset, 37, 13 + offset);
        }
        pixmap.setColor(0.74f, 1f, 0.38f, 1f);
        pixmap.fillCircle(19, 21, 4);
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
        iconTexture.dispose();
    }

    public enum IconType {
        SHOVEL,
        PLANT_FOOD
    }
}
