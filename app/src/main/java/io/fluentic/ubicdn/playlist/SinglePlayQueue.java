package io.fluentic.ubicdn.playlist;

import io.fluentic.ubicdn.extractor.stream.StreamInfo;

import java.util.Collections;

public final class SinglePlayQueue extends PlayQueue {
    public SinglePlayQueue(final StreamInfo info) {
        super(0, Collections.singletonList(new PlayQueueItem(info)));
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void fetch() {}
}
