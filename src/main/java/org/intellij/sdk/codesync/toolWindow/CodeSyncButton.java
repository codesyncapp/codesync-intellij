package org.intellij.sdk.codesync.toolWindow;

import javax.swing.*;
import java.awt.*;

public class CodeSyncButton extends JButton {

    public CodeSyncButton(String buttonText){
        super(buttonText);
        this.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension dimension = new Dimension(200, 50);
        this.setMaximumSize(dimension);
    }

}
