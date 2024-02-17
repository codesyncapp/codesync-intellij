package org.intellij.sdk.codesync.database.enums;

public enum MigrationState {
    NOT_STARTED ("Not Started"),
    DONE ("Done"),
    IN_PROGRESS("In Progress"),
    ERROR("Error");

    private final String state;

    MigrationState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return state;
    }
}
