package org.intellij.sdk.codesync.toolWindow;

import javax.swing.*;
import java.awt.*;

public class CodeSyncMenu extends JPanel {

    CodeSyncMenu(){
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new GridBagLayout());
        controlsPanel.add(new ButtonList());

        this.add(controlsPanel);
    }

}
