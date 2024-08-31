package space.themelon.openspace.server

import com.moandjiezana.toml.Toml
import java.io.File
import java.net.InetAddress

object TrafficControl {

    val ALLOWED_ADDRESSES = mutableMapOf<String, LongArray>()

    fun loadYAML(file: File) {
        val toml = Toml().read(file)
        val addresses = toml.getList<List<Any>>("addresses")
        for (address in addresses) {
            val addr = (address[0] as String).trim().lowercase()
            val fromPort = (address[1] as String).toLong()
            val toPort = (address[2] as String).toLong()

            ALLOWED_ADDRESSES[InetAddress.getByName(addr).hostAddress] = longArrayOf(
                fromPort, toPort,
            )
        }
    }
}