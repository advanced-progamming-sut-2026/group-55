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
    private final Map<AnimationKey, List<Consumer<Rectangle>>> callbacks =
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
     */
    public void prepare(
            String pamPath,
            String clipName,
            Consumer<Rectangle> callback
    ) {
        Objects.requireNonNull(callback, "callback cannot be null");
        AnimationKey key = new AnimationKey(pamPath, clipName);

        if (disposed) {
            postResult(callback, null);
            return;
        }

        Rectangle cached = boundsCache.get(key);
        if (cached != null) {
            postResult(callback, cached);
            return;
        }
        if (failed.contains(key)) {
            postResult(callback, null);
            return;
        }

        callbacks.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(callback);
        if (pending.add(key)) {
            loadQueue.add(key);
        }
        scheduleNextLoad();
    }

    /**
     * Resolves the first usable clip from an ordered list. This keeps asset
     * naming differences out of screen code while preserving the same
     * one-at-a-time loading and failure cache used by {@link #prepare}.
     */
    public void prepareFirstAvailable(
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

        if (candidates.isEmpty()) {
            postClipResult(callback, null);
            return;
        }
        prepareCandidate(pamPath, candidates, 0, callback);
    }

    public boolean isReady(String pamPath, String clipName) {
        return boundsCache.containsKey(new AnimationKey(pamPath, clipName));
    }

    public int cachedAnimationCount() {
        return boundsCache.size();
    }

    public void dispose() {
        disposed = true;
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
            List<Consumer<Rectangle>> waiting = callbacks.remove(key);
            if (waiting != null) {
                Rectangle result = bounds;
                for (Consumer<Rectangle> callback : waiting) {
                    callback.accept(copy(result));
                }
            }
        }

        loadScheduled = false;
        scheduleNextLoad();
    }

    private void postResult(
            Consumer<Rectangle> callback,
            Rectangle bounds
    ) {
        Rectangle result = copy(bounds);
        Gdx.app.postRunnable(() -> callback.accept(result));
    }

    private void prepareCandidate(
            String pamPath,
            List<String> candidates,
            int index,
            Consumer<String> callback
    ) {
        if (index >= candidates.size()) {
            postClipResult(callback, null);
            return;
        }

        String candidate = candidates.get(index);
        prepare(pamPath, candidate, bounds -> {
            if (bounds != null) {
                callback.accept(candidate);
                return;
            }
            prepareCandidate(pamPath, candidates, index + 1, callback);
        });
    }

    private static void postClipResult(
            Consumer<String> callback,
            String clipName
    ) {
        Gdx.app.postRunnable(() -> callback.accept(clipName));
    }

    private static Rectangle copy(Rectangle bounds) {
        return bounds == null ? null : new Rectangle(bounds);
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
