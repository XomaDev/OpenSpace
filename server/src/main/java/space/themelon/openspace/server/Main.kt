package space.themelon.openspace.server

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val serverPort = 2248
        Server.start(serverPort)
    }
}