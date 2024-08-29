package space.themelon.openspace.server

import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object Server {
    fun start(port: Int) {
        thread {
            val server = ServerSocket(port)
            while (true) {
                val client = server.accept()
                thread {
                    serveClient(client)
                }
            }
        }
    }

    private fun serveClient(client: Socket) {
        val input = client.getInputStream()
        val output = client.getOutputStream()

        fun sayBye() {
            output.write(NOPE)
            client.close()
        }

        // ensure version compatibility
        val version = input.read()
        if (version != SERVER_VERSION) {
            sayBye()
            return
        }
        output.write(YES)

        // read connection destination
        val addressType = input.read()
        val address = when (addressType) {
            ADDRESS_IPV4 -> ByteArray(4)
            ADDRESS_IPV6 -> ByteArray(16)
            ADDRESS_DOMAIN -> ByteArray(input.read())
            else -> {
                sayBye()
                return
            }
        }
        if (readBytesFully(input, address)) {
            // failed condition
            sayBye()
            return
        }
        val portBytes = ByteArray(4)
        if (readBytesFully(input, portBytes)) {
            sayBye()
            return
        }
        output.write(YES)

        // > Our server acts as a protection layer. If we blindly proxy the requests forward
        //   to the Host network, it could be vulnerable to Spam attacks.

        // (here host refers to local network)
        // > We could make a system where we ask the host device, if it allows
        //   a request to certain address or a domain to go through.
        //   If the device answers Yes, then we cache the answer for newer requests

        // > Alternatively
        //   Instead of 'asking' the host device, we have a file that tells
        //   if certain requests are allowed, certain ips are denied, etc.
        //   This could be updated by the host local network if it wants and the changes are reflected.
    }

    private fun readBytesFully(input: InputStream, address: ByteArray): Boolean {
        // we have to fully fill the byte array
        val arraySize = address.size
        var totalRead = 0
        while (true) {
            val read = input.read(address, totalRead, arraySize)
            if (read == -1) return true
            totalRead += read
            if (totalRead == arraySize) break
        }
        return false
    }

    const val SERVER_VERSION = 0

    // Answers
    const val NOPE = 0
    const val YES = 1

    // Address format
    const val ADDRESS_IPV4 = 0
    const val ADDRESS_IPV6 = 1
    const val ADDRESS_DOMAIN = 2
}