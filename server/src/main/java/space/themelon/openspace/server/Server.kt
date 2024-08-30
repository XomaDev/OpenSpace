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

        if (acceptSocksGreet(input, output)) {
            serveSocksProxy(input, output)
        }
    }

    private fun serveSocksProxy(input: InputStream, output: OutputStream) {
        matchSocksVersion(input)
        input.read().let {
            if (it != CMD_STREAM) {
                throw Exception("Expected CMD_STREAM for SOCKS5 but got $it")
            }
        }
        matchSocksRSV(input)

        // read destination addr
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
                output.write(byteArrayOf(STATUS_ADDRESS_UNSUPPORTED.toByte(), 0))
                throw RuntimeException("Unknown address type: $addressType")
            }
        }
        val portBytes = BytesIO.readBytes(2, input)
        val port = (portBytes[0].toInt() and 0xff) shl 8 or portBytes[1].toInt() and 0xff

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
            output.write(byteArrayOf(STATUS_NOT_ALLOWED.toByte(), 0))
            return
        }
        output.write(byteArrayOf(STATUS_GRANTED.toByte(), 0))

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

    private fun acceptSocksGreet(input: InputStream, output: OutputStream): Boolean {
        matchSocksVersion(input)
        val authTypes = BytesIO.readBytes(input.read(), input)
        for (type in authTypes) {
            if (type == AUTH_NONE) {
                output.write(byteArrayOf(SOCKS_VERSION.toByte(), type))
                return true
            }
        }
        // no supported auth type b/w client and server
        output.write(byteArrayOf(SOCKS_VERSION.toByte(), 0xFF.toByte()))
        return false
    }

    private fun matchSocksVersion(input: InputStream) {
        input.read().let {
            if (it != SOCKS_VERSION) {
                throw Exception("Expected Socks 5 Version, got $it")
            }
        }
    }

    private fun matchSocksRSV(input: InputStream) {
        input.read().let {
            if (it != 0x00) {
                throw Exception("Expected SOCKS5 RSV Byte, got $it")
            }
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

    // SOCKS5 Protocol
    private const val SOCKS_VERSION = 5
    private const val ADDR_IPV4 = 0x01
    private const val ADDR_DOMAIN = 0x03
    private const val ADDR_IPV6 = 0x04

    // tcp relay format
    private const val CMD_STREAM = 0x01

    private const val STATUS_GRANTED = 0x00
    private const val STATUS_FAILURE = 0x01
    private const val STATUS_NOT_ALLOWED = 0x02
    private const val STATUS_NETWORK_UNREACHABLE = 0x03
    private const val STATUS_HOST_UNREACHABLE = 0x04
    private const val STATUS_CONNECTION_REFUSED = 0x05
    private const val STATUS_PROTOCOL_ERROR = 0x07
    private const val STATUS_ADDRESS_UNSUPPORTED = 0X08

    // proxy auth type
    private const val AUTH_NONE: Byte = 0x00
    private const val AUTH_USER_PASS: Byte = 0x02

    // Address format
    private const val ADDRESS_IPV4 = 0
    private const val ADDRESS_IPV6 = 1
    private const val ADDRESS_DOMAIN = 2
}