import java.nio.file.Paths

fun String.relativePath(path: String) = Paths.get(this).relativize(Paths.get(path)).toString().replace("\\", "/")

fun String.parent() = "$this/.."

fun String.substring(startStr: String = "", endStr: String = ""): String {
    val start = if (startStr.isEmpty()) {
        0
    } else {
        this.indexOf(startStr) + startStr.length
    }
    val str = this.substring(start)
    val end = if (endStr.isEmpty()) {
        str.length
    } else {
        str.indexOf(endStr)
    }
    if (end == -1) {
        return ""
    }
    return str.substring(0, end)
}
