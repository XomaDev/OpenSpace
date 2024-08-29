package space.themelon.openspace.server

import java.io.File

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty() || args.size != 2) {
            println("Invalid args, use `<port> <file_path>`")
            return
        }
        val port = try {
            args[0].toInt()
        } catch (e: NumberFormatException) {
            println("Invalid port provided: ${args[0]}")
            return
        }
        val yamlFilePath = File(args[1])
        if (!yamlFilePath.isFile || !yamlFilePath.exists()) {
            println("File does not exist: $yamlFilePath")
            return
        }
        TrafficControl.loadYAML(yamlFilePath)
        Server.start(port)
    }
}