package com.liteldev.headeroutput

import com.liteldev.headeroutput.entity.BaseType
import java.nio.file.Paths

fun String.relativePathTo(path: String): String {
    return Paths.get(this.substringBeforeLast("/")).relativize(Paths.get(path)).toString().replace("\\", "/")
}

fun StringBuilder.appendSpace(count: Int): StringBuilder = append(" ".repeat(count))

fun BaseType.isClass(): Boolean = this.type == BaseType.TypeKind.CLASS

fun BaseType.isStruct() = this.type == BaseType.TypeKind.STRUCT

fun BaseType.isNamespace() = this.type == BaseType.TypeKind.NAMESPACE

fun BaseType.isEnum() = this.type == BaseType.TypeKind.ENUM

fun BaseType.getTopLevelFileType(): BaseType {
    assert(TypeManager.nestingMap.isNotEmpty()) { "TypeManager.nestingMap is empty" }

    outerType ?: return this
    if (isNamespace()) {
        return this
    }
    var outer = outerType!!
    if (outer.isNamespace()) {
        return this
    }
    while (outer.outerType != null) {
        if (outer.outerType!!.isNamespace()) {
            return outer
        }
        outer = outer.outerType!!
    }
    return outer
}

fun String.toSnakeCase(): String {
    val sb = StringBuilder()
    var lastCharIsSeparator = false
    forEachIndexed { index, c ->
        if (c.isUpperCase()) {
            if (index != 0 && !lastCharIsSeparator) {
                sb.append("_")
            }
            sb.append(c.lowercaseChar())
        } else {
            sb.append(c)
        }
        lastCharIsSeparator = c == '/'
    }
    return sb.toString()
}
