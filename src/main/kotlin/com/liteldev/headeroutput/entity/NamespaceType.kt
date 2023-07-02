package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.config.origindata.TypeData

class NamespaceType(
    name: String, typeData: TypeData
) : BaseType(name, typeData) {
    override fun getPath(): String {
        return "./$name.hpp"
    }

    fun genPublic(): String {
        val sb = StringBuilder()
        typeData.publicTypes?.sortedBy { it.name }?.forEach {
            sb.appendLine(it.genFuncString(namespace = true, comment = this.getCommentOf(it)))
        }
        return sb.toString()
    }
}