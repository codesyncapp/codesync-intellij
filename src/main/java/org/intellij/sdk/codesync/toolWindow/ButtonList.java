package org.intellij.sdk.codesync.toolWindow;

import javax.swing.*;
import java.awt.*;

public class ButtonList extends JPanel {

    public ButtonList(){
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.add(new CodeSyncButton("View File Playback"));
        this.add(new CodeSyncButton("View Dashboard"));
        this.add(new CodeSyncButton("Connect Repo"));
        this.add(new CodeSyncButton("Logout"));

        this.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

}
