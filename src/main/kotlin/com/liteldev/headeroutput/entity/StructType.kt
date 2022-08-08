package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.HeaderOutput
import com.liteldev.headeroutput.config.MemberTypeData
import com.liteldev.headeroutput.config.TypeData
import com.liteldev.headeroutput.substring
import java.io.File

class StructType(
    name: String, typeData: TypeData,
) : BaseType(name, typeData) {
    override fun getPath(): String {
        return "./$name.hpp"
    }

    override fun readOldAddition() {
        val origin = File(HeaderOutput.OLD_PATH, getPath()).readText().replace("\r\n", "\n")
        beforeAddition = origin.substring(
            "#define BEFORE_EXTRA\n", "\n#undef BEFORE_EXTRA"
        )
        afterAddition = origin.substring(
            "#define AFTER_EXTRA\n", "\n#undef AFTER_EXTRA"
        )
    }

    fun genAntiReconstruction(): String {
        val public = arrayListOf<MemberTypeData>()
        typeData.publicTypes?.filter { it.isConstructor() || (it.isOperator() && it.name == "operator=") }
            ?.let(public::addAll)
        val genOperator = public.find {
            it.isOperator() && it.params?.run {
                size == 1 && this[0].Name  == "struct $name const &"
            } == true && it.valType?.Name  == "struct $name &"
        } == null
        val genEmptyParamConstructor = public.find { it.name == name && it.params?.isEmpty() == true } == null
        val genMoveConstructor = public.find {
            it.name == name && it.params?.run {
                size == 1 && this[0].Name  == "struct $name const &"
            } == true
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
        typeData.publicTypes?.sortedBy { it.name }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        return sb.toString()
    }

    fun genProtected(): String {
        val sb = StringBuilder()
        sb.appendLine("protected:")
        typeData.protectedTypes?.sortedBy { it.name }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        typeData.protectedStaticTypes?.sortedBy { it.name }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        return sb.toString()
    }

    fun genPrivate(): String {
        val sb = StringBuilder()
        sb.appendLine("private:")
        typeData.privateTypes?.sortedBy { it.name }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        typeData.privateStaticTypes?.sortedBy { it.name }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        return sb.toString()
    }
}