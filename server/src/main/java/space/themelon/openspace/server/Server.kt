package space.themelon.openspace.server

import space.themelon.openspace.server.TrafficControl.ALLOWED_ADDRESSES
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object Server {
    fun start(port: Int) {
        // manages external client requests
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
            ADDRESS_IPV4 -> readArray(input, 4)
            ADDRESS_IPV6 -> readArray(input, 16)
            ADDRESS_DOMAIN -> readArray(input, input.read())
            else -> {
                sayBye()
                return
            }
        }
        if (address == null) {
            // addr read failed
            sayBye()
            return
        }
        val port = (input.read() and 0xff) shl 8 or input.read() and 0xff
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

        val inetAddress = if (addressType == ADDRESS_DOMAIN)
            InetAddress.getByName(String(address))
        else InetAddress.getByAddress(address)

        if (!checkIfAllowed(inetAddress.hostAddress, port)) {
            // address isn't allowed
            output.write(NOPE)
            sayBye()
            return
        }
        output.write(YES)

        // now we have to add a consumable hook into a pending queue

    }

    private fun checkIfAllowed(address: String, port: Int): Boolean {
        val portRange = ALLOWED_ADDRESSES[address] ?: return false
        return port in portRange[0]..portRange[1]
    }

    private fun readArray(input: InputStream, size: Int): ByteArray? {
        // we have to fully fill the byte array
        val array = ByteArray(size)
        var totalRead = 0
        while (true) {
            val read = input.read(array, totalRead, size)
            if (read == -1) return null
            totalRead += read
            if (totalRead == size) array
        }
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