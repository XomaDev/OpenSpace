package space.themelon.openspace.client

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket

object ProxyTester {
    @JvmStatic
    fun main(args: Array<String>) {
        val proxyAddress = InetSocketAddress("hackclub.app", 2832)
        val proxy = Proxy(Proxy.Type.SOCKS, proxyAddress)
        val destinationAddress = InetSocketAddress("192.168.0.103", 1024)

        val socket = Socket(proxy)
        socket.connect(destinationAddress)

        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = socket.getOutputStream()

        output.write("GET / HTTP/1.1\r\nHost: 192.168.0.103\r\n\r\n".toByteArray())
        output.flush()

        var line: String?
        while (input.readLine().also { line = it } != null) {
            println(line)
        }

        socket.close()
    }
}