package pvz.model.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.quest.QuestEvent;
import pvz.model.quest.QuestObjective;
import pvz.model.quest.QuestProgress;
import pvz.model.quest.QuestProgressSource;
import pvz.model.quest.QuestResetPolicy;
import pvz.model.quest.QuestSpec;
import pvz.model.quest.QuestState;
import pvz.model.quest.UserQuestProgressSource;

/**
 * Runtime Travel Log service.
 *
 * <p>This class owns quest synchronization, daily-cycle handling, event
 * progress, claiming and persistence. Gameplay systems remain decoupled and
 * only need to publish {@link QuestEvent} values later.</p>
 */
public final class QuestService {
    private final UserManager userManager;
    private final QuestProgressSource progressSource;
    private final QuestRewardService rewardService;
    private final Clock clock;

    public QuestService(UserManager userManager) {
        this(
                userManager,
                new UserQuestProgressSource(),
                new QuestRewardService(),
                Clock.systemDefaultZone()
        );
    }

    public QuestService(
            UserManager userManager,
            QuestProgressSource progressSource,
            QuestRewardService rewardService,
            Clock clock
    ) {
        this.userManager = Objects.requireNonNull(
                userManager,
                "user manager cannot be null"
        );
        this.progressSource = Objects.requireNonNull(
                progressSource,
                "quest progress source cannot be null"
        );
        this.rewardService = Objects.requireNonNull(
                rewardService,
                "quest reward service cannot be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    /** Refreshes one quest from persistent user metrics without saving. */
    public boolean synchronize(User user, QuestSpec spec) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(spec, "quest spec cannot be null");

        boolean existed = user.getQuestLog().find(spec.id()) != null;
        QuestProgress progress = getOrInitializeProgress(user, spec);
        boolean changed = !existed;
        changed |= prepareCycle(user, spec, progress);

        if (progress.getState() != QuestState.AVAILABLE
                || !progressSource.supports(spec.objective().metric())) {
            return changed;
        }

        int sourceValue = progressSource.currentValue(user, spec.objective());
        int questValue = sourceQuestValue(spec, progress, sourceValue);
        int capped = Math.min(spec.objective().target(), questValue);

        if (capped != progress.getValue()) {
            progress.setValue(capped);
            changed = true;
        }
        if (capped >= spec.objective().target()) {
            progress.markCompleted();
            changed = true;
        }
        return changed;
    }

    /** Refreshes a group of quests and returns how many records changed. */
    public int synchronize(
            User user,
            Collection<QuestSpec> specs
    ) {
        Objects.requireNonNull(specs, "quest specs cannot be null");
        int changed = 0;
        for (QuestSpec spec : specs) {
            if (synchronize(user, spec)) {
                changed++;
            }
        }
        return changed;
    }

    /**
     * Migrates existing quest records when a newer game version makes a quest
     * available by default. Missing records are left untouched and will still
     * be created by normal synchronization.
     */
    public int restoreInitialAvailability(
            User user,
            Collection<QuestSpec> specs
    ) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(specs, "quest specs cannot be null");

        int changed = 0;
        for (QuestSpec spec : specs) {
            Objects.requireNonNull(spec, "quest spec cannot be null");
            if (!spec.initiallyAvailable()) {
                continue;
            }
            QuestProgress progress = user.getQuestLog().find(spec.id());
            if (progress != null
                    && progress.getState() == QuestState.UNAVAILABLE) {
                progress.activate();
                changed++;
            }
        }
        return changed;
    }

    /** Refreshes and persists only when the quest log actually changed. */
    public SyncResult synchronizeAndSave(
            User user,
            Collection<QuestSpec> specs
    ) {
        int changed = restoreInitialAvailability(user, specs);
        changed += synchronize(user, specs);
        if (changed == 0) {
            return new SyncResult(0, true, user);
        }
        if (userManager.save()) {
            return new SyncResult(changed, true, user);
        }
        return new SyncResult(
                changed,
                false,
                reloadUser(user.getUsername())
        );
    }

    /**
     * Applies one gameplay/adventure/minigame event in memory.
     * No screen or gameplay class is referenced here.
     */
    public boolean recordEvent(
            User user,
            QuestSpec spec,
            QuestEvent event
    ) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(spec, "quest spec cannot be null");
        Objects.requireNonNull(event, "quest event cannot be null");

        if (!matches(spec.objective(), event)) {
            return false;
        }

        boolean existed = user.getQuestLog().find(spec.id()) != null;
        QuestProgress progress = getOrInitializeProgress(user, spec);
        boolean changed = !existed;
        changed |= prepareCycle(user, spec, progress);
        if (progress.getState() != QuestState.AVAILABLE) {
            return changed;
        }

        long next = (long) progress.getValue() + event.amount();
        int capped = (int) Math.min(
                spec.objective().target(),
                Math.min(Integer.MAX_VALUE, next)
        );
        if (capped != progress.getValue()) {
            progress.setValue(capped);
            changed = true;
        }
        if (capped >= spec.objective().target()) {
            progress.markCompleted();
            changed = true;
        }
        return changed;
    }

    /** Applies an event to every matching quest without saving. */
    public int recordEvent(
            User user,
            Collection<QuestSpec> specs,
            QuestEvent event
    ) {
        Objects.requireNonNull(specs, "quest specs cannot be null");
        int changed = 0;
        for (QuestSpec spec : specs) {
            if (recordEvent(user, spec, event)) {
                changed++;
            }
        }
        return changed;
    }

    /**
     * Applies a batch of already-collected gameplay events without saving.
     *
     * <p>The returned value is the number of event-to-quest updates that
     * changed quest state. A single quest may contribute more than once when
     * several events advance it in the same batch.</p>
     */
    public int recordEvents(
            User user,
            Collection<QuestSpec> specs,
            Collection<QuestEvent> events
    ) {
        Objects.requireNonNull(events, "quest events cannot be null");
        int changed = 0;
        for (QuestEvent event : events) {
            changed += recordEvent(user, specs, Objects.requireNonNull(
                    event,
                    "quest event cannot be null"
            ));
        }
        return changed;
    }

    /** Applies one event and persists the resulting quest state. */
    public SyncResult recordEventAndSave(
            User user,
            Collection<QuestSpec> specs,
            QuestEvent event
    ) {
        int changed = recordEvent(user, specs, event);
        if (changed == 0) {
            return new SyncResult(0, true, user);
        }
        if (userManager.save()) {
            return new SyncResult(changed, true, user);
        }
        return new SyncResult(
                changed,
                false,
                reloadUser(user.getUsername())
        );
    }

    /**
     * Claims a completed quest. Rewards and CLAIMED state are written in one
     * UserManager save operation so a successful claim cannot be paid twice.
     */
    public ClaimResult claim(User user, QuestSpec spec) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(spec, "quest spec cannot be null");

        synchronize(user, spec);
        QuestProgress progress = getOrInitializeProgress(user, spec);
        QuestState state = progress.getState();

        if (state == QuestState.UNAVAILABLE) {
            return new ClaimResult(ClaimStatus.UNAVAILABLE, user, null);
        }
        if (state == QuestState.CLAIMED) {
            return new ClaimResult(ClaimStatus.ALREADY_CLAIMED, user, null);
        }
        if (state != QuestState.COMPLETED) {
            return new ClaimResult(ClaimStatus.NOT_COMPLETED, user, null);
        }

        QuestRewardService.Validation validation = rewardService.validate(
                user,
                spec.rewards()
        );
        if (!validation.valid()) {
            return new ClaimResult(
                    ClaimStatus.REWARD_BLOCKED,
                    user,
                    validation.message()
            );
        }

        rewardService.apply(user, spec.rewards());
        progress.markClaimed();

        if (userManager.save()) {
            return new ClaimResult(ClaimStatus.SUCCESS, user, null);
        }

        User reloaded = reloadUser(user.getUsername());
        return new ClaimResult(
                ClaimStatus.SAVE_FAILED,
                reloaded,
                "quest claim could not be saved; persisted state was reloaded"
        );
    }

    /** Changes future-content availability without coupling to its screen. */
    public boolean setAvailable(
            User user,
            QuestSpec spec,
            boolean available
    ) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(spec, "quest spec cannot be null");
        boolean existed = user.getQuestLog().find(spec.id()) != null;
        QuestProgress progress = getOrInitializeProgress(user, spec);
        QuestState before = progress.getState();

        if (available) {
            progress.activate();
        } else if (before == QuestState.AVAILABLE) {
            progress.markUnavailable();
        }
        return !existed || before != progress.getState();
    }

    private QuestProgress getOrInitializeProgress(
            User user,
            QuestSpec spec
    ) {
        QuestProgress existing = user.getQuestLog().find(spec.id());
        if (existing != null) {
            return existing;
        }

        QuestProgress created = user.getQuestLog().getOrCreate(spec.id());
        if (!spec.initiallyAvailable()) {
            created.markUnavailable();
        }
        return created;
    }

    private boolean prepareCycle(
            User user,
            QuestSpec spec,
            QuestProgress progress
    ) {
        if (spec.resetPolicy() != QuestResetPolicy.DAILY) {
            return false;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate currentCycle = progress.getCycleDate();
        int sourceValue = snapshotValue(user, spec.objective());
        int baseline = progressSource.supports(spec.objective().metric())
                ? sourceValue
                : 0;

        if (currentCycle == null) {
            // Compatibility path for a 5B1-era save that may already contain
            // progress but did not yet have service-managed cycle dates.
            int migratedBaseline = progressSource.supports(
                    spec.objective().metric()
            )
                    ? Math.max(0, sourceValue - progress.getValue())
                    : 0;
            progress.initializeCycle(today, migratedBaseline);
            return true;
        }

        if (!today.equals(currentCycle)) {
            boolean unavailable = progress.getState()
                    == QuestState.UNAVAILABLE;
            progress.resetForCycle(today, baseline);
            if (unavailable) {
                progress.markUnavailable();
            }
            return true;
        }
        return false;
    }

    private int snapshotValue(User user, QuestObjective objective) {
        if (!progressSource.supports(objective.metric())) {
            return 0;
        }
        return Math.max(0, progressSource.currentValue(user, objective));
    }

    private int sourceQuestValue(
            QuestSpec spec,
            QuestProgress progress,
            int sourceValue
    ) {
        int safeSource = Math.max(0, sourceValue);
        if (spec.resetPolicy() != QuestResetPolicy.DAILY) {
            return safeSource;
        }
        return Math.max(0, safeSource - progress.getBaselineValue());
    }

    private boolean matches(QuestObjective objective, QuestEvent event) {
        if (objective.metric() != event.metric()) {
            return false;
        }
        if (!objective.hasSubject()) {
            return true;
        }
        if (event.subjectId() == null) {
            return false;
        }
        return normalize(objective.subjectId())
                .equals(normalize(event.subjectId()));
    }

    private User reloadUser(String username) {
        userManager.reload();
        return userManager.find(candidate ->
                candidate.getUsername().equals(username)
        );
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    public enum ClaimStatus {
        SUCCESS,
        NOT_COMPLETED,
        ALREADY_CLAIMED,
        UNAVAILABLE,
        REWARD_BLOCKED,
        SAVE_FAILED
    }

    public record ClaimResult(
            ClaimStatus status,
            User user,
            String message
    ) {
        public boolean success() {
            return status == ClaimStatus.SUCCESS;
        }
    }

    public record SyncResult(
            int changedQuests,
            boolean saved,
            User user
    ) {
    }
}
