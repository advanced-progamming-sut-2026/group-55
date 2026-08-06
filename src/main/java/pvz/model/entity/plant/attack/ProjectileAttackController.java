package pvz.model.entity.plant.attack;

import java.util.Objects;
import java.util.function.IntPredicate;

public final class ProjectileAttackController {

    @FunctionalInterface
    public interface TargetChecker {
        boolean hasTarget(
                ShotPath path,
                int targetRow
        );
    }

    @FunctionalInterface
    public interface ProjectileLauncher {
        void launch(
                ShotPath path,
                int targetRow
        );
    }

    private final ProjectileAttackProfile profile;
    private final IntPredicate targetRowIsValid;
    private final TargetChecker targetChecker;
    private final ProjectileLauncher projectileLauncher;

    private boolean burstActive;
    private int nextBurstStep;
    private long nextBurstShotTick;

    public ProjectileAttackController(
            ProjectileAttackProfile profile,
            IntPredicate targetRowIsValid,
            TargetChecker targetChecker,
            ProjectileLauncher projectileLauncher
    ) {
        this.profile = Objects.requireNonNull(
                profile,
                "projectile attack profile cannot be null"
        );

        this.targetRowIsValid = Objects.requireNonNull(
                targetRowIsValid,
                "target row validator cannot be null"
        );

        this.targetChecker = Objects.requireNonNull(
                targetChecker,
                "target checker cannot be null"
        );

        this.projectileLauncher = Objects.requireNonNull(
                projectileLauncher,
                "projectile launcher cannot be null"
        );
    }

    public boolean hasOngoingAction() {
        return burstActive;
    }

    public boolean canStartAction(int sourceRow) {
        for (ShotPath path : profile.shotPaths()) {
            int targetRow = sourceRow + path.laneOffset();

            if (!targetRowIsValid.test(targetRow)) {
                continue;
            }

            if (targetChecker.hasTarget(path, targetRow)) {
                return true;
            }
        }

        return false;
    }

    public void startAction(
            long currentTick,
            int sourceRow
    ) {
        fireBurstStep(0, sourceRow);

        if (profile.burstLength() <= 1) {
            burstActive = false;
            return;
        }

        burstActive = true;
        nextBurstStep = 1;
        nextBurstShotTick =
                currentTick + profile.ticksBetweenShots();
    }

    public void updateOngoingAction(
            long currentTick,
            int sourceRow
    ) {
        if (!burstActive
                || currentTick < nextBurstShotTick) {
            return;
        }

        fireBurstStep(nextBurstStep, sourceRow);

        nextBurstStep++;

        if (nextBurstStep >= profile.burstLength()) {
            burstActive = false;
            return;
        }

        nextBurstShotTick =
                currentTick + profile.ticksBetweenShots();
    }

    public void cancelOngoingAction() {
        burstActive = false;
    }

    private void fireBurstStep(
            int burstStep,
            int sourceRow
    ) {
        for (ShotPath path : profile.shotPaths()) {
            if (burstStep >= path.shotsPerVolley()) {
                continue;
            }

            int targetRow = sourceRow + path.laneOffset();

            if (!targetRowIsValid.test(targetRow)) {
                continue;
            }

            projectileLauncher.launch(path, targetRow);
        }
    }
}
