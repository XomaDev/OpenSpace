package space.themelon.openspace.helper.io

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object BytesIO {

    fun readInt(input: InputStream) : Int {
        val bytes = readBytes(4, input)
        return bytes[0].toInt() and 255 shl 24 or
                (bytes[1].toInt() and 255 shl 16) or
                (bytes[2].toInt() and 255 shl 8) or
                (bytes[3].toInt() and 255)
    }

    private fun writeInt(n: Int, output: OutputStream) {
        val bytes = byteArrayOf(
            (n shr 24).toByte(),
            (n shr 16).toByte(),
            (n shr 8).toByte(),
            n.toByte()
        )
        output.write(bytes)
    }

    fun writeFixedString(bytes: ByteArray, output: OutputStream) {
        writeInt(bytes.size, output)
        output.write(bytes)
    }

    fun readFixedString(input: InputStream) : ByteArray {
        val length = readInt(input)
        val bytes = readBytes(length, input)
        return bytes
    }

    fun readBytes(length: Int, input: InputStream): ByteArray {
        val bytes = ByteArray(length)
        var bytesRead = 0
        var offset = 0
        while (offset < length) {
            bytesRead = input.read(bytes, bytesRead, length - bytesRead)
            if (bytesRead == -1) {
                throw IOException("End of stream before completely reading data")
            }
            offset += bytesRead
        }
        return bytes
    }
}
