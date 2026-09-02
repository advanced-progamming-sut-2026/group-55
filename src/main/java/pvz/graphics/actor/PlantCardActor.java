package pvz.graphics.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import java.util.Objects;
import pvz.graphics.asset.PamAnimationService;

public final class PlantCardActor extends Table {
    public static final float CARD_WIDTH = 210f;
    public static final float CARD_HEIGHT = 292f;

    public record Model(
            String name,
            int level,
            int sunCost,
            String seedProgress,
            boolean owned,
            boolean selected,
            boolean boosted,
            TextureRegion preview,
            String pamPath,
            String animationClip
    ) {
    }

    private static final float PREVIEW_SIZE = 68f;
    private static final float PREVIEW_PADDING = 8f;
    private static final float CONTENT_WIDTH = 180f;
    private static final float HALF_BUTTON_WIDTH = 88f;

    public PlantCardActor(
            Skin skin,
            PamAnimationService animationService,
            Model model,
            Runnable selectionAction,
            Runnable boostAction,
            Runnable upgradeAction
    ) {
        Objects.requireNonNull(skin, "skin cannot be null");
        Objects.requireNonNull(
                animationService,
                "animation service cannot be null"
        );
        Objects.requireNonNull(model, "model cannot be null");

        setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        setColor(model.selected() ? Color.GOLD : Color.WHITE);
        setClip(true);
        defaults().pad(1f);

        if (model.pamPath() != null) {
            add(createAnimatedPreview(
                    animationService,
                    model.pamPath(),
                    model.animationClip(),
                    model.preview()
            ))
                    .size(PREVIEW_SIZE)
                    .colspan(2)
                    .pad(PREVIEW_PADDING)
                    .row();
        } else if (model.preview() == null) {
            Label missingPreview = new Label("PREVIEW NEEDED", skin);
            missingPreview.setColor(Color.GRAY);
            missingPreview.setAlignment(Align.center);
            add(missingPreview)
                    .size(PREVIEW_SIZE)
                    .colspan(2)
                    .pad(PREVIEW_PADDING)
                    .row();
        } else {
            add(createPreview(model.preview()))
                    .size(PREVIEW_SIZE)
                    .colspan(2)
                    .pad(PREVIEW_PADDING)
                    .row();
        }

        Label name = new Label(model.name(), skin);
        name.setAlignment(Align.center);
        name.setWrap(true);
        name.setColor(model.owned() ? Color.DARK_GRAY : Color.GRAY);
        add(name)
                .width(CONTENT_WIDTH)
                .height(32f)
                .colspan(2)
                .row();

        String details = "Lvl " + model.level()
                + "   Sun " + model.sunCost()
                + "\nSeeds " + model.seedProgress();
        Label detailLabel = new Label(details, skin);
        detailLabel.setAlignment(Align.center);
        detailLabel.setColor(model.owned() ? Color.DARK_GRAY : Color.GRAY);
        add(detailLabel)
                .width(CONTENT_WIDTH)
                .height(40f)
                .colspan(2)
                .row();

        Label state = new Label(stateText(model), skin);
        state.setColor(model.owned() ? Color.DARK_GRAY : Color.GRAY);
        state.setAlignment(Align.center);
        add(state)
                .width(CONTENT_WIDTH)
                .height(24f)
                .colspan(2)
                .row();

        TextButton select = new TextButton(
                model.selected() ? "REMOVE" : "SELECT",
                skin,
                model.selected() ? "brown" : "green"
        );
        select.setDisabled(!model.owned());
        select.addListener(click(selectionAction));
        add(select).size(HALF_BUTTON_WIDTH, 42f);

        TextButton boost = new TextButton("BOOST", skin, "brown");
        boost.setDisabled(!model.owned() || model.boosted());
        boost.addListener(click(boostAction));
        add(boost).size(HALF_BUTTON_WIDTH, 42f).row();

        TextButton upgrade = new TextButton("UPGRADE", skin, "brown");
        upgrade.setDisabled(!model.owned());
        upgrade.addListener(click(upgradeAction));
        add(upgrade)
                .size(CONTENT_WIDTH, 42f)
                .colspan(2)
                .padBottom(4f);
    }

    private static Image createPreview(TextureRegion region) {
        Image preview = new Image(region);
        preview.setScaling(Scaling.fit);
        return preview;
    }

    private Actor createAnimatedPreview(
            PamAnimationService animationService,
            String pamPath,
            String animationClip,
            TextureRegion fallback
    ) {
        return new LazyAnimatedPreview(
                animationService,
                pamPath,
                animationClip,
                fallback
        );
    }

    private static Actor fallbackPreview(TextureRegion fallback) {
        if (fallback != null) {
            return createPreview(fallback);
        }
        Group empty = new Group();
        empty.setSize(PREVIEW_SIZE, PREVIEW_SIZE);
        return empty;
    }

    private static final class LazyAnimatedPreview extends Group {
        private final PamAnimationService animationService;
        private final String pamPath;
        private final String animationClip;
        private boolean requested;
        private boolean initialized;

        private LazyAnimatedPreview(
                PamAnimationService animationService,
                String pamPath,
                String animationClip,
                TextureRegion fallback
        ) {
            this.animationService = animationService;
            this.pamPath = pamPath;
            this.animationClip = animationClip;
            setSize(PREVIEW_SIZE, PREVIEW_SIZE);
            addActor(fallbackPreview(fallback));
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (!requested && !initialized) {
                requested = true;
                animationService.prepare(
                        pamPath,
                        animationClip,
                        this::initializeAnimation
                );
            }
            super.draw(batch, parentAlpha);
        }

        private void initializeAnimation(Rectangle bounds) {
            if (initialized || getStage() == null) {
                return;
            }
            if (bounds == null || bounds.width <= 0f || bounds.height <= 0f) {
                initialized = true;
                return;
            }

            float usableSize = PREVIEW_SIZE * 0.86f;
            float scale = Math.min(
                    usableSize / bounds.width,
                    usableSize / bounds.height
            );
            float center = PREVIEW_SIZE / 2f;

            Group scaler = new Group();
            scaler.setTransform(true);
            scaler.setSize(PREVIEW_SIZE, PREVIEW_SIZE);
            scaler.setOrigin(center, center);
            scaler.setScale(scale);

            PlantActor plant = new PlantActor(
                    animationService.player(),
                    pamPath,
                    animationClip
            );
            plant.setSize(PREVIEW_SIZE, PREVIEW_SIZE);
            plant.setPosition(
                    -bounds.x - bounds.width / 2f,
                    center - 45f + bounds.y + bounds.height / 2f
            );

            clearChildren();
            scaler.addActor(plant);
            addActor(scaler);
            initialized = true;
        }
    }

    private String stateText(Model model) {
        if (!model.owned()) {
            return "LOCKED";
        }
        if (model.boosted()) {
            return "BOOSTED";
        }
        return model.selected() ? "SELECTED" : "AVAILABLE";
    }

    private ClickListener click(Runnable action) {
        return new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                action.run();
            }
        };
    }
}
