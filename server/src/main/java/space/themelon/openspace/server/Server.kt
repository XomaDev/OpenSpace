package space.themelon.openspace.server

import space.themelon.openspace.server.TrafficControl.ALLOWED_ADDRESSES
import space.themelon.openspace.server.io.BidirectionalSocket
import space.themelon.openspace.server.io.BytesIO
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
                    serveClient(client)
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
        val addrSize: Int
        val address: ByteArray
        when (addressType) {
            ADDRESS_IPV4 -> {
                addrSize = 4
                address = BytesIO.readBytes(4, input)
            }
            ADDRESS_IPV6 -> {
                addrSize = 16
                address = BytesIO.readBytes(16, input)
            }
            ADDRESS_DOMAIN -> {
                address = BytesIO.readFixedString(input)
                addrSize = address.size
            }
            else -> {
                sayBye()
                return
            }
        }
        val portBytes = BytesIO.readBytes(2, input)
        val port = (portBytes[0].toInt() and 0xff) shl 8 or portBytes[1].toInt() and 0xff
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
        HookManager.addHook { hostClient ->
            hostClient.getOutputStream().apply {
                // forward the address of requester
                BytesIO.writeFixedString(client.inetAddress.address, this)

                // send in destination address
                write(
                    byteArrayOf(
                        // port
                        portBytes[0],
                        portBytes[1],

                        // addr_type, addr_len
                        addressType.toByte(),
                        addrSize.toByte(),
                    )
                )
                // address
                write(address)
                flush()
            }
            // relays information b/w host and proxy req client
            BidirectionalSocket.relay(client, hostClient)
        }
    }

    private fun checkIfAllowed(address: String, port: Int): Boolean {
        val portRange = ALLOWED_ADDRESSES[address] ?: return false
        return port in portRange[0]..portRange[1]
    }

    private fun useHost(client: Socket) {
        // TODO:
        //  In future, we have to authenticate this connection before
        //  we pair and relay data back and forth
        HookManager.notifyHook(client)
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