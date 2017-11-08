package io.fluentic.ubicdn.playlist.events;

public class InitEvent implements PlayQueueEvent {
    @Override
    public PlayQueueEventType type() {
        return PlayQueueEventType.INIT;
    }
}
