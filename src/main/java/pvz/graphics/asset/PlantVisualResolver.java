package pvz.graphics.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import pvz.libpvz.textures.TextureBank;

/** Resolves plant packet art and PAM animation metadata in one place. */
public final class PlantVisualResolver {
    private static final Map<String, String> PAM_CACHE = new HashMap<>();
    private static final Map<String, String> PAM_ALIASES = Map.ofEntries(
            Map.entry("twin sunflower", "768/INITIAL/PLANT/SUNFLOWER_TWIN/SUNFLOWER_TWIN.PAM"),
            Map.entry("rotobaga", "768/FULL/PLANT/ROTORUTABAGA/ROTORUTABAGA.PAM"),
            Map.entry("mega gatling pea", "768/INITIAL/PLANT/MEGAGATLING/MEGAGATLING.PAM"),
            Map.entry("kernel-pult", "768/INITIAL/PLANT/KERNALPULT/KERNALPULT.PAM"),
            Map.entry("primal potato mine", "768/FULL/PLANT/PRIMAL_POTATOMINE/PRIMAL_POTATOMINE.PAM"),
            Map.entry("iceberg lettuce", "768/INITIAL/PLANT/ICEBURG/ICEBURG.PAM"),
            Map.entry("phat beet", "768/FULL/PLANT/PHATBEETS/PHATBEETS.PAM"),
            Map.entry("enlighten-mint", "768/INITIAL/EMPOWERMINTS/PLANT/ENLIGHTENMINT/ENLIGHTENMINT.PAM"),
            Map.entry("appease-mint", "768/INITIAL/EMPOWERMINTS/PLANT/APPEASEMINT/APPEASEMINT.PAM"),
            Map.entry("arma-mint", "768/INITIAL/EMPOWERMINTS/PLANT/ARMAMINT/ARMAMINT.PAM"),
            Map.entry("bombard-mint", "768/INITIAL/EMPOWERMINTS/PLANT/BOMBARDMINT/BOMBARDMINT.PAM"),
            Map.entry("enforce-mint", "768/INITIAL/EMPOWERMINTS/PLANT/ENFORCEMINT/ENFORCEMINT.PAM"),
            Map.entry("reinforce-mint", "768/INITIAL/EMPOWERMINTS/PLANT/REINFORCEMINT/REINFORCEMINT.PAM"),
            Map.entry("enchant-mint", "768/INITIAL/EMPOWERMINTS/PLANT/ENCHANTMINT/ENCHANTMINT.PAM"),
            Map.entry("pierce-mint", "768/INITIAL/EMPOWERMINTS/PLANT/SPEARMINT/SPEARMINT.PAM")
    );
    private static final Map<String, String> CLIP_ALIASES = Map.ofEntries(
            Map.entry("sun-shroom", "idle_stage1"),
            Map.entry("caulipower", "idle1_1"),
            Map.entry("electric blueberry", "idle1_1"),
            Map.entry("puff-shroom", "idle_stage1"),
            Map.entry("doom-shroom", "stage1_idle"),
            Map.entry("kiwibeast", "idle_stage1_"),
            Map.entry("grave buster", "attack"),
            Map.entry("enlighten-mint", "loop"),
            Map.entry("appease-mint", "loop"),
            Map.entry("arma-mint", "loop"),
            Map.entry("bombard-mint", "loop"),
            Map.entry("enforce-mint", "loop"),
            Map.entry("reinforce-mint", "loop"),
            Map.entry("enchant-mint", "loop"),
            Map.entry("pierce-mint", "loop")
    );
    private static final Map<String, String> PREVIEW_ALIASES = Map.of(
            "rotobaga", "IMAGE_PLANT_ROTORUTABAGA_ROTORUTABAGA_73X127",
            "goo peashooter", "IMAGE_UI_PACKETS_POISONPEASHOOTER",
            "mega gatling pea", "IMAGE_UI_PACKETS_MEGAGATLING",
            "iceberg lettuce", "IMAGE_UI_PACKETS_ICEBURG",
            "pierce-mint", "IMAGE_UI_PACKETS_SPEARMINT"
    );

    private final TextureBank textures;
    private final FileHandle assetsRoot;

    public PlantVisualResolver(TextureBank textures, FileHandle assetsRoot) {
        this.textures = Objects.requireNonNull(textures, "textures cannot be null");
        this.assetsRoot = Objects.requireNonNull(
                assetsRoot,
                "assets root cannot be null"
        );
    }

    public TextureRegion preview(String plantName) {
        String key = normalizeKey(plantName);
        String alias = PREVIEW_ALIASES.get(key);
        if (alias != null) {
            TextureRegion aliased = textures.region(alias);
            if (aliased != null) {
                return aliased;
            }
        }

        String normalized = assetName(plantName);
        TextureRegion preview = textures.region(
                "IMAGE_UI_PACKETS_" + normalized.replace("_", "")
        );
        return preview != null
                ? preview
                : textures.region("IMAGE_UI_PACKETS_" + normalized);
    }

    public String animationPath(String plantName) {
        String key = normalizeKey(plantName);
        if (PAM_CACHE.containsKey(key)) {
            return PAM_CACHE.get(key);
        }

        String alias = PAM_ALIASES.get(key);
        if (alias != null && assetExists(alias)) {
            PAM_CACHE.put(key, alias);
            return alias;
        }

        String normalized = assetName(plantName);
        String compact = normalized.replace("_", "");
        for (String folder : new String[]{"INITIAL", "FULL"}) {
            String regular = "768/" + folder + "/PLANT/"
                    + normalized + "/" + normalized + ".PAM";
            if (assetExists(regular)) {
                PAM_CACHE.put(key, regular);
                return regular;
            }

            String compactPath = "768/" + folder + "/PLANT/"
                    + compact + "/" + compact + ".PAM";
            if (assetExists(compactPath)) {
                PAM_CACHE.put(key, compactPath);
                return compactPath;
            }
        }
        return null;
    }

    public String animationClip(String plantName) {
        return CLIP_ALIASES.getOrDefault(normalizeKey(plantName), "idle");
    }

    private boolean assetExists(String path) {
        return assetsRoot.child("IMAGES/" + path).exists()
                || Gdx.files.internal("IMAGES/" + path).exists();
    }

    private static String normalizeKey(String plantName) {
        Objects.requireNonNull(plantName, "plant name cannot be null");
        return plantName.strip().toLowerCase(Locale.ROOT);
    }

    private static String assetName(String plantName) {
        return normalizeKey(plantName).toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }
}
