package pvz.model.entity.zombie.behavior;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import pvz.model.entity.zombie.ZombieBehaviorDefinition;

public final class ZombieBehaviorFactory {
    private static final Map<String, Set<String>> PARAMETER_NAMES = Map.ofEntries(
            Map.entry("TOMB_SPAWN", Set.of(
                    "intervalSeconds", "tombsPerCast"
            )),
            Map.entry("FREEZE_RESISTANCE", Set.of()),
            Map.entry("FIRE_RESISTANCE", Set.of()),
            Map.entry("LOBBED_SHIELD", Set.of()),
            Map.entry("CRUSH_PLANT", Set.of("requiredArmorId")),
            Map.entry("PUSH_OBSTACLES", Set.of(
                    "obstacleId", "obstacleName", "obstacleHealth",
                    "obstacleCount", "spacingTiles",
                    "blocksStraightProjectiles", "crushesPlants",
                    "meltsOnFire", "spawnOnDestroyZombieId",
                    "spawnOnDestroyCount", "spawnOnDestroySpacingTiles"
            )),
            Map.entry("GARGANTUAR", Set.of(
                    "throwHealthRatio", "impTargetColumn", "impZombieId"
            )),
            Map.entry("RA_SUN_STEAL", Set.of("maximumStolenSun")),
            Map.entry("EXPLORER_TORCH", Set.of()),
            Map.entry("DODO_VAULT", Set.of(
                    "minimumVaultTiles", "maximumVaultTiles",
                    "minimumObstacleHealth"
            )),
            Map.entry("HUNTER_ICE", Set.of(
                    "rangeTiles", "hitsToFreeze", "attackIntervalSeconds"
            )),
            Map.entry("FISHERMAN", Set.of(
                    "maximumRange", "pullDistanceTiles",
                    "discardDistanceTiles", "castIntervalSeconds"
            )),
            Map.entry("OCTOPUS_THROW", Set.of(
                    "rangeTiles", "attackIntervalSeconds"
            )),
            Map.entry("SNORKEL", Set.of()),
            Map.entry("JUGGLER_REFLECT", Set.of(
                    "spinningSpeedMultiplier", "spinDurationSeconds"
            )),
            Map.entry("WIZARD_TRANSFORM", Set.of("castIntervalSeconds")),
            Map.entry("KING_KNIGHT", Set.of(
                    "eligibleZombieId", "columnRange", "rowRange",
                    "castIntervalSeconds", "crownArmorId",
                    "crownArmorName", "crownHealth", "crownMetallic",
                    "shoulderArmorId", "shoulderArmorName",
                    "shoulderHealth", "shoulderMetallic"
            )),
            Map.entry("ALLSTAR", Set.of(
                    "runningMultiplier", "walkingMultiplier"
            )),
            Map.entry("TURQUOISE_LASER", Set.of(
                    "detectionRadiusTiles", "laserRangeTiles",
                    "sunPerSecond", "chargingSeconds", "cooldownSeconds",
                    "sunDropRatio"
            )),
            Map.entry("PROSPECTOR", Set.of(
                    "launchDelaySeconds", "launchTargetColumn"
            )),
            Map.entry("PIANO_LANE_SHIFT", Set.of(
                    "shiftIntervalSeconds", "laneShiftTiles"
            )),
            Map.entry("NEWS_ENRAGE", Set.of(
                    "speedMultiplier", "damageMultiplier", "triggerArmorId"
            ))
    );

    public ZombieBehavior create(ZombieBehaviorDefinition definition) {
        return create(definition, 1);
    }

    public ZombieBehavior create(
            ZombieBehaviorDefinition definition,
            double healthMultiplier
    ) {
        if (!Double.isFinite(healthMultiplier) || healthMultiplier <= 0) {
            throw new IllegalArgumentException(
                    "health multiplier must be a positive number"
            );
        }
        validateParameterNames(definition);
        return switch (definition.type()) {
            case "TOMB_SPAWN" -> new TombSpawnBehavior(
                    definition.requirePositiveInt("intervalSeconds"),
                    definition.requirePositiveInt("tombsPerCast")
            );
            case "FREEZE_RESISTANCE" -> new FreezeResistanceBehavior();
            case "FIRE_RESISTANCE" -> new FireResistanceBehavior();
            case "LOBBED_SHIELD" -> new LobbedShieldBehavior();
            case "CRUSH_PLANT" -> new CrushPlantBehavior(
                    definition.optionalText("requiredArmorId")
            );
            case "PUSH_OBSTACLES" -> new PushObstacleBehavior(
                    definition.requireText("obstacleId"),
                    definition.requireText("obstacleName"),
                    definition.requirePositiveDouble("obstacleHealth")
                            * healthMultiplier,
                    definition.requirePositiveInt("obstacleCount"),
                    definition.requirePositiveDouble("spacingTiles"),
                    definition.requireBoolean("blocksStraightProjectiles"),
                    definition.requireBoolean("crushesPlants"),
                    definition.requireBoolean("meltsOnFire"),
                    definition.optionalText("spawnOnDestroyZombieId"),
                    definition.optionalPositiveInt(
                            "spawnOnDestroyCount",
                            0
                    ),
                    definition.optionalPositiveDouble(
                            "spawnOnDestroySpacingTiles",
                            0
                    )
            );
            case "GARGANTUAR" -> new GargantuarBehavior(
                    definition.requirePositiveDouble("throwHealthRatio"),
                    definition.requirePositiveInt("impTargetColumn"),
                    definition.requireText("impZombieId")
            );
            case "RA_SUN_STEAL" -> new RaSunStealBehavior(
                    definition.requirePositiveInt("maximumStolenSun")
            );
            case "EXPLORER_TORCH" -> new ExplorerTorchBehavior();
            case "DODO_VAULT" -> new DodoVaultBehavior(
                    definition.requirePositiveInt("minimumVaultTiles"),
                    definition.requirePositiveInt("maximumVaultTiles"),
                    definition.requirePositiveInt("minimumObstacleHealth")
            );
            case "HUNTER_ICE" -> new HunterIceBehavior(
                    definition.requirePositiveInt("rangeTiles"),
                    definition.requirePositiveInt("hitsToFreeze"),
                    definition.requirePositiveDouble("attackIntervalSeconds")
            );
            case "FISHERMAN" -> new FishermanBehavior(
                    definition.requirePositiveInt("maximumRange"),
                    definition.requirePositiveInt("pullDistanceTiles"),
                    definition.requirePositiveInt("discardDistanceTiles"),
                    definition.requirePositiveDouble("castIntervalSeconds")
            );
            case "OCTOPUS_THROW" -> new OctopusThrowBehavior(
                    definition.requirePositiveInt("rangeTiles"),
                    definition.requirePositiveDouble("attackIntervalSeconds")
            );
            case "SNORKEL" -> new SnorkelBehavior();
            case "JUGGLER_REFLECT" -> new JugglerReflectBehavior(
                    definition.requirePositiveDouble("spinningSpeedMultiplier"),
                    definition.requirePositiveDouble("spinDurationSeconds")
            );
            case "WIZARD_TRANSFORM" -> new WizardTransformBehavior(
                    definition.requirePositiveDouble("castIntervalSeconds")
            );
            case "KING_KNIGHT" -> new KingKnightBehavior(
                    definition.requireText("eligibleZombieId"),
                    definition.requirePositiveInt("columnRange"),
                    definition.requirePositiveInt("rowRange"),
                    definition.requirePositiveDouble("castIntervalSeconds"),
                    definition.requireText("crownArmorId"),
                    definition.requireText("crownArmorName"),
                    definition.requirePositiveDouble("crownHealth"),
                    definition.requireBoolean("crownMetallic"),
                    definition.requireText("shoulderArmorId"),
                    definition.requireText("shoulderArmorName"),
                    definition.requirePositiveDouble("shoulderHealth"),
                    definition.requireBoolean("shoulderMetallic")
            );
            case "ALLSTAR" -> new AllStarBehavior(
                    definition.requirePositiveDouble("runningMultiplier"),
                    definition.requirePositiveDouble("walkingMultiplier")
            );
            case "TURQUOISE_LASER" -> new TurquoiseLaserBehavior(
                    definition.requirePositiveInt("detectionRadiusTiles"),
                    definition.requirePositiveInt("laserRangeTiles"),
                    definition.requirePositiveInt("sunPerSecond"),
                    definition.requirePositiveDouble("chargingSeconds"),
                    definition.requirePositiveDouble("cooldownSeconds"),
                    definition.requireRatio("sunDropRatio")
            );
            case "PROSPECTOR" -> new ProspectorBehavior(
                    definition.requirePositiveDouble("launchDelaySeconds"),
                    definition.requirePositiveInt("launchTargetColumn")
            );
            case "PIANO_LANE_SHIFT" -> new PianoLaneShiftBehavior(
                    definition.requirePositiveDouble("shiftIntervalSeconds"),
                    definition.requirePositiveInt("laneShiftTiles")
            );
            case "NEWS_ENRAGE" -> new NewspaperEnrageBehavior(
                    definition.requirePositiveDouble("speedMultiplier"),
                    definition.requirePositiveDouble("damageMultiplier"),
                    definition.requireText("triggerArmorId")
            );
            default -> throw new IllegalArgumentException(
                    "unknown or unimplemented zombie behavior: "
                            + definition.type()
            );
        };
    }

    private void validateParameterNames(
            ZombieBehaviorDefinition definition
    ) {
        Set<String> expected = PARAMETER_NAMES.get(definition.type());
        if (expected == null) {
            return;
        }
        Set<String> unexpected = new LinkedHashSet<>(
                definition.parameters().keySet()
        );
        unexpected.removeAll(expected);
        if (!unexpected.isEmpty()) {
            throw new IllegalArgumentException(
                    "unexpected parameter(s) for " + definition.type()
                            + ": " + String.join(", ", unexpected)
            );
        }
    }
}
