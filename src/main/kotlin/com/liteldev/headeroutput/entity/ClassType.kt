package com.liteldev.headeroutput.entity

import com.liteldev.headeroutput.TypeManager
import com.liteldev.headeroutput.ast.template.NodeType
import com.liteldev.headeroutput.data.MemberTypeData
import com.liteldev.headeroutput.data.TypeData
import com.liteldev.headeroutput.getTopLevelFileType
import com.liteldev.headeroutput.isNamespace
import com.liteldev.headeroutput.relativePathTo

open class ClassType(
    name: String, typeData: TypeData, val isTemplateClass: Boolean = false,
) : BaseType(name, TypeKind.CLASS, typeData) {

    private val publicFunctions = arrayListOf<MemberTypeData>()
    private val publicVariables = arrayListOf<MemberTypeData>()
    private val protectedFunctions = arrayListOf<MemberTypeData>()
    private val protectedVariables = arrayListOf<MemberTypeData>()
    private val privateFunctions = arrayListOf<MemberTypeData>()
    private val privateVariables = arrayListOf<MemberTypeData>()

    val parents = arrayListOf<BaseType>()

    // fixme: Fix in header generator
    init {
        typeData.virtual?.forEach { virtual ->
            typeData.virtualUnordered?.removeIf { unordered ->
                virtual.symbol == unordered.symbol
            }
        }
        fun determineType(
            members: List<MemberTypeData>?,
            funList: MutableList<MemberTypeData>,
            varList: MutableList<MemberTypeData>
        ) {
            members?.forEach { member ->
                if (member.isStaticGlobalVariable()) {
                    varList.add(member)
                } else {
                    funList.add(member)
                }
            }
        }
        determineType(typeData.publicTypes, publicFunctions, publicVariables)
        determineType(typeData.publicStaticTypes, publicFunctions, publicVariables)
        determineType(typeData.protectedTypes, protectedFunctions, protectedVariables)
        determineType(typeData.protectedStaticTypes, protectedFunctions, protectedVariables)
        determineType(typeData.privateTypes, privateFunctions, privateVariables)
        determineType(typeData.privateStaticTypes, privateFunctions, privateVariables)
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
            val inners = generateOrder.filterIsInstance<ClassType>()
            if (inners.isEmpty()) {
                return@buildString
            }
            appendLine("// $simpleName inner types declare")
            appendLine("// clang-format off")
            appendLine(inners.joinToString(separator = "\n") { it.generateTypeDeclare() })
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
        getTemplateDefine()?.let(this::appendLine)
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
            val canNotForwardDeclareSimply =
                outerType?.isNamespace() == true || it.name.contains("::") || ((it as? ClassType)?.isTemplateClass == true)
            notInSameFile && canNotForwardDeclareSimply
        }
        declareRequiredTypes.forEach {
            if (it.outerType is ClassType || (it as? ClassType)?.isTemplateClass == true) {
                this.path.relativePathTo(it.path).let(includeList::add)
            } else {
                it.generateTypeDeclare().let(forwardDeclareList::add)
            }
        }
        val parents = collectParents()
        if (parents.isNotEmpty()) {
            includeList.addAll(parents.map { this.path.relativePathTo(it.path) })
        }
        includeList.remove(this.path.relativePathTo(this.path))
        includeList.remove("")
    }

    private fun genParents(): String {
        if (parents.isEmpty()) {
            return ""
        }
        val sb = StringBuilder(": ")
        parents.joinToString { "public ::${it.name}" }.let(sb::append)
        sb.append(" ")
        return sb.toString()
    }

    private fun genAntiReconstruction(): String {
        val classType = if (this.type == TypeKind.STRUCT) "struct" else "class"
        val public = typeData.collectInstanceFunction()
            .filter { it.isConstructor() || (it.isOperator() && it.name == "operator=") }
        val genOperator = public.none {
            it.isOperator() && it.params?.let { params ->
                params.size == 1 && params[0].Name == "$classType $name const &"
            } == true // && it.valType.Name == "$classType $name &" Removed because of overload
        }
        val genEmptyParamConstructor = public.none { it.name == simpleName && it.params?.isEmpty() ?: true }
        val genCopyConstructor = public.none {
            it.name == simpleName && it.params?.let { params ->
                params.size == 1 && params[0].Name == "$classType $name const &"
            } == true
        }
        return if (!genOperator && !genEmptyParamConstructor && !genCopyConstructor) {
            "\n"
        } else StringBuilder(
            "\npublic:\n    // prevent constructor by default\n"
        ).apply {
            if (genOperator) {
                appendLine("    $simpleName& operator=($simpleName const &) = delete;")
            }
            if (genCopyConstructor) {
                appendLine("    $simpleName($simpleName const &) = delete;")
            }
            if (genEmptyParamConstructor) {
                appendLine("    $simpleName() = delete;")
            }
            appendLine()
        }.toString()
    }

    private fun genPublic() = buildString {
        if (typeData.publicTypes?.isEmpty() != false &&
            typeData.publicStaticTypes?.isEmpty() != false &&
            typeData.virtual?.isEmpty() != false &&
            typeData.virtualUnordered?.isEmpty() != false
        ) {
            return ""
        }
        appendLine("public:")
        appendLine("    // NOLINTBEGIN")
        var counter = 0
        typeData.virtual?.forEach {
            if (it.namespace.isEmpty() || it.namespace == name) {
                appendLine(it.genFuncString(vIndex = counter))
            }
            counter++
        }
        if (typeData.virtualUnordered?.isNotEmpty() == true) {
            appendLine("#ifdef ENABLE_VIRTUAL_FAKESYMBOL_${fullUpperEscapeName}")
            typeData.virtualUnordered?.sortedBy { it.name }?.forEach {
                appendLine(it.genFuncString(useFakeSymbol = true))
            }
            appendLine("#endif")
        }
        publicFunctions.let(::generateMembers).let(::append)
        publicVariables.let(::generateMembers).let(::append)
        appendLine("    // NOLINTEND")
        trim()
        appendLine()
    }

    /**
     * @param genFunc if true, generate function, otherwise generate static global variable
     */
    private fun genProtected(genFunc: Boolean = true) = buildString {
        if (genFunc) {
            if (protectedFunctions.isEmpty()) {
                return ""
            }
            appendLine("    // protected:")
            appendLine("    // NOLINTBEGIN")
            protectedFunctions.let(::generateMembers).let(::append)
            appendLine("    // NOLINTEND")
        } else {
            if (protectedVariables.isEmpty()) {
                return ""
            }
            appendLine("protected:")
            appendLine("    // NOLINTBEGIN")
            protectedVariables.let(::generateMembers).let(::append)
            appendLine("    // NOLINTEND")
        }
        trim()
        appendLine()
    }

    /**
     * @param genFunc if true, generate function, otherwise generate static global variable
     */
    private fun genPrivate(genFunc: Boolean = true) = buildString {
        if (genFunc) {
            if (privateFunctions.isEmpty()) {
                return ""
            }
            appendLine("    // private:")
            appendLine("    // NOLINTBEGIN")
            privateFunctions.let(::generateMembers).let(::append)
            appendLine("    // NOLINTEND")
        } else {
            if (privateVariables.isEmpty()) {
                return ""
            }
            appendLine("private:")
            appendLine("    // NOLINTBEGIN")
            privateVariables.let(::generateMembers).let(::append)
            appendLine("    // NOLINTEND")
        }
        trim()
        appendLine()
    }

    private fun generateMembers(members: List<MemberTypeData>) = buildString {
        fun isStatic(member: MemberTypeData) = !(member.isPtrCall() || member.isVirtual())
        members.sortedWith { o1, o2 ->
            if (isStatic(o1) == isStatic(o2)) {
                o1.name.compareTo(o2.name)
            } else {
                if (isStatic(o1)) 1 else -1
            }
        }.forEach {
            appendLine(it.genFuncString())
        }
    }

    private fun getTemplateDefine(): String? {
        val params = TypeManager.template[name] ?: return null
        check(params.isNotEmpty()) { "$name: template must have at least one parameter." }
        return buildString {
            append("template<")
            if (params[0] == NodeType.VARIABLE) {
                append("typename... T0>")
                return@buildString
            }
            check(params.last() != NodeType.VARIABLE || params.size > 1) {
                "$name: variable template must be the last template parameter, but not the only one."
            }
            val isVariable = params.last() == NodeType.VARIABLE
            params.forEachIndexed { index, nodeType ->
                when (nodeType) {
                    NodeType.TYPE -> append("typename")
                    NodeType.INTEGER -> append("int")
                    NodeType.FLOAT -> append("float")
                    NodeType.BOOLEAN -> append("bool")
                    NodeType.VARIABLE -> return@forEachIndexed
                }
                if (isVariable && index == params.size - 2) {
                    append("...")
                }
                append(" T$index")
                if (index != params.size - 1 && params.getOrNull(index + 1) != NodeType.VARIABLE) {
                    append(", ")
                }
            }
            append(">")
        }
    }

    fun collectParents(): List<BaseType> {
        val parents = arrayListOf<BaseType>()
        parents.addAll(this.parents)
        innerTypes.forEach { innerType ->
            if (innerType is ClassType) parents.addAll(innerType.collectParents())
        }
        return parents
    }
}
