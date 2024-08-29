package space.themelon.openspace.client

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val serverIp = args[0]
        val port = try {
            args[1].toInt()
        } catch (e: NumberFormatException) {
            println("Invalid port provided: ${args[1]}")
        }

    }
}