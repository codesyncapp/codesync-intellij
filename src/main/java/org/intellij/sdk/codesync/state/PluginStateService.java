package org.intellij.sdk.codesync.state;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "PluginStateService", storages = { @Storage("PluginState.xml") })
public class PluginStateService implements PersistentStateComponent<PluginState> {
    private PluginState pluginState = new PluginState();

    public PluginState getState() {
        return pluginState;
    }

    public void loadState(@NotNull PluginState state) {
        pluginState = state;
    }
}
