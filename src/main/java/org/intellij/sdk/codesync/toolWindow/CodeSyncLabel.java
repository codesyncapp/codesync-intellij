package org.intellij.sdk.codesync.toolWindow;

import javax.swing.*;
import java.awt.*;

public class CodeSyncLabel extends JLabel {

    public CodeSyncLabel(String labelText){
        super("<html><p>" + labelText + "</p></html>");
        this.setBackground(Color.GREEN);
        this.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.setPreferredSize(new Dimension(200, this.getPreferredSize().height+20));
    }

}
