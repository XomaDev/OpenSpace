package space.themelon.openspace.server

import java.net.Socket

object HookManager {
    // Creates pending hooks in a linear linked referenced format
    // As pending hooks are created, tail is appended
    // As pending hooks are fulfilled, heads are shortened

    private val lock = Any()

    private var head: PendingHook? = null
    private var tail: PendingHook? = null

    fun addHook(hook: (Socket) -> Unit) {
        synchronized(lock) {
            PendingHook(hook).let {
                if (head == null) head = it

                if (tail == null) tail = it
                else tail!!.next = it
                tail = it
            }
        }
    }

    fun notifyHook(client: Socket) {
        val callHook: PendingHook
        synchronized(lock) {
            if (head == null) {
                println("No pending hooks")
                return
            }
            callHook = head!!
            head = head!!.next
        }
        // invoke outside sync
        callHook.callback(client)
    }
}