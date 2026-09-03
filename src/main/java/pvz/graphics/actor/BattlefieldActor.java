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
import pvz.graphics.asset.SunVisualResolver;
import pvz.graphics.asset.ZombieVisualResolver;
import pvz.graphics.battle.BattleCellTargeting;
import pvz.graphics.battle.DamageFlashTracker;
import pvz.graphics.battle.HealthBarLayout;
import pvz.graphics.battle.SunShroomAnimationController;
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
import pvz.model.entity.collectible.sun.SunCollectionOutcome;
import pvz.model.entity.collectible.sun.SunSource;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.behavior.capability.GrowthStageCapability;
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
    private static final float SUN_LANDING_Y_IN_CELL = 0.62f;
    private static final float SKY_SUN_START_MARGIN_IN_CELL = 0.58f;
    private static final float SUN_CLICK_SUPPRESSION_SECONDS = 0.22f;

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
    private final SunVisualResolver sunVisuals = new SunVisualResolver();
    private final TextureBank textures;
    private final PamAnimationService animationService;
    private final PamAnimationRenderer animationRenderer;
    private final CellListener cellListener;
    private final InputListener inputListener;
    private final Map<String, TextureRegion> plantPreviewCache = new HashMap<>();
    private final Map<PlantAnimationKey, String> plantClipCache = new HashMap<>();
    private final Map<PlantAnimationKey, PamAnimationService.AnimationRequest>
            plantClipRequests = new HashMap<>();
    private final Map<Plant, PlantPlayback> plantPlaybacks =
            new IdentityHashMap<>();
    private final Set<String> sunShroomPreloadedPamPaths =
            new HashSet<>();
    private final Map<Zombie, ZombiePlayback> zombiePlaybacks =
            new IdentityHashMap<>();
    private final List<ZombieDeathPlayback> zombieDeaths = new ArrayList<>();
    private final Map<Entity, ProjectilePlayback> projectilePlaybacks =
            new IdentityHashMap<>();
    private final List<ProjectileImpactPlayback> projectileImpacts =
            new ArrayList<>();
    private final List<SunExplosionPlayback> sunExplosions =
            new ArrayList<>();
    private final DamageFlashTracker<Object> damageFlashes =
            new DamageFlashTracker<>();
    private final Map<String, TextureRegion> projectileTextureCache =
            new HashMap<>();
    private final Color oldBatchColor = new Color();
    private final Color oldFontColor = new Color();

    private int hoverColumn = -1;
    private int hoverRow = -1;
    private String selectedPlant;
    private Plant selectedPlantCandidate;
    private ToolMode toolMode = ToolMode.PLANT;
    private boolean showGrid;
    private boolean paused;
    private boolean radioactiveExplosionPreloaded;
    private int nextSunExplosionVariant;
    private float sunClickSuppressionTime;
    private float lastSunCollectX;
    private float lastSunCollectY;
    private boolean disposed;

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
        inputListener = createInputListener();
        addListener(inputListener);
    }

    @Override
    public void act(float delta) {
        if (paused || disposed) {
            return;
        }
        super.act(delta);
        float elapsed = Math.max(0f, delta);
        sunClickSuppressionTime = Math.max(
                0f, sunClickSuppressionTime - elapsed
        );
        damageFlashes.advance(elapsed);
        observeDamage();
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
        sunExplosions.forEach(explosion -> explosion.advance(elapsed));
        sunExplosions.removeIf(SunExplosionPlayback::finished);
    }

    private void observeDamage() {
        Set<Object> live = Collections.newSetFromMap(
                new IdentityHashMap<>()
        );

        for (Plant plant : world.getPlants()) {
            live.add(plant);
            damageFlashes.observe(plant, plant.getHealth());
        }
        for (Zombie zombie : world.getZombies()) {
            live.add(zombie);
            damageFlashes.observe(zombie, zombieDurability(zombie));
        }
        for (int column = 1; column <= board.getCols(); column++) {
            for (int row = 1; row <= board.getRows(); row++) {
                Tile tile = board.getTile(column, row);
                if (!tile.hasDestructibleContent()) {
                    continue;
                }
                live.add(tile);
                damageFlashes.observe(tile, tileDurability(tile));
            }
        }

        damageFlashes.retainKeys(live);
    }

    private static double zombieDurability(Zombie zombie) {
        double armor = zombie.getArmorSet().layers().stream()
                .mapToDouble(layer -> layer.remainingHealth())
                .sum();
        return zombie.getHealth() + armor;
    }

    private static double tileDurability(Tile tile) {
        double overlays = tile.getOverlays().stream()
                .mapToDouble(overlay -> overlay.getRemainingHealth())
                .sum();
        return tile.getHealth() + overlays;
    }

    public void setSelectedPlant(String selectedPlant) {
        if (Objects.equals(this.selectedPlant, selectedPlant)) {
            return;
        }
        this.selectedPlant = selectedPlant;
        selectedPlantCandidate = selectedPlant == null
                ? null
                : session.createPlant(selectedPlant);
    }

    public void setToolMode(ToolMode toolMode) {
        this.toolMode = Objects.requireNonNull(toolMode);
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public void setPaused(boolean paused) {
        if (disposed) {
            return;
        }
        this.paused = paused;
        setTouchable(paused
                ? com.badlogic.gdx.scenes.scene2d.Touchable.disabled
                : com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        if (paused) {
            hoverColumn = -1;
            hoverRow = -1;
            sunClickSuppressionTime = 0f;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        oldBatchColor.set(batch.getColor());
        drawLawn(batch, parentAlpha);
        drawHoverSurface(batch, parentAlpha);
        drawTombstones(batch, parentAlpha);
        drawMowers(batch, parentAlpha);
        drawPlants(batch, parentAlpha);
        drawZombies(batch, parentAlpha);
        drawProjectiles(batch, parentAlpha);
        drawProjectileImpacts(batch, parentAlpha);
        drawCollectibles(batch, parentAlpha);
        drawSunExplosions(batch, parentAlpha);
        drawHealthBars(batch, parentAlpha);
        drawHoverIndicator(batch, parentAlpha);
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
                float healthRatio = (float) Math.max(
                        0d,
                        Math.min(
                                1d,
                                tile.getHealth()
                                        / TileType.TOMBSTONE.getInitialHealth()
                        )
                );
                if (tombstone != null) {
                    float brightness = 0.58f + healthRatio * 0.42f;
                    batch.setColor(
                            brightness,
                            brightness,
                            brightness,
                            parentAlpha
                    );
                    drawFit(batch, tombstone, left, bottom, width, height);
                } else {
                    setColor(batch, 0.40f, 0.40f, 0.42f, parentAlpha);
                    batch.draw(solid, left, bottom, width, height);
                }
                drawTombstoneCracks(
                        batch,
                        left,
                        bottom,
                        width,
                        height,
                        healthRatio,
                        parentAlpha
                );
                drawDamageFlash(
                        batch,
                        left,
                        bottom,
                        width,
                        height,
                        damageFlashes.intensity(tile),
                        parentAlpha
                );
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
            PlantPlayback playback = plantPlaybacks.computeIfAbsent(
                    plant,
                    ignored -> new PlantPlayback()
            );

            long tick = session.game().getCurrentTick();
            SunShroomAnimationController.Selection sunShroomSelection =
                    sunShroomSelection(plant, pamPath, playback, tick);

            String clipName;
            String referenceClip;
            boolean loop;
            if (sunShroomSelection != null) {
                clipName = sunShroomSelection.clip();
                referenceClip = sunShroomSelection.referenceClip();
                loop = sunShroomSelection.loop();
            } else {
                PlantAnimationState state = animationState(plant);
                clipName = resolveClip(pamPath, idleClip, state);
                referenceClip = idleClip;
                loop = true;
            }
            playback.use(pamPath, clipName);

            if (plant.isPlantFoodActive(tick)) {
                drawPlantFoodAura(
                        batch,
                        left,
                        bottom,
                        width,
                        height,
                        tick,
                        parentAlpha
                );
            }

            boolean animated = pamPath != null && animationRenderer.draw(
                    batch,
                    pamPath,
                    clipName,
                    referenceClip,
                    playback.stateTime(),
                    left,
                    bottom,
                    width,
                    height,
                    parentAlpha,
                    false,
                    loop,
                    Color.WHITE,
                    Map.of()
            );
            if (!animated) {
                TextureRegion preview = preview(plant.getName());
                if (preview != null) {
                    batch.setColor(1f, 1f, 1f, parentAlpha);
                    drawFit(batch, preview, left, bottom, width, height);
                } else {
                    setColor(batch, 0.20f, 0.75f, 0.18f, parentAlpha);
                    batch.draw(solid, left, bottom, width, height);
                }
            }

            drawDamageFlash(
                    batch,
                    left,
                    bottom,
                    width,
                    height,
                    damageFlashes.intensity(plant),
                    parentAlpha
            );
        }
    }

    private SunShroomAnimationController.Selection sunShroomSelection(
            Plant plant,
            String pamPath,
            PlantPlayback playback,
            long currentTick
    ) {
        if (!SunShroomAnimationController.supports(plant.getName())) {
            return null;
        }

        preloadSunShroomClips(pamPath);

        GrowthStageCapability growth = plant.behaviorCapability(
                GrowthStageCapability.class
        );
        int stage = growth == null ? 1 : growth.getGrowthStage(currentTick);

        return playback.sunShroom().select(
                stage,
                plant.isPlantFoodActive(currentTick),
                plant.getLastActionStartedTick(),
                playback.stateTime()
        );
    }

    private void preloadSunShroomClips(String pamPath) {
        if (pamPath == null || !sunShroomPreloadedPamPaths.add(pamPath)) {
            return;
        }
        for (String clip : SunShroomAnimationController.clipsToPreload()) {
            animationRenderer.preload(pamPath, clip);
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

        if (!plantClipRequests.containsKey(key)) {
            PamAnimationService.AnimationRequest request =
                    animationService.prepareFirstAvailable(
                            pamPath,
                            clipCandidates(state, idleClip),
                            clip -> {
                                plantClipRequests.remove(key);
                                if (!disposed) {
                                    plantClipCache.put(
                                            key,
                                            clip == null ? idleClip : clip
                                    );
                                }
                            }
                    );
            plantClipRequests.put(key, request);
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

            drawDamageFlash(
                    batch,
                    left,
                    bottom,
                    width,
                    height,
                    damageFlashes.intensity(zombie),
                    parentAlpha
            );

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
            if (collectible instanceof Sun battleSun) {
                drawSun(batch, battleSun, parentAlpha);
                continue;
            }
            drawNonSunCollectible(batch, collectible, parentAlpha);
        }
    }

    private void drawSun(Batch batch, Sun battleSun, float parentAlpha) {
        long tick = session.game().getCurrentTick();
        SunVisualResolver.Visual visual = sunVisuals.resolve(battleSun, tick);
        SunPlacement placement = sunPlacement(battleSun, visual, tick);
        if (battleSun.isRadioactiveWhileFalling()) {
            preloadRadioactiveExplosion();
            drawRadioactiveSunAura(batch, placement, tick, parentAlpha);
        }

        float left = getX() + placement.left();
        float bottom = getY() + placement.bottom();
        float ageSeconds = Math.max(0L, tick - battleSun.getSpawnTick())
                / (float) Game.TICKS_PER_SECOND;
        boolean animated = animationRenderer.draw(
                batch,
                visual.pamPath(),
                visual.clipName(),
                ageSeconds,
                left,
                bottom,
                placement.size(),
                placement.size(),
                parentAlpha,
                false,
                true,
                visual.tint(),
                Map.of()
        );
        if (!animated) {
            drawSunFallback(batch, placement, visual.tint(), parentAlpha);
        }
    }

    private void drawRadioactiveSunAura(
            Batch batch,
            SunPlacement placement,
            long tick,
            float parentAlpha
    ) {
        float ageSeconds = Math.max(0L, tick)
                / (float) Game.TICKS_PER_SECOND;
        float pulse = 0.5f + 0.5f
                * (float) Math.sin(ageSeconds * Math.PI * 3.4f);
        float radius = placement.size() * (1.12f + pulse * 0.20f);
        float thickness = Math.max(2f, radius * 0.055f);
        float centerX = getX() + placement.centerX();
        float centerY = getY() + placement.centerY();
        float outerAlpha = (0.13f + pulse * 0.11f) * parentAlpha;
        float innerAlpha = (0.20f + pulse * 0.12f) * parentAlpha;

        setColor(batch, 0.64f, 0.12f, 1.00f, outerAlpha);
        drawRadioactiveRays(batch, centerX, centerY, radius, thickness, 0f);
        setColor(batch, 0.90f, 0.62f, 1.00f, innerAlpha);
        drawRadioactiveRays(
                batch,
                centerX,
                centerY,
                radius * 0.74f,
                Math.max(2f, thickness * 0.72f),
                22.5f
        );
    }

    private void drawRadioactiveRays(
            Batch batch,
            float centerX,
            float centerY,
            float size,
            float thickness,
            float rotation
    ) {
        drawRotated(
                batch, solid, centerX - size / 2f, centerY - thickness / 2f,
                size, thickness, rotation
        );
        drawRotated(
                batch, solid, centerX - size / 2f, centerY - thickness / 2f,
                size, thickness, rotation + 45f
        );
        drawRotated(
                batch, solid, centerX - size / 2f, centerY - thickness / 2f,
                size, thickness, rotation + 90f
        );
        drawRotated(
                batch, solid, centerX - size / 2f, centerY - thickness / 2f,
                size, thickness, rotation + 135f
        );
    }

    private void drawSunFallback(
            Batch batch,
            SunPlacement placement,
            Color tint,
            float parentAlpha
    ) {
        if (sun == null) {
            return;
        }
        batch.setColor(tint.r, tint.g, tint.b, tint.a * parentAlpha);
        drawFit(
                batch,
                sun,
                getX() + placement.left(),
                getY() + placement.bottom(),
                placement.size(),
                placement.size()
        );
    }

    private void drawNonSunCollectible(
            Batch batch,
            Collectible collectible,
            float parentAlpha
    ) {
        float size = Math.min(cellWidth(), cellHeight()) * 0.48f;
        float centerX = getX() + (float) collectible.getX() * cellWidth();
        float centerY = cellBottom(collectible.getTileY())
                + cellHeight() * SUN_LANDING_Y_IN_CELL;
        float left = centerX - size / 2f;
        float bottom = centerY - size / 2f;

        if (collectible instanceof PlantFood) {
            setColor(batch, 0.15f, 0.95f, 0.35f, parentAlpha);
        } else {
            setColor(batch, 1f, 0.90f, 0.05f, parentAlpha);
        }
        batch.draw(solid, left, bottom, size, size);
    }

    private SunPlacement sunPlacement(
            Sun battleSun,
            SunVisualResolver.Visual visual,
            long tick
    ) {
        float size = Math.min(cellWidth(), cellHeight())
                * visual.sizeInCell();
        float centerX = (float) battleSun.getTargetX() * cellWidth();
        float targetY = targetSunCenterY(battleSun);
        if (battleSun.getSource() == SunSource.PLANT) {
            targetY = modelSunCenterY(battleSun);
        }
        float centerY = targetY;

        if (battleSun.isFalling()) {
            centerY = fallingSunCenterY(battleSun, size, targetY, tick);
        } else if (battleSun.getSource() == SunSource.PLANT) {
            centerY += sunVisuals.plantPopOffsetInCells(battleSun, tick)
                    * cellHeight();
        }

        return SunPlacement.centered(centerX, centerY, size);
    }

    private float targetSunCenterY(Sun battleSun) {
        return getHeight() - (float) battleSun.getTargetY() * cellHeight()
                + (SUN_LANDING_Y_IN_CELL - 0.5f) * cellHeight();
    }

    private float modelSunCenterY(Sun battleSun) {
        return getHeight() - (float) battleSun.getTargetY() * cellHeight();
    }

    private float fallingSunCenterY(
            Sun battleSun,
            float size,
            float targetY,
            long tick
    ) {
        float progress = (float) battleSun.getFallProgress(tick);
        float eased = progress * progress * (3f - 2f * progress);
        float startY = getHeight() + size * SKY_SUN_START_MARGIN_IN_CELL;
        return startY + (targetY - startY) * eased;
    }

    private void preloadRadioactiveExplosion() {
        if (radioactiveExplosionPreloaded) {
            return;
        }
        radioactiveExplosionPreloaded = true;
        for (String clip : sunVisuals.explosionClips()) {
            animationRenderer.preload(sunVisuals.explosionPamPath(), clip);
        }
    }

    private void drawSunExplosions(Batch batch, float parentAlpha) {
        for (SunExplosionPlayback explosion : sunExplosions) {
            drawSunExplosion(batch, explosion, parentAlpha);
        }
    }

    private void drawSunExplosion(
            Batch batch,
            SunExplosionPlayback explosion,
            float parentAlpha
    ) {
        float centerX = getX() + explosion.targetX() * cellWidth();
        float centerY = getY() + targetSunCenterY(explosion.targetY());
        float width = cellWidth() * sunVisuals.explosionSizeInCells();
        float height = cellHeight() * sunVisuals.explosionSizeInCells();
        float left = centerX - width / 2f;
        float bottom = centerY - height / 2f;
        Color tint = sunVisuals.explosionTint();
        drawRadioactiveExplosionAura(
                batch, explosion, centerX, centerY, parentAlpha
        );

        boolean animated = animationRenderer.draw(
                batch,
                sunVisuals.explosionPamPath(),
                explosion.clipName(),
                explosion.stateTime(),
                left,
                bottom,
                width,
                height,
                parentAlpha,
                false,
                false,
                tint,
                Map.of()
        );
        if (!animated) {
            drawSunExplosionFallback(
                    batch, explosion, centerX, centerY, parentAlpha
            );
        }
    }

    private void drawRadioactiveExplosionAura(
            Batch batch,
            SunExplosionPlayback explosion,
            float centerX,
            float centerY,
            float parentAlpha
    ) {
        float progress = explosion.progress();
        float size = Math.min(cellWidth(), cellHeight())
                * (1.25f + progress * 1.85f);
        float thickness = Math.max(3f, size * 0.07f);
        float alpha = (1f - progress) * 0.32f * parentAlpha;
        setColor(batch, 0.72f, 0.20f, 1.00f, alpha);
        drawRadioactiveRays(
                batch, centerX, centerY, size, thickness, progress * 70f
        );
    }

    private float targetSunCenterY(float targetY) {
        return getHeight() - targetY * cellHeight()
                + (SUN_LANDING_Y_IN_CELL - 0.5f) * cellHeight();
    }

    private void drawSunExplosionFallback(
            Batch batch,
            SunExplosionPlayback explosion,
            float centerX,
            float centerY,
            float parentAlpha
    ) {
        float progress = explosion.progress();
        float size = Math.min(cellWidth(), cellHeight())
                * (1.15f + progress * 1.65f);
        float alpha = (1f - progress) * 0.82f * parentAlpha;
        Color tint = sunVisuals.explosionTint();
        setColor(batch, tint.r, tint.g, tint.b, alpha);
        drawRotated(
                batch, solid, centerX - size / 2f, centerY - size * 0.07f,
                size, size * 0.14f, progress * 95f
        );
        drawRotated(
                batch, solid, centerX - size / 2f, centerY - size * 0.07f,
                size, size * 0.14f, 60f + progress * 95f
        );
        drawRotated(
                batch, solid, centerX - size / 2f, centerY - size * 0.07f,
                size, size * 0.14f, 120f + progress * 95f
        );
    }

    private void drawHoverSurface(Batch batch, float parentAlpha) {
        if (!hasActiveHoverTarget()) {
            return;
        }
        float left = cellLeft(hoverColumn);
        float bottom = cellBottom(hoverRow);
        boolean valid = isHoverTargetValid();
        Color tint = valid ? Color.LIME : Color.RED;
        setColor(batch, tint.r, tint.g, tint.b, 0.25f * parentAlpha);
        batch.draw(solid, left, bottom, cellWidth(), cellHeight());

        if (toolMode != ToolMode.PLANT || selectedPlant == null) {
            return;
        }
        TextureRegion preview = preview(selectedPlant);
        if (preview == null) {
            return;
        }
        if (valid) {
            batch.setColor(1f, 1f, 1f, 0.68f * parentAlpha);
        } else {
            batch.setColor(1f, 0.34f, 0.34f, 0.54f * parentAlpha);
        }
        drawFit(batch, preview, left + cellWidth() * 0.14f,
                bottom + cellHeight() * 0.08f,
                cellWidth() * 0.72f, cellHeight() * 0.82f);
    }

    private void drawHoverIndicator(Batch batch, float parentAlpha) {
        if (!hasActiveHoverTarget()) {
            return;
        }
        float left = cellLeft(hoverColumn);
        float bottom = cellBottom(hoverRow);
        boolean valid = isHoverTargetValid();
        Color tint = valid ? Color.LIME : Color.RED;
        drawHoverBorder(batch, left, bottom, tint, parentAlpha);
        drawValidityMark(batch, left, bottom, valid, parentAlpha);
    }

    private boolean hasActiveHoverTarget() {
        return board.inBounds(hoverColumn, hoverRow)
                && (toolMode != ToolMode.PLANT || selectedPlant != null);
    }

    private void drawHealthBars(Batch batch, float parentAlpha) {
        drawPlantHealthBars(batch, parentAlpha);
        drawZombieHealthBars(batch, parentAlpha);
    }

    private void drawPlantHealthBars(Batch batch, float parentAlpha) {
        float barWidth = Math.min(cellWidth() * 0.58f, 48f);
        float barHeight = 6f;
        HealthBarLayout.Bounds bounds = healthBarBounds();

        for (Plant plant : world.getPlants()) {
            double maximum = plant.getSpec().getBaseHp();
            float ratio = HealthBarLayout.clampRatio(
                    maximum <= 0d ? 0d : plant.getHealth() / maximum
            );
            if (ratio >= 0.999f
                    || board.getTopPlant(
                            plant.getTileX(), plant.getTileY()
                    ) != plant) {
                continue;
            }

            float centerX = cellLeft(plant.getTileX()) + cellWidth() / 2f;
            HealthBarLayout.Bar bar = HealthBarLayout.clampCentered(
                    centerX,
                    cellBottom(plant.getTileY()) + 5f,
                    barWidth,
                    barHeight,
                    bounds
            );
            drawHealthBar(batch, bar, ratio, false, parentAlpha);
        }
    }

    private void drawZombieHealthBars(Batch batch, float parentAlpha) {
        float barWidth = Math.min(cellWidth() * 0.58f, 48f);
        float barHeight = 6f;
        float laneStep = barHeight + 2f;
        HealthBarLayout.Bounds bounds = healthBarBounds();
        Map<Integer, List<Zombie>> byRow = new HashMap<>();

        for (Zombie zombie : world.getZombies()) {
            byRow.computeIfAbsent(
                    zombie.getTileY(), ignored -> new ArrayList<>()
            ).add(zombie);
        }

        for (int row = 1; row <= board.getRows(); row++) {
            List<Zombie> zombies = byRow.get(row);
            if (zombies == null) {
                continue;
            }
            zombies.sort((first, second) -> Double.compare(
                    first.getX(), second.getX()
            ));
            float[] occupiedUntil = {
                    Float.NEGATIVE_INFINITY,
                    Float.NEGATIVE_INFINITY,
                    Float.NEGATIVE_INFINITY,
                    Float.NEGATIVE_INFINITY,
                    Float.NEGATIVE_INFINITY
            };

            for (Zombie zombie : zombies) {
                float centerX = getX() + (float) zombie.getX() * cellWidth();
                HealthBarLayout.Bar horizontal =
                        HealthBarLayout.clampCentered(
                                centerX,
                                bounds.bottom(),
                                barWidth,
                                barHeight,
                                bounds
                        );
                int lane = HealthBarLayout.reserveLane(
                        horizontal.left(),
                        horizontal.right(),
                        occupiedUntil,
                        3f
                );
                float desiredBottom = cellBottom(row) + cellHeight()
                        - barHeight - 4f - lane * laneStep;
                HealthBarLayout.Bar bar =
                        HealthBarLayout.clampCentered(
                                centerX,
                                desiredBottom,
                                barWidth,
                                barHeight,
                                bounds
                        );
                drawHealthBar(
                        batch,
                        bar,
                        HealthBarLayout.clampRatio(zombie.getHealthRatio()),
                        zombie.isAllied(),
                        parentAlpha
                );
            }
        }
    }

    private HealthBarLayout.Bounds healthBarBounds() {
        return new HealthBarLayout.Bounds(
                getX(), getY(), getWidth(), getHeight()
        );
    }

    private void drawHealthBar(
            Batch batch,
            HealthBarLayout.Bar bar,
            float ratio,
            boolean allied,
            float parentAlpha
    ) {
        setColor(batch, 0.05f, 0.05f, 0.05f, 0.90f * parentAlpha);
        batch.draw(
                solid,
                bar.left(),
                bar.bottom(),
                bar.width(),
                bar.height()
        );

        float inset = 1f;
        float innerWidth = Math.max(0f, bar.width() - inset * 2f);
        if (allied) {
            setColor(batch, 0.28f, 0.72f, 1f, parentAlpha);
        } else if (ratio > 0.60f) {
            setColor(batch, 0.24f, 0.90f, 0.25f, parentAlpha);
        } else if (ratio > 0.30f) {
            setColor(batch, 1f, 0.76f, 0.12f, parentAlpha);
        } else {
            setColor(batch, 0.95f, 0.20f, 0.16f, parentAlpha);
        }
        batch.draw(
                solid,
                bar.left() + inset,
                bar.bottom() + inset,
                innerWidth * ratio,
                Math.max(0f, bar.height() - inset * 2f)
        );
    }

    private boolean isHoverTargetValid() {
        return switch (toolMode) {
            case PLANT -> BattleCellTargeting.canPlant(
                    session,
                    selectedPlant,
                    selectedPlantCandidate,
                    hoverColumn,
                    hoverRow
            );
            case SHOVEL -> BattleCellTargeting.canShovel(
                    board,
                    hoverColumn,
                    hoverRow
            );
            case PLANT_FOOD -> BattleCellTargeting.canUsePlantFood(
                    session,
                    hoverColumn,
                    hoverRow
            );
        };
    }

    private void drawHoverBorder(
            Batch batch,
            float left,
            float bottom,
            Color tint,
            float parentAlpha
    ) {
        float border = 3f;
        setColor(batch, tint.r, tint.g, tint.b, 0.90f * parentAlpha);
        batch.draw(solid, left, bottom, cellWidth(), border);
        batch.draw(solid, left, bottom + cellHeight() - border,
                cellWidth(), border);
        batch.draw(solid, left, bottom, border, cellHeight());
        batch.draw(solid, left + cellWidth() - border, bottom,
                border, cellHeight());
    }

    private void drawValidityMark(
            Batch batch,
            float left,
            float bottom,
            boolean valid,
            float parentAlpha
    ) {
        float size = Math.min(cellWidth(), cellHeight()) * 0.22f;
        float centerX = left + cellWidth() - size * 0.82f;
        float centerY = bottom + cellHeight() - size * 0.82f;
        float lineHeight = Math.max(2.5f, size * 0.16f);
        setColor(batch, 1f, 1f, 1f, 0.94f * parentAlpha);

        if (valid) {
            drawRotated(batch, solid,
                    centerX - size * 0.48f,
                    centerY - lineHeight * 0.70f,
                    size * 0.43f,
                    lineHeight,
                    -45f);
            drawRotated(batch, solid,
                    centerX - size * 0.13f,
                    centerY - lineHeight * 0.55f,
                    size * 0.72f,
                    lineHeight,
                    45f);
            return;
        }

        drawRotated(batch, solid,
                centerX - size * 0.50f,
                centerY - lineHeight / 2f,
                size,
                lineHeight,
                45f);
        drawRotated(batch, solid,
                centerX - size * 0.50f,
                centerY - lineHeight / 2f,
                size,
                lineHeight,
                -45f);
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
                if (paused || disposed) {
                    return false;
                }
                updateHover(x, y);
                if (collectSunUnderPointer(x, y)) {
                    rememberSunCollection(x, y);
                }
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
                if (paused || disposed || button != Input.Buttons.LEFT) {
                    return false;
                }
                updateHover(x, y);
                if (collectSunUnderPointer(x, y)) {
                    rememberSunCollection(x, y);
                    return true;
                }
                if (shouldSuppressCellClick(x, y)) {
                    return true;
                }
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

    private void rememberSunCollection(float x, float y) {
        lastSunCollectX = x;
        lastSunCollectY = y;
        sunClickSuppressionTime = SUN_CLICK_SUPPRESSION_SECONDS;
    }

    private boolean shouldSuppressCellClick(float x, float y) {
        if (sunClickSuppressionTime <= 0f) {
            return false;
        }
        float radius = Math.min(cellWidth(), cellHeight()) * 0.35f;
        float deltaX = x - lastSunCollectX;
        float deltaY = y - lastSunCollectY;
        if (deltaX * deltaX + deltaY * deltaY > radius * radius) {
            return false;
        }
        sunClickSuppressionTime = 0f;
        return true;
    }

    private boolean collectSunUnderPointer(float x, float y) {
        Sun battleSun = sunAtPointer(x, y);
        if (battleSun == null
                || !board.inBounds(hoverColumn, hoverRow)) {
            return false;
        }

        int collectionColumn = hoverColumn;
        int collectionRow = hoverRow;
        SunCollectionOutcome outcome = cellListener.collectSun(
                battleSun,
                collectionColumn,
                collectionRow
        );
        if (outcome == null) {
            return false;
        }
        if (outcome == SunCollectionOutcome.EXPLODED) {
            startRadioactiveExplosion(collectionColumn, collectionRow);
        }
        return true;
    }

    private Sun sunAtPointer(float x, float y) {
        long tick = session.game().getCurrentTick();
        Sun best = null;
        float bestDistanceSquared = Float.MAX_VALUE;

        for (Collectible collectible : world.getCollectibles()) {
            if (!(collectible instanceof Sun battleSun)
                    || battleSun.isRemoved()) {
                continue;
            }
            SunVisualResolver.Visual visual = sunVisuals.resolve(
                    battleSun, tick
            );
            SunPlacement placement = sunPlacement(battleSun, visual, tick);
            if (!placement.contains(x, y, sunVisuals.hitboxScale())) {
                continue;
            }

            float distanceSquared = placement.distanceSquaredTo(x, y);
            if (distanceSquared < bestDistanceSquared) {
                best = battleSun;
                bestDistanceSquared = distanceSquared;
            }
        }
        return best;
    }

    private void startRadioactiveExplosion(int column, int row) {
        preloadRadioactiveExplosion();
        String clip = sunVisuals.explosionClip(nextSunExplosionVariant++);
        sunExplosions.add(new SunExplosionPlayback(
                column - 0.5f,
                row - 0.5f,
                clip,
                sunVisuals.explosionDurationSeconds()
        ));
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

    private void drawPlantFoodAura(
            Batch batch,
            float left,
            float bottom,
            float width,
            float height,
            long tick,
            float parentAlpha
    ) {
        float pulse = 0.5f + 0.5f
                * (float) Math.sin(tick * 0.36f);
        float centerX = left + width / 2f;
        float centerY = bottom + height / 2f;
        float rayWidth = width * (0.72f + pulse * 0.16f);
        float rayHeight = Math.max(2f, height * 0.055f);
        float alpha = (0.20f + pulse * 0.10f) * parentAlpha;

        setColor(batch, 0.66f, 1f, 0.22f, alpha);
        drawRotated(
                batch,
                solid,
                centerX - rayWidth / 2f,
                centerY - rayHeight / 2f,
                rayWidth,
                rayHeight,
                0f
        );
        drawRotated(
                batch,
                solid,
                centerX - rayWidth / 2f,
                centerY - rayHeight / 2f,
                rayWidth,
                rayHeight,
                60f
        );
        drawRotated(
                batch,
                solid,
                centerX - rayWidth / 2f,
                centerY - rayHeight / 2f,
                rayWidth,
                rayHeight,
                120f
        );
    }

    private void drawDamageFlash(
            Batch batch,
            float left,
            float bottom,
            float width,
            float height,
            float intensity,
            float parentAlpha
    ) {
        if (intensity <= 0f) {
            return;
        }

        float centerX = left + width / 2f;
        float centerY = bottom + height * 0.58f;
        float size = Math.min(width, height)
                * (0.30f + intensity * 0.22f);
        float lineHeight = Math.max(2f, size * 0.10f);
        float alpha = Math.min(1f, intensity) * parentAlpha;

        setColor(batch, 1f, 0.98f, 0.78f, alpha * 0.88f);
        drawRotated(
                batch,
                solid,
                centerX - size / 2f,
                centerY - lineHeight / 2f,
                size,
                lineHeight,
                45f
        );
        drawRotated(
                batch,
                solid,
                centerX - size / 2f,
                centerY - lineHeight / 2f,
                size,
                lineHeight,
                -45f
        );
        setColor(batch, 1f, 1f, 1f, alpha);
        batch.draw(
                solid,
                centerX - size * 0.12f,
                centerY - size * 0.12f,
                size * 0.24f,
                size * 0.24f
        );
    }

    private void drawTombstoneCracks(
            Batch batch,
            float left,
            float bottom,
            float width,
            float height,
            float healthRatio,
            float parentAlpha
    ) {
        if (healthRatio > 0.66f) {
            return;
        }

        float lineWidth = Math.max(2f, width * 0.035f);
        float centerX = left + width * 0.52f;
        float centerY = bottom + height * 0.58f;
        setColor(batch, 0.18f, 0.13f, 0.10f, 0.78f * parentAlpha);
        drawRotated(
                batch,
                solid,
                centerX - width * 0.15f,
                centerY,
                width * 0.30f,
                lineWidth,
                -55f
        );
        drawRotated(
                batch,
                solid,
                centerX - width * 0.03f,
                centerY - height * 0.16f,
                height * 0.22f,
                lineWidth,
                -112f
        );
        if (healthRatio > 0.33f) {
            return;
        }
        drawRotated(
                batch,
                solid,
                centerX - width * 0.28f,
                centerY - height * 0.20f,
                width * 0.34f,
                lineWidth,
                28f
        );
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
        if (disposed) {
            return;
        }
        disposed = true;
        paused = true;
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        hoverColumn = -1;
        hoverRow = -1;
        selectedPlant = null;
        selectedPlantCandidate = null;

        removeListener(inputListener);
        clearActions();
        clearListeners();
        remove();

        for (PamAnimationService.AnimationRequest request
                : plantClipRequests.values()) {
            request.cancel();
        }
        plantClipRequests.clear();
        animationRenderer.dispose();

        plantPreviewCache.clear();
        plantClipCache.clear();
        sunShroomPreloadedPamPaths.clear();
        plantPlaybacks.clear();
        zombiePlaybacks.clear();
        zombieDeaths.clear();
        projectilePlaybacks.clear();
        projectileImpacts.clear();
        sunExplosions.clear();
        projectileTextureCache.clear();
        damageFlashes.clear();

        // solidTexture belongs only to this battle actor. Regions from the
        // shared TextureBank (plants, zombies, projectiles, mower, sun, ...)
        // are references only and must remain alive for the next screen.
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
        private SunShroomAnimationController sunShroom;

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

        private SunShroomAnimationController sunShroom() {
            if (sunShroom == null) {
                sunShroom = new SunShroomAnimationController();
            }
            return sunShroom;
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

    private record SunPlacement(
            float left,
            float bottom,
            float size,
            float centerX,
            float centerY
    ) {
        private static SunPlacement centered(
                float centerX,
                float centerY,
                float size
        ) {
            return new SunPlacement(
                    centerX - size / 2f,
                    centerY - size / 2f,
                    size,
                    centerX,
                    centerY
            );
        }

        private boolean contains(float x, float y, float hitboxScale) {
            float half = size * hitboxScale / 2f;
            return x >= centerX - half
                    && x <= centerX + half
                    && y >= centerY - half
                    && y <= centerY + half;
        }

        private float distanceSquaredTo(float x, float y) {
            float deltaX = x - centerX;
            float deltaY = y - centerY;
            return deltaX * deltaX + deltaY * deltaY;
        }
    }

    private static final class SunExplosionPlayback {
        private final float targetX;
        private final float targetY;
        private final String clipName;
        private final float duration;
        private float stateTime;

        private SunExplosionPlayback(
                float targetX,
                float targetY,
                String clipName,
                float duration
        ) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.clipName = Objects.requireNonNull(clipName);
            this.duration = duration;
        }

        private void advance(float delta) {
            stateTime += delta;
        }

        private boolean finished() {
            return stateTime >= duration;
        }

        private float progress() {
            if (duration <= 0f) {
                return 1f;
            }
            return Math.min(1f, stateTime / duration);
        }

        private float targetX() { return targetX; }
        private float targetY() { return targetY; }
        private String clipName() { return clipName; }
        private float stateTime() { return stateTime; }
    }

    public interface CellListener {
        void clicked(int column, int row);

        SunCollectionOutcome collectSun(
                Sun sun,
                int collectionColumn,
                int collectionRow
        );
    }
}
