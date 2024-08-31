package space.themelon.openspace.server

import space.themelon.openspace.helper.io.BidirectionalSocket
import space.themelon.openspace.helper.io.BytesIO
import space.themelon.openspace.server.TrafficControl.ALLOWED_ADDRESSES
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class Socks5(
    private val client: Socket,
    private val routeServer: ServerSocket
) {

    companion object {
        private const val SOCKS_VERSION = 5

        private const val AUTH_NONE: Byte = 0x00
        private const val AUTH_USER_PASS: Byte = 0x02

        private const val CMD_STREAM = 0x01

        private const val ADDR_IPV4 = 0x01
        private const val ADDR_DOMAIN = 0x03
        private const val ADDR_IPV6 = 0x04

        private const val STATUS_GRANTED: Byte = 0x00
        private const val STATUS_FAILURE: Byte = 0x01
        private const val STATUS_NOT_ALLOWED: Byte = 0x02
        private const val STATUS_NETWORK_UNREACHABLE: Byte = 0x03
        private const val STATUS_HOST_UNREACHABLE: Byte = 0x04
        private const val STATUS_CONNECTION_REFUSED: Byte = 0x05
        private const val STATUS_PROTOCOL_ERROR: Byte = 0x07
        private const val STATUS_ADDRESS_UNSUPPORTED: Byte = 0X08
    }

    private val input = client.getInputStream()
    private val output = client.getOutputStream()

    init {
        acceptGreet()
    }

    private fun acceptGreet() {
        matchVersion()
        val authTypes = BytesIO.readBytes(input.read(), input)
        for (type in authTypes) {
            if (type == AUTH_NONE) {
                output.write(byteArrayOf(SOCKS_VERSION.toByte(), type))
                serveProxy()
                return
            }
        }
        // no mutual auth types
        output.write(byteArrayOf(SOCKS_VERSION.toByte(), 0xFF.toByte()))
    }

    private fun serveProxy() {
        matchVersion()
        input.read().let {
            if (it != CMD_STREAM) {
                throw Exception("SOCKS5 Non CMD_STREAM type requested")
            }
        }
        matchRSV()

        // read destination address
        val addressType = input.read()
        val addrSize: Int
        val address: ByteArray
        val inetAddr: InetAddress
        when (addressType) {
            ADDR_IPV4 -> {
                addrSize = 4
                address = BytesIO.readBytes(4, input)
                inetAddr = InetAddress.getByAddress(address)
            }
            ADDR_IPV6 -> {
                addrSize = 16
                address = BytesIO.readBytes(16, input)
                inetAddr = InetAddress.getByAddress(address)
            }
            ADDR_DOMAIN -> {
                address = BytesIO.readFixedString(input)
                addrSize = address.size
                inetAddr = InetAddress.getByName(String(address))
            }
            else -> {
                output.write(byteArrayOf(STATUS_ADDRESS_UNSUPPORTED, 0))
                throw RuntimeException("Unknown address type: $addressType")
            }
        }
        val portBytes = BytesIO.readBytes(2, input)
        val port = (portBytes[0].toInt() and 0xff) shl 8 or portBytes[1].toInt() and 0xff

        if (notRoutable(inetAddr.hostAddress, port)) {
            // this address is not listed
            output.write(byteArrayOf(STATUS_NOT_ALLOWED, 0))
            return
        }
        output.write(byteArrayOf(STATUS_GRANTED, 0))

        val hostSocket = routeServer.accept()
        // request origination
        hostSocket.getOutputStream().apply {
            BytesIO.writeFixedString(client.inetAddress.address, this)

            write(
                byteArrayOf(
                    // write destination port
                    portBytes[0], portBytes[1],

                    // write dest. addr_type, addr_len
                    addressType.toByte(), addrSize.toByte()
                )
            )
            // now write the address itself
            write(address)
            flush()
        }
        // now begin relay information b/w host and origin
        BidirectionalSocket.relay(client, hostSocket)
    }

    private fun notRoutable(address: String, port: Int): Boolean {
        val portRange = ALLOWED_ADDRESSES[address] ?: return false
        return port in portRange[0]..portRange[1]
    }

    private fun matchVersion() {
        input.read().let {
            if (it != SOCKS_VERSION) {
                throw Exception("SOCKS5 Version mismatch, got $it")
            }
        }
    }

    private fun matchRSV() {
        input.read().let {
            if (it != 0x00) {
                throw Exception("Expected SOCKS5 RSV Byte, got $it")
            }
        }
    }
}