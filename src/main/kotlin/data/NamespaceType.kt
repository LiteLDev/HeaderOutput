package data

import OLD_PATH
import substring
import java.io.File

class NamespaceType(
    name: String, typeData: TypeData
) : BaseType(name, typeData) {
    override fun getPath(): String {
        return "./$name.hpp"
    }

    override fun readOldAddition() {
        val origin = File(OLD_PATH, getPath()).readText().replace("\r\n", "\n")
        beforeAddition = origin.substring(
            "#define BEFORE_EXTRA\n",
            "\n#undef BEFORE_EXTRA"
        )
        afterAddition = origin.substring(
            "#define AFTER_EXTRA\n",
            "\n#undef AFTER_EXTRA"
        )
    }

    fun genPublic(): String {
        val sb = StringBuilder()
        typeData.publicTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString(namespace = true))
        }
        return sb.toString()
    }
}