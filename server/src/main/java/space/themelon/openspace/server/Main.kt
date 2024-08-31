package space.themelon.openspace.server

import java.io.File

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty() || args.size != 3) {
            println("Invalid args, use `<host_port> <proxy_port> <file_path>`")
            return
        }
        val hostPort = try {
            args[0].toInt()
        } catch (e: NumberFormatException) {
            println("Invalid host port provided: ${args[0]}")
            return
        }
        val proxyPort = try {
            args[0].toInt()
        } catch (e: NumberFormatException) {
            println("Invalid host port provided: ${args[1]}")
            return
        }
        val filePath = if (args[2].startsWith('/')) args[2] else "${System.getProperty("user.dir")}/${args[2]}  "
        val yamlFilePath = File(filePath)
        if (!yamlFilePath.isFile || !yamlFilePath.exists()) {
            println("File does not exist: $yamlFilePath")
            return
        }
        TrafficControl.loadYAML(yamlFilePath)
        Server(hostPort, proxyPort)
    }
}