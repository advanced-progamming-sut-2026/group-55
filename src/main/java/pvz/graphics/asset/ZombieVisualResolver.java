package pvz.graphics.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import pvz.model.entity.zombie.ArmorInstance;
import pvz.model.entity.zombie.Zombie;

/** Resolves gameplay zombie ids to their PAM presentation metadata. */
public final class ZombieVisualResolver {
    private static final Color NORMAL = Color.WHITE;
    private static final Color ALLIED = new Color(0.78f, 1f, 0.80f, 1f);
    private static final Color CHILLED = new Color(0.64f, 0.88f, 1f, 1f);
    private static final Color FROZEN = new Color(0.48f, 0.76f, 1f, 1f);
    private static final Color POISONED = new Color(0.62f, 1f, 0.58f, 1f);
    private static final Color GLOWING = new Color(1f, 0.94f, 0.56f, 1f);

    private static final String EGYPT_BASIC =
            "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM";
    private static final Map<String, Visual> VISUALS = Map.ofEntries(
            visual("ZombieDefault", EGYPT_BASIC),
            visual("ZombieArmor1", EGYPT_BASIC),
            visual("ZombieArmor2", EGYPT_BASIC),
            visual("ZombieArmor4", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC_BRICK/ZOMBIE_DARK_BASIC_BRICK.PAM"),
            visual("ZombieDarkArmor3", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM"),
            visual("ZombieGargantuar", "768/FULL/ZOMBIE/GARGANTUAR/GARGANTUAR.PAM", 1.45f, 1.75f),
            visual("ZombieImp", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_IMP/ZOMBIE_EGYPT_IMP.PAM", 0.78f, 0.92f),
            visual("ZombieRa", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM"),
            visual("ZombieExplorer", "768/INITIAL/ZOMBIE/ZOMBIE_EXPLORER/ZOMBIE_EXPLORER.PAM"),
            visual("ZombieTombRaiser", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_TOMBRAISER/ZOMBIE_EGYPT_TOMBRAISER.PAM"),
            visual(
                    "ZombieIceAgeDodo",
                    "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_DODORIDER/ZOMBIE_ICEAGE_DODORIDER.PAM",
                    1.18f, 1.30f
            ),
            visual("ZombieIceAgeHunter", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_HUNTER/ZOMBIE_ICEAGE_HUNTER.PAM", 1.10f, 1.35f),
            visual(
                    "ZombieIceAgeTroglobite",
                    "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_TROGLOBITE/ZOMBIE_ICEAGE_TROGLOBITE.PAM",
                    1.34f, 1.42f
            ),
            visual(
                    "ZombieBeachFisherman",
                    "768/FULL/ZOMBIE/ZOMBIE_BEACH_FISHERMAN/ZOMBIE_BEACH_FISHERMAN.PAM",
                    1.20f, 1.38f
            ),
            visual("ZombieBeachOctopus", "768/FULL/ZOMBIE/ZOMBIE_BEACH_OCTOPUS/ZOMBIE_BEACH_OCTOPUS.PAM"),
            visual("ZombieBeachSnorkel", "768/FULL/ZOMBIE/ZOMBIE_BEACH_SNORKELER/ZOMBIE_BEACH_SNORKELER.PAM"),
            visual("ZombieDarkJuggler", "768/FULL/ZOMBIE/ZOMBIE_DARK_JESTER/ZOMBIE_DARK_JESTER.PAM"),
            visual("ZombieWizard", "768/FULL/ZOMBIE/ZOMBIE_DARK_WIZARD/ZOMBIE_DARK_WIZARD.PAM"),
            visual("ZombieDarkKing", "768/FULL/ZOMBIE/ZOMBIE_DARK_KING/ZOMBIE_DARK_KING.PAM", 1.18f, 1.36f),
            visual(
                    "ZombieDarkImpDragon",
                    "768/FULL/ZOMBIE/ZOMBIE_DARK_IMP_DRAGON/ZOMBIE_DARK_IMP_DRAGON.PAM",
                    0.82f, 0.98f
            ),
            visual(
                    "ZombieModernAllStar",
                    "768/FULL/ZOMBIE/ZOMBIE_MODERN_ALLSTAR/ZOMBIE_MODERN_ALLSTAR.PAM",
                    1.14f, 1.42f
            ),
            visual("ZombieArcade", "768/FULL/ZOMBIE/ZOMBIE_80S_ARCADE/ZOMBIE_80S_ARCADE.PAM", 1.48f, 1.38f),
            visual("ZombieLostCityJane", "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_JANE/ZOMBIE_LOSTCITY_JANE.PAM"),
            visual(
                    "ZombieCrystalSkull",
                    "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_CRYSTALSKULL/ZOMBIE_LOSTCITY_CRYSTALSKULL.PAM"
            ),
            visual("ZombieProspector", "768/FULL/ZOMBIE/ZOMBIE_PROSPECTOR/ZOMBIE_PROSPECTOR.PAM"),
            visual("ZombiePiano", "768/FULL/ZOMBIE/ZOMBIE_PIANO/ZOMBIE_PIANO.PAM", 1.66f, 1.32f),
            visual("ZombieNewspaper", "768/FULL/ZOMBIE/ZOMBIE_MODERN_NEWSPAPER/ZOMBIE_MODERN_NEWSPAPER.PAM"),
            visual(
                    "ZombieBarrelRoller",
                    "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER/ZOMBIE_PIRATE_BARREL_PUSHER.PAM",
                    1.38f, 1.34f
            )
    );

    private final FileHandle assetsRoot;
    private final Map<String, Boolean> availability = new HashMap<>();

    public ZombieVisualResolver(FileHandle assetsRoot) {
        this.assetsRoot = Objects.requireNonNull(
                assetsRoot,
                "assets root cannot be null"
        );
    }

    public Visual visual(Zombie zombie) {
        Objects.requireNonNull(zombie, "zombie cannot be null");
        Visual visual = VISUALS.get(zombie.getSpec().getId());
        if (visual == null) {
            return null;
        }
        boolean exists = availability.computeIfAbsent(
                visual.path(),
                this::assetExists
        );
        return exists ? visual : null;
    }

    public String clip(Zombie zombie, Motion motion) {
        String id = zombie.getSpec().getId();
        if ("ZombieNewspaper".equals(id)) {
            boolean paper = zombie.getArmorSet().hasIntactArmor("NEWSPAPER");
            return switch (motion) {
                case IDLE -> paper ? "idle_newspaper" : "walk";
                case WALK -> paper ? "walk_newspaper" : "walk";
                case ATTACK -> paper ? "eat_newspaper" : "eat";
                case DIE -> "die";
            };
        }
        if ("ZombieBeachFisherman".equals(id)) {
            return switch (motion) {
                case ATTACK -> "cast";
                case DIE -> "die";
                default -> "idle";
            };
        }
        if ("ZombieDarkKing".equals(id)) {
            return motion == Motion.ATTACK ? "special"
                    : motion == Motion.DIE ? "die" : "idle";
        }
        if ("ZombiePiano".equals(id)) {
            return motion == Motion.DIE ? "die"
                    : motion == Motion.IDLE ? "idle" : "play";
        }
        if ("ZombieModernAllStar".equals(id) && motion == Motion.WALK) {
            return "run";
        }
        return switch (motion) {
            case IDLE -> "idle";
            case WALK -> "walk";
            case ATTACK -> "eat";
            case DIE -> "die";
        };
    }

    /**
     * Returns a state-independent clip used only to keep render scale and
     * anchoring stable while the active motion changes.
     */
    public String referenceClip(Zombie zombie) {
        return switch (zombie.getSpec().getId()) {
            case "ZombieBeachFisherman", "ZombieDarkKing" -> "idle";
            case "ZombiePiano" -> "play";
            case "ZombieModernAllStar" -> "run";
            default -> "walk";
        };
    }

    public Map<String, Boolean> partsVisibility(Zombie zombie, long tick) {
        Map<String, Boolean> visible = new HashMap<>();
        showArmor(visible, zombie, "CONE", "_zombie_egypt_armor1_states",
                "zombie_armor_cone");
        showArmor(visible, zombie, "BUCKET", "_zombie_egypt_armor2_states",
                "zombie_armor_bucket");
        showArmor(visible, zombie, "BRICK", null, "zombie_armor_brick");
        showArmor(visible, zombie, "CROWN", "_zombie_armor_crown_states",
                "zombie_armor_crown");
        showArmor(visible, zombie, "SHOULDER_ARMOR", "zombie_shoulder_armor",
                "zombie_shoulder_armor");
        if (zombie.getArmorSet().hasIntactArmor("CROWN")) {
            visible.put("knight_feather", true);
        }
        if (zombie.isButtered(tick)) {
            visible.put("butter", true);
        }
        return visible;
    }

    public Color tint(Zombie zombie, long tick) {
        if (zombie.isFrozen(tick)) {
            return FROZEN;
        }
        if (zombie.isChilled(tick)) {
            return CHILLED;
        }
        if (zombie.isPoisoned(tick)) {
            return POISONED;
        }
        if (zombie.isGlowing()) {
            return GLOWING;
        }
        return zombie.isAllied() ? ALLIED : NORMAL;
    }

    private static void showArmor(
            Map<String, Boolean> visible,
            Zombie zombie,
            String armorId,
            String parentPart,
            String partPrefix
    ) {
        ArmorInstance armor = zombie.getArmorSet().layers().stream()
                .filter(layer -> layer.spec().id().equalsIgnoreCase(armorId))
                .filter(layer -> !layer.isBroken())
                .findFirst()
                .orElse(null);
        if (armor == null) {
            return;
        }
        if (parentPart != null) {
            visible.put(parentPart, true);
        }
        double ratio = armor.remainingHealth() / armor.spec().maxHealth();
        String suffix = ratio > 0.66d
                ? "_norm"
                : ratio > 0.33d ? "_damage_01" : "_damage_02";
        visible.put(partPrefix + suffix, true);
    }

    private boolean assetExists(String path) {
        return assetsRoot.child("IMAGES/" + path).exists()
                || Gdx.files.internal("IMAGES/" + path).exists();
    }

    private static Map.Entry<String, Visual> visual(String id, String path) {
        return visual(id, path, 1f, 1.34f);
    }

    private static Map.Entry<String, Visual> visual(
            String id,
            String path,
            float widthInCells,
            float heightInCells
    ) {
        return Map.entry(id, new Visual(path, widthInCells, heightInCells));
    }

    public enum Motion {
        IDLE,
        WALK,
        ATTACK,
        DIE
    }

    public record Visual(
            String path,
            float widthInCells,
            float heightInCells
    ) {
    }
}
