package pvz.graphics.actor;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import pvz.graphics.asset.PamAnimationRenderer;
import pvz.graphics.asset.PamAnimationService;
import pvz.graphics.asset.PlantVisualResolver;
import pvz.graphics.asset.ProjectileVisualResolver;
import pvz.graphics.asset.ZombieVisualResolver;
import pvz.libpvz.textures.TextureBank;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.Tile;
import pvz.model.core.board.TileType;
import pvz.model.entity.Entity;
import pvz.model.entity.collectible.Collectible;
import pvz.model.entity.collectible.plantfood.PlantFood;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.behavior.capability.PlantActivationCapability;
import pvz.model.entity.projectile.BowlingBulbProjectile;
import pvz.model.entity.projectile.LobbedProjectile;
import pvz.model.entity.zombie.Zombie;
import pvz.model.session.GameSession;

/** Draws the live battle snapshot and translates pointer positions to cells. */
public final class BattlefieldActor extends Actor implements Disposable {
    private static final float ZOMBIE_RENDER_SCALE = 1.5f;
    private static final String TOMBSTONE_TEXTURE =
            "IMAGE_GRAVESTONES_EGYPT_HIEROGLYPH_EGYPT_HIEROGLYPH_109X119";
    private static final String MOWER_TEXTURE =
            "IMAGE_MOWERS_MOWER_EGYPT_MOWER_EGYPT_96X63";
    private static final String SUN_TEXTURE = "IMAGE_EFFECTS_SUN_SUN_78X78";

    private final GameSession session;
    private final Board board;
    private final World world;
    private final Texture solidTexture;
    private final TextureRegion solid;
    private final TextureRegion tombstone;
    private final TextureRegion mower;
    private final TextureRegion sun;
    private final BitmapFont font;
    private final PlantVisualResolver plantVisuals;
    private final ZombieVisualResolver zombieVisuals;
    private final ProjectileVisualResolver projectileVisuals =
            new ProjectileVisualResolver();
    private final TextureBank textures;
    private final PamAnimationService animationService;
    private final PamAnimationRenderer animationRenderer;
    private final CellListener cellListener;
    private final Map<String, TextureRegion> plantPreviewCache = new HashMap<>();
    private final Map<PlantAnimationKey, String> plantClipCache = new HashMap<>();
    private final Set<PlantAnimationKey> plantClipRequests = new HashSet<>();
    private final Map<Plant, PlantPlayback> plantPlaybacks =
            new IdentityHashMap<>();
    private final Map<Zombie, ZombiePlayback> zombiePlaybacks =
            new IdentityHashMap<>();
    private final List<ZombieDeathPlayback> zombieDeaths = new ArrayList<>();
    private final Map<Entity, ProjectilePlayback> projectilePlaybacks =
            new IdentityHashMap<>();
    private final List<ProjectileImpactPlayback> projectileImpacts =
            new ArrayList<>();
    private final Map<String, TextureRegion> projectileTextureCache =
            new HashMap<>();
    private final Color oldBatchColor = new Color();
    private final Color oldFontColor = new Color();

    private int hoverColumn = -1;
    private int hoverRow = -1;
    private String selectedPlant;
    private ToolMode toolMode = ToolMode.PLANT;
    private boolean showGrid;

    public BattlefieldActor(
            GameSession session,
            TextureBank textures,
            BitmapFont font,
            PlantVisualResolver plantVisuals,
            ZombieVisualResolver zombieVisuals,
            PamAnimationService animationService,
            CellListener cellListener
    ) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.board = session.board();
        this.world = session.world();
        this.textures = Objects.requireNonNull(
                textures,
                "textures cannot be null"
        );
        this.font = Objects.requireNonNull(font, "font cannot be null");
        this.plantVisuals = Objects.requireNonNull(
                plantVisuals,
                "plant visuals cannot be null"
        );
        this.zombieVisuals = Objects.requireNonNull(
                zombieVisuals,
                "zombie visuals cannot be null"
        );
        this.animationService = Objects.requireNonNull(
                animationService,
                "animation service cannot be null"
        );
        this.animationRenderer = new PamAnimationRenderer(animationService);
        this.cellListener = Objects.requireNonNull(
                cellListener,
                "cell listener cannot be null"
        );

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        solidTexture = new Texture(pixmap);
        pixmap.dispose();
        solid = new TextureRegion(solidTexture);

        tombstone = textures.region(TOMBSTONE_TEXTURE);
        mower = textures.region(MOWER_TEXTURE);
        sun = textures.region(SUN_TEXTURE);

        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        addListener(createInputListener());
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        float elapsed = Math.max(0f, delta);
        plantPlaybacks.values().forEach(playback -> playback.advance(elapsed));

        Set<Plant> livePlants = Collections.newSetFromMap(
                new IdentityHashMap<>()
        );
        livePlants.addAll(world.getPlants());
        plantPlaybacks.keySet().removeIf(plant -> !livePlants.contains(plant));

        long tick = session.game().getCurrentTick();
        Set<Zombie> liveZombies = Collections.newSetFromMap(
                new IdentityHashMap<>()
        );
        liveZombies.addAll(world.getZombies());

        Iterator<Map.Entry<Zombie, ZombiePlayback>> iterator =
                zombiePlaybacks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Zombie, ZombiePlayback> entry = iterator.next();
            Zombie zombie = entry.getKey();
            if (!liveZombies.contains(zombie)) {
                if (zombie.isDead()) {
                    addDeathPlayback(zombie, tick);
                }
                iterator.remove();
                continue;
            }
            entry.getValue().advance(zombie, elapsed, tick);
        }

        zombieDeaths.forEach(death -> death.advance(elapsed));
        zombieDeaths.removeIf(ZombieDeathPlayback::finished);

        updateProjectilePlaybacks(elapsed);
    }

    public void setSelectedPlant(String selectedPlant) {
        this.selectedPlant = selectedPlant;
    }

    public void setToolMode(ToolMode toolMode) {
        this.toolMode = Objects.requireNonNull(toolMode);
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        oldBatchColor.set(batch.getColor());
        drawLawn(batch, parentAlpha);
        drawTombstones(batch, parentAlpha);
        drawMowers(batch, parentAlpha);
        drawPlants(batch, parentAlpha);
        drawZombies(batch, parentAlpha);
        drawProjectiles(batch, parentAlpha);
        drawProjectileImpacts(batch, parentAlpha);
        drawCollectibles(batch, parentAlpha);
        drawHover(batch, parentAlpha);
        batch.setColor(oldBatchColor);
    }

    private void drawLawn(Batch batch, float parentAlpha) {
        if (!showGrid) {
            return;
        }
        float cellWidth = cellWidth();
        float cellHeight = cellHeight();
        for (int column = 1; column <= board.getCols(); column++) {
            for (int row = 1; row <= board.getRows(); row++) {
                float left = cellLeft(column);
                float bottom = cellBottom(row);
                boolean light = (column + row) % 2 == 0;
                setColor(batch, light ? 0.78f : 0.62f,
                        light ? 0.92f : 0.78f, 0.55f,
                        (light ? 0.08f : 0.045f) * parentAlpha);
                batch.draw(solid, left, bottom, cellWidth, cellHeight);

                setColor(batch, 1f, 1f, 1f, 0.28f * parentAlpha);
                batch.draw(solid, left, bottom, cellWidth, 1.5f);
                batch.draw(solid, left, bottom, 1.5f, cellHeight);
            }
        }
        setColor(batch, 1f, 1f, 1f, 0.28f * parentAlpha);
        batch.draw(solid, getX(), getY() + getHeight(), getWidth(), 1.5f);
        batch.draw(solid, getX() + getWidth(), getY(), 1.5f, getHeight());
    }

    private void drawTombstones(Batch batch, float parentAlpha) {
        for (int column = 1; column <= board.getCols(); column++) {
            for (int row = 1; row <= board.getRows(); row++) {
                Tile tile = board.getTile(column, row);
                if (tile.getType() != TileType.TOMBSTONE) {
                    continue;
                }
                float left = cellLeft(column) + cellWidth() * 0.18f;
                float bottom = cellBottom(row) + cellHeight() * 0.06f;
                float width = cellWidth() * 0.64f;
                float height = cellHeight() * 0.86f;
                if (tombstone != null) {
                    batch.setColor(1f, 1f, 1f, parentAlpha);
                    drawFit(batch, tombstone, left, bottom, width, height);
                } else {
                    setColor(batch, 0.40f, 0.40f, 0.42f, parentAlpha);
                    batch.draw(solid, left, bottom, width, height);
                }
            }
        }
    }

    private void drawMowers(Batch batch, float parentAlpha) {
        for (int row = 1; row <= board.getRows(); row++) {
            if (!world.isLawnMowerAvailable(row)) {
                continue;
            }
            float width = Math.min(78f, cellWidth() * 0.72f);
            float height = Math.min(58f, cellHeight() * 0.70f);
            // The mower strip sits left of the stone lawn with a sand gap.
            float left = Math.max(8f, getX() - width - 105f);
            float bottom = cellBottom(row) + (cellHeight() - height) / 2f;
            if (mower != null) {
                batch.setColor(1f, 1f, 1f, parentAlpha);
                drawFit(batch, mower, left, bottom, width, height);
            } else {
                setColor(batch, 0.85f, 0.40f, 0.08f, parentAlpha);
                batch.draw(solid, left, bottom, width, height);
            }
        }
    }

    private void drawPlants(Batch batch, float parentAlpha) {
        for (Plant plant : world.getPlants()) {
            int column = plant.getTileX();
            int row = plant.getTileY();
            float width = cellWidth() * 0.90f;
            float height = cellHeight() * 0.96f;
            float left = cellLeft(column) + (cellWidth() - width) / 2f;
            float bottom = cellBottom(row) + cellHeight() * 0.02f;

            String pamPath = plantVisuals.animationPath(plant.getName());
            String idleClip = plantVisuals.animationClip(plant.getName());
            PlantAnimationState state = animationState(plant);
            String clipName = resolveClip(pamPath, idleClip, state);
            PlantPlayback playback = plantPlaybacks.computeIfAbsent(
                    plant,
                    ignored -> new PlantPlayback()
            );
            playback.use(pamPath, clipName);

            if (pamPath != null && animationRenderer.draw(
                    batch,
                    pamPath,
                    clipName,
                    idleClip,
                    playback.stateTime(),
                    left,
                    bottom,
                    width,
                    height,
                    parentAlpha
            )) {
                continue;
            }

            TextureRegion preview = preview(plant.getName());

            if (preview != null) {
                batch.setColor(1f, 1f, 1f, parentAlpha);
                drawFit(batch, preview, left, bottom, width, height);
            } else {
                setColor(batch, 0.20f, 0.75f, 0.18f, parentAlpha);
                batch.draw(solid, left, bottom, width, height);
            }
        }
    }

    private PlantAnimationState animationState(Plant plant) {
        long currentTick = session.game().getCurrentTick();
        if (plant.isPlantFoodActive(currentTick)) {
            return PlantAnimationState.PLANT_FOOD;
        }

        PlantActivationCapability activation = plant.behaviorCapability(
                PlantActivationCapability.class
        );
        if (activation != null) {
            return activation.isActivationActive()
                    ? PlantAnimationState.ATTACK
                    : PlantAnimationState.IDLE;
        }

        if (plant.getSpec().getCategory() == PlantCategory.EXPLOSIVE) {
            return PlantAnimationState.IDLE;
        }

        long actionTick = plant.getLastActionStartedTick();
        boolean recentAction = actionTick != Long.MIN_VALUE
                && currentTick >= actionTick
                && currentTick - actionTick <= Game.TICKS_PER_SECOND / 2L;
        return plant.hasOngoingAction() || recentAction
                ? PlantAnimationState.ATTACK
                : PlantAnimationState.IDLE;
    }

    private String resolveClip(
            String pamPath,
            String idleClip,
            PlantAnimationState state
    ) {
        if (pamPath == null || state == PlantAnimationState.IDLE) {
            return idleClip;
        }

        PlantAnimationKey key = new PlantAnimationKey(pamPath, state);
        String resolved = plantClipCache.get(key);
        if (resolved != null) {
            return resolved;
        }

        if (plantClipRequests.add(key)) {
            animationService.prepareFirstAvailable(
                    pamPath,
                    clipCandidates(state, idleClip),
                    clip -> plantClipCache.put(
                            key,
                            clip == null ? idleClip : clip
                    )
            );
        }
        return idleClip;
    }

    private static List<String> clipCandidates(
            PlantAnimationState state,
            String idleClip
    ) {
        List<String> candidates = new ArrayList<>();
        if (state == PlantAnimationState.PLANT_FOOD) {
            Collections.addAll(
                    candidates,
                    "plantfood",
                    "plant_food",
                    "plantfood_idle",
                    "plantfood_loop",
                    "powerup",
                    "special"
            );
        } else {
            Collections.addAll(
                    candidates,
                    "attack",
                    "attack1",
                    "attack_1",
                    "shoot",
                    "shooting",
                    "special"
            );
        }
        candidates.add(idleClip);
        return candidates;
    }

    private void drawZombies(Batch batch, float parentAlpha) {
        for (ZombieDeathPlayback death : zombieDeaths) {
            drawZombieDeath(batch, death, parentAlpha);
        }

        long tick = session.game().getCurrentTick();
        for (Zombie zombie : world.getZombies()) {
            float centerX = getX() + (float) zombie.getX() * cellWidth();
            float centerY = cellBottom(zombie.getTileY()) + cellHeight() / 2f;
            ZombieVisualResolver.Visual visual = zombieVisuals.visual(zombie);
            float width = cellWidth() * (visual == null
                    ? 0.52f : visual.widthInCells()) * ZOMBIE_RENDER_SCALE;
            float height = cellHeight() * (visual == null
                    ? 0.82f : visual.heightInCells()) * ZOMBIE_RENDER_SCALE;
            float left = centerX - width / 2f;
            float bottom = centerY - height * 0.52f;

            boolean allied = zombie.isAllied();
            ZombieVisualResolver.Motion motion = zombieMotion(zombie, tick);
            String clip = visual == null
                    ? null : zombieVisuals.clip(zombie, motion);
            String referenceClip = visual == null
                    ? null : zombieVisuals.referenceClip(zombie);
            ZombiePlayback playback = zombiePlaybacks.computeIfAbsent(
                    zombie,
                    ignored -> new ZombiePlayback()
            );
            playback.use(visual == null ? null : visual.path(), clip);

            boolean animated = visual != null && animationRenderer.draw(
                    batch,
                    visual.path(),
                    clip,
                    referenceClip,
                    playback.stateTime(),
                    left,
                    bottom,
                    width,
                    height,
                    parentAlpha,
                    allied,
                    true,
                    zombieVisuals.tint(zombie, tick),
                    zombieVisuals.partsVisibility(zombie, tick)
            );
            if (!animated) {
                setColor(batch, allied ? 0.35f : 0.38f,
                        allied ? 0.62f : 0.30f, 0.22f, parentAlpha);
                batch.draw(solid, left, bottom, width, height);
            }

            float barWidth = Math.min(cellWidth() * 0.72f, 64f);
            float barLeft = centerX - barWidth / 2f;
            float barBottom = centerY + cellHeight() * 0.43f;
            setColor(batch, 0.08f, 0.08f, 0.08f, 0.88f * parentAlpha);
            batch.draw(solid, barLeft, barBottom, barWidth, 5f);
            setColor(batch, allied ? 0.30f : 0.25f,
                    allied ? 0.72f : 0.90f,
                    allied ? 1f : 0.25f, parentAlpha);
            batch.draw(solid, barLeft, barBottom,
                    barWidth * (float) Math.max(0d, zombie.getHealthRatio()),
                    5f);

            if (!animated) {
                oldFontColor.set(font.getColor());
                font.setColor(1f, 1f, 1f, parentAlpha);
                font.draw(batch, abbreviate(zombie.getName()), left - 12f,
                        bottom + 20f, width + 24f, Align.center, true);
                font.setColor(oldFontColor);
            }
        }
    }

    private ZombieVisualResolver.Motion zombieMotion(Zombie zombie, long tick) {
        if (zombie.isFrozen(tick) || zombie.isButtered(tick)) {
            return ZombieVisualResolver.Motion.IDLE;
        }
        if (zombie.isEating() || zombie.isFightingZombie()) {
            return ZombieVisualResolver.Motion.ATTACK;
        }
        return ZombieVisualResolver.Motion.WALK;
    }

    private void addDeathPlayback(Zombie zombie, long tick) {
        ZombieVisualResolver.Visual visual = zombieVisuals.visual(zombie);
        if (visual == null) {
            return;
        }
        zombieDeaths.add(new ZombieDeathPlayback(
                visual,
                zombieVisuals.clip(zombie, ZombieVisualResolver.Motion.DIE),
                zombieVisuals.referenceClip(zombie),
                (float) zombie.getX(),
                zombie.getTileY(),
                zombie.isAllied(),
                new Color(zombieVisuals.tint(zombie, tick)),
                Map.copyOf(zombieVisuals.partsVisibility(zombie, tick))
        ));
    }

    private void drawZombieDeath(
            Batch batch,
            ZombieDeathPlayback death,
            float parentAlpha
    ) {
        float centerX = getX() + death.gridX() * cellWidth();
        float centerY = cellBottom(death.row()) + cellHeight() / 2f;
        float width = cellWidth() * death.visual().widthInCells()
                * ZOMBIE_RENDER_SCALE;
        float height = cellHeight() * death.visual().heightInCells()
                * ZOMBIE_RENDER_SCALE;
        animationRenderer.draw(
                batch,
                death.visual().path(),
                death.clip(),
                death.referenceClip(),
                death.stateTime(),
                centerX - width / 2f,
                centerY - height * 0.52f,
                width,
                height,
                parentAlpha * death.alpha(),
                death.flipX(),
                false,
                death.tint(),
                death.partsVisibility()
        );
    }

    private void updateProjectilePlaybacks(float elapsed) {
        List<Entity> liveProjectiles = session.game()
                .getRegisteredObjects(Entity.class)
                .stream()
                .filter(projectileVisuals::supports)
                .toList();
        Set<Entity> liveSet = Collections.newSetFromMap(
                new IdentityHashMap<>()
        );
        liveSet.addAll(liveProjectiles);

        for (Entity projectile : liveProjectiles) {
            ProjectileVisualResolver.Visual visual =
                    projectileVisuals.visual(projectile);
            ProjectilePlayback playback = projectilePlaybacks.computeIfAbsent(
                    projectile,
                    ignored -> new ProjectilePlayback(visual)
            );
            boolean startedBowlingBounce = playback.observe(
                    projectile,
                    visual,
                    elapsed
            );
            if (startedBowlingBounce) {
                addProjectileImpact(playback, false);
            }
        }

        Iterator<Map.Entry<Entity, ProjectilePlayback>> iterator =
                projectilePlaybacks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, ProjectilePlayback> entry = iterator.next();
            if (liveSet.contains(entry.getKey())) {
                continue;
            }
            ProjectilePlayback playback = entry.getValue();
            playback.captureTerminalPosition(entry.getKey());
            if (shouldShowTerminalImpact(playback)) {
                addProjectileImpact(playback, playback.visual().largeImpact());
            }
            iterator.remove();
        }

        projectileImpacts.forEach(impact -> impact.advance(elapsed));
        projectileImpacts.removeIf(ProjectileImpactPlayback::finished);
    }

    private boolean shouldShowTerminalImpact(ProjectilePlayback playback) {
        if (playback.visual().flight()
                == ProjectileVisualResolver.Flight.LOBBED
                || playback.visual().flight()
                == ProjectileVisualResolver.Flight.HOMING) {
            return true;
        }

        // Straight and directional shots which simply leave the board should
        // disappear cleanly instead of producing a misleading collision flash.
        float edge = 0.18f;
        return playback.gridX() > edge
                && playback.gridX() < board.getCols() - edge
                && playback.gridY() > edge
                && playback.gridY() < board.getRows() - edge;
    }

    private void addProjectileImpact(
            ProjectilePlayback playback,
            boolean large
    ) {
        projectileImpacts.add(new ProjectileImpactPlayback(
                playback.gridX(),
                playback.gridY(),
                new Color(playback.visual().tint()),
                large ? 0.82f : 0.46f
        ));
    }

    private void drawProjectiles(Batch batch, float parentAlpha) {
        long currentTick = session.game().getCurrentTick();
        for (ProjectilePlayback playback : projectilePlaybacks.values()) {
            ProjectileVisualResolver.Visual visual = playback.visual();
            float centerX = getX() + playback.gridX() * cellWidth();
            float centerY = getY() + getHeight()
                    - playback.gridY() * cellHeight();

            if (playback.projectile() instanceof LobbedProjectile lobbed) {
                float progress = (float) lobbed.getFlightProgress(currentTick);
                centerY += 4f * progress * (1f - progress)
                        * cellHeight() * 1.12f;
            }

            float width = cellWidth() * visual.widthInCells();
            float height = cellHeight() * visual.heightInCells();
            float left = centerX - width / 2f;
            float bottom = centerY - height / 2f;

            boolean animated = visual.pamPath() != null
                    && animationRenderer.draw(
                            batch,
                            visual.pamPath(),
                            visual.clip(),
                            playback.stateTime(),
                            left,
                            bottom,
                            width,
                            height,
                            parentAlpha,
                            false,
                            true,
                            visual.tint(),
                            Map.of()
                    );
            if (animated) {
                continue;
            }

            TextureRegion texture = projectileTexture(visual.textureId());
            if (texture != null) {
                batch.setColor(
                        visual.tint().r,
                        visual.tint().g,
                        visual.tint().b,
                        visual.tint().a * parentAlpha
                );
                drawRotatedFit(
                        batch,
                        texture,
                        left,
                        bottom,
                        width,
                        height,
                        playback.rotationDegrees()
                );
                continue;
            }

            drawProjectileFallback(batch, playback, centerX, centerY,
                    width, height, parentAlpha);
        }
    }

    private void drawProjectileFallback(
            Batch batch,
            ProjectilePlayback playback,
            float centerX,
            float centerY,
            float width,
            float height,
            float parentAlpha
    ) {
        Color tint = playback.visual().tint();
        float rotation = playback.rotationDegrees() + 45f;
        setColor(batch, tint.r, tint.g, tint.b, parentAlpha);
        drawRotated(batch, solid, centerX - width * 0.35f,
                centerY - height * 0.35f, width * 0.70f,
                height * 0.70f, rotation);
        setColor(batch, 1f, 1f, 1f, 0.72f * parentAlpha);
        drawRotated(batch, solid, centerX - width * 0.13f,
                centerY - height * 0.13f, width * 0.26f,
                height * 0.26f, rotation);
    }

    private void drawProjectileImpacts(Batch batch, float parentAlpha) {
        for (ProjectileImpactPlayback impact : projectileImpacts) {
            float centerX = getX() + impact.gridX() * cellWidth();
            float centerY = getY() + getHeight()
                    - impact.gridY() * cellHeight();
            float size = Math.min(cellWidth(), cellHeight())
                    * impact.sizeInCells() * impact.scale();
            Color tint = impact.tint();
            float alpha = impact.alpha() * parentAlpha;

            setColor(batch, tint.r, tint.g, tint.b, alpha);
            drawRotated(batch, solid, centerX - size * 0.50f,
                    centerY - size * 0.08f, size, size * 0.16f,
                    impact.rotation());
            drawRotated(batch, solid, centerX - size * 0.50f,
                    centerY - size * 0.08f, size, size * 0.16f,
                    impact.rotation() + 90f);
            setColor(batch, 1f, 0.96f, 0.72f, alpha * 0.90f);
            drawRotated(batch, solid, centerX - size * 0.17f,
                    centerY - size * 0.17f, size * 0.34f,
                    size * 0.34f, impact.rotation() + 45f);
        }
    }

    private TextureRegion projectileTexture(String textureId) {
        if (textureId == null) {
            return null;
        }
        if (!projectileTextureCache.containsKey(textureId)) {
            projectileTextureCache.put(textureId, textures.region(textureId));
        }
        return projectileTextureCache.get(textureId);
    }

    private void drawCollectibles(Batch batch, float parentAlpha) {
        for (Collectible collectible : world.getCollectibles()) {
            float size = Math.min(cellWidth(), cellHeight()) * 0.48f;
            float centerX = getX() + (float) collectible.getX() * cellWidth();
            float centerY = cellBottom(collectible.getTileY()) + cellHeight() * 0.62f;
            float left = centerX - size / 2f;
            float bottom = centerY - size / 2f;

            if (collectible instanceof Sun && sun != null) {
                batch.setColor(1f, 1f, 1f, parentAlpha);
                drawFit(batch, sun, left, bottom, size, size);
            } else if (collectible instanceof PlantFood) {
                setColor(batch, 0.15f, 0.95f, 0.35f, parentAlpha);
                batch.draw(solid, left, bottom, size, size);
            } else {
                setColor(batch, 1f, 0.90f, 0.05f, parentAlpha);
                batch.draw(solid, left, bottom, size, size);
            }
        }
    }

    private void drawHover(Batch batch, float parentAlpha) {
        if (!board.inBounds(hoverColumn, hoverRow)) {
            return;
        }
        // PLANT without a selected packet is the neutral state.
        if (toolMode == ToolMode.PLANT && selectedPlant == null) {
            return;
        }
        float left = cellLeft(hoverColumn);
        float bottom = cellBottom(hoverRow);

        Color tint = switch (toolMode) {
            case PLANT -> Color.LIME;
            case SHOVEL -> Color.RED;
            case PLANT_FOOD -> Color.PURPLE;
        };
        setColor(batch, tint.r, tint.g, tint.b, 0.28f * parentAlpha);
        batch.draw(solid, left, bottom, cellWidth(), cellHeight());

        if (toolMode != ToolMode.PLANT || selectedPlant == null) {
            return;
        }
        TextureRegion preview = preview(selectedPlant);
        if (preview == null) {
            return;
        }
        batch.setColor(1f, 1f, 1f, 0.55f * parentAlpha);
        drawFit(batch, preview, left + cellWidth() * 0.14f,
                bottom + cellHeight() * 0.08f,
                cellWidth() * 0.72f, cellHeight() * 0.82f);
    }

    private TextureRegion preview(String plantName) {
        String key = plantName.toLowerCase(Locale.ROOT);
        if (!plantPreviewCache.containsKey(key)) {
            plantPreviewCache.put(key, plantVisuals.preview(plantName));
        }
        return plantPreviewCache.get(key);
    }

    private InputListener createInputListener() {
        return new InputListener() {
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                updateHover(x, y);
                cellListener.hovered(hoverColumn, hoverRow);
                return true;
            }

            @Override
            public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
            ) {
                if (button != Input.Buttons.LEFT) {
                    return false;
                }
                updateHover(x, y);
                if (board.inBounds(hoverColumn, hoverRow)) {
                    cellListener.clicked(hoverColumn, hoverRow);
                    return true;
                }
                return false;
            }

            @Override
            public void exit(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor toActor
            ) {
                hoverColumn = -1;
                hoverRow = -1;
            }
        };
    }

    private void updateHover(float x, float y) {
        int column = (int) (x / cellWidth()) + 1;
        int visualRow = (int) (y / cellHeight());
        int row = board.getRows() - visualRow;
        if (board.inBounds(column, row)) {
            hoverColumn = column;
            hoverRow = row;
        } else {
            hoverColumn = -1;
            hoverRow = -1;
        }
    }

    private float cellWidth() {
        return getWidth() / board.getCols();
    }

    private float cellHeight() {
        return getHeight() / board.getRows();
    }

    private float cellLeft(int column) {
        return getX() + (column - 1) * cellWidth();
    }

    private float cellBottom(int row) {
        return getY() + (board.getRows() - row) * cellHeight();
    }

    private static void setColor(
            Batch batch,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        batch.setColor(red, green, blue, alpha);
    }

    private static void drawFit(
            Batch batch,
            TextureRegion region,
            float x,
            float y,
            float boxWidth,
            float boxHeight
    ) {
        float scale = Math.min(
                boxWidth / region.getRegionWidth(),
                boxHeight / region.getRegionHeight()
        );
        float width = region.getRegionWidth() * scale;
        float height = region.getRegionHeight() * scale;
        batch.draw(region, x + (boxWidth - width) / 2f,
                y + (boxHeight - height) / 2f, width, height);
    }

    private static void drawRotatedFit(
            Batch batch,
            TextureRegion region,
            float x,
            float y,
            float boxWidth,
            float boxHeight,
            float rotation
    ) {
        float scale = Math.min(
                boxWidth / region.getRegionWidth(),
                boxHeight / region.getRegionHeight()
        );
        float width = region.getRegionWidth() * scale;
        float height = region.getRegionHeight() * scale;
        drawRotated(batch, region,
                x + (boxWidth - width) / 2f,
                y + (boxHeight - height) / 2f,
                width, height, rotation);
    }

    private static void drawRotated(
            Batch batch,
            TextureRegion region,
            float x,
            float y,
            float width,
            float height,
            float rotation
    ) {
        batch.draw(region, x, y, width / 2f, height / 2f,
                width, height, 1f, 1f, rotation);
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "ZOMBIE";
        }
        String text = value.toUpperCase(Locale.ROOT);
        return text.length() <= 9 ? text : text.substring(0, 8) + ".";
    }

    @Override
    public void dispose() {
        solidTexture.dispose();
    }

    public enum ToolMode {
        PLANT,
        SHOVEL,
        PLANT_FOOD
    }

    private enum PlantAnimationState {
        IDLE,
        ATTACK,
        PLANT_FOOD
    }

    private record PlantAnimationKey(
            String pamPath,
            PlantAnimationState state
    ) {
    }

    private static final class PlantPlayback {
        private String pamPath;
        private String clipName;
        private float stateTime;

        private void use(String requestedPath, String requestedClip) {
            if (Objects.equals(pamPath, requestedPath)
                    && Objects.equals(clipName, requestedClip)) {
                return;
            }
            pamPath = requestedPath;
            clipName = requestedClip;
            stateTime = 0f;
        }

        private void advance(float delta) {
            stateTime += delta;
        }

        private float stateTime() {
            return stateTime;
        }
    }

    private static final class ZombiePlayback {
        private String pamPath;
        private String clipName;
        private float stateTime;

        private void use(String requestedPath, String requestedClip) {
            if (Objects.equals(pamPath, requestedPath)
                    && Objects.equals(clipName, requestedClip)) {
                return;
            }
            pamPath = requestedPath;
            clipName = requestedClip;
            stateTime = 0f;
        }

        private void advance(Zombie zombie, float delta, long tick) {
            if (zombie.isFrozen(tick) || zombie.isButtered(tick)) {
                return;
            }
            stateTime += zombie.isChilled(tick) ? delta * 0.5f : delta;
        }

        private float stateTime() {
            return stateTime;
        }
    }

    private static final class ZombieDeathPlayback {
        private static final float DURATION = 2.4f;

        private final ZombieVisualResolver.Visual visual;
        private final String clip;
        private final String referenceClip;
        private final float gridX;
        private final int row;
        private final boolean flipX;
        private final Color tint;
        private final Map<String, Boolean> partsVisibility;
        private float stateTime;

        private ZombieDeathPlayback(
                ZombieVisualResolver.Visual visual,
                String clip,
                String referenceClip,
                float gridX,
                int row,
                boolean flipX,
                Color tint,
                Map<String, Boolean> partsVisibility
        ) {
            this.visual = visual;
            this.clip = clip;
            this.referenceClip = referenceClip;
            this.gridX = gridX;
            this.row = row;
            this.flipX = flipX;
            this.tint = tint;
            this.partsVisibility = partsVisibility;
        }

        private void advance(float delta) {
            stateTime += delta;
        }

        private boolean finished() {
            return stateTime >= DURATION;
        }

        private float alpha() {
            return Math.min(1f, (DURATION - stateTime) / 0.45f);
        }

        private ZombieVisualResolver.Visual visual() { return visual; }
        private String clip() { return clip; }
        private String referenceClip() { return referenceClip; }
        private float gridX() { return gridX; }
        private int row() { return row; }
        private boolean flipX() { return flipX; }
        private Color tint() { return tint; }
        private Map<String, Boolean> partsVisibility() {
            return partsVisibility;
        }
        private float stateTime() { return stateTime; }
    }

    private static final class ProjectilePlayback {
        private Entity projectile;
        private ProjectileVisualResolver.Visual visual;
        private float gridX;
        private float gridY;
        private float movementRotation;
        private float stateTime;
        private boolean positioned;

        private ProjectilePlayback(ProjectileVisualResolver.Visual visual) {
            this.visual = visual;
        }

        private boolean observe(
                Entity observedProjectile,
                ProjectileVisualResolver.Visual requestedVisual,
                float delta
        ) {
            projectile = observedProjectile;
            visual = requestedVisual;
            float nextX = (float) observedProjectile.getX();
            float nextY = (float) observedProjectile.getY();
            boolean startedBowlingBounce = false;

            if (positioned) {
                float deltaX = nextX - gridX;
                float deltaScreenY = -(nextY - gridY);
                if (Math.abs(deltaX) + Math.abs(deltaScreenY) > 0.0001f) {
                    movementRotation = (float) Math.toDegrees(
                            Math.atan2(deltaScreenY, deltaX)
                    );
                }
                startedBowlingBounce = observedProjectile
                        instanceof BowlingBulbProjectile
                        && onLaneCenter(gridY)
                        && !onLaneCenter(nextY);
            }

            gridX = nextX;
            gridY = nextY;
            positioned = true;
            stateTime += delta;
            return startedBowlingBounce;
        }

        private static boolean onLaneCenter(float gridY) {
            float fraction = gridY - (float) Math.floor(gridY);
            return Math.abs(fraction - 0.5f) < 0.015f;
        }

        private void captureTerminalPosition(Entity observedProjectile) {
            gridX = (float) observedProjectile.getX();
            gridY = (float) observedProjectile.getY();
        }

        private float rotationDegrees() {
            if (visual.flight() == ProjectileVisualResolver.Flight.BOWLING) {
                return -stateTime * 230f;
            }
            return movementRotation;
        }

        private Entity projectile() { return projectile; }
        private ProjectileVisualResolver.Visual visual() { return visual; }
        private float gridX() { return gridX; }
        private float gridY() { return gridY; }
        private float stateTime() { return stateTime; }
    }

    private static final class ProjectileImpactPlayback {
        private static final float DURATION = 0.34f;

        private final float gridX;
        private final float gridY;
        private final Color tint;
        private final float sizeInCells;
        private float stateTime;

        private ProjectileImpactPlayback(
                float gridX,
                float gridY,
                Color tint,
                float sizeInCells
        ) {
            this.gridX = gridX;
            this.gridY = gridY;
            this.tint = tint;
            this.sizeInCells = sizeInCells;
        }

        private void advance(float delta) { stateTime += delta; }
        private boolean finished() { return stateTime >= DURATION; }
        private float alpha() {
            float progress = Math.min(1f, stateTime / DURATION);
            return (1f - progress) * 0.86f;
        }
        private float scale() {
            return 0.55f + Math.min(1f, stateTime / DURATION) * 0.75f;
        }
        private float rotation() { return stateTime * 210f; }
        private float gridX() { return gridX; }
        private float gridY() { return gridY; }
        private Color tint() { return tint; }
        private float sizeInCells() { return sizeInCells; }
    }

    public interface CellListener {
        void clicked(int column, int row);

        void hovered(int column, int row);
    }
}
