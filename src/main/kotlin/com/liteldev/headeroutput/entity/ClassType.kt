package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.config.origindata.MemberTypeData
import com.liteldev.headeroutput.config.origindata.TypeData

open class ClassType(
    name: String, typeData: TypeData,
    var parent: ClassType? = null,
) : BaseType(name, typeData) {

    // TODO: Fix in header generator
    init {
        typeData.virtual?.forEach { virtual ->
            typeData.virtualUnordered?.removeIf { unordered ->
                virtual.symbol == unordered.symbol
            }
        }
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ClassType
        if (name != other.name) return false
        return typeData == other.typeData
    }

    open fun genAntiReconstruction(): String {
        val public = arrayListOf<MemberTypeData>()
        typeData.virtual?.let(public::addAll)
        typeData.publicTypes?.let(public::addAll)
        typeData.protectedTypes?.let(public::addAll)
        typeData.privateTypes?.let(public::addAll)
        public.filter { it.isConstructor() || (it.isOperator() && it.name == "operator=") }
            .let(public::addAll)
        val genOperator = public.find {
            it.isOperator() && it.params?.run {
                size == 1 && this[0].Name == "class $name const &"
            } == true && it.valType.Name == "class $name &"
        } == null
        val genEmptyParamConstructor = public.find { it.name == name && it.params?.isEmpty() ?: true } == null
        val genMoveConstructor = public.find {
            it.name == name && it.params?.run {
                size == 1 && this[0].Name == "class $name const &"
            } == true
        } == null
        val sb = StringBuilder()
        if (genOperator || genEmptyParamConstructor || genMoveConstructor) {
            sb.appendLine()
            sb.appendLine("#ifndef DISABLE_CONSTRUCTOR_PREVENTION_${name.uppercase()}")
            sb.appendLine("public:")
            if (genOperator) {
                sb.appendLine("    class $name& operator=(class $name const &) = delete;")
            }
            if (genMoveConstructor) {
                sb.appendLine("    $name(class $name const &) = delete;")
            }
            if (genEmptyParamConstructor) {
                sb.appendLine("    $name() = delete;")
            }
            sb.appendLine("#endif")
        }
        sb.appendLine()
        return sb.toString()
    }

    fun genPublic(): String {
        val sb = StringBuilder()
        sb.appendLine("public:")
        var counter = 0
        typeData.virtual?.forEach {
            if (it.namespace.isEmpty() || it.namespace == name)
                sb.appendLine(it.genFuncString(vIndex = counter))
            counter++
        }

        if (typeData.virtualUnordered?.isNotEmpty() == true) {
            sb.appendLine("#ifdef ENABLE_VIRTUAL_FAKESYMBOL_${name.uppercase()}")
            typeData.virtualUnordered?.sortedBy { it.name }?.forEach {
                sb.appendLine(
                    it.genFuncString(
                        useFakeSymbol = true
                    )
                )
            }
            sb.appendLine("#endif")
        }

        typeData.publicTypes?.sortedBy { it.name }?.forEach {
            sb.appendLine(it.genFuncString())
        }
        typeData.publicStaticTypes?.sortedBy { it.name }?.forEach {
            sb.appendLine(it.genFuncString())
        }
        if (sb.equals("public:\n"))
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
                sb.appendLine(it.genFuncString())
        }
        typeData.protectedStaticTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable()))
                sb.appendLine(it.genFuncString())
        }
        if (sb.equals("protected:\n") || sb.equals("//protected:\n"))
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
                sb.appendLine(it.genFuncString())
        }
        typeData.privateStaticTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable()))
                sb.appendLine(it.genFuncString())
        }
        if (sb.equals("private:\n") || sb.equals("//private:\n"))
            return ""
        sb.trim()
        sb.appendLine()
        return sb.toString()
    }
}
