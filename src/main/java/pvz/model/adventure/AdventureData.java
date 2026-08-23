package pvz.model.adventure;

import java.util.Map;
import java.util.Objects;
import pvz.model.wave.WaveConfiguration;

public record AdventureData(
        LevelCatalog catalog,
        Map<String, WaveConfiguration> wavesByLevelId
) {
    public AdventureData {
        Objects.requireNonNull(catalog, "level catalog cannot be null");
        wavesByLevelId = Map.copyOf(
                Objects.requireNonNull(
                        wavesByLevelId,
                        "wave configurations cannot be null"
                )
        );
    }
}
