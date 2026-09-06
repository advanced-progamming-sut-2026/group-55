package pvz.graphics.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.utils.Align;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Shared mouse interaction polish for Scene2D actors. */
public final class HoverEffect {

    private static final float DEFAULT_HOVER_MULTIPLIER = 1.05f;
    private static final float DEFAULT_PRESSED_MULTIPLIER = 0.96f;
    private static final float DEFAULT_DURATION = 0.08f;

    private HoverEffect() {
    }

    public static ScaleHandle addScale(Actor actor) {
        return addScale(actor, () -> true);
    }

    public static ScaleHandle addScale(Actor actor, BooleanSupplier enabled) {
        return new ScaleHandle(
                actor,
                enabled,
                DEFAULT_HOVER_MULTIPLIER,
                DEFAULT_PRESSED_MULTIPLIER,
                DEFAULT_DURATION
        );
    }

    /**
     * Keeps hover/press scale separate from the actor's logical base scale.
     * This matters for widgets whose selected state also changes scale.
     */
    public static final class ScaleHandle {

        private final Actor actor;
        private final BooleanSupplier enabled;
        private final float hoverMultiplier;
        private final float pressedMultiplier;
        private final float duration;

        private float baseScaleX;
        private float baseScaleY;
        private boolean hovered;
        private boolean pressed;
        private int pressedPointer = -1;
        private Action scaleAction;

        private ScaleHandle(
                Actor actor,
                BooleanSupplier enabled,
                float hoverMultiplier,
                float pressedMultiplier,
                float duration
        ) {
            this.actor = Objects.requireNonNull(actor, "actor");
            this.enabled = Objects.requireNonNull(enabled, "enabled");
            this.hoverMultiplier = hoverMultiplier;
            this.pressedMultiplier = pressedMultiplier;
            this.duration = duration;
            baseScaleX = actor.getScaleX();
            baseScaleY = actor.getScaleY();

            actor.setOrigin(Align.center);
            enableGroupTransforms(actor);
            actor.addListener(new HoverInputListener());
        }

        public void setBaseScale(float scale) {
            setBaseScale(scale, scale);
        }

        public void setBaseScale(float scaleX, float scaleY) {
            baseScaleX = scaleX;
            baseScaleY = scaleY;
            cancelScaleAction();
            float multiplier = currentMultiplier();
            actor.setScale(baseScaleX * multiplier, baseScaleY * multiplier);
        }

        public void reset() {
            hovered = false;
            pressed = false;
            pressedPointer = -1;
            cancelScaleAction();
            actor.setScale(baseScaleX, baseScaleY);
        }

        private void enableGroupTransforms(Actor target) {
            if (target instanceof Group) {
                ((Group) target).setTransform(true);
            }
        }

        private boolean isEnabled() {
            if (!enabled.getAsBoolean()
                    || !actor.isVisible()
                    || actor.getTouchable() == Touchable.disabled) {
                return false;
            }
            return !(actor instanceof Button) || !((Button) actor).isDisabled();
        }

        private boolean isPointerOver(float x, float y) {
            Actor hit = actor.hit(x, y, true);
            return hit != null
                    && (hit == actor || hit.isDescendantOf(actor));
        }

        private float currentMultiplier() {
            if (!isEnabled()) {
                return 1f;
            }
            if (pressed) {
                return pressedMultiplier;
            }
            return hovered ? hoverMultiplier : 1f;
        }

        private void animateTo(float multiplier) {
            cancelScaleAction();
            scaleAction = Actions.scaleTo(
                    baseScaleX * multiplier,
                    baseScaleY * multiplier,
                    duration,
                    Interpolation.sineOut
            );
            actor.addAction(scaleAction);
        }

        private void cancelScaleAction() {
            if (scaleAction != null) {
                actor.removeAction(scaleAction);
                scaleAction = null;
            }
        }

        private final class HoverInputListener extends InputListener {

            @Override
            public void enter(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor fromActor
            ) {
                if (pointer == -1 && isEnabled()) {
                    hovered = true;
                    if (!pressed) {
                        animateTo(hoverMultiplier);
                    }
                }
            }

            @Override
            public void exit(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor toActor
            ) {
                if (pointer == -1) {
                    hovered = false;
                    if (!pressed) {
                        animateTo(1f);
                    }
                }
            }

            @Override
            public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
            ) {
                if (!isEnabled()
                        || button != Input.Buttons.LEFT
                        || pressedPointer != -1) {
                    return false;
                }
                pressedPointer = pointer;
                pressed = true;
                animateTo(pressedMultiplier);
                return true;
            }

            @Override
            public void touchDragged(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer
            ) {
                if (pointer != pressedPointer) {
                    return;
                }

                boolean pressedNow = isEnabled() && isPointerOver(x, y);
                if (pressed != pressedNow) {
                    pressed = pressedNow;
                    animateTo(pressed ? pressedMultiplier : 1f);
                }
            }

            @Override
            public void touchUp(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
            ) {
                if (pointer != pressedPointer) {
                    return;
                }

                boolean overNow = isEnabled() && isPointerOver(x, y);
                pressedPointer = -1;
                pressed = false;
                hovered = overNow;
                animateTo(overNow ? hoverMultiplier : 1f);
            }
        }
    }
}
