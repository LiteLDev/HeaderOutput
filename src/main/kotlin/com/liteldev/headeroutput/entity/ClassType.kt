package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.config.origindata.TypeData
import com.liteldev.headeroutput.getTopLevelFileType
import com.liteldev.headeroutput.relativePathTo

open class ClassType(
    name: String, typeData: TypeData, private val isTemplateClass: Boolean = false,
) : BaseType(name, TypeKind.CLASS, typeData) {

    val parents = arrayListOf<BaseType>()

    // fixme: Fix in header generator
    init {
        typeData.virtual?.forEach { virtual ->
            typeData.virtualUnordered?.removeIf { unordered ->
                virtual.symbol == unordered.symbol
            }
        }
    }

    override fun generateTypeDefine(): String {
        val sb = StringBuilder(
            "class $simpleName ${genParents()}{\n"
        )
        if (innerTypes.isNotEmpty()) {
            sb.appendLine("public:")
            sb.append(generateInnerTypeDefine().replace("\n", "\n    "))
        }
        sb.append(genAntiReconstruction())
        sb.append(genPublic())
        sb.append(genProtected())
        sb.append(genPrivate())
        sb.append(genProtected(genFunc = false))
        sb.append(genPrivate(genFunc = false))
        sb.appendLine("};")
        return sb.toString()
    }

    override fun initIncludeList() {
        // not include self, inner type, and types can forward declare
        collectAllReferencedType().filter {
            it.getTopLevelFileType() != this.getTopLevelFileType() && (it.name.contains("::") || (it as? ClassType)?.isTemplateClass == true)
        }
            .map { this.path.relativePathTo(it.path) }.let(includeList::addAll)
        if (parents.isNotEmpty()) {
            includeList.addAll(parents.map { this.path.relativePathTo(it.path) })
        }
        includeList.remove(this.path.relativePathTo(this.path))
        includeList.remove("")
    }

    fun genParents(): String {
        if (parents.isEmpty()) {
            return ""
        }
        val sb = StringBuilder(": ")
        parents.joinToString(", ") { "public ${it.name}" }.let(sb::append)
        sb.append(" ")
        return sb.toString()
    }

    fun genAntiReconstruction(): String {
        val classType = if (this.type == TypeKind.STRUCT) "struct" else "class"
        val public = typeData.collectInstanceFunction()
            .filter { it.isConstructor() || (it.isOperator() && it.name == "operator=") }
        val genOperator = public.none {
            it.isOperator() && it.params?.let { params ->
                params.size == 1 && params[0].Name == "$classType $name const &"
            } == true && it.valType.Name == "$classType $name &"
        }
        val genEmptyParamConstructor = public.none { it.name == simpleName && it.params?.isEmpty() ?: true }
        val genMoveConstructor = public.none {
            it.name == simpleName && it.params?.let { params ->
                params.size == 1 && params[0].Name == "$classType $name const &"
            } == true
        }
        return if (!genOperator && !genEmptyParamConstructor && !genMoveConstructor) {
            "\n"
        } else
            StringBuilder(
                """

#ifndef DISABLE_CONSTRUCTOR_PREVENTION_$fullUpperEscapeName
public:

            """.trimIndent()
            ).apply {
                if (genOperator) {
                    appendLine("    $simpleName& operator=($simpleName const &) = delete;")
                }
                if (genMoveConstructor) {
                    appendLine("    $simpleName($simpleName const &) = delete;")
                }
                if (genEmptyParamConstructor) {
                    appendLine("    $simpleName() = delete;")
                }
                appendLine("#endif")
                appendLine()
            }.toString()
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
            sb.appendLine("#ifdef ENABLE_VIRTUAL_FAKESYMBOL_${fullUpperEscapeName}")
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
        if ((typeData.privateTypes?.isEmpty() != false) && (typeData.privateStaticTypes?.isEmpty() != false)) {
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
