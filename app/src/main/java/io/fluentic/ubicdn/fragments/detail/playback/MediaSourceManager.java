package io.fluentic.ubicdn.fragments.detail.playback;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import io.fluentic.ubicdn.extractor.stream.StreamInfo;
import io.fluentic.ubicdn.playlist.PlayQueue;
import io.fluentic.ubicdn.playlist.PlayQueueItem;
import io.fluentic.ubicdn.playlist.events.MoveEvent;
import io.fluentic.ubicdn.playlist.events.PlayQueueEvent;
import io.fluentic.ubicdn.playlist.events.RemoveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.SerialDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

public class MediaSourceManager {
    private final String TAG = "MediaSourceManager@" + Integer.toHexString(hashCode());
    // One-side rolling window size for default loading
    // Effectively loads windowSize * 2 + 1 streams, must be greater than 0
    private final int windowSize;
    private final PlaybackListener playbackListener;
    private final PlayQueue playQueue;

    // Process only the last load order when receiving a stream of load orders (lessens I/O)
    // The higher it is, the less loading occurs during rapid noncritical timeline changes
    // Not recommended to go below 100ms
    private final long loadDebounceMillis;
    private final PublishSubject<Long> loadSignal;
    private final Disposable debouncedLoader;

    private final DeferredMediaSource.Callback sourceBuilder;

    private DynamicConcatenatingMediaSource sources;

    private Subscription playQueueReactor;
    private SerialDisposable syncReactor;

    private boolean isBlocked;

    public MediaSourceManager(@NonNull final PlaybackListener listener,
                              @NonNull final PlayQueue playQueue) {
        this(listener, playQueue, 1, 1000L);
    }

    private MediaSourceManager(@NonNull final PlaybackListener listener,
                               @NonNull final PlayQueue playQueue,
                               final int windowSize,
                               final long loadDebounceMillis) {
        if (windowSize <= 0) {
            throw new UnsupportedOperationException("MediaSourceManager window size must be greater than 0");
        }

        this.playbackListener = listener;
        this.playQueue = playQueue;
        this.windowSize = windowSize;
        this.loadDebounceMillis = loadDebounceMillis;

        this.syncReactor = new SerialDisposable();
        this.loadSignal = PublishSubject.create();
        this.debouncedLoader = getDebouncedLoader();

        this.sourceBuilder = getSourceBuilder();

        this.sources = new DynamicConcatenatingMediaSource();

        playQueue.getBroadcastReceiver()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getReactor());
    }

    /*//////////////////////////////////////////////////////////////////////////
    // DeferredMediaSource listener
    //////////////////////////////////////////////////////////////////////////*/

    private DeferredMediaSource.Callback getSourceBuilder() {
        return new DeferredMediaSource.Callback() {
            @Override
            public MediaSource sourceOf(PlayQueueItem item, StreamInfo info) {
                return playbackListener.sourceOf(item, info);
            }
        };
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Exposed Methods
    //////////////////////////////////////////////////////////////////////////*/
    /**
     * Dispose the manager and releases all message buses and loaders.
     * */
    public void dispose() {
        if (loadSignal != null) loadSignal.onComplete();
        if (debouncedLoader != null) debouncedLoader.dispose();
        if (playQueueReactor != null) playQueueReactor.cancel();
        if (syncReactor != null) syncReactor.dispose();
        if (sources != null) sources.releaseSource();

        playQueueReactor = null;
        syncReactor = null;
        sources = null;
    }

    /**
     * Loads the current playing stream and the streams within its windowSize bound.
     *
     * Unblocks the player once the item at the current index is loaded.
     * */
    public void load() {
        loadSignal.onNext(System.currentTimeMillis());
    }

    /**
     * Blocks the player and repopulate the sources.
     *
     * Does not ensure the player is unblocked and should be done explicitly through {@link #load() load}.
     * */
    public void reset() {
        tryBlock();
        populateSources();
    }
    /*//////////////////////////////////////////////////////////////////////////
    // Event Reactor
    //////////////////////////////////////////////////////////////////////////*/

    private Subscriber<PlayQueueEvent> getReactor() {
        return new Subscriber<PlayQueueEvent>() {
            @Override
            public void onSubscribe(@NonNull Subscription d) {
                if (playQueueReactor != null) playQueueReactor.cancel();
                playQueueReactor = d;
                playQueueReactor.request(1);
            }

            @Override
            public void onNext(@NonNull PlayQueueEvent playQueueMessage) {
                if (playQueueReactor != null) onPlayQueueChanged(playQueueMessage);
            }

            @Override
            public void onError(@NonNull Throwable e) {}

            @Override
            public void onComplete() {}
        };
    }

    private void onPlayQueueChanged(final PlayQueueEvent event) {
        if (playQueue.isEmpty()) {
            playbackListener.shutdown();
            return;
        }

        // why no pattern matching in Java =(
        switch (event.type()) {
            case INIT:
            case REORDER:
            case ERROR:
                reset();
                break;
            case APPEND:
                populateSources();
                break;
            case SELECT:
                sync();
                break;
            case REMOVE:
                final RemoveEvent removeEvent = (RemoveEvent) event;
                remove(removeEvent.getRemoveIndex());
                // Sync only when the currently playing is removed
                if (removeEvent.getQueueIndex() == removeEvent.getRemoveIndex()) sync();
                break;
            case MOVE:
                final MoveEvent moveEvent = (MoveEvent) event;
                move(moveEvent.getFromIndex(), moveEvent.getToIndex());
                break;
            case RECOVERY:
            default:
                break;
        }

        switch (event.type()) {
            case INIT:
            case REORDER:
            case ERROR:
            case APPEND:
                loadInternal(); // low frequency, critical events
                break;
            case REMOVE:
            case SELECT:
            case MOVE:
            case RECOVERY:
            default:
                load(); // high frequency or noncritical events
                break;
        }

        if (!isPlayQueueReady()) {
            tryBlock();
            playQueue.fetch();
        }
        if (playQueueReactor != null) playQueueReactor.request(1);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Internal Helpers
    //////////////////////////////////////////////////////////////////////////*/

    private boolean isPlayQueueReady() {
        return playQueue.isComplete() || playQueue.size() - playQueue.getIndex() > windowSize;
    }

    private boolean tryBlock() {
        if (!isBlocked) {
            playbackListener.block();
            resetSources();
            isBlocked = true;
            return true;
        }
        return false;
    }

    private boolean tryUnblock() {
        if (isPlayQueueReady() && isBlocked && sources != null) {
            isBlocked = false;
            playbackListener.unblock(sources);
            return true;
        }
        return false;
    }

    private void sync() {
        final PlayQueueItem currentItem = playQueue.getItem();
        if (currentItem == null) return;

        final Consumer<StreamInfo> syncPlayback = new Consumer<StreamInfo>() {
            @Override
            public void accept(StreamInfo streamInfo) throws Exception {
                playbackListener.sync(currentItem, streamInfo);
            }
        };

        final Consumer<Throwable> onError = new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.e(TAG, "Sync error:", throwable);
                playbackListener.sync(currentItem,null);
            }
        };

        syncReactor.set(currentItem.getStream().subscribe(syncPlayback, onError));
    }

    private void loadInternal() {
        // The current item has higher priority
        final int currentIndex = playQueue.getIndex();
        final PlayQueueItem currentItem = playQueue.getItem(currentIndex);
        if (currentItem == null) return;
        loadItem(currentItem);

        // The rest are just for seamless playback
        final int leftBound = Math.max(0, currentIndex - windowSize);
        final int rightLimit = currentIndex + windowSize + 1;
        final int rightBound = Math.min(playQueue.size(), rightLimit);
        final List<PlayQueueItem> items = new ArrayList<>(playQueue.getStreams().subList(leftBound, rightBound));

        // Do a round robin
        final int excess = rightLimit - playQueue.size();
        if (excess >= 0) items.addAll(playQueue.getStreams().subList(0, Math.min(playQueue.size(), excess)));

        for (final PlayQueueItem item: items) loadItem(item);
    }

    private void loadItem(@Nullable final PlayQueueItem item) {
        if (item == null) return;

        final int index = playQueue.indexOf(item);
        if (index > sources.getSize() - 1) return;

        final DeferredMediaSource mediaSource = (DeferredMediaSource) sources.getMediaSource(playQueue.indexOf(item));
        if (mediaSource.state() == DeferredMediaSource.STATE_PREPARED) mediaSource.load();
        if (tryUnblock()) sync();
    }

    private void resetSources() {
        if (this.sources != null) this.sources.releaseSource();
        this.sources = new DynamicConcatenatingMediaSource();
    }

    private void populateSources() {
        if (sources == null) return;

        for (final PlayQueueItem item : playQueue.getStreams()) {
            insert(playQueue.indexOf(item), new DeferredMediaSource(item, sourceBuilder));
        }
    }

    private Disposable getDebouncedLoader() {
        return loadSignal
                .debounce(loadDebounceMillis, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long timestamp) throws Exception {
                loadInternal();
            }
        });
    }
    /*//////////////////////////////////////////////////////////////////////////
    // Media Source List Manipulation
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * Inserts a source into {@link DynamicConcatenatingMediaSource} with position
     * in respect to the play queue.
     *
     * If the play queue index already exists, then the insert is ignored.
     * */
    private void insert(final int queueIndex, final DeferredMediaSource source) {
        if (sources == null) return;
        if (queueIndex < 0 || queueIndex < sources.getSize()) return;

        sources.addMediaSource(queueIndex, source);
    }

    /**
     * Removes a source from {@link DynamicConcatenatingMediaSource} with the given play queue index.
     *
     * If the play queue index does not exist, the removal is ignored.
     * */
    private void remove(final int queueIndex) {
        if (sources == null) return;
        if (queueIndex < 0 || queueIndex > sources.getSize()) return;

        sources.removeMediaSource(queueIndex);
    }

    private void move(final int source, final int target) {
        if (sources == null) return;
        if (source < 0 || target < 0) return;
        if (source >= sources.getSize() || target >= sources.getSize()) return;

        sources.moveMediaSource(source, target);
    }
}
