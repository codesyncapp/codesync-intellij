package kt.org.intellij.sdk.codesync.tasks

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class TaskQueue {
    private val queue: BlockingQueue<Runnable> = LinkedBlockingQueue()

    private inner class TaskExecutor : Thread() {
        override fun run() {
            try {
                while (true) {
                    val task = queue.take()
                    task.run()
                }
            } catch (e: InterruptedException) {
                // No action needed.
            }
        }
    }

    fun start() {
        TaskExecutor().start()
    }

    fun addTask(task: Runnable) {
        queue.add(task)
    }
}
