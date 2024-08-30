package space.themelon.openspace.server

import java.net.Socket

data class PendingHook(
    val callback: (Socket) -> Unit,
    var next: PendingHook? = null
)