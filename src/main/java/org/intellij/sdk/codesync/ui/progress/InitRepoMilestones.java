package org.intellij.sdk.codesync.ui.progress;

public final class InitRepoMilestones {
    private InitRepoMilestones() {
        // restrict instantiation
    }

    // Milestone
    public static class MileStone {
        double mileage;
        String message;

        public MileStone(double mileage, String message) {
            this.mileage = mileage;
            this.message = message;
        }
    }

    public static MileStone START = new MileStone(0.00, "Sync started");
    public static MileStone CHECK_USER_ACCESS = new MileStone(0.10, "Checking access");
    public static MileStone FETCH_FILES = new MileStone(0.20, "Fetching files");
    public static MileStone COPY_FILES = new MileStone(0.30, "Copying files");
    public static MileStone SENDING_REPO = new MileStone(0.50, "Sending file to the server");
    public static MileStone PROCESS_RESPONSE = new MileStone(0.60, "Processing server response");
    public static MileStone CONFIG_UPDATE = new MileStone(0.70, "Updating local config");
    public static MileStone CLEANUP = new MileStone(0.90, "Cleaning up");
    public static MileStone END = new MileStone(1.00, "Sync complete");
}
