package org.intellij.sdk.codesync.ui.progress;

import com.intellij.openapi.progress.ProgressIndicator;

public class CodeSyncProgressIndicator {
    ProgressIndicator progressIndicator;

    public CodeSyncProgressIndicator(ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    public void setMileStone(InitRepoMilestones.MileStone mileStone) {
        this.progressIndicator.setFraction(mileStone.mileage);
        this.progressIndicator.setText(mileStone.message);
    }
}
