package org.intellij.sdk.codesync.enums;

public enum RepoState {
    SYNCED ("Synced"),
    NOT_SYNCED("Not Synced"),
    DELETED("Deleted"),
    DISCONNECTED("Disconnected");

    private final String state;

    RepoState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return state;
    }
}
