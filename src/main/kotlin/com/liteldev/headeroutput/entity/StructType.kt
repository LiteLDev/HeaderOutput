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
            } == true && it.valType.Name == "struct $name &"
        } == null
        val genEmptyParamConstructor = public.find { it.name == name && it.params?.isEmpty() ?: true } == null
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
        var counter = 0
        typeData.virtual?.forEach {
            if (it.namespace.isEmpty() || it.namespace == name) sb.append("    /*${counter}*/ ")
                .appendLine(it.genFuncString())
            counter++
        }

        sb.appendLine("#ifdef ENABLE_VIRTUAL_FAKESYMBOL_${name.uppercase()}")
        typeData.virtualUnordered?.sortedBy { it.name }?.forEach {
            sb.append("    ").appendLine(it.genFuncString(use_fake_symbol = true))
        }
        sb.appendLine("#endif")

        typeData.publicTypes?.sortedBy { it.name }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        typeData.publicStaticTypes?.sortedBy { it.name }?.forEach {
            sb.append("    ").appendLine(it.genFuncString())
        }
        if (sb.equals("public:"))
            return ""
        sb.trim()
        sb.appendLine()
        return sb.toString()
    }

    fun genProtected(genFunc: Boolean = true): String {
        if ((typeData.protectedTypes == null || typeData.protectedTypes?.isEmpty() == true)
            && (typeData.protectedStaticTypes == null || typeData.protectedStaticTypes?.isEmpty() == true)
        ) {
            return ""
        }
        val sb = StringBuilder()
        if (genFunc)
            sb.appendLine("//protected:")
        else
            sb.appendLine("protected:")
        typeData.protectedTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable()))
                sb.append("    ").appendLine(it.genFuncString())
        }
        typeData.protectedStaticTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable()))
                sb.append("    ").appendLine(it.genFuncString())
        }
        if (sb.equals("protected:") || sb.equals("//protected:"))
            return ""
        sb.trim()
        sb.appendLine()
        return sb.toString()
    }

    fun genPrivate(genFunc: Boolean = true): String {
        if ((typeData.privateTypes == null || typeData.privateTypes?.isEmpty() == true)
            && (typeData.privateStaticTypes == null || typeData.privateStaticTypes?.isEmpty() == true)
        ) {
            return ""
        }
        val sb = StringBuilder()
        if (genFunc)
            sb.appendLine("//private:")
        else
            sb.appendLine("private:")
        typeData.privateTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable()))
                sb.append("    ").appendLine(it.genFuncString())
        }
        typeData.privateStaticTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable()))
                sb.append("    ").appendLine(it.genFuncString())
        }
        if (sb.equals("private:") || sb.equals("//private:"))
            return ""
        sb.trim()
        sb.appendLine()
        return sb.toString()
    }
}