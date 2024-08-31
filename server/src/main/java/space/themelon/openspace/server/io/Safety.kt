package space.themelon.openspace.server.io

import java.io.IOException
import java.net.Socket
import kotlin.concurrent.thread

object Safety {

    fun ioSafe(tag: String, block: () -> Unit) {
        try {
            block()
        } catch (e: IOException) {
            println("$tag IOException ${e.message}")
        } catch (e: InterruptedException) {
            println("$tag InterruptedException ${e.message}")
        }
    }

    fun safeContainer(tag: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            println("[Contained $tag] ${t.javaClass.name} ${t.message}")
        }
    }

    fun ioThreadSafe(block: () -> Unit) {
        thread {
            try {
                block()
            }
            catch (ignored: IOException) { }
            catch (ignored: InterruptedException) { }
        }
    }

    fun safeClose(socket: Socket) {
        try {
            socket.close()
        } catch (ignored: Exception) {

        }
    }
}