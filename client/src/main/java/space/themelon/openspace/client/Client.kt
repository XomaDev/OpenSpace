package space.themelon.openspace.client

import space.themelon.openspace.client.io.BidirectionalSocket
import space.themelon.openspace.client.io.BytesIO
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

object Client {
    fun start(serverIp: String, serverPort: Int) {
        thread {
            while (true) {
                val socket = Socket()
                socket.connect(InetSocketAddress(serverIp, serverPort), 0)
                thread {
                    routeRequest(socket)
                }
            }
        }
    }

    private fun routeRequest(server: Socket) {
        // TODO:
        //  In future, we have to create an auth system with server
        //  before routing each requests

        // the server has approved this request
        val input = server.getInputStream()
        //val output = client.getOutputStream()

        val originAddr = InetAddress.getByAddress(BytesIO.readFixedString(input))

        println("Origin address: $originAddr")

        // read route destination
        val portBytes = BytesIO.readBytes(2, input)
        val port = (portBytes[0].toInt() and 0xff) shl 8 or portBytes[1].toInt() and 0xff

        val addressType = input.read()
        val addressSize = input.read()
        val inetAddr = when (addressType) {
            ADDR_IPV4 -> InetAddress.getByAddress(BytesIO.readBytes(4, input))
            ADDR_IPV6 -> InetAddress.getByAddress(BytesIO.readBytes(16, input))
            ADDR_DOMAIN -> InetAddress.getByName(String(BytesIO.readBytes(addressSize, input)))
            else -> throw RuntimeException("Unknown address type: $addressType")
        }

        println("Route address $inetAddr, port $port")

        val socket = Socket(inetAddr, 1024)
        BidirectionalSocket.relay(server, socket)
    }

    private const val ADDR_IPV4 = 0x01
    private const val ADDR_DOMAIN = 0x03
    private const val ADDR_IPV6 = 0x04
}