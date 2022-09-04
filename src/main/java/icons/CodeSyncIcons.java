package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class CodeSyncIcons {
    private CodeSyncIcons(){
        // restrict instantiation
    }

    public static final Icon codeSyncIcon = IconLoader.getIcon("/icons/icon.png", CodeSyncIcons.class);

    public static Icon getCodeSyncIcon () {
        return codeSyncIcon;
    }
}
