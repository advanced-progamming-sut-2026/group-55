package pvz.graphics.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

/**
 * Owns the application's single PAM player and prepares animation metadata
 * one item at a time on the render thread.
 */
public final class PamAnimationService {
    private final PamPlayer player;
    private final Queue<AnimationKey> loadQueue = new ArrayDeque<>();
    private final Set<AnimationKey> pending = new HashSet<>();
    private final Set<AnimationKey> failed = new HashSet<>();
    private final Map<AnimationKey, Rectangle> boundsCache = new HashMap<>();
    private final Map<AnimationKey, List<CallbackRegistration>> callbacks =
            new HashMap<>();

    private boolean loadScheduled;
    private boolean disposed;

    public PamAnimationService(
            TextureBank textures,
            FileHandle assetsRoot
    ) {
        player = new PamPlayer(
                Objects.requireNonNull(textures, "textures cannot be null"),
                Objects.requireNonNull(
                        assetsRoot,
                        "assets root cannot be null"
                )
        );
    }

    public PamPlayer player() {
        return player;
    }

    /**
     * Delivers cached clip bounds asynchronously. A path/clip pair is loaded
     * only once, and only one new pair is prepared per render cycle.
     *
     * <p>The returned request is owned by the caller. Battle-local renderers
     * cancel it during disposal so the application-level animation service
     * cannot retain a dead screen through a queued callback.</p>
     */
    public AnimationRequest prepare(
            String pamPath,
            String clipName,
            Consumer<Rectangle> callback
    ) {
        Objects.requireNonNull(callback, "callback cannot be null");
        AnimationKey key = new AnimationKey(pamPath, clipName);
        CallbackRegistration registration = new CallbackRegistration(
                key,
                callback
        );

        if (disposed) {
            postResult(registration, null);
            return registration;
        }

        Rectangle cached = boundsCache.get(key);
        if (cached != null) {
            postResult(registration, cached);
            return registration;
        }
        if (failed.contains(key)) {
            postResult(registration, null);
            return registration;
        }

        callbacks.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(registration);
        if (pending.add(key)) {
            loadQueue.add(key);
        }
        scheduleNextLoad();
        return registration;
    }

    /**
     * Resolves the first usable clip from an ordered list. This keeps asset
     * naming differences out of screen code while preserving the same
     * one-at-a-time loading and failure cache used by {@link #prepare}.
     */
    public AnimationRequest prepareFirstAvailable(
            String pamPath,
            List<String> clipCandidates,
            Consumer<String> callback
    ) {
        Objects.requireNonNull(
                clipCandidates,
                "clip candidates cannot be null"
        );
        Objects.requireNonNull(callback, "callback cannot be null");

        List<String> candidates = clipCandidates.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(candidate -> !candidate.isEmpty())
                .distinct()
                .toList();

        FirstAvailableRequest request = new FirstAvailableRequest(
                pamPath,
                candidates,
                callback
        );
        request.start();
        return request;
    }

    public boolean isReady(String pamPath, String clipName) {
        return boundsCache.containsKey(new AnimationKey(pamPath, clipName));
    }

    public int cachedAnimationCount() {
        return boundsCache.size();
    }

    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;

        for (List<CallbackRegistration> waiting : callbacks.values()) {
            for (CallbackRegistration registration : waiting) {
                registration.cancelWithoutRemoving();
            }
        }
        loadQueue.clear();
        pending.clear();
        callbacks.clear();
        boundsCache.clear();
        failed.clear();
    }

    private void scheduleNextLoad() {
        if (loadScheduled || loadQueue.isEmpty() || disposed) {
            return;
        }
        loadScheduled = true;
        Gdx.app.postRunnable(this::loadNext);
    }

    private void loadNext() {
        AnimationKey key = loadQueue.poll();
        Rectangle bounds = null;
        if (key != null && !disposed) {
            try {
                Rectangle loaded = player.bounds(key.pamPath(), key.clipName());
                if (loaded != null && loaded.width > 0f && loaded.height > 0f) {
                    bounds = new Rectangle(loaded);
                    boundsCache.put(key, bounds);
                } else {
                    failed.add(key);
                }
            } catch (RuntimeException exception) {
                failed.add(key);
            }
        }

        if (key != null) {
            pending.remove(key);
            List<CallbackRegistration> waiting = callbacks.remove(key);
            if (waiting != null) {
                Rectangle result = bounds;
                for (CallbackRegistration registration : waiting) {
                    registration.deliver(result);
                }
            }
        }

        loadScheduled = false;
        scheduleNextLoad();
    }

    private void cancelRegistration(
            AnimationKey key,
            CallbackRegistration registration
    ) {
        List<CallbackRegistration> waiting = callbacks.get(key);
        if (waiting == null) {
            return;
        }
        waiting.remove(registration);
        if (!waiting.isEmpty()) {
            return;
        }

        callbacks.remove(key);
        if (pending.remove(key)) {
            loadQueue.remove(key);
        }
    }

    private void postResult(
            CallbackRegistration registration,
            Rectangle bounds
    ) {
        Rectangle result = copy(bounds);
        Gdx.app.postRunnable(() -> registration.deliver(result));
    }

    private static Rectangle copy(Rectangle bounds) {
        return bounds == null ? null : new Rectangle(bounds);
    }

    /** Handle for a queued animation callback. Cancellation is idempotent. */
    public interface AnimationRequest {
        void cancel();
    }

    private final class CallbackRegistration implements AnimationRequest {
        private AnimationKey key;
        private Consumer<Rectangle> callback;
        private boolean cancelled;
        private boolean delivered;

        private CallbackRegistration(
                AnimationKey key,
                Consumer<Rectangle> callback
        ) {
            this.key = key;
            this.callback = callback;
        }

        @Override
        public void cancel() {
            if (cancelled || delivered) {
                return;
            }
            cancelled = true;
            AnimationKey registeredKey = key;
            key = null;
            callback = null;
            if (registeredKey != null) {
                cancelRegistration(registeredKey, this);
            }
        }

        private void cancelWithoutRemoving() {
            if (cancelled || delivered) {
                return;
            }
            cancelled = true;
            key = null;
            callback = null;
        }

        private void deliver(Rectangle bounds) {
            if (cancelled || delivered) {
                return;
            }
            delivered = true;
            key = null;
            Consumer<Rectangle> recipient = callback;
            callback = null;
            if (recipient != null) {
                recipient.accept(copy(bounds));
            }
        }
    }

    private final class FirstAvailableRequest implements AnimationRequest {
        private final String pamPath;
        private final List<String> candidates;
        private Consumer<String> callback;
        private AnimationRequest activeRequest;
        private boolean cancelled;
        private boolean completed;

        private FirstAvailableRequest(
                String pamPath,
                List<String> candidates,
                Consumer<String> callback
        ) {
            this.pamPath = Objects.requireNonNull(
                    pamPath,
                    "PAM path cannot be null"
            );
            this.candidates = candidates;
            this.callback = callback;
        }

        private void start() {
            if (candidates.isEmpty()) {
                Gdx.app.postRunnable(() -> complete(null));
                return;
            }
            prepareCandidate(0);
        }

        private void prepareCandidate(int index) {
            if (cancelled || completed) {
                return;
            }
            if (index >= candidates.size()) {
                Gdx.app.postRunnable(() -> complete(null));
                return;
            }

            String candidate = candidates.get(index);
            activeRequest = prepare(pamPath, candidate, bounds -> {
                activeRequest = null;
                if (cancelled || completed) {
                    return;
                }
                if (bounds != null) {
                    complete(candidate);
                    return;
                }
                prepareCandidate(index + 1);
            });
        }

        private void complete(String clipName) {
            if (cancelled || completed) {
                return;
            }
            completed = true;
            activeRequest = null;
            Consumer<String> recipient = callback;
            callback = null;
            if (recipient != null) {
                recipient.accept(clipName);
            }
        }

        @Override
        public void cancel() {
            if (cancelled || completed) {
                return;
            }
            cancelled = true;
            callback = null;
            if (activeRequest != null) {
                activeRequest.cancel();
                activeRequest = null;
            }
        }
    }

    private record AnimationKey(String pamPath, String clipName) {
        private AnimationKey {
            Objects.requireNonNull(pamPath, "PAM path cannot be null");
            Objects.requireNonNull(clipName, "clip name cannot be null");
            if (pamPath.isBlank()) {
                throw new IllegalArgumentException("PAM path cannot be blank");
            }
            if (clipName.isBlank()) {
                throw new IllegalArgumentException("clip name cannot be blank");
            }
        }
    }
}
