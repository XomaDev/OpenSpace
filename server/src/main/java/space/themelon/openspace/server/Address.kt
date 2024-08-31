package space.themelon.openspace.server

data class Address(
    val address: String,
    val portRangeStart: Long,
    val portRangeEnd: Long
)