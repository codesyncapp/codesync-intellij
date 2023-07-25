package org.intellij.sdk.codesync.eventBus;

import com.intellij.util.messages.Topic;

public interface CodeSyncEventBus {
    Topic<CodeSyncEventBus> TOPIC = Topic.create("CodeSyncEventBus", CodeSyncEventBus.class);

    void onEvent(); // Define your event method here
}
