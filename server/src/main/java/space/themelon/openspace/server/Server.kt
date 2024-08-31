package space.themelon.openspace.server

import java.net.ServerSocket
import kotlin.concurrent.thread

class Server(hostPort: Int, private val servePort: Int) {

    private val hostServer = ServerSocket(hostPort)

    init {
        // manages proxy requests
        thread {
            val server = ServerSocket(servePort)
            while (true) {
                val client = server.accept()
                thread {
                    Socks5(client, hostServer)
                }
            }
        }
    }
}