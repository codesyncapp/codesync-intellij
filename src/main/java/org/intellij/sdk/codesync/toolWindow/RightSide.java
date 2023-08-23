package org.intellij.sdk.codesync.toolWindow;

import javax.swing.*;
import java.util.Objects;

public class RightSide extends JPanel {

    public RightSide(){

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        JLabel imageLabel = new JLabel();
        JLabel introText = new JLabel();
        JLabel lineOne = new JLabel();
        JLabel lineTwo = new JLabel();
        JLabel lineThree = new JLabel();

        imageLabel.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/codesync.png"))));
        introText.setText("CodeSync streams your code in real-time to the cloud, allowing you to review the entire history of the codebase on the web.");
        lineOne.setText("• Review your coding progress every day. (What did I actually write today?!)");
        lineTwo.setText("• Align with teammates. (Never again be surprised by a Pull Request!)");
        lineThree.setText("• Or, rewind back to any point in the past. (Never again lose code you didn't commit!)");

        add(Box.createVerticalGlue());
        add(Box.createHorizontalBox());
        this.add(imageLabel);
        this.add(introText);
        this.add(lineOne);
        this.add(lineTwo);
        this.add(lineThree);
        add(Box.createHorizontalBox());
        add(Box.createVerticalGlue());

    }

}
