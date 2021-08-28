package org.intellij.sdk.codesync.messages;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class CodeSyncMessages {
    public static final int OK = Messages.OK;
    public static final int YES = Messages.YES;
    public static final int NO = Messages.NO;
    public static final int CANCEL = Messages.CANCEL;

    public static boolean showYesNoMessage(String title, String message, Project project) {
        Integer result = invokeAndWait(
                () -> Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon()),
                ModalityState.defaultModalityState()
        );

        return result == YES;
    }

    /**
     * Runs the passed computation synchronously on the EDT and returns the result.
     *
     * ref: https://www.programcreek.com/java-api-examples/?code=saros-project%2Fsaros%2Fsaros-master%2Fintellij%2Fsrc%2Fsaros%2Fintellij%2Fui%2Futil%2FSafeDialogUtils.java#
     *
     * <p>If an exception occurs during the execution it is thrown back to the caller, including
     * <i>RuntimeException<i> and <i>Error</i>.
     *
     * @param <T> the type of the result of the computation
     * @param <E> the type of the exception that might be thrown by the computation
     * @param computation the computation to run
     * @param modalityState the modality state to use
     * @return returns the result of the computation
     * @throws E any exception that occurs while executing the computation
     * @see Application#invokeAndWait(Runnable,ModalityState)
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends Throwable> T invokeAndWait(
            @NotNull Computable<T> computation, @NotNull ModalityState modalityState)
            throws E {

        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        Application application = ApplicationManager.getApplication();

        application.invokeAndWait(
                () -> {
                    try {
                        result.set((T) computation.compute());

                    } catch (Throwable t) {
                        throwable.set(t);
                    }
                },
                modalityState);

        Throwable t = throwable.get();

        if (t == null) return result.get();

        if (t instanceof Error) throw (Error) t;

        if (t instanceof RuntimeException) throw (RuntimeException) t;

        throw (E) t;
    }
}
