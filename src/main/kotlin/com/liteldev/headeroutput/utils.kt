package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.TypeData
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

fun StringBuilder.appendSpace(count: Int): StringBuilder {
    for (i in 0 until count) {
        this.append(" ")
    }
    return this
}

fun isNameSpace(typeName: String, typeData: TypeData): Boolean {
    if (typeData.privateTypes != null
        || typeData.privateStaticTypes != null
        || typeData.protectedTypes != null
        || typeData.protectedStaticTypes != null
        || typeData.publicStaticTypes != null
        || typeData.virtual != null
        || typeData.vtblEntry != null
    ) {
        return false
    }
    val containsInIdentifierList =
        HeaderOutput.realStructNameList.contains(typeName) || HeaderOutput.realClassNameList.contains(typeName)
    return (typeData.publicTypes?.find { it.isPtrCall() } == null
            && !containsInIdentifierList)
}