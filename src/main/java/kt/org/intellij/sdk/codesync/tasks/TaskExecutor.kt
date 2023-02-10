package kt.org.intellij.sdk.codesync.tasks

object TaskExecutor {
    private var taskQueue: TaskQueue = TaskQueue();

    fun execute(runnable: Runnable) {
        println("execute called.")
        taskQueue.addTask(runnable)
    }

    fun start() {
        println("Starting ...");
        taskQueue.start()
    }
}
