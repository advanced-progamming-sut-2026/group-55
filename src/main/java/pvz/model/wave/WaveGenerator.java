package pvz.model.wave;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;
import pvz.model.entity.zombie.ZombieFactory;
import pvz.model.entity.zombie.ZombieRuntimeStats;
import pvz.model.entity.zombie.ZombieSpec;

public final class WaveGenerator {
    private final ZombieFactory zombieFactory;
    private final RandomGenerator random;

    public WaveGenerator(
            ZombieFactory zombieFactory,
            RandomGenerator random
    ) {
        this.zombieFactory = Objects.requireNonNull(
                zombieFactory,
                "zombie factory cannot be null"
        );
        this.random = Objects.requireNonNull(
                random,
                "random generator cannot be null"
        );
    }

    public List<Wave> generate(
            WaveConfiguration configuration,
            int rows,
            int difficultyLevel
    ) {
        Objects.requireNonNull(
                configuration,
                "wave configuration cannot be null"
        );
        if (rows <= 0) {
            throw new IllegalArgumentException("rows must be positive");
        }

        List<Candidate> candidates = configuration.allowedZombieIds()
                .stream()
                .map(id -> createCandidate(id, difficultyLevel))
                .toList();

        List<Wave> result = new ArrayList<>();
        for (WaveConfig config : configuration.waves()) {
            result.add(new Wave(
                    config.number(),
                    generateEntries(config.budget(), rows, candidates),
                    config.startDelayTicks(),
                    config.spawnIntervalTicks(),
                    config.finalWave()
            ));
        }
        return List.copyOf(result);
    }

    private Candidate createCandidate(String id, int difficultyLevel) {
        ZombieSpec spec = zombieFactory.getSpecById(id);
        if (spec == null) {
            throw new IllegalArgumentException(
                    "unknown zombie id in wave pool: " + id
            );
        }
        if (!spec.isImplemented()) {
            throw new IllegalArgumentException(
                    "planned zombie cannot be generated: " + id
            );
        }
        int cost = ZombieRuntimeStats.from(spec, difficultyLevel).waveCost();
        return new Candidate(spec, cost);
    }

    private List<WaveZombieEntry> generateEntries(
            int budget,
            int rows,
            List<Candidate> candidates
    ) {
        boolean[] reachable = calculateReachableBudgets(
                budget,
                candidates
        );
        if (!reachable[budget]) {
            throw new IllegalArgumentException(
                    "wave budget " + budget
                            + " cannot be filled exactly by the allowed zombies"
            );
        }

        List<WaveZombieEntry> entries = new ArrayList<>();
        int remaining = budget;
        while (remaining > 0) {
            int currentRemaining = remaining;
            List<Candidate> valid = candidates.stream()
                    .filter(candidate -> candidate.cost() <= currentRemaining)
                    .filter(candidate -> reachable[
                            currentRemaining - candidate.cost()
                    ])
                    .toList();
            Candidate chosen = chooseWeighted(valid);
            int lane = random.nextInt(1, rows + 1);
            entries.add(new WaveZombieEntry(
                    chosen.spec().getId(),
                    lane,
                    chosen.cost()
            ));
            remaining -= chosen.cost();
        }
        return List.copyOf(entries);
    }

    private boolean[] calculateReachableBudgets(
            int budget,
            List<Candidate> candidates
    ) {
        boolean[] reachable = new boolean[budget + 1];
        reachable[0] = true;
        for (int value = 1; value <= budget; value++) {
            for (Candidate candidate : candidates) {
                if (candidate.cost() <= value
                        && reachable[value - candidate.cost()]) {
                    reachable[value] = true;
                    break;
                }
            }
        }
        return reachable;
    }

    private Candidate chooseWeighted(List<Candidate> candidates) {
        long totalWeight = candidates.stream()
                .mapToLong(candidate -> candidate.spec().getWaveWeight())
                .sum();
        long selected = random.nextLong(totalWeight);
        long cursor = 0;
        for (Candidate candidate : candidates) {
            cursor += candidate.spec().getWaveWeight();
            if (selected < cursor) {
                return candidate;
            }
        }
        throw new IllegalStateException("weighted zombie selection failed");
    }

    private record Candidate(ZombieSpec spec, int cost) {
    }
}
