package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.TypeManager
import com.liteldev.headeroutput.data.TypeData
import com.liteldev.headeroutput.getTopLevelFileType
import com.liteldev.headeroutput.relativePathTo

open class ClassType(
    name: String, typeData: TypeData, val isTemplateClass: Boolean = false,
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

    private fun generateInnerTypeDefine(): String {
        val generateOrder = innerTypes.toMutableList()
        generateOrder.sortWith(Comparator { o1, o2 ->
            if (o1 is EnumType && o2 is EnumType) {
                return@Comparator o1.name.compareTo(o2.name)
            }
            if (o1 is EnumType || o2 is EnumType) {
                return@Comparator if (o1 is EnumType) -1 else 1
            }
            if (o1.allReferences.any { it in o2.allInnerTypes }) {
                return@Comparator 1
            }
            if (o2.allReferences.any { it in o1.allInnerTypes }) {
                return@Comparator -1
            }

            return@Comparator o1.name.compareTo(o2.name)
        })

        val generateDeclares = buildString {
            appendLine("// $simpleName inner types declare")
            appendLine("// clang-format off")
            appendLine(generateOrder.filterIsInstance<ClassType>()
                .joinToString(separator = "\n") { it.generateTypeDeclare() })
            appendLine("// clang-format on\n")
        }
        val generatedTypes = buildString {
            appendLine("// $simpleName inner types define")
            append(generateOrder.joinToString(separator = "\n") { it.generateTypeDefine() })
        }
        return if (innerTypes.isNotEmpty()) "\n$generateDeclares$generatedTypes" else ""
    }

    override fun generateTypeDefine(): String = buildString {
        val classType = if (type == TypeKind.STRUCT) "struct" else "class"
        TypeManager.template[name]?.let(this::appendLine)
        appendLine("$classType $simpleName ${genParents()}{")
        if (innerTypes.isNotEmpty()) {
            appendLine("public:")
            append(generateInnerTypeDefine().replace("\n", "\n    "))
        }
        append(genAntiReconstruction())
        append(genPublic())
        append(genProtected())
        append(genPrivate())
        if (type == TypeKind.CLASS) {
            append(genProtected(genFunc = false))
            append(genPrivate(genFunc = false))
        }
        appendLine("};")
    }

    /**
     * generate type declare
     * when type's outer type is null or class, just return the type's declaration because nested type declare forward is not allowed in c++
     * when type's outer type is namespace, return namespace declaration + type's declaration
     */
    override fun generateTypeDeclare(): String {
        var baseDeclaration = when (type) {
            TypeKind.CLASS -> "class $simpleName;"
            TypeKind.STRUCT -> "struct $simpleName;"
            else -> error("not support type $type")
        }
        getTemplateDefine()?.let { baseDeclaration = "$it $baseDeclaration" }
        if (outerType == null || outerType is ClassType) {
            return baseDeclaration
        }
        if (outerType?.type == TypeKind.NAMESPACE) {
            return "namespace ${outerType?.name} { $baseDeclaration }"
        }
        error("unreachable")
    }

    override fun initIncludeAndForwardDeclareList() {
        // not include self, inner type, and types can forward declare
        val declareRequiredTypes = allReferences.filter {
            val notInSameFile = it.getTopLevelFileType() != this.getTopLevelFileType()
            val canNotForwardDeclare = it.name.contains("::") || ((it as? ClassType)?.isTemplateClass == true)
            notInSameFile && canNotForwardDeclare
        }
        declareRequiredTypes
            .filter { it.outerType is ClassType }.map { this.path.relativePathTo(it.path) }
            .let(includeList::addAll)
        if (parents.isNotEmpty()) {
            includeList.addAll(parents.map { this.path.relativePathTo(it.path) })
        }
        includeList.remove(this.path.relativePathTo(this.path))
        includeList.remove("")

        declareRequiredTypes
            .filter { it.outerType !is ClassType }.map { it.generateTypeDeclare() }
            .let(forwardDeclareList::addAll)
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
        } else StringBuilder(
            "\n#ifndef DISABLE_CONSTRUCTOR_PREVENTION_$fullUpperEscapeName\npublic:\n"
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
            if (it.namespace.isEmpty() || it.namespace == name) {
                sb.appendLine(it.genFuncString(vIndex = counter))
            }
            counter++
        }

        if (typeData.virtualUnordered?.isNotEmpty() == true) {
            sb.appendLine("#ifdef ENABLE_VIRTUAL_FAKESYMBOL_${fullUpperEscapeName}")
            typeData.virtualUnordered?.sortedBy { it.name }?.forEach {
                sb.appendLine(it.genFuncString(useFakeSymbol = true))
            }
            sb.appendLine("#endif")
        }

        typeData.publicTypes?.sortedBy { it.name }?.forEach {
            sb.appendLine(it.genFuncString())
        }
        typeData.publicStaticTypes?.sortedBy { it.name }?.forEach {
            sb.appendLine(it.genFuncString())
        }
        if (sb.equals("public:\n")) return ""
        sb.trim()
        sb.appendLine()
        return sb.toString()
    }

    fun genProtected(genFunc: Boolean = true): String {
        if ((typeData.protectedTypes == null || typeData.protectedTypes?.isEmpty() == true) &&
            (typeData.protectedStaticTypes == null || typeData.protectedStaticTypes?.isEmpty() == true)
        ) {
            return ""
        }
        val sb = StringBuilder()
        if (genFunc) sb.appendLine("//protected:")
        else sb.appendLine("protected:")
        typeData.protectedTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable())) {
                sb.appendLine(it.genFuncString())
            }
        }
        typeData.protectedStaticTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable())) {
                sb.appendLine(it.genFuncString())
            }
        }
        if (sb.equals("protected:\n") || sb.equals("//protected:\n")) return ""
        sb.trim()
        sb.appendLine()
        return sb.toString()
    }

    fun genPrivate(genFunc: Boolean = true): String {
        if ((typeData.privateTypes?.isEmpty() != false) && (typeData.privateStaticTypes?.isEmpty() != false)) {
            return ""
        }
        val sb = StringBuilder()
        if (genFunc) sb.appendLine("//private:")
        else sb.appendLine("private:")
        typeData.privateTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable())) {
                sb.appendLine(it.genFuncString())
            }
        }
        typeData.privateStaticTypes?.sortedBy { it.name }?.forEach {
            if ((genFunc && !it.isStaticGlobalVariable()) || (!genFunc && it.isStaticGlobalVariable())) {
                sb.appendLine(it.genFuncString())
            }
        }
        if (sb.equals("private:\n") || sb.equals("//private:\n")) return ""
        sb.trim()
        sb.appendLine()
        return sb.toString()
    }

    private fun getTemplateDefine() = TypeManager.template[name]
}
