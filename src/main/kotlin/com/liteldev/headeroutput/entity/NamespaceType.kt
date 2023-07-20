package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.config.origindata.TypeData
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
            .map { this.getPath().relativePathTo(it.getPath()) }
            .let(includeList::addAll)
        includeList.remove(this.getPath().relativePathTo(this.getPath()))
        includeList.remove("")
    }
}
