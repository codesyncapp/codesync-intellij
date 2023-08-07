package org.intellij.sdk.codesync.toolWindow;

import javax.swing.*;
import java.awt.*;

public class CodeSyncButton extends JButton {

    public CodeSyncButton(String buttonText){
        super(buttonText);
        Dimension buttonSize = new Dimension(200, 50);
        this.setMaximumSize(buttonSize);
    }

}