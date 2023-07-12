package com.liteldev.headeroutput

import java.nio.file.Paths

fun String.relativePath(path: String) = Paths.get(this).relativize(Paths.get(path)).toString().replace("\\", "/")

fun String.parent() = "$this/.."

fun String.substring(startStr: String, endStr: String): String {
    return substringAfter(startStr, "").substringBefore(endStr, "")
}

fun StringBuilder.appendSpace(count: Int): StringBuilder {
    for (i in 0 until count) {
        this.append(" ")
    }
    return this
}

//fun removeTypeModifier(type: String): String {
//    return type.replace("const ", "").replace("volatile ", "").replace("restrict ", "")
//}
