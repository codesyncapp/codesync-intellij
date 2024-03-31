package kt.org.intellij.sdk.codesync.tasks

object TaskExecutor {
    private var taskQueue: TaskQueue = TaskQueue();

    fun execute(runnable: Runnable) {
        taskQueue.addTask(runnable)
    }

    fun start() {
        taskQueue.start()
    }
}
