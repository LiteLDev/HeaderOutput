package data

import OLD_PATH
import substring
import java.io.File

class StructType(
    name: String, typeData: TypeData,
) : BaseType(name, typeData) {
    override fun getPath(): String {
        return "./$name.hpp"
    }

    override fun readOldAddition() {
        val origin = File(OLD_PATH, getPath()).readText().replace("\r\n", "\n")
        beforeAddition = origin.substring(
            "#define BEFORE_EXTRA\n", "\n#undef BEFORE_EXTRA"
        )
        afterAddition = origin.substring(
            "#define AFTER_EXTRA\n", "\n#undef AFTER_EXTRA"
        )
    }

    fun genAntiReconstruction(): String {
        val public = arrayListOf<MemberTypeData>()
        typeData.publicTypes?.filter { it.isNew() || (it.isOperator() && it.method == "operator=") }
            ?.let(public::addAll)
        val genOperator = public.find {
            it.isOperator() && it.params.run {
                size == 1 && this[0] == "struct $name const &"
            } && it.returnType == "struct $name &"
        } == null
        val genEmptyParamConstructor = public.find { it.method == name && it.params.isEmpty() } == null
        val genMoveConstructor = public.find {
            it.method == name && it.params.run {
                size == 1 && this[0] == "struct $name const &"
            }
        } == null
        val sb = StringBuilder()
        if (genOperator || genEmptyParamConstructor || genMoveConstructor) {
            sb.appendLine()
            sb.appendLine("#ifndef DISABLE_CONSTRUCTOR_PREVENTION_${name.uppercase()}")
            sb.appendLine("public:")
            if (genOperator) {
                sb.appendLine("    struct $name& operator=(struct $name const &) = delete;")
            }
            if (genMoveConstructor) {
                sb.appendLine("    $name(struct $name const &) = delete;")
            }
            if (genEmptyParamConstructor) {
                sb.appendLine("    $name() = delete;")
            }
            sb.appendLine("#endif")
        }
        return sb.toString()
    }

    fun genPublic(): String {
        val sb = StringBuilder()
        sb.appendLine("public:")
        typeData.publicTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        return sb.toString()
    }

    fun genProtected(): String {
        val sb = StringBuilder()
        sb.appendLine("protected:")
        typeData.protectedTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        typeData.protectedStaticTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        return sb.toString()
    }

    fun genPrivate(): String {
        val sb = StringBuilder()
        sb.appendLine("private:")
        typeData.privateTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        typeData.privateStaticTypes?.sortedBy { it.method }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        return sb.toString()
    }
}