package space.themelon.openspace.server

import space.themelon.openspace.server.TrafficControl.ALLOWED_ADDRESSES
import space.themelon.openspace.server.io.BidirectionalSocket
import space.themelon.openspace.server.io.BytesIO
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object Server {
    fun start(hostPort: Int, servePort: Int) {
        // manages proxy requests
        thread {
            val server = ServerSocket(servePort)
            while (true) {
                val client = server.accept()
                thread {
                    Socks5(client)
                }
            }
        }

        // manages host device connections
        thread {
            val server = ServerSocket(hostPort)
            while (true) {
                val client = server.accept()
                thread {
                    useHost(client)
                }
            }
        }
    }

    private fun useHost(client: Socket) {
        // TODO:
        //  In future, we have to authenticate this connection before
        //  we pair and relay data back and forth
        HookManager.notifyHook(client)
    }
}