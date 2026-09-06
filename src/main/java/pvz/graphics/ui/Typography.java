package pvz.graphics.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import java.util.Objects;

/** Shared readable typography helpers for menu UI. */
public final class Typography {

    private static final String BODY_FONT_NAME = "AVENIRNEXTLTPRO-DEMICN";

    private Typography() {
    }

    public static void applyBody(Label label, Skin skin) {
        Objects.requireNonNull(label, "label");
        Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
        style.font = resolveBodyFont(skin);
        label.setStyle(style);
    }

    public static void applyBody(TextButton button, Skin skin) {
        Objects.requireNonNull(button, "button");
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(
                button.getStyle()
        );
        style.font = resolveBodyFont(skin);
        button.setStyle(style);
    }

    /** Keep menu text readable in the 1280x720 virtual viewport.
     * Uses the actual font cap height, so different skin fonts remain legible.
     * Does not mutate shared BitmapFont data.
     */
    public static void setReadableScale(Label label, float requestedScale) {
        float capHeight = label.getStyle().font.getCapHeight();
        float minimumScale = capHeight > 0f ? 17f / capHeight : 1f;
        label.setFontScale(Math.max(requestedScale, minimumScale));
    }

    private static BitmapFont resolveBodyFont(Skin skin) {
        Objects.requireNonNull(skin, "skin");
        if (skin.has(BODY_FONT_NAME, BitmapFont.class)) {
            return skin.getFont(BODY_FONT_NAME);
        }
        return skin.get(Label.LabelStyle.class).font;
    }
}
