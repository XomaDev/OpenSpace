package space.themelon.openspace.helper.io

import space.themelon.openspace.helper.io.Safety.ioThreadSafe
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

object BidirectionalSocket {

    fun relay(proxySocket: Socket, workerSocket: Socket) {
        // [Read] Proxy Socket -> [Write] Worker Socket
        ioThreadSafe {
            transfer(proxySocket.getInputStream(), workerSocket.getOutputStream())
        }

        // [Read] Worker Socket -> [Write] Proxy Socket
        ioThreadSafe {
            transfer(workerSocket.getInputStream(), proxySocket.getOutputStream())
        }
    }

    private fun transfer(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(4096)
        var bytesRead: Int

        while ((input.read(buffer).also { bytesRead = it }) != -1) {
            output.write(buffer, 0, bytesRead)
            output.flush()
        }
        input.close()
        output.close()
    }
}