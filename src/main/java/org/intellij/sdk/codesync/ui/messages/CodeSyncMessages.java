package org.intellij.sdk.codesync.ui.messages;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.intellij.sdk.codesync.utils.CommonUtils;

public class CodeSyncMessages {
    public static final int OK = Messages.OK;
    public static final int YES = Messages.YES;
    public static final int NO = Messages.NO;
    public static final int CANCEL = Messages.CANCEL;

    public static boolean showYesNoMessage(String title, String message, Project project) {
        Integer result = CommonUtils.invokeAndWait(
                () -> Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon()),
                ModalityState.defaultModalityState()
        );

        return result == YES;
    }

}
