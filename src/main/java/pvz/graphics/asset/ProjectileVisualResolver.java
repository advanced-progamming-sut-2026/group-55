package pvz.graphics.asset;

import com.badlogic.gdx.graphics.Color;
import java.util.Locale;
import java.util.Objects;
import pvz.model.entity.Entity;
import pvz.model.entity.projectile.BowlingBulbProjectile;
import pvz.model.entity.projectile.DirectionalProjectile;
import pvz.model.entity.projectile.LobbedProjectile;
import pvz.model.entity.projectile.Projectile;
import pvz.model.entity.projectile.ProjectileFamily;
import pvz.model.entity.projectile.ProjectileModifierTarget;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.projectile.homing.HomingProjectile;

/** Maps live projectile models to PvZ2 art with a safe generated fallback. */
public final class ProjectileVisualResolver {
    private static final String PEA_TEXTURE = "IMAGE_PROJECTILEPEA";
    private static final String BUTTER_TEXTURE =
            "IMAGE_EFFECTS_KERNELPULT_PROJECTILE_BUTTER";

    public Visual visual(Entity projectile) {
        Objects.requireNonNull(projectile, "projectile cannot be null");
        String name = normalize(projectile.getName());
        ProjectileType type = projectileType(projectile, name);

        if (projectile instanceof BowlingBulbProjectile bulb) {
            return bowlingVisual(name, type, bulb.isExplosiveProjectile());
        }
        if (projectile instanceof LobbedProjectile lobbed) {
            return lobbedVisual(name, type, lobbed.isButterProjectile());
        }
        if (projectile instanceof HomingProjectile) {
            return homingVisual(name, type);
        }
        if (projectile instanceof ProjectileModifierTarget target
                && target.getProjectileFamily() == ProjectileFamily.PEA) {
            return new Visual(
                    PEA_TEXTURE,
                    "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
                    "animation",
                    typeTint(type),
                    0.30f,
                    0.30f,
                    Flight.STRAIGHT,
                    false
            );
        }
        return directVisual(name, type,
                projectile instanceof DirectionalProjectile
                        ? Flight.DIRECTIONAL : Flight.STRAIGHT);
    }

    public boolean supports(Entity entity) {
        return entity instanceof Projectile
                || entity instanceof DirectionalProjectile
                || entity instanceof LobbedProjectile
                || entity instanceof BowlingBulbProjectile
                || entity instanceof HomingProjectile;
    }

    private Visual directVisual(
            String name,
            ProjectileType type,
            Flight flight
    ) {
        if (name.contains("cactus")) {
            return pam("768/INITIAL/EFFECTS/T_CACTUS_PROJECTILE/"
                    + "T_CACTUS_PROJECTILE.PAM", "idle", type,
                    0.54f, 0.30f, flight, false);
        }
        if (name.contains("starfruit")) {
            return pam("768/INITIAL/EFFECTS/T_STARFRUIT_PROJECTILE/"
                    + "T_STARFRUIT_PROJECTILE.PAM", "animation", type,
                    0.42f, 0.42f, flight, false);
        }
        if (name.contains("rotobaga")) {
            return pam("768/FULL/EFFECTS/T_ROTORUTABAGA_PROJECTILE1/"
                    + "T_ROTORUTABAGA_PROJECTILE1.PAM", "animation", type,
                    0.45f, 0.38f, flight, false);
        }
        if (name.contains("citron")) {
            return pam("768/FULL/EFFECTS/T_CITRON_CITRUS_ORB/"
                    + "T_CITRON_CITRUS_ORB.PAM", "Citron_Citrus_Orb", type,
                    0.55f, 0.55f, flight, true);
        }
        if (name.contains("goo peashooter")) {
            return pam("768/INITIAL/EFFECTS/GOOPEASHOOTER_PROJECTILES/"
                    + "GOOPEASHOOTER_PROJECTILES.PAM", "projectile_t1", type,
                    0.35f, 0.35f, flight, false);
        }
        if (name.contains("sea-shroom")) {
            return pam("768/FULL/EFFECTS/SEASHROOM_PROJECTILE/"
                    + "SEASHROOM_PROJECTILE.PAM", "animation", type,
                    0.34f, 0.34f, flight, false);
        }
        if (name.contains("puff-shroom")) {
            return pam("768/INITIAL/EFFECTS/T_PUFFSHROOM_PROJECTILE/"
                    + "T_PUFFSHROOM_PROJECTILE.PAM", "animation", type,
                    0.34f, 0.34f, flight, false);
        }
        if (name.contains("fume-shroom")) {
            return pam("768/INITIAL/EFFECTS/FUMESHROOM_BUBBLES/"
                    + "FUMESHROOM_BUBBLES.PAM", "special", type,
                    0.56f, 0.40f, flight, false);
        }
        return fallback(type, 0.32f, 0.32f, flight, false);
    }

    private Visual lobbedVisual(
            String name,
            ProjectileType type,
            boolean butter
    ) {
        if (butter) {
            return new Visual(BUTTER_TEXTURE, null, null,
                    Color.WHITE, 0.48f, 0.40f, Flight.LOBBED, false);
        }
        if (name.contains("cabbage-pult")) {
            return pam("768/INITIAL/EFFECTS/T_CABBAGEPULT_PROJECTILE/"
                    + "T_CABBAGEPULT_PROJECTILE.PAM", "animation", type,
                    0.53f, 0.53f, Flight.LOBBED, false);
        }
        if (name.contains("kernel-pult")) {
            return pam("768/INITIAL/EFFECTS/T_KERNALPULT_PROJECTILE/"
                    + "T_KERNALPULT_PROJECTILE.PAM", "animation", type,
                    0.42f, 0.42f, Flight.LOBBED, false);
        }
        if (name.contains("winter melon")) {
            return pam("768/FULL/EFFECTS/T_WINTERMELON_PROJECTILE/"
                    + "T_WINTERMELON_PROJECTILE.PAM", "animation", type,
                    0.68f, 0.68f, Flight.LOBBED, true);
        }
        if (name.contains("melon-pult")) {
            return pam("768/INITIAL/EFFECTS/T_MELON_PROJECTILE/"
                    + "T_MELON_PROJECTILE.PAM", "animation", type,
                    0.66f, 0.66f, Flight.LOBBED, true);
        }
        if (name.contains("pepper-pult")) {
            return pam("768/FULL/EFFECTS/T_PEPPERPULT_PROJECTILE/"
                    + "T_PEPPERPULT_PROJECTILE.PAM", "animation", type,
                    0.62f, 0.62f, Flight.LOBBED, true);
        }
        return fallback(type, 0.50f, 0.50f, Flight.LOBBED, false);
    }

    private Visual bowlingVisual(
            String name,
            ProjectileType type,
            boolean explosive
    ) {
        String folder;
        float size;
        if (name.contains("orange") || name.contains("charged")) {
            folder = "BOWLINGBULB_PROJECTILE3";
            size = 0.82f;
        } else if (name.contains("blue")) {
            folder = "BOWLINGBULB_PROJECTILE2";
            size = 0.70f;
        } else {
            folder = "BOWLINGBULB_PROJECTILE1";
            size = 0.60f;
        }
        return pam("768/FULL/EFFECTS/" + folder + "/" + folder + ".PAM",
                "animation", type, size, size, Flight.BOWLING, explosive);
    }

    private Visual homingVisual(String name, ProjectileType type) {
        if (name.contains("caulipower")) {
            return pam("768/INITIAL/EFFECTS/CAULIPOWER_PROJECTILE/"
                    + "CAULIPOWER_PROJECTILE.PAM", "animation", type,
                    0.48f, 0.48f, Flight.HOMING, false);
        }
        if (name.contains("electric blueberry")) {
            return pam("768/INITIAL/EFFECTS/"
                    + "ELECTRICBLUEBERRY_CLOUD_PROJECTILE/"
                    + "ELECTRICBLUEBERRY_CLOUD_PROJECTILE.PAM",
                    "idle", ProjectileType.ELECTRIC,
                    0.55f, 0.55f, Flight.HOMING, true);
        }
        return pam("768/INITIAL/EFFECTS/T_HOMING_THISTLE_PROJECTILE/"
                + "T_HOMING_THISTLE_PROJECTILE.PAM", "animation", type,
                0.45f, 0.35f, Flight.HOMING, false);
    }

    private static Visual pam(
            String path,
            String clip,
            ProjectileType type,
            float width,
            float height,
            Flight flight,
            boolean largeImpact
    ) {
        return new Visual(null, path, clip, typeTint(type), width, height,
                flight, largeImpact);
    }

    private static Visual fallback(
            ProjectileType type,
            float width,
            float height,
            Flight flight,
            boolean largeImpact
    ) {
        return new Visual(null, null, null, typeTint(type), width, height,
                flight, largeImpact);
    }

    private static ProjectileType projectileType(
            Entity projectile,
            String name
    ) {
        if (projectile instanceof ProjectileModifierTarget target) {
            return target.getProjectileType();
        }
        if (projectile instanceof LobbedProjectile lobbed) {
            return lobbed.getProjectileType();
        }
        if (projectile instanceof BowlingBulbProjectile bowling) {
            return bowling.getProjectileType();
        }
        return name.contains("electric")
                ? ProjectileType.ELECTRIC : ProjectileType.NORMAL;
    }

    private static Color typeTint(ProjectileType type) {
        return switch (type) {
            case NORMAL -> Color.WHITE;
            case ELECTRIC -> new Color(1f, 0.92f, 0.18f, 1f);
            case FIRE -> new Color(1f, 0.48f, 0.10f, 1f);
            case ICE -> new Color(0.48f, 0.88f, 1f, 1f);
            case POISON -> new Color(0.62f, 0.30f, 0.92f, 1f);
        };
    }

    private static String normalize(String name) {
        return name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
    }

    public enum Flight {
        STRAIGHT,
        DIRECTIONAL,
        HOMING,
        LOBBED,
        BOWLING
    }

    public record Visual(
            String textureId,
            String pamPath,
            String clip,
            Color tint,
            float widthInCells,
            float heightInCells,
            Flight flight,
            boolean largeImpact
    ) {
        public Visual {
            Objects.requireNonNull(tint, "tint cannot be null");
            Objects.requireNonNull(flight, "flight cannot be null");
            if (widthInCells <= 0f || heightInCells <= 0f) {
                throw new IllegalArgumentException(
                        "projectile visual size must be positive");
            }
        }
    }
}
