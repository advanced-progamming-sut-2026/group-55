package pvz.graphics.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.Group;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pvz.model.wave.WaveProgressSnapshot;

/** Compact stage-progress strip with one marker for each wave boundary. */
public final class BattleWaveProgressActor extends Group {
    private static final Color TRACK_COLOR = Color.valueOf("2d3b3f");
    private static final Color FILL_COLOR = Color.valueOf("79b84a");
    private static final Color MARKER_COLOR = Color.valueOf("f1dfad");

    private final Skin skin;
    private final Table track = new Table();
    private final Table fill = new Table();
    private final List<Table> markers = new ArrayList<>();
    private int totalWaves;
    private double progress;

    public BattleWaveProgressActor(Skin skin, int totalWaves) {
        this.skin = Objects.requireNonNull(skin, "skin cannot be null");
        setTouchable(Touchable.disabled);
        track.setBackground(skin.newDrawable(
                "image_ui_dialog_asset_inner_bkgd_10",
                TRACK_COLOR
        ));
        fill.setBackground(skin.newDrawable(
                "image_ui_dialog_asset_inner_bkgd_10",
                FILL_COLOR
        ));
        addActor(track);
        addActor(fill);
        setTotalWaves(totalWaves);
    }

    public void update(WaveProgressSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "wave progress cannot be null");
        if (snapshot.totalWaves() != totalWaves) {
            setTotalWaves(snapshot.totalWaves());
        }
        progress = snapshot.overallProgress();
        layoutParts();
    }

    @Override
    protected void sizeChanged() {
        layoutParts();
    }

    private void setTotalWaves(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("total waves must be positive");
        }
        totalWaves = value;
        rebuildMarkers();
        layoutParts();
    }

    private void rebuildMarkers() {
        for (Table marker : markers) {
            marker.remove();
        }
        markers.clear();
        for (int index = 1; index < totalWaves; index++) {
            Table marker = new Table();
            marker.setBackground(skin.newDrawable(
                    "image_ui_dialog_asset_inner_bkgd_10",
                    MARKER_COLOR
            ));
            marker.setTouchable(Touchable.disabled);
            markers.add(marker);
            addActor(marker);
        }
    }

    private void layoutParts() {
        float width = getWidth();
        float height = getHeight();
        if (width <= 0f || height <= 0f) {
            return;
        }
        track.setBounds(0f, 0f, width, height);
        fill.setBounds(0f, 0f, width * (float) progress, height);
        for (int index = 0; index < markers.size(); index++) {
            float x = width * (index + 1f) / totalWaves;
            markers.get(index).setBounds(x - 1.5f, -2f, 3f, height + 4f);
        }
    }
}
