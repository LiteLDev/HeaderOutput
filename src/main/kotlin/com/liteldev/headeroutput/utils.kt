package com.liteldev.headeroutput

import com.liteldev.headeroutput.config.origindata.TypeData
import com.liteldev.headeroutput.entity.BaseType
import com.liteldev.headeroutput.entity.ClassType
import com.liteldev.headeroutput.entity.NamespaceType
import com.liteldev.headeroutput.entity.StructType
import java.nio.file.Paths

fun String.relativePath(path: String): String {
    // fixme: 修复路径前多余的 `../`
    return Paths.get(this).relativize(Paths.get(path)).toString().replace("\\", "/").removePrefix("../")
}

fun StringBuilder.appendSpace(count: Int): StringBuilder = append(" ".repeat(count))

fun BaseType.isClass(): Boolean = this is ClassType && !this.isStruct()

fun BaseType.isStruct() = this is StructType
fun BaseType.isNamespace() = this is NamespaceType

fun BaseType.getTopLevelFileType(): BaseType {
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
    val isClassOrStruct = isStruct(typeName) || isClass(typeName)
    return (typeData.publicTypes?.find { it.isPtrCall() } == null && !isClassOrStruct)
}

fun isStruct(typeName: String): Boolean {
    return TypeManager.structNameList.contains(typeName)
}

fun isClass(typeName: String): Boolean {
    return TypeManager.classNameList.contains(typeName)
}

fun removeTypeSpecifier(type: String): String {
    var result = type.replace("*", "").replace("&", "").trim()
    while (result.contains(" ")) {
        result = when {
            result.startsWith("const ") -> result.substring(6).trim()
            result.endsWith(" const") -> result.substring(0, result.length - 6).trim()

            result.startsWith("class ") -> result.substring(6).trim()
            result.startsWith("struct ") -> result.substring(7).trim()

            else -> break
        }
    }
    return result
}
