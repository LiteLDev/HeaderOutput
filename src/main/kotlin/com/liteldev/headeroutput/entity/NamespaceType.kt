package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.HeaderOutput
import com.liteldev.headeroutput.config.TypeData
import com.liteldev.headeroutput.substring
import java.io.File

class NamespaceType(
    name: String, typeData: TypeData
) : BaseType(name, typeData) {
    override fun getPath(): String {
        return "./$name.hpp"
    }

    override fun readOldAddition() {
        val origin = File(HeaderOutput.OLD_PATH, getPath()).readText().replace("\r\n", "\n")
        beforeAddition = origin.substring(
            "#define BEFORE_EXTRA\n",
            "\n#undef BEFORE_EXTRA"
        )
        afterAddition = origin.substring(
            "#define AFTER_EXTRA\n",
            "\n#undef AFTER_EXTRA"
        )
        readComments()
    }

    override fun readComments() {
        readComments("namespace")
    }

    fun genPublic(): String {
        val sb = StringBuilder()
        typeData.publicTypes?.sortedBy { it.name }?.forEach {
            sb.appendLine(it.genFuncString(namespace = true, comment = this.getCommentOf(it)))
        }
        return sb.toString()
    }
}