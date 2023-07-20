package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.data.TypeData
import com.liteldev.headeroutput.relativePathTo

class NamespaceType(
    name: String, typeData: TypeData
) : BaseType(name, TypeKind.NAMESPACE, typeData) {

    fun genPublic(): String {
        val sb = StringBuilder()
        typeData.publicTypes?.sortedBy { it.name }?.forEach {
            sb.appendLine(it.genFuncString(namespace = true))
        }
        sb.appendLine()
        return sb.toString()
    }

    override fun generateTypeDefine(): String {
        val sb = StringBuilder()
        sb.append("namespace $name {\n")
        sb.append(genPublic())
        sb.appendLine("};")
        return sb.toString()
    }

    override fun initIncludeList() {
        collectAllReferencedType()
            // not include types can forward declare
            .filter { it.name.contains("::") }
            .map { this.path.relativePathTo(it.path) }
            .let(includeList::addAll)
        includeList.remove(this.path.relativePathTo(this.path))
        includeList.remove("")
    }
}
