package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.HeaderGenerator.HEADER_SUFFIX
import com.liteldev.headeroutput.config.origindata.TypeData

class NamespaceType(
    name: String, typeData: TypeData
) : BaseType(name, typeData) {

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

    override fun getPath(): String {
        return name.replace("::", "/") + ".$HEADER_SUFFIX"
    }
}
