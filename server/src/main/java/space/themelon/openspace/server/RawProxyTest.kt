package space.themelon.openspace.server

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket


object RawProxyTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val socket = Socket("hackclub.app", 2832)
        val output: OutputStream = socket.getOutputStream()
        val input: InputStream = socket.getInputStream()

        output.write(0x05) // VERSION
        output.write(0x01) // NAUTH
        output.write(0x00) // NO AUTH

        if (input.read() != 0x05 || input.read() != 0x00) {
            throw IOException("Unable to greet")
        }

        output.write(0x05) // VERSION
        output.write(0x01) // COMMAND BIND
        output.write(0x00) // RSV
        output.write(0x01) // ADDRESS IPV4
        output.write(byteArrayOf(192.toByte(), 168.toByte(), 0.toByte(), 103.toByte())) // IP ADDRESS
        output.write(byteArrayOf((1024 shr 8).toByte(), (1024 and 0xFF).toByte()))

        if (input.read() != 0x05 || input.read() != 0x00 || input.read() != 0x00 || input.read() != 0x01
        ) {
            throw IOException("Unable to connect")
        }
        val hostname = ByteArray(4)
        input.read(hostname)

        val address = InetAddress.getByAddress(hostname)
        val port = input.read() shl 8 or input.read()

        println("Address: $address")
        println("Bound on: $port")

        Thread {
            try {
                val echoOutput = Socket(address, port).getOutputStream()
                echoOutput.write("hello world".toByteArray())
                echoOutput.close()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }.start()

        while (true) {
            val n = input.read()
            if (n == -1) {
                break
            }
            print(n.toChar())
        }
        socket.close()
    }
}