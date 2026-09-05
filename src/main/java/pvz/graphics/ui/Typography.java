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

    private static BitmapFont resolveBodyFont(Skin skin) {
        Objects.requireNonNull(skin, "skin");
        if (skin.has(BODY_FONT_NAME, BitmapFont.class)) {
            return skin.getFont(BODY_FONT_NAME);
        }
        return skin.get(Label.LabelStyle.class).font;
    }
}
